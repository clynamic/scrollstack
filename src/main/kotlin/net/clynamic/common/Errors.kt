package net.clynamic.common

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureErrors() {
    install(StatusPages) {
        // Service errors when a record is not found
        exception<NoSuchRecordException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message ?: "Not found")
        }
        // CDN errors when a record has expired
        exception<RecordExpiredException> { call, cause ->
            call.respond(HttpStatusCode.Gone, cause.message ?: "Resource has expired")
        }
        exception<MissingRequiredParameterException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message ?: "Missing required parameter")
        }
    }
}

val Parameters.id: Int
    get() = this["id"]?.toIntOrNull() ?: throw MissingRequiredParameterException("id")

class MissingRequiredParameterException(key: String) :
    IllegalArgumentException("Missing required parameter: $key")

class RecordExpiredException(id: Any?, type: String? = null) :
    NoSuchElementException("${type ?: "Record"} $id has expired")