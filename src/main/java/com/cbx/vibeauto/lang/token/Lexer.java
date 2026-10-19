package com.cbx.vibeauto.lang.token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int lineStart = 0;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("void", TokenType.VOID);
        KEYWORDS.put("int", TokenType.INT);
        KEYWORDS.put("float", TokenType.FLOAT);
        KEYWORDS.put("bool", TokenType.BOOL);
        KEYWORDS.put("string", TokenType.STRING_KEYWORD);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("for", TokenType.FOR);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
    }

    public Lexer(String source) {
        this.source = source != null ? source : "";
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, current - lineStart + 1));
        return tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case '[' -> addToken(TokenType.LEFT_BRACKET);
            case ']' -> addToken(TokenType.RIGHT_BRACKET);
            case ',' -> addToken(TokenType.COMMA);
            case '.' -> addToken(TokenType.DOT);
            case ';' -> addToken(TokenType.SEMICOLON);
            case ':' -> addToken(TokenType.COLON);
            case '%' -> addToken(TokenType.PERCENT);
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '+' -> {
                if (match('+')) addToken(TokenType.PLUS_PLUS);
                else if (match('=')) addToken(TokenType.PLUS_EQUAL);
                else addToken(TokenType.PLUS);
            }
            case '-' -> {
                if (match('-')) addToken(TokenType.MINUS_MINUS);
                else if (match('=')) addToken(TokenType.MINUS_EQUAL);
                else addToken(TokenType.MINUS);
            }
            case '*' -> addToken(match('=') ? TokenType.STAR_EQUAL : TokenType.STAR);
            case '/' -> {
                if (match('/')) {
                    // Line comment
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // Block comment
                    blockComment();
                } else if (match('=')) {
                    addToken(TokenType.SLASH_EQUAL);
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            case '&' -> {
                if (match('&')) addToken(TokenType.AND_AND);
                else addToken(TokenType.AND);
            }
            case '|' -> {
                if (match('|')) addToken(TokenType.OR_OR);
                else addToken(TokenType.OR);
            }
            case ' ', '\r', '\t' -> {}
            case '\n' -> {
                line++;
                lineStart = current;
            }
            case '"' -> string();
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    error("Unexpected character '" + c + "'");
                }
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = KEYWORDS.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type, text);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the dot
            advance();
            while (isDigit(peek())) advance();
            double val = Double.parseDouble(source.substring(start, current));
            addToken(TokenType.NUMBER, val);
            return;
        }

        long val = Long.parseLong(source.substring(start, current));
        addToken(TokenType.NUMBER, val);
    }

    private void string() {
        StringBuilder sb = new StringBuilder();
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                lineStart = current;
            }
            if (peek() == '\\') {
                advance();
                char esc = advance();
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(esc);
                }
            } else {
                sb.append(advance());
            }
        }

        if (isAtEnd()) {
            error("Unterminated string");
            return;
        }

        // The closing quote
        advance();
        addToken(TokenType.STRING, sb.toString());
    }

    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '\n') {
                line++;
                lineStart = current;
            }
            if (peek() == '*' && peekNext() == '/') {
                advance(); // *
                advance(); // /
                return;
            }
            advance();
        }
        error("Unterminated block comment");
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        int col = start - lineStart + 1;
        tokens.add(new Token(type, text, literal, line, col));
    }

    private void error(String message) {
        int col = start - lineStart + 1;
        errors.add("[" + line + ":" + col + "] Lexer Error: " + message);
    }
}
