package com.cbx.vibeauto.lang.token;

public enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET, COMMA, DOT, SEMICOLON, COLON,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    PLUS, PLUS_PLUS, PLUS_EQUAL,
    MINUS, MINUS_MINUS, MINUS_EQUAL,
    STAR, STAR_EQUAL,
    SLASH, SLASH_EQUAL,
    PERCENT,
    AND, AND_AND,
    OR, OR_OR,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    VOID, INT, FLOAT, BOOL, STRING_KEYWORD,
    IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE,

    // End of file
    EOF
}
