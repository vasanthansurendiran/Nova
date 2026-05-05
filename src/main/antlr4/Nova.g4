grammar Nova;

@header {
    package org.example;
}

// ==========================================
// PARSER RULES (Syntax & Structure)
// ... rest of your rules stay exactly the same
// A program is just a list of statements followed by the End Of File
program : statement* EOF ;

// The different types of lines you can write in Nova
statement
    : varDecl
    | assignment
    | printStmt
    | ifStmt
    | forStmt
    ;

// Variable Declaration: let x: int = 10;
varDecl : LET ID ':' type '=' expr ';' ;

// Assignment: x = 20;
assignment : ID '=' expr ';' ;

// Print: print(x);
printStmt : PRINT '(' expr ')' ';' ;

// If/Else Block
ifStmt : IF '(' expr ')' '{' statement* '}' (ELSE '{' statement* '}')? ;

// Range For Loop: for i in 0..10 { ... }
forStmt : FOR ID IN expr DOT_DOT expr '{' statement* '}' ;

// Data Types
type : INT_TYPE | FLOAT_TYPE ;

// Expressions (Math and Logic).
// The '#' tags tell ANTLR to generate specific Visitor methods for us later. This is critical for TAC.
expr
    : left=expr op=(MUL | DIV) right=expr         # MulDivExpr
    | left=expr op=(ADD | SUB) right=expr         # AddSubExpr
    | left=expr op=(LT | GT | EQ | NEQ) right=expr # RelationalExpr
    | '(' expr ')'                                # ParenExpr
    | ID                                          # IdExpr
    | NUMBER                                      # NumberExpr
    ;

// ==========================================
// LEXER RULES (Vocabulary & Tokens)
// ==========================================

// Keywords
LET : 'let' ;
PRINT : 'print' ;
IF : 'if' ;
ELSE : 'else' ;
FOR : 'for' ;
IN : 'in' ;
INT_TYPE : 'int' ;
FLOAT_TYPE : 'float' ;

// Operators & Symbols
DOT_DOT : '..' ;
ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
LT  : '<' ;
GT  : '>' ;
EQ  : '==' ;
NEQ : '!=' ;

// Identifiers (Variables) and Numbers
ID : [a-zA-Z_][a-zA-Z0-9_]* ;
NUMBER : [0-9]+ ('.' [0-9]+)? ;

// Ignore spaces, tabs, and newlines
WS : [ \t\r\n]+ -> skip ;