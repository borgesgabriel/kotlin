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

package org.jetbrains.kotlin.idea.versions

import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses
import org.jetbrains.kotlin.load.java.AbiVersionUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.*
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

public object KotlinAbiVersionIndex : KotlinAbiVersionIndexBase<KotlinAbiVersionIndex>(KotlinAbiVersionIndex::class.java) {

    override fun getIndexer() = INDEXER

    override fun getInputFilter() = FileBasedIndex.InputFilter() { file -> file.fileType == StdFileTypes.CLASS }

    override fun getVersion() = VERSION

    private val VERSION = 2

    private val kotlinAnnotationsDesc = setOf(
            KOTLIN_CLASS,
            KOTLIN_FILE_FACADE,
            KOTLIN_MULTIFILE_CLASS
    ).map { asmDescByFqNameWithoutInnerClasses(it) }

    private val INDEXER = DataIndexer<BinaryVersion, Void, FileContent>() { inputData: FileContent ->
        var version: BinaryVersion? = null
        var annotationPresent = false

        tryBlock(inputData) {
            val classReader = ClassReader(inputData.content)
            classReader.accept(object : ClassVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    if (!kotlinAnnotationsDesc.contains(desc)) {
                        return null
                    }
                    annotationPresent = true
                    return object : AnnotationVisitor(Opcodes.ASM5) {
                        override fun visit(name: String, value: Any) {
                            when (name) {
                                VERSION_FIELD_NAME -> if (value is IntArray) {
                                    version = BinaryVersion.create(value)
                                }
                                OLD_ABI_VERSION_FIELD_NAME -> if (version == null && value is Int) {
                                    version = BinaryVersion.create(0, value, 0)
                                }
                            }
                        }
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }

        if (annotationPresent && version == null) {
            // No version at all because the class is too old, or version is set to something weird
            version = AbiVersionUtil.INVALID_VERSION
        }

        if (version != null) mapOf(version!! to null) else mapOf()
    }
}
