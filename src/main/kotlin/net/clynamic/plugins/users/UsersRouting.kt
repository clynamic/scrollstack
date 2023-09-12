package net.clynamic.plugins.users

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

fun Application.configureUsersRouting() {
    val database = attributes[DATABASE_KEY]
    val userService = UserService(database)

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
                    body<Int> {}
                }
            }
        }) {
            val user = call.receive<UserRequest>()
            val id = userService.create(user)
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
            val user = userService.read(id)
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
                }
                response {
                    HttpStatusCode.OK to {
                        body<List<User>> {}
                    }
                }
            }) {
            val (page, size) = call.getPageAndSize()
            val users = userService.page(page, size)
            call.respond(HttpStatusCode.OK, users)
        }
        put("/users/{id}", {
            tags = listOf("users")
            description = "Update a user"
            request {
                pathParameter<Int>("id") { description = "The user ID" }
                body<UserRequest> {
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
            val request = call.receive<UserRequest>()
            userService.update(id, request)
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
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}