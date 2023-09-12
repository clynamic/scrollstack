package net.clynamic.plugins

import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getPageAndSize(): Pair<Int?, Int?> {
    val page = this.parameters["page"]?.toIntOrNull()
    val size = this.parameters["size"]?.toIntOrNull()
    return Pair(page, size)
}
