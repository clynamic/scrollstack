package net.clynamic

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.clynamic.common.configureAuth
import net.clynamic.common.configureCors
import net.clynamic.common.configureDatabases
import net.clynamic.common.configureEnvironment
import net.clynamic.common.configureErrors
import net.clynamic.common.configureHttpDocs
import net.clynamic.common.configureSerialization
import net.clynamic.common.dotenv
import net.clynamic.contents.configureContentsRouting
import net.clynamic.projects.configureProjectsRouting
import net.clynamic.userprojects.configureUserProjectsRouting
import net.clynamic.users.configureUsersRouting

fun main() {
    val port = dotenv["PORT"]?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureEnvironment()
    configureSerialization()
    configureHttpDocs()
    configureAuth()
    configureCors()
    configureErrors()
    configureDatabases()
    configureContentsRouting()
    configureUsersRouting()
    configureProjectsRouting()
    configureUserProjectsRouting()
}
