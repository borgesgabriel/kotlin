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


package org.jetbrains.kotlin.incremental

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.CompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunner
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.util.*

fun<Target> compileChanged(
        kotlinPaths: KotlinPaths,
        moduleName: String,
        isTest: Boolean,
        targets: Iterable<Target>,
        getDependencies: (Target) -> Iterable<Target>,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        additionalArguments: Iterable<String>,
        outputDir: File,
        sourcesToCompile: List<File>,
        javaSourceRoots: Iterable<File>,
        classpath: Iterable<File>,
        friendDirs: Iterable<File>,
        compilationCanceledStatus: CompilationCanceledStatus,
        getIncrementalCache: (Target) -> IncrementalCacheImpl<Target>,
        lookupTracker: LookupTracker,
        getTargetId: Target.() -> TargetId,
        messageCollector: MessageCollector, outputItemCollector: OutputItemsCollectorImpl
)
{
    val moduleFile = makeModuleFile(moduleName, isTest, outputDir, sourcesToCompile, javaSourceRoots, classpath, friendDirs)
    println("module file created: $moduleFile")
    println(moduleFile.readText())

    val incrementalCaches = getIncrementalCaches(targets, getDependencies, getIncrementalCache, getTargetId)
    val environment = createCompileEnvironment(kotlinPaths, incrementalCaches, lookupTracker, compilationCanceledStatus)

    commonArguments.verbose = true // Make compiler report source to output files mapping

    KotlinCompilerRunner.runK2JvmCompiler(commonArguments, k2JvmArguments, additionalArguments, messageCollector, environment, moduleFile, outputItemCollector)

    moduleFile.delete()
}


fun getJavaSourceRoots(sources: Iterable<File>, roots: Iterable<File>): Iterable<File> =
        sources
            .filter { it.isJavaFile() }
            .map { findSrcDirRoot(it, roots) }
            .filterNotNull()

private fun File.isJavaFile() = extension.equals(JavaFileType.INSTANCE.defaultExtension, ignoreCase = true)


private fun makeModuleFile(name: String, isTest: Boolean, outputDir: File, sourcesToCompile: List<File>, javaSourceRoots: Iterable<File>, classpath: Iterable<File>, friendDirs: Iterable<File>): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
            name,
            outputDir.absolutePath,
            sourcesToCompile,
            javaSourceRoots.map { JvmSourceRoot(it) },
            classpath,
            "java-production",
            isTest,
            // this excludes the output directories from the class path, to be removed for true incremental compilation
            setOf(outputDir),
            friendDirs
    )

    val scriptFile = File.createTempFile("kjps", StringUtil.sanitizeJavaIdentifier(name) + ".script.xml")

    FileUtil.writeToFile(scriptFile, builder.asText().toString())

    return scriptFile
}


private fun findSrcDirRoot(file: File, roots: Iterable<File>): File? {
    for (root in roots) {
        if (FileUtil.isAncestor(root, file, false)) {
            return root
        }
    }
    return null
}

private fun createCompileEnvironment(
        kotlinPaths: KotlinPaths,
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus
): CompilerEnvironment {
    val compilerServices = Services.Builder()
            .register(javaClass<IncrementalCompilationComponents>(), IncrementalCompilationComponentsImpl(incrementalCaches, lookupTracker))
            .register(javaClass<CompilationCanceledStatus>(), compilationCanceledStatus)
            .build()

    return CompilerEnvironment.getEnvironmentFor(
            kotlinPaths,
            { className ->
                className.startsWith("org.jetbrains.kotlin.load.kotlin.incremental.components.")
                || className.startsWith("org.jetbrains.kotlin.incremental.components.")
                || className == "org.jetbrains.kotlin.config.Services"
                || className.startsWith("org.apache.log4j.") // For logging from compiler
                || className == "org.jetbrains.kotlin.progress.CompilationCanceledStatus"
                || className == "org.jetbrains.kotlin.progress.CompilationCanceledException"
                || className == "org.jetbrains.kotlin.modules.TargetId"
            },
            compilerServices
    )
}


fun makeLookupTracker(parentLookupTracker: LookupTracker = LookupTracker.DO_NOTHING): LookupTracker =
        if (IncrementalCompilation.isExperimental()) LookupTrackerImpl(parentLookupTracker)
        else parentLookupTracker


