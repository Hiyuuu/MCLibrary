package com.github.hiyuuu.guimaker

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable

class GUIMaker(
    private val plugin: Plugin,
    private val refreshFunction: (GUIMaker) -> Unit = { _ -> }
) : Inventory, Listener {

    private var GUI : Inventory =  Bukkit.createInventory(null, 54, "")
    private var GUI_viewer : Player? = null
    private var GUI_title = ""
    private var GUI_size = 54
    private var GUI_backGround = ItemStack(Material.AIR)

    private var GUI_autoUpdate : Int? = null
    private var GUI_autoUpdateCount = 0

    private var GUI_isEditable = false
    private var GUI_isClickClose = false
    private var GUI_canItemIn = false
    private var GUI_canItemOut = false
    private var GUI_isLocalSession = false

    // GUIデータ
    private val GUI_items = HashMap<Int, ItemStack>()
    private val GUI_slots = HashMap<Int, (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit>()
    private var GUI_hotbarSwap = HashMap<Int, (Player, ItemStack?) -> Boolean>()
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

    //壁紙
    fun setBackGround(itemStack: ItemStack) : GUIMaker {
        (0..GUI_size.minus(1)).forEach { onClickSlot(it, itemStack) }
        return this
    }
    fun getBackGround() : ItemStack = GUI_backGround

    //GUIサイズ
    fun setLineSize(size: Int) : GUIMaker { GUI_size = size * 9 ; return this }
    fun setSize(size: Int) : GUIMaker { GUI_size = size ; return this }

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

    //GUIインベントリ情報取得
    fun getGUI() : Inventory = GUI
    fun getSlotItems() : List<ItemStack> = GUI_items.map { it.value }

    fun getSlotItem(slot: Int) : ItemStack? = GUI_items[slot]

    //操作規制


    fun setClickClose(toggle: Boolean = true) : GUIMaker { GUI_isClickClose = toggle ; return this }

    fun setHotbarSwap(toggle: Boolean = true) : GUIMaker {
        (0..8).forEach { slot -> setHotbarSwap(slot) { _, _ -> toggle } }
        return this
    }

    fun setHotbarSwaps(function: (Player, ItemStack?) -> Boolean) : GUIMaker {
        (0..8).forEach { slot -> setHotbarSwap(slot, function) }
        return this
    }

    fun setHotbarSwap(hotbarSlot: Int, function: (Player, ItemStack?) -> Boolean) : GUIMaker {
        if (hotbarSlot !in 0..8) throw IllegalArgumentException("hotbarSlot must be between 0 and 8")
        GUI_hotbarSwap[hotbarSlot] = function
        return this
    }

    fun waitForClose() : GUIMaker {
        Thread.sleep(50)

        var isContinue = true
        object:BukkitRunnable() { override fun run() {
            isContinue = isOpening()
        }}.runTaskTimer(plugin, 0, 1)

        while (isContinue) { Thread.sleep(500) }
        return this
    }

    fun setEditable(toggle: Boolean = true) : GUIMaker { GUI_isEditable = toggle ; return this }
    fun setCanItemIn(toggle: Boolean = true) : GUIMaker { GUI_canItemIn = toggle ; return this }
    //fun setCanItemOut(Toggle: Boolean = true) { GUI_CanItemOut = Toggle }

    // GUIが開かれているか
    fun isOpening(player: Player? = getAuthor()) : Boolean = player?.openInventory?.topInventory?.isSame(GUI) ?: false

    fun Inventory.isSame(gui: Inventory) : Boolean = this.hashCode() == gui.hashCode()

    fun resetItems() {
        GUI.clear()
        if (GUI_isLocalSession)
            GUI_sessionName?.run { GUIMakerSession.clearLocal(this) }
        else
            GUI_sessionName?.run { GUIMakerSession.clearGlobal(this) }
    }

    //GUI情報一部リセット
    fun resetClicks() {
        GUI_slots.clear()
        GUI_items.clear()
    }

    //
    //
    //  制御
    //
    //

    //Item
    fun onClickSlot(slot: Int, itemStack: ItemStack) = GUI_items.set(slot, itemStack)
    fun onClickSlot(slotRange: IntRange, itemStack: ItemStack) = slotRange.forEach { slot -> onClickSlot(slot, itemStack) }

    fun onClickSlot(slot: Int, itemStack: ItemStack? = null, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit) {
        GUI_slots.set(slot, function)
        itemStack?.let { onClickSlot(slot, itemStack) }
    }
    fun onClickSlot(slotRange: IntRange, itemStack: ItemStack? = null, function: (Player, ItemStack, ItemStack, GUIMaker_ClickType) -> Unit)
        = slotRange.forEach { slot -> onClickSlot(slot , itemStack, function) }

    //閉じる
    fun onClose(function: (Player, Inventory) -> Unit) { GUI_closeFunction = function }

    // 外側クリック
    fun onOutsideClick(function: (Player) -> Unit) { GUI_outsideFunction = function }

    //リセット
    fun resetSlotFunction(slot: Int) { if (GUI_slots.containsKey(slot)) { GUI_slots.remove(slot) } }
    fun resetItem(slot: Int) {
        onClickSlot(slot, ItemStack(Material.AIR))
    }
    fun resetSlot(slot: Int) { resetItem(slot) ; resetSlotFunction(slot) }
    fun clearSlots() { GUI_slots.clear() }

    fun open(player: Player? = getAuthor()): GUIMaker {

        // 同期処理
        val thisClass = this
        object : BukkitRunnable() { override fun run() {

            GUI_viewer = player ?: return

            resetClicks()
            refreshFunction.invoke(thisClass)

            val sessionName = GUI_sessionName
            if (sessionName == null) {

                GUI = Bukkit.createInventory(null, GUI_size, GUI_title)

                repeat(GUI_size) { GUI.setItem(it, GUI_backGround) }

                GUI_items
                    .toList()
                    .filterIndexed { index, pair -> index <= GUI_size - 1 }
                    .forEach { item ->
                        GUI.setItem(item.first, item.second)
                    }

            } else {

                val restoreInv =
                    if (GUI_isLocalSession) GUIMakerSession.restoreLocal(player, sessionName)
                    else GUIMakerSession.restoreGlobal(sessionName)

                restoreInv?.run {
                    GUI = this
                }
            }

            // リスナー登録
            HandlerList.unregisterAll(thisClass)
            Bukkit.getPluginManager().registerEvents(thisClass, plugin)

            // オープン
            player.openInventory(GUI)

        }}.runTask(plugin)

        return this
    }

    //
    //
    // イベント
    //
    //

    @EventHandler
    private fun onDropEvent(event: PlayerDropItemEvent) {
        if (!isOpening(event.player)) return
            event.isCancelled = true
    }

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

        // 編集バイパス
        var bypassSwap = false

        // 開いているGUIを照合
        if (!isOpening(player)) return


        // ホットバー操作
        val hotbarSwap = GUI_hotbarSwap[hotBarSlot]
        val canHotbarSwap = hotbarSwap?.invoke(player, event.currentItem)
        if (canHotbarSwap == true) bypassSwap = true
        if ((canHotbarSwap != null && !canHotbarSwap) && action == InventoryAction.HOTBAR_SWAP) {
            event.isCancelled = true
            return
        }

        // プレイヤーインベントリからのアイテム取入れを禁止
        if (!bypassSwap && !GUI_canItemIn && action.name.contains("MOVE")) {
            event.isCancelled = true
        }

        // GUI外 クリック処理
        if (clickedInventory == null) {
            GUI_outsideFunction.invoke(player)
            return
        }

        // クリックしたGUIを照合
        if (!GUI.isSame(clickedInventory)) return

        //GUI内アイテム操作禁止
        if (!bypassSwap && GUI_isEditable) event.isCancelled = false
        else event.isCancelled = true

        //実行
        val function = GUI_slots.get(clickedSlot)
        if (clickedItem != null && function != null) {

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

        //GUI閉じる
        if (GUI_isClickClose) player.closeInventory()
    }

    @EventHandler //閉じる
    private fun onCloseGUI(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val closedInventory = event.inventory

        // GUI照合
        if (!GUI.isSame(closedInventory)) return

        // クローズ処理
        GUI_closeFunction.invoke(player, closedInventory)

        // セッションへ保存
        val sessionName = GUI_sessionName
        if (isSaveGUI() && sessionName != null) {

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
        val inventorySlots = event.inventorySlots

        // GUI照合
        if (!isOpening(player)) return

        // インベントリ情報取得
        val pInventory = player.inventory
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
                    val amount1 = it.second

                    // 変更後 数量取得
                    val amount2 = pInventory.getItem(slot)?.amount ?: 0
                    val amountDiff = amount2 - amount1

                    val item = pInventory.getItem(slot) ?: return@forEach
                    item.amount = amount1

                    cursorOverride.run { amount += amountDiff }
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