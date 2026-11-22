package com.cokalku.sc100;

/** Colour palette for the calculator shell. */
public final class Theme {
    public final String name;
    public final int bodyTop, bodyMid, bodyBottom;
    public final int faceTop, faceBottom, faceInnerTop, faceInnerBottom;
    public final int keyTop, keyMid, keyBottom, keyEdge, keyText, keyStroke;
    public final int redTop, redBottom, yellowTop, yellowBottom;
    public final int lcdTop, lcdMid, lcdBottom, lcdInk, lcdDim, lcdBorder;
    public final int accent, muted, footer;
    public final boolean light;

    private Theme(String name, boolean light,
                  int bodyTop, int bodyMid, int bodyBottom,
                  int faceTop, int faceBottom, int faceInnerTop, int faceInnerBottom,
                  int keyTop, int keyMid, int keyBottom, int keyEdge, int keyText, int keyStroke,
                  int redTop, int redBottom, int yellowTop, int yellowBottom,
                  int lcdTop, int lcdMid, int lcdBottom, int lcdInk, int lcdDim, int lcdBorder,
                  int accent, int muted, int footer) {
        this.name = name; this.light = light;
        this.bodyTop = bodyTop; this.bodyMid = bodyMid; this.bodyBottom = bodyBottom;
        this.faceTop = faceTop; this.faceBottom = faceBottom;
        this.faceInnerTop = faceInnerTop; this.faceInnerBottom = faceInnerBottom;
        this.keyTop = keyTop; this.keyMid = keyMid; this.keyBottom = keyBottom;
        this.keyEdge = keyEdge; this.keyText = keyText; this.keyStroke = keyStroke;
        this.redTop = redTop; this.redBottom = redBottom;
        this.yellowTop = yellowTop; this.yellowBottom = yellowBottom;
        this.lcdTop = lcdTop; this.lcdMid = lcdMid; this.lcdBottom = lcdBottom;
        this.lcdInk = lcdInk; this.lcdDim = lcdDim; this.lcdBorder = lcdBorder;
        this.accent = accent; this.muted = muted; this.footer = footer;
    }

    public static Theme graphite() {
        return new Theme("GRAPHITE", false,
                0xFF3C3C41, 0xFF2C2C31, 0xFF1C1C1F,
                0xFF4A4A50, 0xFF303035, 0xFF45454B, 0xFF323237,
                0xFF67676D, 0xFF57575D, 0xFF444449, 0xFF18181B,
                0xFFF7F7F9, 0x22FFFFFF,
                0xFFE02A2A, 0xFF8E0606, 0xFFF2CC42, 0xFFB78810,
                0xFFB3CDB1, 0xFF95B197, 0xFF7E9C81,
                0xFF111A12, 0xFF354736, 0xFFE7F4E2,
                0xFFD9B64B, 0xFFA7A7AF, 0xFF85858E);
    }

    public static Theme silver() {
        return new Theme("SILVER", true,
                0xFFE4E2DD, 0xFFC9C7C1, 0xFFAAA8A2,
                0xFFF0EEE8, 0xFFC8C6C0, 0xFFE6E4DE, 0xFFBDBBB5,
                0xFFF3F1EC, 0xFFDAD8D2, 0xFFC1BFB9, 0xFF8F8D87,
                0xFF2A2A2C, 0x33000000,
                0xFFC02121, 0xFF8A0A0A, 0xFFE9BD2D, 0xFFA87C0C,
                0xFFA9C4A7, 0xFF8FAF92, 0xFF78987B,
                0xFF111A12, 0xFF354736, 0xFFF4FAF0,
                0xFF9A6A00, 0xFF6E6C66, 0xFF77756F);
    }

    public static Theme midnight() {
        return new Theme("MIDNIGHT", false,
                0xFF202A38, 0xFF141D28, 0xFF0B1119,
                0xFF273344, 0xFF17212D, 0xFF202B3A, 0xFF141D28,
                0xFF3A485C, 0xFF2E3A4B, 0xFF242E3C, 0xFF0C1118,
                0xFFF2F5FA, 0x22FFFFFF,
                0xFFD42C2C, 0xFF7D0505, 0xFFEDC23E, 0xFFA87C0C,
                0xFFA8C7C4, 0xFF8DAFAC, 0xFF739795,
                0xFF0F1A19, 0xFF334A48, 0xFFE8F7F4,
                0xFF8FD3C7, 0xFF93A3B8, 0xFF6E7E93);
    }

    public static Theme byIndex(int i) {
        if (i == 1) return silver();
        if (i == 2) return midnight();
        return graphite();
    }
}

