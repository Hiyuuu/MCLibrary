package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigSpace(
    val classSpaceSize: Int,
    val fieldSpaceSize: Int,
)
