/**
 * Calculator.java
 * Evaluates a math expression string as a number.
 * Supports: + - * / % ^ and parentheses.
 * Spaces are skipped. Uses the same rules as typical calculators (^ is power).
 *
 * How it works: we walk through the string with an index "pos" and parse
 * using small methods (each method handles one part of the grammar).
 */
public class Calculator {

    //contains the expression to evaluate
    private final String expr;
    //current character index
    private int pos;

    private Calculator(String expression) {
        this.expr = expression;
        this.pos = 0;
    }

    //evaluates the expression and returns the result
    public static double evaluate(String expression) throws IllegalArgumentException {
        if (expression == null) {
            throw new IllegalArgumentException("expression is null");
        }
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("expression is empty");
        }
        Calculator calc = new Calculator(trimmed);
        double value = calc.parseExpression();
        calc.skipSpaces();
        if (calc.pos < calc.expr.length()) {
            throw new IllegalArgumentException("extra characters after expression");
        }
        return value;
    }

    //skips spaces so "3 + 4" and "3+4" both work
    private void skipSpaces() {
        while (pos < expr.length() && expr.charAt(pos) == ' ') {
            pos++;
        }
    }

    //for add/sub expressions
    private double parseExpression() {
        return parseAddSub(); 
    }

    //for mul/div expressions
    private double parseAddSub() {
        double value = parseMulDiv();
        while (true) {
            skipSpaces();
            if (pos >= expr.length()) break;
            char op = expr.charAt(pos);
            if (op != '+' && op != '-') break;
            pos++;
            double right = parseMulDiv();
            if (op == '+') {
                value = value + right;
            } else {
                value = value - right;
            }
        }
        return value;
    }

    //for mul/div/mod expressions
    private double parseMulDiv() {
        double value = parsePow();
        while (true) {
            skipSpaces();
            if (pos >= expr.length()) break;
            char op = expr.charAt(pos);
            if (op != '*' && op != '/' && op != '%') break;
            pos++;
            double right = parsePow();
            if (op == '*') {
                value = value * right;
            } else if (op == '/') {
                if (right == 0.0) {
                    throw new IllegalArgumentException("division by zero");
                }
                value = value / right;
            } else {
                if (right == 0.0) {
                    throw new IllegalArgumentException("modulo by zero");
                }
                value = value % right;
            }
        }
        return value;
    }

    // Parses exponents like 2^3
    private double parsePow() {
        double base = parsePrimary();
        skipSpaces();
        if (pos < expr.length() && expr.charAt(pos) == '^') {
            pos++;
            double exp = parsePow();
            return Math.pow(base, exp);
        }
        return base;
    }

    /** primary -> number | '(' expr ')' */
    private double parsePrimary() {
        skipSpaces();
        if (pos >= expr.length()) {
            throw new IllegalArgumentException("unexpected end of expression");
        }
        char c = expr.charAt(pos);
        if (c == '(') {
            pos++;
            double inside = parseExpression();
            skipSpaces();
            if (pos >= expr.length() || expr.charAt(pos) != ')') {
                throw new IllegalArgumentException("missing ')'");
            }
            pos++;
            return inside;
        }
        return readNumber();
    }

    /** Reads digits and one optional dot for a decimal number. */
    private double readNumber() {
        skipSpaces();
        int start = pos;
        if (pos < expr.length() && expr.charAt(pos) == '.') {
            throw new IllegalArgumentException("number cannot start with '.'");
        }
        while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) {
            pos++;
        }
        if (pos < expr.length() && expr.charAt(pos) == '.') {
            pos++;
            while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) {
                pos++;
            }
        }
        if (start == pos) {
            throw new IllegalArgumentException("number expected");
        }
        String numStr = expr.substring(start, pos);
        return Double.parseDouble(numStr);
    }
}
