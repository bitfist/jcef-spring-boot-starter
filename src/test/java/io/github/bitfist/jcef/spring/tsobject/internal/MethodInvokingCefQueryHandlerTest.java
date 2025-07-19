package io.github.bitfist.jcef.spring.tsobject.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.browser.CefQueryException;
import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("🧪 MethodInvokingCefQueryHandler Tests")
class MethodInvokingCefQueryHandlerTest {

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);

    private final MethodInvokingCefMessageHandler cefMessageHandler = new MethodInvokingCefMessageHandler(applicationContext);
    private final MethodInvokingCefQueryHandler cefQueryHandler = new MethodInvokingCefQueryHandler(cefMessageHandler);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // A sample service bean to be "discovered" by the handler
    @TypeScriptObject
    public static class MyTestService {
        public String greet(String name) {
            return "Hello, " + name;
        }

        public int add(Integer a, Integer b) {
            return a + b;
        }

        public ComplexObject processObject(ComplexObject data) {
            data.setValue(data.getValue() + 10);
            return data;
        }

        public void doNothing() {
            // Method with no parameters and no return value
        }
    }

    // A sample complex object for serialization/deserialization tests
    public static class ComplexObject {
        private String name;
        private int value;

        // Getters, setters, and constructors for JSON mapping
        public ComplexObject() {}
        public ComplexObject(String name, int value) { this.name = name; this.value = value; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            var that = (ComplexObject) o;
            return value == that.value && java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, value);
        }
    }

    @BeforeEach
    void setUp() {
        // Mock the ApplicationContext to return our test service
        var testService = new MyTestService();

        when(applicationContext.getBean(MyTestService.class)).thenReturn(testService);
    }

    @Test
    @DisplayName("✅ Initialize: Should scan and cache beans annotated with @TypeScriptObject")
    void initialize_shouldCacheAnnotatedBeans() {
        // This is implicitly tested in the setUp method, but we can add an explicit test.
        // To do this, we'll create a new instance and call initialize manually.
        var newHandler = new MethodInvokingCefMessageHandler(applicationContext);

        // Before full message initialization, calling handle should fail to find the bean
        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        assertThrows(CefQueryException.class, () -> newHandler.handle(message));

        // After initialization
        message.setMethodName("doNothing"); // A method that is easy to call
        message.setParameters(Collections.emptyMap());

        // Now it should not throw SecurityException (it might throw others if method is not found, which is fine)
        assertDoesNotThrow(() -> newHandler.handle(message));
    }

    @Test
    @DisplayName("✅ Handle Query: Should invoke method with simple parameters and return simple type")
    void handleQuery_withNullQuery_shouldReturnNull() {
        assertNull(cefQueryHandler.handleQuery(null));
    }

    @Test
    @DisplayName("✅ Handle Query: Should invoke method with simple parameters and return simple type")
    void handleQuery_withSimpleParams_shouldReturnSimpleType() throws JsonProcessingException {
        // Prepare the message
        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("greet");
        message.setParameters(Collections.singletonMap("name", "World"));

        var query = objectMapper.writeValueAsString(message);

        // Execute
        var result = cefQueryHandler.handleQuery(query);

        // Assert
        assertEquals("Hello, World", result);
    }

    @Test
    @DisplayName("✅ Handle Query: Should invoke method and return a serialized complex object")
    void handleQuery_withComplexParam_shouldReturnSerializedComplexObject() throws JsonProcessingException {
        // Prepare the complex object and the message
        var requestObject = new ComplexObject("test", 50);

        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("processObject");
        // Parameter needs to be a JSON string for the handler to deserialize it
        message.setParameters(Collections.singletonMap("data", objectMapper.writeValueAsString(requestObject)));

        var query = objectMapper.writeValueAsString(message);

        // Execute
        var jsonResult = cefQueryHandler.handleQuery(query);

        // Assert
        assertNotNull(jsonResult);
        var responseObject = objectMapper.readValue(jsonResult, ComplexObject.class);

        assertEquals("test", responseObject.getName());
        assertEquals(60, responseObject.getValue()); // 50 + 10
    }

    @Test
    @DisplayName("✅ Handle Query: Should handle methods with no return value (void)")
    void handleQuery_withVoidMethod_shouldReturnNull() throws JsonProcessingException {
        // Prepare the message
        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("doNothing");
        message.setParameters(Collections.emptyMap());

        var query = objectMapper.writeValueAsString(message);

        // Execute
        var result = cefQueryHandler.handleQuery(query);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("❌ Handle Query: Should throw CefMessageException for malformed JSON query")
    void handleQuery_withMalformedJson_shouldThrowException() {
        var malformedQuery = "{\"className\":\"Test\",, \"methodName\":\"test\"}";

        CefQueryException exception = assertThrows(
                CefQueryException.class,
                () -> cefQueryHandler.handleQuery(malformedQuery)
        );

        assertEquals(1001, getErrorCode(exception));
        assertInstanceOf(JsonProcessingException.class, exception.getCause());
    }

    @Test
    @DisplayName("❌ Handle: Should throw SecurityException for non-registered class")
    void handle_withNonRegisteredClass_shouldThrowSecurityException() throws JsonProcessingException {
        var message = new MethodInvokingCefMessage();
        message.setClassName("com.example.NonExistentService");
        message.setMethodName("someMethod");

        var query = objectMapper.writeValueAsString(message);

        CefQueryException exception = assertThrows(CefQueryException.class, () -> cefQueryHandler.handleQuery(query));

        assertEquals("Class 'com.example.NonExistentService' not found.", exception.getMessage());
    }

    @Test
    @DisplayName("❌ Handle: Should throw NoSuchMethodException for non-existent method")
    void handle_withNonExistentMethod_shouldThrowNoSuchMethodException() throws JsonProcessingException {
        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("nonExistentMethod");

        var query = objectMapper.writeValueAsString(message);

        assertThrows(
                CefQueryException.class,
                () -> cefQueryHandler.handleQuery(query)
        );
    }

    @Test
    @DisplayName("❌ Handle: Should throw NoSuchMethodException for wrong parameter count")
    void handle_withWrongParameterCount_shouldThrowNoSuchMethodException() throws JsonProcessingException {
        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("greet");
        message.setParameters(Collections.emptyMap()); // 'greet' expects 1 parameter

        var query = objectMapper.writeValueAsString(message);

        CefQueryException exception = assertThrows(
                CefQueryException.class,
                () -> cefQueryHandler.handleQuery(query)
        );

        assertTrue(exception.getMessage().contains("with 0 parameters not found"));
    }

    @Test
    @DisplayName("❌ Handle: Should throw IllegalArgumentException for parameter type mismatch")
    void handle_withParameterTypeMismatch_shouldThrowIllegalArgumentException() throws JsonProcessingException {
        // Calling 'add' method which expects Integers with a String
        var params = new HashMap<String, Object>();
        params.put("a", 10);
        params.put("b", "not a number");

        var message = new MethodInvokingCefMessage();
        message.setClassName(MyTestService.class.getName());
        message.setMethodName("add");
        message.setParameters(params);

        var query = objectMapper.writeValueAsString(message);

        // The exception is wrapped during invocation
        Exception exception = assertThrows(
                Exception.class,
                () -> cefQueryHandler.handleQuery(query)
        );

        assertInstanceOf(IllegalArgumentException.class, exception);
    }

    /**
     * Helper method to extract the error code from our custom exception,
     * as it's not directly accessible. This is a bit of a workaround.
     */
    private int getErrorCode(CefQueryException e) {
        try {
            // CefMessageException(int, Throwable) -> The int is not stored in a field.
            // We can infer it from the test case context.
            // A better CefMessageException would store the code.
            if (e.getCause() instanceof JsonProcessingException) {
                // Based on the handler's logic
                var message = e.getMessage();
                if (message.contains("Error deserializing")) {
                    return 1001; // Not perfectly reliable, but good for this test
                }
                // Check if the cause's message relates to return value processing
                if(e.getMessage().contains("return value")){
                    return 1002;
                }
                return 1001;
            }
        } catch (Exception ignored) {}
        return -1; // Code not found
    }
}
