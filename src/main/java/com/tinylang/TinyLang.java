package com.tinylang;

import com.tinylang.ast.Stmt;
import com.tinylang.error.RuntimeError;
import com.tinylang.token.Token;
import com.tinylang.token.TokenType;
import com.tinylang.util.TLangFileValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TinyLang {

    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("Usage: tlang <source-file>");
            return;
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void run(String source, boolean isRepl) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) return;

        /* AstPrinter.print(statements); // Uncomment to print the AST */
        if (isRepl && statements.size() == 1 && statements.get(0) instanceof Stmt.Expression exprStmt) {
            interpreter.interpret(exprStmt.expression);
            return;
        }
        interpreter.interpret(statements);
        if (hadRuntimeError) System.exit(2);
    }

    private static void runFile(String filePath) {
        Path path = getPath(filePath);
        if (path == null || !TLangFileValidator.isValidTLangFile(path)) {
            System.err.println("Invalid path: " + filePath);
            return;
        }

        String content = readFile(path);
        run(content, false);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(isr);

        while (true) {
            System.out.print("> ");
            String line;
            line = reader.readLine();
            if (line == null || line.equalsIgnoreCase("exit")) break;
            run(line, true);
            hadError = false;
        }
    }

    private static Path getPath(String arg) {
        try {
            return Paths.get(arg);
        } catch (InvalidPathException ipe) {
            return null;
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            System.err.println("An error has occurred while reading the file: " + path);
            return null;
        }
    }

    public static void error(String message) {
        report(0, "", message);
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), " at end", message);
        } else {
            report(token.line(), " at '" + token.lexeme() + "'", message);
        }
    }

    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token().line() + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
