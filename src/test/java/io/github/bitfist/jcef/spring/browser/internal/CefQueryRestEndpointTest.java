package io.github.bitfist.jcef.spring.browser.internal;

import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CefQueryRestEndpointTest {

	private final CefQueryHandler queryHandler = mock(CefQueryHandler.class);

	private final CefQueryRestEndpoint endpoint = new CefQueryRestEndpoint(queryHandler);

	@Test
	void shouldInvokeQueryHandler() {
		var payload = "<payload>";

		// when
		endpoint.invokeMethod(payload);

		// then
		verify(queryHandler).handleQuery(payload);
	}

}