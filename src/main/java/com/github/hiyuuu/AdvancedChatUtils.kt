package com.github.hiyuuu

import net.md_5.bungee.api.chat.*
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.random.Random

/**
 * TELLRAWを表現するクラスです
 * @version 3.1
 * @changelog
 *   v1.0 - クラスを作成
 *   v2.0 - クリックアクションの作成が出来るようになった
 *   v3.0 - サジェストアクションの作成が出来るようになった
 *
 * @usage sendMessageのMessage引数に下記の文字列を渡します
 *
 *  @説明: 「RUN_COMMAND と HOVER のように、同時に2つのプロパティを指定出来ます。(HOVERで１つ、その他で１つ)」
 *    例: &6&lクリックしてください(RUN_COMMAND:/say テキスト,HOVER:&b&l&nこの文字をクリックすると「/say テキスト」コマンドが実行されます)
 *
 *  @説明: 「(NONE)を入力することで、通常のテキストになります。」
 *    例: &6&lクリックすると何か起きます。(RUN_COMMAND:こんにちは)&c&lクリックしても何も起きません。(NONE)
 *
 *  @説明: 「()をエスケープしてテキストとして表示したい場合は \ を前に入力します」
 *     例: &6&lクリックしてください&r\(&4&l絶対クリックするな&r\) (NONE)
 *
 *  @説明:  makeClickAction()の戻り値を含めることでクリック処理を追加できます
 *  @説明: 「&b&lクリックするとラムダ式が呼び出されます(RUN_FUNCTION:${String.valueOf(makeClickAction(...))})」
 */
class AdvancedChatUtils(private val Plugin: Plugin) : Listener, CommandExecutor {

    init {
        Bukkit.getPluginManager().registerEvents(this, Plugin)
        Bukkit.getPluginCommand("c")?.setExecutor(this)
    }

    /**
     * c コマンド定義
     */
    override fun onCommand(Sender: CommandSender, Cmd: Command, Label: String, Args: Array<String>): Boolean = true

    /**
     * クリックアクション
     */
    class ClickAction(val API: AdvancedChatUtils, val HashCode: Int, val Handle : (Array<String>) -> Unit, val isResend: Boolean, val isAsync: Boolean = false, val Args: ArrayList<String> = arrayListOf()) {
        override fun toString(): String { return "|||||||||||||||||||||||||INDEX:${HashCode}|||||||||||||||||||||||||" }
    }

    /**
     * サジェストアクション
     */
    class SuggestAction(val API: AdvancedChatUtils, val HashCode: Int, val Handle : (String, Array<String>) -> Unit, val isResend: Boolean, val isAsync: Boolean = false, var Input: String, val Args: ArrayList<String> = arrayListOf()) {
        override fun toString(): String { return "/c ${HashCode}${if (Args.size > 0) " " else ""}${Args.joinToString(" ")} ➔ ${Input}" }
    }

    // マッチするプロパティ
    private val matchOptions = listOf("RUN_COMMAND", "SUGGEST_COMMAND", "URL", "CLIPBOARD", "OPEN_FILE", "HOVER", "NONE")

    // 個体識別ハッシュ
    private var randomHash = (1..6).map { Random.nextInt(10) }.joinToString("")

    // クリックアクション
    private val clickActions = HashMap<Int, ClickAction>()

    // サジェストアクション
    private val suggestActions = HashMap<Int, SuggestAction>()

    // 前回の履歴保持
    private var messageHandleHistory : ((AdvancedChatUtils) -> String)? = null

    /**
     * プレイヤーへメッセージ描画
     */
    fun sendMessage(Player: Player, MessageHandle: (AdvancedChatUtils) -> String) { messageHandleHistory = MessageHandle
        val message = MessageHandle.invoke(this)
        if (message.isEmpty()) return
        message.split("\n").forEach {
            val components = parseMessage(it)
            Player.spigot().sendMessage(*components)
        }
    }

    /**
     * プレイヤーへメッセージ再描画
     */
    fun sendAgain(Player: Player) { messageHandleHistory?.let { sendMessage(Player, it) } }

    /**
     * クリックアクションを生成します
     */
    fun makeInvokeAction(isAsync: Boolean, isResend: Boolean = true, Handle: (Array<String>) -> Unit) : ClickAction { val actionCount = clickActions.size + 1
        val action = ClickAction(this, actionCount, Handle, isResend, isAsync)
        clickActions.set(actionCount, action)
        return action
    }

    /**
     * サジェストアクションを生成します
     */
    fun makeSuggestAction(isAsync: Boolean, isResend: Boolean = true, Input: String = "", Args: Array<String> = arrayOf(), Handle: (String, Array<String>) -> Unit) : SuggestAction {
        val action = SuggestAction(this, (1..6).map { Random.nextInt(1, 9).toString() }.joinToString("").toInt(), Handle, isResend, isAsync, Input.replace("[§&]".toRegex(), "＆"), arrayListOf<String>().apply { addAll(Args) })
        suggestActions.set(action.HashCode, action)
        return action
    }

