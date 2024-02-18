package net.clynamic.projects

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import net.clynamic.common.CaseInsensitiveEnumDeserializer
import net.clynamic.common.CaseInsensitiveEnumSerializer
import java.time.Instant

/**
 * The type of project.
 * This indicates how the project should be resolved.
 */
@JsonDeserialize(using = CaseInsensitiveEnumDeserializer::class)
@JsonSerialize(using = CaseInsensitiveEnumSerializer::class)
enum class ProjectType {
    /**
     * A project hosted on GitHub.
     * Its source should be in the format `owner/repo`.
     */
    GITHUB
}

/**
 * A project source.
 * This is used to resolve a project.
 * Depending on the type, the source will be interpreted differently.
 */
data class ProjectSource(
    @field:Schema(required = true)
    val id: Int,
    @field:Schema(required = true)
    val name: String,
    @field:Schema(required = true)
    val source: String,
    @field:Schema(required = true)
    val type: ProjectType,
)


fun ProjectSource.getOwnerAndRepo(): Pair<String, String> {
    if (this.type != ProjectType.GITHUB) {
        throw IllegalArgumentException("Cannot get owner and repo for non-github project")
    }
    val regex = Regex("^([a-zA-Z0-9-]+)/([a-zA-Z0-9-]+)$")
    val match = regex.find(this.source) ?: throw IllegalArgumentException("Invalid source")
    val (owner, repo) = match.destructured
    return Pair(owner, repo)
}

/**
 * A project request.
 * This is used to create a project.
 * Depending on the type, the source will be interpreted differently.
 */
data class ProjectRequest(
    @field:Schema(required = true)
    val name: String,
    @field:Schema(required = true)
    val source: String,
    @field:Schema(required = true)
    val type: ProjectType,
)

/**
 * A project update.
 * This is used to update a project.
 * Depending on the type, the source will be interpreted differently.
 */
data class ProjectUpdate(
    @field:Schema(nullable = true)
    val name: String?,
    @field:Schema(nullable = true)
    val source: String?,
    // making enums nullable is illegal and breaks the swagger schema generation
    // @field:Schema(nullable = true)
    val type: ProjectType?,
)

/**
 * A full project.
 * The source field should be a full URL to the project.
 */
data class Project(
    @field:Schema(required = true)
    val id: Int,
    @field:Schema(required = true)
    val name: String,
    @field:Schema(required = true)
    val source: String,
    @field:Schema(nullable = true)
    val description: String?,
    @field:Schema(nullable = true)
    val updated: Instant?,
    @field:Schema(nullable = true)
    val website: String?,
    @field:Schema(nullable = true)
    val language: String?,
    @field:Schema(nullable = true)
    val banner: String?,
    @field:Schema(nullable = true)
    val stars: Int?,
)