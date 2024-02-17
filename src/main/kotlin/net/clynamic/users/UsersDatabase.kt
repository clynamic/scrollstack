package net.clynamic.users

import net.clynamic.common.IntServiceTable
import net.clynamic.common.IntSqlService
import net.clynamic.common.setAll
import net.clynamic.userprojects.UserProjectsService.UserProjects
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement

class UserService(database: Database) :
    IntSqlService<UserRequest, User, UserUpdate, UserService.Users>(database) {
    object Users : IntServiceTable() {
        val name = varchar("name", 128)
        val email = varchar("email", 128)
        val pronouns = varchar("pronouns", 32).nullable()
        val bio = text("bio").nullable()
        val discord = varchar("discord", 32).nullable()
        val github = varchar("github", 32).nullable()
    }

    override val table: Users
        get() = Users

    override fun toModel(row: ResultRow): User {
        return User(
            id = row[Users.id],
            name = row[Users.name],
            email = row[Users.email],
            pronouns = row[Users.pronouns],
            bio = row[Users.bio],
            discord = row[Users.discord],
            github = row[Users.github]
        )
    }

    override fun fromUpdate(statement: UpdateStatement, update: UserUpdate) {
        statement.setAll {
            Users.name set update.name
            Users.email set update.email
            Users.pronouns set update.pronouns
            Users.bio set update.bio
            Users.discord set update.discord
            Users.github set update.github
        }
    }

    override fun fromRequest(statement: InsertStatement<*>, request: UserRequest) {
        statement.setAll {
            Users.name set request.name
            Users.email set request.email
            Users.pronouns set request.pronouns
            Users.bio set request.bio
            Users.discord set request.discord
            Users.github set request.github
        }
    }

    suspend fun page(
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
        order: SortOrder? = null,
        project: Int? = null
    ): List<User> = dbQuery {
        query(page, size, sort, order).let { baseQuery ->
            project?.let {
                baseQuery.andWhere {
                    Users.id inList UserProjects.select { UserProjects.projectId eq project }
                        .map { it[UserProjects.userId] }
                }
            } ?: baseQuery
        }.mapNotNull(::toModel)
    }
}
