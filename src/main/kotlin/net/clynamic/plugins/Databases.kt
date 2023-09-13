package net.clynamic.plugins

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import org.jetbrains.exposed.sql.Database

val DATABASE_KEY = AttributeKey<Database>("database")

fun Application.configureDatabases() {
    attributes.put(
        DATABASE_KEY,
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = ""
        )
    )
}

interface Service<R, T, U> {
    suspend fun create(request: R): Int
    suspend fun read(id: Int): T?
    suspend fun page(page: Int?, size: Int?): List<T>
    suspend fun update(id: Int, update: U)
    suspend fun delete(id: Int)
}