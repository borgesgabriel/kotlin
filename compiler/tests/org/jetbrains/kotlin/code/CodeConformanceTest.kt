/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import java.io.File
import java.util.*
import java.util.regex.Pattern

public class CodeConformanceTest : TestCase() {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val SOURCES_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)")
        private val EXCLUDED_FILES_AND_DIRS = listOf(
                "android.tests.dependencies",
                "core/reflection.jvm/src/kotlin/reflect/jvm/internal/pcollections",
                "libraries/tools/kotlin-reflect/target/copied-sources",
                "dependencies",
                "js/js.translator/qunit/qunit.js",
                "libraries/tools/kotlin-js-tests/src/test/web/qunit.js",
                "out",
                "dist",
                "ideaSDK",
                "libraries/tools/kotlin-gradle-plugin-core/gradle_api_jar/build/tmp",
                "libraries/tools/kotlin-maven-plugin/target/generated-sources",
                "compiler/testData/psi/kdoc",
                "compiler/tests/org/jetbrains/kotlin/code/CodeConformanceTest.kt"
        ).map { File(it) }
    }

    public fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    public fun testNoBadSubstringsInProjectCode() {
        class TestData(val message: String, val filter: (String) -> Boolean) {
            val result: MutableList<File> = ArrayList()
        }

        val atAuthorPattern = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL)

        val tests = listOf(
                TestData(
                        "%d source files contain @author javadoc tag.\nPlease remove them or exclude in this test:\n%s",
                        { source ->
                            // substring check is an optimization
                            "@author" in source && atAuthorPattern.matcher(source).find()
                        }
                ),
                TestData(
                        "%d source files use something from com.beust.jcommander.internal package.\n" +
                        "This code won't work when there's no TestNG in the classpath of our IDEA plugin, " +
                        "because there's only an optional dependency on testng.jar.\n" +
                        "Most probably you meant to use Guava's Lists, Maps or Sets instead. " +
                        "Please change references in these files to com.google.common.collect:\n%s",
                        { source ->
                            "com.beust.jcommander.internal" in source
                        }
                ),
                TestData(
                        "%d source files contain references to package org.jetbrains.jet.\n" +
                        "Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                        "Please consider changing the package in these files:\n%s",
                        { source ->
                            "org.jetbrains.jet" in source
                        }
                ),
                TestData(
                        "%d source files contain references to package kotlin.reflect.jvm.internal.impl.\n" +
                        "This package contains internal reflection implementation and is a result of a " +
                        "post-processing of kotlin-reflect.jar by jarjar.\n" +
                        "Most probably you meant to use classes from org.jetbrains.kotlin.** or com.google.protobuf.**.\n" +
                        "Please change references in these files or exclude them in this test:\n%s",
                        { source ->
                            "kotlin.reflect.jvm.internal.impl" in source
                        }
                )
        )

        for (sourceFile in FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, File("."))) {
            if (EXCLUDED_FILES_AND_DIRS.any { FileUtil.isAncestor(it, sourceFile, false) }) continue

            val source = sourceFile.readText()
            for (test in tests) {
                if (test.filter(source)) test.result.add(sourceFile)
            }
        }

        if (tests.flatMap { it.result }.isNotEmpty()) {
            fail(StringBuilder {
                for (test in tests) {
                    if (test.result.isNotEmpty()) {
                        append(test.message.format(test.result.size(), test.result.joinToString("\n")))
                        appendln()
                        appendln()
                    }
                }
            }.toString())
        }
    }
}
