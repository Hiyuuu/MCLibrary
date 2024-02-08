package com.github.hiyuuu.config.annotations

/**
 * インラインコメントアウトを変更するアノテーション
 * フィールドアノテーションが優先されます
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class InlineComment(
    vararg val message: String
)