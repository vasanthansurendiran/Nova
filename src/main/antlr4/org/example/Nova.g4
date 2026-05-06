grammar Nova;

program : statement* EOF ;

statement
    : varDecl
    | arrayDecl
    | assignment
    | arrayAssign
    | printStmt
    | ifStmt
    | whileStmt
    | forStmt
    | block
    ;

block : '{' statement* '}' ;

// 1. Declarations: Optional type inference, NO semicolons
varDecl : LET ID (':' type)? ('=' expr)? ;

// 2. Arrays: Optional type, inline initialization list, NO semicolons
arrayDecl : LET ID '[' NUMBER ']' (':' type)? ('=' '[' expr (',' expr)* ']')? ;

// 3. Assignments: NO semicolons
assignment : ID '=' expr ;
arrayAssign : ID '[' expr ']' '=' expr ;

// 4. Print: NO semicolons
printStmt : PRINT '(' expr ')' ;

// Control Flow (Requires blocks)
ifStmt : IF '(' expr ')' block (ELSE block)? ;
whileStmt : WHILE '(' expr ')' block ;
forStmt : FOR ID IN expr DOT_DOT expr block ;

type : INT_TYPE | FLOAT_TYPE ;

expr
    : left=expr op=(MUL | DIV) right=expr                    # MulDivExpr
    | left=expr op=(ADD | SUB) right=expr                    # AddSubExpr
    | left=expr op=(LT | GT | LE | GE | EQ | NEQ) right=expr # RelationalExpr
    | ID '[' index=expr ']'                                  # ArrayAccessExpr
    | '(' expr ')'                                           # ParenExpr
    | ID                                                     # IdExpr
    | NUMBER                                                 # NumberExpr
    ;

// ==========================================
// LEXER RULES
// ==========================================

LET : 'let' ;
PRINT : 'print' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FOR : 'for' ;
IN : 'in' ;
INT_TYPE : 'int' ;
FLOAT_TYPE : 'float' ;

DOT_DOT : '..' ;
ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
LT  : '<' ;
GT  : '>' ;
LE  : '<=' ;
GE  : '>=' ;
EQ  : '==' ;
NEQ : '!=' ;

ID : [a-zA-Z_][a-zA-Z0-9_]* ;
NUMBER : [0-9]+ ('.' [0-9]+)? ;

COMMENT : '//' ~[\r\n]* -> skip ;
WS : [ \t\r\n]+ -> skip ;