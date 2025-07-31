package io.github.bitfist.jcef.spring.tsobject.internal;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Data
public class MethodInvokingCefMessage {

	private String className;
	private String methodName;
	private @Nullable Map<String, Object> parameters;
}
