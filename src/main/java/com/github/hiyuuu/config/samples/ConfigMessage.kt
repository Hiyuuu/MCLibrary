package com.github.hiyuuu.config.samples

import com.github.hiyuuu.config.annotations.Comment
import com.github.hiyuuu.config.annotations.PathMapping
import com.github.hiyuuu.config.annotations.SubConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@SubConfig
@PathMapping("message")
class ConfigMessage(
    @Comment("メッセージ")
    var message : String
) {

    private fun coloredMessage() = message.replace("&|＆".toRegex(), "§")
    private fun noColoredMessage() = message.replace("[&|＆][A-FIK-O0-9]".toRegex(RegexOption.IGNORE_CASE), "")

    fun String.send(player: Player) = player.sendMessage(this)

    fun send(player: Player, vararg replaceWith: String) {
        player.sendMessage(coloredMessage())
    }

    fun broadCast() = Bukkit.broadcastMessage(coloredMessage())

    fun print() = print(noColoredMessage())

    fun println() = print(noColoredMessage())

    fun warning() = Bukkit.getLogger().warning(coloredMessage())

    fun info() = Bukkit.getLogger().info(coloredMessage())

    fun fine() = Bukkit.getLogger().fine(coloredMessage())

    fun config() = Bukkit.getLogger().config(coloredMessage())

}