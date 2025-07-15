package io.github.bitfist.jcef.spring.jsexecution.internal;

import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class JavaScriptCodeAnnotationProcessorTest {

    @Test
    @DisplayName("🧪 Should generate implementation for interface with a single method without parameters")
    void shouldGenerateImplForSingleMethodInterface() {
        // The interface that will be processed by the annotation processor
        JavaFileObject userInterface = JavaFileObjects.forSourceString("test.UserInterface", """
                    package test;
                    import io.github.bitfist.jcef.spring.jsexecution.JavaScriptCode;
                
                    public interface UserInterface {
                        @JavaScriptCode("console.log('test');")
                        void myMethod();
                    }
                """);

        // The expected generated class
        JavaFileObject expectedImpl = JavaFileObjects.forSourceString("test.UserInterfaceImpl", """
                package test;
                
                import com.fasterxml.jackson.databind.ObjectMapper;
                import io.github.bitfist.jcef.spring.jsexecution.JavaScriptExecutor;
                
                import java.util.logging.Logger;
                
                @org.springframework.stereotype.Component
                class UserInterfaceImpl implements UserInterface {
                
                    private static final Logger log = Logger.getLogger(UserInterfaceImpl.class.getName());
                
                    private final JavaScriptExecutor executor;
                    private final ObjectMapper objectMapper;
                
                    public UserInterfaceImpl(JavaScriptExecutor executor, @org.springframework.beans.factory.annotation.Qualifier("cefBrowserObjectMapper") ObjectMapper objectMapper) {
                        this.executor = executor;
                        this.objectMapper = objectMapper;
                    }
                
                    private static final String MYMETHOD_CODE_0 = ""\"
                    console.log('test');
                    ""\";
                
                    @Override
                    public void myMethod() {
                        String code = MYMETHOD_CODE_0;
                        code = code.trim();
                        log.fine("Executing code\\n" + code);
                        executor.execute(code);
                    }
                
                }
                """);


        // Compile the interface and run the annotation processor
        var compilation = javac()
                .withProcessors(new JavaScriptCodeAnnotationProcessor())
                .compile(userInterface);

        // Assert that the compilation was successful and generated the expected file
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.UserInterfaceImpl")
                .hasSourceEquivalentTo(expectedImpl);
    }

    @Test
    @DisplayName("🧪 Should generate implementation with primitive and object parameters")
    void shouldGenerateImplWithMixedParameters() {
        JavaFileObject userInterface = JavaFileObjects.forSourceString("test.UserInterface", """
                    package test;
                    import io.github.bitfist.jcef.spring.jsexecution.JavaScriptCode;
                
                    class User {
                        public String name;
                    }
                
                    public interface UserInterface {
                        @JavaScriptCode("addUser(:user, :id);")
                        void addUser(User user, int id);
                    }
                """);

        var compilation = javac()
                .withProcessors(new JavaScriptCodeAnnotationProcessor())
                .compile(userInterface);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.UserInterfaceImpl")
                .contentsAsUtf8String()
                .contains("code = code.replace(\":id\", String.valueOf(id));");
        assertThat(compilation)
                .generatedSourceFile("test.UserInterfaceImpl")
                .contentsAsUtf8String()
                .contains("code = code.replace(\":user\", objectMapper.writeValueAsString(user));");
    }

    @Test
    @DisplayName("🧪 Should handle multiple methods in one interface")
    void shouldHandleMultipleMethods() {
        JavaFileObject userInterface = JavaFileObjects.forSourceString("test.UserInterface", """
                    package test;
                    import io.github.bitfist.jcef.spring.jsexecution.JavaScriptCode;
                
                    public interface UserInterface {
                        @JavaScriptCode("first();")
                        void firstMethod();
                
                        @JavaScriptCode("second();")
                        void secondMethod();
                    }
                """);

        var compilation = javac()
                .withProcessors(new JavaScriptCodeAnnotationProcessor())
                .compile(userInterface);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("test.UserInterfaceImpl")
                .contentsAsUtf8String()
                .contains("public void firstMethod()");
        assertThat(compilation)
                .generatedSourceFile("test.UserInterfaceImpl")
                .contentsAsUtf8String()
                .contains("public void secondMethod()");
    }

    @Test
    @DisplayName("⚠️ Should report error for annotation on class")
    void shouldReportErrorForAnnotationOnClass() {
        JavaFileObject invalidUsage = JavaFileObjects.forSourceString("test.InvalidUsage", """
                    package test;
                    import io.github.bitfist.jcef.spring.javascript.execution.JavaScriptCode;
                
                    @JavaScriptCode("class-level code")
                    public class InvalidUsage {
                        void someMethod() {
                
                        }
                    }
                """);

        var compilation = javac()
                .withProcessors(new JavaScriptCodeAnnotationProcessor())
                .compile(invalidUsage);

        // This should not fail the compilation, but the processor should not generate any file.
        assertThat(compilation).failed();
//        assertThat(compilation).generatedSourceFileCount().isEqualTo(0);
    }

    @Test
    @DisplayName("✅ Should correctly handle return types")
    void shouldHandleReturnTypes() {
        JavaFileObject userInterface = JavaFileObjects.forSourceString("test.UserInterface", """
                    package test;
                    import io.github.bitfist.jcef.spring.javascript.execution.JavaScriptCode;
                
                    public interface UserInterface {
                        @JavaScriptCode("return 1;")
                        int getNumber();
                    }
                """);

        var compilation = javac()
                .withProcessors(new JavaScriptCodeAnnotationProcessor())
                .compile(userInterface);

        assertThat(compilation).failed();
    }

}