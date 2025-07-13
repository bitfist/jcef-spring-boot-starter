package io.github.bitfist.jcef.spring.query.internal.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("📝  TypeScriptGeneratorProcessor Tests")
class TypeScriptConfigurationGeneratorProcessorTest {

    @Mock
    private ProcessingEnvironment env;
    @Mock
    private Messager messager;
    @Mock
    private Elements elementUtils;

    private TypeScriptGeneratorProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TypeScriptGeneratorProcessor();
        when(env.getMessager()).thenReturn(messager);
    }

    @Test
    @DisplayName("⏭️  init() skips generation for unsupported output type")
    void initSkipsForUnsupportedType() throws Exception {
        Map<String, String> opts = Map.of(
                TypeScriptGeneratorProcessor.OPT_OUTPUT_TYPE, "java",
                TypeScriptGeneratorProcessor.OPT_OUTPUT_PATH, "/tmp/out"
        );
        when(env.getOptions()).thenReturn(opts);

        processor.init(env);

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(messager).printMessage(eq(Kind.WARNING), cap.capture());
        assertTrue(cap.getValue().contains("unsupported output type"));
        assertNull(getField("copier"));
        assertNull(getField("writer"));
        assertNull(getField("scanner"));
    }

    @Test
    @DisplayName("⏭️  init() skips generation for blank output path")
    void initSkipsForBlankPath() throws Exception {
        Map<String, String> opts = Map.of(
                TypeScriptGeneratorProcessor.OPT_OUTPUT_TYPE, "typescript",
                TypeScriptGeneratorProcessor.OPT_OUTPUT_PATH, "   "
        );
        when(env.getOptions()).thenReturn(opts);

        processor.init(env);

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(messager).printMessage(eq(Kind.WARNING), cap.capture());
        assertTrue(cap.getValue().contains(TypeScriptGeneratorProcessor.OPT_OUTPUT_PATH));
        assertNull(getField("copier"));
        assertNull(getField("writer"));
        assertNull(getField("scanner"));
    }

    @Test
    @DisplayName("🛠️  process() copies assets, writes files & logs I/O errors")
    void processInvokesCopyAndWriteAndLogsErrorOnException() throws Exception {
        // valid init
        Map<String, String> opts = Map.of(
                TypeScriptGeneratorProcessor.OPT_OUTPUT_TYPE, "typescript",
                TypeScriptGeneratorProcessor.OPT_OUTPUT_PATH, "/tmp/out"
        );
        when(env.getOptions()).thenReturn(opts);
        when(env.getElementUtils()).thenReturn(elementUtils);
        processor.init(env);

        // inject mocks via initialize()
        var copierMock = mock(ResourceCopier.class);
        var scannerMock = mock(ModelScanner.class);
        var writerMock = mock(TsFileWriter.class);
        processor.initialize(copierMock, scannerMock, writerMock);

        // prepare two handler models
        var goodModel = mock(HandlerModel.class);
        var badModel = mock(HandlerModel.class);
        var goodType = mock(TypeElement.class);
        var badType = mock(TypeElement.class);

        // **Corrected**: mock Name, not raw CharSequence
        Name goodName = mock(Name.class);
        Name badName = mock(Name.class);
        when(goodName.toString()).thenReturn("GoodController");
        when(badName.toString()).thenReturn("BadController");
        when(goodType.getSimpleName()).thenReturn(goodName);
        when(badType.getSimpleName()).thenReturn(badName);

        when(goodModel.javaType()).thenReturn(goodType);
        when(badModel.javaType()).thenReturn(badType);
        when(scannerMock.scan(any(RoundEnvironment.class)))
                .thenReturn(Arrays.asList(goodModel, badModel));

        doNothing().when(writerMock).write(goodModel);
        doThrow(new IOException("disk error")).when(writerMock).write(badModel);

        boolean result = processor.process(Set.of(), mock(RoundEnvironment.class));

        assertTrue(result);
        verify(copierMock).copyStaticAssets();
        verify(writerMock).write(goodModel);
        verify(writerMock).write(badModel);

        ArgumentCaptor<String> errCap = ArgumentCaptor.forClass(String.class);
        verify(messager).printMessage(eq(Kind.ERROR), errCap.capture());
        assertTrue(errCap.getValue().contains("BadController"));
        assertTrue(errCap.getValue().contains("disk error"));
    }

    private Object getField(String name) throws Exception {
        var f = TypeScriptGeneratorProcessor.class
                .getDeclaredField(name);
        f.setAccessible(true);
        return f.get(processor);
    }
}