private fun<Target> getIncrementalCaches(
        targets: Iterable<Target>,
        getDependencies: (Target) -> Iterable<Target>,
        getCache: (Target) -> IncrementalCacheImpl<Target>,
        getTargetId: Target.() -> TargetId
): Map<TargetId, IncrementalCacheImpl<Target>>
{
    val dependents = targets.keysToMap { hashSetOf<Target>() }
    val targetsWithDependents = targets.toHashSet()

    for (target in targets) {
        for (dependency in getDependencies(target)) {
            if (dependency !in targets) continue

            dependents[dependency]!!.add(target)
            targetsWithDependents.add(target)
        }
    }

    val caches = targetsWithDependents.keysToMap { getCache(it) }

    for ((target, cache) in caches) {
        dependents[target]?.forEach {
            cache.addDependentCache(caches[it]!!)
        }
    }

    return caches.mapKeys { it.key.getTargetId() }
}


fun<Target> updateKotlinIncrementalCache(
        targets: Iterable<Target>,
        compilationErrors: Boolean,
        getIncrementalCache: (Target) -> IncrementalCacheImpl<Target>,
        generatedFiles: List<GeneratedFile<Target>>
): CompilationResult {

    assert(IncrementalCompilation.isEnabled()) { "updateKotlinIncrementalCache should not be called when incremental compilation disabled" }

    targets.forEach { getIncrementalCache(it).saveCacheFormatVersion() }

    var changesInfo = CompilationResult.NO_CHANGES
    for (generatedFile in generatedFiles) {
        val ic = getIncrementalCache(generatedFile.target)
        when {
            generatedFile is GeneratedJvmClass<Target> -> changesInfo += ic.saveFileToCache(generatedFile)
            generatedFile.outputFile.isModuleMappingFile() -> changesInfo += ic.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
        }
    }

    if (!compilationErrors) {
        targets.forEach {
            val newChangesInfo = getIncrementalCache(it).clearCacheForRemovedClasses()
            changesInfo += newChangesInfo
        }
    }

    return changesInfo
}


fun LookupStorage.update(
        lookupTracker: LookupTracker,
        filesToCompile: Iterable<File>,
        removedFiles: Iterable<File>
) {
    if (!IncrementalCompilation.isExperimental()) return

    if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker.javaClass}")

    filesToCompile.forEach { this.removeLookupsFrom(it) }
    removedFiles.forEach { this.removeLookupsFrom(it) }

    lookupTracker.lookups.entrySet().forEach { this.add(it.key, it.value) }
}


fun<Target> getGeneratedFiles(
        targets: Collection<Target>,
        representativeTarget: Target,
        getSources: (Target) -> Iterable<File>,
        getOutputDir: (Target) -> File?,
        outputItemCollector: OutputItemsCollectorImpl
): List<GeneratedFile<Target>> {
    // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
    val sourceToTarget = HashMap<File, Target>()
    if (targets.size > 1) {
        for (target in targets) {
            for (file in getSources(target)) {
                sourceToTarget.put(file, target)
            }
        }
    }

    val result = ArrayList<GeneratedFile<Target>>()

    for (outputItem in outputItemCollector.outputs) {
        val sourceFiles = outputItem.sourceFiles
        val outputFile = outputItem.outputFile
        val target =
                sourceFiles.firstOrNull()?.let { sourceToTarget[it] } ?:
                targets.filter { getOutputDir(it)?.let { outputFile.startsWith(it) } ?: false }.singleOrNull() ?:
                representativeTarget

        if (outputFile.getName().endsWith(".class")) {
            result.add(GeneratedJvmClass(target, sourceFiles, outputFile))
        }
        else {
            result.add(GeneratedFile(target, sourceFiles, outputFile))
        }
    }
    return result
}


fun CompilationResult.dirtyFiles(lookupStorage: LookupStorage) =
    // TODO group by fqName?
    changes.mapNotNull { it as? ChangeInfo.MembersChanged }
           .flatMap { change ->
               change.names.asSequence()
                       .flatMap { lookupStorage.get(LookupSymbol(it, change.fqName.asString())).asSequence() }
                       .map(::File)
           }


public open class GeneratedFile<Target> internal constructor(
        val target: Target,
        val sourceFiles: Collection<File>,
        val outputFile: File
)

class GeneratedJvmClass<Target> (
        target: Target,
        sourceFiles: Collection<File>,
        outputFile: File
) : GeneratedFile<Target>(target, sourceFiles, outputFile) {
    val outputClass = LocalFileKotlinClass.create(outputFile).sure {
        "Couldn't load KotlinClass from $outputFile; it may happen because class doesn't have valid Kotlin annotations"
    }
}
