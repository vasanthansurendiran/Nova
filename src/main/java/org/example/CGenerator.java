package org.example;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CGenerator {

    public static String generate(List<TACInstruction> tacList) {
        StringBuilder cCode = new StringBuilder();

        // C Standard Boilerplate
        cCode.append("#include <stdio.h>\n\n");
        cCode.append("int main() {\n");

        // We use a Set to track which variables we have already declared
        // so we don't accidentally write "int x = 5; int x = 10;"
        Set<String> declaredVars = new HashSet<>();

        for (TACInstruction instr : tacList) {
            cCode.append("    "); // Indentation for readability

            // Handle Print statements
            if (instr.operator.equals("print")) {
                cCode.append("printf(\"%d\\n\", ").append(instr.arg1).append(");\n");
            }
            // Handle Math and Assignments
            else {
                // If we haven't seen this variable before, declare it as an integer
                if (!declaredVars.contains(instr.result)) {
                    cCode.append("int ");
                    declaredVars.add(instr.result);
                }

                if (instr.operator.equals("=")) {
                    // Simple assignment: a = 10;
                    cCode.append(instr.result).append(" = ").append(instr.arg1).append(";\n");
                } else {
                    // Math operation: t1 = a + b;
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