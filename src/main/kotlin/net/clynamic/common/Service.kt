package net.clynamic.common

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

interface Service<Request, Model, Update, Id> {
    suspend fun create(request: Request): Id
    suspend fun read(id: Id): Model = readOrNull(id) ?: throw NoSuchRecordException(id)
    suspend fun readOrNull(id: Id): Model?
    suspend fun page(page: Int? = null, size: Int? = null): List<Model>
    suspend fun update(id: Id, update: Update)
    suspend fun delete(id: Id)

    companion object {
        const val defaultPage = 0
        const val defaultSize = 20
    }
}

class NoSuchRecordException(id: Any?, type: String? = null) :
    NoSuchElementException("No ${type ?: "record"} found for id: $id")

abstract class ServiceTable<Id>(name: String = "") : Table(name) {
    abstract fun selector(id: Id): Op<Boolean>
    abstract fun toId(row: ResultRow): Id
}

abstract class IntServiceTable(name: String = "") : ServiceTable<Int>(name) {
    val id: Column<Int>
        get() = _id

    // We need to use a backing field to allow for overriding the id column
    // And it cannot be late init because we need access to the Table functions
    @Suppress("LeakingThis")
    private var _id: Column<Int> = getIdColumn()

    protected open fun getIdColumn(): Column<Int> = integer("id").autoIncrement()

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(id)

    override fun selector(id: Int): Op<Boolean> = this.id eq id
    override fun toId(row: ResultRow): Int = row[id]
}

abstract class SqlService<Request, Model, Update, Id, TableType : ServiceTable<Id>>(
    database: Database,
) : Service<Request, Model, Update, Id> {

    abstract val table: TableType

    init {
        transaction(database) { SchemaUtils.create(table) }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    abstract fun toModel(row: ResultRow): Model

    internal fun Query.toModel(): Model? = toModelList().singleOrNull()
    internal fun Query.toModelList(): List<Model> = mapNotNull(::toModel)

    open suspend fun query(
        page: Int?,
        size: Int?,
        sort: String?,
        order: SortOrder?,
    ): Query = dbQuery {
        val pageSize = max(0, min(size ?: Service.defaultSize, 100))
        val pageNumber = max(0, (page ?: Service.defaultPage) - 1)

        val query = table.selectAll()

        if (sort != null) {
            val column = table.columns.firstOrNull { it.name.equals(sort, ignoreCase = true) }
            if (column != null) {
                query.orderBy(column to (order ?: SortOrder.DESC))
            }
        }

        query.limit(pageSize, pageSize * pageNumber.toLong())
    }

    open suspend fun page(
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
        order: SortOrder? = null,
    ): List<Model> = dbQuery { query(page, size, sort, order).toModelList() }

    override suspend fun page(page: Int?, size: Int?): List<Model> = dbQuery {
        page(page, size, null, null)
    }

    abstract fun fromRequest(statement: InsertStatement<*>, request: Request)
    abstract fun fromUpdate(statement: UpdateStatement, update: Update)

    override suspend fun create(request: Request): Id = dbQuery {
        table.insert {
            fromRequest(it, request)
        }.resultedValues!!.single().let(table::toId)
    }

    override suspend fun read(id: Id): Model =
        readOrNull(id) ?: throw NoSuchRecordException(id, table.tableName)

    override suspend fun readOrNull(id: Id): Model? = dbQuery {
        table.select { table.selector(id) }
            .mapNotNull(::toModel)
            .singleOrNull()
    }

    override suspend fun update(id: Id, update: Update): Unit = dbQuery {
        table.update({ table.selector(id) }) {
            fromUpdate(it, update)
        }.let { if (it == 0) throw NoSuchRecordException(id, table.tableName) }
    }

    override suspend fun delete(id: Id): Unit = dbQuery {
        table.deleteWhere { table.selector(id) }
            .let { if (it == 0) throw NoSuchRecordException(id, table.tableName) }
    }
}


abstract class IntSqlService<Request, Model, Update, TableType : IntServiceTable>(
    database: Database,
) : SqlService<Request, Model, Update, Int, TableType>(database)


class InstantAsISO : ColumnType() {
    override fun sqlType(): String = "VARCHAR"

    override fun valueFromDB(value: Any): Instant {
        return Instant.parse(value as String)
    }

    override fun notNullValueToDB(value: Any): Any {
        if (value is Instant) {
            return value.toString()
        }
        return super.notNullValueToDB(value)
    }
}

fun Table.instant(name: String): Column<Instant> = registerColumn(name, InstantAsISO())


class UpdateStatementSets(private val statement: UpdateStatement) {
    infix fun <T> Column<T>.set(value: T?) {
        if (value != null) statement[this] = value
    }
}

fun UpdateStatement.setAll(block: UpdateStatementSets.() -> Unit) {
    val dsl = UpdateStatementSets(this)
    dsl.block()
}

class InsertStatementSets(private val statement: InsertStatement<*>) {
    infix fun <T> Column<T>.set(value: T) {
        statement[this] = value
    }
}

fun InsertStatement<*>.setAll(block: InsertStatementSets.() -> Unit) {
    val dsl = InsertStatementSets(this)
    dsl.block()
}