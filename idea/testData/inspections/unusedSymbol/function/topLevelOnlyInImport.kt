package foo

import foo.onlyInImport
import foo.onlyInImportNoWarn

fun onlyInImport() {

}

@SuppressWarnings("unused")
fun onlyInImportNoWarn() {}
