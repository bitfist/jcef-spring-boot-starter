package io.github.bitfist.jcef.spring.browser.javascript;

/**
 * Represents an executor capable of running JavaScript code.
 * This interface must be implemented to provide a mechanism for executing
 * JavaScript within the browser.
 */
public interface JavaScriptExecutor {

    /**
     * Executes the provided JavaScript code within the browser.
     *
     * @param code the JavaScript code to be executed
     */
    void execute(String code);
}
