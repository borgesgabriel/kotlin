package templates

import templates.Family.*

fun aggregates(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("all(predicate: (T) -> Boolean)") {
        inline(true)
        doc { f -> "Returns `true` if all ${f.element.pluralize()} match the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (!predicate(element)) return false
            return true
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("none(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns `true` if no ${f.element.pluralize()} match the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return false
            return true
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("none()") {
        doc { f -> "Returns `true` if the ${f.collection} has no ${f.element.pluralize()}." }
        returns("Boolean")
        body {
            """
            for (element in this) return false
            return true
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("any(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns `true` if at least one ${f.element} matches the given [predicate]." }
        returns("Boolean")
        body {
            """
            for (element in this) if (predicate(element)) return true
            return false
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("any()") {
        doc { f -> "Returns `true` if ${f.collection} has at least one ${f.element}." }
        returns("Boolean")
        body {
            """
            for (element in this) return true
            return false
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("count(predicate: (T) -> Boolean)") {
        inline(true)

        doc { f -> "Returns the number of ${f.element.pluralize()} matching the given [predicate]." }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) if (predicate(element)) count++
            return count
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("count()") {
        doc { f -> "Returns the number of ${f.element.pluralize()} in this ${f.collection}." }
        returns("Int")
        body {
            """
            var count = 0
            for (element in this) count++
            return count
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        doc(CharSequences) { "Returns the length of this char sequence."}
        body(CharSequences, Strings) {
            "return length"
        }
        body(Maps, Collections, ArraysOfObjects, ArraysOfPrimitives) {
            "return size"
        }
    }

    templates add f("sumBy(selector: (T) -> Int)") {
        inline(true)
        include(CharSequences, Strings)
        deprecate(Strings) { forBinaryCompatibility }
        doc { f -> "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Int")
        body {
            """
            var sum: Int = 0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }
    }

    templates add f("sumByDouble(selector: (T) -> Double)") {
        inline(true)
        include(CharSequences, Strings)
        deprecate(Strings) { forBinaryCompatibility }
        doc { f -> "Returns the sum of all values produced by [selector] function applied to each ${f.element} in the ${f.collection}." }
        returns("Double")
        body {
            """
            var sum: Double = 0.0
            for (element in this) {
                sum += selector(element)
            }
            return sum
            """
        }
    }

    templates add f("min()") {
        doc { f -> "Returns the smallest ${f.element} or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        exclude(PrimitiveType.Boolean)
        typeParam("T : Comparable<T>")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var min = iterator.next()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (min > e) min = e
            }
            return min
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null
            var min = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (min > e) min = e
            }
            return min
            """
        }
    }

    templates add f("minBy(selector: (T) -> R)") {
        inline(true)

        doc { f -> "Returns the first ${f.element} yielding the smallest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        typeParam("R : Comparable<R>")
        typeParam("T : Any")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            var minValue = selector(minElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var minElem = this[0]
            var minValue = selector(minElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
    }

    templates add f("minBy(selector: (T) -> R)") {
        inline(true)

        only(Maps)
        doc { "Returns the first map entry yielding the smallest value of the given function or `null` if there are no entries." }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var minElem = iterator.next()
            var minValue = selector(minElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (minValue > v) {
                    minElem = e
                    minValue = v
                }
            }
            return minElem
            """
        }
    }

    templates add f("max()") {
        doc { f -> "Returns the largest ${f.element} or `null` if there are no ${f.element.pluralize()}." }
        returns("T?")
        exclude(PrimitiveType.Boolean)
        typeParam("T : Comparable<T>")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var max = iterator.next()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (max < e) max = e
            }
            return max
            """
        }

        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var max = this[0]
            for (i in 1..lastIndex) {
                val e = this[i]
                if (max < e) max = e
            }
            return max
            """
        }
    }

    templates add f("maxBy(selector: (T) -> R)") {
        inline(true)

        doc { f -> "Returns the first ${f.element} yielding the largest value of the given function or `null` if there are no ${f.element.pluralize()}." }
        typeParam("R : Comparable<R>")
        typeParam("T : Any")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            var maxValue = selector(maxElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        body(CharSequences, Strings, ArraysOfObjects, ArraysOfPrimitives) {
            """
            if (isEmpty()) return null

            var maxElem = this[0]
            var maxValue = selector(maxElem)
            for (i in 1..lastIndex) {
                val e = this[i]
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
    }

    templates add f("maxBy(selector: (T) -> R)") {
        inline(true)

        only(Maps)
        doc { "Returns the first map entry yielding the largest value of the given function or `null` if there are no entries." }
        typeParam("R : Comparable<R>")
        returns("T?")
        body {
            """
            val iterator = iterator()
            if (!iterator.hasNext()) return null

            var maxElem = iterator.next()
            var maxValue = selector(maxElem)
            while (iterator.hasNext()) {
                val e = iterator.next()
                val v = selector(e)
                if (maxValue < v) {
                    maxElem = e
                    maxValue = v
                }
            }
            return maxElem
            """
        }
    }

    templates add f("foldIndexed(initial: R, operation: (Int, R, T) -> R)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f ->
            """
            Accumulates value starting with [initial] value and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = 0
            var accumulator = initial
            for (element in this) accumulator = operation(index++, accumulator, element)
            return accumulator
            """
        }
    }

    templates add f("foldRightIndexed(initial: R, operation: (Int, T, R) -> R)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        only(CharSequences, Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { f ->
            """
            Accumulates value starting with [initial] value and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            """
        }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }
            return accumulator
            """
        }
    }

    templates add f("fold(initial: R, operation: (R, T) -> R)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        typeParam("R")
        returns("R")
        body {
            """
            var accumulator = initial
            for (element in this) accumulator = operation(accumulator, element)
            return accumulator
            """
        }
    }

    templates add f("foldRight(initial: R, operation: (T, R) -> R)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        only(CharSequences, Strings, Lists, ArraysOfObjects, ArraysOfPrimitives)
        doc { f -> "Accumulates value starting with [initial] value and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        typeParam("R")
        returns("R")
        body {
            """
            var index = lastIndex
            var accumulator = initial
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }
            return accumulator
            """
        }
    }

    templates add f("reduceIndexed(operation: (Int, T, T) -> T)") {
        inline(true)
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        exclude(ArraysOfObjects, Iterables, Sequences)

        doc { f ->
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        returns("T")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var index = 1
            var accumulator = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(index++, accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceIndexed(operation: (Int, S, T) -> S)") {
        inline(true)
        only(ArraysOfObjects, Iterables, Sequences)

        doc { f ->
            """
            Accumulates value starting with the first ${f.element} and applying [operation] from left to right
            to current accumulator value and each ${f.element} with its index in the original ${f.collection}.
            """
        }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var index = 1
            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(index++, accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceRightIndexed(operation: (Int, T, T) -> T)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        only(CharSequences, Strings, ArraysOfPrimitives)
        doc { f ->
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            """
        }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    templates add f("reduceRightIndexed(operation: (Int, T, S) -> S)") {
        inline(true)

        only(Lists, ArraysOfObjects)
        doc { f ->
            """
            Accumulates value starting with last ${f.element} and applying [operation] from right to left
            to each ${f.element} with its index in the original ${f.collection} and current accumulator value.
            """
        }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(index, get(index), accumulator)
                --index
            }

            return accumulator
            """
        }
    }

    templates add f("reduce(operation: (T, T) -> T)") {
        inline(true)
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        exclude(ArraysOfObjects, Iterables, Sequences)

        doc { f -> "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        returns("T")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduce(operation: (S, T) -> S)") {
        inline(true)
        only(ArraysOfObjects, Iterables, Sequences)

        doc { f -> "Accumulates value starting with the first ${f.element} and applying [operation] from left to right to current accumulator value and each ${f.element}." }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            val iterator = this.iterator()
            if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = iterator.next()
            while (iterator.hasNext()) {
                accumulator = operation(accumulator, iterator.next())
            }
            return accumulator
            """
        }
    }

    templates add f("reduceRight(operation: (T, T) -> T)") {
        inline(true)

        deprecate(Strings) { forBinaryCompatibility }
        only(CharSequences, Strings, ArraysOfPrimitives)
        doc { f -> "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        returns("T")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    templates add f("reduceRight(operation: (T, S) -> S)") {
        inline(true)

        only(Lists, ArraysOfObjects)
        doc { f -> "Accumulates value starting with last ${f.element} and applying [operation] from right to left to each ${f.element} and current accumulator value." }
        typeParam("S")
        typeParam("T: S")
        returns("S")
        body {
            """
            var index = lastIndex
            if (index < 0) throw UnsupportedOperationException("Empty iterable can't be reduced.")

            var accumulator: S = get(index--)
            while (index >= 0) {
                accumulator = operation(get(index--), accumulator)
            }

            return accumulator
            """
        }
    }

    templates add f("forEach(action: (T) -> Unit)") {
        inline(true)

        doc { f -> "Performs the given [action] on each ${f.element}." }
        returns("Unit")
        body {
            """
            for (element in this) action(element)
            """
        }
        deprecate(Strings) { forBinaryCompatibility }
        include(Maps, CharSequences, Strings)
    }

    templates add f("forEachIndexed(action: (Int, T) -> Unit)") {
        inline(true)
        deprecate(Strings) { forBinaryCompatibility }
        include(CharSequences, Strings)
        doc { f -> "Performs the given [action] on each ${f.element}, providing sequential index with the ${f.element}." }
        returns("Unit")
        body {
            """
            var index = 0
            for (item in this) action(index++, item)
            """
        }
    }

    return templates
}