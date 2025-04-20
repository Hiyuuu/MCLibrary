package com.github.hiyuuu.config.impl

interface Replacer {

    fun String.toSend(vararg replace: String) : Send {

        var message = this
        var count = 0
        while (true) {

            val key = replace.getOrNull(count) ?: break
            val value = replace.getOrNull(count + 1) ?: break

            message = message.replace(key, value)
            count += 2
        }

        return Send(message)
    }

    fun String.toSend() : Send = toSend("")

}
