package net.clynamic.plugins.projects

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.util.Locale

class CaseInsensitiveEnumDeserializer : JsonDeserializer<ProjectType>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): ProjectType {
        val valueAsString = jp.text.uppercase(Locale.getDefault())
        return ProjectType.valueOf(valueAsString)
    }
}

class CaseInsensitiveEnumSerializer : JsonSerializer<ProjectType>() {
    override fun serialize(
        value: ProjectType,
        gen: JsonGenerator,
        serializers: SerializerProvider?
    ) {
        gen.writeString(value.name.lowercase(Locale.getDefault()))
    }
}

@JsonDeserialize(using = CaseInsensitiveEnumDeserializer::class)
@JsonSerialize(using = CaseInsensitiveEnumSerializer::class)
enum class ProjectType {
    REMOTE_GITHUB,
    GITHUB
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RemoteGithubProject::class, name = "remote_github"),
    JsonSubTypes.Type(value = GithubProject::class, name = "github")
)
sealed class PartialProject {
    abstract val id: Int
    abstract val name: String
    abstract val type: ProjectType
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RemoteGithubProjectRequest::class, name = "remote_github")
)
sealed interface ProjectRequest {
    val name: String
    val type: ProjectType
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RemoteGithubProjectUpdate::class, name = "remote_github")
)
sealed interface ProjectUpdate {
    val type: ProjectType
}

data class RemoteGithubProject(
    override val id: Int,
    override val name: String,
    val owner: String,
    val repo: String,
) : PartialProject() {
    override val type = ProjectType.REMOTE_GITHUB
}

data class RemoteGithubProjectRequest(
    override val name: String,
    val owner: String,
    val repo: String,
) : ProjectRequest {
    override val type = ProjectType.REMOTE_GITHUB
}

data class RemoteGithubProjectUpdate(
    val owner: String?,
    val repo: String?,
) : ProjectUpdate {
    override val type: ProjectType = ProjectType.REMOTE_GITHUB
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GithubProject::class, name = "github")
)
sealed class Project : PartialProject()

data class GithubProject(
    override val id: Int,
    override val name: String,
    val owner: String,
    val repo: String,
    val description: String?,
    val stars: Int,
    val lastCommit: String?,
    val homepage: String?,
    val language: String?,
    val banner: String?
) : Project() {
    override val type = ProjectType.GITHUB
}
