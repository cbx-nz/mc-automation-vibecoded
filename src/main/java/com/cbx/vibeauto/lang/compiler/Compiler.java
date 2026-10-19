package com.cbx.vibeauto.lang.compiler;

import com.cbx.vibeauto.lang.ast.Expr;
import com.cbx.vibeauto.lang.ast.Stmt;
import com.cbx.vibeauto.lang.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Compiler {
    private final CompiledProgram program = new CompiledProgram();
    private final List<String> errors = new ArrayList<>();

    public CompiledProgram compile(Stmt.Program ast) {
        // Emit jump over functions to setup / main
        int mainJump = program.emit(OpCode.JUMP, 0, 1);

        for (Stmt stmt : ast.statements()) {
            if (stmt instanceof Stmt.Function f) {
                compileFunction(f);
            }
        }

        int mainStart = program.getInstructions().size();
        program.patch(mainJump, mainStart);

        for (Stmt stmt : ast.statements()) {
            if (!(stmt instanceof Stmt.Function)) {
                compileStmt(stmt);
            }
        }

        program.emit(OpCode.HALT, 0, 1);
        return program;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void compileFunction(Stmt.Function f) {
        int address = program.getInstructions().size();
        program.registerFunction(f.name().lexeme(), address);

        // Function arguments are popped into variables in reverse
        for (int i = f.params().size() - 1; i >= 0; i--) {
            int varConst = program.addConstant(f.params().get(i).lexeme());
            program.emit(OpCode.STORE_VAR, varConst, f.name().line());
        }

        for (Stmt s : f.body()) {
            compileStmt(s);
        }

        // Default return void
        int nullConst = program.addConstant(null);
        program.emit(OpCode.CONST, nullConst, f.name().line());
        program.emit(OpCode.RETURN, 0, f.name().line());
    }

    private void compileStmt(Stmt stmt) {
        switch (stmt) {
            case Stmt.Expression e -> {
                compileExpr(e.expression());
                program.emit(OpCode.POP, 0, 1);
            }
            case Stmt.Var v -> {
                if (v.initializer() != null) {
                    compileExpr(v.initializer());
                } else {
                    int c = program.addConstant(null);
                    program.emit(OpCode.CONST, c, v.name().line());
                }
                int varConst = program.addConstant(v.name().lexeme());
                program.emit(OpCode.STORE_VAR, varConst, v.name().line());
            }
            case Stmt.Block b -> {
                for (Stmt s : b.statements()) {
                    compileStmt(s);
                }
            }
            case Stmt.If i -> {
                compileExpr(i.condition());
                int jumpIfFalse = program.emit(OpCode.JUMP_IF_FALSE, 0, 1);
                compileStmt(i.thenBranch());

                if (i.elseBranch() != null) {
                    int jumpOverElse = program.emit(OpCode.JUMP, 0, 1);
                    program.patch(jumpIfFalse, program.getInstructions().size());
                    compileStmt(i.elseBranch());
                    program.patch(jumpOverElse, program.getInstructions().size());
                } else {
                    program.patch(jumpIfFalse, program.getInstructions().size());
                }
            }
            case Stmt.While w -> {
                int loopStart = program.getInstructions().size();
                compileExpr(w.condition());
                int jumpOut = program.emit(OpCode.JUMP_IF_FALSE, 0, 1);
                compileStmt(w.body());
                program.emit(OpCode.JUMP, loopStart, 1);
                program.patch(jumpOut, program.getInstructions().size());
            }
            case Stmt.For f -> {
                if (f.initializer() != null) compileStmt(f.initializer());
                int loopStart = program.getInstructions().size();
                int jumpOut = -1;
                if (f.condition() != null) {
                    compileExpr(f.condition());
                    jumpOut = program.emit(OpCode.JUMP_IF_FALSE, 0, 1);
                }
                compileStmt(f.body());
                if (f.increment() != null) {
                    compileExpr(f.increment());
                    program.emit(OpCode.POP, 0, 1);
                }
                program.emit(OpCode.JUMP, loopStart, 1);
                if (jumpOut != -1) {
                    program.patch(jumpOut, program.getInstructions().size());
                }
            }
            case Stmt.Return r -> {
                if (r.value() != null) {
                    compileExpr(r.value());
                } else {
                    int c = program.addConstant(null);
                    program.emit(OpCode.CONST, c, r.keyword().line());
                }
                program.emit(OpCode.RETURN, 0, r.keyword().line());
            }
            case Stmt.Program p -> {
                for (Stmt s : p.statements()) compileStmt(s);
            }
            case Stmt.Function f -> compileFunction(f);
        }
    }

    private void compileExpr(Expr expr) {
        switch (expr) {
            case Expr.Literal l -> {
                int c = program.addConstant(l.value());
                program.emit(OpCode.CONST, c, 1);
            }
            case Expr.Variable v -> {
                int c = program.addConstant(v.name().lexeme());
                program.emit(OpCode.LOAD_VAR, c, v.name().line());
            }
            case Expr.Assignment a -> {
                compileExpr(a.value());
                int c = program.addConstant(a.name().lexeme());
                program.emit(OpCode.STORE_VAR, c, a.name().line());
                program.emit(OpCode.LOAD_VAR, c, a.name().line());
            }
            case Expr.Grouping g -> compileExpr(g.expression());
            case Expr.Unary u -> {
                compileExpr(u.right());
                if (u.operator().type() == TokenType.MINUS) {
                    program.emit(OpCode.NEG, 0, u.operator().line());
                } else if (u.operator().type() == TokenType.BANG) {
                    program.emit(OpCode.NOT, 0, u.operator().line());
                }
            }
            case Expr.Binary b -> {
                compileExpr(b.left());
                compileExpr(b.right());
                int line = b.operator().line();
                switch (b.operator().type()) {
                    case PLUS -> program.emit(OpCode.ADD, 0, line);
                    case MINUS -> program.emit(OpCode.SUB, 0, line);
                    case STAR -> program.emit(OpCode.MUL, 0, line);
                    case SLASH -> program.emit(OpCode.DIV, 0, line);
                    case PERCENT -> program.emit(OpCode.MOD, 0, line);
                    case EQUAL_EQUAL -> program.emit(OpCode.EQ, 0, line);
                    case BANG_EQUAL -> program.emit(OpCode.NEQ, 0, line);
                    case LESS -> program.emit(OpCode.LT, 0, line);
                    case LESS_EQUAL -> program.emit(OpCode.LTE, 0, line);
                    case GREATER -> program.emit(OpCode.GT, 0, line);
                    case GREATER_EQUAL -> program.emit(OpCode.GTE, 0, line);
                    default -> {}
                }
            }
            case Expr.PropertyAccess p -> {
                compileExpr(p.object());
                int propConst = program.addConstant(p.name().lexeme());
                program.emit(OpCode.GET_PROP, propConst, p.name().line());
            }
            case Expr.Call c -> {
                // Callee can be a PropertyAccess (e.g. obj.method()) or Variable (function())
                if (c.callee() instanceof Expr.PropertyAccess pa) {
                    for (Expr.CallArg arg : c.arguments()) {
                        compileExpr(arg.value());
                    }
                    compileExpr(pa.object());
                    int methodConst = program.addConstant(pa.name().lexeme());
                    program.emit(OpCode.CALL_METHOD, methodConst, c.paren().line());
                } else if (c.callee() instanceof Expr.Variable v) {
                    // Check if it's a global call
                    for (Expr.CallArg arg : c.arguments()) {
                        compileExpr(arg.value());
                    }
                    int fnConst = program.addConstant(v.name().lexeme());
                    program.emit(OpCode.CALL, fnConst, c.paren().line());
                } else {
                    compileExpr(c.callee());
                    for (Expr.CallArg arg : c.arguments()) {
                        compileExpr(arg.value());
                    }
                    int fnConst = program.addConstant("anonymous");
                    program.emit(OpCode.CALL, fnConst, c.paren().line());
                }
            }
        }
    }
}
