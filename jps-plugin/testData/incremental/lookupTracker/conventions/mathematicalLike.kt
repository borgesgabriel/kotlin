package foo.bar

/*p:foo.bar*/fun testOperators(a: /*p:foo.bar*/A, b: /*p:foo.bar*/Int) {
    var d = a

    d/*c:foo.bar.A(inc)*/++
    /*c:foo.bar.A(inc)*/++d
    d/*c:foo.bar.A(dec) p:foo.bar(dec) p:java.lang(dec) p:kotlin(dec) p:kotlin.annotation(dec) p:kotlin.jvm(dec) p:kotlin.collections(dec) p:kotlin.ranges(dec) p:kotlin.sequences(dec) p:kotlin.text(dec) p:kotlin.io(dec)*/--
    /*c:foo.bar.A(dec) p:foo.bar(dec) p:java.lang(dec) p:kotlin(dec) p:kotlin.annotation(dec) p:kotlin.jvm(dec) p:kotlin.collections(dec) p:kotlin.ranges(dec) p:kotlin.sequences(dec) p:kotlin.text(dec) p:kotlin.io(dec)*/--d

    a /*c:foo.bar.A(plus)*/+ b
    a /*c:foo.bar.A(minus) p:foo.bar(minus) p:java.lang(minus) p:kotlin(minus) p:kotlin.annotation(minus) p:kotlin.jvm(minus) p:kotlin.collections(minus) p:kotlin.ranges(minus) p:kotlin.sequences(minus) p:kotlin.text(minus) p:kotlin.io(minus)*/- b
    /*c:foo.bar.A(not) p:foo.bar(not) p:java.lang(not) p:kotlin(not) p:kotlin.annotation(not) p:kotlin.jvm(not) p:kotlin.collections(not) p:kotlin.ranges(not) p:kotlin.sequences(not) p:kotlin.text(not) p:kotlin.io(not)*/!a

    // for val
    a /*c:foo.bar.A(timesAssign)*/*= b
    a /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) p:java.lang(divAssign) p:kotlin(divAssign) p:kotlin.annotation(divAssign) p:kotlin.jvm(divAssign) p:kotlin.collections(divAssign) p:kotlin.ranges(divAssign) p:kotlin.sequences(divAssign) p:kotlin.text(divAssign) p:kotlin.io(divAssign)*//= b

    // for var
    d /*c:foo.bar.A(plusAssign) p:foo.bar(plusAssign) p:java.lang(plusAssign) p:kotlin(plusAssign) p:kotlin.annotation(plusAssign) p:kotlin.jvm(plusAssign) p:kotlin.collections(plusAssign) p:kotlin.ranges(plusAssign) p:kotlin.sequences(plusAssign) p:kotlin.text(plusAssign) p:kotlin.io(plusAssign) c:foo.bar.A(getPlusAssign) c:foo.bar.A(getPLUSAssign) c:foo.bar.A(plus)*/+= b
    d /*c:foo.bar.A(minusAssign) p:foo.bar(minusAssign) p:java.lang(minusAssign) p:kotlin(minusAssign) p:kotlin.annotation(minusAssign) p:kotlin.jvm(minusAssign) p:kotlin.collections(minusAssign) p:kotlin.ranges(minusAssign) p:kotlin.sequences(minusAssign) p:kotlin.text(minusAssign) p:kotlin.io(minusAssign) c:foo.bar.A(getMinusAssign) c:foo.bar.A(getMINUSAssign) c:foo.bar.A(minus) p:foo.bar(minus) p:java.lang(minus) p:kotlin(minus) p:kotlin.annotation(minus) p:kotlin.jvm(minus) p:kotlin.collections(minus) p:kotlin.ranges(minus) p:kotlin.sequences(minus) p:kotlin.text(minus) p:kotlin.io(minus)*/-= b
    d /*c:foo.bar.A(timesAssign) c:foo.bar.A(times) p:foo.bar(times) p:java.lang(times) p:kotlin(times) p:kotlin.annotation(times) p:kotlin.jvm(times) p:kotlin.collections(times) p:kotlin.ranges(times) p:kotlin.sequences(times) p:kotlin.text(times) p:kotlin.io(times) c:foo.bar.A(getTimes) c:foo.bar.A(getTIMES)*/*= b
    d /*c:foo.bar.A(divAssign) p:foo.bar(divAssign) p:java.lang(divAssign) p:kotlin(divAssign) p:kotlin.annotation(divAssign) p:kotlin.jvm(divAssign) p:kotlin.collections(divAssign) p:kotlin.ranges(divAssign) p:kotlin.sequences(divAssign) p:kotlin.text(divAssign) p:kotlin.io(divAssign) c:foo.bar.A(div) p:foo.bar(div) p:java.lang(div) p:kotlin(div) p:kotlin.annotation(div) p:kotlin.jvm(div) p:kotlin.collections(div) p:kotlin.ranges(div) p:kotlin.sequences(div) p:kotlin.text(div) p:kotlin.io(div) c:foo.bar.A(getDiv) c:foo.bar.A(getDIV)*//= b
}
