package net.clynamic.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import net.clynamic.projects.ProjectType


class CaseInsensitiveEnumDeserializer : JsonDeserializer<ProjectType>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): ProjectType {
        val valueAsString = jp.text.uppercase()
        return ProjectType.valueOf(valueAsString)
    }
}

class CaseInsensitiveEnumSerializer : JsonSerializer<ProjectType>() {
    override fun serialize(
        value: ProjectType,
        gen: JsonGenerator,
        serializers: SerializerProvider?
    ) {
        gen.writeString(value.name.lowercase())
    }
}
