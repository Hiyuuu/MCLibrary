package com.github.hiyuuu.config

import com.github.hiyuuu.config.annotations.*
import com.github.hiyuuu.config.events.ConfigReloadEvent
import com.github.hiyuuu.config.events.ConfigSaveDefaultEvent
import com.github.hiyuuu.config.events.ConfigSaveEvent
import com.github.hiyuuu.config.events.ConfigSetEvent
import com.github.hiyuuu.config.samples.ConfigLocation
import com.github.hiyuuu.config.samples.ConfigMessage
import com.github.hiyuuu.config.samples.ConfigSound
import com.github.hiyuuu.config.samples.ConfigUUID
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet


/**
 * 自動再読み込みに対応したコンフィグユーティリティ
 */
class ConfigUtils(private var plugin: Plugin) : YamlConfiguration(), Listener {

    companion object {
        private val serializers = ArrayList<Pair<Class<*>, ConfigSerializer<*>>>()
    }

    /**
     * ファイルを取得
     * @return File
     */
    lateinit var filePath: String
    var player : Player? = null
    var file = File(plugin.dataFolder.parentFile, "config.yml")
    var isLoaded = false
    var isAutoReload: Boolean = true
    var isLoadDefaultSection = true
    var reloadListener : BukkitTask? = null
    var savedClass : Any? = null
    var classCheckInterval = 100
    var fileModifiedHistory = 0L
    var fileReloadInterval = 20

