fun foo(list: List<String>){}

fun f(){
    foo(<caret>)
}

// EXIST: { lookupString: "listOf", tailText: "() (kotlin)", typeText: "List<String>" }
// EXIST: { lookupString: "listOf", tailText: "(vararg elements: String) (kotlin)", typeText: "List<String>" }
// EXIST: { lookupString: "arrayListOf", tailText: "(vararg elements: String) (kotlin)", typeText: "ArrayList<String>" }
