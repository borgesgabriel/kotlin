fun box(): String {
    var encl1 = "fail";
    var encl2 = "fail";
    test {
        {
            encl1 = "OK"
            {
                encl2 = "OK"
            }()
        }()
    }

    return "OK"
}

inline fun test(s: () -> Unit) {
    s()
}

// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$1
// 2 INNERCLASS Kt10259Kt\$box\$\$inlined\$test\$lambda\$lambda\$1
// 4 INNERCLASS