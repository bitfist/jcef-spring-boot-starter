package io.github.bitfist.jcef.spring.query.internal.processor;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 🔧 Writes the class wrapper for generated TypeScript code.
 */
final class ClassRenderer {

    void start(BufferedWriter writer, String name) throws IOException {
        writer.write("export class " + name + " {\n  constructor() {}\n\n");
    }
    void end(BufferedWriter w) throws IOException { w.write("}\n"); }
}
