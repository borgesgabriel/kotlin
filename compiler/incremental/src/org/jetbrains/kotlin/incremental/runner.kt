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
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jps.incremental.IncrementalCompilationComponentsImpl
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File


private fun<Target> compileChanged(
        targets: Iterable<Target>,
        getDependencies: (Target) -> Iterable<Target>,
        name: String,
        commonArguments: CommonCompilerArguments,
        k2JvmArguments: K2JVMCompilerArguments,
        outputDir: File,
        sourcesToCompile: List<File>,
        allSources: List<File>,
        roots: Iterable<File>,
        classpath: Iterable<File>,
        friendDirs: Iterable<File>,
        compilationCanceledStatus: CompilationCanceledStatus,
        getIncrementalCache: (Target) -> IncrementalCacheImpl<Target>,
        getTargetId: Target.() -> TargetId,
        compilerSettings: CompilerSettings,
        messageCollector: MessageCollector)
{
    val outputItemCollector = OutputItemsCollectorImpl()
    val moduleFile = makeModuleFile(name, outputDir, sourcesToCompile, allSources, roots, classpath, friendDirs)

    // TODO: take as an argument - it should come from use site (may be already merged with common args)

    val incrementalCaches = getIncrementalCaches(targets, getDependencies, getIncrementalCache, getTargetId)
    val lookupTracker = getLookupTracker()
    val environment = createCompileEnvironment(incrementalCaches, lookupTracker, compilationCanceledStatus)

    KotlinCompilerRunner.runK2JvmCompiler(commonArguments, k2JvmArguments, compilerSettings, messageCollector, environment, moduleFile, outputItemCollector)

    moduleFile.delete()
}


private fun makeModuleFile(name: String, outputDir: File, sourcesToCompile: List<File>, allSources: List<File>, roots: Iterable<File>, classpath: Iterable<File>, friendDirs: Iterable<File>): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
            name,
            outputDir.absolutePath,
            sourcesToCompile,
            getJavaSourceRoots(allSources, roots).map { JvmSourceRoot(it) },
            classpath,
            "java-production",
            false,
            // this excludes the output directories from the class path, to be removed for true incremental compilation
            setOf(outputDir),
            friendDirs
    )

    val scriptFile = File.createTempFile("kjps", StringUtil.sanitizeJavaIdentifier(name) + ".script.xml")

    FileUtil.writeToFile(scriptFile, builder.asText().toString())

    return scriptFile
}


private fun getJavaSourceRoots(sources: Iterable<File>, roots: Iterable<File>): Set<File> =
        sources
            .filter { it.isJavaFile() }
            .map { findSrcDirRoot(it, roots) }
            .filterNotNull()
            .toSet()

private fun File.isJavaFile() = extension.equals(JavaFileType.INSTANCE.defaultExtension, ignoreCase = true)


private fun findSrcDirRoot(file: File, roots: Iterable<File>): File? {
    for (root in roots) {
        if (FileUtil.isAncestor(root, file, false)) {
            return root
        }
    }
    return null
}

private fun createCompileEnvironment(
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus
): CompilerEnvironment {
    val compilerServices = Services.Builder()
            .register(javaClass<IncrementalCompilationComponents>(), IncrementalCompilationComponentsImpl(incrementalCaches, lookupTracker))
            .register(javaClass<CompilationCanceledStatus>(), compilationCanceledStatus)
            .build()

    return CompilerEnvironment.getEnvironmentFor(
            PathUtil.getKotlinPathsForJpsPluginOrJpsTests(),
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


private fun getLookupTracker(parentLookupTracker: LookupTracker = LookupTracker.DO_NOTHING): LookupTracker =
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


private fun<TargetId> updateKotlinIncrementalCache(
        targets: Iterable<TargetId>,
        compilationErrors: Boolean,
        incrementalCaches: Map<TargetId, IncrementalCacheImpl<TargetId>>,
        generatedFiles: List<GeneratedFile<TargetId>>
): CompilationResult {

    assert(IncrementalCompilation.isEnabled()) { "updateKotlinIncrementalCache should not be called when incremental compilation disabled" }

    targets.forEach { incrementalCaches[it]!!.saveCacheFormatVersion() }

    var changesInfo = CompilationResult.NO_CHANGES
    for (generatedFile in generatedFiles) {
        val ic = incrementalCaches[generatedFile.target]!!
        val newChangesInfo =
                if (generatedFile is GeneratedJvmClass<TargetId>) {
                    ic.saveFileToCache(generatedFile)
                }
                else if (generatedFile.outputFile.isModuleMappingFile()) {
                    ic.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
                }
                else {
                    continue
                }

        changesInfo += newChangesInfo
    }

    if (!compilationErrors) {
        incrementalCaches.values().forEach {
            val newChangesInfo = it.clearCacheForRemovedClasses()
            changesInfo += newChangesInfo
        }
    }

    return changesInfo
}


private fun LookupStorage.update(
        lookupTracker: LookupTracker,
        filesToCompile: Sequence<File>,
        removedFiles: Sequence<File>
) {
    if (!IncrementalCompilation.isExperimental()) return

    if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker.javaClass}")

    filesToCompile.forEach { this.removeLookupsFrom(it) }
    removedFiles.forEach { this.removeLookupsFrom(it) }

    lookupTracker.lookups.entrySet().forEach { this.add(it.key, it.value) }
}

