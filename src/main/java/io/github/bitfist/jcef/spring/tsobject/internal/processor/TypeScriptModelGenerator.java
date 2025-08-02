package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import io.github.bitfist.jcef.spring.tsobject.TypeScriptClass;
import io.github.bitfist.jcef.spring.tsobject.TypeScriptService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
class TypeScriptModelGenerator {

	private final Elements elementUtils;

	@Getter
	private final Map<String, TSClass> classModel = new HashMap<>();
	private final Set<String> processedTypes = new HashSet<>();

	void processEnum(TypeElement typeElement) {
		var qualifiedName = typeElement.getQualifiedName().toString();
		if (processedTypes.contains(qualifiedName)) {
			return;
		}
		processedTypes.add(qualifiedName);

		var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
		var simpleName = typeElement.getSimpleName().toString();
		var outputPath = packageName.replace('.', '/');

		var tsClass = new TSClass(qualifiedName, simpleName, packageName, outputPath, TSClass.Type.ENUM);
		classModel.put(qualifiedName, tsClass);

		// Process enum constants
		for (Element enclosedElement : typeElement.getEnclosedElements()) {
			if (enclosedElement.getKind() == ElementKind.ENUM_CONSTANT) {
				var enumConstant = enclosedElement.getSimpleName().toString();
				// For enums, we use the field name as both name and type
				tsClass.getFields().add(new Field(enumConstant, enumConstant, false));
			}
		}
	}

	void processClass(TypeElement typeElement) {
		var qualifiedName = typeElement.getQualifiedName().toString();
		if (processedTypes.contains(qualifiedName)) {
			return;
		}
		processedTypes.add(qualifiedName);

		var annotation = typeElement.getAnnotation(TypeScriptClass.class);
		var customPath = annotation.path();

		var tsClass = createTypeScriptClass(typeElement, customPath, TSClass.Type.CLASS);
		classModel.put(qualifiedName, tsClass);

		// Process fields
		for (Element enclosedElement : typeElement.getEnclosedElements()) {
			if (enclosedElement.getKind() == ElementKind.FIELD) {
				var field = (VariableElement) enclosedElement;
				if (!field.getModifiers().contains(Modifier.STATIC) && !field.getModifiers().contains(Modifier.TRANSIENT)) {
					processField(field, tsClass);
				}
			}
		}
	}

	void processService(TypeElement typeElement) {
		var qualifiedName = typeElement.getQualifiedName().toString();
		if (processedTypes.contains(qualifiedName)) {
			return;
		}
		processedTypes.add(qualifiedName);

		var annotation = typeElement.getAnnotation(TypeScriptService.class);
		var customPath = annotation.path();

		var tsClass = createTypeScriptClass(typeElement, customPath, TSClass.Type.SERVICE);
		classModel.put(qualifiedName, tsClass);

		// Process methods
		for (Element enclosedElement : typeElement.getEnclosedElements()) {
			if (enclosedElement.getKind() == ElementKind.METHOD) {
				var method = (ExecutableElement) enclosedElement;
				if (!method.getModifiers().contains(Modifier.PRIVATE) && !method.getModifiers().contains(Modifier.STATIC)) {
					processMethod(method, tsClass);
				}
			}
		}
	}

	private TSClass createTypeScriptClass(TypeElement typeElement, String customPath, TSClass.Type type) {
		var qualifiedName = typeElement.getQualifiedName().toString();
		var simpleName = typeElement.getSimpleName().toString();
		var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();

		String outputPath;
		if (!customPath.isEmpty()) {
			outputPath = customPath;
		} else {
			outputPath = packageName.replace('.', '/');
		}

		return new TSClass(qualifiedName, simpleName, packageName, outputPath, type);
	}

	private void processField(VariableElement field, TSClass tsClass) {
		var fieldName = field.getSimpleName().toString();
		var fieldType = field.asType();

		var tsType = convertToTypeScriptType(fieldType);
		var isOptional = false; // Could be enhanced with nullable annotations

		tsClass.getFields().add(new Field(fieldName, tsType, isOptional));

		// Check if we need to generate DTO for this type
		checkAndAddClassForType(fieldType);
	}

