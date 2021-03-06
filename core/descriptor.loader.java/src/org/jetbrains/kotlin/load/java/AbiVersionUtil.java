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

package org.jetbrains.kotlin.load.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion;

public final class AbiVersionUtil {
    public static final BinaryVersion INVALID_VERSION = BinaryVersion.create(new int[0]);

    public static boolean isAbiVersionCompatible(@NotNull BinaryVersion actual) {
        return actual.getMajor() == JvmAbi.VERSION.getMajor() &&
               actual.getMinor() <= JvmAbi.VERSION.getMinor();
    }

    private AbiVersionUtil() {
    }
}
