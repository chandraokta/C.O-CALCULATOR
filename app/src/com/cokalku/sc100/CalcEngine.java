package com.cokalku.sc100;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Expression parser and evaluator for the Kalku calculator. */
public final class CalcEngine {
    public enum Angle { DEG, RAD, GRAD }

    public static final class EvalException extends Exception {
        public EvalException(String message) { super(message); }
    }

    public static final class Context {
        public Angle angle = Angle.DEG;
        public BigDecimal ans = BigDecimal.ZERO;
        public Context() {}
        public Context(Angle a, BigDecimal last) { angle = a; ans = last == null ? BigDecimal.ZERO : last; }
    }

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal PI = new BigDecimal("3.1415926535897932384626433832795028841971693993751");
    private static final BigDecimal E = new BigDecimal("2.7182818284590452353602874713526624977572470937000");
    private static final BigDecimal PHI = new BigDecimal("1.6180339887498948482045868343656381177203091798058");

    private interface Node {
        BigDecimal eval(Context ctx, BigDecimal percentBase) throws EvalException;
    }

    private static final class Num implements Node {
        final BigDecimal v;
        Num(BigDecimal v) { this.v = v; }
        public BigDecimal eval(Context c, BigDecimal p) { return v; }
    }

    private static final class Constant implements Node {
        final String name;
        Constant(String name) { this.name = name; }
        public BigDecimal eval(Context c, BigDecimal p) throws EvalException {
            switch (name) {
                case "pi": return PI;
                case "e": return E;
                case "phi": return PHI;
                case "ans": return c.ans;
                case "c": return new BigDecimal("299792458");
                case "g": return new BigDecimal("9.80665");
                default: throw new EvalException("Unknown constant");
            }
        }
    }

    private static final class Unary implements Node {
        final boolean negate;
        final Node child;
        Unary(boolean n, Node c) { negate = n; child = c; }
        public BigDecimal eval(Context ctx, BigDecimal p) throws EvalException {
            BigDecimal v = child.eval(ctx, p);
            return negate ? v.negate(MC) : v;
        }
    }

    private static final class Percent implements Node {
        final Node child;
        Percent(Node c) { child = c; }
        public BigDecimal eval(Context ctx, BigDecimal base) throws EvalException {
            BigDecimal v = child.eval(ctx, null).divide(BigDecimal.valueOf(100), MC);
            return base == null ? v : base.multiply(v, MC);
        }
    }

    private static final class Factorial implements Node {
        final Node child;
        Factorial(Node c) { child = c; }
        public BigDecimal eval(Context ctx, BigDecimal p) throws EvalException {
            BigDecimal v = child.eval(ctx, p);
            if (v.signum() < 0) throw new EvalException("Factorial of a negative number");
            if (v.stripTrailingZeros().scale() > 0) throw new EvalException("Factorial needs a whole number");
            int n;
            try { n = v.intValueExact(); } catch (ArithmeticException ex) { throw new EvalException("Factorial too large"); }
            if (n > 170) throw new EvalException("Factorial too large");
            double r = 1.0;
            for (int i = 2; i <= n; i++) r *= i;
            return BigDecimal.valueOf(r);
        }
    }

    private static final class Binary implements Node {
        final char op;
        final Node left, right;
        Binary(char op, Node l, Node r) { this.op = op; left = l; right = r; }
        public BigDecimal eval(Context ctx, BigDecimal p) throws EvalException {
            BigDecimal a = left.eval(ctx, p);
            if (op == '+' || op == '-') {
                BigDecimal b = right.eval(ctx, a);
                return op == '+' ? a.add(b, MC) : a.subtract(b, MC);
            }
            BigDecimal b = right.eval(ctx, null);
            switch (op) {
                case '*': return a.multiply(b, MC);
                case '/':
                    if (b.signum() == 0) throw new EvalException("Division by zero");
                    return a.divide(b, MC);
                case '^': return power(a, b);
                case 'm': return mod(a, b);
                case 'C': return combinations(a, b);
                case 'P': return permutations(a, b);
                default: throw new EvalException("Unknown operator");
            }
        }
    }

