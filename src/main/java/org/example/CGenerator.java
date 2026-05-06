package org.example;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CGenerator {

    public static String generate(List<TACInstruction> tacList, List<TACGeneratorVisitor.Symbol> symbolTracker) {
        StringBuilder cCode = new StringBuilder();
        cCode.append("#include <stdio.h>\n\n");
        cCode.append("int main() {\n");

        Set<String> declaredVars = new HashSet<>();

        for (TACInstruction instr : tacList) {
            cCode.append("    ");

            if (instr.operator.equals("print")) {
                String formatStr = "%d\\n"; // Default to integer

                // Trust the Symbol Table implicitly. No more guessing.
                for (TACGeneratorVisitor.Symbol s : symbolTracker) {
                    if (s.name.equals(instr.arg1)) {
                        if (s.type.equals("float") || s.type.equals("double")) {
                            formatStr = "%f\\n";
                        }
                        break;
                    }
                }

                cCode.append("printf(\"").append(formatStr).append("\", ").append(instr.arg1).append(");\n");
            }
            else if (instr.operator.equals("alloc")) {
                String baseType = instr.arg2.replace("[]", "");
                cCode.append(baseType).append(" ").append(instr.result).append("[").append(instr.arg1).append("];\n");
                declaredVars.add(instr.result);
            }
            else if (instr.operator.equals("label")) {
                cCode.append(instr.result).append(":\n");
            }
            else if (instr.operator.equals("goto")) {
                cCode.append("goto ").append(instr.result).append(";\n");
            }
            else if (instr.operator.equals("ifFalseGOTO")) {
                cCode.append("if (!(").append(instr.arg1).append(")) goto ").append(instr.result).append(";\n");
            }
            else {
                String baseVar = instr.result.replaceAll("\\[.*?\\]", "");
                if (!declaredVars.contains(baseVar)) {

                    String type = "int"; // Strict fallback
                    boolean typeFound = false;

                    // Look up the exact type in the Symbol Table
                    for (TACGeneratorVisitor.Symbol s : symbolTracker) {
                        if (s.name.equals(baseVar)) {
                            type = s.type;
                            typeFound = true;
                            break;
                        }
                    }

                    // Catch frontend bugs: Warn if the variable isn't registered
                    if (!typeFound) {
                        System.err.println("[COMPILER WARNING] Variable '" + baseVar + "' is missing from the Symbol Table! Defaulting to int.");
                    }

                    cCode.append(type).append(" ");
                    declaredVars.add(baseVar);
                }

                if (instr.operator.equals("=")) {
                    cCode.append(instr.result).append(" = ").append(instr.arg1).append(";\n");
                } else {
                    cCode.append(instr.result).append(" = ").append(instr.arg1)
                            .append(" ").append(instr.operator).append(" ").append(instr.arg2).append(";\n");
                }
            }
        }

        cCode.append("    return 0;\n");
        cCode.append("}\n");
        return cCode.toString();
    }
}