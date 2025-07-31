package io.github.bitfist.jcef.spring.tsobject.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.browser.CefQueryException;
import io.github.bitfist.jcef.spring.browser.CefQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
class MethodInvokingCefQueryHandler implements CefQueryHandler {

	static final int JSON_MESSAGE_PROCESSING_ERROR = 1001;
	static final int JSON_RETURN_VALUE_PROCESSING_ERROR = 1002;

	private final MethodInvokingCefMessageHandler messageHandler;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public @Nullable String handleQuery(@Nullable String query) {
		if (query == null) {
			log.warn("Received null query");
			return null;
		}
		MethodInvokingCefMessage message;
		try {
			message = objectMapper.readValue(query, MethodInvokingCefMessage.class);
		} catch (JsonProcessingException jsonProcessingException) {
			log.error("Failed to deserialize query: {}", query, jsonProcessingException);
			throw new CefQueryException(JSON_MESSAGE_PROCESSING_ERROR, jsonProcessingException);
		}

		var result = messageHandler.handle(message);

		try {
			var convertedResult = serializeIfComplex(result);
			return convertedResult == null ? null : convertedResult.toString();
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize return value: {}", result, e);
			throw new CefQueryException(JSON_RETURN_VALUE_PROCESSING_ERROR, e);
		}
	}

	/**
	 * Serializes an object to a JSON string if it is a complex type.
	 * If the object is null, a primitive, a wrapper type, or a String, it is returned as is.
	 *
	 * @param object The object to potentially serialize.
	 * @return A JSON string for complex objects, or the original object for simple types.
	 */
	private @Nullable Object serializeIfComplex(@Nullable Object object) throws JsonProcessingException {
		if (object == null) {
			return null;
		}

		Class<?> objectClass = object.getClass();

		// If the type is not considered complex, return it directly.
		if (!isComplexType(objectClass)) {
			return object;
		}

		// Otherwise, serialize the complex object to a JSON string.
		return objectMapper.writeValueAsString(object);
	}

	/**
	 * Checks if a type is a complex object (i.e., not a primitive or standard Java language type).
	 */
	static boolean isComplexType(Class<?> type) {
		return !type.isPrimitive() && !type.getPackage().getName().startsWith("java.lang");
	}
}