    constructor(plugin: Plugin, player: Player, filePath: String, isAutoReload: Boolean = true, isLoadDefaultSection: Boolean = true) : this(plugin) {
        this.plugin = plugin
        this.player = player
        this.filePath = filePath
        this.isAutoReload = isAutoReload
        this.isLoadDefaultSection = isLoadDefaultSection
        this.initialize()
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    constructor(plugin: Plugin, filePath: String, isAutoReload: Boolean = true, isLoadDefaultSection: Boolean = true) : this(plugin) {
        this.plugin = plugin
        this.filePath = filePath
        this.isAutoReload = isAutoReload
        this.isLoadDefaultSection = isLoadDefaultSection
        this.initialize()
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    constructor(plugin: Plugin, filePath: String, classInstance: Any, isAutoReload: Boolean, isLoadDefaultSection: Boolean = false) : this(plugin) {
        this.plugin = plugin
        this.filePath = filePath
        this.isAutoReload = isAutoReload
        this.isLoadDefaultSection = isLoadDefaultSection
        this.initialize()
        this.registerClass(classInstance)
        this.classMonitor()

        ConfigurationSerialization.registerClass(ConfigUUID::class.java, "ConfigUUID")
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /**
     * コンフィグユーティリティ
     * @param plugin プラグイン
     * @param filePath 読み込みコンフィグファイルを指定します。
     * / 又は \ 等を含まない場合、プラグインのデータディレクトリ内へ自動的にスコープします。
     * @param isAutoReload 外部からファイルをエディター等で保存した場合に、即座に自動読み込みを行うか否か
     */
    private fun initialize() {

        // ファイルパスを生成
        val path = (plugin
            .dataFolder
            .absolutePath
                + File.separator
                + filePath.replace("\\|/".toRegex(), File.separator))

        // ファイルを変数へ代入
        file = File(path)

        // デフォルトコンフィグの配置
        saveDefaultConfig()

        // 存在しない場合、ファイルを初期化
        if (!file.exists()) {
            file.getParentFile().mkdirs()
            file.createNewFile()
        }

        // コンフィグをロード
        try {
            this.load(file)
        } catch (e: java.lang.Exception) {
            println("$filePath ファイルのロードに失敗しました。原因: $e")
            return
        }

        // デフォルトセクションの設定
        if (isLoadDefaultSection) saveDefaultSection()

        // 自動リロード用ハンドル
        val configUtils = this
        reloadListener = object : BukkitRunnable() { override fun run() {

            // 自動リロード
            if (!isAutoReload) return

            val lastModified: Long = file.lastModified()
            // Bukkit.broadcastMessage("${fileModifiedHistory} vs ${lastModified} (${configUtils.filePath})")
            if (fileModifiedHistory != lastModified) {
                try {
                    reloadConfig()
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getPluginManager().callEvent(ConfigReloadEvent(configUtils))
                    })
                    resetFileModifiedHistory()
                    // Bukkit.broadcastMessage("RELOADED! ${fileModifiedHistory} vs ${lastModified} (${configUtils.filePath})")
                } catch (ignored: IOException) {
                } catch (ignored: InvalidConfigurationException) {}
            }
        }}.runTaskTimerAsynchronously(plugin, 0L, fileReloadInterval.toLong())

        // ロード完了フラグ
        isLoaded = true
    }


    override fun set(path: String, value: Any?) {
        super.set(path, value)
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getPluginManager().callEvent(ConfigSetEvent(this))
        })
    }

    /**
     * 変更内容を保存します
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveConfig(): ConfigUtils {
        this.save(file)
        val configUtils = this
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.getPluginManager().callEvent(ConfigSaveEvent(configUtils))
        })
        return this
    }

    /**
     * 自動リロード用処理を実装した、Resourcesのコンフィグファイル出力メソッド
     */
    fun saveDefaultConfig() {
        if (file.exists()) return
        try {
            plugin.saveResource(file.name, false)
            resetFileModifiedHistory()
            Bukkit.getScheduler().runTask(plugin, Runnable {
                Bukkit.getPluginManager().callEvent(ConfigSaveDefaultEvent(this))
            })
        } catch (ignored: IllegalArgumentException) {
        }
    }

    /**
     * 設定されていないセクションを新たに追加
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveDefaultSection() {

        // resourcesのファイル読み取り
        val resource = plugin.getResource(file.name) ?: return

        // YamlConfiguration を生成
        val isr = InputStreamReader(resource, StandardCharsets.UTF_8)
        val yamlConf = loadConfiguration(isr)
        val keys = yamlConf.getKeys(true)

        // 全キーを処理
        keys.forEach { k ->
            val comments = yamlConf.getComments(k)
            val inlineComments = yamlConf.getInlineComments(k)
            val obj = yamlConf[k]!!

            // resources ファイルとの差分を抽出
            if (!isConfigurationSection(k) && !isSet(k)) {
                this.setComments(k, comments)
                this[k] = obj
                this.setInlineComments(k, inlineComments)
            }
        }

        // 保存
        this.saveConfig()
    }

    /**
     * 自動リロード用処理を実装したコンフィグ再読み込みメソッド
     *
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    @Throws(IOException::class, InvalidConfigurationException::class)
    fun reloadConfig() {
        this.load(file)
        resetFileModifiedHistory()
    }

    /**
     * 自動リロード用処理を実装した保存メソッド
     * @param file
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun save(file: File) {
        super.save(file)
        resetFileModifiedHistory()
    }

    /**
     * 自動リロード用処理を実装した保存メソッド
     * @param file
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun save(file: String) {
        super.save(file)
        resetFileModifiedHistory()
    }

    /**
     * ファイルロードメソッドの実装
     * @param file File to load from.
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    @Throws(IOException::class, InvalidConfigurationException::class)
    override fun load(file: File) {
        // コンフィグファイルを設定

        resetFileModifiedHistory()
        this.file = file

        // コンフィグをロード
        try {
            super.load(this.file)
        } catch (e: Exception) {
            println(file.name + " ファイルのロードに失敗しました。原因: " + e)
        }
    }

    /**
     * ファイルロードメソッドの実装
     * @param filePath File to load from.
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    @Throws(IOException::class, InvalidConfigurationException::class)
    override fun load(filePath: String) {
        // ファイルパスを生成

        val path = (plugin
            .dataFolder
            .absolutePath
                + File.separator
                + filePath.replace("\\|/".toRegex(), File.separator))

        // ファイルを変数へ代入
        val file = File(path)

        // 読み込み
        this.load(file)
    }

    /**
     * ファイル更新履歴をリセット
     */
    fun resetFileModifiedHistory() {
        fileModifiedHistory = file.lastModified()
    }

    /**
     * リスナーを停止
     */
    fun destroyListener() = reloadListener?.cancel()

    @EventHandler
    private fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        if (player == null) return
        if (player?.uniqueId == event.player.uniqueId) destroyListener()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPluginDisableEvent(event: PluginDisableEvent) {
        if (event.plugin != plugin) return
        if (savedClass != null) syncConfigBetweenClass(savedClass, true)
    }

    @EventHandler
    private fun onConfigReloadEvent(event: ConfigReloadEvent) {

        // 自動リロード
        if (!isAutoReload) return

        // クラスが登録されていない場合は終了
        if (savedClass == null) return

        syncConfigBetweenClass(savedClass)
        replaceSpaces(this)
    }

    private fun classMonitor() {
        object : BukkitRunnable() { override fun run() {

            if (savedClass != null) syncConfigBetweenClass(savedClass, true)
        }}.runTaskTimerAsynchronously(plugin, 0, classCheckInterval.toLong())
    }

    /**
     * 監視するクラスを登録
     */
    fun <T> registerClass(anyClass: T) : T? {
        val result = syncConfigBetweenClass(anyClass) ?: return null
        replaceSpaces(this)
        savedClass = result
        return result
    }

    fun <T> registerSerializer(serializer: ConfigSerializer<*>) : Boolean {
        val type = serializer::class.java.componentType
        if (serializers.any { it.first == type }) return false
        return serializers.add(Pair(type, serializer))
    }

    fun getSerializer(clazz: Class<*>) : ConfigSerializer<*>? = serializers.find { it.first == clazz }?.second

    /**
     * コンフィグ <-> クラス 間の変換
     */
    fun <T> syncConfigBetweenClass(anyClass: T, isReverse: Boolean = false) : T? {

        var isSaveConfig = false
        val conf = this@ConfigUtils

        // スーパークラスの情報を取得
        val anyClazz = (anyClass ?: return null)::class.java
        val classes = arrayListOf(Triple<String, Any, Field?>("", anyClass as Any, null))

        // コンフィグスペース
        val configSpace = runCatching { anyClazz.getAnnotation(ConfigSpace::class.java) }.getOrNull()

        // ヘッダー
        val headerComment = runCatching { anyClazz.getAnnotation(HeaderComment::class.java) }.getOrNull()
        val headerSpaceSize = headerComment?.spaceSize ?: configSpace?.classSpaceSize ?: 2
        val headerSpaceList = (1..headerSpaceSize).map { "#SPACE#" }.toTypedArray()
        headerComment?.run { this@ConfigUtils.options().setHeader(listOf(*headerSpaceList, *this.message)) }

        // フッター
        val footerComment = runCatching { anyClazz.getAnnotation(FooterComment::class.java) }.getOrNull()
        val footerSpaceSize = footerComment?.spaceSize ?: configSpace?.classSpaceSize ?: 1
        val footerSpaceList = (1..footerSpaceSize).map { "#SPACE#" }.toTypedArray()
        footerComment?.run { this@ConfigUtils.options().setFooter(listOf(*footerSpaceList, *this.message)) }

        val sectionNameCache = HashMap<String, String>()
        val deletePathQueue = ArrayList<String>()
        while (classes.size > 0) {

            // クラスの情報復元
            val lastKeyClass = classes.removeLastOrNull() ?: break
            var classSection = lastKeyClass.first
            val classInstance = lastKeyClass.second
            val parentField = lastKeyClass.third
            val clazz = classInstance::class.java

            // クラスのアノテーションを取得
            val classSectionName = runCatching { clazz.getAnnotation(SectionName::class.java) }.getOrNull()
            val classComment = runCatching { clazz.getAnnotation(Comment::class.java) }.getOrNull()
            val classInlineComment = runCatching { clazz.getAnnotation(InlineComment::class.java) }.getOrNull()
            val classSpace = runCatching { clazz.getAnnotation(Space::class.java) }.getOrNull()
            val classCopy = runCatching { clazz.getAnnotation(Copy::class.java) }.getOrNull()
            val classDelete = runCatching { clazz.getAnnotation(Delete::class.java) }.getOrNull()
            val classPathMapping = runCatching { clazz.getAnnotation(PathMapping::class.java) }.getOrNull()

            // ペアレントクラスのアノテーションを取得
            val parentComment = runCatching { parentField?.getAnnotation(Comment::class.java) }.getOrNull()
            val parentInlineComment = runCatching { parentField?.getAnnotation(InlineComment::class.java) }.getOrNull()
            val parentSpace = runCatching { parentField?.getAnnotation(Space::class.java) }.getOrNull()
            val parentCopy = runCatching { parentField?.getAnnotation(Copy::class.java) }.getOrNull()
            val parentPathMapping = runCatching { parentField?.getAnnotation(PathMapping::class.java) }.getOrNull()

            // クラスセクションパスを改ざん (@SectionName)
            if (classSectionName != null) classSection = classSectionName.path

            // 削除キュー追加
            if (classDelete != null) {
                deletePathQueue.add(classSection)
                continue
            }

            // フィールドコピー (オブジェクトコピー)
            if (parentCopy != null) {
                val fromPath = sectionNameCache[parentCopy.fromPath] ?: parentCopy.fromPath

                val getObj = conf.get(fromPath)
                val getComments = conf.getComments(fromPath)
                val getInlineComments = conf.getInlineComments(fromPath)

                val configSpaceSize = parentSpace?.size ?: configSpace?.fieldSpaceSize ?: 1
                val spaceList = (1..configSpaceSize).map { "#SPACE#" }.toTypedArray()

                if (!conf.isConfigurationSection(classSection)) conf.set(classSection, getObj)
                if (conf.getComments(classSection).filterNotNull().none { it != "#SPACE#" }) conf.setComments(
                    classSection,
                    listOf(*spaceList, *(parentComment?.message ?: getComments.toTypedArray()))
                )
//                if (conf.getInlineComments(classSection).filterNotNull().none { it != "#SPACE#" }) conf.setInlineComments(classSection, parentInlineComment?.message?.toList() ?: getInlineComments ?: listOf())
                // f.set(classInstance, getObj)
            }

            // レベルが高い順に上から並び替える
            val fields = clazz.declaredFields
                .sortedByDescending { f -> runCatching { f.getAnnotation(Weight::class.java)?.weight }.getOrElse { 2147483647 } }

            // フィールドをループ
            fields.forEach { f ->

                // フィールドのアノテーションを取得
                val fieldSectionName = runCatching { f.getAnnotation(SectionName::class.java) }.getOrNull()
                val fieldComment = runCatching { f.getAnnotation(Comment::class.java) }.getOrNull()
                val fieldInlineComment = runCatching { f.getAnnotation(InlineComment::class.java) }.getOrNull()
                val fieldSpace = runCatching { f.getAnnotation(Space::class.java) }.getOrNull()
                val fieldCopy = runCatching { f.getAnnotation(Copy::class.java) }.getOrNull()
                val fieldDelete = runCatching { f.getAnnotation(Delete::class.java) }.getOrNull()

                // フィールドの情報取得
                f.isAccessible = true
                val fieldName = f.name
                var section = "${classSection}${if (classSection.isNotBlank()) "." else ""}$fieldName"
                val obj = f.get(classInstance)

                // セクション保存
                if (fieldSectionName != null) sectionNameCache[section] = fieldSectionName.path

                // パスマッピング
                if (parentPathMapping != null)
                    section = section.replace("(?:^|\\.)${parentPathMapping.onlyUseArgumentPath}".toRegex(RegexOption.IGNORE_CASE), "")
                else if (classPathMapping != null)
                    section = section.replace("(?:^|\\.)${classPathMapping.onlyUseArgumentPath}".toRegex(RegexOption.IGNORE_CASE), "")

                // 削除キュー追加
                if (fieldDelete != null) {
                    deletePathQueue.add(section)
                    return@forEach
                }

                // クラス内クラスを処理待機配列に追加
                if (f.type.isAnnotationPresent(SubConfig::class.java)) {

                    // セクション名を強制変更
                    if (fieldSectionName != null && fieldSectionName.path.isNotBlank()) {
                        section = section.replace("(.*(?:^|\\.))(.*?)\$".toRegex(), "$1${fieldSectionName.path}")
                    }

                    // 処理クラス配列に追加
                    classes.add(Triple(section, obj, f))

                    // 処理スキップ
                    return@forEach
                }

                // クラスコピー (セクションコピー)
                if (classCopy != null && conf.isConfigurationSection(classCopy.fromPath) && !conf.isSet(classSection)) {
                    val fromPath = sectionNameCache[classCopy.fromPath] ?: classCopy.fromPath

                    val getSection = conf.getConfigurationSection(fromPath)
                    val getObj = conf.get(fromPath)
                    val getComments = conf.getComments(fromPath)
                    val getInlineComments = conf.getInlineComments(fromPath)

                    conf.createSection(classSection, getSection?.getValues(true) ?: mapOf<String, Any>())
                    if (conf.getComments(classSection).filterNotNull().none { it != "#SPACE#" })
                        conf.setComments(classSection, getComments)
                    if (conf.getComments(classSection).filterNotNull().none { it != "#SPACE#" })
                        conf.setInlineComments(classSection, getInlineComments)
                }

                if (!conf.isSet(section)) {

                    // セクション生成
                    if (!conf.isConfigurationSection(classSection)) conf.createSection(classSection)

                    // デフォルト値 設定
                    conf.set(section, obj)

                    // フィールドのコメントを設定する
                    val fieldSpaceSize = fieldSpace?.size ?: configSpace?.fieldSpaceSize ?: 1
                    val spaceList = (1..fieldSpaceSize).map { "#SPACE#" }.toTypedArray()
                    if (conf.getComments(section).filterNotNull().none { it != "#SPACE#" })
                        conf.setComments(section, listOf(*spaceList, *fieldComment?.message ?: arrayOf()))
                    if (conf.getInlineComments(section).filterNotNull().none { it != "#SPACE#" })
                        conf.setInlineComments(section, fieldInlineComment?.message?.toList() ?: listOf())
                }

                // コンフィグからクラスへ代入
                if (!isReverse) {

                    val getObj = conf.get(section)
                    if (getObj != null) {
                        val result = runCatching { f.set(classInstance, getObj) }
                        if (result.isFailure)
                            println((section + "の代入失敗。データ型は正常ですか? エラー内容:" + (result.exceptionOrNull()?.message ?: "")))
                    }

                    // コンフィグ保存
                    isSaveConfig = true

                } else { // クラスからコンフィグへ代入
                    val getObj = f.get(classInstance)
                    if (getObj != null) {
                        val result = runCatching { conf.set(section, getObj) }
                        if (result.isFailure)
                            println((section + "の保存失敗。データ型は正常ですか? エラー内容:" + (result.exceptionOrNull()?.message ?: "")))
                        else // コンフィグ保存
                            isSaveConfig = true
                    }
                }
            }

            // コメントアウトを最後に挿入 (先に挿入すると反映されないバグに対処)
            val lastClassComment = mutableListOf<String>()
            val lastClassInlineComment = mutableListOf<String>()

            val commentSpaceSize = parentSpace?.size ?: classSpace?.size ?: configSpace?.classSpaceSize ?: 1
            val commentSpaceList = (1..commentSpaceSize).map { "#SPACE#" }.toTypedArray()
            lastClassComment.addAll(commentSpaceList)
            lastClassComment.addAll(parentComment?.message ?: arrayOf())
            lastClassComment.addAll(classComment?.message ?: arrayOf())

            lastClassInlineComment.addAll(parentInlineComment?.message ?: arrayOf())
            lastClassInlineComment.addAll(classInlineComment?.message ?: arrayOf())

            if (conf.getComments(classSection).filterNotNull().none { it != "#SPACE#" })
                conf.setComments(classSection, lastClassComment)

            if (conf.getInlineComments(classSection).filterNotNull().none { it != "#SPACE#" })
                conf.setInlineComments(classSection, lastClassInlineComment)
        }

        // 削除キュー
        deletePathQueue.forEach { path ->
            val convertPath = sectionNameCache[path] ?: path
            conf.set(convertPath, null)
            conf.setComments(convertPath, null)
            conf.setInlineComments(convertPath, null)
        }

        // 保存
        if (isSaveConfig) {
            conf.saveConfig()
            replaceSpaces(this@ConfigUtils)
        }
        return anyClass
    }

    /**
     * スペース特殊文字を空白文字に置き換える
     */
    fun replaceSpaces(conf: ConfigUtils) {

        val yamlFile = conf.file
        val text = yamlFile.readText()
        val lines = text
            .split("\n")
            .map {
                it.replace("(?: +|)# #SPACE#.*\$".toRegex(), "")
            }

        // 書き込み
        yamlFile.writeText(lines.joinToString("\n").trimEnd('\n'))

        // リセット
        conf.resetFileModifiedHistory()
    }

}