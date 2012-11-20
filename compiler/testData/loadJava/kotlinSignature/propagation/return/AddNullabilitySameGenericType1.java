package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilitySameGenericType1 {

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String>")
        List<String> foo();
    }

    public interface Sub extends Super {
        @ExpectLoadError("Auto type 'jet.String' is not-null, while type in alternative signature is nullable: 'String?'")
        @KotlinSignature("fun foo(): MutableList<String?>")
        List<String> foo();
    }
}