package org.isaiahp.utils;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class CpuUtils {

    public static MemorySegment lookup(String symbol) {
        return Linker.nativeLinker().defaultLookup().find(symbol)
                .or(() -> SymbolLookup.loaderLookup().find(symbol))
                .orElseThrow();
    }

    private static final MethodHandle GETTID =
            Linker.nativeLinker().downcallHandle(
                    lookup("gettid"),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT));

    public static int getNativeThreadId() throws Throwable{
        return (int) GETTID.invokeExact();
    }
}
