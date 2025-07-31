package io.github.bitfist.jcef.spring.tsobject.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.bitfist.jcef.spring.browser.CefQueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MethodInvokingCefQueryHandlerTest {

	@Mock
	private MethodInvokingCefMessageHandler messageHandler;

	private MethodInvokingCefQueryHandler handler;

	@BeforeEach
	void setUp() {
		handler = new MethodInvokingCefQueryHandler(messageHandler);
	}

	@Test
	@DisplayName("🛑 handleQuery(null) should return null and not throw")
	void testHandleQuery_NullQuery_ReturnsNull() {
		var result = handler.handleQuery(null);
		assertNull(result, "Expected null when query is null");
	}

	@Test
	@DisplayName("❌ handleQuery(invalid JSON) should throw CefQueryException with cause JsonProcessingException")
	void testHandleQuery_InvalidJson_ThrowsCefQueryException() {
		var invalidJson = "not a json";
		CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handleQuery(invalidJson), "Expected CefQueryException for invalid JSON");
		assertInstanceOf(JsonProcessingException.class, ex.getCause(), "Cause should be JsonProcessingException");
	}

	@Test
	@DisplayName("🔍 handleQuery with null result should return null")
	void testHandleQuery_NullResult_ReturnsNull() {
		when(messageHandler.handle(any(MethodInvokingCefMessage.class))).thenReturn(null);
		var output = handler.handleQuery("{}");
		assertNull(output, "Expected null when handler returns null");
	}

	@Test
	@DisplayName("🎯 handleQuery with simple result should return toString() of the result")
	void testHandleQuery_SimpleTypeResult_ReturnsToString() {
		when(messageHandler.handle(any())).thenReturn(123);
		var output = handler.handleQuery("{}");
		assertEquals("123", output, "Expected string '123' for Integer result");

		when(messageHandler.handle(any())).thenReturn("hello");
		output = handler.handleQuery("{}");
		assertEquals("hello", output, "Expected original string for String result");
	}

	@Test
	@DisplayName("📦 handleQuery with complex result should return JSON string of the object")
	void testHandleQuery_ComplexTypeResult_ReturnsJsonString() {
		var complex = new HashMap<String, Object>();
		complex.put("key", "value");
		when(messageHandler.handle(any())).thenReturn(complex);

		var output = handler.handleQuery("{}");
		// JSON might have fields in any order, but should contain key and value
		assertNotNull(output);
		assertTrue(output.contains("\"key\""), "JSON output should contain 'key'");
		assertTrue(output.contains("\"value\""), "JSON output should contain 'value'");
	}

	@Test
	@DisplayName("💥 handleQuery when input JSON serialization fails should throw CefQueryException")
	void testHandleQuery_InputSerializationError_ThrowsCefQueryException() {
		when(messageHandler.handle(any())).thenReturn(new Object());

		CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handleQuery("{-"), "Expected CefQueryException when serialization fails");
		assertInstanceOf(JsonProcessingException.class, ex.getCause(), "Cause should be JsonProcessingException");
		assertEquals(MethodInvokingCefQueryHandler.JSON_MESSAGE_PROCESSING_ERROR, ex.getErrorCode(), "Expected error code for JSON processing error");
	}

	@Test
	@DisplayName("💥 handleQuery when output JSON serialization fails should throw CefQueryException")
	void testHandleQuery_OutputSerializationError_ThrowsCefQueryException() {
		when(messageHandler.handle(any())).thenReturn(new InvalidJsonClass());

		CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handleQuery("{}"), "Expected CefQueryException when serialization fails");
		assertInstanceOf(JsonProcessingException.class, ex.getCause(), "Cause should be JsonProcessingException");
		assertEquals(MethodInvokingCefQueryHandler.JSON_RETURN_VALUE_PROCESSING_ERROR, ex.getErrorCode(), "Expected error code for JSON processing error");
	}

	@Test
	@DisplayName("⚙️ isComplexType should correctly identify simple and complex types")
	void testIsComplexType_VariousTypes() {
		// primitives
		assertFalse(MethodInvokingCefQueryHandler.isComplexType(int.class));
		assertFalse(MethodInvokingCefQueryHandler.isComplexType(double.class));

		// java.lang types
		assertFalse(MethodInvokingCefQueryHandler.isComplexType(String.class));
		assertFalse(MethodInvokingCefQueryHandler.isComplexType(Integer.class));

		// custom type
		class Dummy {
		}
		assertTrue(MethodInvokingCefQueryHandler.isComplexType(Dummy.class));

		// other java package
		assertTrue(MethodInvokingCefQueryHandler.isComplexType(java.util.Map.class));
	}

	static class InvalidJsonClass {
		private final String something = "something";
	}
}
