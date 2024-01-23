package net.clynamic.userprojects

import kotlinx.coroutines.Dispatchers
import net.clynamic.projects.ProjectService
import net.clynamic.users.UserService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class UserProjectsService(database: Database) {

    object UserProjects : Table() {
        val userId =
            integer("userId").references(UserService.Users.id, onDelete = ReferenceOption.CASCADE)
        val projectId =
            integer("projectId").references(
                ProjectService.PartialProjects.id,
                onDelete = ReferenceOption.CASCADE
            )

        override val primaryKey = PrimaryKey(userId, projectId)
    }

    init {
        transaction(database) { SchemaUtils.create(UserProjects) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun has(userId: Int, projectId: Int): Boolean {
        return dbQuery {
            UserProjects.select {
                UserProjects.userId.eq(userId) and UserProjects.projectId.eq(
                    projectId
                )
            }
                .count() > 0
        }
    }

    suspend fun associate(userId: Int, projectId: Int) {
        dbQuery {
            UserProjects.insert {
                it[UserProjects.userId] = userId
                it[UserProjects.projectId] = projectId
            }
        }
    }

    suspend fun dissociate(userId: Int, projectId: Int) {
        dbQuery {
            UserProjects.deleteWhere {
                UserProjects.userId.eq(userId) and UserProjects.projectId.eq(projectId)
            }
        }
    }
}