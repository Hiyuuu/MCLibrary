package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FooterComment(
    vararg val message: String,
    val spaceSize: Int = 1
)
