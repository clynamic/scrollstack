package net.clynamic.plugins.users

data class UserRequest(
    val name: String,
    val email: String,
    val pronouns: String? = null,
    val bio: String? = null,
    val discord: String? = null,
    val github: String? = null,
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val pronouns: String? = null,
    val bio: String? = null,
    val discord: String? = null,
    val github: String? = null,
)

data class UserUpdate(
    val name: String? = null,
    val email: String? = null,
    val pronouns: String? = null,
    val bio: String? = null,
    val discord: String? = null,
    val github: String? = null,
)
