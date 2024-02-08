package com.github.hiyuuu.config.annotations

/**
 * メインクラスに付与するスペースを管理するアノテーション
 * フィールドに付与された Space アノテーションが優先されます
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigSpace(
    val classSpaceSize: Int,
    val fieldSpaceSize: Int,
)