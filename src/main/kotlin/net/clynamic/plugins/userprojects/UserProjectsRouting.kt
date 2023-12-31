package net.clynamic.plugins.userprojects

import io.github.smiley4.ktorswaggerui.dsl.delete
import io.github.smiley4.ktorswaggerui.dsl.get
import io.github.smiley4.ktorswaggerui.dsl.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import net.clynamic.plugins.DATABASE_KEY

fun Application.configureUserProjectsRouting() {
    val database = attributes[DATABASE_KEY]
    val service = UserProjectsService(database)

    routing {
        post("/user-projects", {
            tags = listOf("user-projects")
            description = "Associate a user with a project"
            request {
                body<UserProjectRelation> {
                    description = "User and project IDs"
                }
            }
            response {
                HttpStatusCode.Created to {
                    description = "User and project associated"
                }
            }
        }) {
            val relation = call.receive<UserProjectRelation>()
            val (userId, projectId) = relation
            service.associate(userId, projectId)
            call.response.headers.append(
                "Location",
                "/user-projects/${userId}/${projectId}"
            )
            call.respond(HttpStatusCode.Created)
        }
        get("/user-projects/{userId}/{projectId}", {
            tags = listOf("user-projects")
            description = "Check if a user is associated with a project"
            request {
                pathParameter<Int>("userId") { description = "The user ID" }
                pathParameter<Int>("projectId") { description = "The project ID" }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User is associated with project"
                }
                HttpStatusCode.NotFound to {
                    description = "User is not associated with project"
                }
            }
        }) {
            val userId = call.parameters["userId"]?.toIntOrNull()
            val projectId = call.parameters["projectId"]?.toIntOrNull()
            if (userId == null || projectId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val has = service.has(userId, projectId)
            if (has) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        delete("/user-projects/{userId}/{projectId}", {
            tags = listOf("user-projects")
            description = "Dissociate a user from a project"
            response {
                HttpStatusCode.NoContent to {
                    description = "User and project dissociated"
                }
            }
        }) {
            val userId = call.parameters["userId"]?.toIntOrNull()
            val projectId = call.parameters["projectId"]?.toIntOrNull()
            if (userId == null || projectId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@delete
            }

            service.dissociate(userId, projectId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}