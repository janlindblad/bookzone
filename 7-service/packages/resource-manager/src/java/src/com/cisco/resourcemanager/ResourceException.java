package com.cisco.resourcemanager;

public class ResourceException extends Exception {
    private static final long serialVersionUID = -5218497282239836227L;

    /**
     *
     */

    private ErrorCode errorCode = ErrorCode.UNDEFINED;

    public ResourceException(String msg) {
        super(msg);
    }

    public ResourceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public ResourceException(String msg, ErrorCode code, Throwable cause) {
        super(msg, cause);
        this.errorCode = code;
    }

    protected ResourceException(String msg, ErrorCode code) {
        super(msg);
        this.errorCode = code;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
