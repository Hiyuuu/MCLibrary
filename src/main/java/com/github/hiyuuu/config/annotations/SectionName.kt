package com.github.hiyuuu.config.annotations

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SectionName(
    val name: String
)
