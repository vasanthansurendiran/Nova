package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.gui.Trees;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {

        String fileName = "test_program.nova";
        System.out.println("[System] Compiling file: " + fileName);
        CharStream input = CharStreams.fromFileName(fileName);

        NovaLexer lexer = new NovaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NovaParser parser = new NovaParser(tokens);

        ParseTree tree = parser.program();

        System.out.println("\n--- PARSE TREE (AST) ---");
        System.out.println(tree.toStringTree(parser));
        System.out.println("------------------------");

        Trees.inspect(tree, parser);

        TACGeneratorVisitor visitor = new TACGeneratorVisitor();
        visitor.visit(tree);

        System.out.println("\n--- SYMBOL TABLE ---");
        System.out.printf("%-15s %-15s %-15s\n", "NAME", "TYPE", "SCOPE_LEVEL");
        System.out.println("---------------------------------------------");
        for (TACGeneratorVisitor.Symbol sym : visitor.symbolTracker) {
            System.out.printf("%-15s %-15s %-15d\n", sym.name, sym.type, sym.scopeLevel);
        }
        System.out.println("---------------------------------------------\n");

        System.out.println("--- NOVA COMPILER: TAC OUTPUT ---");
        for (TACInstruction instr : visitor.tacList) {
            System.out.println(instr.toString());
        }
        System.out.println("---------------------------------\n");

        String cCode = CGenerator.generate(visitor.tacList, visitor.symbolTracker);
        Files.writeString(Paths.get("output.c"), cCode);
        System.out.println("[System] Generated output.c successfully.");

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