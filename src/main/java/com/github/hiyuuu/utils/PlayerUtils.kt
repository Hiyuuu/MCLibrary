package com.github.hiyuuu.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * ハッシュマップを使用上で便利するクラス
 */
interface PlayerUtils {

    /**
     * プレイヤーインスタンスを最新状態へ変更する
     * @see プレイヤーがオフラインの場合は、元のインスタンスを返します
     */
    private fun Player.new() : Player = Bukkit.getPlayer(this.uniqueId) ?: this

}