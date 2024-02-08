package com.github.hiyuuu.config.annotations

/**
 * ヘッダーアノテーション
 * コンフィグの最先端にコメントが挿入されます
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HeaderComment(
    val spaceSize: Int,
    vararg val message: String
)