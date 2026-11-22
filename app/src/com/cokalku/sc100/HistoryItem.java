package com.cokalku.sc100;

/** A single result-history entry. */
public final class HistoryItem {
    public final String expression;
    public final String value;   // formatted, as shown
    public final String raw;     // unformatted decimal, for re-use
    public final long time;

    public HistoryItem(String expression, String value, String raw, long time) {
        this.expression = expression;
        this.value = value;
        this.raw = raw;
        this.time = time;
    }
}

