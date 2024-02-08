package com.github.hiyuuu.config.annotations

/**
 * ルートのセクションパスを変更するアノテーション
 * 変更した場合はパスが自動的にマッピングされます
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SectionName(
    val path: String
)