package io.github.bitfist.jcef.spring.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.cef.callback.CefQueryCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class DefaultCefQueryRouterSendResultTest {

    private final DefaultCefQueryRouter converter = new DefaultCefQueryRouter(mock(ApplicationContext.class));

    @Test
    @DisplayName("🔚 should send empty string when result is null")
    void testNullResult() throws JsonProcessingException {
        CefQueryCallback callback = mock(CefQueryCallback.class);

        converter.sendResult(callback, null);

        verify(callback).success("");
        verifyNoMoreInteractions(callback);
    }

    @Test
    @DisplayName("📝 should send raw text when result is CharSequence")
    void testCharSequenceResult() throws JsonProcessingException {
        CefQueryCallback callback = mock(CefQueryCallback.class);
        CharSequence text = "hello world";

        converter.sendResult(callback, text);

        verify(callback).success("hello world");
        verifyNoMoreInteractions(callback);
    }

    @Test
    @DisplayName("🔢 should send raw number string when result is Number")
    void testNumberResult() throws JsonProcessingException {
        CefQueryCallback callback = mock(CefQueryCallback.class);

        converter.sendResult(callback, 123);
        converter.sendResult(callback, 45.67);

        verify(callback).success("123");
        verify(callback).success("45.67");
        verifyNoMoreInteractions(callback);
    }

    @Test
    @DisplayName("✅ should send raw boolean string when result is Boolean")
    void testBooleanResult() throws JsonProcessingException {
        CefQueryCallback callback = mock(CefQueryCallback.class);

        converter.sendResult(callback, Boolean.TRUE);
        converter.sendResult(callback, Boolean.FALSE);

        verify(callback).success("true");
        verify(callback).success("false");
        verifyNoMoreInteractions(callback);
    }

    @Test
    @DisplayName("🗄️ should serialize custom object to JSON")
    void testCustomObjectResult() throws JsonProcessingException {
        CefQueryCallback callback = mock(CefQueryCallback.class);

        @Data
        class Dummy {
            public String test;
        }
        Dummy dummy = new Dummy();
        dummy.test = "dummy";

        converter.sendResult(callback, dummy);

        verify(callback).success("{\"test\":\"dummy\"}");
        verifyNoMoreInteractions(callback);
    }
}
