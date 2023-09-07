package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HeaderComment(
    vararg val message: String,
    val spaceSize: Int = 1
)
