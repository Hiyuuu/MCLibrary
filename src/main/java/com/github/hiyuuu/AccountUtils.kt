package com.gmail.streamchecker.util

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * UUIDやプレイヤー名を相互に変換できるクラス
 * @author hiyuuu
 */
object AccountUtils {

    /**
     * プレイヤー リンク UUID
     */
    class PlayerLinkUUID {

        private var PlayerData : Player? = null
        private var PlayerNameData : String? = null
        private var UUIDData : UUID? = null

        constructor(Player: Player) {
            PlayerData = Player
            PlayerNameData = Player.name
            UUIDData = Player.uniqueId
        }
        constructor(PlayerName: String, Player: Player? = null, UUID: UUID? = null) {
            PlayerData = Player
            PlayerNameData = PlayerName
            UUIDData = UUID
        }
        constructor(UUID: UUID, Player: Player? = null, PlayerName: String? = null) {
            PlayerData = Player
            PlayerNameData = PlayerName
            UUIDData = UUID
        }

        /**
         * プレイヤー名またはPlayerインスタンスからUUIDを取得します
         */
        fun UUID(CacheValidSeconds: Int = 0) : UUID? {
            if (PlayerData != null) return PlayerData?.uniqueId

            val accountInfo = PlayerNameData?.let { nameToPlayerInfo(it, CacheValidSeconds) }
            if (accountInfo != null) return accountInfo.UUID

            return null
        }

        /**
         * UUIDまたはPlayerインスタンスからUUIDを取得します
         */
        fun PlayerName(CacheValidSeconds: Int = 0) : String? {
            if (PlayerData != null) return PlayerData?.name

            val accountInfo = UUIDData?.let { uuidToPlayerInfo(it, CacheValidSeconds) }
            if (accountInfo != null) return accountInfo.PlayerName

            return null
        }

        /**
         * プレイヤー名またはUUIDからPlayerインスタンスを取得します
         */
        fun Player() : Player? {

            UUIDData?.let { Bukkit.getPlayer(it)?.let { return it } }
            PlayerNameData?.let { Bukkit.getPlayer(it)?.let { return it } }
            return null
        }

        fun OfflinePlayer() : OfflinePlayer? {
            UUIDData?.let {
                return  runCatching { Bukkit.getOfflinePlayer(it) }.getOrNull()
            }
            return null
        }

    }

