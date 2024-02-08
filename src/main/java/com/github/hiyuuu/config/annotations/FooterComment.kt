package com.github.hiyuuu.config.annotations

/**
 * フッターアノテーション
 * コンフィグの最末端にコメントが挿入されます
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FooterComment(
    val spaceSize: Int,
    vararg val message: String
)