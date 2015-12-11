interface A {
    companion object : <!UNRESOLVED_REFERENCE!>A_<!>() {
        fun foo() {}

        open class A_
    }
}

open class L(val a: Any) {}

open class B {
    companion object : L(<!UNRESOLVED_REFERENCE!>bar<!>()) {
        fun bar() {}

        class B_
    }
}

class C: B(), A {
    init {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        A.foo()
        A.Companion.foo()
        C.<!UNRESOLVED_REFERENCE!>foo<!>()

        <!UNRESOLVED_REFERENCE!>A_<!>()
        A.<!UNRESOLVED_REFERENCE!>A_<!>()
        A.Companion.A_()
        C.<!UNRESOLVED_REFERENCE!>A_<!>()

        bar()
        B.bar()
        B.Companion.bar()
        C.<!UNRESOLVED_REFERENCE!>bar<!>()

        B_()
        B.<!UNRESOLVED_REFERENCE!>B_<!>()
        B.Companion.B_()
        C.<!UNRESOLVED_REFERENCE!>B_<!>()
    }
}
