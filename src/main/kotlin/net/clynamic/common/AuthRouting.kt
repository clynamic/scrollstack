package net.clynamic.common

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

fun Application.configureAuth() {
    val logger = LoggerFactory.getLogger("AuthRouting")

    val adminToken = attributes[ENVIRONMENT_KEY].get("ADMIN_TOKEN", null)
    if (adminToken == null) {
        // If no admin token is set, allow all requests
        logger.warn("No admin token set, allowing all requests")
    }

    intercept(Plugins) {
        if (adminToken == null) {
            return@intercept
        }
        if (call.request.httpMethod != HttpMethod.Get) {
            val providedToken = call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (providedToken == null || providedToken != adminToken) {
                logger.warn("Unauthorized request from ${call.request.origin.remoteHost}")
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                finish()
            }
        }
    }
}