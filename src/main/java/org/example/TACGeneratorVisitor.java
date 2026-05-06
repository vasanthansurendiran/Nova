package org.example;

import java.util.*;

public class TACGeneratorVisitor extends NovaBaseVisitor<String> {

    public static class Symbol {
        public String name, type;
        public int scopeLevel;
        public Symbol(String n, String t, int s) { name = n; type = t; scopeLevel = s; }
    }

    public List<TACInstruction> tacList = new ArrayList<>();
    public List<Symbol> symbolTracker = new ArrayList<>();

    private Stack<Map<String, String>> scopes = new Stack<>();
    private int currentScopeLevel = 0;
    private int tempCounter = 1;
    private int labelCounter = 1;

    public TACGeneratorVisitor() {
        scopes.push(new HashMap<>()); // Global Scope
    }

    private String newTemp() { return "t" + (tempCounter++); }
    private String newLabel() { return "L" + (labelCounter++); }

    // --- SECURITY PROTOCOL: SEMANTIC ERROR CHECKING ---
    private String getVarType(String id) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(id)) return scopes.get(i).get(id);
        }
        System.err.println("SEMANTIC ERROR: Undefined variable '" + id + "'.");
        System.exit(1);
        return null;
    }

    private void enforceType(String expected, String actual, String errorMsg) {
        if (!expected.equals(actual) && !actual.equals("unknown")) {
            System.err.println("SEMANTIC ERROR: " + errorMsg + " (Expected: " + expected + ", Found: " + actual + ")");
            System.exit(1);
        }
    }

    private String resolveType(String value) {
        if (value.matches("[0-9]+")) return "int";
        if (value.matches("[0-9]+\\.[0-9]+")) return "float";
        if (value.equals("true") || value.equals("false")) return "int";

        for (int i = symbolTracker.size() - 1; i >= 0; i--) {
            if (symbolTracker.get(i).name.equals(value)) {
                return symbolTracker.get(i).type;
            }
        }
        return "unknown";
    }

    // --- SCOPE & BLOCKS ---
    @Override
    public String visitBlock(NovaParser.BlockContext ctx) {
        currentScopeLevel++;
        scopes.push(new HashMap<>());
        for (NovaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        scopes.pop();
        currentScopeLevel--;
        return null;
    }

    // --- DECLARATIONS (MODERNIZED) ---
    @Override
    public String visitVarDecl(NovaParser.VarDeclContext ctx) {
        String id = ctx.ID().getText();

        if (scopes.peek().containsKey(id)) {
            System.err.println("SEMANTIC ERROR: Variable '" + id + "' is already defined in this scope.");
            System.exit(1);
        }

        String declaredType;
        String exprResult = null;
        String exprType = null;

        if (ctx.expr() != null) {
            exprResult = visit(ctx.expr());
            exprType = resolveType(exprResult);
        }

        if (ctx.type() != null) {
            declaredType = ctx.type().getText();
            if (exprType != null) {
                enforceType(declaredType, exprType, "Type mismatch in declaration for '" + id + "'");
            }
        } else {
            if (exprType == null || exprType.equals("unknown")) {
                System.err.println("SEMANTIC ERROR: Cannot infer type for '" + id + "'. You must provide an initial value or an explicit type.");
                System.exit(1);
                return null;
            }
            declaredType = exprType;
        }

        scopes.peek().put(id, declaredType);
        symbolTracker.add(new Symbol(id, declaredType, currentScopeLevel));

        if (exprResult != null) {
            tacList.add(new TACInstruction(id, exprResult, "=", ""));
        }
        return null;
    }

    @Override
    public String visitArrayDecl(NovaParser.ArrayDeclContext ctx) {
        String id = ctx.ID().getText();
        String sizeStr = ctx.NUMBER().getText();
        int size = Integer.parseInt(sizeStr);
        String baseType;

        if (ctx.type() != null) {
            baseType = ctx.type().getText();
        } else {
            if (!ctx.expr().isEmpty()) {
                baseType = resolveType(visit(ctx.expr(0)));
            } else {
                System.err.println("SEMANTIC ERROR: Cannot infer type for array '" + id + "'. You must provide values or an explicit type.");
                System.exit(1);
                return null;
            }
        }

        String arrayType = baseType + "[]";
        scopes.peek().put(id, arrayType);
        symbolTracker.add(new Symbol(id, arrayType, currentScopeLevel));

        tacList.add(new TACInstruction(id, sizeStr, "alloc", arrayType));

        if (!ctx.expr().isEmpty()) {
            if (ctx.expr().size() > size) {
                System.err.println("SEMANTIC ERROR: Array '" + id + "' is size " + size + " but you provided " + ctx.expr().size() + " values.");
                System.exit(1);
            }
            for (int i = 0; i < ctx.expr().size(); i++) {
                String val = visit(ctx.expr(i));
                String valType = resolveType(val);
                enforceType(baseType, valType, "Type mismatch in array initialization for '" + id + "' at index " + i);
                tacList.add(new TACInstruction(id + "[" + i + "]", val, "=", ""));
            }
        }
        return null;
    }

    // --- ASSIGNMENTS & ARRAYS ---
    @Override
    public String visitAssignment(NovaParser.AssignmentContext ctx) {
        String id = ctx.ID().getText();
        String declaredType = getVarType(id);
        String exprResult = visit(ctx.expr());
        String exprType = resolveType(exprResult);

        enforceType(declaredType, exprType, "Type mismatch in assignment for '" + id + "'");
        tacList.add(new TACInstruction(id, exprResult, "=", ""));
        return null;
    }

    @Override
    public String visitArrayAssign(NovaParser.ArrayAssignContext ctx) {
        String id = ctx.ID().getText();
        String arrayType = getVarType(id);
        String baseType = arrayType.replace("[]", "");

        String index = visit(ctx.expr(0));
        String indexType = resolveType(index);
        enforceType("int", indexType, "Array index for '" + id + "' must be an integer");

        String val = visit(ctx.expr(1));
        String valType = resolveType(val);
        enforceType(baseType, valType, "Type mismatch when assigning to array '" + id + "'");

        tacList.add(new TACInstruction(id + "[" + index + "]", val, "=", ""));
        return null;
    }

    @Override
    public String visitArrayAccessExpr(NovaParser.ArrayAccessExprContext ctx) {
        String id = ctx.ID().getText();
        String arrayType = getVarType(id);

        String index = visit(ctx.index);
        String indexType = resolveType(index);
        enforceType("int", indexType, "Array index for '" + id + "' must be an integer");

        String temp = newTemp();
        tacList.add(new TACInstruction(temp, id + "[" + index + "]", "=", ""));
        String baseType = arrayType.replace("[]", "");
        symbolTracker.add(new Symbol(temp, baseType, currentScopeLevel));

        return temp;
    }

    // --- CONTROL FLOW ---
    @Override
    public String visitIfStmt(NovaParser.IfStmtContext ctx) {
        String condTemp = visit(ctx.expr());
        String condType = resolveType(condTemp);
        enforceType("int", condType, "'if' condition must evaluate to a logical boolean/int");

        String falseLabel = newLabel();
        String endLabel = newLabel();

        tacList.add(new TACInstruction(falseLabel, condTemp, "ifFalseGOTO", ""));
        visit(ctx.block(0));
        tacList.add(new TACInstruction(endLabel, "", "goto", ""));
        tacList.add(new TACInstruction(falseLabel, "", "label", ""));

        if (ctx.block().size() > 1) {
            visit(ctx.block(1));
        }
        tacList.add(new TACInstruction(endLabel, "", "label", ""));
        return null;
    }

    @Override
    public String visitWhileStmt(NovaParser.WhileStmtContext ctx) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        tacList.add(new TACInstruction(startLabel, "", "label", ""));
        String condTemp = visit(ctx.expr());
        String condType = resolveType(condTemp);
        enforceType("int", condType, "'while' condition must evaluate to a logical boolean/int");

        tacList.add(new TACInstruction(endLabel, condTemp, "ifFalseGOTO", ""));
        visit(ctx.block());
        tacList.add(new TACInstruction(startLabel, "", "goto", ""));
        tacList.add(new TACInstruction(endLabel, "", "label", ""));
        return null;
    }

    @Override
    public String visitForStmt(NovaParser.ForStmtContext ctx) {
        String iterId = ctx.ID().getText();
        String startExpr = visit(ctx.expr(0));
        enforceType("int", resolveType(startExpr), "'for' loop start boundary must be an integer");

        String endExpr = visit(ctx.expr(1));
        enforceType("int", resolveType(endExpr), "'for' loop end boundary must be an integer");

        currentScopeLevel++;
        scopes.push(new HashMap<>());
        scopes.peek().put(iterId, "int");
        symbolTracker.add(new Symbol(iterId, "int", currentScopeLevel));

        tacList.add(new TACInstruction(iterId, startExpr, "=", ""));

        String startLabel = newLabel();
        String endLabel = newLabel();
        tacList.add(new TACInstruction(startLabel, "", "label", ""));

        String condTemp = newTemp();
        tacList.add(new TACInstruction(condTemp, iterId, "<", endExpr));
        symbolTracker.add(new Symbol(condTemp, "int", currentScopeLevel));
        tacList.add(new TACInstruction(endLabel, condTemp, "ifFalseGOTO", ""));

        visit(ctx.block());

        String incTemp = newTemp();
        tacList.add(new TACInstruction(incTemp, iterId, "+", "1"));
        symbolTracker.add(new Symbol(incTemp, "int", currentScopeLevel));
        tacList.add(new TACInstruction(iterId, incTemp, "=", ""));

        tacList.add(new TACInstruction(startLabel, "", "goto", ""));
        tacList.add(new TACInstruction(endLabel, "", "label", ""));

        scopes.pop();
        currentScopeLevel--;
        return null;
    }

    // --- MATH & RELATIONAL ---
    @Override
    public String visitAddSubExpr(NovaParser.AddSubExprContext ctx) { return buildMath(ctx.left, ctx.right, ctx.op.getText()); }
    @Override
    public String visitMulDivExpr(NovaParser.MulDivExprContext ctx) { return buildMath(ctx.left, ctx.right, ctx.op.getText()); }
    @Override
    public String visitRelationalExpr(NovaParser.RelationalExprContext ctx) { return buildMath(ctx.left, ctx.right, ctx.op.getText()); }

    private String buildMath(NovaParser.ExprContext leftCtx, NovaParser.ExprContext rightCtx, String op) {
        String left = visit(leftCtx);
        String right = visit(rightCtx);

        String leftType = resolveType(left);
        String rightType = resolveType(right);
        String resultType;

        if (leftType.contains("[]") || rightType.contains("[]")) {
            System.err.println("SEMANTIC ERROR: Cannot perform math/relational operations directly on arrays.");
            System.exit(1);
        }

        if (op.matches("<|>|<=|>=|==|!=")) {
            resultType = "int";
        } else {
            resultType = (leftType.equals("float") || rightType.equals("float")) ? "float" : "int";
        }

        String temp = newTemp();
        tacList.add(new TACInstruction(temp, left, op, right));
        symbolTracker.add(new Symbol(temp, resultType, currentScopeLevel));

        return temp;
    }

    // --- BASICS ---
    @Override
    public String visitIdExpr(NovaParser.IdExprContext ctx) {
        getVarType(ctx.ID().getText());
        return ctx.ID().getText();
    }
    @Override
    public String visitNumberExpr(NovaParser.NumberExprContext ctx) { return ctx.NUMBER().getText(); }
    @Override
    public String visitParenExpr(NovaParser.ParenExprContext ctx) { return visit(ctx.expr()); }

    @Override
    public String visitPrintStmt(NovaParser.PrintStmtContext ctx) {
        String exprResult = visit(ctx.expr());
        String format = resolveType(exprResult).equals("float") ? "float" : "int";
        tacList.add(new TACInstruction("", exprResult, "print", format));
        return null;
    }
}