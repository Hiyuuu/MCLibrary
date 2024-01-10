package com.github.hiyuuu.utils

/**
 * ハッシュマップを使用上で便利するクラス
 */
interface HashMapUtils {

    /**
     * ハッシュマップのキーを最新に置き換える
     * @param newKey 新しいキー
     * @param keyCondition 新しいキーに置き換える条件式
     */
    fun <K, V> HashMap<K, V>.replaceHashMapKey(newKey: K, keyCondition: (K) -> Boolean, valueCondition: (V) -> V = { it })
        = this.forEach { k, v ->
            val value = valueCondition.invoke(v)
            if (keyCondition.invoke(k)) this[newKey] = value else this[k] = value
        }
}