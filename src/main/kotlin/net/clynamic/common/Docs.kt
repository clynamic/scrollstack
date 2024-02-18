package net.clynamic.common

import io.github.smiley4.ktorswaggerui.SwaggerUI
import io.github.smiley4.ktorswaggerui.dsl.AuthScheme
import io.github.smiley4.ktorswaggerui.dsl.AuthType
import io.ktor.server.application.Application
import io.ktor.server.application.install

fun Application.configureHttpDocs() {
    install(SwaggerUI) {
        swagger {
            forwardRoot = true
        }
        info {
            title = AppMeta.name
            version = AppMeta.version
            description = AppMeta.description
        }
        securityScheme("bearer") {
            type = AuthType.HTTP
            scheme = AuthScheme.BEARER
        }
    }
}