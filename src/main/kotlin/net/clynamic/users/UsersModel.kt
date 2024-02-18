package net.clynamic.users

import io.swagger.v3.oas.annotations.media.Schema

data class UserRequest(
    @field:Schema(required = true)
    val name: String,
    @field:Schema(required = true)
    val email: String,
    @field:Schema(nullable = true)
    val pronouns: String? = null,
    @field:Schema(nullable = true)
    val bio: String? = null,
    @field:Schema(nullable = true)
    val discord: String? = null,
    @field:Schema(nullable = true)
    val github: String? = null,
)

data class User(
    @field:Schema(required = true)
    val id: Int,
    @field:Schema(required = true)
    val name: String,
    @field:Schema(required = true)
    val email: String,
    @field:Schema(nullable = true)
    val pronouns: String? = null,
    @field:Schema(nullable = true)
    val bio: String? = null,
    @field:Schema(nullable = true)
    val discord: String? = null,
    @field:Schema(nullable = true)
    val github: String? = null,
)

data class UserUpdate(
    @field:Schema(nullable = true)
    val name: String? = null,
    @field:Schema(nullable = true)
    val email: String? = null,
    @field:Schema(nullable = true)
    val pronouns: String? = null,
    @field:Schema(nullable = true)
    val bio: String? = null,
    @field:Schema(nullable = true)
    val discord: String? = null,
    @field:Schema(nullable = true)
    val github: String? = null,
)