    /**
     * メッセージのコンポネントを解析
     */
    private fun parseMessage(Message: String) : Array<BaseComponent> {

        // クリックアクション
        val regex0 = "(?i)RUN_FUNCTION:\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|INDEX:([0-9]+)\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|\\|".toRegex()
        val actionMessage = (Message + "(NONE)")
            .replace(regex0, "RUN_COMMAND:/AdvancedChat-${randomHash} $1")

        // ()のエスケープ
        val safeMessage = actionMessage
            .replace("\\(", "\\\\\\\\\\（")
            .replace("\\)", "\\\\\\\\\\）")

        // 初期設定
        class ComponentHolder(val text: String, val baseComponentText: String?)
        val matchOptionsString = matchOptions.joinToString("|")
        val regex1 = "(?i).*?(.*?)\\(((?:$matchOptionsString).*?)\\)".toRegex()
        val regex2 = "((?:$matchOptionsString)):(.*?)(?:,|\$)".toRegex()

        // 正規表現で、テキストとコンポネントを分けて配列に格納
        val result1 = regex1.findAll(safeMessage).toList()
        val parsedArray = result1.map {

            val groups = it.groups
            val text = groups.get(1)?.value
                ?.replace("&", "§")
                ?.replace("\\\\\\\\\\（", "(")
                ?.replace("\\\\\\\\\\）", ")")
            val baseComponentText = groups.get(2)?.value

            // Text_BaseComponentTextクラスに格納して返す
            if (text != null) ComponentHolder(text, baseComponentText) else null
        }.filterNotNull()

        // ビルド
        val completedArray = parsedArray.flatMap {

            val text = it.text
            val baseComponentText = it.baseComponentText

            // コンポネントビルダー
            val componentBuilder = ComponentBuilder(text)

            // プロパティとコンポネントを正規表現で捜索
            val result2 = baseComponentText?.let { regex2.findAll(baseComponentText).toList() }

            // ループ処理
            result2?.forEach {

                // プロパティと値を正規表現で取得
                val property = it.groups.get(1)?.value
                val value = it.groups.get(2)?.value?.replace("&", "§")?.replace("＆", "&")

                if (property != null && value != null) {

                    // プロパティと値を解析して、コンポネントビルダーに設定
                    when (property.uppercase()) {
                        "RUN_COMMAND" -> { componentBuilder.event(ClickEvent(ClickEvent.Action.RUN_COMMAND, value)) }
                        "SUGGEST_COMMAND" -> { componentBuilder.event(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value)) }
                        "URL" -> { componentBuilder.event(ClickEvent(ClickEvent.Action.OPEN_URL, value)) }
                        "OPEN_FILE" -> { componentBuilder.event(ClickEvent(ClickEvent.Action.OPEN_FILE, value)) }
                        "CLIPBOARD" -> { componentBuilder.event(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value)) }
                        "HOVER" -> { componentBuilder.event(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(value.replace("|", "\n")))) }
                    }
                }
            }

            // ビルド
            return@flatMap componentBuilder.create().toList()
        }

        return completedArray.toTypedArray()
    }

    /**
     *
     * 　イベント
     *
     */

    /**
     * クリックアクションイベント
     */
    @EventHandler
    private fun onCommandEvent1(Event: PlayerCommandPreprocessEvent) { val player = Event.player ; val command = Event.message
        if (!command.startsWith("/AdvancedChat-${randomHash}")) return

        // インデックス取得
        val regex = "(?i)\\/AdvancedChat-[0-9]{0,6} ([0-9]+)(?: |)(.*)".toRegex()
        val result = regex.find(command)
        val commandIndex = result?.groups?.get(1)?.value?.toIntOrNull() ?: return
        val commandArgs = result?.groups?.get(2)?.value?.split(" ")?.toTypedArray() ?: arrayOf()

        // アクション取得
        val retrieveAction = clickActions.get(commandIndex) ?: return

        Event.setCancelled(true)
        if (retrieveAction.isAsync) {
            object : BukkitRunnable() { override fun run() {
                retrieveAction.Handle.invoke(commandArgs)
                if (retrieveAction.isResend) retrieveAction.API.sendAgain(player)
            }}.runTaskAsynchronously(Plugin)
        } else {
            retrieveAction.Handle.invoke(commandArgs)
            if (retrieveAction.isResend) retrieveAction.API.sendAgain(player)
        }
    }

    /**
     * サジェストアクションイベント
     */
    @EventHandler
    private fun onCommandEvent2(Event: PlayerCommandPreprocessEvent) { val player = Event.player ; val command = Event.message
        val regex = "\\/c ([0-9]{6,6}) (?:(.*?) |)➔(?: |)(.*)".toRegex()

        val result = regex.find(command)
        val serialId = result?.groups?.get(1)?.value?.toIntOrNull() ?: return
        val args = result.groups.get(2)?.value?.split(" ")?.toTypedArray() ?: arrayOf()
        val inputText = result.groups.get(3)?.value?.replace("^[ 　]".toRegex(), "") ?: return
        val action = suggestActions.get(serialId) ?: return

        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 2.0F)
        Event.isCancelled = true

        if (action.isAsync) {
            object : BukkitRunnable() { override fun run() {
                action.Handle.invoke(inputText, args)
                if (action.isResend) action.API.sendAgain(player)
            }}.runTaskAsynchronously(Plugin)
        } else {
            action.Handle.invoke(inputText, args)
            if (action.isResend) action.API.sendAgain(player)
        }
    }

}