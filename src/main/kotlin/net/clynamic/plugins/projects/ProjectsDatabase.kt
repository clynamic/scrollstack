package net.clynamic.plugins.projects

import kotlinx.coroutines.Dispatchers
import net.clynamic.plugins.Service
import net.clynamic.plugins.userprojects.UserProjectsService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
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

class ProjectService(database: Database) : Service<ProjectRequest, PartialProject, ProjectUpdate> {
    object PartialProjects : Table() {
        val id = integer("id").autoIncrement()
        val title = text("title")

        override val primaryKey = PrimaryKey(id)
    }

    object RemoteProjects : Table() {
        val id = integer("id").references(PartialProjects.id, onDelete = ReferenceOption.CASCADE)
        val url = text("url")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) { SchemaUtils.create(PartialProjects, RemoteProjects) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun lookupProjects(rows: List<ResultRow>): List<PartialProject> {
        return dbQuery {
            val ids = rows.map { it[PartialProjects.id] }
            val remoteProjectsMap = RemoteProjects.select { RemoteProjects.id inList ids }
                .associateBy { it[RemoteProjects.id] }

            val finalProjects = mutableListOf<PartialProject>()

            for (row in rows) {
                val id = row[PartialProjects.id]
                val title = row[PartialProjects.title]

                remoteProjectsMap[id]?.let { remoteProjectRow ->
                    finalProjects.add(
                        RemoteProject(
                            id = id,
                            title = title,
                            url = remoteProjectRow[RemoteProjects.url]
                        )
                    )
                }
            }

            finalProjects.toList()
        }
    }

    override suspend fun create(request: ProjectRequest): Int = dbQuery {
        val id = PartialProjects.insert {
            it[title] = request.title
        }[PartialProjects.id]

        when (request) {
            is RemoteProjectRequest -> RemoteProjects.insert {
                it[this.id] = id
                it[url] = request.url
            }
        }

        id
    }

    override suspend fun read(id: Int): PartialProject? {
        return dbQuery {
            val row = PartialProjects.select { PartialProjects.id eq id }.limit(1).firstOrNull()
                ?: return@dbQuery null
            lookupProjects(listOf(row)).firstOrNull()
        }
    }

    override suspend fun page(page: Int?, size: Int?): List<PartialProject> {
        return this.page(page, size)
    }

    suspend fun page(page: Int?, size: Int?, user: Int? = null): List<PartialProject> {
        val sized = (size ?: 20).coerceAtMost(40)
        val paged = (page ?: 1).coerceAtLeast(1)

        return dbQuery {
            val query = if (user != null) {
                PartialProjects
                    .innerJoin(
                        UserProjectsService.UserProjects,
                        { id },
                        { projectId })
                    .select { UserProjectsService.UserProjects.userId eq user }
            } else {
                PartialProjects.selectAll()
            }
            query.limit(sized, ((paged - 1) * sized).toLong())
                .toList()

            val partialProjects = query.toList()
            lookupProjects(partialProjects)
        }
    }

    override suspend fun update(id: Int, update: ProjectUpdate) {
        when (update) {
            is RemoteProjectUpdate -> {
                dbQuery {
                    RemoteProjects.update({ RemoteProjects.id eq id }) {
                        if (update.url != null)
                            it[url] = update.url
                    }
                }
            }
        }
    }

    override suspend fun delete(id: Int) {
        dbQuery {
            PartialProjects.deleteWhere { PartialProjects.id eq id }
        }
    }
}