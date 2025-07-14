package io.github.bitfist.jcef.spring.browser;

import lombok.Getter;

/**
 * Exception whose code and message will be send as a response to the current CEF query.
 */
@Getter
public class CefMessageException extends RuntimeException {

    private final int errorCode;

    public CefMessageException(int errorCode, Exception cause) {
        super(cause);
        this.errorCode = errorCode;
    }
}
