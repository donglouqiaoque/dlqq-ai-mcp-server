package com.donglouqiaoque.aitools.exit;

public enum ExitCode {

    UNKNOWN_ERROR(-1),

    SUCCESS(0),

    PARAM_EMPTY(1),

    CONFIG_ERROR(2),

    API_ERROR(3),

    FILE_ERROR(4);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据退出码获取对应的枚举值
     *
     * @param code 退出码
     * @return ExitCode枚举值
     */
    public static ExitCode fromCode(int code) {
        for (ExitCode exitCode : values()) {
            if (exitCode.code == code) {
                return exitCode;
            }
        }
        return UNKNOWN_ERROR;
    }

}
