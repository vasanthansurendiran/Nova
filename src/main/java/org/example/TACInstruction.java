package org.example; // Make sure this matches your package name!

public class TACInstruction {
    public String result;
    public String arg1;
    public String operator;
    public String arg2;

    public TACInstruction(String result, String arg1, String operator, String arg2) {
        this.result = result;
        this.arg1 = arg1;
        this.operator = operator;
        this.arg2 = arg2;
    }

    // This method handles printing the instruction formatted correctly
    @Override
    public String toString() {
        if (operator.equals("=")) {
            // Simple assignment: x = 5
            return result + " = " + arg1;
        } else if (operator.equals("print")) {
            // Print statement: print x
            return "print " + arg1;
        } else if (operator.equals("label")) {
            // Label for loops/ifs: L1:
            return result + ":";
        } else if (operator.equals("goto") || operator.equals("ifFalseGOTO")) {
            // Jumps: goto L1
            return operator + " " + result;
        } else {
            // Standard math/logic: t1 = a + b
            return result + " = " + arg1 + " " + operator + " " + arg2;
        }
    }
}