package net.clynamic.users

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
import net.clynamic.common.DATABASE_KEY
import net.clynamic.common.getPageAndSize

fun Application.configureUsersRouting() {
    val database = attributes[DATABASE_KEY]
    val service = UserService(database)

    routing {
        post("/users", {
            tags = listOf("users")
            description = "Create a user"
            request {
                body<UserRequest> {
                    description = "New user properties"
                }
            }
            response {
                HttpStatusCode.Created to {
                    body<Int> {
                        description = "The new user ID"
                    }
                }
            }
        }) {
            val user = call.receive<UserRequest>()
            val id = service.create(user)
            call.response.headers.append("Location", "/users/${id}")
            call.respond(HttpStatusCode.Created, id)
        }
        get("/users/{id}", {
            tags = listOf("users")
            description = "Get a user by ID"
            request {
                pathParameter<Int>("id") { description = "The user ID" }
            }
            response {
                HttpStatusCode.OK to {
                    body<User> {}
                }
                HttpStatusCode.NotFound to {
                    description = "User not found"
                }
            }
        }) {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = service.read(id)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }
        get("/users",
            {
                tags = listOf("users")
                description = "Get a page of users"
                request {
                    queryParameter<Int?>("page") { description = "The page number" }
                    queryParameter<Int?>("size") { description = "The page size" }
                    queryParameter<Int?>("project") {
                        description = "Project ID to filter by association"
                    }
                }
                response {
                    HttpStatusCode.OK to {
                        body<List<User>> {}
                    }
                }
            }) {
            val (page, size) = call.getPageAndSize()
            val project = call.parameters["project"]?.toIntOrNull()
            val users = service.page(page, size, project)
            call.respond(HttpStatusCode.OK, users)
        }
        put("/users/{id}", {
            tags = listOf("users")
            description = "Update a user"
            request {
                pathParameter<Int>("id") { description = "The user ID" }
                body<UserUpdate> {
                    description = "Changed user properties"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User updated"
                }
            }
        }) {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val request = call.receive<UserUpdate>()
            service.update(id, request)
            call.respond(HttpStatusCode.OK)
        }
        delete("/users/{id}", {
            tags = listOf("users")
            description = "Delete a user"
            request {
                pathParameter<Int>("id") { description = "The user ID" }
            }
            response {
                HttpStatusCode.OK to {
                    description = "User deleted"
                }
            }
        }) {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            service.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}