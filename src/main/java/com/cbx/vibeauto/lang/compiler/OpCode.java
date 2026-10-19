package com.cbx.vibeauto.lang.compiler;

public enum OpCode {
    // Constants & Stack
    CONST, POP, DUP,

    // Arithmetic
    ADD, SUB, MUL, DIV, MOD, NEG,

    // Comparison
    EQ, NEQ, LT, GT, LTE, GTE,

    // Logic
    NOT,

    // Variables
    LOAD_VAR, STORE_VAR,

    // Control flow
    JUMP, JUMP_IF_FALSE,

    // Functions
    CALL, RETURN,

    // Native Calls & Property Access
    CALL_METHOD, GET_PROP,

    // Special
    HALT
}
