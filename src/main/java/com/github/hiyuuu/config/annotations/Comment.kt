package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(
    vararg val message: String
)
