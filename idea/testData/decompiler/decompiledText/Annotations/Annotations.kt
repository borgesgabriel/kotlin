package test

import dependency.*

@A("a") @B(1) @C class Annotations {

    inline @A("f") @B(2) @C fun f(@A("i") @B(3) @C i: @A("int") Int) {
    }

    @A("p") @B(3) @C val p: @B(4) Int = 2

    enum class En { Entry1, @A("2") @B(2) Entry2, @A("3") Entry3 }
}
