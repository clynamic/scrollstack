package net.clynamic.contents

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class ContentRequest(
    @field:Schema(required = true)
    val source: String,
    @field:Schema(required = true)
    val mime: String,
    @field:Schema(nullable = true)
    val expiresAt: Instant? = null,
)

data class Content(
    @field:Schema(required = true)
    val id: Int,
    @field:Schema(required = true)
    val source: String,
    @field:Schema(required = true)
    val mime: String,
    @field:Schema(required = true)
    val createdAt: Instant,
    @field:Schema(nullable = true)
    val updatedAt: Instant? = null,
    @field:Schema(nullable = true)
    val expiresAt: Instant? = null,
)

data class ContentUpdate(
    @field:Schema(nullable = true)
    val source: String? = null,
    @field:Schema(nullable = true)
    val mime: String? = null,
    @field:Schema(nullable = true)
    val expiresAt: Instant? = null,
)

/**
 * URL of this content.
 * This is different from the source, as the source is the internal location of the content,
 * while the URL is the external location of the content.
 */
val Content.url: String
    get() = "/cdn/$id"