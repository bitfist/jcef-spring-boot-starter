package io.github.bitfist.jcef.spring.query;

import lombok.Getter;

/**
 * Exception whose code and message will be send as a response to the current CEF query.
 */
@Getter
public class CefQueryException extends RuntimeException {

    private final int errorCode;

    public CefQueryException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
