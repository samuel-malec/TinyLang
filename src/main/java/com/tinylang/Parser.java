package com.tinylang;

import com.tinylang.ast.Expr;
import com.tinylang.ast.Stmt;
import com.tinylang.token.Token;
import com.tinylang.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDeclaration();
            if (check(TokenType.FN) && checkNext(TokenType.IDENTIFIER)) {
                consume(TokenType.FN, "Expect 'fn'.");
                return funDeclaration("function", false);
            }
            if (match(TokenType.LET)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
        Expr.VarExpr superClass = null;
        if (match(TokenType.EXTENDS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superClass = new Expr.VarExpr(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            boolean isStatic = false;
            if (match(TokenType.CLASS)) isStatic = true;
            methods.add(funDeclaration("method", isStatic));
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, superClass, methods);
    }

    private Stmt.Function funDeclaration(String kind, boolean isStatic) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name.").lexeme());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name.lexeme(), parameters, body, isStatic);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Let(name.lexeme(), initializer);
    }

    private Stmt statement() {
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.LET)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) {
            condition = new Expr.LiteralExpr(true);
        }
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(value);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = logicOr();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.VarExpr varExpr) {
                Token name = varExpr.name;
                return new Expr.AssignExpr(name, value);
            } else if (expr instanceof Expr.GetExpr getExpr) {
                return new Expr.SetExpr(getExpr.object, getExpr.name, value);
            }
            TinyLang.error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr logicOr() {
        Expr expr = logicalAnd();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = logicalAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr logicalAnd() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = power();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            Expr right = power();
            expr = new Expr.BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expr power() {
        Expr expr = unary();
        while (match(TokenType.STAR_STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.BinaryExpr(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.UnaryExpr(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.GetExpr(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.CallExpr(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.LiteralExpr(false);
        if (match(TokenType.TRUE)) return new Expr.LiteralExpr(true);
        if (match(TokenType.NIL)) return new Expr.LiteralExpr(null);
        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.LiteralExpr(previous().literal());
        }
        if (match(TokenType.THIS)) {
            return new Expr.ThisExpr(previous());
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.VarExpr(previous());
        }
        if (match(TokenType.SUPER)) {
            Token keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'.");
            Token method = consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.GroupingExpr(expr);
        }
        if (match(TokenType.FN)) return functionExpression();
        throw error(peek(), "Expect expression.");
    }

    private Expr functionExpression() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'fn'.");
        List<String> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name.").lexeme());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before function body.");
        List<Stmt> body = block();
        return new Expr.Function(parameters, body);
    }

    private boolean match(TokenType... tokenTypes) {
        for (TokenType type : tokenTypes) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private boolean checkNext(TokenType tokenType) {
        if (isAtEnd()) return false;
        if (tokens.get(current + 1).type() == TokenType.EOF) return false;
        return tokens.get(current + 1).type() == tokenType;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) return;
            switch (peek().type()) {
                case CLASS, FN, LET, FOR, IF, WHILE, PRINT, RETURN -> {
                    return;
                }
            }
            advance();
        }
    }

    private ParseError error(Token token, String message) {
        TinyLang.error(token, message);
        return new ParseError();
    }
}
