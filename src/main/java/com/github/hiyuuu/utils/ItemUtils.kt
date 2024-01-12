package com.github.hiyuuu.utils

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.profile.PlayerProfile
import java.net.URL
import java.util.*


class ItemUtils(function: (ItemUtils) -> Unit = {}) : ItemStack(Material.STONE) {

    companion object {
        lateinit private var plugin : Plugin
        fun onEnable(plugin: Plugin) { Companion.plugin = plugin }
    }

    private var itemMetaData = fun() : ItemMeta? { return this.itemMeta }

    constructor(textures: String, function: (ItemUtils) -> Unit = {}) : this(textures, null, null, function)
    constructor(textures: String, display: String? = null, function: (ItemUtils) -> Unit = {}) : this(textures, display, null, function)
    constructor(textures: String, display: String? = null, lore: List<String>? = null, function: (ItemUtils) -> Unit = {}) : this(Material.PLAYER_HEAD, display, lore, function) {
        makeCustomSkull(textures)
    }

    constructor(material: Material, function: (ItemUtils) -> Unit = {}) : this(material, null, null, function)
    constructor(material: Material, display: String? = null, function: (ItemUtils) -> Unit = {}) : this(material, display, null, function)
    constructor(material: Material, display: String? = null, lore: List<String>? = null, function: (ItemUtils) -> Unit = {}) : this() {
        this.type = material
        display?.let { setDisplay(it) }
        lore?.let { setLore(it) }
        function.invoke(this)
    }

    constructor(itemStack: ItemStack, function: (ItemUtils) -> Unit = {}) : this(itemStack, null, null, function)
    constructor(itemStack: ItemStack, display: String? = null, function: (ItemUtils) -> Unit = {}) : this(itemStack, display, null, function)
    constructor(itemStack: ItemStack, display: String? = null, lore: List<String>? = null, function: (ItemUtils) -> Unit = {}) : this() {
        this.type = itemStack.type
        this.amount = itemStack.amount
        this.durability = itemStack.durability
        this.data = itemStack.data
        this.addEnchantments(itemStack.enchantments)
        this.setItemMeta(itemStack.itemMeta)
        display?.let { setDisplay(it) }
        lore?.let { setLore(it) }
        function.invoke(this)
    }

    fun isItem(itemStack: ItemStack): Boolean {
        val itemUtils = ItemUtils(itemStack)
        return isItem(itemUtils.type, itemUtils.run { getDisplay() })
    }
    fun isItem(material: Material, displayName: String) : Boolean {
        val itemMeta = itemMetaData.invoke() ?: return false
        if (this.type != material || !itemMeta.hasDisplayName()) return false
        return itemMeta.displayName.toNonColorText().equals(displayName.toNonColorText(), true)
    }

    fun setDisplay(name: String) : ItemUtils {
        val itemMeta = itemMetaData.invoke() ?: return this
        itemMeta.setDisplayName(name.toColorText())
        this.setItemMeta(itemMeta)
        return this
    }
    fun getDisplay() : String {
        val itemMeta = itemMetaData.invoke() ?: return ""
        if (!itemMeta.hasDisplayName()) return ""
        return itemMeta.displayName
    }
    fun getNonColorDisplay() : String = getDisplay().toNonColorText()

    fun setLore(lore: List<String>) : ItemUtils {
        val itemMeta = itemMetaData.invoke() ?: return this
        itemMeta.lore = lore.map { it.replace("&", "§") } ?: listOf()
        this.setItemMeta(itemMeta)
        return this
    }

    fun getLore() : List<String> {
        val itemMeta = itemMetaData.invoke() ?: return listOf()
        return itemMeta.lore?.map { it.toColorText() } ?: listOf()
    }

    fun makePlayerSkull(offlinePlayer: OfflinePlayer) : ItemUtils {
        val skullMeta = itemMetaData.invoke() as? SkullMeta ?: throw IllegalStateException("item is not player head")
        skullMeta.owningPlayer = offlinePlayer
        this.setItemMeta(skullMeta)
        return this
    }

