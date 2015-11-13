class Klass {
    fun unusedFun() {
    }

    @SuppressWarnings("unused")
    fun unusedNoWarn() {

    }
}

fun main(args: Array<String>) {
    Klass()
}