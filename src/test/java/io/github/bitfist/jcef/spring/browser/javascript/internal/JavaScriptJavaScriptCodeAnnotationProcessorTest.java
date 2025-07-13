package io.github.bitfist.jcef.spring.browser.javascript.internal;

import io.github.bitfist.jcef.spring.browser.javascript.JavaScriptCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("🧪 CodeAnnotationProcessor Tests")
class JavaScriptJavaScriptCodeAnnotationProcessorTest {

    @Mock
    private ProcessingEnvironment processingEnv;

    @Mock
    private Filer filer;

    @Mock
    private Messager messager;

    @Mock
    private Elements elementUtils;

    private JavaScriptCodeAnnotationProcessor processor;

    @BeforeEach
    @DisplayName("🔄 Initialize processor before each test")
    void setUp() {
        when(processingEnv.getFiler()).thenReturn(filer);
        when(processingEnv.getMessager()).thenReturn(messager);
        when(processingEnv.getElementUtils()).thenReturn(elementUtils);

        processor = new JavaScriptCodeAnnotationProcessor();
        processor.init(processingEnv);
    }

    @Test
    @DisplayName("✅ Generates implementation for a single annotated method")
    void testSingleMethodGeneration() throws Exception {
        // Mock a single annotated method
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("testMethod");
        when(method.getSimpleName()).thenReturn(methodName);

        // Enclosing interface
        TypeElement iface = mock(TypeElement.class);
        Name ifaceName = mock(Name.class);
        when(ifaceName.toString()).thenReturn("TestInterface");
        when(iface.getSimpleName()).thenReturn(ifaceName);
        when(method.getEnclosingElement()).thenReturn(iface);

        // Package for interface
        PackageElement pkg = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("com.test");
        when(pkg.getQualifiedName()).thenReturn(pkgName);
        when(elementUtils.getPackageOf(iface)).thenReturn(pkg);

        // Annotation value
        JavaScriptCode javaScriptCodeAnnotation = new JavaScriptCode() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return JavaScriptCode.class; }
            @Override public String value() { return "alert(\\\":param\\\")"; }
        };
        when(method.getAnnotation(JavaScriptCode.class)).thenReturn(javaScriptCodeAnnotation);
        TypeMirror returnType = mock(TypeMirror.class);
        when(returnType.getKind()).thenReturn(TypeKind.VOID);
        when(method.getReturnType()).thenReturn(returnType);

        // One primitive parameter
        VariableElement param = mock(VariableElement.class);
        Name paramName = mock(Name.class);
        when(paramName.toString()).thenReturn("param");
        when(param.getSimpleName()).thenReturn(paramName);
        TypeMirror paramType = mock(TypeMirror.class);
        when(paramType.getKind()).thenReturn(TypeKind.INT);
        when(param.asType()).thenReturn(paramType);
        doReturn(List.of(param)).when(method).getParameters();

        // Stub filer to capture output
        JavaFileObject fileObject = mock(JavaFileObject.class);
        StringWriter writer = new StringWriter();
        when(filer.createSourceFile("com.test.TestInterfaceImpl")).thenReturn(fileObject);
        when(fileObject.openWriter()).thenReturn(writer);

        // Round environment
        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        doReturn(Set.of(method)).when(roundEnv).getElementsAnnotatedWith(JavaScriptCode.class);

        // Execute
        boolean result = processor.process(Set.of(), roundEnv);
        assertTrue(result);

        String output = writer.toString();
        assertTrue(output.contains("package com.test;"));
        assertTrue(output.contains("class TestInterfaceImpl implements TestInterface"));
        assertTrue(output.contains("private static final String TESTMETHOD_CODE"));
        assertTrue(output.contains("executor.execute(code);"));

        // No errors
        verify(messager, never()).printMessage(any(Diagnostic.Kind.class), anyString());
    }

    @Test
    @DisplayName("🔧 Handles object parameters with JSON serialization")
    void testObjectParamHandling() throws Exception {
        // Mock a single annotated method with an object parameter
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("doObject");
        when(method.getSimpleName()).thenReturn(methodName);

        TypeElement iface = mock(TypeElement.class);
        Name ifaceName = mock(Name.class);
        when(ifaceName.toString()).thenReturn("ObjInterface");
        when(iface.getSimpleName()).thenReturn(ifaceName);
        when(method.getEnclosingElement()).thenReturn(iface);

        PackageElement pkg = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("com.obj");
        when(pkg.getQualifiedName()).thenReturn(pkgName);
        when(elementUtils.getPackageOf(iface)).thenReturn(pkg);

        JavaScriptCode javaScriptCodeAnnotation = new JavaScriptCode() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return JavaScriptCode.class; }
            @Override public String value() { return "process(:obj)"; }
        };
        when(method.getAnnotation(JavaScriptCode.class)).thenReturn(javaScriptCodeAnnotation);
        TypeMirror returnType = mock(TypeMirror.class);
        when(returnType.getKind()).thenReturn(TypeKind.VOID);
        when(method.getReturnType()).thenReturn(returnType);

        // One object parameter
        VariableElement param = mock(VariableElement.class);
        Name paramName = mock(Name.class);
        when(paramName.toString()).thenReturn("obj");
        when(param.getSimpleName()).thenReturn(paramName);
        TypeMirror paramType = mock(TypeMirror.class);
        when(paramType.getKind()).thenReturn(TypeKind.DECLARED);
        when(paramType.toString()).thenReturn("com.example.MyType");
        when(param.asType()).thenReturn(paramType);
        doReturn(List.of(param)).when(method).getParameters();

        JavaFileObject fileObject = mock(JavaFileObject.class);
        StringWriter writer = new StringWriter();
        when(filer.createSourceFile("com.obj.ObjInterfaceImpl")).thenReturn(fileObject);
        when(fileObject.openWriter()).thenReturn(writer);

        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        doReturn(Set.of(method)).when(roundEnv).getElementsAnnotatedWith(JavaScriptCode.class);

        boolean result = processor.process(Set.of(), roundEnv);
        assertTrue(result);

        String output = writer.toString();
        // Check that objectMapper is used and try/catch around JsonProcessingException
        assertTrue(output.contains("objectMapper.writeValueAsString(obj)"));
        assertTrue(output.contains("catch (com.fasterxml.jackson.core.JsonProcessingException e)"));
        // Parameter type in signature
        assertTrue(output.contains("com.example.MyType obj"));
    }

    @Test
    @DisplayName("💥 Reports IOException errors via Messager")
    void testIOExceptionReporting() throws Exception {
        // Setup a single method as before
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("failMethod");
        when(method.getSimpleName()).thenReturn(methodName);

        TypeElement iface = mock(TypeElement.class);
        Name ifaceName = mock(Name.class);
        when(ifaceName.toString()).thenReturn("FailInterface");
        when(iface.getSimpleName()).thenReturn(ifaceName);
        when(method.getEnclosingElement()).thenReturn(iface);

        PackageElement pkg = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("com.fail");
        when(pkg.getQualifiedName()).thenReturn(pkgName);
        when(elementUtils.getPackageOf(iface)).thenReturn(pkg);

        JavaScriptCode javaScriptCodeAnnotation = new JavaScriptCode() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return JavaScriptCode.class; }
            @Override public String value() { return "won't write"; }
        };
        when(method.getAnnotation(JavaScriptCode.class)).thenReturn(javaScriptCodeAnnotation);
        when(method.getParameters()).thenReturn(List.of());
        TypeMirror returnType = mock(TypeMirror.class);
        when(returnType.getKind()).thenReturn(TypeKind.VOID);
        when(method.getReturnType()).thenReturn(returnType);

        JavaFileObject fileObject = mock(JavaFileObject.class);
        when(filer.createSourceFile("com.fail.FailInterfaceImpl")).thenReturn(fileObject);
        when(fileObject.openWriter()).thenThrow(new IOException("disk full"));

        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        doReturn(Set.of(method)).when(roundEnv).getElementsAnnotatedWith(JavaScriptCode.class);

        // Should not throw, but report error
        assertTrue(processor.process(Set.of(), roundEnv));
        verify(messager).printMessage(eq(Diagnostic.Kind.ERROR), contains("Failed to write FailInterfaceImpl: disk full"));
    }

    @Test
    @DisplayName("🚫 Ignores non-method elements")
    void testIgnoresNonMethodElements() {
        // ElementKind.CLASS should be ignored
        Element notAMethod = mock(Element.class);
        when(notAMethod.getKind()).thenReturn(ElementKind.CLASS);

        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        doReturn(Set.of(notAMethod)).when(roundEnv).getElementsAnnotatedWith(JavaScriptCode.class);

        assertTrue(processor.process(Set.of(), roundEnv));
        verifyNoInteractions(filer);
        verifyNoInteractions(messager);
    }

    @Test
    @DisplayName("🔀 Processes multiple interfaces in one round")
    void testMultipleInterfaces() throws Exception {
        // First interface + method
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        Name m1Name = mock(Name.class);
        when(m1Name.toString()).thenReturn("methodOne");
        when(method.getSimpleName()).thenReturn(m1Name);

        TypeElement iface1 = mock(TypeElement.class);
        Name iface1Name = mock(Name.class);
        when(iface1Name.toString()).thenReturn("InterfaceOne");
        when(iface1.getSimpleName()).thenReturn(iface1Name);
        when(method.getEnclosingElement()).thenReturn(iface1);

        PackageElement pkg1 = mock(PackageElement.class);
        Name pkg1Name = mock(Name.class);
        when(pkg1Name.toString()).thenReturn("com.pkg1");
        when(pkg1.getQualifiedName()).thenReturn(pkg1Name);
        when(elementUtils.getPackageOf(iface1)).thenReturn(pkg1);

        JavaScriptCode javaScriptCodeAnno1 = new JavaScriptCode() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return JavaScriptCode.class; }
            @Override public String value() { return "code1"; }
        };
        when(method.getAnnotation(JavaScriptCode.class)).thenReturn(javaScriptCodeAnno1);
        TypeMirror returnType = mock(TypeMirror.class);
        when(returnType.getKind()).thenReturn(TypeKind.VOID);
        when(method.getReturnType()).thenReturn(returnType);

        VariableElement p1 = mock(VariableElement.class);
        Name p1Name = mock(Name.class);
        when(p1Name.toString()).thenReturn("param1");
        when(p1.getSimpleName()).thenReturn(p1Name);
        TypeMirror p1Type = mock(TypeMirror.class);
        when(p1Type.getKind()).thenReturn(TypeKind.INT);
        when(p1.asType()).thenReturn(p1Type);
        doReturn(List.of(p1)).when(method).getParameters();

        JavaFileObject file1 = mock(JavaFileObject.class);
        StringWriter w1 = new StringWriter();
        when(filer.createSourceFile("com.pkg1.InterfaceOneImpl")).thenReturn(file1);
        when(file1.openWriter()).thenReturn(w1);

        // Second interface + method
        ExecutableElement method2 = mock(ExecutableElement.class);
        when(method2.getKind()).thenReturn(ElementKind.METHOD);
        Name m2Name = mock(Name.class);
        when(m2Name.toString()).thenReturn("methodTwo");
        when(method2.getSimpleName()).thenReturn(m2Name);

        TypeElement iface2 = mock(TypeElement.class);
        Name iface2Name = mock(Name.class);
        when(iface2Name.toString()).thenReturn("InterfaceTwo");
        when(iface2.getSimpleName()).thenReturn(iface2Name);
        when(method2.getEnclosingElement()).thenReturn(iface2);

        PackageElement pkg2 = mock(PackageElement.class);
        Name pkg2Name = mock(Name.class);
        when(pkg2Name.toString()).thenReturn("com.pkg2");
        when(pkg2.getQualifiedName()).thenReturn(pkg2Name);
        when(elementUtils.getPackageOf(iface2)).thenReturn(pkg2);

        JavaScriptCode javaScriptCodeAnno2 = new JavaScriptCode() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return JavaScriptCode.class; }
            @Override public String value() { return "code2"; }
        };
        when(method2.getAnnotation(JavaScriptCode.class)).thenReturn(javaScriptCodeAnno2);
        when(method2.getReturnType()).thenReturn(returnType);

        VariableElement p2 = mock(VariableElement.class);
        Name p2Name = mock(Name.class);
        when(p2Name.toString()).thenReturn("param2");
        when(p2.getSimpleName()).thenReturn(p2Name);
        TypeMirror p2Type = mock(TypeMirror.class);
        when(p2Type.getKind()).thenReturn(TypeKind.INT);
        when(p2.asType()).thenReturn(p2Type);
        doReturn(List.of(p2)).when(method2).getParameters();

        JavaFileObject file2 = mock(JavaFileObject.class);
        StringWriter w2 = new StringWriter();
        when(filer.createSourceFile("com.pkg2.InterfaceTwoImpl")).thenReturn(file2);
        when(file2.openWriter()).thenReturn(w2);

        RoundEnvironment roundEnv = mock(RoundEnvironment.class);
        doReturn(Set.of(method, method2)).when(roundEnv).getElementsAnnotatedWith(JavaScriptCode.class);

        boolean result = processor.process(Set.of(), roundEnv);
        assertTrue(result);

        String out1 = w1.toString();
        assertTrue(out1.contains("package com.pkg1;"));
        assertTrue(out1.contains("class InterfaceOneImpl implements InterfaceOne"));

        String out2 = w2.toString();
        assertTrue(out2.contains("package com.pkg2;"));
        assertTrue(out2.contains("class InterfaceTwoImpl implements InterfaceTwo"));
    }
}
