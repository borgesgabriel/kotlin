package smartStepIntoInterfaceImpl

interface I {
    fun foo(a: String) {
        val a = 1
    }

    fun foo(a: Int = 1) {
        val a = 1
    }
}

fun bar() : I = object: I {}

fun main(args: Array<String>) {
    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    bar().foo("1")

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    bar().foo()

    // SMART_STEP_INTO_BY_INDEX: 2
    // RESUME: 1
    //Breakpoint!
    bar().foo(2)
}
