package com.cokalku.sc100;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Formats numbers for the LCD, in AUTO / FIX / SCI / ENG styles. */
public final class Formatter {
    public enum Mode { AUTO, FIX, SCI, ENG }

    private static final MathContext AUTO_MC = new MathContext(12, RoundingMode.HALF_UP);

    private Formatter() {}

    public static String format(BigDecimal value, Mode mode, int digits, boolean grouping) {
        if (value == null) value = BigDecimal.ZERO;
        if (value.signum() == 0) {
            if (mode == Mode.FIX) return zeroFixed(digits);
            if (mode == Mode.SCI || mode == Mode.ENG) return zeroSci(digits);
            return "0";
        }
        digits = Math.max(1, Math.min(12, digits));
        switch (mode) {
            case FIX: {
                BigDecimal v = value.setScale(digits, RoundingMode.HALF_UP);
                return group(v.toPlainString(), grouping);
            }
            case SCI:
                return scientific(value, digits, false);
            case ENG:
                return scientific(value, digits, true);
            case AUTO:
            default: {
                BigDecimal abs = value.abs();
                if (abs.compareTo(new BigDecimal("1E12")) >= 0
                        || abs.compareTo(new BigDecimal("0.0001")) < 0) {
                    return scientific(value, 10, false);
                }
                BigDecimal v = value.round(AUTO_MC);
                String s = trim(v.toPlainString());
                return group(s, grouping);
            }
        }
    }

    private static String zeroFixed(int digits) {
        if (digits <= 0) return "0";
        StringBuilder b = new StringBuilder("0.");
        for (int i = 0; i < digits; i++) b.append('0');
        return b.toString();
    }

    private static String zeroSci(int digits) {
        StringBuilder b = new StringBuilder("0");
        if (digits > 1) {
            b.append('.');
            for (int i = 1; i < digits; i++) b.append('0');
        }
        return b.append("E0").toString();
    }

    private static String scientific(BigDecimal value, int digits, boolean engineering) {
        boolean neg = value.signum() < 0;
        BigDecimal v = value.abs();
        int exp = v.precision() - v.scale() - 1;
        if (engineering) {
            int rem = exp % 3;
            if (rem < 0) rem += 3;
            exp -= rem;
        }
        BigDecimal mant = v.movePointLeft(exp);
        mant = mant.setScale(Math.max(0, digits - 1), RoundingMode.HALF_UP);
        if (mant.compareTo(BigDecimal.TEN) >= 0) {
            mant = mant.movePointLeft(1).setScale(Math.max(0, digits - 1), RoundingMode.HALF_UP);
            exp++;
            if (engineering && exp % 3 != 0) {
                int rem = exp % 3;
                if (rem < 0) rem += 3;
                exp -= rem;
                mant = v.movePointLeft(exp).setScale(Math.max(0, digits - 1), RoundingMode.HALF_UP);
            }
        }
        String m = trim(mant.toPlainString());
        return (neg ? "-" : "") + m + "E" + exp;
    }

    private static String trim(String s) {
        int e = s.indexOf('E');
        String tail = "";
        if (e >= 0) { tail = s.substring(e); s = s.substring(0, e); }
        if (s.indexOf('.') >= 0) {
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') end--;
            if (end > 0 && s.charAt(end - 1) == '.') end--;
            s = s.substring(0, end);
        }
        if (s.isEmpty() || s.equals("-")) s = "0";
        return s + tail;
    }

    private static String group(String s, boolean grouping) {
        if (!grouping) return s;
        int e = s.indexOf('E');
        String tail = "";
        if (e >= 0) { tail = s.substring(e); s = s.substring(0, e); }
        boolean neg = s.startsWith("-");
        if (neg) s = s.substring(1);
        int dot = s.indexOf('.');
        String ip = dot >= 0 ? s.substring(0, dot) : s;
        String fp = dot >= 0 ? s.substring(dot) : "";
        if (ip.length() > 4) {
            StringBuilder b = new StringBuilder();
            int first = ip.length() % 3;
            if (first == 0) first = 3;
            b.append(ip, 0, first);
            for (int i = first; i < ip.length(); i += 3) {
                b.append(',').append(ip, i, i + 3);
            }
            ip = b.toString();
        }
        return (neg ? "-" : "") + ip + fp + tail;
    }

    public static String superscript(String text) {
        String sups = "⁰¹²³⁴⁵⁶⁷⁸⁹";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '-') b.append('⁻');
            else if (c == '+') b.append('⁺');
            else if (c >= '0' && c <= '9') b.append(sups.charAt(c - '0'));
            else b.append(c);
        }
        return b.toString();
    }

    /** Small wrapper that keeps a formatted number split for the LCD renderer. */
    public static final class Display {
        public final String mantissa;
        public final String exponent;
        public Display(String mantissa, String exponent) {
            this.mantissa = mantissa;
            this.exponent = exponent;
        }
    }

    public static Display split(String formatted) {
        int e = formatted.indexOf('E');
        if (e < 0) return new Display(formatted, null);
        return new Display(formatted.substring(0, e), formatted.substring(e + 1));
    }
}



