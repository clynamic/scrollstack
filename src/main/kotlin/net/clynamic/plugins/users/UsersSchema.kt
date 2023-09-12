package net.clynamic.plugins.users

import kotlinx.coroutines.Dispatchers
import net.clynamic.plugins.Service
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class UserRequest(
    val name: String,
    val email: String,
    val pronouns: String? = null,
    val bio: String? = null,
    val discord: String? = null,
    val github: String? = null,
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val pronouns: String? = null,
    val bio: String? = null,
    val discord: String? = null,
    val github: String? = null,
)

class UserService(database: Database) : Service<UserRequest, User> {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 128)
        val email = varchar("email", 128)
        val pronouns = varchar("pronouns", 32).nullable()
        val bio = text("bio").nullable()
        val discord = varchar("discord", 32).nullable()
        val github = varchar("github", 32).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) { SchemaUtils.create(Users) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(request: UserRequest): Int = dbQuery {
        Users.insert {
            it[name] = request.name
            it[email] = request.email
            it[pronouns] = request.pronouns
            it[bio] = request.bio
            it[discord] = request.discord
            it[github] = request.github
        }[Users.id]
    }

    override suspend fun read(id: Int): User? {
        return dbQuery {
            Users.select { Users.id eq id }
                .map {
                    User(
                        id = it[Users.id],
                        name = it[Users.name],
                        email = it[Users.email],
                        pronouns = it[Users.pronouns],
                        bio = it[Users.bio],
                        discord = it[Users.discord],
                        github = it[Users.github],
                    )
                }
                .singleOrNull()
        }
    }

    override suspend fun page(page: Int?, size: Int?): List<User> {
        val sized = (size ?: 20).coerceAtMost(40)
        val paged = (page ?: 1).coerceAtLeast(1)
        return dbQuery {
            Users.selectAll()
                .limit(sized, ((paged - 1) * sized).toLong())
                .map {
                    User(
                        id = it[Users.id],
                        name = it[Users.name],
                        email = it[Users.email],
                        pronouns = it[Users.pronouns],
                        bio = it[Users.bio],
                        discord = it[Users.discord],
                        github = it[Users.github],
                    )
                }
        }
    }

    override suspend fun update(id: Int, request: UserRequest) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[name] = request.name
                it[email] = request.email
                it[pronouns] = request.pronouns
                it[bio] = request.bio
                it[discord] = request.discord
                it[github] = request.github
            }
        }
    }

    override suspend fun delete(id: Int) {
        dbQuery { Users.deleteWhere { Users.id.eq(id) } }
    }
}
