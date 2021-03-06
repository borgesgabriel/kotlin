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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.lang.reflect.Modifier

public interface MemberScope : ResolutionScope {

    public override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>

    /**
     * Is supposed to be used in tests and debug only
     */
    public fun printScopeStructure(p: Printer)

    object Empty : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println("Empty member scope")
        }
    }

    companion object {
        public val ALL_NAME_FILTER: (Name) -> Boolean = { true }
    }
}

/**
 * The same as getDescriptors(kindFilter, nameFilter) but the result is guaranteed to be filtered by kind and name.
 */
public fun MemberScope.getDescriptorsFiltered(
        kindFilter: DescriptorKindFilter = DescriptorKindFilter.ALL,
        nameFilter: (Name) -> Boolean = { true }
): Collection<DeclarationDescriptor> {
    if (kindFilter.kindMask == 0) return listOf()
    return getContributedDescriptors(kindFilter, nameFilter).filter { kindFilter.accepts(it) && nameFilter(it.getName()) }
}

public class DescriptorKindFilter(
        kindMask: Int,
        public val excludes: List<DescriptorKindExclude> = listOf()
) {
    public val kindMask: Int

    init {
        var mask = kindMask
        excludes.forEach { mask = mask and it.fullyExcludedDescriptorKinds.inv() }
        this.kindMask = mask
    }

    public fun accepts(descriptor: DeclarationDescriptor): Boolean
            = kindMask and descriptor.kind() != 0 && excludes.all { !it.excludes(descriptor) }

    public fun acceptsKinds(kinds: Int): Boolean
            = kindMask and kinds != 0

    public infix fun exclude(exclude: DescriptorKindExclude): DescriptorKindFilter
            = DescriptorKindFilter(kindMask, excludes + listOf(exclude))

    public fun withoutKinds(kinds: Int): DescriptorKindFilter
            = DescriptorKindFilter(kindMask and kinds.inv(), excludes)

    public fun withKinds(kinds: Int): DescriptorKindFilter
            = DescriptorKindFilter(kindMask or kinds, excludes)

    public fun restrictedToKinds(kinds: Int): DescriptorKindFilter
            = DescriptorKindFilter(kindMask and kinds, excludes)

    public fun restrictedToKindsOrNull(kinds: Int): DescriptorKindFilter? {
        val mask = kindMask and kinds
        if (mask == 0) return null
        return DescriptorKindFilter(mask, excludes)
    }

    public fun intersect(other: DescriptorKindFilter) = DescriptorKindFilter(kindMask and other.kindMask, excludes + other.excludes)

    override fun toString(): String {
        val predefinedFilterName = DEBUG_PREDEFINED_FILTERS_MASK_NAMES.firstOrNull { it.mask == kindMask } ?.name
        val kindString = predefinedFilterName ?: DEBUG_MASK_BIT_NAMES
                .map { if (acceptsKinds(it.mask)) it.name else null }
                .filterNotNull()
                .joinToString(separator = " | ")

        return "DescriptorKindFilter($kindString, $excludes)"
    }

    private fun DeclarationDescriptor.kind(): Int {
        return when (this) {
            is ClassDescriptor -> if (this.getKind().isSingleton()) SINGLETON_CLASSIFIERS_MASK else NON_SINGLETON_CLASSIFIERS_MASK
            is ClassifierDescriptor -> NON_SINGLETON_CLASSIFIERS_MASK
            is PackageFragmentDescriptor, is PackageViewDescriptor -> PACKAGES_MASK
            is FunctionDescriptor -> FUNCTIONS_MASK
            is VariableDescriptor -> VARIABLES_MASK
            else -> 0
        }
    }

    companion object {
        public val NON_SINGLETON_CLASSIFIERS_MASK: Int = 0x01
        public val SINGLETON_CLASSIFIERS_MASK: Int = 0x02
        public val PACKAGES_MASK: Int = 0x04
        public val FUNCTIONS_MASK: Int = 0x08
        public val VARIABLES_MASK: Int = 0x10

        public val ALL_KINDS_MASK: Int = 0x1F
        public val CLASSIFIERS_MASK: Int = NON_SINGLETON_CLASSIFIERS_MASK or SINGLETON_CLASSIFIERS_MASK
        public val VALUES_MASK: Int = SINGLETON_CLASSIFIERS_MASK or FUNCTIONS_MASK or VARIABLES_MASK
        public val CALLABLES_MASK: Int = FUNCTIONS_MASK or VARIABLES_MASK

        @JvmField public val ALL: DescriptorKindFilter = DescriptorKindFilter(ALL_KINDS_MASK)
        @JvmField public val CALLABLES: DescriptorKindFilter = DescriptorKindFilter(CALLABLES_MASK)
        @JvmField public val NON_SINGLETON_CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(NON_SINGLETON_CLASSIFIERS_MASK)
        @JvmField public val SINGLETON_CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(SINGLETON_CLASSIFIERS_MASK)
        @JvmField public val CLASSIFIERS: DescriptorKindFilter = DescriptorKindFilter(CLASSIFIERS_MASK)
        @JvmField public val PACKAGES: DescriptorKindFilter = DescriptorKindFilter(PACKAGES_MASK)
        @JvmField public val FUNCTIONS: DescriptorKindFilter = DescriptorKindFilter(FUNCTIONS_MASK)
        @JvmField public val VARIABLES: DescriptorKindFilter = DescriptorKindFilter(VARIABLES_MASK)
        @JvmField public val VALUES: DescriptorKindFilter = DescriptorKindFilter(VALUES_MASK)

        private class MaskToName(val mask: Int, val name: String)

        private val DEBUG_PREDEFINED_FILTERS_MASK_NAMES = staticFields<DescriptorKindFilter>()
                .map { field ->
                    val filter = field.get(null) as? DescriptorKindFilter
                    if (filter != null) MaskToName(filter.kindMask, field.getName()) else null
                }
                .filterNotNull()
                .toReadOnlyList()

        private val DEBUG_MASK_BIT_NAMES = staticFields<DescriptorKindFilter>()
                .filter { it.getType() == Integer.TYPE }
                .map { field ->
                    val mask = field.get(null) as Int
                    val isOneBitMask = mask == (mask and (-mask))
                    if (isOneBitMask) MaskToName(mask, field.getName()) else null
                }
                .filterNotNull()
                .toReadOnlyList()

        private inline fun <reified T : Any> staticFields() = javaClass<T>().getFields().filter { Modifier.isStatic(it.getModifiers()) }
    }
}

public abstract class DescriptorKindExclude {
    public abstract fun excludes(descriptor: DeclarationDescriptor): Boolean

    public abstract val fullyExcludedDescriptorKinds: Int

    override fun toString() = this.javaClass.getSimpleName()

    public object Extensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor is CallableDescriptor && descriptor.getExtensionReceiverParameter() != null

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    public object NonExtensions : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor !is CallableDescriptor || descriptor.getExtensionReceiverParameter() == null

        override val fullyExcludedDescriptorKinds: Int
            get() = DescriptorKindFilter.ALL_KINDS_MASK and (DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.VARIABLES_MASK).inv()
    }

    public object EnumEntry : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor)
                = descriptor is ClassDescriptor && descriptor.getKind() == ClassKind.ENUM_ENTRY

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }

    public object TopLevelPackages : DescriptorKindExclude() {
        override fun excludes(descriptor: DeclarationDescriptor): Boolean {
            val fqName = when (descriptor) {
                is PackageFragmentDescriptor -> descriptor.fqName
                is PackageViewDescriptor -> descriptor.fqName
                else -> return false
            }
            return fqName.parent().isRoot()
        }

        override val fullyExcludedDescriptorKinds: Int get() = 0
    }
}

