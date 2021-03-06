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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.frontend.di.createContainerForLazyBodyResolve
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import java.util.*

internal class PerFileAnalysisCache(val file: KtFile, val componentProvider: ComponentProvider) {
    private val cache = HashMap<PsiElement, AnalysisResult>()

    private fun lookUp(analyzableElement: KtElement): AnalysisResult? {
        // Looking for parent elements that are already analyzed
        // Also removing all elements whose parents are already analyzed, to guarantee consistency
        val descendantsOfCurrent = arrayListOf<PsiElement>()
        val toRemove = hashSetOf<PsiElement>()

        var result: AnalysisResult? = null
        for (current in analyzableElement.parentsWithSelf) {
            val cached = cache[current]
            if (cached != null) {
                result = cached
                toRemove.addAll(descendantsOfCurrent)
                descendantsOfCurrent.clear()
            }

            descendantsOfCurrent.add(current)
        }

        cache.keySet().removeAll(toRemove)

        return result
    }

    fun getAnalysisResults(element: KtElement): AnalysisResult {
        assert (element.getContainingKtFile() == file) { "Wrong file. Expected $file, but was ${element.getContainingKtFile()}" }

        val analyzableParent = KotlinResolveDataProvider.findAnalyzableParent(element)

        return synchronized<AnalysisResult>(this) {

            val cached = lookUp(analyzableParent)
            if (cached != null) return@synchronized cached

            val result = analyze(analyzableParent)

            cache[analyzableParent] = result

            return@synchronized result
        }
    }

    private fun analyze(analyzableElement: KtElement): AnalysisResult {
        val project = analyzableElement.getProject()
        if (DumbService.isDumb(project)) {
            return AnalysisResult.EMPTY
        }

        try {
            return KotlinResolveDataProvider.analyze(project, componentProvider, analyzableElement)
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: IndexNotReadyException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalysisResult.error(BindingContext.EMPTY, e)
        }
    }
}

private object KotlinResolveDataProvider {
    private val topmostElementTypes = arrayOf<Class<out PsiElement?>?>(
            javaClass<KtNamedFunction>(),
            javaClass<KtAnonymousInitializer>(),
            javaClass<KtProperty>(),
            javaClass<KtImportDirective>(),
            javaClass<KtPackageDirective>(),
            javaClass<KtCodeFragment>(),
            // TODO: Non-analyzable so far, add more granular analysis
            javaClass<KtAnnotationEntry>(),
            javaClass<KtTypeConstraint>(),
            javaClass<KtSuperTypeList>(),
            javaClass<KtTypeParameter>(),
            javaClass<KtParameter>()
    )

    fun findAnalyzableParent(element: KtElement): KtElement {
        if (element is KtFile) return element

        val topmostElement = KtPsiUtil.getTopmostParentOfTypes(element, *topmostElementTypes) as KtElement?
        // parameters and supertype lists are not analyzable by themselves, but if we don't count them as topmost, we'll stop inside, say,
        // object expressions inside arguments of super constructors of classes (note that classes themselves are not topmost elements)
        val analyzableElement = when (topmostElement) {
            is KtAnnotationEntry,
            is KtTypeConstraint,
            is KtSuperTypeList,
            is KtTypeParameter,
            is KtParameter -> PsiTreeUtil.getParentOfType(topmostElement, javaClass<KtClassOrObject>(), javaClass<KtCallableDeclaration>())
            else -> topmostElement
        }
        return analyzableElement
                    // if none of the above worked, take the outermost declaration
                    ?: PsiTreeUtil.getTopmostParentOfType(element, javaClass<KtDeclaration>())
                    // if even that didn't work, take the whole file
                    ?: element.getContainingKtFile()
    }

    fun analyze(project: Project, componentProvider: ComponentProvider, analyzableElement: KtElement): AnalysisResult {
        try {
            val module = componentProvider.get<ModuleDescriptor>()
            if (analyzableElement is KtCodeFragment) {
                return AnalysisResult.success(analyzeExpressionCodeFragment(componentProvider, analyzableElement), module)
            }

            val file = analyzableElement.getContainingKtFile()
            if (LightClassUtil.belongsToKotlinBuiltIns(file) || file.getModuleInfo() is LibrarySourceInfo) {
                // Library sources: mark file to skip
                file.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true)
            }

            val resolveSession = componentProvider.get<ResolveSession>()
            val trace = DelegatingBindingTrace(resolveSession.getBindingContext(), "Trace for resolution of " + analyzableElement)

            val targetPlatform = TargetPlatformDetector.getPlatform(analyzableElement.getContainingKtFile())

            val lazyTopDownAnalyzer = createContainerForLazyBodyResolve(
                    //TODO: should get ModuleContext
                    componentProvider.get<GlobalContext>().withProject(project).withModule(module),
                    resolveSession,
                    trace,
                    targetPlatform,
                    componentProvider.get<BodyResolveCache>()
            ).get<LazyTopDownAnalyzerForTopLevel>()

            lazyTopDownAnalyzer.analyzeDeclarations(
                    TopDownAnalysisMode.TopLevelDeclarations,
                    listOf(analyzableElement)
            )
            return AnalysisResult.success(
                    trace.getBindingContext(),
                    module
            )
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: IndexNotReadyException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalysisResult.error(BindingContext.EMPTY, e)
        }
    }

    private fun analyzeExpressionCodeFragment(componentProvider: ComponentProvider, codeFragment: KtCodeFragment): BindingContext {
        val trace = BindingTraceContext()
        componentProvider.get<CodeFragmentAnalyzer>().analyzeCodeFragment(
                codeFragment,
                trace,
                BodyResolveMode.PARTIAL_FOR_COMPLETION //TODO: discuss it
        )
        return trace.bindingContext
    }
}
