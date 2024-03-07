package com.github.hiyuuu.guimaker

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.math.max
import kotlin.math.min

class GUIMaker(
    private val plugin: Plugin,
    private val refreshFunction: (GUIMaker) -> Unit = { _ -> }
) : Inventory, Listener {

    private var GUI_type = InventoryType.CHEST
    private var GUI_size = 54
    private var GUI : Inventory =  Bukkit.createInventory(null, GUI_size)
    private var GUI_viewer : Player? = null
    private var GUI_title = ""
    private var GUI_backGround = ItemStack(Material.AIR)

    private var GUI_autoUpdate : Int? = null
    private var GUI_autoUpdateCount = 0

    private var GUI_isEditableAll = false
    private var GUI_isClickClose = false
    private var GUI_canItemPlace = false
    private var GUI_canItemPick = false
    private var GUI_canCreativeItemClone = false
    private var GUI_isLocalSession = false

    // GUIデータ
    private val GUI_items = HashMap<Int, ItemStack>()
    private val GUI_functions = HashMap<Int, (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit>()
    private val GUI_hotbarSwap = HashMap<Int, (Player, ItemStack?, ItemStack?, Boolean, Int) -> Boolean>()

    private val GUI_keyStructures = LinkedList<CharArray>()
    private val GUI_keyItems = HashMap<Char, ItemStack>()
    private val GUI_keyFunctions = HashMap<Char, (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit>()

    private var GUI_closeFunction : (Player, Inventory) -> Unit = { _, _ -> }
    private var GUI_outsideFunction : (Player) -> Unit = { _ -> }
    private var GUI_sessionName : String? = null

    init {

        // 自動 GUI再描画
        object:BukkitRunnable() { override fun run() {
            if (!isOpening()) return
            val getAutoUpdate = getAutoUpdate() ?: return

            GUI_autoUpdateCount++
            if (GUI_autoUpdateCount >= getAutoUpdate) {

                GUI_autoUpdateCount = 0
                open(getAuthor() ?: return)
            }
        }}.runTaskTimer(plugin, 0, 1)
    }

    // タイトル
    fun setTitle(title: String) : GUIMaker { GUI_title = title.replace("&", "§") ; return this }
    fun getTile() : String = GUI_title

    // インベントリタイプ
    fun setInvType(inventoryType: InventoryType) {
        GUI_type = inventoryType
        GUI_size = inventoryType.defaultSize
    }
    fun getInvType() : InventoryType = GUI_type

    // 壁紙
    fun setBackGround(itemStack: ItemStack) : GUIMaker {
        (0..GUI_size.minus(1)).forEach { onClickSlot(it, itemStack) }
        return this
    }
    fun getBackGround() : ItemStack = GUI_backGround

    // GUIサイズ
    fun setLineSize(size: Int) : GUIMaker { GUI_size = size * 9 ; return this }
    fun setSize(size: Int) : GUIMaker { GUI_size = size ; return this }

    // GUI操作
    fun setClickClose(toggle: Boolean = true) : GUIMaker { GUI_isClickClose = toggle ; return this }
    fun setItemEditableAll(toggle: Boolean = true) : GUIMaker { GUI_isEditableAll = toggle ; return this }
    fun setCanItemPlace(toggle: Boolean = true) : GUIMaker { GUI_canItemPlace = toggle ; return this }
    fun setCanItemPick(toggle: Boolean = true) : GUIMaker { GUI_canItemPick = toggle ; return this }
    fun setCanCreativeItemClone(toggle: Boolean = true) : GUIMaker { GUI_canCreativeItemClone = toggle ; return this }

    // GUI自動アップデート
    fun enableAutoUpdate(ticks: Int) : GUIMaker { GUI_autoUpdate = ticks ; GUI_autoUpdateCount = 0 ; return this }
    fun disableAutoUpdate() : GUIMaker { GUI_autoUpdate = null ; return this }
    fun getAutoUpdate() : Int? = GUI_autoUpdate

    // GUIの再利用
    fun isSaveGUI() : Boolean = GUI_sessionName != null
    fun enableSaveGUI(sessionName: String, perPlayerSession: Boolean = true) : GUIMaker {
        GUI_sessionName = sessionName
        GUI_isLocalSession = perPlayerSession
        return this
    }
    fun disableSaveGUI() : GUIMaker { GUI_sessionName = null ; return this }

    // GUI使用者
    fun getAuthor() : Player? = GUI_viewer

    // GUIインベントリ情報取得
    fun getGUI() : Inventory = GUI
    fun getSlotItems() : List<ItemStack> = GUI_items.map { it.value }
    fun getSlotItem(slot: Int) : ItemStack? = GUI_items[slot]

    fun getGUIItem(slot: Int) : ItemStack? = GUI_items[slot]

    fun getGUIFunction(slot: Int) : ((Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit)? = GUI_functions[slot]

    fun getGUIhotbarSwap(slot: Int) : ((Player, ItemStack?, ItemStack?, Boolean, Int) -> Boolean)? = GUI_hotbarSwap[slot]

    fun getGUICloseFunction() : (Player, Inventory) -> Unit = GUI_closeFunction

    fun getOutsideFunction() : (Player) -> Unit = GUI_outsideFunction

    // ホットバースワップを全有効
    fun setHotbarSwap(toggle: Boolean = true) : GUIMaker {
        (0..8).forEach { slot -> setHotbarSwap(slot) { _, _, _, _, _ -> toggle } }
        return this
    }

    // ホットバースワップを条件付きで有効
    fun setHotbarSwapAll(function: (Player, ItemStack?, ItemStack?, Boolean, Int) -> Boolean) : GUIMaker {
        setHotbarSwap(0..8, function)
        return this
    }
    // ホットバースワップを条件付きで一部有効
    fun setHotbarSwap(slotRange: IntRange, function: (Player, ItemStack?, ItemStack?, Boolean, Int) -> Boolean) : GUIMaker {
        slotRange.forEach { slot -> setHotbarSwap(slot, function) }
        return this
    }
    fun setHotbarSwap(hotbarSlot: Int, function: (Player, ItemStack?, ItemStack?, Boolean, Int) -> Boolean) : GUIMaker {
        if (hotbarSlot !in 0..8) throw IllegalArgumentException("hotbarSlot must be between 0 and 8")
        GUI_hotbarSwap[hotbarSlot] = function
        return this
    }

    // GUIが閉じるのを待機
    fun waitForClose() : GUIMaker {
        Thread.sleep(50)

        var isContinue = true
        object:BukkitRunnable() { override fun run() {
            isContinue = isOpening()
        }}.runTaskTimer(plugin, 0, 1)

        while (isContinue) { Thread.sleep(500) }
        return this
    }

    // GUIが開かれているか
    fun isOpening(player: Player? = getAuthor()) : Boolean = player?.openInventory?.topInventory?.isSame(GUI) ?: false
    fun Inventory.isSame(gui: Inventory) : Boolean = this.hashCode() == gui.hashCode()

    //
    //
    //  アイテム制御
    //
    //

    // クリック時
    fun onClickSlot(slot: Int, itemStack: ItemStack, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> }) {
        GUI_functions[slot] = function
        GUI_items[slot] = itemStack
    }
    fun onClickSlot(slotRange: IntRange, itemStack: ItemStack, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> })
            = slotRange.forEach { slot -> onClickSlot(slot , itemStack, function) }
    fun onClickSlot(slots: List<Int>, itemStack: ItemStack, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> })
            = slots.forEach { slot -> onClickSlot(slot, itemStack, function)}
    fun onClickSlotHollow(startSlot: Int, endSlot: Int, itemStack: ItemStack, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> }) {

        val minSlot = min(startSlot, endSlot)
        val maxSlot = max(startSlot, endSlot)
        val minColumn = minSlot % 9
        val maxColumn = maxSlot % 9

        val upLeft = if (maxColumn > minColumn) minSlot else minSlot - (minColumn - maxColumn)
        val downRight = if (maxColumn > minColumn) maxSlot else maxSlot + (minColumn - maxColumn)
        val diffRightToLeft = (downRight % 9) - (upLeft % 9)
        val upRight = upLeft + diffRightToLeft
        val downLeft = downRight - diffRightToLeft

        onClickSlot(upLeft..upRight, itemStack, function)
        onClickSlot(downLeft..downRight, itemStack, function)

        val lines = (downLeft - upLeft) / 9 + 1
        (1..lines).forEach { l ->
            val line = (l - 1)

            onClickSlot(upLeft + (9 * line), itemStack, function)
            onClickSlot(upRight + (9 * line), itemStack, function)
        }
    }
    fun onClickSlotFill(startSlot: Int, endSlot: Int, itemStack: ItemStack, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> }) {

        val minSlot = min(startSlot, endSlot)
        val maxSlot = max(startSlot, endSlot)
        val minColumn = minSlot % 9
        val maxColumn = maxSlot % 9

        val upLeft = if (maxColumn > minColumn) minSlot else minSlot - (minColumn - maxColumn)
        val downRight = if (maxColumn > minColumn) maxSlot else maxSlot + (minColumn - maxColumn)
        val diffRightToLeft = (downRight % 9) - (upLeft % 9)
        val upRight = upLeft + diffRightToLeft
        val downLeft = downRight - diffRightToLeft

        var line = 0
        while (true) {
            val startAt = upLeft + (9 * line)
            if (startAt > downLeft) break

            onClickSlot(startAt..(startAt + diffRightToLeft), itemStack, function)

            line++
        }
    }

    // 閉じる際
    fun onClose(function: (Player, Inventory) -> Unit) { GUI_closeFunction = function }

    // 外側クリック時
    fun onOutsideClick(function: (Player) -> Unit) { GUI_outsideFunction = function }

    /**
     * シンボル対応のアイテム及び処理を追加
     * @param keys
     * @param function
     * @return GUIMaker
     */
    fun registerKeyItems(
        keys: List<Pair<Char, ItemStack>>,
        function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit = { _, _, _, _ -> }
    ) : GUIMaker {

        keys.forEach { pair ->
            val key = pair.first
            val itemStack = pair.second

            GUI_keyItems[key] = itemStack
            GUI_keyFunctions[key] = function
        }

        return this
    }

    /**
     * シンボル対応のアイテム及び処理を追加
     * @param key
     * @param itemStack
     * @param function
     * @return GUIMaker
     */
    fun registerKeyItem(
        key: Char,
        itemStack: ItemStack,
        function: ((Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit)? = null
    ): GUIMaker {

        // 保存
        GUI_keyItems[key] = itemStack
        function?.let { GUI_keyFunctions[key] = function }

        return this
    }

    /**
     * 文字列をGUIとして追加
     * @param symbols
     * @return GUIUtils
     */
    fun addKeyLines(symbols: String): GUIMaker {
        symbols.split("[|\n]".toRegex()).forEachIndexed { index, line ->
            if (line.length < 9) throw IllegalArgumentException("${index + 1} 行目に引き渡されたシンボルが9個以下です")
            GUI_keyStructures.add(charArrayOf(line[0], line[1], line[2], line[3], line[4], line[5], line[6], line[7], line[8]))
        }
        applyKeyItems()
        return this
    }

    fun applyKeyItems() {

        // キーアイテムの設置処理
        var slot = -1
        GUI_keyStructures.toList().forEach { line ->

            line.forEach { key ->
                if (slot++ >= GUI.size) return
                val retrieveItem = GUI_keyItems[key] ?: return@forEach
                val retrieveFunction = GUI_keyFunctions[key] ?: { _, _, _, _ -> }

                onClickSlot(slot, retrieveItem, retrieveFunction)
            }
        }
    }

    /**
     * 指定行のGUIをcharで設定
     * @return GUIMaker
     */
    fun setKeyLine(
        line: Int,
        slot1: Char, slot2: Char, slot3: Char, slot4: Char, slot5: Char,
        slot6: Char, slot7: Char, slot8: Char, slot9: Char
    ): GUIMaker {
        GUI_keyStructures[line] = charArrayOf(slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9)
        applyKeyItems()
        return this
    }

    /**
     * 指定ラインのGUIを取得
     * @param line
     * @return charArray
     */
    fun getKeyLine(line: Int): CharArray = GUI_keyStructures[line]

    fun clearKeyItems() {
        GUI_keyItems.clear()
        GUI_keyFunctions.clear()
        GUI_keyStructures.clear()
    }

    /**
     * キーをアイテムへ変換
     * @param key
     * @return ItemStack
     */
    fun keyToItem(key: Char): ItemStack? = GUI_keyItems[key]

    /**
     * キーをスロット番号へ変換
     * @param key
     * @return ItemStack
     */
    fun keyToSlots(key: Char): List<Int> {
        val slots = ArrayList<Int>()

        var row = 0
        GUI_keyStructures.forEach { characters ->
            row++

            var column = 0
            characters.forEach { c ->

                column++
                if (key == c) {
                    val slot = (column - 1) + (9 * (row - 1))
                    slots.add(slot)
                }
            }
        }

        return slots
    }

    /**
     * アイテムをキーに変換
     * @param item
     * @return ItemStack
     */
    fun itemToKey(item: ItemStack): Char? {
        for ((key, itemStack) in GUI_keyItems) {
            if (item.isSimilar(itemStack)) return key
        }
        return null
    }

    // リセット
    fun resetSlot(slot: Int) : Boolean = resetItem(slot) && resetFunction(slot)
    fun resetFunction(slot: Int) : Boolean { return GUI_functions.remove(slot) != null }
    fun resetItem(slot: Int) : Boolean { return GUI_items.remove(slot) != null }
    fun resetSession() {
        if (GUI_isLocalSession) GUI_sessionName?.run { GUIMakerSession.clearLocal(this) }
        else GUI_sessionName?.run { GUIMakerSession.clearGlobal(this) }
    }
    fun resetSoftData() {
        GUI_functions.clear()
        GUI_items.clear()
        GUI_keyItems.clear()
        GUI_keyStructures.clear()
        GUI_keyFunctions.clear()
    }
    fun resetHardData() {
        resetSession()
        resetSoftData()
    }
    fun resetAll() {
        GUI.clear()
        resetHardData()
    }

    // インベントリを開く
    fun open(player: Player? = getAuthor()): GUIMaker {
        GUI_viewer = player ?: return  this

        // セッション復元
        val sessionName = GUI_sessionName
        val restoreInv = if (sessionName != null && GUI_isLocalSession) GUIMakerSession.restoreLocal(player, sessionName)
        else if (sessionName != null) GUIMakerSession.restoreGlobal(sessionName)
        else null

        if (restoreInv == null) {

            // リセット処理処理
            resetHardData()
            refreshFunction.invoke(this)

            // インベントリ生成
            if (GUI_type == InventoryType.CHEST) {
                GUI = Bukkit.createInventory(null, GUI_size, GUI_title)
            } else {
                GUI = Bukkit.createInventory(null, GUI_type, GUI_title)
            }

            // 背景アイテム処理
            repeat(GUI_size) { GUI.setItem(it, GUI_backGround) }

            // 通常アイテムの設置処理
            GUI_items.toList()
                .filter { item -> item.first < GUI_size }
                .forEach { item -> GUI.setItem(item.first, item.second) }

        } else {

            // リセット処理処理
            resetSoftData()
            refreshFunction.invoke(this)

            // セッションを再代入
            GUI = restoreInv
        }

        // リスナー登録
        HandlerList.unregisterAll(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)

        // 同期処理でオープン
        object : BukkitRunnable() { override fun run() {

            player.openInventory(GUI)
        }}.runTask(plugin)

        return this
    }

    //
    //
    // イベント
    //
    //

    /**
     * インベントリクリックインベントのアクション一覧
     *
     * NOTHING: クリックから何も起こらないことを表します。
     * PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE: クリックしたスロットのアイテムがカーソルに移動する際の異なる操作を表します。
     * PLACE_ALL, PLACE_SOME, PLACE_ONE: カーソルのアイテムがクリックしたスロットに配置される際の異なる操作を表します。
     * SWAP_WITH_CURSOR: クリックしたアイテムとカーソルのアイテムが交換されます。
     * DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT: クリックしたスロットまたはカーソルのアイテムがドロップされる際の異なる操作を表します。
     * MOVE_TO_OTHER_INVENTORY: アイテムが空きがあれば別のインベントリに移動します。
     * HOTBAR_MOVE_AND_READD: クリックしたスロットのアイテムがホットバーに移動し、ホットバーのアイテムはプレイヤーのインベントリに再追加されます。
     * HOTBAR_SWAP: クリックしたスロットとホットバーの選択されたスロットが交換されます。
     * CLONE_STACK: クリックしたアイテムの最大スタックサイズの複製がカーソルに配置されます。
     * COLLECT_TO_CURSOR: 同じ素材のアイテムがインベントリ内で検索され、カーソルに最大スタックサイズまで配置されます。
     * UNKNOWN: 未知のクリックタイプを表します。
     */
    @EventHandler
    private fun onClickInventoryEvent(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedInventory = event.clickedInventory
        val clickedSlot = event.slot
        val clickedItem = event.currentItem
        val putItem = event.cursor ?: ItemStack(Material.AIR)
        val clickType = event.click
        val hotBarSlot = event.hotbarButton
        val action = event.action

        // 開いているGUIを照合
        if (!isOpening(player)) return

        // GUI外 クリック処理
        if (clickedInventory == null) {
            GUI_outsideFunction.invoke(player)
            return
        }

        // クリック処理の取得
        val function = GUI_functions[clickedSlot]

        // ホットバー操作
        val hotbarSwapInvoker = GUI_hotbarSwap[hotBarSlot]
        val playerHotbarItem = runCatching { player.openInventory.bottomInventory.getItem(hotBarSlot) }.getOrNull()
        val canHotbarSwap = hotbarSwapInvoker?.invoke(player, playerHotbarItem, event.currentItem, function != null, clickedSlot)

        // 上部GUIをクリックした場合
        if (GUI.isSame(clickedInventory)) {

            if (GUI_canItemPlace && action.name.contains("PLACE|COLLECT".toRegex())) event.isCancelled = false
            else if (GUI_canItemPick && action.name.contains("PICKUP|MOVE_TO".toRegex())) event.isCancelled = false
            else if (GUI_canCreativeItemClone && action.name.contains("CLONE")) event.isCancelled = false
            else if (canHotbarSwap != null && canHotbarSwap && action.name.contains("HOTBAR")) event.isCancelled = false
            else event.isCancelled = true

        } else { // 下部GUIをクリックした場合

            // GUI内アイテム操作の処理
            if (GUI_canItemPlace && action.name.contains("MOVE")) event.isCancelled = false
            else if ((GUI_canItemPick && GUI_canItemPlace) && action.name.contains("COLLECT".toRegex())) event.isCancelled = false
            else if (GUI_canItemPick && action.name.contains("PICKUP|PLACE".toRegex())) event.isCancelled = false
            else if (GUI_canItemPick && action.name.contains("HOTBAR")) event.isCancelled = false
            else event.isCancelled = true

            return
        }
        if (GUI_isEditableAll) event.isCancelled = false

        // クリック処理実行
        if (clickedItem != null && function != null) {
            if (!GUI_isEditableAll) event.isCancelled = true

            if (event.isRightClick && event.isShiftClick) {
                function.invoke(player, clickedItem, putItem, GUIMaker_ClickType.SHIFT_RIGHT)
            } else if (event.isLeftClick && event.isShiftClick) {
                function.invoke(player, clickedItem, putItem, GUIMaker_ClickType.SHIFT_LEFT)
            } else if (event.isRightClick) {
                function.invoke(player, clickedItem, putItem, GUIMaker_ClickType.RIGHT)
            } else if (event.isLeftClick) {
                function.invoke(player, clickedItem, putItem, GUIMaker_ClickType.LEFT)
            } else if (!event.isLeftClick && !event.isRightClick && !event.isShiftClick) {
                function.invoke(player, clickedItem, putItem, GUIMaker_ClickType.WHOLE)
            }
        }
        if (canHotbarSwap != null && canHotbarSwap) event.isCancelled = false

        //GUI閉じる
        if (GUI_isClickClose) player.closeInventory()
    }

    @EventHandler // 閉じる
    private fun onCloseGUI(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val closedInventory = event.inventory

        // GUI照合
        if (!GUI.isSame(closedInventory)) return

        // クローズ処理
        GUI_closeFunction.invoke(player, closedInventory)

        // セッションへ保存
        val sessionName = GUI_sessionName
        if (sessionName != null) {

            if (GUI_isLocalSession)
                GUIMakerSession.saveLocal(player, sessionName, closedInventory)
            else
                GUIMakerSession.saveGlobal(sessionName, closedInventory)
        }

        //イベント取り消し
        HandlerList.unregisterAll(this)
    }

    @EventHandler // インベントリドラッグ
    private fun onInventoryDragEvent(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventorySlots = event.rawSlots.filter { it < GUI.size }

        // GUI照合
        if (!isOpening(player)) return

        // GUIアイテム取り入れ許可
        if (GUI_canItemPlace) return

        // インベントリ情報取得
        val pInventory = event.inventory // player.inventory
        val cursorOverride = event.oldCursor.clone()

        // 元 数量情報取得
        val slotAmountArray = inventorySlots.map { slot ->
            val amount = pInventory.getItem(slot)?.amount ?: 0
            return@map slot to amount
        }

        object:  BukkitRunnable() {
            override fun run() {

                // カーソルアイテム 元数量取得
                cursorOverride.amount = player.itemOnCursor.amount

                // アイテムバック
                slotAmountArray.forEach {
                    val slot = it.first
                    val oldAmount = it.second

                    // 変更後 数量取得
                    val newAmount = pInventory.getItem(slot)?.amount ?: 0
                    val diffAmount = newAmount - oldAmount

                    val item = pInventory.getItem(slot) ?: return@forEach
                    item.amount = oldAmount

                    cursorOverride.run { amount += diffAmount }
                }

                // 反映
                player.setItemOnCursor(cursorOverride)

            }
        }.runTask(plugin)
    }

    @EventHandler // プラグイン無効時に閉じる
    private fun onPluginDisableEvent(event: PluginDisableEvent) {
        if (event.plugin != plugin) return
        Bukkit.getOnlinePlayers().filter { isOpening(it) }.forEach { it.closeInventory() }
    }

    //
    //
    //  Inventory オーバーライド実装
    //
    //

    override fun iterator(): MutableListIterator<ItemStack> = GUI.iterator()

    override fun iterator(index: Int): MutableListIterator<ItemStack> = GUI.iterator(index)

    override fun getSize(): Int = GUI.size

    override fun getMaxStackSize(): Int = GUI.maxStackSize

    override fun setMaxStackSize(size: Int) = GUI.setMaxStackSize(size)

    override fun getItem(index: Int): ItemStack? = GUI.getItem(index)

    override fun setItem(index: Int, item: ItemStack?) = GUI.setItem(index, item)

    override fun addItem(vararg items: ItemStack): java.util.HashMap<Int, ItemStack> = GUI.addItem(*items)

    override fun removeItem(vararg items: ItemStack): java.util.HashMap<Int, ItemStack> = GUI.removeItem(*items)

    override fun getContents(): Array<out ItemStack?> = GUI.getContents()

    override fun setContents(items: Array<out ItemStack?>) = GUI.setContents(items)

    override fun getStorageContents(): Array<out ItemStack?> = GUI.getStorageContents()

    override fun setStorageContents(items: Array<out ItemStack?>) = GUI.setStorageContents(items)

    override fun contains(material: Material): Boolean = GUI.contains(material)

    override fun contains(item: ItemStack?): Boolean = GUI.contains(item)

    override fun contains(material: Material, amount: Int): Boolean = GUI.contains(material, amount)

    override fun contains(item: ItemStack?, amount: Int): Boolean = GUI.contains(item, amount)

    override fun containsAtLeast(item: ItemStack?, amount: Int): Boolean = GUI.containsAtLeast(item, amount)

    override fun all(material: Material): java.util.HashMap<Int, out ItemStack> = GUI.all(material)

    override fun all(item: ItemStack?): java.util.HashMap<Int, out ItemStack> = GUI.all(item)

    override fun first(material: Material): Int = GUI.first(material)

    override fun first(item: ItemStack): Int = GUI.first(item)

    override fun firstEmpty(): Int = GUI.firstEmpty()

    override fun isEmpty(): Boolean = GUI.isEmpty

    override fun remove(material: Material) = GUI.remove(material)

    override fun remove(item: ItemStack) = GUI.remove(item)

    override fun clear(index: Int) = GUI.clear(index)

    override fun clear() = GUI.clear()

    override fun getViewers(): MutableList<HumanEntity> = GUI.viewers

    override fun getType(): InventoryType = GUI.type

    override fun getHolder(): InventoryHolder? = GUI.holder

    override fun getLocation(): Location? = GUI.location

}