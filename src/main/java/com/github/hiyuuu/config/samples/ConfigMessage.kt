package com.github.hiyuuu.config.samples

import com.github.hiyuuu.config.annotations.Comment
import com.github.hiyuuu.config.annotations.PathMapping
import com.github.hiyuuu.config.annotations.SubConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@SubConfig
@PathMapping("message")
class ConfigMessage(
    @Comment("メッセージ") var message : String
) {

    fun send(player: Player, vararg replaceWith: String = arrayOf()) = player.sendMessage(coloredMessage().replaceWith(*replaceWith))

    fun broadCast(vararg replaceWith: String = arrayOf()) = Bukkit.broadcastMessage(coloredMessage().replaceWith(*replaceWith))

    fun print(vararg replaceWith: String = arrayOf()) = System.out.print(noColoredMessage().replaceWith(*replaceWith))

    fun println(vararg replaceWith: String = arrayOf()) = System.out.println(noColoredMessage().replaceWith(*replaceWith))

    fun warning(vararg replaceWith: String = arrayOf()) = Bukkit.getLogger().warning(coloredMessage().replaceWith(*replaceWith))

    fun info(vararg replaceWith: String = arrayOf()) = Bukkit.getLogger().info(coloredMessage().replaceWith(*replaceWith))

    fun fine(vararg replaceWith: String = arrayOf()) = Bukkit.getLogger().fine(coloredMessage().replaceWith(*replaceWith))

    private fun coloredMessage() = message.replace("&|＆".toRegex(), "§")
    private fun noColoredMessage() = message.replace("[&|＆][A-FIK-O0-9]".toRegex(RegexOption.IGNORE_CASE), "")
    private fun String.replaceWith(vararg replaceWith: String) : String {

        var message = this
        var count = 0
        while (true) {

            val key = replaceWith.getOrNull(count) ?: break
            val value = replaceWith.getOrNull(count + 1) ?: break

            message = message.replace(key, value)
            count += 2
        }

        return message
    }

}