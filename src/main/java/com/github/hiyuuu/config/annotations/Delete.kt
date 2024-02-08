package com.github.hiyuuu.config.annotations

/**
 * セクションまたはセットを削除するアノテーション
 * 非推奨となったフィールド、サブクラスにも付与可能です
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Delete