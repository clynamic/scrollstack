package net.clynamic.userprojects

import io.swagger.v3.oas.annotations.media.Schema

data class UserProjectRelation(
    @field:Schema(required = true)
    val userId: Int,
    @field:Schema(required = true)
    val projectId: Int
)