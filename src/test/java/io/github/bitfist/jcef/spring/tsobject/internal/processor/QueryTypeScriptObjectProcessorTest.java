package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static io.github.bitfist.jcef.spring.tsobject.internal.processor.TypeScriptObjectProcessor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueryTypeScriptObjectProcessorTest {

    @TempDir
    Path temporaryOutputDirectory;

    private final JavaFileObject typeScriptConfigurationAnnotation = JavaFileObjects.forSourceLines(
            "io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration",
            "package io.github.bitfist.jcef.spring.tsobject;",
            "import java.lang.annotation.*;",
            "@Target(ElementType.TYPE)",
            "@Retention(RetentionPolicy.SOURCE)",
            "public @interface TypeScriptConfiguration {",
            "    String path() default \"jcef\";",
            "}"
    );

    private final JavaFileObject typeScriptObjectAnnotation = JavaFileObjects.forSourceLines(
            "io.github.bitfist.jcef.spring.tsobject.TypeScriptObject",
            "package io.github.bitfist.jcef.spring.tsobject;",
            "import java.lang.annotation.*;",
            "@Target(ElementType.TYPE)",
            "@Retention(RetentionPolicy.SOURCE)",
            "public @interface TypeScriptObject {}"
    );

    /**
     * Helper method to run the compilation with the annotation processor.
     * It combines the necessary annotation definitions with the provided test sources.
     */
    private Compilation compile(JavaFileObject... sources) {
        var outputPathOption = "-A" + JCEF_OUTPUT_PATH_OPTION + "=" + temporaryOutputDirectory.toString();
        var serviceTypeOption = "-A" + JCEF_SERVICE_TYPE_OPTION + "=" + ServiceType.QUERY;

        // Combine the mandatory annotations and the test-specific sources into one list.
        var allSources = new ArrayList<JavaFileObject>();
        allSources.add(typeScriptConfigurationAnnotation);
        allSources.add(typeScriptObjectAnnotation);
        allSources.addAll(List.of(sources));

        return javac()
                .withProcessors(new TypeScriptObjectProcessor())
                .withOptions(outputPathOption, serviceTypeOption)
                .compile(allSources); // Pass the combined list of sources
    }

    @Test
    void init_missingOutputPath_logsMessage() {
        Messager messager = mock(Messager.class);
        ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class);
        when(processingEnvironment.getMessager()).thenReturn(messager);

        new TypeScriptObjectProcessor().init(processingEnvironment);

        verify(messager).printError( "Required option " + JCEF_OUTPUT_PATH_OPTION + " is not set.");
    }

    @Test
    void init_missingServiceType_logsMessage() {
        Messager messager = mock(Messager.class);
        ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class);
        when(processingEnvironment.getMessager()).thenReturn(messager);
        when(processingEnvironment.getOptions()).thenReturn(Map.of(JCEF_OUTPUT_PATH_OPTION, "output"));

        new TypeScriptObjectProcessor().init(processingEnvironment);

        verify(messager).printError("No service type defined. Must be one of " + JCEF_SERVICE_TYPE_WEB + " or " + JCEF_SERVICE_TYPE_QUERY);
    }

    @Test
    void init_invalidServiceType_logsMessage() {
        Messager messager = mock(Messager.class);
        ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class);
        when(processingEnvironment.getMessager()).thenReturn(messager);
        when(processingEnvironment.getOptions()).thenReturn(
                Map.of(
                        JCEF_OUTPUT_PATH_OPTION, "output",
                        JCEF_SERVICE_TYPE_OPTION, "invalid"
                )
        );

        new TypeScriptObjectProcessor().init(processingEnvironment);

        verify(messager).printError("Invalid service type: invalid. Must be one of " + JCEF_SERVICE_TYPE_WEB + " or " + JCEF_SERVICE_TYPE_QUERY);
    }

    @Test
    @DisplayName("Processor should succeed with a simple service and generate correct TS")
    void process_simpleService_succeedsAndGeneratesCorrectly() throws IOException {
        JavaFileObject userService = JavaFileObjects.forSourceLines("com.example.UserService",
                "package com.example;",
                "import io.github.bitfist.jcef.spring.tsobject.*;",
                "@TypeScriptObject",
                "@TypeScriptConfiguration(path = \"api/services\")",
                "public class UserService {",
                "    public String getUser(int id) { return \"User \" + id; }",
                "    public void updateUser(String name) { }",
                "}"
        );

        var compilation = compile(userService);

        assertThat(compilation).succeeded();

        var generatedFile = temporaryOutputDirectory.resolve("api/services/UserService.ts");
        assertThat(Files.exists(generatedFile)).isTrue();

        String content = Files.readString(generatedFile);
        var expectedContent = """
                /** AUTO-GENERATED by JCEF TypeScriptObjectProcessor – DO NOT EDIT **/
                import { CefQueryService } from '../../jcef/CefQueryService';
                
                export class UserService {
                    public static getUser(id: number): Promise<string> {
                        return CefQueryService.request('com.example.UserService', 'getUser', {id}, 'string');
                    }
                
                    public static updateUser(name: string): void {
                        return CefQueryService.request('com.example.UserService', 'updateUser', {name}, 'object');
                    }
                
                }
                """;

        assertThat(content).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("Processor should generate correct TS based on visibility")
    void process_visibility_succeedsAndGeneratesCorrectly() throws IOException {
        JavaFileObject userService = JavaFileObjects.forSourceLines("com.example.UserService",
                "package com.example;",
                "import io.github.bitfist.jcef.spring.tsobject.*;",
                "@TypeScriptObject",
                "@TypeScriptConfiguration(path = \"api/services\")",
                "public class UserService {",
                "    public String getUser(int id) { return \"User \" + id; }",
                "    String getUser2(int id) { return \"User \" + id; }",
                "    private String getUser3(int id) { return \"User \" + id; }",
                "}"
        );

        var compilation = compile(userService);

        assertThat(compilation).succeeded();

        var generatedFile = temporaryOutputDirectory.resolve("api/services/UserService.ts");
        assertThat(Files.exists(generatedFile)).isTrue();

        String content = Files.readString(generatedFile);
        var expectedContent = """
                /** AUTO-GENERATED by JCEF TypeScriptObjectProcessor – DO NOT EDIT **/
                import { CefQueryService } from '../../jcef/CefQueryService';
                
                export class UserService {
                    public static getUser(id: number): Promise<string> {
                        return CefQueryService.request('com.example.UserService', 'getUser', {id}, 'string');
                    }
                
                    public static getUser2(id: number): Promise<string> {
                        return CefQueryService.request('com.example.UserService', 'getUser2', {id}, 'string');
                    }
                
                }
                """;

        assertThat(content).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("Processor should generate dependent type definitions")
    void process_serviceWithComplexTypes_generatesDependencies() throws IOException {
        JavaFileObject userDto = JavaFileObjects.forSourceLines("com.example.model.UserDto",
                "package com.example.model;",
                "import io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration;",
                "@TypeScriptConfiguration(path = \"api/model\")",
                "public class UserDto {",
                "    private int id;",
                "    private String username;",
                "}"
        );

        JavaFileObject dataService = JavaFileObjects.forSourceLines("com.example.DataService",
                "package com.example;",
                "import com.example.model.UserDto;",
                "import io.github.bitfist.jcef.spring.tsobject.*;",
                "import java.util.List;",
                "@TypeScriptObject",
                "@TypeScriptConfiguration(path = \"api/services\")",
                "public class DataService {",
                "    public UserDto findUser(int id) { return null; }",
                "    public List<UserDto> findAllUsers() { return null; }",
                "}"
        );

        var compilation = compile(userDto, dataService);

        assertThat(compilation).succeeded();

        // Assert the main service file
        var serviceFile = temporaryOutputDirectory.resolve("api/services/DataService.ts");
        assertThat(Files.exists(serviceFile)).isTrue();
        String serviceContent = Files.readString(serviceFile);
        assertThat(serviceContent).contains("import type { UserDto } from '../model/UserDto';");

        // Assert the dependency file
        var dependencyFile = temporaryOutputDirectory.resolve("api/model/UserDto.ts");
        assertThat(Files.exists(dependencyFile)).isTrue();
        String dependencyContent = Files.readString(dependencyFile);
        var expectedDependencyContent = """
                export interface UserDto {
                    id: number;
                    username: string;
                }
                """;
        assertThat(dependencyContent).isEqualTo(expectedDependencyContent);
    }

    @Test
    @DisplayName("Processor should handle dependencies without explicit configuration by using their package name")
    void process_dependencyWithoutConfig_usesPackageAsPath() throws IOException {
        JavaFileObject product = JavaFileObjects.forSourceLines("com.store.inventory.Product",
                "package com.store.inventory;",
                // No @TypeScriptConfiguration on this dependency
                "public class Product {",
                "    String sku;",
                "}"
        );
        JavaFileObject storeService = JavaFileObjects.forSourceLines("com.store.api.StoreService",
                "package com.store.api;",
                "import com.store.inventory.Product;",
                "import io.github.bitfist.jcef.spring.tsobject.*;",
                "@TypeScriptObject",
                "@TypeScriptConfiguration(path = \"store-front/api\")",
                "public class StoreService {",
                "    public Product getProduct(String id) { return null; }",
                "}"
        );

        var compilation = compile(product, storeService);

        assertThat(compilation).succeeded();

        // Assert the dependency was generated in a path derived from its Java package
        var dependencyFile = temporaryOutputDirectory.resolve("com/store/inventory/Product.ts");
        assertThat(Files.exists(dependencyFile)).isTrue();

        String dependencyContent = Files.readString(dependencyFile);
        var expectedDependencyContent = """
                export interface Product {
                    sku: string;
                }
                """;
        assertThat(dependencyContent).isEqualTo(expectedDependencyContent);

        // Assert the main service has the correct relative import path to the dependency
        var serviceFile = temporaryOutputDirectory.resolve("store-front/api/StoreService.ts");
        assertThat(Files.exists(serviceFile)).isTrue();
        String serviceContent = Files.readString(serviceFile);
        assertThat(serviceContent).contains("""
                import { CefQueryService } from '../../jcef/CefQueryService';
                import type { Product } from '../../com/store/inventory/Product';
                
                export class StoreService {
                    public static getProduct(id: string): Promise<Product> {
                        return CefQueryService.request('com.store.api.StoreService', 'getProduct', {id}, 'object');
                    }
                
                }
                """);
    }

    @Test
    @DisplayName("📄➡️ ✅ When class has methods, support files should be copied")
    void when_classHasMethods_then_supportFilesAreCopied() throws IOException, URISyntaxException {
        // --- ARRANGE ---
        // Create a source file for a class that has a method and is annotated with @TypeScriptObject.
        JavaFileObject sourceFileWithMethod = JavaFileObjects.forSourceString("test.MyApiWithMethod", """
                    package test;
                    import io.github.bitfist.jcef.spring.tsobject.TypeScriptObject;
                    import io.github.bitfist.jcef.spring.tsobject.TypeScriptConfiguration;
                
                    @TypeScriptObject
                    @TypeScriptConfiguration
                    public class MyApiWithMethod {
                        public String greet(String name) {
                            return "Hello, " + name;
                        }
                    }
                """);

        // --- ACT ---
        // Run the annotation processor
        var compilation = Compiler.javac()
                .withProcessors(new TypeScriptObjectProcessor())
                .withOptions(
                        "-A" + JCEF_OUTPUT_PATH_OPTION + "=" + temporaryOutputDirectory.toAbsolutePath(),
                        "-A" + JCEF_SERVICE_TYPE_OPTION + "=" + ServiceType.QUERY
                )
                .compile(sourceFileWithMethod);


        // --- ASSERT ---
        // 1. Assert that the compilation was successful.
        assertThat(compilation).succeeded();

        // 2. Define the expected paths for the copied files.
        var cefQueryServicePath = temporaryOutputDirectory.resolve("jcef/CefQueryService.ts");
        var cefDtsPath = temporaryOutputDirectory.resolve("types/cef.d.ts");
        var responseTypePath = temporaryOutputDirectory.resolve("jcef/ResponseType.ts");
        var responseValueConverterPath = temporaryOutputDirectory.resolve("jcef/ResponseValueConverter.ts");

        // 3. Assert that the files were created.
        assertTrue(Files.exists(cefQueryServicePath), "CefQueryService.ts should have been created.");
        assertTrue(Files.exists(cefDtsPath), "cef.d.ts should have been created.");
        assertTrue(Files.exists(responseTypePath), "ResponseType.ts should have been created.");
        assertTrue(Files.exists(responseValueConverterPath), "ResponseValueConverter.ts should have been created.");

        // 4. Load the original content from test resources.
        var expectedCefQueryServiceContent = getResourceContent("generator/templates/CefQueryService.ts");
        var expectedCefDtsContent = getResourceContent("generator/templates/cef.d.ts");
        var expectedResponseTypeContent = getResourceContent("generator/templates/ResponseType.ts");
        var expectedResponseValueConverterContent = getResourceContent("generator/templates/ResponseValueConverter.ts");

        // 5. Read the content of the newly created files.
        String actualCefQueryServiceContent = Files.readString(cefQueryServicePath);
        String actualCefDtsContent = Files.readString(cefDtsPath);
        String actualResponseTypeContent = Files.readString(responseTypePath);
        String actualResponseValueConverterContent = Files.readString(responseValueConverterPath);

        // 6. Assert that the content of the copied files matches the original resources.
        assertEquals(expectedCefQueryServiceContent, actualCefQueryServiceContent, "Content of CefQueryService.ts does not match.");
        assertEquals(expectedCefDtsContent, actualCefDtsContent, "Content of cef.d.ts does not match.");
        assertEquals(expectedResponseTypeContent, actualResponseTypeContent, "Content of ResponseType.ts does not match.");
        assertEquals(expectedResponseValueConverterContent, actualResponseValueConverterContent, "Content of ResponseValueConverter.ts does not match.");
    }

    @Test
    @DisplayName("📄➡️ ❌ When class has no methods, support files should not be copied")
    void when_classHasNoMethods_then_supportFilesAreNotCopied() {
        // --- ARRANGE ---
        // Create a source file for a class with no methods, only fields.
        JavaFileObject sourceFileWithoutMethod = JavaFileObjects.forSourceString("test.MyApiWithoutMethod", """
                    package test;
                
                    public class MyApiWithoutMethod {
                        public final String name = "test";
                    }
                """);

        // --- ACT ---
        // Run the annotation processor
        var compilation = Compiler.javac()
                .withProcessors(new TypeScriptObjectProcessor())
                .withOptions(
                        "-A" + JCEF_OUTPUT_PATH_OPTION + "=" + temporaryOutputDirectory.toAbsolutePath(),
                        "-A" + JCEF_SERVICE_TYPE_OPTION + "=" + ServiceType.QUERY
                )
                .compile(sourceFileWithoutMethod);

        // --- ASSERT ---
        // 1. Assert that the compilation was successful.
        assertThat(compilation).succeeded();

        // 2. Define the expected paths for the copied files.
        var cefQueryServicePath = temporaryOutputDirectory.resolve("jcef/CefQueryService.ts");
        var cefDtsPath = temporaryOutputDirectory.resolve("types/cef.d.ts");

        // 3. Assert that the files do NOT exist.
        assertFalse(Files.exists(cefQueryServicePath), "CefQueryService.ts should NOT have been created.");
        assertFalse(Files.exists(cefDtsPath), "cef.d.ts should NOT have been created.");
    }

    /**
     * Helper method to read the content of a resource file from the classpath.
     *
     * @param resourceName The path to the resource.
     * @return The content of the file as a String.
     * @throws IOException        if the resource cannot be read.
     * @throws URISyntaxException if the resource path is invalid.
     */
    private String getResourceContent(String resourceName) throws IOException, URISyntaxException {
        return Files.readString(
                Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource(resourceName)).toURI()),
                StandardCharsets.UTF_8
        );
    }
}