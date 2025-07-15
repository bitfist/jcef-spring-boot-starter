package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Converts Java types (TypeMirror) to TypeScript type strings.
 */
class TypeConverter {

    String toTypeScript(TypeMirror typeMirror) {
        // Handle void
        if (typeMirror.getKind() == TypeKind.VOID) {
            return "void";
        }

        // Handle primitives
        if (typeMirror.getKind().isPrimitive()) {
            return switch (typeMirror.getKind()) {
                case BOOLEAN -> "boolean";
                case CHAR -> "string";
                default -> "number";
            };
        }

        // Handle Arrays
        if (typeMirror.getKind() == TypeKind.ARRAY) {
            var arrayType = (ArrayType) typeMirror;
            return toTypeScript(arrayType.getComponentType()) + "[]";
        }

        // Handle boxed primitives and common types
        var typeString = typeMirror.toString();
        if (typeString.equals("java.lang.String")) return "string";
        if (typeString.equals("java.lang.Boolean")) return "boolean";
        if (typeString.matches("java\\.lang\\.(Byte|Short|Integer|Long|Float|Double)")) return "number";
        if (typeString.equals("java.util.Date")) return "Date";

        // Handle Collections
        if (typeString.startsWith("java.util.List") || typeString.startsWith("java.util.Set")) {
            var declaredType = (DeclaredType) typeMirror;
            if (declaredType.getTypeArguments().isEmpty()) {
                return "any[]"; // Raw list/set
            }
            return toTypeScript(declaredType.getTypeArguments().getFirst()) + "[]";
        }

        // Handle Map
        if (typeString.startsWith("java.util.Map")) {
            var declaredType = (DeclaredType) typeMirror;
            if (declaredType.getTypeArguments().size() == 2) {
                var keyType = toTypeScript(declaredType.getTypeArguments().get(0));
                var valueType = toTypeScript(declaredType.getTypeArguments().get(1));
                return "{ [key: " + keyType + "]: " + valueType + " }";
            }
            return "{ [key: string]: any }"; // Raw map
        }

        // Handle complex objects
        if (typeMirror.getKind() == TypeKind.DECLARED) {
            return ((DeclaredType) typeMirror).asElement().getSimpleName().toString();
        }

        return "any";
    }

    public String getCefResponseType(TypeMirror returnType) {
        var tsType = toTypeScript(returnType);
        return switch (tsType) {
            case "string" -> "string";
            case "boolean" -> "boolean";
            case "number" -> "number";
            default -> "object";
        };
    }
}