package net.clynamic.plugins.projects

import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.github.smiley4.ktorswaggerui.dsl.put
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import net.clynamic.plugins.DATABASE_KEY
import net.clynamic.plugins.getPageAndSize

fun Application.configureProjectsRouting() {
    val database = attributes[DATABASE_KEY]
    val service = ProjectService(database)
    val client = ProjectClient()

    routing {
        post("/projects", {
            tags = listOf("projects")
            description = "Create a project"
            request {
                body<ProjectRequest> {
                    description = "New project properties"
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
            call.respond(HttpStatusCode.Created, id)
            call.response.headers.append("Location", "/projects/${id}")
        }
        get("/projects/{id}", {
            tags = listOf("projects")
            description = "Get a project by ID"
            request {
                pathParameter<Int>("id") { description = "The project ID" }
            }
            response {
                HttpStatusCode.OK to {
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

            val partialProject = service.read(id)
            if (partialProject == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val project = client.resolve(partialProject)
            call.respond(HttpStatusCode.OK, project)
        }
        get("/projects", {
            tags = listOf("projects")
            description = "Get a page of projects"
            request {
                queryParameter<Int?>("page") { description = "The page number" }
                queryParameter<Int?>("size") { description = "The page size" }
                queryParameter<Int?>("user") { description = "User ID to filter by association" }
            }
            response {
                HttpStatusCode.OK to {
                    body<List<Project>> {}
                }
            }
        }) {
            val (page, size) = call.getPageAndSize()
            val user = call.parameters["user"]?.toIntOrNull()
            val partialProjects = service.page(page, size, user)
            val projects = client.resolve(partialProjects)
            call.respond(HttpStatusCode.OK, projects)
        }
        put("/projects/{id}", {
            tags = listOf("projects")
            description = "Update a project by ID"
            request {
                pathParameter<Int>("id") { description = "The project ID" }
                body<ProjectUpdate> {
                    description = "New project properties"
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
            request {
                pathParameter<Int>("id") { description = "The project ID" }
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