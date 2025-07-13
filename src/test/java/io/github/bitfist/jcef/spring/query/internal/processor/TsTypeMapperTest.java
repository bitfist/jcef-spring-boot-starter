package io.github.bitfist.jcef.spring.query.internal.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("🗺️  TsTypeMapper Tests")
class TsTypeMapperTest {

    private TypeMirror primitiveMirror;
    private DeclaredType declaredMirror;
    private TypeElement element;

    @BeforeEach
    void setUp() {
        // common mocks for declared types
        declaredMirror = mock(DeclaredType.class);
        element = mock(TypeElement.class);
        when(declaredMirror.asElement()).thenReturn(element);
        // make declaredMirror.getKind() non-primitive so it skips the primitive branch
        when(declaredMirror.getKind()).thenReturn(TypeKind.DECLARED);
    }

    @Test
    void mapsVoidToVoid() {
        TypeMirror voidMirror = mock(TypeMirror.class);
        when(voidMirror.getKind()).thenReturn(TypeKind.VOID);

        assertEquals("void", TsTypeMapper.map(voidMirror));
    }

    @Test
    void mapsPrimitiveNumbersToNumber() {
        for (TypeKind k : new TypeKind[]{TypeKind.INT, TypeKind.LONG, TypeKind.FLOAT, TypeKind.DOUBLE, TypeKind.SHORT, TypeKind.BYTE}) {
            TypeMirror m = mock(TypeMirror.class);
            when(m.getKind()).thenReturn(k);
            assertEquals("number", TsTypeMapper.map(m), "Expected " + k + " → number");
        }
    }

    @Test
    void mapsPrimitiveBooleanToBoolean() {
        TypeMirror boolMirror = mock(TypeMirror.class);
        when(boolMirror.getKind()).thenReturn(TypeKind.BOOLEAN);

        assertEquals("boolean", TsTypeMapper.map(boolMirror));
    }

    @Test
    void mapsOtherPrimitivesToAny() {
        // e.g. char
        TypeMirror other = mock(TypeMirror.class);
        when(other.getKind()).thenReturn(TypeKind.CHAR);

        assertEquals("any", TsTypeMapper.map(other));
    }

    @Test
    void mapsBoxedStringToString() {
        stubFqn("java.lang.String", "String");
        assertEquals("string", TsTypeMapper.map(declaredMirror));
    }

    @Test
    void mapsBoxedNumberWrappersToNumber() {
        String[] wrappers = {
                "java.lang.Integer", "java.lang.Long",
                "java.lang.Double", "java.lang.Float",
                "java.lang.Short",  "java.lang.Byte"
        };
        for (String fqn : wrappers) {
            stubFqn(fqn, simpleName(fqn));
            assertEquals("number", TsTypeMapper.map(declaredMirror), "Expected " + fqn + " → number");
        }
    }

    @Test
    void mapsBoxedBooleanToBoolean() {
        stubFqn("java.lang.Boolean", "Boolean");
        assertEquals("boolean", TsTypeMapper.map(declaredMirror));
    }

    @Test
    void mapsUnknownDeclaredToSimpleName() {
        stubFqn("com.example.CustomType", "CustomType");
        assertEquals("CustomType", TsTypeMapper.map(declaredMirror));
    }

    // helper to stub element.getQualifiedName() and getSimpleName()
    private void stubFqn(String fqn, String simple) {
        // Mock a Name for the qualified name
        Name qualifiedNameMock = mock(Name.class);
        when(qualifiedNameMock.toString()).thenReturn(fqn);

        // Mock a Name for the simple name
        Name simpleNameMock = mock(Name.class);
        when(simpleNameMock.toString()).thenReturn(simple);

        // Wire them up on the TypeElement
        when(element.getQualifiedName()).thenReturn(qualifiedNameMock);
        when(element.getSimpleName()).thenReturn(simpleNameMock);
    }

    // extract simple name from FQN if needed
    private String simpleName(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }
}

