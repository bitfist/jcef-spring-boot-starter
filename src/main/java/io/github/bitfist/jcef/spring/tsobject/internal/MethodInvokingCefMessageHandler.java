package io.github.bitfist.jcef.spring.tsobject.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bitfist.jcef.spring.browser.CefQueryException;
import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.github.bitfist.jcef.spring.tsobject.internal.MethodInvokingCefQueryHandler.isComplexType;

@Slf4j
@RequiredArgsConstructor
class MethodInvokingCefMessageHandler {

    private static final int JAVA_OBJECT_NOT_REGISTERED_AS_JAVASCRIPT_OBJECT = 2001;

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> beans = new HashMap<>();

    /**
     * Scans the ApplicationContext for beans annotated with @JavaScriptObject
     * and populates the cache upon bean initialization.
     */
    @PostConstruct
    void initialize() {
        var beans = applicationContext.getBeansWithAnnotation(TypeScriptObject.class);
        for (Object bean : beans.values()) {
            this.beans.put(bean.getClass().getName(), bean);
        }
    }

    /**
     * Handles the incoming MethodInvokingCefMessage by invoking the specified method on a cached bean.
     *
     * @param message The MethodInvokingCefMessage object containing class, method, and parameters.
     * @return The result of the method invocation.
     */
    @SneakyThrows
    Object handle(MethodInvokingCefMessage message) {
        var bean = findBean(message.getClassName());
        var method = findMethod(bean.getClass(), message.getMethodName(), message.getParameters());
        var arguments = prepareArguments(method, message.getParameters());
        method.setAccessible(true);
        return method.invoke(bean, arguments);
    }

    /**
     * Finds a bean from the cache using its class name.
     */
    private Object findBean(String className) {
        var bean = beans.get(className);
        if (bean == null) {
            log.error("Failed to find JavaScriptObject with class name '{}'", className);
            throw new CefQueryException(JAVA_OBJECT_NOT_REGISTERED_AS_JAVASCRIPT_OBJECT, "Class '" + className + "' is not registered as a @JavaScriptObject.");
        }
        return bean;
    }

    /**
     * Finds a method by name in a class that matches the provided parameter count.
     */
    @SneakyThrows
    private Method findMethod(Class<?> beanClass, String methodName, @Nullable Map<String, Object> parameters) {
        int parameterCount = (parameters != null) ? parameters.size() : 0;
        for (Method method : beanClass.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                // For simplicity, we match by name and parameter count.
                return method;
            }
        }
        throw new NoSuchMethodException("No suitable method '" + methodName + "' with " + parameterCount + " parameters found in " + beanClass.getName());
    }

    /**
     * Prepares method arguments, deserializing JSON strings into objects where necessary.
     */
    private Object[] prepareArguments(Method method, Map<String, Object> parameters) {
        if (method.getParameterCount() == 0) {
            return new Object[0];
        }

        var paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        var methodParameters = method.getParameters();

        for (var i = 0; i < paramTypes.length; i++) {
            var paramName = methodParameters[i].getName();
            var paramValue = parameters.get(paramName);
            Class<?> paramType = paramTypes[i];

            if (paramValue instanceof String && !paramType.equals(String.class) && isComplexType(paramType)) {
                try {
                    args[i] = objectMapper.readValue((String) paramValue, paramType);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error deserializing JSON parameter '" + paramName + "'", e);
                }
            } else {
                // Use Jackson to convert between basic types as well (e.g., Integer to Long)
                args[i] = objectMapper.convertValue(paramValue, paramType);
            }
        }
        return args;
    }

}