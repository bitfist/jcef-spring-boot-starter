package io.github.bitfist.jcef.spring.query.internal.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterfaceRendererTest {

    @Mock
    BufferedWriter writer;
    @Mock
    TypeElement dto;

    private final InterfaceRenderer renderer = new InterfaceRenderer();

    @Test
    @DisplayName("🖼 should render empty interface when no fields")
    void renderEmptyInterface() throws Exception {
        // given
        Name dtoName = mock(Name.class);
        when(dtoName.toString()).thenReturn("MyDto");
        when(dto.getSimpleName()).thenReturn(dtoName);
        when(dto.getEnclosedElements()).thenReturn(Collections.emptyList());

        // when
        renderer.render(writer, dto);

        // then
        InOrder inOrder = inOrder(writer);
        inOrder.verify(writer).write("export interface MyDto {\n");
        inOrder.verify(writer).write("}\n\n");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("🔧 should render interface with one field")
    void renderInterfaceWithOneField() throws Exception {
        // given
        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("username");

        TypeMirror fieldType = mock(TypeMirror.class);

        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.asType()).thenReturn(fieldType);

        Element other = mock(Element.class);
        when(other.getKind()).thenReturn(ElementKind.METHOD);

        Name dtoName = mock(Name.class);
        when(dtoName.toString()).thenReturn("UserDto");

        when(dto.getSimpleName()).thenReturn(dtoName);
        doReturn(Arrays.asList(other, field)).when(dto).getEnclosedElements();

        try (MockedStatic<TsTypeMapper> tsMock = mockStatic(TsTypeMapper.class)) {
            tsMock.when(() -> TsTypeMapper.map(fieldType)).thenReturn("string");

            // when
            renderer.render(writer, dto);
        }

        // then
        InOrder inOrder = inOrder(writer);
        inOrder.verify(writer).write("export interface UserDto {\n");
        inOrder.verify(writer).write("  username: string;\n");
        inOrder.verify(writer).write("}\n\n");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("💥 should wrap IOException in RuntimeException")
    void renderThrowsRuntimeExceptionOnFieldWriteError() throws Exception {
        // given
        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("badField");

        TypeMirror fieldType = mock(TypeMirror.class);
        when(fieldType.getKind()).thenReturn(TypeKind.INT);

        VariableElement field = mock(VariableElement.class);
        when(field.getKind()).thenReturn(ElementKind.FIELD);
        when(field.getSimpleName()).thenReturn(fieldName);
        when(field.asType()).thenReturn(fieldType);

        Name dtoName = mock(Name.class);
        when(dtoName.toString()).thenReturn("ErrorDto");

        when(dto.getSimpleName()).thenReturn(dtoName);
        doReturn(List.of(field)).when(dto).getEnclosedElements();

        doNothing().when(writer).write("export interface ErrorDto {\n");
        doThrow(new IOException("boom")).when(writer).write("  badField: number;\n");

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.render(writer, dto));

        // then
        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("boom", ex.getCause().getMessage());
    }
}
