package net.clynamic.projects

import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import net.clynamic.common.DATABASE_KEY
import net.clynamic.common.getPageAndSize
import net.clynamic.common.getSortAndOrder
import net.clynamic.common.serverUrl
import net.clynamic.contents.ContentsService
import java.io.IOException

fun Application.configureProjectsRouting() {
    val database = attributes[DATABASE_KEY]
    val service = ProjectsService(database)
    val contentsService = ContentsService(database)
    val client = ProjectsClient(contentsService)

    routing {
        get("/projects/{id}", {
            tags = listOf("projects")
            description = "Get a project by ID"
            operationId = "project"
            request {
                pathParameter<Int>("id") {
                    description = "The project ID"
                    required = true
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "The project"
                    body<Project> {}
                }
                HttpStatusCode.NotFound to {
                    description = "Project not found"
                }
            }
        }) {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val projectSource = service.read(id)
            val project: Project
            try {
                project = client.resolve(projectSource, call.request.serverUrl)
            } catch (e: IOException) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respond(HttpStatusCode.OK, project)
        }
        get("/projects", {
            tags = listOf("projects")
            description = "Get a page of projects"
            operationId = "projects"
            request {
                queryParameter<Int?>("page") { description = "The page number" }
                queryParameter<Int?>("size") { description = "The page size" }
                queryParameter<String?>("sort") { description = "The sort field" }
                queryParameter<String?>("order") { description = "The sort order" }
                queryParameter<Int?>("user") { description = "User ID to filter by association" }
            }
            response {
                HttpStatusCode.OK to {
                    body<List<Project>> {}
                }
            }
        }) {
            val (page, size) = call.getPageAndSize()
            val (sort, order) = call.getSortAndOrder()
            val user = call.parameters["user"]?.toIntOrNull()
            val projectSources = service.page(page, size, sort, order, user)
            val projects = client.resolve(projectSources, call.request.serverUrl)
            call.respond(HttpStatusCode.OK, projects)
        }
        authenticate {
            post("/projects", {
                tags = listOf("projects")
                description = "Create a project"
                securitySchemeName = "bearer"
                operationId = "createProject"
                request {
                    body<ProjectRequest> {
                        description = "New project properties"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.Created to {
                        body<Int> {
                            description = "The new project ID"
                        }
                    }
                }
            }) {
                val project = call.receive<ProjectRequest>()
                val id = service.create(project)
                call.response.headers.append("Location", "/projects/${id}")
                call.respond(HttpStatusCode.Created, id)
            }
            put("/projects/{id}", {
                tags = listOf("projects")
                description = "Update a project by ID"
                securitySchemeName = "bearer"
                operationId = "updateProject"
                request {
                    pathParameter<Int>("id") {
                        description = "The project ID"
                        required = true
                    }
                    body<ProjectUpdate> {
                        description = "New project properties"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.NoContent to {
                        description = "Project updated"
                    }
                }
            }) {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }

                val project = call.receive<ProjectUpdate>()
                service.update(id, project)
                call.respond(HttpStatusCode.NoContent)
            }
            delete("/projects/{id}", {
                tags = listOf("projects")
                description = "Delete a project by ID"
                securitySchemeName = "bearer"
                operationId = "deleteProject"
                request {
                    pathParameter<Int>("id") {
                        description = "The project ID"
                        required = true
                    }
                }
                response {
                    HttpStatusCode.NoContent to {
                        description = "Project deleted"
                    }
                }
            }) {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }

                service.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}