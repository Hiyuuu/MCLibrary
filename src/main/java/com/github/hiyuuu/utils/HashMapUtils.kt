package com.github.hiyuuu.utils

/**
 * ハッシュマップを使用上で便利するクラス
 */
interface HashMapUtils {

    /**
     * ハッシュマップのキーを最新に置き換える
     * @param newKey 新しいキー
     * @param conditions 新しいキーに置き換える条件式
     */
    fun <K, V> HashMap<K, V>.replaceHashMapKey(newKey: K, conditions: (K) -> Boolean)
            = this.keys.filter(conditions).forEach { v -> this.remove(v)?.let { this[newKey] = it } }

}