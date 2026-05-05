package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TACGeneratorVisitor extends NovaBaseVisitor<String> {

    // The Intermediate Representation list
    public List<TACInstruction> tacList = new ArrayList<>();
    // The Symbol Table (Stores Variable Name -> Data Type)
    public Map<String, String> symbolMap = new HashMap<>();

    private int tempCounter = 1;

    // Helper to generate unique temp variables (t1, t2, t3...)
    private String newTemp() {
        return "t" + (tempCounter++);
    }

    // Handles: let x: int = 10 + 5;
    @Override
    public String visitVarDecl(NovaParser.VarDeclContext ctx) {
        String id = ctx.ID().getText();
        String type = ctx.type().getText(); // Gets 'int' or 'float'

        // Semantic Error Check 1: Variable already declared?
        if (symbolMap.containsKey(id)) {
            System.err.println("SEMANTIC ERROR: Variable '" + id + "' is already declared.");
            System.exit(1);
        }

        // Add to Symbol Table
        symbolMap.put(id, type);

        String exprResult = visit(ctx.expr()); // Visit the math part first

        // Add to our TAC list
        tacList.add(new TACInstruction(id, exprResult, "=", ""));
        return id;
    }

    // Handles re-assignment: x = 20;
    @Override
    public String visitAssignment(NovaParser.AssignmentContext ctx) {
        String id = ctx.ID().getText();
        String exprResult = visit(ctx.expr());
        tacList.add(new TACInstruction(id, exprResult, "=", ""));
        return id;
    }

    // Handles Addition & Subtraction: a + b
    @Override
    public String visitAddSubExpr(NovaParser.AddSubExprContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String op = ctx.op.getText();

        String temp = newTemp();
        tacList.add(new TACInstruction(temp, left, op, right));
        return temp;
    }

    // Handles Multiplication & Division: a * b
    @Override
    public String visitMulDivExpr(NovaParser.MulDivExprContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String op = ctx.op.getText();

        String temp = newTemp();
        tacList.add(new TACInstruction(temp, left, op, right));
        return temp;
    }

    // Handles raw variables like 'x'
    @Override
    public String visitIdExpr(NovaParser.IdExprContext ctx) {
        String id = ctx.ID().getText();

        // Semantic Error Check 2: Did they declare it before using it?
        if (!symbolMap.containsKey(id)) {
            System.err.println("SEMANTIC ERROR: Undefined variable '" + id + "'. You must declare it first.");
            System.exit(1);
        }

        return id;
    }

    // Handles raw numbers like '15'
    @Override
    public String visitNumberExpr(NovaParser.NumberExprContext ctx) {
        return ctx.NUMBER().getText();
    }

    // Handles: print(x);
    @Override
    public String visitPrintStmt(NovaParser.PrintStmtContext ctx) {
        String exprResult = visit(ctx.expr());
        tacList.add(new TACInstruction("", exprResult, "print", ""));
        return null;
    }
}