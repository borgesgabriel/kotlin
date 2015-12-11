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

@file:kotlin.jvm.JvmName("ArrayIntrinsicsKt")
@file:kotlin.jvm.JvmMultifileClass
package kotlin

/**
 * Returns an array with the specified [size], where each element is calculated by calling the specified
 * [init] function. The `init` function returns an array element given its index.
 */
public inline fun <reified T> Array(size: Int, init: (Int) -> T): Array<T> {
    val result = arrayOfNulls<T>(size)

    for (i in 0..size - 1) {
        result[i] = init(i)
    }

    return result as Array<T>
}

/**
 * Returns an empty array of the specified type [T].
 */
public inline fun <reified T> emptyArray(): Array<T> = arrayOfNulls<T>(0) as Array<T>
