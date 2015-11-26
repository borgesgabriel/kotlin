fun main(args: Array<String>) {
    class LocalClass {
        fun f() {
        }

        @Suppress("unused")
        fun fNoWarn() {}

        val p = 5
    }


    LocalClass().f()
    LocalClass().p
}