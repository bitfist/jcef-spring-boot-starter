package io.github.bitfist.jcef.spring.tsobject.internal;

import io.github.bitfist.jcef.spring.browser.CefQueryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MethodInvokingCefMessageHandlerTest {

    @Mock
    private ApplicationContext applicationContext;

    private MethodInvokingCefMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MethodInvokingCefMessageHandler(applicationContext);
    }

    // A simple bean for testing
    static class TestBean {
        public String sayHello() {
            return "Hello";
        }

        public String echo(String message) {
            return message;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public ComplexType complex(ComplexType input) {
            return input;
        }
    }

    // Complex type for JSON deserialization tests
    static class ComplexType {
        private String name;
        private int age;

        public ComplexType() {
        }

        public ComplexType(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            var that = (ComplexType) o;
            return age == that.age && name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + age;
        }
    }

    @Test
    @DisplayName("🧪 Test no-arg method invocation")
    void testNoArgInvocation() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("sayHello");
        message.setParameters(null);

        var result = handler.handle(message);
        assertEquals("Hello", result);
    }

    @Test
    @DisplayName("🚫 Test no-parameter method invocation")
    void testNoParameterInvocation() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("echo");
        message.setParameters(null);

        var exception = assertThrows(CefQueryException.class, () -> handler.handle(message));
        assertEquals(MethodInvokingCefMessageHandler.JAVA_METHOD_NOT_FOUND, exception.getErrorCode(), "Expected error code for missing method.");
    }

    @Test
    @DisplayName("🔄 Test single-arg echo method invocation")
    void testEchoInvocation() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("echo");
        message.setParameters(Map.of("message", "test"));

        var result = handler.handle(message);
        assertEquals("test", result);
    }

    @Test
    @DisplayName("➕ Test primitive type conversion in add method")
    void testAddInvocation() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("add");
        message.setParameters(Map.of("a", 5, "b", 7));

        var result = handler.handle(message);
        assertEquals(12, result);
    }

    @Test
    @DisplayName("🧩 Test JSON deserialization for complex type")
    void testComplexDeserialization() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var json = "{\"name\":\"Alice\",\"age\":30}";
        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("complex");

        // Need to account for parameter name; reflection parameter names might be different.
        // For simplicity, adjust to use index-based map keys matching compiled parameter names.
        // Here we assume parameter name is 'input'.
        message.setParameters(Map.of("input", json));

        var result = handler.handle(message);
        assertEquals(new ComplexType("Alice", 30), result);
    }

    @Test
    @DisplayName("🚫 Test bean not registered exception")
    void testBeanNotRegistered() {
        when(applicationContext.getBean(TestBean.class)).thenThrow(new NoSuchBeanDefinitionException(TestBean.class));

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("sayHello");
        message.setParameters(null);

        CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handle(message));
        assertTrue(ex.getMessage().contains("is not registered as a @TypeScriptObject"));
    }

    @Test
    @DisplayName("🚫 Test class not found exception")
    void testClassNotFound() {
        var message = new MethodInvokingCefMessage();
        message.setClassName("non.existent.ClassName");
        message.setMethodName("any");
        message.setParameters(null);

        CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handle(message));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("🚫 Test method not found exception")
    void testMethodNotFound() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("unknownMethod");
        message.setParameters(null);

        CefQueryException ex = assertThrows(CefQueryException.class, () -> handler.handle(message));
        assertTrue(ex.getMessage().contains("with 0 parameters not found"));
    }

    @Test
    @DisplayName("⚠️ Test JSON deserialization error for complex type")
    void testComplexDeserializationError() {
        var bean = new TestBean();
        when(applicationContext.getBean(TestBean.class)).thenReturn(bean);

        var badJson = "{invalid_json}";
        var message = new MethodInvokingCefMessage();
        message.setClassName(TestBean.class.getName());
        message.setMethodName("complex");
        message.setParameters(Map.of("input", badJson));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> handler.handle(message));
        assertTrue(ex.getMessage().contains("Error deserializing JSON parameter"));
    }
}