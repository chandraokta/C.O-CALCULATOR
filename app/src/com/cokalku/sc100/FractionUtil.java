package com.cokalku.sc100;

import java.math.BigDecimal;

/** Converts a decimal result to a small, readable fraction when it is exact enough. */
public final class FractionUtil {
    private static final long MAX_DEN = 100000;
    private static final double TOLERANCE = 1e-9;

    private FractionUtil() {}

    /**
     * Returns a mixed fraction such as "2 1/2", "1/3" or "-1/2".
     * Returns null for integers, irrational-looking values and very large denominators.
     */
    public static String toMixed(BigDecimal value) {
        if (value == null || value.signum() == 0) return null;
        double x = Math.abs(value.doubleValue());
        if (!Double.isFinite(x) || x > 1e12) return null;

        long p0 = 0, q0 = 1, p1 = 1, q1 = 0;
        double rest = x;
        for (int i = 0; i < 40; i++) {
            long a = (long) Math.floor(rest);
            if (a < 0) break;
            long p2, q2;
            try {
                p2 = Math.addExact(Math.multiplyExact(a, p1), p0);
                q2 = Math.addExact(Math.multiplyExact(a, q1), q0);
            } catch (ArithmeticException e) {
                break;
            }
            if (q2 <= 0 || q2 > MAX_DEN) break;
            p0 = p1; q0 = q1; p1 = p2; q1 = q2;
            double frac = rest - a;
            if (frac < 1e-13) break;
            rest = 1.0 / frac;
            if (!Double.isFinite(rest) || rest > 1e15) break;
        }
        if (q1 <= 1 || q1 > MAX_DEN) return null;
        double approx = (double) p1 / (double) q1;
        if (Math.abs(x - approx) > TOLERANCE) return null;

        boolean negative = value.signum() < 0;
        long whole = p1 / q1;
        long rem = p1 % q1;
        String sign = negative ? "-" : "";
        if (whole == 0) return sign + rem + "/" + q1;
        if (rem == 0) return sign + whole;
        return sign + whole + " " + rem + "/" + q1;
    }
}
