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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyFqnNameIndex
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.ErrorUtils
import java.util.*

fun findDecompiledDeclaration(
        project: Project,
        referencedDescriptor: DeclarationDescriptor
): KtDeclaration? {
    if (isLocal(referencedDescriptor)) return null
    if (referencedDescriptor is PackageFragmentDescriptor || referencedDescriptor is PackageViewDescriptor) return null

    val decompiledFiles = findDecompiledFilesForDescriptor(project, referencedDescriptor)

    return decompiledFiles.asSequence().mapNotNull {
        it.getDeclarationForDescriptor(referencedDescriptor)
    }.firstOrNull()
}

private fun isLocal(descriptor: DeclarationDescriptor): Boolean {
    if (descriptor is ParameterDescriptor) {
        return isLocal(descriptor.containingDeclaration)
    }
    else {
        return DescriptorUtils.isLocal(descriptor)
    }
}

private fun findDecompiledFilesForDescriptor(
        project: Project,
        referencedDescriptor: DeclarationDescriptor
): Collection<KtDecompiledFile> {
    return findCandidateDeclarationsInIndex(project, referencedDescriptor).mapNotNullTo(LinkedHashSet()) {
        it?.containingFile as? KtDecompiledFile
    }
}

private fun findCandidateDeclarationsInIndex(
        project: Project,
        referencedDescriptor: DeclarationDescriptor
): Collection<KtDeclaration?> {
    if (ErrorUtils.isError(referencedDescriptor)) return emptyList()

    val scope = KotlinSourceFilterScope.sourceAndClassFiles(GlobalSearchScope.allScope(project), project)

    val containingClass = DescriptorUtils.getParentOfType(referencedDescriptor, ClassDescriptor::class.java, false)
    if (containingClass != null) {
        return KotlinFullClassNameIndex.getInstance().get(containingClass.fqNameSafe.asString(), project, scope)
    }

    val topLevelDeclaration = DescriptorUtils.getParentOfType(referencedDescriptor, PropertyDescriptor::class.java, false)
                              ?: DescriptorUtils.getParentOfType(referencedDescriptor, FunctionDescriptor::class.java, false)!!
    //TODO_R: assert is top level
    val fqName = topLevelDeclaration.fqNameSafe.asString()
    if (referencedDescriptor is FunctionDescriptor) {
        return KotlinTopLevelFunctionFqnNameIndex.getInstance().get(fqName, project, scope)
    }
    else if (referencedDescriptor is PropertyDescriptor) {
        return KotlinTopLevelPropertyFqnNameIndex.getInstance().get(fqName, project, scope)
    }
    return emptyList()
    //TODO_R: log
    error("Referenced non local and non top level declaration that is not function or property:\n $referencedDescriptor")
}

