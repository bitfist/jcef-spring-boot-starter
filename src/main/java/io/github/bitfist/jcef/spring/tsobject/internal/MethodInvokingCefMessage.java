package io.github.bitfist.jcef.spring.tsobject.internal;

import lombok.Data;

import java.util.Map;

@Data
public class MethodInvokingCefMessage {

    private String className;
    private String methodName;
    private Map<String, Object> parameters;
}
