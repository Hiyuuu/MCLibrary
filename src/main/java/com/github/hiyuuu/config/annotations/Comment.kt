package com.github.hiyuuu.config.annotations

/**
 * コメントアウトを変更するアノテーション
 * フィールドアノテーションが優先されます
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(
    vararg val message: String
)