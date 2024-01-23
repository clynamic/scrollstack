package net.clynamic.projects

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
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
    val id: Int,
    val name: String,
    val source: String,
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
    val name: String,
    val source: String,
    val type: ProjectType,
)

/**
 * A project update.
 * This is used to update a project.
 * Depending on the type, the source will be interpreted differently.
 */
data class ProjectUpdate(
    val name: String?,
    val source: String?,
    val type: ProjectType?,
)

/**
 * A full project.
 * The source field should be a full URL to the project.
 */
data class Project(
    val id: Int,
    val name: String,
    val source: String,
    val description: String?,
    val updated: Instant?,
    val website: String?,
    val language: String?,
    val banner: String?,
    val stars: Int?,
)