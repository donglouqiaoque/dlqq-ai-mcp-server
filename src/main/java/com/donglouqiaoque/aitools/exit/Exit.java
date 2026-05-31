package com.donglouqiaoque.aitools.exit;

import java.util.Objects;

public final class Exit {

    private Exit() {
        throw new UnsupportedOperationException("This class is not instantiable");
    }

    public static void exit(ExitCode code) {
        System.exit(Objects.requireNonNullElse(code, ExitCode.UNKNOWN_ERROR).getCode());
    }

}
