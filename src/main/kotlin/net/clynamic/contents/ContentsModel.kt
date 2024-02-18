package net.clynamic.contents

import java.time.Instant

data class ContentRequest(
    val source: String,
    val mime: String,
    val expiresAt: Instant? = null,
)

data class Content(
    val id: Int,
    val source: String,
    val mime: String,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val expiresAt: Instant? = null,
)

data class ContentUpdate(
    val source: String? = null,
    val mime: String? = null,
    val expiresAt: Instant? = null,
)

/**
 * URL of this content.
 * This is different from the source, as the source is the internal location of the content,
 * while the URL is the external location of the content.
 */
val Content.url: String
    get() = "/cdn/$id"