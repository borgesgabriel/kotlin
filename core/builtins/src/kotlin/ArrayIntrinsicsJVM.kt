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

// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun <reified T> arrayOf(vararg elements: T) : Array<T> = elements as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun doubleArrayOf(vararg elements: Double) : DoubleArray    = elements

/**
 * Returns an array containing the specified [Float] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun floatArrayOf(vararg elements: Float) : FloatArray       = elements

/**
 * Returns an array containing the specified [Long] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun longArrayOf(vararg elements: Long) : LongArray          = elements

/**
 * Returns an array containing the specified [Int] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun intArrayOf(vararg elements: Int) : IntArray             = elements

/**
 * Returns an array containing the specified characters.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun charArrayOf(vararg elements: Char) : CharArray          = elements

/**
 * Returns an array containing the specified [Short] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun shortArrayOf(vararg elements: Short) : ShortArray       = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun byteArrayOf(vararg elements: Byte) : ByteArray          = elements

/**
 * Returns an array containing the specified boolean values.
 */
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun booleanArrayOf(vararg elements: Boolean) : BooleanArray = elements
