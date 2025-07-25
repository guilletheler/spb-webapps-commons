package com.gt.toolbox.spb.webapps.commons.exception;

public class GtBackendException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GtBackendException() {
        super();
    }

    public GtBackendException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public GtBackendException(String message, Throwable cause) {
        super(message, cause);
    }

    public GtBackendException(String message) {
        super(message);
    }

    public GtBackendException(Throwable cause) {
        super(cause);
    }
}
