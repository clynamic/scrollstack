package net.clynamic.projects

import kotlinx.coroutines.Dispatchers
import net.clynamic.common.Service
import net.clynamic.userprojects.UserProjectsService
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
        val name = text("name")

        override val primaryKey = PrimaryKey(id)
    }

    object GithubProjects : Table() {
        val id = integer("id").references(PartialProjects.id, onDelete = ReferenceOption.CASCADE)
        val owner = text("owner")
        val repo = text("repo")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) { SchemaUtils.create(PartialProjects, GithubProjects) }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private suspend fun lookupProjects(rows: List<ResultRow>): List<PartialProject> {
        return dbQuery {
            val ids = rows.map { it[PartialProjects.id] }
            val remoteGithubProjectsMap =
                GithubProjects.select { GithubProjects.id inList ids }
                    .associateBy { it[GithubProjects.id] }

            val finalProjects = mutableListOf<PartialProject>()

            for (row in rows) {
                val id = row[PartialProjects.id]
                val title = row[PartialProjects.name]

                remoteGithubProjectsMap[id]?.let { remoteProjectRow ->
                    finalProjects.add(
                        RemoteGithubProject(
                            id = id,
                            name = title,
                            owner = remoteProjectRow[GithubProjects.owner],
                            repo = remoteProjectRow[GithubProjects.repo]
                        )
                    )
                }
            }

            finalProjects.toList()
        }
    }

    override suspend fun create(request: ProjectRequest): Int = dbQuery {
        val id = PartialProjects.insert {
            it[name] = request.name
        }[PartialProjects.id]

        when (request) {
            is GithubProjectRequest -> GithubProjects.insert {
                it[this.id] = id
                it[owner] = request.owner
                it[repo] = request.repo
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
            is GithubProjectUpdate -> {
                dbQuery {
                    GithubProjects.update({ GithubProjects.id eq id }) {
                        update.owner?.let { owner -> it[GithubProjects.owner] = owner }
                        update.repo?.let { repo -> it[GithubProjects.repo] = repo }
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