	private void processMethod(ExecutableElement method, TSClass tsClass) {
		var methodName = method.getSimpleName().toString();
		var returnType = method.getReturnType();

		var tsReturnType = convertToTypeScriptType(returnType);
		var parameters = new ArrayList<Parameter>();

		for (VariableElement param : method.getParameters()) {
			var paramName = param.getSimpleName().toString();
			var paramType = convertToTypeScriptType(param.asType());
			parameters.add(new Parameter(paramName, paramType));

			// Check if we need to generate DTO for parameter type
			checkAndAddClassForType(param.asType());
		}

		// Check if we need to generate DTO for return type
		checkAndAddClassForType(returnType);

		tsClass.getMethods().add(new Method(methodName, tsReturnType, parameters));
	}

	private void checkAndAddClassForType(TypeMirror type) {
		if (type.getKind() == TypeKind.DECLARED) {
			var declaredType = (DeclaredType) type;
			var typeElement = (TypeElement) declaredType.asElement();
			var qualifiedName = typeElement.getQualifiedName().toString();

			// Skip java.lang types and already processed types
			if (!qualifiedName.startsWith("java.lang.") && !processedTypes.contains(qualifiedName)) {
				// Create class for this type
				if (typeElement.getKind() == ElementKind.ENUM) {
					processEnum(typeElement);
				} else {
					processedTypes.add(qualifiedName);
					var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
					var simpleName = typeElement.getSimpleName().toString();
					var outputPath = packageName.replace('.', '/');

					var tsClass = new TSClass(qualifiedName, simpleName, packageName, outputPath, TSClass.Type.CLASS);
					classModel.put(qualifiedName, tsClass);

					// Process fields of this type
					for (Element enclosedElement : typeElement.getEnclosedElements()) {
						if (enclosedElement.getKind() == ElementKind.FIELD) {
							var field = (VariableElement) enclosedElement;
							if (!field.getModifiers().contains(Modifier.STATIC) && !field.getModifiers().contains(Modifier.TRANSIENT)) {
								processField(field, tsClass);
							}
						}
					}
				}
			}

			// Process generic type arguments
			for (TypeMirror typeArg : declaredType.getTypeArguments()) {
				checkAndAddClassForType(typeArg);
			}
		}
	}

	private String convertToTypeScriptType(TypeMirror type) {
		switch (type.getKind()) {
			case BOOLEAN:
				return "boolean";
			case BYTE:
			case SHORT:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
				return "number";
			case CHAR:
				return "string";
			case DECLARED:
				var declaredType = (DeclaredType) type;
				var typeElement = (TypeElement) declaredType.asElement();
				var typeName = typeElement.getQualifiedName().toString();

				if (typeElement.getKind() == ElementKind.ENUM) {
					return typeElement.getSimpleName().toString();
				}

				// Handle common Java types
				if (typeName.equals("java.lang.String")) {
					return "string";
				} else if (typeName.equals("java.lang.Boolean")) {
					return "boolean";
				} else if (typeName.matches("java.lang.(Byte|Short|Integer|Long|Float|Double)")) {
					return "number";
				} else if (typeName.equals("java.util.Date") || typeName.equals("java.time.LocalDateTime") || typeName.equals("java.time.LocalDate")) {
					return "string"; // ISO date string
				} else if (typeName.startsWith("java.util.List") || typeName.startsWith("java.util.Set")) {
					List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
					if (!typeArgs.isEmpty()) {
						var elementType = convertToTypeScriptType(typeArgs.getFirst());
						return elementType + "[]";
					}
					return "any[]";
				} else if (typeName.startsWith("java.util.Map")) {
					List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
					if (typeArgs.size() == 2) {
						var keyType = convertToTypeScriptType(typeArgs.getFirst());
						var valueType = convertToTypeScriptType(typeArgs.get(1));
						return "{ [key: " + keyType + "]: " + valueType + " }";
					}
					return "{ [key: string]: any }";
				} else if (typeName.startsWith("java.lang.")) {
					return "any";
				} else {
					// Custom class - use simple name
					return typeElement.getSimpleName().toString();
				}
			case ARRAY:
				var componentType = ((javax.lang.model.type.ArrayType) type).getComponentType();
				return convertToTypeScriptType(componentType) + "[]";
			case VOID:
				return "void";
			default:
				return "any";
		}
	}
}
