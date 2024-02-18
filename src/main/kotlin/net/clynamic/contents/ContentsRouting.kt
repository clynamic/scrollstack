package net.clynamic.contents

import io.github.smiley4.ktorswaggerui.dsl.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import net.clynamic.common.DATABASE_KEY
import net.clynamic.common.RecordExpiredException
import java.time.Instant

fun Application.configureContentsRouting() {
    val database = attributes[DATABASE_KEY]
    val service = ContentsService(database)
    val client = ContentsClient()

    routing {
        get("/cdn/{id}", {
            tags = listOf("contents")
            description = "Get a byte stream of a content by ID"
            request {
                pathParameter<Int>("id") { description = "The content ID" }
            }
            response {
                HttpStatusCode.OK to {
                    description = "The content byte stream"
                    body<ByteArray>()
                }
                HttpStatusCode.NotFound to {
                    description = "Content not found"
                }
            }
        }) {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val content = service.read(id)
            if (content.expiresAt != null && content.expiresAt < Instant.now())
                throw RecordExpiredException(id, "Content")
            val stream = client.resolve(content.source)
            call.response.headers.append(HttpHeaders.ContentType, content.mime)
            call.respond(HttpStatusCode.OK, stream)
        }
    }
}