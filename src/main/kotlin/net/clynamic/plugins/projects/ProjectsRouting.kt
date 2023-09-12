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
    val projectService = ProjectService(database)

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
                    body<Int> {}
                }
            }
        }) {
            val project = call.receive<ProjectRequest>()
            val id = projectService.create(project)
            call.respond(HttpStatusCode.Created, id)
        }
        get("/projects/{id}", {
            tags = listOf("projects")
            description = "Get a project by ID"
            request {
                pathParameter<Int>("id") { description = "The project ID" }
            }
            response {
                HttpStatusCode.OK to {
                    body<PartialProject> {}
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

            val project = projectService.read(id)
            if (project == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(HttpStatusCode.OK, project)
        }
        get("/projects", {
            tags = listOf("projects")
            description = "Get a page of projects"
            request {
                queryParameter<Int?>("page") { description = "The page number" }
                queryParameter<Int?>("size") { description = "The page size" }
            }
            response {
                HttpStatusCode.OK to {
                    body<List<PartialProject>> {}
                }
            }
        }) {
            val (page, size) = call.getPageAndSize()
            val projects = projectService.page(page, size)
            call.respond(HttpStatusCode.OK, projects)
        }
        put("/projects/{id}", {
            tags = listOf("projects")
            description = "Update a project by ID"
            request {
                pathParameter<Int>("id") { description = "The project ID" }
                body<ProjectRequest> {
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

            val project = call.receive<ProjectRequest>()
            projectService.update(id, project)
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

            projectService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}