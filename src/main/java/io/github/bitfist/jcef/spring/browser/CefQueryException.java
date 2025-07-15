package io.github.bitfist.jcef.spring.browser;

import lombok.Getter;

/**
 * Exception whose code and message will be sent as a response to the current CEF query.
 */
@Getter
public class CefQueryException extends RuntimeException {

    private final int errorCode;

    public CefQueryException(int errorCode, Exception cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public CefQueryException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
