package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

class TestClass {
    companion object {
        fun finally() { finally() }

        fun test() {
            testNotRenamed("finally", { finally() })
        }
    }
}

fun box(): String {
    TestClass.test()

    return "OK"
}