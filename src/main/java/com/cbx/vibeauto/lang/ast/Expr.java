package com.cbx.vibeauto.lang.ast;

import com.cbx.vibeauto.lang.token.Token;
import java.util.List;

public sealed interface Expr {
    record Literal(Object value) implements Expr {}
    record Variable(Token name) implements Expr {}
    record Binary(Expr left, Token operator, Expr right) implements Expr {}
    record Unary(Token operator, Expr right) implements Expr {}
    record Grouping(Expr expression) implements Expr {}
    record Assignment(Token name, Expr value) implements Expr {}
    record PropertyAccess(Expr object, Token name) implements Expr {}
    record Call(Expr callee, Token paren, List<CallArg> arguments) implements Expr {}

    record CallArg(String name, Expr value) {}
}
