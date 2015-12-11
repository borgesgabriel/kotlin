// FILE: C.java
public class C {
    public static void foo() {}
}

// FILE: test.kt
open class A {
    companion object : C() {
        fun bar() {}
    }
}

class B : A() {
        init {
            foo()
            bar()
        }

    companion object {
        init {
            foo()
            bar()
        }
    }
}


class D {
    init {
        foo()
    }

    companion object : C() {
        init {
            foo()
        }
    }
}
