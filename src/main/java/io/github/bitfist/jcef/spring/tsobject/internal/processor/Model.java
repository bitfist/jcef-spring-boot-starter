package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

record Field(String name, String type, boolean isOptional) { }

record Parameter(String name, String type) { }

record Method(String name, String returnType, List<Parameter> parameters) { }

@Data
class TSClass {

	private final String javaClassName;
	private final String tsClassName;
	private final String packageName;
	private final String outputPath;
	private final Type type;
	private final List<Field> fields = new ArrayList<>();
	private final List<Method> methods = new ArrayList<>();

	TSClass(String javaClassName, String tsClassName, String packageName, String outputPath, Type type) {
		this.javaClassName = javaClassName;
		this.tsClassName = tsClassName;
		this.packageName = packageName;
		this.outputPath = outputPath;
		this.type = type;
	}

	enum Type {
		SERVICE, CLASS, ENUM
	}
}