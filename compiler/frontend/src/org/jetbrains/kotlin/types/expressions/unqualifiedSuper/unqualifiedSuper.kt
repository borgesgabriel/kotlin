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

package org.jetbrains.kotlin.types.expressions.unqualifiedSuper

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.singletonList


public fun resolveUnqualifiedSuperFromExpressionContext(
        superExpression: KtSuperExpression,
        supertypes: Collection<KotlinType>,
        anyType: KotlinType
): Collection<KotlinType> {
    val parentElement = superExpression.parent

    if (parentElement is KtDotQualifiedExpression) {
        val selectorExpression = parentElement.selectorExpression
        when (selectorExpression) {
            is KtCallExpression -> {
                // super.foo(...): foo can be a function or a property of a callable type
                val calleeExpression = selectorExpression.calleeExpression
                if (calleeExpression is KtSimpleNameExpression) {
                    val calleeName = calleeExpression.getReferencedNameAsName()
                    val location = NoLookupLocation.WHEN_TYPING
                    if (isCallingMethodOfAny(selectorExpression, calleeName)) {
                        return resolveSupertypesForMethodOfAny(supertypes, calleeName, location, anyType)
                    }
                    else {
                        return resolveSupertypesByCalleeName(supertypes, calleeName, location)
                    }
                }
            }
            is KtSimpleNameExpression -> {
                // super.x: x can be a property only
                // NB there are no properties in kotlin.Any
                return resolveSupertypesByPropertyName(supertypes, selectorExpression.getReferencedNameAsName(), NoLookupLocation.WHEN_TYPING)
            }
        }
    }

    return emptyList()
}

private val ARITY_OF_METHODS_OF_ANY = hashMapOf("hashCode" to 0, "equals" to 1, "toString" to 0)

private fun isCallingMethodOfAny(callExpression: KtCallExpression, calleeName: Name): Boolean =
        ARITY_OF_METHODS_OF_ANY.getOrElse(calleeName.asString(), { -1 }) == callExpression.valueArguments.size()

public fun isPossiblyAmbiguousUnqualifiedSuper(superExpression: KtSuperExpression, supertypes: Collection<KotlinType>): Boolean =
        supertypes.size() > 1 ||
        (supertypes.size() == 1 && supertypes.single().isInterface() && isCallingMethodOfAnyWithSuper(superExpression))

private fun isCallingMethodOfAnyWithSuper(superExpression: KtSuperExpression): Boolean {
    val parent = superExpression.parent
    if (parent is KtDotQualifiedExpression) {
        val selectorExpression = parent.selectorExpression
        if (selectorExpression is KtCallExpression) {
            val calleeExpression = selectorExpression.calleeExpression
            if (calleeExpression is KtSimpleNameExpression) {
                val calleeName = calleeExpression.getReferencedNameAsName()
                return isCallingMethodOfAny(selectorExpression, calleeName)
            }
        }
    }

    return false
}

private fun KotlinType.isInterface(): Boolean =
        TypeUtils.getClassDescriptor(this)?.kind == ClassKind.INTERFACE

private fun resolveSupertypesForMethodOfAny(supertypes: Collection<KotlinType>, calleeName: Name, location: LookupLocation, anyType: KotlinType): Collection<KotlinType> {
    val typesWithConcreteOverride = resolveSupertypesByMembers(supertypes, false) { getFunctionMembers(it, calleeName, location) }
    return if (typesWithConcreteOverride.isNotEmpty())
        typesWithConcreteOverride
    else
        anyType.singletonList()
}

private fun resolveSupertypesByCalleeName(supertypes: Collection<KotlinType>, calleeName: Name, location: LookupLocation): Collection<KotlinType> =
        resolveSupertypesByMembers(supertypes, true) { getFunctionMembers(it, calleeName, location) + getPropertyMembers(it, calleeName, location) }

private fun resolveSupertypesByPropertyName(supertypes: Collection<KotlinType>, propertyName: Name, location: LookupLocation): Collection<KotlinType> =
        resolveSupertypesByMembers(supertypes, true) { getPropertyMembers(it, propertyName, location) }

private inline fun resolveSupertypesByMembers(
        supertypes: Collection<KotlinType>,
        allowArbitraryMembers: Boolean,
        getMembers: (KotlinType) -> Collection<MemberDescriptor>
): Collection<KotlinType> {
    val typesWithConcreteMembers = SmartList<KotlinType>()
    val typesWithArbitraryMembers = SmartList<KotlinType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
            typesWithArbitraryMembers.add(supertype)
            if (members.any { isConcreteMember(supertype, it) }) {
                typesWithConcreteMembers.add(supertype)
            }
        }
    }

    return if (typesWithConcreteMembers.isNotEmpty()) typesWithConcreteMembers
        else if (allowArbitraryMembers) typesWithArbitraryMembers
        else emptyList<KotlinType>()
}

private fun getFunctionMembers(type: KotlinType, name: Name, location: LookupLocation): Collection<MemberDescriptor> =
        type.memberScope.getContributedFunctions(name, location)

private fun getPropertyMembers(type: KotlinType, name: Name, location: LookupLocation): Collection<MemberDescriptor> =
        type.memberScope.getContributedVariables(name, location).filterIsInstanceTo(SmartList<MemberDescriptor>())

private fun isConcreteMember(supertype: KotlinType, memberDescriptor: MemberDescriptor): Boolean {
    // "Concrete member" is a function or a property that is not abstract,
    // and is not an implicit fake override for a method of Any on an interface.

    if (memberDescriptor.modality == Modality.ABSTRACT)
        return false

    val classDescriptorForSupertype = TypeUtils.getClassDescriptor(supertype)
    val memberKind = (memberDescriptor as CallableMemberDescriptor).kind
    if (classDescriptorForSupertype?.kind == ClassKind.INTERFACE && memberKind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        // We have a fake override on interface. It should have a dispatch receiver, which should not be Any.
        val dispatchReceiverType = memberDescriptor.dispatchReceiverParameter?.type ?: return false
        val dispatchReceiverClass = TypeUtils.getClassDescriptor(dispatchReceiverType) ?: return false
        return !KotlinBuiltIns.isAny(dispatchReceiverClass)
    }

    return true
}
