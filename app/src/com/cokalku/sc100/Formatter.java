package com.cokalku.sc100;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Formats numbers for the LCD, in AUTO / FIX / SCI / ENG styles. */
public final class Formatter {
    public enum Mode { AUTO, FIX, SCI, ENG }

    private static final MathContext AUTO_MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal AUTO_SCI_HIGH = new BigDecimal("1E12");
    private static final BigDecimal AUTO_SCI_LOW = new BigDecimal("1E-9");

    private Formatter() {}

    /** Keeps the original dot-decimal API for callers that do not expose a preference. */
    public static String format(BigDecimal value, Mode mode, int digits, boolean grouping) {
        return format(value, mode, digits, grouping, false);
    }

    /**
     * @param decimalComma true for 1.234,56 (Indonesian style), false for 1,234.56.
     */
    public static String format(BigDecimal value, Mode mode, int digits, boolean grouping,
                                boolean decimalComma) {
        if (value == null) value = BigDecimal.ZERO;
        if (value.signum() == 0) {
            if (mode == Mode.FIX) return localize(zeroFixed(digits), grouping, decimalComma);
            if (mode == Mode.SCI || mode == Mode.ENG) return localize(zeroSci(digits), false, decimalComma);
            return "0";
        }
        digits = Math.max(1, Math.min(12, digits));
        switch (mode) {
            case FIX: {
                BigDecimal v = value.setScale(digits, RoundingMode.HALF_UP);
                return localize(v.toPlainString(), grouping, decimalComma);
            }
            case SCI:
                return localize(scientific(value, digits, false), false, decimalComma);
            case ENG:
                return localize(scientific(value, digits, true), false, decimalComma);
            case AUTO:
            default: {
                BigDecimal abs = value.abs();
                if (abs.compareTo(AUTO_SCI_HIGH) >= 0 || abs.compareTo(AUTO_SCI_LOW) < 0) {
                    return localize(scientific(value, 10, false), false, decimalComma);
                }
                BigDecimal v = value.round(AUTO_MC);
                return localize(trim(v.toPlainString()), grouping, decimalComma);
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

    /**
     * Turns a canonical dot-decimal string into the requested locale, including grouping.
     * Grouping is only applied to the integer part of a plain number.
     */
    private static String localize(String canonical, boolean grouping, boolean decimalComma) {
        String s = canonical;
        String tail = "";
        int e = s.indexOf('E');
        if (e >= 0) {
            tail = s.substring(e);
            s = s.substring(0, e);
        }
        boolean neg = s.startsWith("-");
        if (neg) s = s.substring(1);
        int dot = s.indexOf('.');
        String ip = dot >= 0 ? s.substring(0, dot) : s;
        String fp = dot >= 0 ? s.substring(dot + 1) : "";
        String groupSep = decimalComma ? "." : ",";
        String decSep = decimalComma ? "," : ".";
        if (grouping && ip.length() > 4) {
            StringBuilder b = new StringBuilder();
            int first = ip.length() % 3;
            if (first == 0) first = 3;
            b.append(ip, 0, first);
            for (int i = first; i < ip.length(); i += 3) {
                b.append(groupSep).append(ip, i, i + 3);
            }
            ip = b.toString();
        }
        return (neg ? "-" : "") + ip + (fp.isEmpty() ? "" : decSep + fp) + tail;
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
