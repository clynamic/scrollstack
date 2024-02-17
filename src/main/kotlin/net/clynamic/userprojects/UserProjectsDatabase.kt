package net.clynamic.userprojects

import net.clynamic.common.ServiceTable
import net.clynamic.common.SqlService
import net.clynamic.common.setAll
import net.clynamic.projects.ProjectService
import net.clynamic.users.UserService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement

class UserProjectsService(database: Database) :
    SqlService<UserProjectRelation, UserProjectRelation, Nothing, UserProjectRelation, UserProjectsService.UserProjects>(
        database
    ) {
    object UserProjects : ServiceTable<UserProjectRelation>() {
        val userId =
            integer("user_id").references(UserService.Users.id, onDelete = ReferenceOption.CASCADE)
        val projectId =
            integer("project_id").references(
                ProjectService.ProjectSources.id,
                onDelete = ReferenceOption.CASCADE
            )

        override val primaryKey = PrimaryKey(userId, projectId)

        override fun selector(id: UserProjectRelation): Op<Boolean> =
            (userId eq id.userId) and (projectId eq id.projectId)

        override fun toId(row: ResultRow): UserProjectRelation = UserProjectRelation(
            row[userId],
            row[projectId]
        )
    }

    override val table: UserProjects
        get() = UserProjects

    override fun toModel(row: ResultRow): UserProjectRelation = UserProjectRelation(
        userId = row[UserProjects.userId],
        projectId = row[UserProjects.projectId]
    )

    override fun fromUpdate(statement: UpdateStatement, update: Nothing): Unit =
        throw NotImplementedError("UserProjectsService does not support updates.")

    override fun fromRequest(statement: InsertStatement<*>, request: UserProjectRelation) =
        statement.setAll {
            UserProjects.userId set request.userId
            UserProjects.projectId set request.projectId
        }
}