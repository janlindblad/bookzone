package com.cisco.resourcemanager;


/**
 * Error codes for all errors reported by resource manager
 */
public enum ErrorCode {

    /** Error with undefined error code */
    UNDEFINED(0),
    /** Typically we tried to read a value through CDB or MAAPI
     *  which does not exist */
    WAIT_FOR_REDEPLOY(1);

    private int errorCode = 0;

    private ErrorCode(int i) {
        errorCode = i;
    }

    public int getValue() {
        return errorCode;
    }

    public static ErrorCode valueOf(int i) {
        for (ErrorCode code : ErrorCode.values()) {
            if (code.getValue() == i) {
                return code;
            }
        }
        return null;
    }

    public boolean equalsTo(int i) {
        return (errorCode == i);
    }

    public String stringValue() {
        String str = this.name();
        return str.replaceFirst("ERR_", "").replace("_", " ").toLowerCase();
    }

}
