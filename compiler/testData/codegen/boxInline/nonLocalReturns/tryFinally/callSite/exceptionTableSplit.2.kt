package test

public class Holder(var value: String = "") {

    operator fun plusAssign(s: String?) {
        if (value.length() != 0) {
            value += " -> "
        }
        value += s
    }

    override fun toString(): String {
        return value
    }

}

public inline fun <R> doCall(h: Holder, block: ()-> R) : R {
    try {
        return block()
    } finally {
        h += "inline fun finally"
    }
}

public inline fun <R> doCallWithException(h: Holder, block: ()-> R) : R {
    try {
        return block()
    } finally {
        h += "inline fun finally"
        throw RuntimeException("fail");
    }
}