    /**
     * プレイヤーのアカウント情報を総括するクラスです
     */
    class PlayerInfo(val PlayerName: String, val UUID: UUID, val AccessDate: Date) {
        override fun toString(): String = "NAME: " + PlayerName + " UUID: $UUID at ${SimpleDateFormat("yy/MM/dd HH:mm`ss").format(AccessDate)}"
    }

    //
    //　　　キャッシュ
    //

    private val CachePlayerInfo = ArrayList<PlayerInfo>()


    /**
     * PlayerAccountInfoのデータをキャッシュします
     * @param PlayerInfo
     */
    fun addCache(PlayerInfo: PlayerInfo) {
        val removeArray = CachePlayerInfo.filter { it.UUID == PlayerInfo.UUID }
        CachePlayerInfo.removeAll(removeArray)
        CachePlayerInfo.add(PlayerInfo)
    }

    /**
     * マインクラフトIDを使用してキャッシュを取り出します。
     * @param PlayerName マインクラフトID
     * @return PlayerAccountInfoを返します
     */
    fun getCache(PlayerName: String) : PlayerInfo?
        = CachePlayerInfo.find { it.PlayerName.equals(PlayerName, true) }

    /**
     * マインクラフトIDを使用してキャッシュを取り出します。
     * @param uuid マインクラフトID
     * @return PlayerAccountInfoを返します
     */
    fun getCache(uuid: UUID) : PlayerInfo?
        = CachePlayerInfo.find { it.UUID == uuid }


    /**
     * キャッシュをマインクラフトIDを利用して削除します
     * @param playerName マインクラフトID
     * @return 削除出来たか否か
     */
    fun deleteCache(playerName: String) : Boolean {
        val targetDelete = CachePlayerInfo
                .filter { it.PlayerName.equals(playerName, true) } ?: return false
        CachePlayerInfo.removeAll(targetDelete.toSet())
        return true
    }

    /**
     * キャッシュをUUIDを使用して削除します
     * @param uuid マインクラフトのUUID
     * @return 削除出来たか否か
     */
    fun deleteCache(uuid: UUID) : Boolean {
        val targetDelete = CachePlayerInfo
                .filter { it.UUID == uuid } ?: return false
        CachePlayerInfo.removeAll(targetDelete.toSet())
        return true
    }

    /**
     * キャッシュを全削除します
     */
    fun clearCache() = CachePlayerInfo.clear()

    //
    //  変換API
    //

    /**
     * マインクラフトIDをPlayerAccountInfoクラスで返します
     * @param playerName プレイヤー名
     * @param validCacheSecond キャッシュの有効時間(秒)
     */
    fun nameToPlayerInfo(playerName: String, validCacheSecond: Int = 300): PlayerInfo? {

        // キャッシュ使用
        val cache = getCache(playerName)

        // キャッシュの有効性の検証
        if (cache != null) {
            val accessDate = cache.AccessDate
            val duration = Duration.between(Instant.ofEpochMilli(accessDate.time), Instant.now())
            if (duration.seconds <= validCacheSecond) return cache
        }

        // プレイヤー名 から UUID のリクエスト
        val json = requestURL("https://api.mojang.com/users/profiles/minecraft/${playerName}") ?: return null
        val id = json.asJsonObject.get("id")?.asString ?: return null
        val uuid = runCatching { stringToUUID(id) }.getOrNull() ?: return null

        // データ構造作成
        val playerInfo = PlayerInfo(playerName, uuid, Date())

        // キャッシュ化
        addCache(playerInfo)

        return playerInfo
    }

    /**
     * マインクラフトUUIDをPlayerAccountInfoクラスで返します
     * @param UUID 文字型マインクラフトUUID
     * @param CacheValidseconds キャッシュの有効時間(秒)
     */
    fun uuidToPlayerInfo(stringUUID: String, validCacheSecond: Int = 300): PlayerInfo?
        = uuidToPlayerInfo(stringToUUID(stringUUID), validCacheSecond)

    /**
     * マインクラフトUUIDをPlayerAccountInfoクラスで返します
     * @param uuid マインクラフトUUID
     * @param CacheValidseconds キャッシュの有効時間(秒)
     */
    fun uuidToPlayerInfo(uuid: UUID, validCacheSecond: Int = 300): PlayerInfo? {

        // キャッシュ使用
        val cache = getCache(uuid)

        // キャッシュの有効性の検証
        if (cache != null) {
            val accessDate = cache.AccessDate
            val duration = Duration.between(Instant.ofEpochMilli(accessDate.time), Instant.now())
            if (duration.seconds <= validCacheSecond) return cache
        }

        // UUID から プレイヤー名 のリクエスト
        val json = requestURL("https://sessionserver.mojang.com/session/minecraft/profile/${uuid.toString().replace("-", "")}")
                ?: return null
        val playerName = json.asJsonObject.get("name")?.asString ?: return null

        // データ構造作成
        val playerInfo = PlayerInfo(playerName, uuid, Date())

        // キャッシュ化
        addCache(playerInfo)

        return playerInfo
    }

    /**
     * STRING型のUUIDをUUIDインスタンスで返します。
     * (区切り文字はあってもなくても対応)
     */
    fun stringToUUID(uuid: String) : UUID { var uuid = uuid.replace("-", "")
        uuid = uuid.substring(0..7)+ "-" +
                uuid.substring(8..11) + "-" +
                uuid.substring(12..15) + "-" +
                uuid.substring(16..19) + "-" +
                uuid.substring(20..(uuid.length - 1))
        return runCatching { java.util.UUID.fromString(uuid) }.getOrNull()
            ?: throw IllegalArgumentException("UUIDの形式が正しくありません: ${uuid}")
    }

    /**
     * 指定URLをGETで取得します
     * @param url アクセスするURL
     * @return Json解析したエレメントを返却します
     */
    private fun requestURL(url: String) : JsonElement? {
        val uri = URL(url)
        val openCon = uri.openConnection()
        val httpCon = openCon as HttpURLConnection
        httpCon.apply { this.requestMethod = "GET" }.connect()
        return runCatching {
            val result = BufferedReader(InputStreamReader(httpCon.inputStream, Charsets.UTF_8)).readText()
            return@runCatching JsonParser.parseString(result)
        }.getOrNull()
    }

}