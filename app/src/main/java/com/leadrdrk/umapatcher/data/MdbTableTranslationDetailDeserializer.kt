package com.leadrdrk.umapatcher.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MdbTableTranslationDetailDeserializer : JsonDeserializer<MdbTableTranslationDetail> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MdbTableTranslationDetail {
        val jsonObject = json.asJsonObject

        val filesProp = jsonObject.get("files")
        return MdbTableTranslationDetail(
            name = jsonObject.get("table").asString,
            textColumn = jsonObject.get("field").asString,
            files = filesProp?.asJsonObject?.keySet()?.toList()
                ?: listOf(jsonObject.get("file").asString),
            subdir = jsonObject.get("subdir")?.asBoolean ?: false
        )
    }
}