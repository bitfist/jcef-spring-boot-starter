package io.github.bitfist.jcef.spring.query.internal.processor;

import io.github.bitfist.jcef.spring.query.TypeScriptConfiguration;
import io.github.bitfist.jcef.spring.query.CefQueryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("🔍  ModelScanner Tests")
class ModelScannerTest {

    @Mock Elements elements;
    @Mock RoundEnvironment roundEnv;

    ModelScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new ModelScanner(elements);
    }

    @Test
    @DisplayName("🚫  Returns empty collection when no handlers are present")
    void returnsEmptyWhenNoHandlersPresent() {
        when(roundEnv.getElementsAnnotatedWith(CefQueryHandler.class))
                .thenReturn(Collections.emptySet());

        assertTrue(scanner.scan(roundEnv).isEmpty());
    }

    @Test
    @DisplayName("📦  Detects handler with default route & same package")
    void detectsHandlerWithDefaultRouteAndPackage() {
        // --- class stub ---
        TypeElement clazz = mock(TypeElement.class);
        when(clazz.getKind()).thenReturn(ElementKind.CLASS);

        CefQueryHandler handlerAnn = mock(CefQueryHandler.class);
        when(handlerAnn.value()).thenReturn("");  // default route
        when(clazz.getAnnotation(CefQueryHandler.class)).thenReturn(handlerAnn);

        // stub package info
        PackageElement pkgElem = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("com.example");
        when(pkgElem.getQualifiedName()).thenReturn(pkgName);
        when(elements.getPackageOf(clazz)).thenReturn(pkgElem);

        doReturn(Set.of(clazz)).when(roundEnv).getElementsAnnotatedWith(CefQueryHandler.class);

        Collection<HandlerModel> models = scanner.scan(roundEnv);
        assertEquals(1, models.size());
        HandlerModel m = models.iterator().next();

        assertSame(clazz, m.javaType());
        assertEquals("", m.routePrefix(), "Missing value() should default to empty string");
        assertEquals("com.example", m.tsPackage(), "Should use packageOf(clazz)");
        assertTrue(m.handlerMethods().isEmpty(), "No methods mean empty handlerMethods");
        assertTrue(m.complexDtos().isEmpty(), "No methods mean no complex DTOs");
    }

    @Test
    @DisplayName("🎯  Respects @TypeScript package override")
    void respectsTypeScriptPackageOverride() {
        // --- class stub ---
        TypeElement clazz = mock(TypeElement.class);
        when(clazz.getKind()).thenReturn(ElementKind.CLASS);

        CefQueryHandler handlerAnn = mock(CefQueryHandler.class);
        when(handlerAnn.value()).thenReturn("api");
        when(clazz.getAnnotation(CefQueryHandler.class)).thenReturn(handlerAnn);

        // override annotation stub
        TypeScriptConfiguration tsAnn = mock(TypeScriptConfiguration.class);
        when(tsAnn.packageName()).thenReturn("over.pkg");
        when(clazz.getAnnotation(TypeScriptConfiguration.class)).thenReturn(tsAnn);

        // still stub getPackageOf to avoid NPE if override path mis-fires
        PackageElement dummyPkg = mock(PackageElement.class);
        Name dummyName = mock(Name.class);
        when(dummyName.toString()).thenReturn("<should-not-be-used>");
        when(dummyPkg.getQualifiedName()).thenReturn(dummyName);
        when(elements.getPackageOf(clazz)).thenReturn(dummyPkg);

        doReturn(Set.of(clazz)).when(roundEnv).getElementsAnnotatedWith(CefQueryHandler.class);

        HandlerModel m = scanner.scan(roundEnv).iterator().next();
        assertEquals("api",     m.routePrefix());
        assertEquals("over.pkg", m.tsPackage());
    }

    @Test
    @DisplayName("🧮  Groups methods and detects complex DTOs")
    void groupsMethodsAndDetectsComplexDto() {
        // --- class stub ---
        TypeElement clazz = mock(TypeElement.class);
        when(clazz.getKind()).thenReturn(ElementKind.CLASS);

        CefQueryHandler handlerAnn = mock(CefQueryHandler.class);
        when(handlerAnn.value()).thenReturn("r");
        when(clazz.getAnnotation(CefQueryHandler.class)).thenReturn(handlerAnn);

        // package stub
        PackageElement pkgElem = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("pkg");
        when(pkgElem.getQualifiedName()).thenReturn(pkgName);
        when(elements.getPackageOf(clazz)).thenReturn(pkgElem);

        // --- method stub ---
        ExecutableElement method = mock(ExecutableElement.class);
        when(method.getKind()).thenReturn(ElementKind.METHOD);
        when(method.getEnclosingElement()).thenReturn(clazz);
        when(method.getAnnotation(CefQueryHandler.class)).thenReturn(handlerAnn);

        // return type → a custom DTO
        DeclaredType dtoType = mock(DeclaredType.class);
        when(dtoType.getKind()).thenReturn(TypeKind.DECLARED);
        TypeElement dtoElem = mock(TypeElement.class);
        Name dtoName = mock(Name.class);
        when(dtoName.toString()).thenReturn("my.custom.Dto");
        when(dtoElem.getQualifiedName()).thenReturn(dtoName);
        when(dtoType.asElement()).thenReturn(dtoElem);
        when(method.getReturnType()).thenReturn(dtoType);

        // one parameter → java.lang.String (primitive skip)
        VariableElement param = mock(VariableElement.class);
        DeclaredType stringType = mock(DeclaredType.class);
        when(stringType.getKind()).thenReturn(TypeKind.DECLARED);
        TypeElement stringElem = mock(TypeElement.class);
        Name stringName = mock(Name.class);
        when(stringName.toString()).thenReturn("java.lang.String");
        when(stringElem.getQualifiedName()).thenReturn(stringName);
        when(stringType.asElement()).thenReturn(stringElem);
        when(param.asType()).thenReturn(stringType);
        doReturn(List.of(param)).when(method).getParameters();

        // Use LinkedHashSet to preserve insertion order: class → method
        Set<Element> anns = new LinkedHashSet<>();
        anns.add(clazz);
        anns.add(method);
        doReturn(anns).when(roundEnv).getElementsAnnotatedWith(CefQueryHandler.class);

        HandlerModel m = scanner.scan(roundEnv).iterator().next();

        // ensure grouping worked
        assertSame(clazz, m.javaType());
        assertEquals(List.of(method), m.handlerMethods(), "Should include the one method");

        // ensure only the custom DTO was picked up
        Map<String, TypeElement> dtos = m.complexDtos();
        assertEquals(1, dtos.size());
        assertTrue(dtos.containsKey("my.custom.Dto"));
        assertSame(dtoElem, dtos.get("my.custom.Dto"));
    }
}
