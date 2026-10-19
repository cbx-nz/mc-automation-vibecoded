package com.cbx.vibeauto.lang.ast;

import com.cbx.vibeauto.lang.token.Token;
import java.util.List;

public sealed interface Stmt {
    record Expression(Expr expression) implements Stmt {}
    record Var(Token name, Expr initializer) implements Stmt {}
    record Block(List<Stmt> statements) implements Stmt {}
    record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}
    record While(Expr condition, Stmt body) implements Stmt {}
    record For(Stmt initializer, Expr condition, Expr increment, Stmt body) implements Stmt {}
    record Return(Token keyword, Expr value) implements Stmt {}
    record Function(Token name, List<Token> params, List<Stmt> body) implements Stmt {}
    record Program(List<Stmt> statements) implements Stmt {}
}
