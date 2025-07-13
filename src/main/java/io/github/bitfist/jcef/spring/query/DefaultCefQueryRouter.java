package io.github.bitfist.jcef.spring.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cef.callback.CefQueryCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Handles the routing and execution of queries from CefQuery events. This class is designed to manage the registration
 * and execution of methods annotated with {@link CefQueryHandler}, allowing for dynamic and flexible route handling.
 * </p>
 * <p>
 * It supports hierarchical route definitions and parameterized paths, converting query payloads into method arguments,
 * including path variables and request payloads, using Jackson for JSON deserialization when needed. The methods are
 * associated with the routes through regular expressions.
 * </p>
 * <p>
 * The {@link DefaultCefQueryRouter} listens to {@link CefQueryEvent} instances and delegates the processing to the
 * appropriate handler methods based on the route and payload provided in the event.
 * </p>
 */
@Slf4j
public class DefaultCefQueryRouter {

    private final ApplicationContext applicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Method> messageHandlers = new HashMap<>();
    private final Map<Method, Object> beans = new HashMap<>();

    public DefaultCefQueryRouter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void initialize() {
        initializeMessageHandlers();
    }

    private void initializeMessageHandlers() {
        Collection<Object> beans = applicationContext.getBeansWithAnnotation(CefQueryHandler.class).values();
        for (Object bean : beans) {
            Class<?> clazz = bean.getClass();
            CefQueryHandler classAnnotation = clazz.getDeclaredAnnotation(CefQueryHandler.class);
            for (Method method : clazz.getDeclaredMethods()) {
                CefQueryHandler methodAnnotation = method.getDeclaredAnnotation(CefQueryHandler.class);
                if (methodAnnotation != null) {
                    String routeRegex = buildRouteRegex(classAnnotation.value(), methodAnnotation.value());
                    log.info("Registering message handler {} for route: {}", method, routeRegex);

                    method.setAccessible(true);

                    messageHandlers.put(routeRegex, method);
                    this.beans.put(method, bean);
                }
            }
        }
    }

    protected String buildRouteRegex(String parentRoute, String route) {
        parentRoute = escapeRoute(parentRoute);
        route = escapeRoute(route);

        if (route.isEmpty()) {
            return parentRoute;
        }

        boolean needsSlash = !parentRoute.endsWith("/") && !route.startsWith("/");
        return parentRoute + (needsSlash ? "/" : "") + route;
    }

    private static String escapeRoute(String route) {
        return route.replace("*", ".*")
                .replace("{", "(?<")
                .replace("}", ">[a-zA-Z0-9_-]+)");
    }

    @Async
    @EventListener
    public void onEvent(CefQueryEvent event) {
        CefQueryCallback callback = event.getCallback();
        CefQueryJson payload = event.getPayload();
        try {
            Object result = handleQuery(payload);
            sendResult(callback, result);
        } catch (CefQueryException e) {
            log.error("Failed to process message {}={} with error code {}", payload.getRoute(), payload.getRoute(), e.getErrorCode(), e);
            callback.failure(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process message {}={}", payload.getRoute(), payload.getRoute(), e);
            callback.failure(-1, "Internal error: " + e.getMessage());
        }
    }

    protected void sendResult(CefQueryCallback callback, @Nullable Object result) throws JsonProcessingException {
        if (result != null) {
            log.debug("Sending result: {}", result);

            // primitives (and wrappers) or String? just toString(), no JSON conversion
            if (result instanceof CharSequence
                    || result instanceof Number
                    || result instanceof Boolean) {
                callback.success(result.toString());
            } else {
                // anything else (your custom objects, arrays, maps…)
                String response = objectMapper.writeValueAsString(result);
                log.debug("Sending response: {}", response);
                callback.success(response);
            }
            return;
        }

        log.debug("Sending null result");
        callback.success("");
    }

    protected @Nullable Object handleQuery(CefQueryJson cefQueryJson) throws Exception {
        Optional<Map.Entry<String, Method>> routeHandlerEntry = getRouteHandler(cefQueryJson.getRoute());
        if (routeHandlerEntry.isEmpty()) {
            log.warn("No handler found for route: {}", cefQueryJson.getRoute());
            return null;
        }
        Map.Entry<String, Method> entry = routeHandlerEntry.get();
        String routeRegex = entry.getKey();
        Method handler = entry.getValue();
        Object bean = beans.get(handler);
        Map<String, String> params = extractParameters(cefQueryJson, routeRegex);
        Object[] args = buildArguments(handler, params, cefQueryJson);

        return handler.invoke(bean, args);
    }

    protected Map<String, String> extractParameters(CefQueryJson cefQueryJson, String routeRegex) {
        Map<String, String> params = new ConcurrentHashMap<>();
        Matcher routeMatcher = Pattern.compile(routeRegex).matcher(cefQueryJson.getRoute());
        routeMatcher.matches();
        Set<String> groups = routeMatcher.namedGroups().keySet();
        for (String group : groups) {
            String value = routeMatcher.group(group);
            params.put(group, value);
        }
        return params;
    }

    private Object[] buildArguments(Method handler, Map<String, String> routeParams, CefQueryJson cefQueryJson) {
        Parameter[] declared = handler.getParameters();
        Object[] args = new Object[declared.length];

        for (int i = 0; i < declared.length; i++) {
            Parameter p = declared[i];
            String name = p.getName();
            Class<?> type = p.getType();

            if (routeParams.containsKey(name)) {
                args[i] = convertParameter(routeParams.get(name), type);
            } else {
                // treat as payload (only one param should fall through)
                args[i] = convertPayload(cefQueryJson, type);
            }
        }

        return args;
    }

    /**
     * Convert primitive/wrapper/String values; fall back to Jackson for anything else.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object convertParameter(String raw, Class<?> targetType) {
        if (targetType == String.class) {
            return raw;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(raw);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(raw);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(raw);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(raw);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(raw);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(raw);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(raw);
        }
        if (targetType == char.class || targetType == Character.class) {
            return raw.charAt(0);
        }
        if (targetType == byte[].class) {
            return raw.getBytes();
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, raw);
        }

        // Fallback – use Jackson to coerce more complex types
        return objectMapper.convertValue(raw, targetType);
    }

    @SneakyThrows
    protected Object convertPayload(CefQueryJson cefQueryJson, Class<?> clazz) {
        if (clazz == String.class && cefQueryJson.getPayload() instanceof String) {
            return cefQueryJson.getPayload().toString();
        }
        return objectMapper.convertValue(cefQueryJson.getPayload(), clazz);
    }

    private Optional<Map.Entry<String, Method>> getRouteHandler(String route) {
        return messageHandlers.entrySet().stream()
                .filter(entry -> route.matches(entry.getKey()))
                .findFirst();
    }
}
