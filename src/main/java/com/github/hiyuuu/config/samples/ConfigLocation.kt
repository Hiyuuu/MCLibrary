package com.github.hiyuuu.config.samples

import com.github.hiyuuu.config.annotations.Comment
import com.github.hiyuuu.config.annotations.ConfigSpace
import com.github.hiyuuu.config.annotations.SubConfig
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

@SubConfig
@ConfigSpace(1, 0)
class ConfigLocation(
    @Comment("ワールド名") val world : String,
    @Comment("X座標") val x : Double,
    @Comment("Y座標") val y : Double,
    @Comment("Z座標") val z : Double,
    @Comment("横回転軸") val yaw : Double,
    @Comment("縦回転軸") val pitch : Double
) {

    constructor(location: Location) : this(location.world!!.name, location.x, location.y, location.z, location.yaw.toDouble(), location.pitch.toDouble())

    fun location() : Location = Location(Bukkit.getWorld(world), x, y, z, yaw.toFloat(), pitch.toFloat())

    fun teleport(entity: Entity) = entity.teleport(location())

    override fun toString(): String = "ConfigLocation{world=${world}, x=${x}, y=${y}, z=${z}, yaw=${yaw}, pitch=${pitch}}"

}