    private static final class Function implements Node {
        final String name;
        final Node arg1, arg2;
        Function(String n, Node a, Node b) { name = n; arg1 = a; arg2 = b; }
        public BigDecimal eval(Context ctx, BigDecimal p) throws EvalException {
            BigDecimal a = arg1.eval(ctx, null);
            double x = a.doubleValue();
            switch (name) {
                case "sin": return d(Math.sin(toRad(x, ctx.angle)));
                case "cos": return d(Math.cos(toRad(x, ctx.angle)));
                case "tan": return d(Math.tan(toRad(x, ctx.angle)));
                case "asin": return d(fromRad(Math.asin(x), ctx.angle));
                case "acos": return d(fromRad(Math.acos(x), ctx.angle));
                case "atan": return d(fromRad(Math.atan(x), ctx.angle));
                case "sinh": return d(Math.sinh(x));
                case "cosh": return d(Math.cosh(x));
                case "tanh": return d(Math.tanh(x));
                case "asinh": return d(Math.log(x + Math.sqrt(x * x + 1)));
                case "acosh": return d(Math.log(x + Math.sqrt(x * x - 1)));
                case "atanh": return d(0.5 * Math.log((1 + x) / (1 - x)));
                case "ln":
                    if (x <= 0) throw new EvalException("Logarithm domain");
                    return d(Math.log(x));
                case "log":
                    if (x <= 0) throw new EvalException("Logarithm domain");
                    return d(Math.log10(x));
                case "exp": return d(Math.exp(x));
                case "sqrt":
                    if (x < 0) throw new EvalException("Square root domain");
                    return d(Math.sqrt(x));
                case "cbrt": return d(Math.cbrt(x));
                case "abs": return a.abs(MC);
                case "round": return BigDecimal.valueOf(Math.rint(x));
                case "floor": return BigDecimal.valueOf(Math.floor(x));
                case "ceil": return BigDecimal.valueOf(Math.ceil(x));
                default: throw new EvalException("Unknown function");
            }
        }
    }

    private static BigDecimal d(double v) throws EvalException {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new EvalException("Result is not a number");
        return BigDecimal.valueOf(v);
    }

    private static double toRad(double x, Angle a) {
        if (a == Angle.DEG) return Math.toRadians(x);
        if (a == Angle.GRAD) return x * Math.PI / 200.0;
        return x;
    }

    private static double fromRad(double x, Angle a) {
        if (a == Angle.DEG) return Math.toDegrees(x);
        if (a == Angle.GRAD) return x * 200.0 / Math.PI;
        return x;
    }

    private static BigDecimal power(BigDecimal a, BigDecimal b) throws EvalException {
        BigDecimal stripped = b.stripTrailingZeros();
        if (stripped.scale() <= 0 && stripped.abs().compareTo(BigDecimal.valueOf(9999)) <= 0) {
            int n = stripped.intValue();
            if (n == 0) return BigDecimal.ONE;
            if (n > 0) return a.pow(n, MC);
            if (a.signum() == 0) throw new EvalException("Division by zero");
            return BigDecimal.ONE.divide(a.pow(-n, MC), MC);
        }
        double v = Math.pow(a.doubleValue(), b.doubleValue());
        return d(v);
    }

    private static BigDecimal mod(BigDecimal a, BigDecimal b) throws EvalException {
        if (b.signum() == 0) throw new EvalException("Division by zero");
        BigDecimal q = a.divide(b, 0, RoundingMode.FLOOR);
        BigDecimal r = a.subtract(b.multiply(q), MC);
        if (r.signum() != 0 && r.signum() != b.signum()) r = r.add(b, MC);
        return r;
    }

    private static BigDecimal combinations(BigDecimal n, BigDecimal k) throws EvalException {
        double nd = whole(n), kd = whole(k);
        if (kd < 0 || nd < 0 || kd > nd) throw new EvalException("Combination domain");
        kd = Math.min(kd, nd - kd);
        double r = 1.0;
        for (int i = 1; i <= (int) kd; i++) r = r * (nd - kd + i) / i;
        return d(Math.rint(r));
    }

    private static BigDecimal permutations(BigDecimal n, BigDecimal k) throws EvalException {
        double nd = whole(n), kd = whole(k);
        if (kd < 0 || nd < 0 || kd > nd) throw new EvalException("Permutation domain");
        double r = 1.0;
        for (int i = 0; i < (int) kd; i++) r *= (nd - i);
        return d(Math.rint(r));
    }

    private static double whole(BigDecimal v) throws EvalException {
        if (v.stripTrailingZeros().scale() > 0) throw new EvalException("Whole number needed");
        return v.doubleValue();
    }

    private static final class Parser {
        final String s;
        final Context ctx;
        int i;

        Parser(String s, Context ctx) { this.s = s; this.ctx = ctx; }

        Node parse() throws EvalException {
            Node n = parseAdd();
            skip();
            if (i < s.length()) throw new EvalException("Unexpected " + s.charAt(i));
            return n;
        }

