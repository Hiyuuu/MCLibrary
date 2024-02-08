package com.github.hiyuuu.config.annotations

/**
 * フィールドに重さを付けます。
 * 数字が大きいほどコンフィグでは上から出力されます
 * 指定しない場合は、優先度が最も低くなります
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Weight(
    val weight: Int = 0
)