package com.github.hiyuuu.configutils

import com.github.hiyuuu.config.ConfigUtils
import com.github.hiyuuu.config.annotations.*

class ConfigReader {

    companion object {

        @JvmStatic
        fun <T> configToClass(conf: ConfigUtils, anyClass: T) : T? {

            // スーパークラスの情報を取得
            val anyClazz = (anyClass ?: return null)::class.java
            val classes  = arrayListOf(KeyInstanceClass("", anyClass, anyClazz))

            // スーパークラスのアノテーションを取得
            val configParserAnno = runCatching { anyClazz.getDeclaredAnnotation(ConfigParser::class.java) }.getOrNull() ?: return null
            val headerCommentAnno = runCatching { anyClazz.getAnnotation(HeaderComment::class.java) }.getOrNull()
            val footerCommentAnno = runCatching { anyClazz.getAnnotation(FooterComment::class.java) }.getOrNull()
            val configSpaceAnno = runCatching { anyClazz.getAnnotation(ConfigSpace::class.java) }.getOrNull()

            // ヘッダー
            val headerSpaceSize = headerCommentAnno?.spaceSize ?: configSpaceAnno?.classSpaceSize ?: 2
            val headerSpaceList = (1..headerSpaceSize).map { "#SPACE#" }.toTypedArray()
            headerCommentAnno?.run { conf.options().setHeader(listOf(*headerSpaceList, *this.message)) }

            // フッター
            val footerSpaceSize = footerCommentAnno?.spaceSize ?: configSpaceAnno?.classSpaceSize ?: 1
            val footerSpaceList = (1..footerSpaceSize).map { "#SPACE#" }.toTypedArray()
            footerCommentAnno?.run { conf.options().setFooter(listOf(*footerSpaceList, *this.message)) }

            val deletePathQueue = ArrayList<String>()
            while (classes.size > 0) {

                // クラスの情報復元
                val lastKeyClass = classes.removeLastOrNull() ?: break
                val classSection = lastKeyClass.section
                val classInstance = lastKeyClass.instance
                val clazz = lastKeyClass.clazz

                clazz.declaredFields.forEach { f ->

                    // クラスのアノテーションを取得
                    val classCommentAnno = runCatching { clazz.getAnnotation(Comment::class.java) }.getOrNull()
                    val classInlineCommentAnno = runCatching { clazz.getAnnotation(InlineComment::class.java) }.getOrNull()
                    val classSpaceAnno = runCatching { clazz.getAnnotation(Space::class.java) }.getOrNull()
                    val classCopyAnno = runCatching { clazz.getAnnotation(Copy::class.java) }.getOrNull()
                    val classDeleteAnno = runCatching { clazz.getAnnotation(Delete::class.java) }.getOrNull()

                    // フィールドのアノテーションを取得
                    val commentAnno = runCatching { f.getAnnotation(Comment::class.java) }.getOrNull()
                    val inlineCommentAnno = runCatching { f.getAnnotation(InlineComment::class.java) }.getOrNull()
                    val spaceAnno = runCatching { f.getAnnotation(Space::class.java) }.getOrNull()
                    val copyAnno = runCatching { f.getAnnotation(Copy::class.java) }.getOrNull()
                    val deleteAnno = runCatching { f.getAnnotation(Delete::class.java) }.getOrNull()

                    // フィールドの情報取得
                    f.isAccessible = true
                    val fieldName = f.name
                    var section = "${classSection}${if (classSection.isNotBlank()) "." else ""}$fieldName"
                    val obj = f.get(classInstance)

                    // クラスを取得
                    if (f.type.isAnnotationPresent(ConfigParser::class.java)) {

                        val subClassCommentAnno = runCatching { f.getAnnotation(Comment::class.java) }.getOrNull()
                        val subClassInlineCommentAnno = runCatching { f.getAnnotation(InlineComment::class.java) }.getOrNull()
                        val subClassSpaceAnno = runCatching { f.getAnnotation(Space::class.java) }.getOrNull()

                        // セクション生成
                        if (!conf.isConfigurationSection(classSection))
                            conf.createSection(classSection)

                        // コンフィグ - サブクラスコメントアウト
                        val subClassSpaceSize = subClassSpaceAnno?.size ?: configSpaceAnno?.classSpaceSize ?: 1
                        val spaceList = (1..subClassSpaceSize).map { "#SPACE#" }.toTypedArray()
                        conf.setComments(classSection, listOf(*spaceList, *subClassCommentAnno?.message ?: arrayOf()))
                        conf.setInlineComments(classSection, subClassInlineCommentAnno?.message?.toList() ?: listOf())

                        val sectionName = runCatching { f.getAnnotation(SectionName::class.java) }.getOrNull()
                        if (sectionName != null && sectionName.name.isNotBlank()) {
                            section = section.replace("(.*(?:^|\\.))(.*?)\$".toRegex(), "$1${sectionName.name}")
                        }

                        val keyInstanceClass = KeyInstanceClass(section, obj, f.type)
                        classes.add(keyInstanceClass)
                        return@forEach
                    }

                    // 削除キュー
                    deleteAnno?.run { deletePathQueue.add(section) }
                    classDeleteAnno?.run { deletePathQueue.add(classSection) }

                    if (classCopyAnno != null) {

                        val getSection = conf.getConfigurationSection(classCopyAnno.fromPath)
                        val getObj = conf.get(classCopyAnno.fromPath)
                        val getComments = conf.getComments(classCopyAnno.fromPath)
                        val getInlineComments = conf.getInlineComments(classCopyAnno.fromPath)

                        conf.createSection(classSection, getSection?.getValues(true) ?: mapOf<String, Any>())
                        conf.setComments(classSection, getComments)
                        conf.setInlineComments(classSection, getInlineComments)
                    }

                    if (copyAnno != null) {

                        val getObj = conf.get(copyAnno.fromPath)

                        conf.set(section, getObj)
                        f.set(classInstance, getObj)

                    } else if (conf.isSet(section)) {

                        // 情報取得してクラスに代入
                        val getObj = conf.get(section)
                        f.set(classInstance, getObj)

                    } else if (!conf.isSet(section) && !conf.isConfigurationSection(section)) {

                        // セクション生成
                        if (!conf.isConfigurationSection(classSection))
                            conf.createSection(classSection)

                        // 初期変数設定
                        conf.set(section, obj)

                        // コンフィグ - フィールドコメントアウト
                        val fieldSpaceSize = spaceAnno?.size ?: configSpaceAnno?.fieldSpaceSize ?: 1
                        val spaceList = (1..fieldSpaceSize).map { "#SPACE#" }.toTypedArray()
                        conf.setComments(section, listOf(*spaceList, *commentAnno?.message ?: arrayOf()))
                        conf.setInlineComments(section, inlineCommentAnno?.message?.toList() ?: listOf())

                        // コンフィグ - セクションコメントアウト
                        val classSpaceSize = classSpaceAnno?.size ?: configSpaceAnno?.classSpaceSize ?: 2
                        val classSpaceList = (1..classSpaceSize).map { "#SPACE#" }.toTypedArray()
                        conf.setComments(classSection, listOf(*classSpaceList, *classCommentAnno?.message ?: arrayOf()))
                        conf.setInlineComments(classSection, classInlineCommentAnno?.message?.toList() ?: listOf())
                    }
                }

            }

            // 削除キューの処理
            deletePathQueue.forEach { p ->
                conf.set(p, null)
                conf.setComments(p, null)
                conf.setInlineComments(p, null)
            }

            // 保存
            conf.saveConfig()
            return anyClass
        }

        @JvmStatic
        fun <T> classToConfig(conf: ConfigUtils, anyClass: T) : T? {

            // スーパークラスの情報を取得
            val anyClazz = (anyClass ?: return null)::class.java
            val classes  = arrayListOf(KeyInstanceClass("", anyClass, anyClazz))

            // スーパークラスのアノテーションを取得
            val configParserAnno = runCatching { anyClazz.getDeclaredAnnotation(ConfigParser::class.java) }.getOrNull() ?: return null

            var isEdit = false
            while (classes.size > 0) {

                // クラスの情報復元
                val lastKeyClass = classes.removeLastOrNull() ?: break
                val classSection = lastKeyClass.section
                val classInstance = lastKeyClass.instance
                val clazz = lastKeyClass.clazz

                clazz.declaredFields.forEach { f ->

                    // フィールドの情報取得
                    f.isAccessible = true
                    val fieldName = f.name
                    var section = "${classSection}${if (classSection.isNotBlank()) "." else ""}$fieldName"
                    val obj = f.get(classInstance)

                    // クラスを取得
                    if (f.type.isAnnotationPresent(ConfigParser::class.java)) {

                        val sectionName = runCatching { f.getAnnotation(SectionName::class.java) }.getOrNull()
                        if (sectionName != null && sectionName.name.isNotBlank()) {
                            section = section.replace("(.*(?:^|\\.))(.*?)\$".toRegex(), "$1${sectionName.name}")
                        }

                        val keyInstanceClass = KeyInstanceClass(section, obj, f.type)
                        classes.add(keyInstanceClass)
                        return@forEach
                    }

                    val getObj = conf.get(section)
                    if (getObj != null && getObj != obj) {
                        conf.set(section, obj)
                        isEdit = true
                    }
                }
            }

            // 保存
            if (isEdit) conf.saveConfig()
            return anyClass
        }

        @JvmStatic
        fun <T> isDiff(conf: ConfigUtils, anyClass: T) : Boolean? {

            // スーパークラスの情報を取得
            val anyClazz = (anyClass ?: return null)::class.java
            val classes  = arrayListOf(KeyInstanceClass("", anyClass, anyClazz))

            // スーパークラスのアノテーションを取得
            val configParserAnno = runCatching { anyClazz.getDeclaredAnnotation(ConfigParser::class.java) }.getOrNull() ?: return null

            var isEdit = false
            while (classes.size > 0) {

                // クラスの情報復元
                val lastKeyClass = classes.removeLastOrNull() ?: break
                val classSection = lastKeyClass.section
                val classInstance = lastKeyClass.instance
                val clazz = lastKeyClass.clazz

                clazz.declaredFields.forEach { f ->

                    // フィールドの情報取得
                    f.isAccessible = true
                    val fieldName = f.name
                    var section = "${classSection}${if (classSection.isNotBlank()) "." else ""}$fieldName"
                    val obj = f.get(classInstance)

                    // クラスを取得
                    if (f.type.isAnnotationPresent(ConfigParser::class.java)) {

                        val sectionName = runCatching { f.getAnnotation(SectionName::class.java) }.getOrNull()
                        if (sectionName != null && sectionName.name.isNotBlank()) {
                            section = section.replace("(.*(?:^|\\.))(.*?)\$".toRegex(), "$1${sectionName.name}")
                        }

                        val keyInstanceClass = KeyInstanceClass(section, obj, f.type)
                        classes.add(keyInstanceClass)
                        return@forEach
                    }

                    val getObj = conf.get(section)
                    if (getObj != null && getObj != obj) { isEdit = true }
                }
            }

            return isEdit
        }

        @JvmStatic
        fun replaceSpaces(conf: ConfigUtils) {

            val yamlFile = conf.file
            val text = yamlFile.readText()
            val lines = text
                .split("\n")
                .map {
                    it.replace("(?: +|)# #SPACE#.*\$".toRegex(), "")
                }

            // 書き込み
            yamlFile.writeText(lines.joinToString("\n").trimEnd('\n'))
        }
    }

}
