package com.github.hiyuuu.config

interface ConfigSerializer<T> {

    fun serialize(path: String, value: T, config: ConfigUtils)

    fun deserialize(path: String, config: ConfigUtils): T

}