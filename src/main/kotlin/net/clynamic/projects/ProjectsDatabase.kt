package net.clynamic.projects

import kotlinx.coroutines.Dispatchers
import net.clynamic.common.Service
import net.clynamic.userprojects.UserProjectsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ProjectService(database: Database) : Service<ProjectRequest, ProjectSource, ProjectUpdate> {
    object ProjectSources : Table() {
        val id = integer("id").autoIncrement()
        val name = text("name")
        val sourceStr = text("source")
        val type = enumeration("type", ProjectType::class)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) { SchemaUtils.create(ProjectSources) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(request: ProjectRequest): Int = dbQuery {
        ProjectSources.insert {
            it[name] = request.name
            it[sourceStr] = request.source
            it[type] = request.type
        } get ProjectSources.id
    }

    override suspend fun read(id: Int): ProjectSource? = dbQuery {
        ProjectSources.select { ProjectSources.id eq id }.firstOrNull()?.let { row ->
            ProjectSource(
                id = row[ProjectSources.id],
                name = row[ProjectSources.name],
                source = row[ProjectSources.sourceStr],
                type = row[ProjectSources.type]
            )
        }
    }

    override suspend fun page(page: Int?, size: Int?): List<ProjectSource> = this.page(page, size)

    suspend fun page(page: Int?, size: Int?, user: Int? = null): List<ProjectSource> = dbQuery {
        val sized = (size ?: 20).coerceAtMost(40)
        val paged = (page ?: 1).coerceAtLeast(1)

        val query = if (user != null) {
            ProjectSources
                .innerJoin(
                    UserProjectsService.UserProjects,
                    { id },
                    { projectId })
                .select { UserProjectsService.UserProjects.userId eq user }
        } else {
            ProjectSources.selectAll()
        }
        query.limit(sized, ((paged - 1) * sized).toLong())
            .toList()

    }.map { row ->
        ProjectSource(
            id = row[ProjectSources.id],
            name = row[ProjectSources.name],
            source = row[ProjectSources.sourceStr],
            type = row[ProjectSources.type]
        )
    }

    override suspend fun update(id: Int, update: ProjectUpdate): Unit = dbQuery {
        ProjectSources.update({ ProjectSources.id eq id }) {
            update.name?.let { name -> it[ProjectSources.name] = name }
            update.source?.let { source -> it[sourceStr] = source }
            update.type?.let { type -> it[ProjectSources.type] = type }
        }
    }


    override suspend fun delete(id: Int): Unit = dbQuery {
        ProjectSources.deleteWhere { ProjectSources.id eq id }
    }
}