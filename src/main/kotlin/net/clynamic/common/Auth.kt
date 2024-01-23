package net.clynamic.common

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.origin
import org.slf4j.LoggerFactory

fun Application.configureAuth() {
    val logger = LoggerFactory.getLogger("AuthRouting")

    val adminToken = attributes[ENVIRONMENT_KEY].get("ADMIN_TOKEN", null)
    if (adminToken == null) {
        // If no admin token is set, allow all requests
        logger.warn("No admin token set, allowing all requests")
    }

    install(Authentication) {
        bearer {
            authenticate { credential ->
                val call = this.request.call
                if (credential.token == adminToken) {
                    logger.info("Admin request from ${call.request.origin.remoteHost}")
                    return@authenticate UserIdPrincipal("admin")
                } else {
                    logger.warn("Unauthorized request from ${call.request.origin.remoteHost}")
                    null
                }
            }
        }
    }
}