[![Gradle build](https://github.com/bitfist/jcef-spring-boot-starter/actions/workflows/test.yml/badge.svg)](https://github.com/bitfist/os-conditions-spring-boot-starter/actions/workflows/test.yml)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

🚀 **JCEF Spring Boot Integration**

*A Spring Boot starter for embedding a Chromium browser (via JCEF) in desktop applications.*

---

## ⚙️ Quick Start

### 1. 📦 Add Dependency

```xml
<dependency>
  <groupId>io.github.bitfist</groupId>
  <artifactId>jcef-spring-boot-starter</artifactId>
  <version>REPLACE_WITH_VERSION</version>
</dependency>
```

### 2. 🛠 Configure Application Properties

```yaml
jcef:
  application-name: MyApp            # (required)
  splash-screen-classpath-resource: splash.png
  distribution-classpath: ui         # defaults to "ui"
  development-options:
    debug-port: 9222                # optional: remote debugging port
    show-developer-tools: true      # optional: opens DevTools on load end
```

### 3. 🚀 Bootstrap the Application

```java
@SpringBootApplication
public class MyApp {
    public static void main(String[] args) {
        JcefApplication.run(MyApp.class, args);
    }
}
```

Customize the SpringApplicationBuilder:

```java
JcefApplication.run(MyApp.class, args, builder -> {
    builder.profiles("desktop");
});
```

---

## 📚 Public API

All packages under `io.github.bitfist.jcef.spring` are organized into modules. Every direct child of a module is part of the public API.

### 🏗 Core Auto-Configuration

* **`JcefAutoConfiguration`**

    * Enables all JCEF Spring Boot beans.
    * Activates binding of `JcefApplicationProperties`.

---

### 🖥️ Application Module (`io.github.bitfist.jcef.spring.application`)

#### `JcefApplication`

* Utility class to launch a desktop Spring Boot app with non-headless mode.
* Methods:

    * `run(Class<T> clazz, String[] args)` – basic startup with native look‑and‑feel.
    * `run(Class<T> clazz, String[] args, Consumer<SpringApplicationBuilder> customizer)` – tweak builder before run.

#### `JcefApplicationProperties`

* Maps properties prefixed `jcef`.

* Required: `application-name` (must not be blank).

* Optional: `splash-screen-classpath-resource`, `distribution-classpath` (default `ui`).

* `DevelopmentOptions` record holds `debugPort` and `showDeveloperTools`.

* Runtime path methods (platform‑aware):

    * `getInstallationPath()` – base app data dir.
    * `getJcefInstallationPath()` – JCEF binaries folder.
    * `getUiInstallationPath()` – UI resource folder.

---

### 🌐 Browser Module (`io.github.bitfist.jcef.spring.browser`)

#### `AbstractSplashScreen`

* Abstract `JFrame` implementing `IProgressHandler`.
* Extend to provide custom installation progress UI.

#### Customizer Interfaces (functional)

* **`CefApplicationCustomizer`** – configure `CefAppBuilder` before build.
* **`CefClientCustomizer`** – customize `CefClient` (routers, handlers).
* **`CefBrowserCustomizer`** – post‑creation hook on `CefBrowser`.
* **`CefBrowserFrameCustomizer`** – adjust the hosting `JFrame` (size, events).

#### Internal Auto-Configuration

* **`BrowserConfiguration`**, **`CefConfiguration`** provide default beans:

    * Default splash screen, query router, JSON mapper.
    * `BrowserStarter` launches the browser on `ApplicationReadyEvent`.
    * `UIInstaller` syncs UI files from classpath to install dir.

---

### 💻 Code Execution Module (`io.github.bitfist.jcef.spring.javascript.execution`)

#### `@Code` Annotation

* Annotate interface methods with JavaScript snippets to execute in browser.
* Syntax: `@Code("console.log('Hi :param');")`.

#### `JavaScriptExecutor` Interface

* Defines `execute(String code)`.

#### `DefaultJavaScriptExecutor`

* Default bean executing code via `cefBrowser.executeJavaScript(code, null, 0)`.

---

### 🔧 Code Generation Module (`io.github.bitfist.jcef.spring.javascript.generation`)

#### `@TypeScript` Annotation

* Apply to classes (`@CefQueryHandler` beans) to trigger TS stub generation.
* Attribute: `packageName()` for output directory.

#### Annotation Processor Components

* **`ModelScanner`** discovers handler classes and DTOs.
* **`HeaderRenderer`, `InterfaceRenderer`, `MethodRenderer`, `ClassRenderer`** compose TS files.
* **`TsFileWriter`** writes `.ts` to configured output path.
* **`ResourceCopier`** adds helper assets (`CefQueryService.ts`, `cef.d.ts`).

---

### 🐞 Debug Module (`io.github.bitfist.jcef.spring.debug`)

* **Developer Tools**: if `jcef.development-options.show-developer-tools=true`, opens DevTools on load finish.
* **Remote Debug**: if `jcef.development-options.debug-port` is set, configures `--remote-debugging-port` and allows origins.

---

### 🔍 Query Module (`io.github.bitfist.jcef.spring.query`)

#### `@CefQueryHandler`

* Annotate classes or methods to handle `window.cefQuery({route, payload})` calls.
* Supports path variables (`/api/{id}`) – compiled with `-parameters`.

#### `CefQueryJson`

* POJO holding `route` and `payload` data.

#### `CefQueryEvent`

* Spring event published on each incoming query.

#### `CefQueryException`

* Throw to respond with a specific error code and message.

#### `DefaultCefQueryRouter`

* Scans for handlers, builds regex routes, extracts path params and payload.
* Deserializes payloads, invokes methods async, serializes responses.

---

## 🛠 Annotation Processors

### 🧩 CodeAnnotationProcessor

* Processes `@Code` methods at compile time.
* Generates `<InterfaceName>Impl` classes:

    * Embeds JS snippet as `String` constant.
    * Replaces placeholders with method args (primitives or JSON via Jackson).
    * Delegates to `JavaScriptExecutor`.

### ✍️ TypeScriptGeneratorProcessor

* Activated if `jcef.output.type=typescript` and `jcef.output.path` is set.
* Copies helper TS assets once per compilation.
* Scans `@CefQueryHandler` beans and generates `.ts` stubs:

    * Renders DTO interfaces for complex types.
    * Emits static methods calling `CefQueryService.request<ReturnType>(route, body?, responseType)`.

**Example Maven Configuration**

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>io.github.bitfist</groupId>
        <artifactId>jcef-spring-boot-starter</artifactId>
        <version>${project.version}</version>
      </path>
    </annotationProcessorPaths>
    <compilerArgs>
      <arg>-Ajcef.output.path=${project.build.directory}/generated-ts</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

---

## 📝 Example Usage

```java
@CefQueryHandler("/api")
public class MyHandler {

    @CefQueryHandler("/echo/{text}")
    public String echo(String text) {
        return text;
    }
}

public interface MyJsApi {
    @Code("console.log('Hello: :msg');")
    void log(String msg);
}
```
