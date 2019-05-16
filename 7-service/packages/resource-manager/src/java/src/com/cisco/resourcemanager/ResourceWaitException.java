package com.cisco.resourcemanager;

public class ResourceWaitException extends ResourceException {
    /**
     * Resource not yet ready
     */

    public ResourceWaitException(String msg) {
        super(msg);
    }

}
