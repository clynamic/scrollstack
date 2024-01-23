package net.clynamic.common

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
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
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

interface Service<Request, Model, Update, Id> {
    suspend fun create(request: Request): Id
    suspend fun read(id: Id): Model?
    suspend fun page(page: Int? = null, size: Int? = null): List<Model>
    suspend fun update(id: Id, update: Update)
    suspend fun delete(id: Id)
}

abstract class ServiceTable<Id>(name: String = "") : Table(name) {
    abstract fun selector(id: Id): Op<Boolean>
    abstract fun toId(row: ResultRow): Id
}

abstract class IntServiceTable(name: String = "") : ServiceTable<Int>(name) {
    open val id = integer("id").autoIncrement()
    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(id)

    override fun selector(id: Int): Op<Boolean> = this.id eq id
    override fun toId(row: ResultRow): Int = row[id]
}

abstract class SqlService<Request, Model, Update, Id, TableType : ServiceTable<Id>>(
    database: Database
) : Service<Request, Model, Update, Id> {

    abstract val table: TableType

    init {
        transaction(database) { SchemaUtils.create(table) }
    }

    internal suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    abstract fun toModel(row: ResultRow): Model
    abstract fun fromRequest(statement: UpdateBuilder<*>, request: Request)
    abstract fun fromUpdate(statement: UpdateBuilder<*>, update: Update)

    override suspend fun create(request: Request): Id = dbQuery {
        table.insert {
            fromRequest(it, request)
        }.resultedValues!!.single().let(table::toId)
    }

    override suspend fun read(id: Id): Model? = dbQuery {
        table.select { table.selector(id) }
            .mapNotNull(::toModel)
            .singleOrNull()
    }

    internal suspend fun query(
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
        order: SortOrder? = null
    ): Query = dbQuery {
        val pageSize = (size ?: 20).coerceAtMost(40)
        val pageNumber = (page ?: 1).coerceAtLeast(1)

        val query = table.selectAll()

        if (sort != null) {
            val column = table.columns.firstOrNull { it.name.equals(sort, ignoreCase = true) }
            if (column != null) {
                query.orderBy(column to (order ?: SortOrder.DESC))
            }
        }

        query.limit(pageSize, pageSize * pageNumber.toLong())
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun page(
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
        order: SortOrder? = null
    ): List<Model> = dbQuery {
        query(page, size, sort, order).map(::toModel)
    }

    override suspend fun page(page: Int?, size: Int?): List<Model> = dbQuery {
        page(page, size, null, null)
    }

    override suspend fun update(id: Id, update: Update): Unit = dbQuery {
        table.update({ table.selector(id) }) {
            fromUpdate(it, update)
        }
    }

    override suspend fun delete(id: Id): Unit = dbQuery {
        table.deleteWhere { table.selector(id) }
    }
}

abstract class IntSqlService<Request, Model, Update, TableType : IntServiceTable>(
    database: Database
) : SqlService<Request, Model, Update, Int, TableType>(database)

class UpdateBuilderSets(private val statement: UpdateBuilder<*>) {
    infix fun <T> Column<T>.set(value: T?) {
        if (value != null) statement[this] = value
    }
}

fun UpdateBuilder<*>.setAll(block: UpdateBuilderSets.() -> Unit) {
    val dsl = UpdateBuilderSets(this)
    dsl.block()
}