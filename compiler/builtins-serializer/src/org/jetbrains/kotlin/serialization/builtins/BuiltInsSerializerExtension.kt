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

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.ProtoBuf

public class BuiltInsSerializerExtension : KotlinSerializerExtensionBase(BuiltInSerializerProtocol) {
    override fun shouldUseTypeTable(): Boolean = true

    override fun serializePackage(packageFragments: Collection<PackageFragmentDescriptor>, proto: ProtoBuf.Package.Builder) {
        if (packageFragments.isEmpty()) return

        val classes = packageFragments.flatMap {
            it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassDescriptor>()
        }

        for (descriptor in DescriptorSerializer.sort(classes)) {
            proto.addExtension(BuiltInsProtoBuf.className, stringTable.getSimpleNameIndex(descriptor.name))
        }

        proto.setExtension(BuiltInsProtoBuf.packageFqName, stringTable.getPackageFqNameIndex(packageFragments.first().fqName))
    }
}
