package io.github.bitfist.jcef.spring.query.internal.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * 🔄 Maps Java TypeMirrors to their corresponding TypeScript type representations.
 */
final class TsTypeMapper {

    private TsTypeMapper() {
    }

    static String map(TypeMirror t) {
        TypeKind k = t.getKind();

        // handle void
        if (k == TypeKind.VOID) {
            return "void";
        }

        // handle all other primitive types
        if (k.isPrimitive()) {
            return switch (k) {
                case INT, LONG, FLOAT, DOUBLE, SHORT, BYTE -> "number";
                case BOOLEAN -> "boolean";
                default -> "any";
            };
        }

        // non-primitive: map boxed types and others
        DeclaredType dt = (DeclaredType) t;
        String fqn = ((TypeElement) dt.asElement()).getQualifiedName().toString();
        return switch (fqn) {
            case "java.lang.String" -> "string";
            case "java.lang.Integer", "java.lang.Long",
                 "java.lang.Double", "java.lang.Float",
                 "java.lang.Short", "java.lang.Byte" -> "number";
            case "java.lang.Boolean" -> "boolean";
            default -> dt.asElement().getSimpleName().toString();
        };
    }
}