    fun makeCustomSkull(textures: String) : ItemUtils {
        val signature = "H116D5fhmj/7BVWqiRQilXmvoJO6wJzXH4Dvou6P2o9YMb+HaJT8s9+zt03GMYTipzK+NsW2D2JfzagnxLUTuiOtrCHm6V2udOM0HG0JeL4zR0Wn5oHmu+S7kUPUbt7HVlKaRXry5bobFQ06nUf7hOV3kPfpUJsfMajfabmoJ9RGMRVot3uQszjKOHQjxyAjfHP2rjeI/SktBrSscx0DzwBW9LCra7g/6Cp7/xPQTIZsqz2Otgp6i2h3YpXJPy02j4pIk0H4biR3CaU7FB0V4/D1Hvjd08giRvUpqF0a1w9rbpIWIH5GTUP8eLFdG/9SnHqMCQrTj4KkQiN0GdBO18JvJS/40LTn3ZLag5LBIa7AyyGus27N3wdIccvToQ6kHHRVpW7cUSXjircg3LOsSQbJmfLoVJ/KAF/m+de4PxIjOJIcbiOkVyQfMQltPg26VzRiu3F0qRvJNAAydH8AHdaqhkpSf6yjHqPU3p3BHFJld5o59WoD4WNkE3wOC//aTpV/f9RJ0JQko08v2mGBVKx7tpN7vHD1qD5ILzV1nDCV1/qbKgiOK9QmdXqZw9J3pM/DHtZ6eiRKni9BuGWlbWFN/qfFO2xY+J7SYFqTxBbffmvwvuF83QP5UdRTNVLYoV5S+yR5ac7fVWUZmLbq7tawyuCu0Dw24M9E1BSnpSc="
        val randomUUID = UUID.randomUUID()
        val gameProfile = GameProfile(randomUUID, randomUUID.toString().replace("-", ""))

        gameProfile.properties.put("textures", Property("textures", textures, signature))

        this.type = Material.PLAYER_HEAD
        val damageable = itemMetaData.invoke() as? Damageable ?: throw IllegalStateException("item is not player head")
        damageable.damage = 3

        val skullMeta = damageable as SkullMeta?
        runCatching {
            val profileField = skullMeta!!.javaClass.getDeclaredField("profile")
            profileField.isAccessible = true
            profileField.set(skullMeta, gameProfile)
        }
        this.setItemMeta(skullMeta)

        return this
    }


    fun makeEnchant() : ItemUtils {
        val itemMeta = itemMetaData.invoke() ?: return this
        itemMeta.addEnchant(Enchantment.DURABILITY, 1, true)
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        this.setItemMeta(itemMeta)

        return this
    }

    fun setMeta(function: (ItemMeta) -> Unit) : ItemUtils {
        val itemMeta = itemMetaData.invoke() ?: return this
        function.invoke(itemMeta)
        this.setItemMeta(itemMeta)

        return this
    }

    /**
     *
     * メタデータアイテムへ保存
     *
     */

    fun setKey(key: String, value: Any) : ItemUtils {
        val nameSpacedKey = NamespacedKey(plugin, key)
        val itemMeta = itemMetaData.invoke() ?: return this
        val container = itemMeta.persistentDataContainer
        when (value) {
            is Boolean -> { container.set(nameSpacedKey, PersistentDataType.BOOLEAN, value) }
            is Int -> { container.set(nameSpacedKey, PersistentDataType.INTEGER, value) }
            is IntArray -> { container.set(nameSpacedKey, PersistentDataType.INTEGER_ARRAY, value) }
            is Long -> { container.set(nameSpacedKey, PersistentDataType.LONG, value) }
            is LongArray -> { container.set(nameSpacedKey, PersistentDataType.LONG_ARRAY, value) }
            is Byte -> { container.set(nameSpacedKey, PersistentDataType.BYTE, value) }
            is ByteArray -> { container.set(nameSpacedKey, PersistentDataType.BYTE_ARRAY, value) }
            is Short -> { container.set(nameSpacedKey, PersistentDataType.SHORT, value) }
            is Double -> { container.set(nameSpacedKey, PersistentDataType.DOUBLE, value) }
            is Float -> { container.set(nameSpacedKey, PersistentDataType.FLOAT, value) }
            is String -> { container.set(nameSpacedKey, PersistentDataType.STRING, value) }
            else -> { throw IllegalArgumentException("取り扱いが不明なデータ型です。 " + value::class.java.typeName) }
        }
        this.setItemMeta(itemMeta)
        return this
    }
    fun hasKey(key: String, type: PersistentDataType<*, *>) : Boolean {
        val nameSpacedKey = NamespacedKey(plugin, key)
        val itemMeta = itemMetaData.invoke() ?: return false
        return itemMeta.persistentDataContainer.has(nameSpacedKey, type)
    }
    fun getKey(key: String, type: PersistentDataType<*, *>) : Any? {
        val nameSpacedKey = NamespacedKey(plugin, key)
        val itemMeta = itemMetaData.invoke() ?: return null
        return itemMeta.persistentDataContainer.get(nameSpacedKey, type)
    }
    fun removeKey(key: String) : ItemUtils {
        val nameSpacedKey = NamespacedKey(plugin, key)
        val itemMeta = itemMetaData.invoke() ?: return this
        itemMeta.persistentDataContainer.remove(nameSpacedKey)
        this.setItemMeta(itemMeta)
        return this
    }

