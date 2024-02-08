package com.github.hiyuuu.config.annotations

/**
 * サブクラスに付与するアノテーション
 * サブクラスに付与することで、フィールド内のクラスが認識されます
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubConfig