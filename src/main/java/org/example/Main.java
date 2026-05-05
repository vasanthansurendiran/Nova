package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.gui.Trees; // Required for the GUI Tree Popup
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {

        // 1. Read from the source file
        String fileName = "test_program.nova";
        System.out.println("[System] Compiling file: " + fileName);
        CharStream input = CharStreams.fromFileName(fileName);

        // 2. The Frontend Pipeline
        NovaLexer lexer = new NovaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NovaParser parser = new NovaParser(tokens);

        // 3. Parse the tree
        ParseTree tree = parser.program();

        // 3.5 Print the Parse Tree to the console (LISP-style text format)
        //System.out.println("\n--- PARSE TREE (AST) ---");
        //System.out.println(tree.toStringTree(parser));
        //System.out.println("------------------------");

        // 3.6 THE FLEX: Launch the visual tree GUI for your Live Demo
        // Note: Execution pauses until you close the popup window!
        //Trees.inspect(tree, parser);

        // 4. Generate the TAC and Symbol Table
        TACGeneratorVisitor visitor = new TACGeneratorVisitor();
        visitor.visit(tree);

        // 5. Print the Symbol Table (Rubric Requirement)
        //System.out.println("\n--- SYMBOL TABLE ---");
        //for (Map.Entry<String, String> entry : visitor.symbolMap.entrySet()) {
        //    System.out.println("Name: " + entry.getKey() + " | Type: " + entry.getValue());
        //}
        System.out.println("--------------------");

        // 6. Print the TAC (Rubric Requirement)
        //System.out.println("\n--- NOVA COMPILER: TAC OUTPUT ---");
        //for (TACInstruction instr : visitor.tacList) {
        //    System.out.println(instr.toString());
        //}
        //System.out.println("---------------------------------\n");

        // 7. The Backend: Translate TAC to C Code and save it
        String cCode = CGenerator.generate(visitor.tacList);
        Files.writeString(Paths.get("output.c"), cCode);
        System.out.println("[System] Generated output.c successfully.");

        // 8. Execute GCC to build and run the native binary
        System.out.println("[System] Compiling to native binary via GCC...");
        Process compileProcess = new ProcessBuilder("gcc", "output.c", "-o", "nova_program").inheritIO().start();
        int compileResult = compileProcess.waitFor();

        if (compileResult == 0) {
            System.out.println("\n=== NATIVE EXECUTION OUTPUT ===");
            Process runProcess = new ProcessBuilder("./nova_program").inheritIO().start();
            runProcess.waitFor();
            System.out.println("===============================");
        } else {
            System.out.println("GCC Compilation Failed. Check your C code generation.");
        }
    }
}