package com.github.hiyuuu.config.annotations

/**
 * セクションまたはセットをコピーするアノテーション
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Copy(
    val fromPath : String
)