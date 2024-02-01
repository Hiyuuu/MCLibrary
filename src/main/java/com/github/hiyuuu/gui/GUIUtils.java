package com.github.hiyuuu.gui;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Deprecated
public class GUIUtils implements Listener  {

    private Plugin plugin;

    // タイトル
    private String title = "";

    // 閉じた際にGUI保存
    private boolean isSaveGUI = false;

    // GUIデータ
    private final LinkedList<char[]> structure = new LinkedList<>();
    private final HashMap<Player, Inventory> inventoryGUI = new HashMap<>();
    private final HashMap<Character, ItemStack> items = new HashMap<>();
    private final HashMap<Character, List<ClickType>> itemClickableType = new HashMap<>();
    private final HashMap<ItemStack, TriConsumer<Player, ItemStack, ClickType>> itemActions = new HashMap<>();
    private Consumer<Inventory> closeActions = c -> {};
    private Consumer<GUIUtils> guiUtils = g -> {};
    private Runnable outSideActions = () -> { };

    /**
     * コンストラクタ
     * @param plugin
     */
    public GUIUtils(Plugin plugin) {
        this.plugin = plugin;
    }
    public GUIUtils(Plugin plugin, Consumer<GUIUtils> guiUtils) {
        this.plugin = plugin;
        this.guiUtils = guiUtils;
    }

    /**
     * GUIのタイトルを設定
     * @param title
     * @return GUIUtils
     */
    public GUIUtils setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * GUIのタイトルを取得
     * @return title
     */
    public String getTitle() { return title; }

    /**
     * GUIを閉じた際にデータを保存する
     * @return GUIUtils
     */
    public GUIUtils makeSaveMode() {
        this.isSaveGUI = true;
        return this;
    }

    /**
     * GUIを閉じた際にデータを抹消する
     * @return GUIUtils
     */
    public GUIUtils makeNonSaveMode() {
        this.isSaveGUI = false;
        return this;
    }

    /**
     * 閉じた際の処理
     * @param closeAction
     * @return
     */
    public GUIUtils close(Consumer<Inventory> closeAction) {
        closeActions = closeAction;
        return this;
    }

    /**
     * 文字列をGUIとして追加
     * @param symbols
     * @return GUIUtils
     */
    public GUIUtils addGUI(String symbols) {
        for (String line : symbols.split("\\||\n")) {
            char char1 = line.charAt(0);
            char char2 = line.charAt(1);
            char char3 = line.charAt(2);
            char char4 = line.charAt(3);
            char char5 = line.charAt(4);
            char char6 = line.charAt(5);
            char char7 = line.charAt(6);
            char char8 = line.charAt(7);
            char char9 = line.charAt(8);
            addGUI(char1, char2, char3, char4, char5, char6, char7, char8, char9);
        }
        return this;
    }

    /**
     * charをGUIとして追加
     * @param slot1
     * @param slot2
     * @param slot3
     * @param slot4
     * @param slot5
     * @param slot6
     * @param slot7
     * @param slot8
     * @param slot9
     * @return
     */
    public GUIUtils addGUI(char slot1, char slot2, char slot3, char slot4, char slot5, char slot6, char slot7, char slot8, char slot9) {
        structure.add(new char[]{ slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9 });
        return this;
    }

    /**
     * 指定行数をGUIから削除
     * @param line
     * @return
     */
    public GUIUtils removeGUI(int line) {
        structure.remove(line - 1);
        return this;
    }

    /**
     * 指定行のGUIをcharで設定
     * @param line
     * @param slot1
     * @param slot2
     * @param slot3
     * @param slot4
     * @param slot5
     * @param slot6
     * @param slot7
     * @param slot8
     * @param slot9
     * @return GUIUtils
     */
    public GUIUtils setGUI(int line, char slot1, char slot2, char slot3, char slot4, char slot5, char slot6, char slot7, char slot8, char slot9) {
        structure.set(line, new char[]{ slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9 });
        return this;
    }

    /**
     * 指定ラインのGUIを取得
     * @param line
     * @return char[]
     */
    public char[] getGUI(int line) { return structure.get(line); }


    /**
     * シンボル対応のアイテム及び処理を追加
     * @param key
     * @param itemStack
     * @param function
     * @return GUIUtils
     */
    public GUIUtils registerAll(char key, ItemStack itemStack, TriConsumer<Player, ItemStack, ClickType> function) {
        register(key, Arrays.toString(ClickableType.values()), itemStack, function);
        return this;
    }
    public GUIUtils registerAll(char key, Material itemType, String displayName, String lore, TriConsumer<Player, ItemStack, ClickType> function) {
        register(key, Arrays.toString(ClickableType.values()), itemType, displayName, lore, function);
        return this;
    }
    public GUIUtils registerAll(char key, Material itemType, String displayName, TriConsumer<Player, ItemStack, ClickType> function) {
        register(key, Arrays.toString(ClickableType.values()), itemType, displayName, "", function);
        return this;
    }
    public GUIUtils registerAll(char key, Material itemType, String displayName, String lore) {
        register(key, Arrays.toString(ClickableType.values()), itemType, displayName, lore, (a, b, c) -> { });
        return this;
    }
    public GUIUtils register(char key, String clickable, Material itemType, String displayName, String lore, TriConsumer<Player,  ItemStack, ClickType> function) {

        // アイテム生成
        ItemStack item = new ItemStack(itemType);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;

        // アイテム設定
        itemMeta.setDisplayName(displayName.replace("&", "§"));
        List<String> loreReplaced = Arrays.stream(lore
                .split("\\|"))
                .map(f -> f.replace("&", "§"))
                .filter(f -> !StringUtils.isBlank(f))
                .collect(Collectors.toList());
        itemMeta.setLore(loreReplaced);

        // アイテムへ反映
        item.setItemMeta(itemMeta);

        register(key, clickable, item, function);
        return this;
    }
    public GUIUtils register(char key, String clickable, ItemStack itemStack, TriConsumer<Player, ItemStack, ClickType> function) {

        // クリック可能
        List<ClickType> clickableTypes = new ArrayList<>();
        if (clickable.contains("SL")) { clickableTypes.add(ClickType.SHIFT_LEFT); }
        if (clickable.contains("SR")) { clickableTypes.add(ClickType.SHIFT_RIGHT); }
        clickable = clickable.replaceAll("SL|SR|SW", "");
        if (clickable.contains("L")) { clickableTypes.add(ClickType.LEFT); }
        if (clickable.contains("R")) { clickableTypes.add(ClickType.RIGHT); }
        if (clickable.contains("W")) { clickableTypes.add(ClickType.WHOLE); }

        // 保存
        items.put(key, itemStack);
        itemClickableType.put(key, clickableTypes);
        itemActions.put(itemStack, function);

        return this;
    }

    /**
     * キーをアイテムへ変換
     * @param key
     * @return ItemStack
     */
    private ItemStack keyToItem(char key) { return items.get(key); }

