package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class InlineComment(
    vararg val message: String
)
