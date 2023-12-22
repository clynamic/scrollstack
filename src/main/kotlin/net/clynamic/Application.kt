package net.clynamic

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.clynamic.plugins.configureAuth
import net.clynamic.plugins.configureCors
import net.clynamic.plugins.configureDatabases
import net.clynamic.plugins.configureEnvironment
import net.clynamic.plugins.configureHttpDocs
import net.clynamic.plugins.configureSerialization
import net.clynamic.plugins.dotenv
import net.clynamic.plugins.projects.configureProjectsRouting
import net.clynamic.plugins.userprojects.configureUserProjectsRouting
import net.clynamic.plugins.users.configureUsersRouting

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
    configureDatabases()
    configureUsersRouting()
    configureProjectsRouting()
    configureUserProjectsRouting()
}
