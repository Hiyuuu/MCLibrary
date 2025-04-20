package com.github.hiyuuu.config.impl

import org.bukkit.command.CommandSender

open class Send(var message: String) {

    companion object {

        @JvmStatic
        var prefix = ""
    }

    fun send(sender: CommandSender) {
        sender.sendMessage(prefix.replace("&", "§") + message.replace("&", "§"))
    }

}
