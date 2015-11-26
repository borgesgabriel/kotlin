class Klass {
    fun unusedFun() {
    }

    @Suppress("unused")
    fun unusedNoWarn() {

    }
}

fun main(args: Array<String>) {
    Klass()
}