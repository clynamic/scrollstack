package net.clynamic.projects

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

class ProjectsService(database: Database) :
    IntSqlService<ProjectRequest, ProjectSource, ProjectUpdate, ProjectsService.ProjectSources>(
        database
    ) {
    object ProjectSources : IntServiceTable() {
        val name = text("name")
        val sourceStr = text("source")
        val type = enumeration("type", ProjectType::class)
    }

    override val table: ProjectSources
        get() = ProjectSources

    override fun toModel(row: ResultRow): ProjectSource {
        return ProjectSource(
            id = row[ProjectSources.id],
            name = row[ProjectSources.name],
            source = row[ProjectSources.sourceStr],
            type = row[ProjectSources.type]
        )
    }

    override fun fromUpdate(statement: UpdateStatement, update: ProjectUpdate) {
        statement.setAll {
            ProjectSources.name set update.name
            ProjectSources.sourceStr set update.source
            ProjectSources.type set update.type
        }
    }

    override fun fromRequest(statement: InsertStatement<*>, request: ProjectRequest) {
        statement.setAll {
            ProjectSources.name set request.name
            ProjectSources.sourceStr set request.source
            ProjectSources.type set request.type
        }
    }

    suspend fun page(
        page: Int? = null,
        size: Int? = null,
        sort: String? = null,
        order: SortOrder? = null,
        user: Int? = null
    ): List<ProjectSource> = dbQuery {
        query(page, size, sort, order).let { baseQuery ->
            user?.let {
                baseQuery.andWhere {
                    ProjectSources.id inList UserProjects.select { UserProjects.userId eq it }
                        .map { it[UserProjects.projectId] }
                }
            } ?: baseQuery
        }.mapNotNull(::toModel)
    }
}