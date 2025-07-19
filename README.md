![GitHub Release Plugin](https://img.shields.io/static/v1?label=GitHub&message=Release&color=blue&logo=github)
![License](https://img.shields.io/badge/License-Apache%20License%20Version%202.0-blue)
[![Gradle build](https://github.com/bitfist/jcef-spring-boot-starter/actions/workflows/test.yml/badge.svg)](https://github.com/bitfist/jcef-spring-boot-starter/actions/workflows/test.yml)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

# JCEF Spring Boot Integration

Start with examples on how to use the API, especially the annotations and the customizers.

---

Table of contents
=================
* [Examples](#-examples)
  * [Basic Application Bootstrap](#-basic-application-bootstrap)
  * [Executing JavaScript via Annotation](#-executing-javascript-via-annotation)
  * [Marking Beans for TypeScript Generation](#-marking-beans-for-typescript-generation)
* [Public Modules](#-public-modules)
  * [Application](#-application)
  * [Browser](#-browser)
  * [Debug](#-debug)
  * [JS Execution](#-javascript-execution)
  * [TypeScript Object](#-typescript-object)
* [Getting started](#-getting-started)
* [Configuration](#-configuration)
---

## 🔍 Examples

### 🚀 Basic Application Bootstrap

```java
public class MyApp {
    public static void main(String[] args) {
        JcefApplication.run(MyApp.class, args);
    }
}
```

### ✍️ Executing JavaScript via Annotation

```java
public interface MyScripts {
    @JavaScriptCode("alert('Hello, :name');")
    void greet(String name);
}
```

### 🎨 Marking Beans for TypeScript Generation

```java
@TypeScriptConfiguration(path = "app")
@TypeScriptObject
public class MyCallback {
    public String echo(String message) {
        return message;
    }
}
```

---

## 📦 Public Modules

All packages under `src/main/java/io/github/bitfist/jcef/spring/` are public API modules.

### ⚙️ Application

- **JcefApplication**
  - `run(Class<T>, String[])`
  - `run(Class<T>, String[], Consumer<SpringApplicationBuilder>)`
- **JcefApplicationProperties**  
  Binds `jcef.*` properties, validates configuration, and provides:
  - Platform-specific installation paths (`getInstallationPath()`)
  - JCEF bundle path (`getJcefInstallationPath()`)
  - UI resources path (`getUiInstallationPath()`)

### 🖥 Browser

- **AbstractSplashScreen** 📦 Base frame for installation progress (implements `IProgressHandler`).
- **Browser** 🖥 Interface to execute JavaScript: `executeJavaScript(String code)`.
- **CefApplicationCustomizer** 🔧 Customize the `CefAppBuilder` before initialization.
- **CefClientCustomizer** 🔧 Customize the `CefClient` (e.g., add message handlers).
- **CefBrowserCustomizer** 🔧 Customize the `CefBrowser` instance.
- **CefBrowserFrameCustomizer** 🔧 Customize the Swing `JFrame` hosting the browser.
- **CefMessageHandler** 📣 Handle incoming CEF queries: `handleQuery(String)`.
- **CefMessageException** 💥 Exception to signal query errors with code and message.

### 🐞 Debug

Autoconfiguration for debugging features:

- `developerToolsCustomizer()` 🐞 Opens devtools on page load if `jcef.development-options.show-developer-tools=true`.
- `debugPortCustomizer()` 🐞 Sets remote debugging port via `jcef.development-options.debug-port`.

### ✍️ JavaScript Execution

- **@JavaScriptCode** ✍️ Annotate methods with JS snippets to generate execution code.
- **JavaScriptExecutor** Interface; default implementation uses `Browser.executeJavaScript`.

### 🎨 TypeScript Object

- **@TypeScriptConfiguration** 🎨 Configure TypeScript output path for generated files.
- **@TypeScriptObject** 🎨 Mark Spring beans as callbacks accessible from JavaScript; triggers TS code generation.

---

## 🚀 Getting Started

See the [jcef-gradle-plugin](https://github.com/bitfist/jcef-gradle-plugin) for details on how to get started.

---

## ⚙️ Configuration

Example `application.yml`:

```yaml
jcef:
  application-name: my-app
  splash-screen-classpath-resource: splash.png
  distribution-classpath: ui
  development:
    show-developer-tools: true
    debug-port: 9222
```
