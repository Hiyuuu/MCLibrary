package com.github.hiyuuu.blind.conf.samples

import com.github.hiyuuu.config.annotations.Comment
import com.github.hiyuuu.config.annotations.SubConfig
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player

@SubConfig
class ConfigSound(
    @Comment("音の種類: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html")
    var sound: String = Sound.BLOCK_NOTE_BLOCK_PLING.name,

    @Comment("音の大きさ: 0.5 ~ (2.0)")
    var volume: Double = 1.0,

    @Comment("音の高さ: 0.5 ~ 2.0")
    var pitch: Double = 1.0
) {

    constructor(sound: Sound, volume: Double, pitch: Double) : this(sound.name, volume, pitch)

    fun play(player: Player) = player.playSound(player.location, Sound.valueOf(sound), volume.toFloat(), pitch.toFloat())

    fun playAt(location: Location) = location.world?.playSound(location, Sound.valueOf(sound), volume.toFloat(), pitch.toFloat())

    override fun toString(): String = "ConfigSound{sound=${sound}, volume=${volume}, pitch=${pitch}}"

}