    /**
     * キーをスロット番号へ変換
     * @param key
     * @return ItemStack
     */
    private List<Integer> keyToSlots(char key) {

        ArrayList<Integer> slots = new ArrayList<>();

        int row = 0;
        for (char[] characters : structure) {
            row++;

            int column = 0;
            for (char c : characters) {
                column++;
                if (key == c) {
                    int slot = (column - 1) + (9 * (row - 1));
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    /**
     * アイテムをキーに変換
     * @param item
     * @return ItemStack
     */
    private Character itemToKey(ItemStack item) {
        for (Map.Entry<Character, ItemStack> entry : items.entrySet()) {
            ItemStack itemStack = entry.getValue();
            if (item.isSimilar(itemStack)) return entry.getKey();
        }
        return null;
    }

    /**
     * アイテムをFunctionへ変換
     * @param item
     * @return ItemStack
     */
    private TriConsumer<Player, ItemStack, ClickType> itemToFunction(ItemStack item) {
        for (Map.Entry<ItemStack, TriConsumer<Player, ItemStack, ClickType>> entry : itemActions.entrySet()) {
            ItemStack itemStack = entry.getKey();
            if (item.isSimilar(itemStack)) return entry.getValue();
        }
        return null;
    }

    /**
     * プレイヤーにGUIを開く
     * @param player
     */
    public void Open(Player player) {

        structure.clear();
        inventoryGUI.clear();
        items.clear();
        itemClickableType.clear();
        itemActions.clear();
        closeActions = c -> {};
        outSideActions = () -> {};

        guiUtils.accept(this);

        // GUI生成モードの取得
        boolean isInitializedGUI = inventoryGUI.containsKey(player);

        // インベントリ生成
        int lines = structure.size();
        Inventory gui = inventoryGUI.computeIfAbsent(player, c -> {
            return Bukkit.createInventory(player, 9 * lines, title.replace("&", "§"));
        });

        if (!isInitializedGUI) {

            // アイテム配置
            int slot = 0;
            for (char[] st : structure) {
                for (char s : st) {

                    ItemStack item = keyToItem(s);
                    try {
                        gui.setItem(slot, item);
                    } catch (ArrayIndexOutOfBoundsException e) { break; }

                    slot++;
                }
            }
        }

        // GUIオープン
        Bukkit.getScheduler().runTask(plugin, () -> { player.openInventory(gui); });

        // ハンドラー除外/登録
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * インベントリクリックイベント
     * @param event
     */
    @EventHandler
    private void onInventoryClickEvent(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof  Player)) return;
        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        org.bukkit.event.inventory.ClickType clickType = event.getClick();
        ItemStack clickedItem = event.getCurrentItem() != null ? event.getCurrentItem() : event.getCursor();

        // 情報取得
        Inventory getGUI = inventoryGUI.get(player);
        Character character = itemToKey(clickedItem);
        List<ClickType> clickTypes = itemClickableType.get(character);
        TriConsumer<Player, ItemStack, ClickType> function = itemToFunction(clickedItem);

        // GUI外クリック
        if (openInventory == getGUI && clickedInventory == null) {
            outSideActions.run();
            return;
        }

        // シフトクリック
        if (openInventory == getGUI && action.name().contains("MOVE")) {
            event.setCancelled(true);
            return;
        }

        // GUIフィルター
        if (clickedInventory != getGUI) return;
        event.setCancelled(true);

        if (clickTypes == null || function == null) return;

        // クリック処理
        if (event.isShiftClick()) {
            if (event.isLeftClick() && clickTypes.contains(ClickType.SHIFT_LEFT)) function.accept(player, clickedItem, ClickType.SHIFT_LEFT);
            if (event.isRightClick() && clickTypes.contains(ClickType.SHIFT_RIGHT)) function.accept(player, clickedItem, ClickType.SHIFT_RIGHT);
        } else {
            if (event.isLeftClick() && clickTypes.contains(ClickType.LEFT)) function.accept(player, clickedItem, ClickType.LEFT);
            if (event.isRightClick() && clickTypes.contains(ClickType.RIGHT)) function.accept(player, clickedItem, ClickType.RIGHT);
            if (!event.isLeftClick() && !event.isRightClick() && clickTypes.contains(ClickType.WHOLE)) function.accept(player, clickedItem, ClickType.WHOLE);
        }
    }

    /**
     * インベントリドラッグイベント
     * @param event
     */
    @EventHandler
    private void onInventoryDragEvent(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        Inventory getGUI = inventoryGUI.get(player);
        if (inventory != getGUI) return;

        event.setCancelled(true);
    }

    /**
     * インベントリクローズイベント
     * @param event
     */
    @EventHandler
    private void onInventoryCloseEvent(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        // フィルター
        Inventory getGUI = inventoryGUI.get(player);
        if (inventory != getGUI) return;

        if (isSaveGUI) inventoryGUI.put(player, inventory);

        // 実行
        closeActions.accept(inventory);

        // ハンドラー削除
        HandlerList.unregisterAll(this);
    }

}