package io.github.bitfist.jcef.spring.query.internal.processor;

import io.github.bitfist.jcef.spring.query.CefQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("🛠️  MethodRenderer Tests")
class MethodRendererTest {

    private MethodRenderer renderer;
    private Messager messager;
    private StringWriter sw;
    private BufferedWriter writer;

    @BeforeEach
    void setup() {
        renderer = new MethodRenderer();
        messager = mock(Messager.class);
        sw = new StringWriter();
        writer = new BufferedWriter(sw);
    }

    @Test
    @DisplayName("🔧  Renders a method containing a path parameter")
    void rendersMethodWithPathParam() throws Exception {
        // (mocks identical to original test)
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getSimpleName()).thenReturn(simpleName("getUser"));
        when(method.getAnnotation(CefQueryHandler.class)).thenReturn(annot("users/{id}"));

        // return type – String
        DeclaredType dtRet = mock(DeclaredType.class);
        TypeElement teRet = mock(TypeElement.class);
        when(teRet.getQualifiedName()).thenReturn(simpleName("java.lang.String"));
        when(dtRet.asElement()).thenReturn(teRet);
        when(dtRet.getKind()).thenReturn(TypeKind.DECLARED);
        when(method.getReturnType()).thenReturn(dtRet);

        // parameter id:int
        VariableElement param = mock(VariableElement.class);
        when(param.getSimpleName()).thenReturn(simpleName("id"));
        TypeMirror pType = mock(TypeMirror.class);
        when(pType.getKind()).thenReturn(TypeKind.INT);
        when(param.asType()).thenReturn(pType);
        doReturn(singletonList(param)).when(method).getParameters();

        renderer.render(writer, "/api", method, messager);
        writer.flush();
        String out = sw.toString();

        assertTrue(out.contains("static getUser(id: number): Promise<string>"));
        assertTrue(Pattern.compile("`/api/users/\\$\\{id}`").matcher(out).find());
        assertTrue(out.contains("CefQueryService.request<string>("));
    }

    // helpers
    private Name simpleName(String s) {
        return new Name() {
            public boolean contentEquals(CharSequence cs) {
                return s.contentEquals(cs);
            }

            public int length() {
                return s.length();
            }

            public char charAt(int i) {
                return s.charAt(i);
            }

            public Name subSequence(int s, int e) {
                return this;
            }

            public String toString() {
                return s;
            }
        };
    }

    private CefQueryHandler annot(String v) {
        return new CefQueryHandler() {
            public String value() {
                return v;
            }

            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return CefQueryHandler.class;
            }
        };
    }
}