        void skip() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        boolean eat(char c) {
            skip();
            if (i < s.length() && s.charAt(i) == c) { i++; return true; }
            return false;
        }

        boolean eatWord(String w) {
            skip();
            int n = w.length();
            if (i + n <= s.length() && s.regionMatches(true, i, w, 0, n)) {
                int after = i + n;
                if (after >= s.length() || !Character.isLetter(s.charAt(after))) {
                    i = after;
                    return true;
                }
            }
            return false;
        }

        Node parseAdd() throws EvalException {
            Node left = parseMul();
            while (true) {
                skip();
                if (eat('+')) left = new Binary('+', left, parseMul());
                else if (eat('-')) left = new Binary('-', left, parseMul());
                else return left;
            }
        }

        Node parseMul() throws EvalException {
            Node left = parseUnary();
            while (true) {
                skip();
                if (eat('*') || eat('×') || eat('·')) left = new Binary('*', left, parseUnary());
                else if (eat('/') || eat('÷')) left = new Binary('/', left, parseUnary());
                else if (eatWord("mod")) left = new Binary('m', left, parseUnary());
                else if (eatWord("nCr")) left = new Binary('C', left, parseUnary());
                else if (eatWord("nPr")) left = new Binary('P', left, parseUnary());
                else if (startsPrimary()) left = new Binary('*', left, parseUnary());
                else return left;
            }
        }

        boolean startsPrimary() {
            skip();
            if (i >= s.length()) return false;
            char c = s.charAt(i);
            return Character.isDigit(c) || c == '.' || c == '(' || c == '√' || c == '∛' || c == 'π'
                    || Character.isLetter(c);
        }

        Node parseUnary() throws EvalException {
            skip();
            if (eat('-') || eat('−')) return new Unary(true, parseUnary());
            if (eat('+')) return parseUnary();
            return parsePower();
        }

        Node parsePower() throws EvalException {
            Node left = parsePostfix();
            skip();
            if (eat('^')) return new Binary('^', left, parseUnary());
            return left;
        }

        Node parsePostfix() throws EvalException {
            Node n = parsePrimary();
            while (true) {
                skip();
                if (eat('!')) n = new Factorial(n);
                else if (eat('%')) n = new Percent(n);
                else return n;
            }
        }

        Node parsePrimary() throws EvalException {
            skip();
            if (i >= s.length()) throw new EvalException("A value is missing");
            char c = s.charAt(i);
            if (c == '(') {
                i++;
                Node n = parseAdd();
                if (!eat(')')) throw new EvalException("Missing )");
                return n;
            }
            if (c == '√') {
                i++;
                Node a = parsePrimary();
                return new Function("sqrt", a, null);
            }
            if (c == '∛') {
                i++;
                Node a = parsePrimary();
                return new Function("cbrt", a, null);
            }
            if (c == 'π') { i++; return new Constant("pi"); }
            if (Character.isDigit(c) || c == '.') return parseNumber();
            if (Character.isLetter(c)) return parseIdent();
            throw new EvalException("Unexpected " + c);
        }

        Node parseNumber() throws EvalException {
            int start = i;
            boolean dot = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) { i++; continue; }
                if (c == '.' && !dot) { dot = true; i++; continue; }
                break;
            }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int save = i;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                int digs = i;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
                if (i == digs) i = save;
            }
            try {
                return new Num(new BigDecimal(s.substring(start, i)));
            } catch (NumberFormatException ex) {
                throw new EvalException("Bad number");
            }
        }

        Node parseIdent() throws EvalException {
            int start = i;
            while (i < s.length() && (Character.isLetter(s.charAt(i)) || s.charAt(i) == '_')) i++;
            String name = s.substring(start, i).toLowerCase();
            switch (name) {
                case "pi": return new Constant("pi");
                case "e": return new Constant("e");
                case "phi": return new Constant("phi");
                case "ans": return new Constant("ans");
                case "c": return new Constant("c");
                case "g": return new Constant("g");
                default: break;
            }
            if (!eat('(')) throw new EvalException("Unknown value " + name);
            Node a = parseAdd();
            Node b = null;
            if (eat(',')) b = parseAdd();
            if (!eat(')')) throw new EvalException("Missing )");
            return new Function(name, a, b);
        }
    }

    public static BigDecimal eval(String expr, Context ctx) throws EvalException {
        if (expr == null || expr.trim().isEmpty()) return BigDecimal.ZERO;
        return new Parser(expr, ctx).parse().eval(ctx, null);
    }
}


