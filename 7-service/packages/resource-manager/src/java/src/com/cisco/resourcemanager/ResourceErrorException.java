package com.cisco.resourcemanager;

public class ResourceErrorException extends ResourceException {
    /**
     * Failed to allocate resource
     */

    public ResourceErrorException(String msg) {
        super(msg);
    }
}
