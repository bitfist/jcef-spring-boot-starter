package io.github.bitfist.jcef.spring.tsobject.internal.processor;

import com.google.common.collect.ImmutableMap;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.testing.compile.Compilation.Status.FAILURE;
import static com.google.testing.compile.Compilation.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("🧪 TypeScriptProcessor Tests")
class TypeScriptProcessorTest {

	// Minimal DTO classes to feed into the processor
	private static final JavaFileObject SIMPLE_DTO = JavaFileObjects.forSourceString("test.ExampleDto",
			// language=java
			"""
			package test;

			import io.github.bitfist.jcef.spring.tsobject.TypeScriptClass;

			@TypeScriptClass(path = "custom/path")
			class ExampleDto {
				public String name;
				public int count;
				public ExampleDto2 example;
			}
			""");

	private static final JavaFileObject SIMPLE_DTO2 = JavaFileObjects.forSourceString("test.ExampleDto2",
			// language=java
			"""
			package test;
			
			class ExampleDto2 {
				public String name;
				public TestEnum testEnum;
			
				public ExampleDto2(String name, TestEnum testEnum){
					this.name = name;
					this.testEnum = testEnum;
				}
			}
			""");

	private static final JavaFileObject SIMPLE_ENUM = JavaFileObjects.forSourceString("test.TestEnum",
			// language=java
			"""
			package test;
			
			enum TestEnum {
				Linz, Wels, Steyr
			}
			""");

	// Minimal Service class with a method
	private static final JavaFileObject SIMPLE_SERVICE = JavaFileObjects.forSourceString("test.ExampleService",
			// language=java
			"""
            package test;

            import io.github.bitfist.jcef.spring.tsobject.TypeScriptService;
            import java.util.List;
            import java.util.Map;

            @TypeScriptService
            class ExampleService {
                ExampleDto2 greet(String who, int age, boolean uber, List<String> parents, Map<String, Integer> aMap, TestEnum testEnum) {
                    return new ExampleDto2("Hello " + who, testEnum);
                }
            }
            """);

	@Nested
	@DisplayName("✅ Successful generation scenarios")
	class SuccessCases {

		@Test
		@DisplayName("📦 Generate DTO and Service TypeScript files with valid options")
		void generatesDtoAndService(@TempDir Path tmpDir) throws IOException {
			var outputPath = tmpDir.toString();

			var compilation = Compiler.javac()
					.withProcessors(new TypeScriptProcessor())
					.withOptions(ImmutableMap.of(
							"jcef.output.path", outputPath,
							"jcef.web.communication.enabled", "false"
					).entrySet().stream().map(e -> "-A" + e.getKey() + "=" + e.getValue()).toList())
					.compile(SIMPLE_DTO, SIMPLE_DTO2, SIMPLE_ENUM, SIMPLE_SERVICE);

			// Processor should succeed
			assertEquals(SUCCESS, compilation.status(), "Compilation should succeed with valid options");

			// Check that expected TypeScript files exist on disk
			var dtoTs = tmpDir.resolve("custom").resolve("path").resolve("ExampleDto.ts");
			var dto2Ts = tmpDir.resolve("test").resolve("ExampleDto2.ts");
			var enumTs = tmpDir.resolve("test").resolve("TestEnum.ts");
			var serviceTs = tmpDir.resolve("test").resolve("ExampleService.ts");

			assertTrue(Files.exists(dtoTs), "DTO TypeScript file should be generated: " + dtoTs);
			assertTrue(Files.exists(dto2Ts), "DTO TypeScript file should be generated: " + dto2Ts);
			assertTrue(Files.exists(enumTs), "Enum TypeScript file should be generated: " + enumTs);
			assertTrue(Files.exists(serviceTs), "Service TypeScript file should be generated: " + serviceTs);

			var dtoContent = Files.readString(dtoTs);
			var dto2Content = Files.readString(dto2Ts);
			var enumContent = Files.readString(enumTs);
			var serviceContent = Files.readString(serviceTs);

			// Basic content assertions
			assertTrue(dtoContent.contains("export interface ExampleDto"), "DTO .ts should declare interface");
			assertTrue(dtoContent.contains("name:"), "DTO should include 'name' field");
			assertTrue(dtoContent.contains("count:"), "DTO should include 'count' field");

			assertTrue(dto2Content.contains("export interface ExampleDto2"), "DTO .ts should declare interface");
			assertTrue(dto2Content.contains("name:"), "DTO should include 'name' field");

			assertTrue(enumContent.contains("export enum TestEnum"), "Enum .ts should declare enum");
			assertTrue(enumContent.contains("Linz = 'Linz'"), "Enum should include 'Linz' value");
			assertTrue(enumContent.contains("Wels = 'Wels'"), "Enum should include 'Wels' value");
			assertTrue(enumContent.contains("Steyr = 'Steyr'"), "Enum should include 'Steyr' value");

			assertTrue(serviceContent.contains("export class ExampleService"), "Service .ts should declare class");
			assertTrue(serviceContent.contains("static async greet(who: string, age: number, uber: boolean, parents: string[], aMap: { [key: string]: number }, testEnum: TestEnum): Promise<ExampleDto2>"), "Service method signature should appear");
			assertTrue(serviceContent.contains("CefCommunicationService.request"), "Service should use CefCommunicationService");

			// Optionally, if support files are expected to be copied, verify one exists
			var cefServiceFile = tmpDir.resolve("jcef").resolve("CefCommunicationService.ts");
			assertTrue(Files.exists(cefServiceFile), "Service file should be generated: " + cefServiceFile);
		}
	}

	@Nested
	@DisplayName("❌ Failure / validation scenarios")
	class FailureCases {

		@Test
		@DisplayName("⚠️ Fail when required output path option is missing")
		void failsWithoutOutputPath() {
			var compilation = Compiler.javac()
					.withProcessors(new TypeScriptProcessor())
					.compile(SIMPLE_DTO);

			// Processor should not crash, but according to logic optionsAreInvalid prints error and returns false.
			assertEquals(FAILURE, compilation.status(), "Compilation should fail when required option is missing");
		}

		@Test
		@DisplayName("🧩 DTO with missing fields still produces interface (resilience)")
		void dtoWithNoFieldsProducesEmptyInterface(@TempDir Path tmpDir) throws IOException {
			var outputPath = tmpDir.toString();
			JavaFileObject emptyDto = JavaFileObjects.forSourceString("test.EmptyDto",
                    // language=java
                    """
                    package test;

                    import io.github.bitfist.jcef.spring.tsobject.TypeScriptClass;

                    @TypeScriptClass
                    class EmptyDto {
                    }
                    """);

			var compilation = Compiler.javac()
					.withProcessors(new TypeScriptProcessor())
					.withOptions(("-Ajcef.output.path=" + outputPath))
					.compile(emptyDto);

			assertEquals(SUCCESS, compilation.status());

			var dtoTs = tmpDir.resolve("test").resolve("EmptyDto.ts");
			assertTrue(Files.exists(dtoTs), "TypeScript file for empty DTO should still be created");

			String content = Files.readString(dtoTs);
			assertTrue(content.contains("export interface EmptyDto"), "Interface declaration present even if no fields");
		}
	}

	// Helpers could be extracted for more reuse in a real test suite
}