    fun hasBoolean(key: String) : Boolean = hasKey(key, PersistentDataType.BOOLEAN)
    fun getBoolean(key: String) : Boolean? = getKey(key, PersistentDataType.BOOLEAN) as? Boolean

    fun hasInt(key: String) : Boolean = hasKey(key, PersistentDataType.INTEGER)
    fun getInt(key: String) : Int? = getKey(key, PersistentDataType.INTEGER) as? Int

    fun hasIntArray(key: String) : Boolean = hasKey(key, PersistentDataType.INTEGER_ARRAY)
    fun getIntArray(key: String) : IntArray? = getKey(key, PersistentDataType.INTEGER_ARRAY) as? IntArray

    fun hasLong(key: String) : Boolean = hasKey(key, PersistentDataType.LONG)
    fun getLong(key: String) : Long? = getKey(key, PersistentDataType.LONG) as? Long

    fun hasLongArray(key: String) : Boolean = hasKey(key, PersistentDataType.LONG_ARRAY)
    fun getLongArray(key: String) : LongArray? =  getKey(key, PersistentDataType.LONG_ARRAY) as? LongArray

    fun hasByte(key: String) : Boolean = hasKey(key, PersistentDataType.BYTE)
    fun getByte(key: String) : Byte? =  getKey(key, PersistentDataType.BYTE) as? Byte

    fun hasByteArray(key: String) : Boolean = hasKey(key, PersistentDataType.BYTE_ARRAY)
    fun getByteArray(key: String) : ByteArray? =  getKey(key, PersistentDataType.BYTE_ARRAY) as? ByteArray

    fun hasShort(key: String) : Boolean = hasKey(key, PersistentDataType.SHORT)
    fun getShort(key: String) : Short? = getKey(key, PersistentDataType.SHORT) as? Short

    fun hasDouble(key: String) : Boolean = hasKey(key, PersistentDataType.DOUBLE)
    fun getDouble(key: String) : Double? = getKey(key, PersistentDataType.DOUBLE) as? Double

    fun hasFloat(key: String) : Boolean = hasKey(key, PersistentDataType.FLOAT)
    fun getFloat(key: String) : Float? = getKey(key, PersistentDataType.FLOAT) as? Float

    fun hasString(key: String) : Boolean = hasKey(key, PersistentDataType.STRING)
    fun getString(key: String) : String? = getKey(key, PersistentDataType.STRING) as? String

    fun addValue(key: String, value: String) : Boolean {
        val values = getValue(key).toMutableList()
        if (values.any { it.equals(key, true) }) return false

        values.add(value)
        setKey(key, values.joinToString("||"))
        return true
    }
    fun getValue(key: String) : List<String> = getString(key)?.split("||") ?: listOf()
    fun removeValue(key: String, value: String) : Boolean {
        val values = getValue(key).toMutableList()
        if (values.none { it.equals(value, true) }) return false

        values.remove(value)
        setKey(key, values.joinToString("||"))
        return true
    }

    fun setValueAt(key: String, value: String, index: Int) : ItemUtils {
        val values = getValue(key).toMutableList()
        values[index] = value
        setKey(key, values.joinToString("||"))
        return this
    }
    fun getValueAt(key: String, index: Int) : String? = getValue(key).getOrNull(index)
    fun removeValueAt(key: String, index: Int) : Boolean {
        val values = getValue(key).toMutableList()
        if (index > values.size - 1) return false

        values.removeAt(index)
        setKey(key, values.joinToString("||"))
        return true
    }
    fun clearValue(key: String) : ItemUtils {
        removeKey(key)
        return this
    }

    /**
     *
     * ユーティリティ
     *
     */

    private fun String.toColorText() : String = this.replace("[&＆]".toRegex(), "§")
    private fun String.toNonColorText() : String = this.replace("[&＆§][A-FK-ORa-fk-or0-9]".toRegex(), "")

    fun hasItem(inventory: Inventory, itemStack: ItemStack) : Boolean = getItemAmount(inventory, itemStack) > 0

    fun getItemAmount(inventory: Inventory, itemStack: ItemStack) : Int
            = inventory.filterNotNull().filter { it.isSimilar(itemStack) }.sumOf { it.amount }

    fun removeItem(inventory: Inventory, itemStack: ItemStack, amount: Int = 1) : Int {
        var removedAmount = 0
        repeat(amount) {
            for (item in inventory.filterNotNull()) {
                if (itemStack.isSimilar(item)) {
                    item.amount -= 1
                    removedAmount++
                    break
                }
            }
        }
        return removedAmount
    }

}