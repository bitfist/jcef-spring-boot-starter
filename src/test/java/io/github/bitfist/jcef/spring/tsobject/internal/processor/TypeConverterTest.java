package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypeConverterTest {

	private final TypeConverter converter = new TypeConverter();

	@Test
	@DisplayName("🔹 should convert void type to 'void'")
	void testVoidType() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.VOID);
		assertEquals("void", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔸 should convert primitive boolean to 'boolean'")
	void testPrimitiveBoolean() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.BOOLEAN);
		assertEquals("boolean", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔸 should convert primitive char to 'string'")
	void testPrimitiveChar() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.CHAR);
		assertEquals("string", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔸 should convert primitive int to 'number'")
	void testPrimitiveNumber() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.INT);
		assertEquals("number", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔹 should convert java.lang.String to 'string'")
	void testStringType() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.DECLARED);
		when(tm.toString()).thenReturn("java.lang.String");
		assertEquals("string", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔹 should convert boxed Integer to 'number'")
	void testBoxedInteger() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.DECLARED);
		when(tm.toString()).thenReturn("java.lang.Integer");
		assertEquals("number", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔹 should convert java.util.Date to 'Date'")
	void testDateType() {
		TypeMirror tm = mock(TypeMirror.class);
		when(tm.getKind()).thenReturn(TypeKind.DECLARED);
		when(tm.toString()).thenReturn("java.util.Date");
		assertEquals("Date", converter.toTypeScript(tm));
	}

	@Test
	@DisplayName("🔹 should convert array of String to 'string[]'")
	void testArrayType() {
		ArrayType arr = mock(ArrayType.class);
		TypeMirror comp = mock(TypeMirror.class);
		when(comp.getKind()).thenReturn(TypeKind.DECLARED);
		when(comp.toString()).thenReturn("java.lang.String");
		when(arr.getComponentType()).thenReturn(comp);
		when(arr.getKind()).thenReturn(TypeKind.ARRAY);
		assertEquals("string[]", converter.toTypeScript(arr));
	}

	@Test
	@DisplayName("🔹 should convert raw List to 'any[]'")
	void testRawList() {
		DeclaredType dt = mock(DeclaredType.class);
		when(dt.getKind()).thenReturn(TypeKind.DECLARED);
		when(dt.toString()).thenReturn("java.util.List");
		when(dt.getTypeArguments()).thenReturn(Collections.emptyList());
		assertEquals("any[]", converter.toTypeScript(dt));
	}

	@Test
	@DisplayName("🔹 should convert List<String> to 'string[]'")
	void testGenericList() {
		DeclaredType dt = mock(DeclaredType.class);
		TypeMirror arg = mock(TypeMirror.class);
		when(arg.getKind()).thenReturn(TypeKind.DECLARED);
		when(arg.toString()).thenReturn("java.lang.String");
		when(dt.getKind()).thenReturn(TypeKind.DECLARED);
		when(dt.toString()).thenReturn("java.util.List<java.lang.String>");
		doReturn(List.of(arg)).when(dt).getTypeArguments();
		assertEquals("string[]", converter.toTypeScript(dt));
	}

	@Test
	@DisplayName("🔹 should convert raw Map to '{ [key: string]: any }'")
	void testRawMap() {
		DeclaredType dt = mock(DeclaredType.class);
		when(dt.getKind()).thenReturn(TypeKind.DECLARED);
		when(dt.toString()).thenReturn("java.util.Map");
		when(dt.getTypeArguments()).thenReturn(Collections.emptyList());
		assertEquals("{ [key: string]: any }", converter.toTypeScript(dt));
	}

	@Test
	@DisplayName("🔹 should convert Map<String, Integer> to object literal type")
	void testGenericMap() {
		DeclaredType dt = mock(DeclaredType.class);
		TypeMirror key = mock(TypeMirror.class);
		TypeMirror val = mock(TypeMirror.class);
		when(key.getKind()).thenReturn(TypeKind.DECLARED);
		when(key.toString()).thenReturn("java.lang.String");
		when(val.getKind()).thenReturn(TypeKind.DECLARED);
		when(val.toString()).thenReturn("java.lang.Integer");
		when(dt.getKind()).thenReturn(TypeKind.DECLARED);
		when(dt.toString()).thenReturn("java.util.Map<java.lang.String, java.lang.Integer>");
		doReturn(List.of(key, val)).when(dt).getTypeArguments();
		assertEquals("{ [key: string]: number }", converter.toTypeScript(dt));
	}

	@Test
	@DisplayName("🔸 should convert custom declared type to its simple name")
	void testDeclaredType() {
		DeclaredType dt = mock(DeclaredType.class);
		Element elem = mock(Element.class);
		Name nm = mock(Name.class);
		when(nm.toString()).thenReturn("MyClass");
		when(elem.getSimpleName()).thenReturn(nm);
		when(dt.getKind()).thenReturn(TypeKind.DECLARED);
		when(dt.toString()).thenReturn("com.example.MyClass");
		when(dt.asElement()).thenReturn(elem);
		assertEquals("MyClass", converter.toTypeScript(dt));
	}

	@Test
	@DisplayName("🔸 getCefResponseType should map TypeScript types to CEF types")
	void testGetCefResponseType() {
		TypeMirror tStr = mock(TypeMirror.class);
		when(tStr.getKind()).thenReturn(TypeKind.DECLARED);
		when(tStr.toString()).thenReturn("java.lang.String");

		TypeMirror tBool = mock(TypeMirror.class);
		when(tBool.getKind()).thenReturn(TypeKind.BOOLEAN);

		TypeMirror tNum = mock(TypeMirror.class);
		when(tNum.getKind()).thenReturn(TypeKind.INT);

		DeclaredType tObj = mock(DeclaredType.class);
		when(tObj.getKind()).thenReturn(TypeKind.DECLARED);
		when(tObj.toString()).thenReturn("com.example.Custom");
		Element elem = mock(Element.class);
		Name nm = mock(Name.class);
		when(nm.toString()).thenReturn("Custom");
		when(elem.getSimpleName()).thenReturn(nm);
		doReturn(elem).when(tObj).asElement();

		assertEquals("string", converter.getCefResponseType(tStr));
		assertEquals("boolean", converter.getCefResponseType(tBool));
		assertEquals("number", converter.getCefResponseType(tNum));
		assertEquals("object", converter.getCefResponseType(tObj));
	}
}