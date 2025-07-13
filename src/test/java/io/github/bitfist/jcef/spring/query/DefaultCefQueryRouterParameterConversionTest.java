package io.github.bitfist.jcef.spring.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DefaultCefQueryRouterParameterConversionTest {

    @InjectMocks
    private DefaultCefQueryRouter target;

    @Test
    @DisplayName("🔤 should return raw String when targetType is String")
    void testStringConversion() {
        Object result = target.convertParameter("hello", String.class);
        assertEquals("hello", result);
    }

    @Test
    @DisplayName("🔢 should parse int and Integer types")
    void testIntegerConversion() {
        Object primitiveInt = target.convertParameter("42", int.class);
        Object wrapperInt = target.convertParameter("99", Integer.class);
        assertEquals(42, primitiveInt);
        assertEquals(99, wrapperInt);
    }

    @Test
    @DisplayName("🔢 should parse long and Long types")
    void testLongConversion() {
        Object primitiveLong = target.convertParameter("1234567890123", long.class);
        Object wrapperLong = target.convertParameter("9876543210987", Long.class);
        assertEquals(1234567890123L, primitiveLong);
        assertEquals(9876543210987L, wrapperLong);
    }

    @Test
    @DisplayName("🟢 should parse boolean and Boolean types")
    void testBooleanConversion() {
        Object primitiveBool = target.convertParameter("true", boolean.class);
        Object wrapperBool = target.convertParameter("false", Boolean.class);
        assertEquals(true, primitiveBool);
        assertEquals(Boolean.FALSE, wrapperBool);
    }

    @Test
    @DisplayName("🔣 should parse double and Double types")
    void testDoubleConversion() {
        Object primitiveDouble = target.convertParameter("3.14", double.class);
        Object wrapperDouble = target.convertParameter("2.718", Double.class);
        assertEquals(3.14, (double) primitiveDouble, 0.0001);
        assertEquals(2.718, wrapperDouble);
    }

    @Test
    @DisplayName("➗ should parse float and Float types")
    void testFloatConversion() {
        Object primitiveFloat = target.convertParameter("1.23", float.class);
        Object wrapperFloat = target.convertParameter("4.56", Float.class);
        assertEquals(1.23f, (float) primitiveFloat, 0.0001f);
        assertEquals(4.56f, wrapperFloat);
    }

    @Test
    @DisplayName("🔢 should parse byte and Byte types")
    void testByteConversion() {
        Object primitiveByte = target.convertParameter("7", byte.class);
        Object wrapperByte = target.convertParameter("8", Byte.class);
        assertEquals((byte) 7, primitiveByte);
        assertEquals((byte) 8, wrapperByte);
    }

    @Test
    @DisplayName("🔢 should parse short and Short types")
    void testShortConversion() {
        Object primitiveShort = target.convertParameter("300", short.class);
        Object wrapperShort = target.convertParameter("400", Short.class);
        assertEquals((short) 300, primitiveShort);
        assertEquals((short) 400, wrapperShort);
    }

    @Test
    @DisplayName("🔤 should take first char for char/Character types")
    void testCharConversion() {
        Object primitiveChar = target.convertParameter("Zebra", char.class);
        Object wrapperChar = target.convertParameter("Xylophone", Character.class);
        assertEquals('Z', primitiveChar);
        assertEquals('X', wrapperChar);
    }

    @Test
    @DisplayName("📦 should convert to byte[] for byte[] type")
    void testByteArrayConversion() {
        String raw = "bytes";
        Object result = target.convertParameter(raw, byte[].class);
        assertArrayEquals(raw.getBytes(), (byte[]) result);
    }

    private enum Color {RED, GREEN, BLUE}

    @Test
    @DisplayName("🎨 should parse enums correctly")
    void testEnumConversion() {
        Object result = target.convertParameter("GREEN", Color.class);
        assertEquals(Color.GREEN, result);
    }
}
