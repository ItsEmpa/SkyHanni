package at.hannibal2.skyhanni.utils.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class SimpleTypeAdapter<T>(
    private val serializer: JsonWriter.(T) -> Unit,
    private val deserializer: JsonReader.() -> T,
) : TypeAdapter<T>() {
    override fun write(writer: JsonWriter, value: T) = serializer(writer, value)
    override fun read(reader: JsonReader): T = deserializer(reader)
}
