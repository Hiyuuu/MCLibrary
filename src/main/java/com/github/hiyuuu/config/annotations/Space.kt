package com.github.hiyuuu.config.annotations

/**
 * コメントアウトのスペースを設定するアノテーション
 * セクション及びセットのコメントアウトを変更できます
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Space(
    val size: Int = 1
)