fun test(bal: Array<Int>) {
    var bar = 4

    val <!UNUSED_VARIABLE!>a<!>: () -> Unit = { bar += 4 }

    val <!UNUSED_VARIABLE!>b<!>: () -> Int = { <!EXPECTED_TYPE_MISMATCH!>bar = 4<!> }

    val <!UNUSED_VARIABLE!>c<!>: () -> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> = { bal[2] = 3 }

    val <!UNUSED_VARIABLE!>d<!>: () -> Int = { <!ASSIGNMENT_TYPE_MISMATCH(kotlin.Int)!>bar += 4<!> }

    val <!UNUSED_VARIABLE!>e<!>: Unit = run { bar += 4 }

    val <!UNUSED_VARIABLE!>f<!>: Int = run { <!ASSIGNMENT_TYPE_MISMATCH!>bar += 4<!> }
}
fun <T> run(f: () -> T): T = f()