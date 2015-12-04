@file:kotlin.jvm.JvmName("ArrayIntrinsicsKt")

package kotlin

// TODO: Move to JVM-specific version of builtins
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


// Array "constructor"
/**
 * Returns an array containing the specified elements.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun <reified T> arrayOf(vararg elements: T) : Array<T> = elements as Array<T>

// "constructors" for primitive types array
/**
 * Returns an array containing the specified [Double] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun doubleArrayOf(vararg elements: Double) : DoubleArray    = elements

/**
 * Returns an array containing the specified [Float] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun floatArrayOf(vararg elements: Float) : FloatArray       = elements

/**
 * Returns an array containing the specified [Long] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun longArrayOf(vararg elements: Long) : LongArray          = elements

/**
 * Returns an array containing the specified [Int] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun intArrayOf(vararg elements: Int) : IntArray             = elements

/**
 * Returns an array containing the specified characters.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun charArrayOf(vararg elements: Char) : CharArray          = elements

/**
 * Returns an array containing the specified [Short] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun shortArrayOf(vararg elements: Short) : ShortArray       = elements

/**
 * Returns an array containing the specified [Byte] numbers.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun byteArrayOf(vararg elements: Byte) : ByteArray          = elements

/**
 * Returns an array containing the specified boolean values.
 */
@kotlin.jvm.JvmVersion
@kotlin.jvm.internal.Intrinsic("kotlin.arrays.array") public fun booleanArrayOf(vararg elements: Boolean) : BooleanArray = elements
