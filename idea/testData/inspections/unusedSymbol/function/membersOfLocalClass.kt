fun main(args: Array<String>) {
    class LocalClass {
        fun f() {
        }

        @SuppressWarnings("unused")
        fun fNoWarn() {}

        val p = 5
    }


    LocalClass().f()
    LocalClass().p
}