package net.clynamic.common

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import org.slf4j.LoggerFactory

fun Application.configureCors() {
    val logger = LoggerFactory.getLogger("Cors")

    val hostUrls = attributes[ENVIRONMENT_KEY].get("HOST_URLS", null)

    install(CORS) {
        hostUrls?.split(",")?.map { it.trim() }?.forEach { hostUrl ->
            if (hostUrl.isNotEmpty()) {
                logger.info("Allowing requests from $hostUrl")
                allowHost(hostUrl, listOf("http", "https"))
            }
        }

        allowOrigins {
            it.matches(Regex("^(https?://)?(localhost|0\\.0\\.0\\.0|(127\\.\\d+\\.\\d+\\.\\d+))(:\\d+)?(/.*)?$"))
        }
        allowSameOrigin = true
    }
}
