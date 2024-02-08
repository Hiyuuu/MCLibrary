package com.github.hiyuuu.config.annotations

/**
 * 指定したパスをトップへ移動しマッピングするアノテーション
 * 変更した場合はパスが自動的にマッピングされます
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable()
annotation class PathMapping(
    val onlyUseArgumentPath: String
)