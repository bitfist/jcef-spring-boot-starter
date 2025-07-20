package io.github.bitfist.jcef.spring.tsobject.internal;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MethodInvokingRestEndpointTest {

    private final MethodInvokingCefQueryHandler queryHandler = mock(MethodInvokingCefQueryHandler.class);

    private final MethodInvokingRestEndpoint endpoint = new MethodInvokingRestEndpoint(queryHandler);

    @Test
    void shouldInvokeQueryHandler() {
        var payload = "<payload>";

        // when
        endpoint.invokeMethod(payload);

        // then
        verify(queryHandler).handleQuery(payload);
    }

}