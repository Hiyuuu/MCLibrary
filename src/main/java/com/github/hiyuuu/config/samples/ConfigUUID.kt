package com.github.hiyuuu.config.samples

import com.github.hiyuuu.config.annotations.PathMapping
import com.github.hiyuuu.config.annotations.SubConfig
import org.bukkit.configuration.serialization.ConfigurationSerializable
import java.util.*

@SubConfig
@PathMapping("uuid")
class ConfigUUID(
    var uuid : UUID
) : ConfigurationSerializable { //ConfigSerializer<UUID> {

//    override fun serialize(path: String, value: UUID, config: ConfigUtils) {
//        config.set(path, value.toString())
//    }
//
//    override fun deserialize(path: String, config: ConfigUtils): UUID
//        = UUID.fromString(config.getString(path))

    override fun serialize(): MutableMap<String, Any> = mutableMapOf(Pair("uuid", uuid.toString()))
    constructor(any: Map<String, Any>) : this(UUID.fromString((any["uuid"] as String)))

}