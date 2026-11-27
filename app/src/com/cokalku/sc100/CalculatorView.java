package com.cokalku.sc100;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The whole calculator: a custom-drawn instrument with physical keys and an LCD. */
public class CalculatorView extends View {
    private static final float PW = 412f, PH = 915f;
    private static final float LW = 915f, LH = 412f;

    public enum Mode { CALC, HISTORY, MEMORY, CONVERTER, CONSTANTS, SETTINGS, RADIX }

    private static final class Key {
        float x, y, w, h;
        String label, legend, action, alt;
        int kind; // 0 normal, 1 yellow, 2 red, 3 dark
        float size;
        boolean bold;
        boolean blue;
        Key(float x, float y, float w, float h, String label, String legend,
            String action, String alt, int kind, float size, boolean bold, boolean blue) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.legend = legend;
            this.action = action; this.alt = alt;
            this.kind = kind; this.size = size; this.bold = bold; this.blue = blue;
        }
    }

    private final Context ctx;
    private final Prefs prefs;
    private Theme theme;
    private final Typeface sans, medium, boldFace, lightFace, dseg;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SoundPool soundPool;
    private int clickSound;
    private volatile boolean clickReady;
    private Vibrator vibrator;
    private boolean resultRecorded;
    private String displayOverride;

    private Bitmap chrome;
    private int W, H;
    private float SX, SY, S;
    private boolean landscape;

    private final List<Key> keys = new ArrayList<>();
    private final RectF lcdModule = new RectF();
    private final RectF lcdScreen = new RectF();
    private Key pressed;
    private Key flashKey;
    private long flashStart;
    private boolean longFired;
    private Runnable longPressRunnable;
    private float downX, downY;

    private String expr = "";
    private String lastExpr = "";
    private BigDecimal lastResult = BigDecimal.ZERO;
    private boolean justEvaluated;
    private boolean shift, hyp, errorState;
    private String errorMessage = "";
    private Mode mode = Mode.CALC;

    private final List<HistoryItem> history;
    private int historyScroll;
    private int memoryAction; // 0 recall, 1 store
    private double[] memories;
    private int convCat;
    private int convFrom;
    private int convTo;
    private int constGroup;
    private int constScroll;
    private int dragStartScroll;
    private float dragStartY;
    private boolean dragging;
    private boolean dragMoved;

    private RectF panel = new RectF();
    private RectF closeRect = new RectF();
    private RectF footerRect = new RectF();
    private int overlayRows;

    private static Typeface loadSegFont(Context c) {
        final String path = "fonts/DSEG7Classic-BoldItalic.ttf";
        try {
            java.io.File out = new java.io.File(c.getCacheDir(), "kalku-dseg.ttf");
            try (java.io.InputStream in = c.getResources().openRawResource(R.raw.dseg);
                 java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            }
            return Typeface.createFromFile(out);
        } catch (Exception e) {
            android.util.Log.e("Kalku", "DSEG raw load failed", e);
        }
        try {
            return Typeface.createFromAsset(c.getAssets(), path);
        } catch (RuntimeException e) {
            android.util.Log.w("Kalku", "createFromAsset failed: " + e.getMessage());
        }
        return Typeface.create("monospace", Typeface.BOLD_ITALIC);
    }

    public CalculatorView(Context c) {
        super(c);
        ctx = c;
        prefs = new Prefs(c);
        theme = Theme.byIndex(prefs.theme());
        history = prefs.history();
        memories = prefs.memories();
        sans = Typeface.create("sans-serif", Typeface.NORMAL);
        medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        boldFace = Typeface.create("sans-serif", Typeface.BOLD);
        lightFace = Typeface.create("sans-serif-light", Typeface.NORMAL);
        dseg = loadSegFont(c);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setHapticFeedbackEnabled(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vibrator = vm.getDefaultVibrator();
                if (vibrator == null) {
                    int[] ids = vm.getVibratorIds();
                    if (ids.length > 0) vibrator = vm.getVibrator(ids[0]);
                }
            } else {
                vibrator = null;
            }
        } else {
            vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        }
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build();
            clickSound = soundPool.load(c, R.raw.key_click, 1);
            soundPool.setOnLoadCompleteListener((sp, id, status) -> {
                clickReady = status == 0;
                android.util.Log.d("COKALKU", "key click loaded: " + status);
            });
        } catch (Exception e) {
            soundPool = null;
        }
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public Mode mode() { return mode; }
    public boolean back() {
        if (mode != Mode.CALC) { mode = Mode.CALC; invalidate(); return true; }
        if (shift || hyp) { shift = false; hyp = false; invalidate(); return true; }
        return false;
    }

    public void setKeepAwake(boolean on) {
        setKeepScreenOn(on);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        W = w; H = h;
        landscape = w > h * 1.2f;
        SX = landscape ? w / LW : w / PW;
        SY = landscape ? h / LH : h / PH;
        S = landscape ? Math.min(SY, SX * 1.18f) : Math.min(SX, SY * 1.12f);
        buildKeys();
        buildChrome();
    }

    private void buildKeys() {
        keys.clear();
        if (landscape) buildLandscapeKeys();
        else buildPortraitKeys();
    }

    private float px(float x) { return x * SX; }
    private float py(float y) { return y * SY; }
    private float ps(float v) { return v * S; }

    private void addKey(float x, float y, float w, float h, String label, String legend,
                        String action, String alt, int kind, float size, boolean bold, boolean blue) {
        keys.add(new Key(x, y, w, h, label, legend, action, alt, kind, size, bold, blue));
    }

    private void buildPortraitKeys() {
        float ux = 18, uy = 226, uw = 376, uh = 26, ug = 7;
        float ucw = (uw - 5 * ug) / 6;
        String[][] utils = {
                {"MENU", "menu"}, {"DRG▸", "drg"}, {"TAB", "tab"},
                {"RCL", "recall"}, {"STO", "store"}, {"M−", "mminus"}
        };
        for (int i = 0; i < utils.length; i++) {
            addKey(ux + i * (ucw + ug), uy, ucw, uh, utils[i][0], null,
                    utils[i][1], null, 3, 8.2f, false, false);
        }

        float fx = 18, fy = 266, fw = 376, fh = 58, fg = 8;
        float fcw = (fw - 5 * fg) / 6;
        Object[][] row1 = {
                {"SHIFT", null, "shift", null, 1, 12.5f, true, false},
                {"DRG", null, "drg", null, 0, 15f, false, false},
                {"FSE", null, "fse", null, 0, 15f, false, false},
                {"MR", null, "mr", null, 0, 15f, false, true},
                {"MS", null, "ms", null, 0, 15f, false, true},
                {"M+", null, "mplus", null, 0, 15f, false, true}
        };
        for (int i = 0; i < row1.length; i++) {
            Object[] k = row1[i];
            addKey(fx + i * (fcw + fg), fy, fcw, fh, (String) k[0], (String) k[1],
                    (String) k[2], (String) k[3], (Integer) k[4], (Float) k[5], (Boolean) k[6], (Boolean) k[7]);
        }

        Object[][] row2 = {
                {"hyp", null, "hyp", null, 0, 16f, false, false},
                {"sin", "sin⁻¹", "sin", "asin", 0, 19f, false, false},
                {"cos", "cos⁻¹", "cos", "acos", 0, 19f, false, false},
                {"tan", "tan⁻¹", "tan", "atan", 0, 19f, false, false},
                {"ln", "eˣ", "ln", "expf", 0, 19f, false, false},
                {"log", "10ˣ", "log", "pow10", 0, 19f, false, false}
        };
        for (int i = 0; i < row2.length; i++) {
            Object[] k = row2[i];
            addKey(fx + i * (fcw + fg), fy + fh + 14, fcw, fh, (String) k[0], (String) k[1],
                    (String) k[2], (String) k[3], (Integer) k[4], (Float) k[5], (Boolean) k[6], (Boolean) k[7]);
        }

        Object[][] row3 = {
                {"yˣ", "ˣ√y", "pow", "nroot", 0, 19f, false, false},
                {"√", "∛", "sqrt", "cbrt", 0, 22f, false, false},
                {"x²", "x³", "sq", "cube", 0, 18f, false, false},
                {"%", "1/x", "percent", "inv", 0, 20f, false, false},
                {"(", null, "lparen", null, 0, 23f, false, false},
                {")", null, "rparen", null, 0, 23f, false, false}
        };
        for (int i = 0; i < row3.length; i++) {
            Object[] k = row3[i];
            addKey(fx + i * (fcw + fg), fy + 2 * (fh + 14), fcw, fh, (String) k[0], (String) k[1],
                    (String) k[2], (String) k[3], (Integer) k[4], (Float) k[5], (Boolean) k[6], (Boolean) k[7]);
        }

        float nx = 18, ny = 496, nw = 376, nh = 80, ng = 9;
        float ncw = (nw - 4 * ng) / 5;
        String[][] numRows = {
                {"7", "n!", "d7", "fact"}, {"8", "nCr", "d8", "ncr"}, {"9", "nPr", "d9", "npr"},
                {"DEL", "DEC", "del", "dec"}, {"AC", "HEX", "ac", "hex"},
                {"4", "π", "d4", "pi"}, {"5", "e", "d5", "e"}, {"6", "OCT", "d6", "oct"},
                {"×", "BIN", "mul", "bin"}, {"÷", null, "div", null},
                {"1", "CONV", "d1", "conv"}, {"2", "CNST", "d2", "cnst"}, {"3", "MOD", "d3", "mod"},
                {"+", null, "add", null}, {"−", null, "sub", null},
                {"0", null, "d0", null}, {"±", null, "sign", null}, {".", null, "dot", null},
                {"EXP", null, "exp", null}, {"=", "ANS", "eq", "ans"}
        };
        for (int i = 0; i < numRows.length; i++) {
            int row = i / 5, col = i % 5;
            String label = numRows[i][0];
            float fs;
            boolean bold = false;
            if (label.equals("DEL") || label.equals("AC")) { fs = 20; bold = true; }
            else if (label.equals("EXP")) { fs = 16; bold = true; }
            else if (label.equals("=")) { fs = 34; }
            else if (label.equals("×") || label.equals("÷") || label.equals("+") || label.equals("−") || label.equals("±")) { fs = 31; }
            else { fs = 31; }
            int kind = (label.equals("DEL") || label.equals("AC")) ? 2 : 0;
            addKey(nx + col * (ncw + ng), ny + row * (nh + 14), ncw, nh,
                    label, numRows[i][1], numRows[i][2], numRows[i][3], kind, fs, bold, false);
        }
    }

    private void buildLandscapeKeys() {
        float kx = 44, ky = 164, leftW = 438, rightX = 506, rightW = 365, kh = 52, gap = 6;
        float lcw = (leftW - 5 * gap) / 6;
        float rcw = (rightW - 4 * gap) / 5;

        // Left: the normal scientific block from the portrait keypad.
        String[][] left = {
                {"MENU", null, "menu", null}, {"DRG▸", null, "drg", null}, {"TAB", null, "tab", null},
                {"RCL", null, "recall", null}, {"STO", null, "store", null}, {"M−", null, "mminus", null},
                {"SHIFT", null, "shift", null}, {"DRG", null, "drg", null}, {"FSE", null, "fse", null},
                {"MR", null, "mr", null}, {"MS", null, "ms", null}, {"M+", null, "mplus", null},
                {"hyp", null, "hyp", null}, {"sin", "sin⁻¹", "sin", "asin"}, {"cos", "cos⁻¹", "cos", "acos"},
                {"tan", "tan⁻¹", "tan", "atan"}, {"ln", "eˣ", "ln", "expf"}, {"log", "10ˣ", "log", "pow10"},
                {"yˣ", "ˣ√y", "pow", "nroot"}, {"√", "∛", "sqrt", "cbrt"}, {"x²", "x³", "sq", "cube"},
                {"%", "1/x", "percent", "inv"}, {"(", null, "lparen", null}, {")", null, "rparen", null}
        };

        // Right: the familiar 5-column number pad, exactly as in portrait.
        String[][] right = {
                {"7", "n!", "d7", "fact"}, {"8", "nCr", "d8", "ncr"}, {"9", "nPr", "d9", "npr"},
                {"DEL", "DEC", "del", "dec"}, {"AC", "HEX", "ac", "hex"},
                {"4", "π", "d4", "pi"}, {"5", "e", "d5", "e"}, {"6", "OCT", "d6", "oct"},
                {"×", "BIN", "mul", "bin"}, {"÷", null, "div", null},
                {"1", "CONV", "d1", "conv"}, {"2", "CNST", "d2", "cnst"}, {"3", "MOD", "d3", "mod"},
                {"+", null, "add", null}, {"−", null, "sub", null},
                {"0", null, "d0", null}, {"±", null, "sign", null}, {".", null, "dot", null},
                {"EXP", null, "exp", null}, {"=", "ANS", "eq", "ans"}
        };

        for (int i = 0; i < left.length; i++) {
            int row = i / 6, col = i % 6;
            addLandscapeKey(kx + col * (lcw + gap), ky + row * (kh + gap), lcw, kh, left[i], row == 0);
        }
        for (int i = 0; i < right.length; i++) {
            int row = i / 5, col = i % 5;
            addLandscapeKey(rightX + col * (rcw + gap), ky + row * (kh + gap), rcw, kh, right[i], false);
        }
    }

    private void addLandscapeKey(float x, float y, float w, float h, String[] k, boolean utility) {
        String label = k[0];
        int kind = utility ? 3 : (label.equals("SHIFT") ? 1
                : (label.equals("DEL") || label.equals("AC")) ? 2 : 0);
        boolean bold = false;
        float fs;
        if (utility) {
            fs = 9.0f;
        } else if (kind == 2) {
            fs = 15f;
            bold = true;
        } else if (label.equals("SHIFT")) {
            fs = 10.5f;
            bold = true;
        } else if (label.equals("×") || label.equals("÷") || label.equals("+")
                || label.equals("−") || label.equals("±") || label.equals("=")) {
            fs = 22f;
        } else if (label.equals("EXP")) {
            fs = 11f;
            bold = true;
        } else {
            fs = 19f;
        }
        boolean blue = label.equals("MR") || label.equals("MS") || label.equals("M+");
        addKey(x, y, w, h, label, k[1], k[2], k[3], kind, fs, bold, blue);
    }

    private void buildChrome() {
        if (W <= 0 || H <= 0) return;
        chrome = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(chrome);
        c.drawColor(0xFF0B0B0D);
        paint.reset();
        paint.setAntiAlias(true);
        if (landscape) drawLandscapeChrome(c);
        else drawPortraitChrome(c);
    }

    private void drawPortraitChrome(Canvas c) {
        RectF body = new RectF(0, 0, W, H);
        fillRound(c, body, ps(36), theme.bodyTop, theme.bodyMid, theme.bodyBottom, true);
        strokeRound(c, body, ps(36), 0xFF57575E, ps(1));
        RectF face = new RectF(px(9), py(9), px(403), py(906));
        fillRound(c, face, ps(29), theme.faceTop, theme.faceBottom, null, true);
        strokeRound(c, face, ps(29), 0xFF101012, ps(1.4f));
        RectF inner = new RectF(px(12.5f), py(12.5f), px(399.5f), py(902.5f));
        fillRound(c, inner, ps(26), theme.faceInnerTop, theme.faceInnerBottom, null, false);
        strokeRound(c, inner, ps(26), 0x14FFFFFF, ps(0.7f));
        texture(c, inner, ps(26));

        // bevel lines
        RectF topBevel = new RectF(px(10.5f), py(10.5f), px(401.5f), py(12.7f));
        fillH(c, topBevel, 0x00FFFFFF, 0x42FFFFFF, 0x00FFFFFF);
        RectF botBevel = new RectF(px(10.5f), py(902.3f), px(401.5f), py(904.5f));
        fillH(c, botBevel, 0x00000000, 0x66000000, 0x00000000);

        for (float[] s : new float[][]{{24, 26}, {388, 26}, {24, 889}, {388, 889}}) {
            drawScrew(c, px(s[0]), py(s[1]), ps(4));
        }

        drawBrand(c, px(44), py(21), px(344), px(96));
        drawLcdPortrait(c);
        drawFooter(c);

        for (Key k : keys) drawKey(c, k);
    }

    private void drawLandscapeChrome(Canvas c) {
        RectF body = new RectF(0, 0, W, H);
        fillRound(c, body, ps(28), theme.bodyTop, theme.bodyMid, theme.bodyBottom, true);
        strokeRound(c, body, ps(28), 0xFF57575E, ps(1));
        RectF face = new RectF(px(8), py(8), px(907), py(404));
        fillRound(c, face, ps(23), theme.faceTop, theme.faceBottom, null, true);
        strokeRound(c, face, ps(23), 0xFF101012, ps(1.3f));
        texture(c, face, ps(23));

        for (float[] s : new float[][]{{24, 24}, {891, 24}, {24, 388}, {891, 388}}) {
            drawScrew(c, px(s[0]), py(s[1]), ps(3.6f));
        }
        drawBrand(c, px(44), py(18), px(827), px(92));
        drawLcdLandscape(c);

        for (Key k : keys) drawKey(c, k);
    }

    private void drawBrand(Canvas c, float x, float y, float w, float h) {
        spacedText(c, "C.O CALCULATOR", x, y + ps(8), ps(10.0f), theme.light ? 0xFF3A3A3E : 0xFFC9C9D1, boldFace, Paint.Align.LEFT, 0.12f);
        spacedText(c, "SCIENTIFIC INSTRUMENT  SC-100", x, y + ps(20), ps(5.6f), theme.muted, medium, Paint.Align.LEFT, 0.06f);
        spacedText(c, "32-DIGIT ENGINE", x + w, y + ps(12), ps(5f), theme.footer, medium, Paint.Align.RIGHT, 0.05f);
    }

    private void drawFooter(Canvas c) {
        text(c, "C.O CALCULATOR · PRECISION INSTRUMENTS", px(18), py(883), ps(5.0f), theme.footer, medium, Paint.Align.LEFT);
        text(c, "SC–100  ·  12 DIGIT", px(394), py(883), ps(5.2f), theme.footer, medium, Paint.Align.RIGHT);
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF101012);
        for (int i = 0; i < 7; i++) {
            float x = px(176 + i * 9);
            c.drawRoundRect(new RectF(x, py(889), x + ps(4), py(889) + ps(4)), ps(2), ps(2), p);
        }
    }

    private void drawLcdPortrait(Canvas c) {
        lcdModule.set(px(20), py(54), px(392), py(212));
        lcdScreen.set(px(30), py(64), px(382), py(202));
        drawLcd(c, lcdModule, lcdScreen, false);
    }

    private void drawLcdLandscape(Canvas c) {
        lcdModule.set(px(44), py(48), px(871), py(146));
        lcdScreen.set(px(52), py(56), px(863), py(138));
        drawLcd(c, lcdModule, lcdScreen, true);
    }

    private void drawLcd(Canvas c, RectF module, RectF screen, boolean land) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setShadowLayer(ps(10), 0, ps(6), 0xB0000000);
        p.setColor(0xFF050506);
        c.drawRoundRect(new RectF(module.left + ps(2), module.top + ps(6), module.right - ps(2), module.bottom), ps(18), ps(18), p);
        p.clearShadowLayer();

        shaderRect(c, module, ps(18), 0xFF26262A, 0xFF0B0B0D);
        strokeRound(c, module, ps(18), 0xFF74747C, ps(0.8f));
        RectF rim = new RectF(module.left + ps(3), module.top + ps(3), module.right - ps(3), module.bottom - ps(3));
        strokeRound(c, rim, ps(15), 0xAA000000, ps(1.2f));
        RectF well = new RectF(module.left + ps(8), module.top + ps(8), module.right - ps(8), module.bottom - ps(8));
        shaderRect(c, well, ps(10), 0xFF0C0E0D, 0xFF1B1F1C);
        strokeRound(c, well, ps(10), 0xFF000000, ps(1.2f));

        shaderRect(c, screen, ps(8), theme.lcdTop, theme.lcdMid, theme.lcdBottom);
        texture(c, screen, ps(8));
        strokeRound(c, screen, ps(8), theme.lcdBorder, ps(0.6f));

        // glass: top band and a soft specular sweep
        RectF glass = new RectF(screen.left + ps(1), screen.top + ps(1), screen.right - ps(1), screen.top + screen.height() * 0.52f);
        LinearGradient lg = new LinearGradient(0, glass.top, 0, glass.bottom,
                0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRoundRect(glass, ps(7), ps(7), p);
        p.setShader(null);

        // scanlines
        p.setColor(0x08000000);
        p.setStrokeWidth(ps(0.6f));
        for (float y = screen.top + ps(3); y < screen.bottom; y += ps(4)) {
            c.drawLine(screen.left + ps(2), y, screen.right - ps(2), y, p);
        }
    }

    private void drawKey(Canvas c, Key k) {
        float x = px(k.x), y = py(k.y), w = px(k.w), h = py(k.h);
        if (w <= 1 || h <= 1) return;
        float r = Math.min(ps(11), h * 0.22f);
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);

        // cast shadow and extruded edge
        p.setShadowLayer(ps(5), 0, ps(4), 0x90000000);
        p.setColor(0xFF08080A);
        c.drawRoundRect(new RectF(x + ps(1), y + ps(4), x + w - ps(1), y + h + ps(4)), r, r, p);
        p.clearShadowLayer();
        p.setColor(theme.keyEdge);
        c.drawRoundRect(new RectF(x, y + ps(3), x + w, y + h + ps(3)), r, r, p);

        int top, bottom, txt;
        if (k.kind == 1) { top = theme.yellowTop; bottom = theme.yellowBottom; txt = 0xFF1C1604; }
        else if (k.kind == 2) { top = theme.redTop; bottom = theme.redBottom; txt = Color.WHITE; }
        else if (k.kind == 3) { top = blend(theme.keyTop, 0xFF000000, 0.35f); bottom = blend(theme.keyBottom, 0xFF000000, 0.25f); txt = theme.keyText; }
        else { top = theme.keyTop; bottom = theme.keyBottom; txt = theme.keyText; }

        LinearGradient cap = new LinearGradient(0, y, 0, y + h, top, bottom, Shader.TileMode.CLAMP);
        p.setShader(cap);
        c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
        p.setShader(null);

        // top sheen
        RectF sheen = new RectF(x + ps(1.2f), y + ps(1.2f), x + w - ps(1.2f), y + h * 0.45f);
        LinearGradient sg = new LinearGradient(0, sheen.top, 0, sheen.bottom,
                0x26FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        p.setShader(sg);
        c.drawRoundRect(sheen, r * 0.9f, r * 0.9f, p);
        p.setShader(null);

        // rim
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(ps(0.8f));
        p.setColor(k.kind == 2 ? 0x40FF7A7A : (k.kind == 1 ? 0x38FFE37A : theme.keyStroke));
        c.drawRoundRect(new RectF(x, y, x + w, y + h), r, r, p);
        p.setStyle(Paint.Style.FILL);

        if (k.legend != null && !k.legend.isEmpty()) {
            float lSize = 12.0f;
            text(c, k.legend, x + w / 2f, y + ps(11), ps(lSize), 0xFFD9B64B, boldFace, Paint.Align.CENTER);
        }
        Paint.Align al = Paint.Align.CENTER;
        float size = ps(k.size);
        Typeface tf = k.bold ? boldFace : (isNumber(k.label) || isOperator(k.label) ? lightFace : sans);
        if (k.label.equals("×") || k.label.equals("÷") || k.label.equals("+") || k.label.equals("−")) tf = lightFace;
        int col = k.kind == 1 ? 0xFF1C1604 : (k.kind == 2 ? Color.WHITE : (k.blue ? 0xFFA0A0FF : txt));
        float cy = y + h * (k.legend != null && !k.legend.isEmpty() ? 0.66f : 0.52f);
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(tf);
        textPaint.setTextSize(size);
        textPaint.setColor(col);
        textPaint.setTextAlign(al);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        c.drawText(k.label, x + w / 2f, baseline, textPaint);
    }

    private boolean isNumber(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        return Character.isDigit(c) || c == '.';
    }
    private boolean isOperator(String s) {
        return s.equals("×") || s.equals("÷") || s.equals("+") || s.equals("−") || s.equals("=");
    }

    private void texture(Canvas c, RectF r, float radius) {
        Bitmap noise = Noise.get();
        Paint p = paint;
        BitmapShader sh = new BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setScale(ps(0.9f), ps(0.9f));
        sh.setLocalMatrix(m);
        p.setShader(sh);
        p.setAlpha(theme.light ? 14 : 22);
        c.drawRoundRect(r, radius, radius, p);
        p.setAlpha(255);
        p.setShader(null);
    }

    private void drawScrew(Canvas c, float x, float y, float r) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0C0C0E);
        c.drawCircle(x, y, r, p);
        p.setColor(0xFF3A3A40);
        c.drawCircle(x, y, r * 0.79f, p);
        p.setColor(0xFF1B1B1F);
        c.drawCircle(x, y, r * 0.55f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, ps(0.4f)));
        p.setColor(0xFF66666E);
        c.drawCircle(x, y, r * 0.55f, p);
        c.drawLine(x - r * 0.36f, y - r * 0.36f, x + r * 0.36f, y + r * 0.36f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void fillRound(Canvas c, RectF r, float radius, int top, int bottom, Integer mid, boolean shadow) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        if (shadow) {
            p.setShadowLayer(ps(14), 0, ps(10), 0x90000000);
            p.setColor(top);
            c.drawRoundRect(r, radius, radius, p);
            p.clearShadowLayer();
        }
        LinearGradient lg = mid == null
                ? new LinearGradient(0, r.top, 0, r.bottom, top, bottom, Shader.TileMode.CLAMP)
                : new LinearGradient(0, r.top, 0, r.bottom, new int[]{top, mid, bottom}, null, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(null);
    }

    private void shaderRect(Canvas c, RectF r, float radius, int top, int bottom) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        LinearGradient lg = new LinearGradient(0, r.top, 0, r.bottom, top, bottom, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(null);
    }

    private void shaderRect(Canvas c, RectF r, float radius, int top, int mid, int bottom) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        LinearGradient lg = new LinearGradient(0, r.top, 0, r.bottom, new int[]{top, mid, bottom}, null, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRoundRect(r, radius, radius, p);
        p.setShader(null);
    }

    private void strokeRound(Canvas c, RectF r, float radius, int color, float width) {
        Paint p = paint;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setColor(color);
        c.drawRoundRect(r, radius, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void fillH(Canvas c, RectF r, int a, int b, int d) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        LinearGradient lg = new LinearGradient(r.left, 0, r.right, 0, new int[]{a, b, d}, null, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRect(r, p);
        p.setShader(null);
    }

    private void text(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align align) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(tf);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        c.drawText(s, x, y, textPaint);
    }

    private static int blend(int a, int b, float t) {
        int aa = (a >>> 24), ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24), br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (chrome != null) canvas.drawBitmap(chrome, 0, 0, null);
        drawDisplay(canvas);
        if (pressed != null) {
            drawPress(canvas, pressed, 0x66000000);
        } else if (flashKey != null) {
            long elapsed = SystemClock.uptimeMillis() - flashStart;
            if (elapsed >= 140) {
                flashKey = null;
            } else {
                int alpha = (int) (0x66 * (1f - elapsed / 140f));
                drawPress(canvas, flashKey, alpha << 24);
                postInvalidateDelayed(16);
            }
        }
        if (mode != Mode.CALC) drawOverlay(canvas);
    }

    private void drawPress(Canvas c, Key k, int color) {
        float x = px(k.x), y = py(k.y), w = px(k.w), h = py(k.h);
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawRoundRect(new RectF(x + ps(2), y + ps(2), x + w - ps(2), y + h), ps(9), ps(9), p);
    }

    private void drawDisplay(Canvas canvas) {
        if (lcdScreen.isEmpty()) return;
        float left = lcdScreen.left, top = lcdScreen.top, right = lcdScreen.right, bottom = lcdScreen.bottom;
        float pad = ps(9);
        float statusSize = ps(8.2f);
        float small = ps(5.8f);

        String angle = prefs.angle() == 0 ? "DEG" : prefs.angle() == 1 ? "RAD" : "GRAD";
        text(canvas, angle, left + pad, top + ps(6) + statusSize, statusSize, theme.lcdInk, boldFace, Paint.Align.LEFT);
        text(canvas, "12 DIGIT", left + ps(66), top + ps(6) + small, small, theme.lcdDim, medium, Paint.Align.LEFT);
        String fmode = formatLabel();
        text(canvas, fmode, right - pad, top + ps(6) + small, small, theme.lcdDim, medium, Paint.Align.RIGHT);

        String expression = justEvaluated ? lastExpr + "  =" : expr;
        if (expression == null || expression.isEmpty()) expression = "READY";
        expression = showNumbers(expression);
        float exprSize = ps(landscape ? 10.5f : 15.6f);
        text(canvas, ellipsizeLeft(expression, right - left - 2 * pad, exprSize), left + pad,
                top + ps(20) + exprSize, exprSize, theme.lcdDim, medium, Paint.Align.LEFT);

        if (shift) text(canvas, "2nd", left + ps(36), bottom - ps(8), ps(7), 0xFF4A5F4B, medium, Paint.Align.LEFT);
        if (hyp) text(canvas, "HYP", left + ps(58), bottom - ps(8), ps(7), 0xFF4A5F4B, medium, Paint.Align.LEFT);
        if (memories[0] != 0) text(canvas, "M", left + pad, bottom - ps(8), ps(7.4f), theme.lcdInk, boldFace, Paint.Align.LEFT);
        text(canvas, "ENGINE READY", right - pad, bottom - ps(8), ps(5.2f), theme.lcdDim, medium, Paint.Align.RIGHT);

        String value = displayText();
        float baseline = top + lcdScreen.height() * (landscape ? 0.80f : 0.73f);
        if (errorState) {
            text(canvas, "Error", right - ps(15), baseline, ps(26), theme.lcdInk, boldFace, Paint.Align.RIGHT);
            return;
        }
        Formatter.Display d = Formatter.split(value);
        float maxRight = right - ps(13);
        float startSize = ps(landscape ? 40 : 54);
        float minSize = ps(18);
        if (d.exponent != null) {
            float expW = ps(78);
            float mw = lcdScreen.width() - ps(26) - expW;
            drawMantissa(canvas, d.mantissa, right - ps(13), baseline, mw, startSize, minSize);
            text(canvas, "×10", right - ps(72), top + ps(47), ps(8.2f), theme.lcdDim, medium, Paint.Align.LEFT);
            drawSmallDseg(canvas, d.exponent, right - ps(50), top + ps(41), ps(13), theme.lcdDim);
        } else {
            drawMantissa(canvas, d.mantissa, maxRight, baseline, lcdScreen.width() - ps(26), startSize, minSize);
        }
    }

    private void drawMantissa(Canvas c, String s, float right, float baseline, float maxW, float start, float min) {
        if (!prefs.decimalComma()) s = s.replace(",", " ");
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(dseg);
        textPaint.setColor(theme.lcdInk);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float size = start;
        textPaint.setTextSize(size);
        while (size > min && textPaint.measureText(s) > maxW) {
            size -= ps(1);
            textPaint.setTextSize(size);
        }
        c.drawText(s, right, baseline, textPaint);
    }

    private void drawSmallDseg(Canvas c, String s, float x, float baseline, float size, int color) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(dseg);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(Paint.Align.LEFT);
        c.drawText(s, x, baseline, textPaint);
    }

    private String formatLabel() {
        int f = prefs.format();
        if (f == 1) return "FIX " + prefs.decimals();
        if (f == 2) return "SCI " + prefs.decimals();
        if (f == 3) return "ENG " + prefs.decimals();
        return "AUTO";
    }

    private String displayText() {
        if (errorState) return "Error";
        if (displayOverride != null) return displayOverride;
        if (justEvaluated) return format(lastResult);
        if (expr.isEmpty()) return "0";
        // While typing, show the current entry, never a live result. The result appears on =.
        return showNumbers(currentEntry());
    }

    /** The numeric token being typed, e.g. "30" in "sin(30" or "3" in "2+3". */
    private String currentEntry() {
        String s = expr.trim();
        if (s.isEmpty()) return "0";
        if (s.equals("Ans")) return format(lastResult);
        int end = s.length();
        char last = s.charAt(end - 1);
        if (last == ')' || last == '!' || last == '%' || last == 'π') return "0";
        if (isOp(last)) {
            end--;
            while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        }
        int start = end;
        while (start > 0) {
            char ch = s.charAt(start - 1);
            if (Character.isDigit(ch) || ch == '.') { start--; continue; }
            if ((ch == 'e' || ch == 'E') && start < end) { start--; continue; }
            if ((ch == '-' || ch == '+') && start >= 2 && (s.charAt(start - 2) == 'e' || s.charAt(start - 2) == 'E')) {
                start--; continue;
            }
            break;
        }
        if (start == end) {
            if (s.startsWith("Ans")) return format(lastResult);
            return "0";
        }
        String r = s.substring(start, end);
        if (start > 0 && s.charAt(start - 1) == '-') r = "-" + r;
        return r;
    }

    private boolean isOp(char c) {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '÷' || c == '*' || c == '/' || c == '^';
    }

    private String trimIncomplete(String s) {
        String t = s.trim();
        while (!t.isEmpty()) {
            char c = t.charAt(t.length() - 1);
            if ("+-×÷−*/^(".indexOf(c) >= 0) { t = t.substring(0, t.length() - 1).trim(); continue; }
            if (t.endsWith("mod") || t.endsWith("nCr") || t.endsWith("nPr")) break;
            break;
        }
        return t;
    }

    private CalcEngine.Context engineCtx() {
        int a = prefs.angle();
        return new CalcEngine.Context(a == 0 ? CalcEngine.Angle.DEG : a == 1 ? CalcEngine.Angle.RAD : CalcEngine.Angle.GRAD, lastResult);
    }

    private String format(BigDecimal v) {
        Formatter.Mode m = prefs.format() == 1 ? Formatter.Mode.FIX
                : prefs.format() == 2 ? Formatter.Mode.SCI
                : prefs.format() == 3 ? Formatter.Mode.ENG : Formatter.Mode.AUTO;
        return Formatter.format(v, m, prefs.decimals(), prefs.grouping(), prefs.decimalComma());
    }

    /** Raw expressions are stored with a dot; the LCD shows the chosen decimal mark. */
    private String showNumbers(String s) {
        if (s == null) return "";
        return prefs.decimalComma() ? s.replace('.', ',') : s;
    }

    private String ellipsizeLeft(String s, float maxW, float size) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(medium);
        textPaint.setTextSize(size);
        if (textPaint.measureText(s) <= maxW) return s;
        String ell = "…";
        int start = 0;
        while (start < s.length() && textPaint.measureText(ell + s.substring(start)) > maxW) start++;
        return ell + s.substring(start);
    }

    // ---------------------------------------------------------------- input
    private void insert(String s) {
        if (s == null || s.isEmpty()) return;
        if (justEvaluated) {
            char c = s.charAt(0);
            if ("+-×÷−*/^!%".indexOf(c) >= 0) expr = "Ans";
            else expr = "";
            justEvaluated = false;
        }
        errorState = false;
        if (expr.length() + s.length() > 160) return;
        expr += s;
        invalidate();
    }

    private void appendDigit(String d) {
        if (justEvaluated) { expr = ""; justEvaluated = false; }
        errorState = false;
        if (expr.length() > 158) return;
        if (expr.endsWith(".")) {
            // fine
        }
        expr += d;
        invalidate();
    }

    private void appendDot() {
        if (justEvaluated) { expr = "0."; justEvaluated = false; invalidate(); return; }
        errorState = false;
        int i = expr.length() - 1;
        while (i >= 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) i--;
        String tail = expr.substring(i + 1);
        if (tail.contains(".")) return;
        if (tail.isEmpty()) expr += "0.";
        else expr += ".";
        invalidate();
    }

    private String lastChar() { return expr.isEmpty() ? "" : expr.substring(expr.length() - 1); }

    private void appendOperator(String op) {
        errorState = false;
        if (justEvaluated) { expr = "Ans"; justEvaluated = false; }
        if (expr.isEmpty()) expr = "0";
        String last = lastChar();
        if ("+−-×÷*/^".contains(last)) {
            expr = expr.substring(0, expr.length() - 1) + op;
        } else if (last.equals("(")) {
            if (op.equals("−") || op.equals("-")) expr += op;
        } else {
            expr += op;
        }
        invalidate();
    }

    private void backspace() {
        errorState = false;
        if (justEvaluated) { expr = ""; justEvaluated = false; invalidate(); return; }
        if (!expr.isEmpty()) expr = expr.substring(0, expr.length() - 1);
        invalidate();
    }

    private void clearAll() {
        expr = ""; lastExpr = ""; errorState = false; justEvaluated = false;
        lastResult = BigDecimal.ZERO;
        resultRecorded = false;
        shift = false; hyp = false;
        invalidate();
    }

    private void equals() {
        if (expr.trim().isEmpty()) {
            if (justEvaluated && !resultRecorded && lastExpr != null && !lastExpr.isEmpty()) {
                remember(lastExpr, lastResult);
                resultRecorded = true;
            }
            return;
        }
        String candidate = completeExpression(expr);
        try {
            BigDecimal v = CalcEngine.eval(candidate, engineCtx());
            lastResult = v;
            lastExpr = candidate;
            android.util.Log.d("COKALKU", "eval " + candidate + " = " + v.toPlainString());
            justEvaluated = true;
            remember(candidate, v);
            resultRecorded = true;
            errorState = false;
            expr = "";
            shift = false; hyp = false;
        } catch (CalcEngine.EvalException e) {
            errorState = true;
            errorMessage = e.getMessage() == null ? "Error" : e.getMessage();
        } catch (Exception e) {
            errorState = true;
            errorMessage = "Error";
        }
        invalidate();
    }

    /**
     * Real calculators close an open function on =, so "sin 30 =" means sin(30).
     * It also drops a trailing decimal point, e.g. "2." to "2".
     */
    private void remember(String expression, BigDecimal value) {
        String shown = format(value);
        history.add(0, new HistoryItem(expression, shown, value.stripTrailingZeros().toPlainString(), System.currentTimeMillis()));
        while (history.size() > 50) history.remove(history.size() - 1);
        prefs.saveHistory(history);
    }

    private String completeExpression(String raw) {
        String s = raw.trim();
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        int open = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') open++;
            else if (c == ')' && open > 0) open--;
        }
        StringBuilder b = new StringBuilder(s);
        while (open-- > 0) b.append(')');
        return b.toString();
    }

    private void toggleSign() {
        if (justEvaluated) { lastResult = lastResult.negate(); invalidate(); return; }
        if (expr.isEmpty()) { expr = "-"; invalidate(); return; }
        int end = expr.length();
        char last = expr.charAt(end - 1);
        if (last == ')' || Character.isLetter(last) || last == '!' || last == '%') {
            expr = "-(" + expr + ")";
            invalidate();
            return;
        }
        int start = end;
        while (start > 0) {
            char c = expr.charAt(start - 1);
            if (Character.isDigit(c) || c == '.') start--;
            else break;
        }
        if (start == end) { expr += "-"; invalidate(); return; }
        boolean neg = start > 0 && (expr.charAt(start - 1) == '-' || expr.charAt(start - 1) == '−');
        if (neg) expr = expr.substring(0, start - 1) + expr.substring(start);
        else expr = expr.substring(0, start) + "-" + expr.substring(start);
        invalidate();
    }

    private void insertValue(BigDecimal v) {
        String s = v.stripTrailingZeros().toPlainString();
        if (justEvaluated || expr.isEmpty()) { expr = s; justEvaluated = false; invalidate(); return; }
        char last = expr.charAt(expr.length() - 1);
        if (Character.isDigit(last) || last == ')' || last == '!' || last == '%' || last == 'π') {
            // implicit multiplication in the parser handles this; add a marker for clarity
            expr += "×";
        }
        expr += s;
        justEvaluated = false;
        invalidate();
    }

    private void insertFunction(String name) {
        if (justEvaluated) {
            expr = name + "(" + lastResult.stripTrailingZeros().toPlainString() + ")";
            justEvaluated = false;
        } else {
            expr += name + "(";
        }
        errorState = false;
        invalidate();
    }

    private void memoryStore(int index, boolean add) {
        double v = currentDouble();
        if (add) memories[index] += v;
        else memories[index] = v;
        prefs.setMemory(index, memories[index]);
        invalidate();
    }

    private double currentDouble() {
        try {
            if (justEvaluated) return lastResult.doubleValue();
            if (!expr.isEmpty()) return CalcEngine.eval(expr, engineCtx()).doubleValue();
            return lastResult.doubleValue();
        } catch (Exception e) { return 0; }
    }

    private void perform(Key k, boolean alt) {
        displayOverride = null;
        String a = (alt && k.alt != null) ? k.alt : k.action;
        if (a == null) return;
        switch (a) {
            case "d0": appendDigit("0"); break;
            case "d1": appendDigit("1"); break;
            case "d2": appendDigit("2"); break;
            case "d3": appendDigit("3"); break;
            case "d4": appendDigit("4"); break;
            case "d5": appendDigit("5"); break;
            case "d6": appendDigit("6"); break;
            case "d7": appendDigit("7"); break;
            case "d8": appendDigit("8"); break;
            case "d9": appendDigit("9"); break;
            case "dot": appendDot(); break;
            case "add": appendOperator("+"); break;
            case "sub": appendOperator("−"); break;
            case "mul": appendOperator("×"); break;
            case "div": appendOperator("÷"); break;
            case "pow": appendOperator("^"); break;
            case "nroot": appendOperator("^"); insert("(1/"); break;
            case "lparen": insert("("); break;
            case "rparen": insert(")"); break;
            case "percent": insert("%"); break;
            case "fact": insert("!"); break;
            case "eq": equals(); break;
            case "del": backspace(); break;
            case "ac": clearAll(); break;
            case "sign": toggleSign(); break;
            case "exp": insert("e"); break;
            case "sqrt": insertFunction("√"); break;
            case "cbrt": insertFunction("∛"); break;
            case "sq": insert("^2"); break;
            case "cube": insert("^3"); break;
            case "inv": if (expr.isEmpty()) expr = "1/("; else expr = "1/(" + expr + ")"; justEvaluated = false; invalidate(); break;
            case "sin": trig("sin", false); break;
            case "cos": trig("cos", false); break;
            case "tan": trig("tan", false); break;
            case "asin": trig("sin", true); break;
            case "acos": trig("cos", true); break;
            case "atan": trig("tan", true); break;
            case "hyp": hyp = !hyp; invalidate(); break;
            case "ln": insertFunction("ln"); break;
            case "log": insertFunction("log"); break;
            case "expf": insertFunction("exp"); break;
            case "pow10": insertFunction("10^"); break;
            case "shift": shift = !shift; if (!shift) hyp = false; invalidate(); break;
            case "drg": prefs.setAngle((prefs.angle() + 1) % 3); invalidate(); break;
            case "fse": prefs.setFormat((prefs.format() + 1) % 4); invalidate(); break;
            case "tab": prefs.setDecimals(nextDecimals()); invalidate(); break;
            case "mr": insertValue(BigDecimal.valueOf(memories[0])); break;
            case "ms": memoryStore(0, false); break;
            case "mplus": memoryStore(0, true); break;
            case "mminus": memories[0] -= currentDouble(); prefs.setMemory(0, memories[0]); invalidate(); break;
            case "recall": mode = Mode.MEMORY; memoryAction = 0; invalidate(); break;
            case "store": mode = Mode.MEMORY; memoryAction = 1; invalidate(); break;
            case "conv": mode = Mode.CONVERTER; convCat = 0; convFrom = 0; convTo = 8; invalidate(); break;
            case "cnst": mode = Mode.CONSTANTS; constGroup = 0; invalidate(); break;
            case "menu": mode = Mode.SETTINGS; invalidate(); break;
            case "ans": mode = Mode.HISTORY; historyScroll = 0; invalidate(); break;
            case "ncr": insert(" nCr "); break;
            case "npr": insert(" nPr "); break;
            case "mod": insert(" mod "); break;
            case "pi": insert("π"); break;
            case "e": insert("e"); break;
            case "dec": case "hex": case "oct": case "bin":
                mode = Mode.RADIX; radixMode = a; invalidate(); break;
            default: break;
        }
        if (!a.equals("shift") && !a.equals("hyp")) shift = false;
    }

    private int nextDecimals() {
        int d = prefs.decimals();
        if (d == 2) return 3;
        if (d == 3) return 4;
        if (d == 4) return 5;
        if (d == 5) return 6;
        if (d == 6) return 8;
        if (d == 8) return 10;
        return 2;
    }

    private String radixMode = "dec";
    private String radixValue = "";

    /**
     * Applies a unary function the way a pocket calculator does: 30 then sin gives 0.5,
     * instead of requiring sin(30). It returns false when there is no value to apply to,
     * so the caller can fall back to the prefix "sin(" entry style.
     */
    private boolean applyUnary(String fn) {
        if (expr.trim().isEmpty() && !justEvaluated) return false;
        displayOverride = null;
        String target;
        String prefix = "";
        if (justEvaluated) {
            target = lastResult.stripTrailingZeros().toPlainString();
        } else {
            String s = expr.trim();
            if (!balanced(s)) return false;
            int start = entryStart(s);
            if (start >= 0) {
                target = s.substring(start);
                prefix = s.substring(0, start);
            } else if (s.endsWith(")") || s.endsWith("!") || s.endsWith("%")
                    || s.equals("π") || s.equals("e") || s.endsWith("π")) {
                target = s;
            } else {
                return false;
            }
        }
        String inner = fn + "(" + target + ")";
        try {
            BigDecimal v = CalcEngine.eval(completeExpression(inner), engineCtx());
            if (prefix.isEmpty()) {
                lastResult = v;
                lastExpr = completeExpression(inner);
                justEvaluated = true;
                expr = "";
                displayOverride = null;
            } else {
                expr = prefix + inner;
                lastExpr = expr;
                justEvaluated = false;
                displayOverride = format(v);
            }
            resultRecorded = false;
            errorState = false;
            shift = false;
            hyp = false;
            android.util.Log.d("COKALKU", "immediate " + lastExpr + " = " + v.toPlainString());
        } catch (CalcEngine.EvalException e) {
            errorState = true;
            errorMessage = e.getMessage() == null ? "Error" : e.getMessage();
        } catch (Exception e) {
            errorState = true;
            errorMessage = "Error";
        }
        invalidate();
        return true;
    }

    private boolean applyPostfix(String suffix) {
        if (expr.trim().isEmpty() && !justEvaluated) return false;
        displayOverride = null;
        String target;
        String prefix = "";
        if (justEvaluated) {
            target = lastResult.stripTrailingZeros().toPlainString();
        } else {
            String s = expr.trim();
            if (!balanced(s)) return false;
            int start = entryStart(s);
            if (start >= 0) {
                target = s.substring(start);
                prefix = s.substring(0, start);
            } else if (s.endsWith(")") || s.endsWith("!") || s.endsWith("%")
                    || s.equals("π") || s.equals("e") || s.endsWith("π")) {
                target = s;
            } else {
                return false;
            }
        }
        String inner = target + suffix;
        try {
            BigDecimal v = CalcEngine.eval(completeExpression(inner), engineCtx());
            if (prefix.isEmpty()) {
                lastResult = v;
                lastExpr = completeExpression(inner);
                justEvaluated = true;
                expr = "";
                displayOverride = null;
            } else {
                expr = prefix + inner;
                lastExpr = expr;
                justEvaluated = false;
                displayOverride = format(v);
            }
            resultRecorded = false;
            errorState = false;
            shift = false;
            hyp = false;
            android.util.Log.d("COKALKU", "immediate " + lastExpr + " = " + v.toPlainString());
        } catch (CalcEngine.EvalException e) {
            errorState = true;
            errorMessage = e.getMessage() == null ? "Error" : e.getMessage();
        } catch (Exception e) {
            errorState = true;
            errorMessage = "Error";
        }
        invalidate();
        return true;
    }

    /** Index where the number currently being typed starts, or -1 when the last token is not a number. */
    private int entryStart(String s) {
        if (s.isEmpty()) return -1;
        char last = s.charAt(s.length() - 1);
        if (last == ')' || last == '!' || last == '%' || last == 'π') return -1;
        int end = s.length();
        if (isOp(last)) {
            end--;
            while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        }
        int start = end;
        while (start > 0) {
            char ch = s.charAt(start - 1);
            if (Character.isDigit(ch) || ch == '.') { start--; continue; }
            if ((ch == 'e' || ch == 'E') && start < end) { start--; continue; }
            if ((ch == '-' || ch == '+') && start >= 2 && (s.charAt(start - 2) == 'e' || s.charAt(start - 2) == 'E')) {
                start--; continue;
            }
            break;
        }
        if (start == end) return -1;
        if (start > 0 && s.charAt(start - 1) == '-'
                && (start == 1 || isOp(s.charAt(start - 2)) || s.charAt(start - 2) == '(')) {
            start--;
        }
        return start;
    }

    private boolean balanced(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') n++;
            else if (c == ')' && --n < 0) return false;
        }
        return n == 0;
    }

    private void trig(String base, boolean inverse) {
        String name = (inverse ? "a" : "") + base + (hyp ? "h" : "");
        insertFunction(name);
        hyp = false;
    }

    // ---------------------------------------------------------------- touch
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (mode != Mode.CALC) return overlayTouch(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                Key k = keyAt(e.getX(), e.getY());
                if (k == null) return true;
                pressed = k;
                flashKey = k;
                flashStart = SystemClock.uptimeMillis();
                longFired = false;
                feedback(false);
                downX = e.getX(); downY = e.getY();
                if (k.alt != null) {
                    longPressRunnable = () -> {
                        if (pressed == k) {
                            longFired = true;
                            feedback(true);
                            perform(k, true);
                            invalidate();
                        }
                    };
                    handler.postDelayed(longPressRunnable, 480);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                if (pressed != null && (Math.abs(e.getX() - downX) > ps(14) || Math.abs(e.getY() - downY) > ps(14))) {
                    handler.removeCallbacks(longPressRunnable);
                    pressed = null;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP: {
                handler.removeCallbacks(longPressRunnable);
                Key k = pressed;
                pressed = null;
                flashKey = k;
                flashStart = SystemClock.uptimeMillis();
                if (k != null && keyAt(e.getX(), e.getY()) == k) {
                    boolean useAlt = shift || longFired;
                    if (k.action.equals("shift")) perform(k, false);
                    else perform(k, useAlt);
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPressRunnable);
                pressed = null;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private Key keyAt(float x, float y) {
        for (int i = keys.size() - 1; i >= 0; i--) {
            Key k = keys.get(i);
            if (x >= px(k.x) && x <= px(k.x + k.w) && y >= py(k.y) && y <= py(k.y + k.h + 4)) return k;
        }
        return null;
    }

    private void feedback(boolean longPress) {
        if (prefs.haptics()) {
            try {
                performHapticFeedback(longPress ? HapticFeedbackConstants.LONG_PRESS : HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
            } catch (Exception ignored) {}
            vibrate(longPress ? 90 : 60);
        }
        if (prefs.sound()) playClick();
    }

    private void playClick() {
        if (soundPool != null && clickSound != 0 && clickReady) {
            float vol = Math.max(0.05f, Math.min(1f, prefs.clickVolume() / 100f));
            if (soundPool.play(clickSound, vol, vol, 1, 0, 1.0f) == 0) fallbackClick();
        } else {
            fallbackClick();
        }
    }

    private void fallbackClick() {
        try {
            android.media.AudioManager am = (android.media.AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) am.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD);
        } catch (Exception ignored) {}
    }

    private void vibrate(int ms) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            android.util.Log.d("COKALKU", "vibrate skipped: no vibrator");
            return;
        }
        int amp = Math.max(1, Math.min(255, prefs.hapticIntensity() * 255 / 100));
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                // Some devices silently ignore an explicit amplitude. Use the platform
                // default amplitude when the actuator cannot control amplitude.
                int amplitude = vibrator.hasAmplitudeControl() ? amp : VibrationEffect.DEFAULT_AMPLITUDE;
                vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude));
                android.util.Log.d("COKALKU", "vibrate " + ms + "ms amp=" + amplitude
                        + " control=" + vibrator.hasAmplitudeControl());
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception e) {
            try {
                vibrator.vibrate(new long[]{0, ms}, -1);
                android.util.Log.d("COKALKU", "vibrate fallback " + ms + "ms");
            } catch (Exception ignored) {
                android.util.Log.d("COKALKU", "vibrate failed: " + e);
            }
        }
    }

    /** A long, unmistakable pattern used by the Settings test. */
    private void vibrateTest() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 300, 120, 300, 120, 600};
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
            android.util.Log.d("COKALKU", "vibrate test pattern sent");
        } catch (Exception e) {
            try {
                vibrator.vibrate(pattern, -1);
                android.util.Log.d("COKALKU", "vibrate test legacy fallback");
            } catch (Exception ignored) {
                android.util.Log.d("COKALKU", "vibrate test failed: " + e);
            }
        }
    }

    // ---------------------------------------------------------------- overlays (stubs, filled below)
    private void drawOverlay(Canvas c) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x9E07070A);
        c.drawRect(0, 0, W, H, p);
        switch (mode) {
            case HISTORY: drawHistory(c); break;
            case MEMORY: drawMemory(c); break;
            case CONVERTER: drawConverter(c); break;
            case CONSTANTS: drawConstants(c); break;
            case SETTINGS: drawSettings(c); break;
            case RADIX: drawRadix(c); break;
            default: break;
        }
    }

    private boolean overlayTouch(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = e.getX(), y = e.getY();
            if (closeRect.contains(x, y)) { mode = Mode.CALC; invalidate(); return true; }
            if (mode == Mode.HISTORY && !dragMoved) return historyTouch(x, y);
            if (mode == Mode.MEMORY) return memoryTouch(x, y);
            if (mode == Mode.CONVERTER) return converterTouch(x, y);
            if (mode == Mode.CONSTANTS) return constantsTouch(x, y);
            if (mode == Mode.SETTINGS) return settingsTouch(x, y);
            if (mode == Mode.RADIX) return radixTouch(x, y);
        }
        if (mode == Mode.HISTORY) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                dragStartY = e.getY(); dragStartScroll = historyScroll; dragging = true; dragMoved = false;
            } else if (e.getActionMasked() == MotionEvent.ACTION_MOVE && dragging) {
                if (Math.abs(e.getY() - dragStartY) > ps(6)) dragMoved = true;
                int delta = (int) ((e.getY() - dragStartY) / Math.max(1, ps(42)));
                if (delta != 0) {
                    int max = Math.max(0, history.size() - overlayRows);
                    int ns = Math.max(0, Math.min(max, dragStartScroll - delta));
                    if (ns != historyScroll) { historyScroll = ns; invalidate(); }
                }
            } else if (e.getActionMasked() == MotionEvent.ACTION_UP
                    || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                dragging = false;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- overlay chrome
    private void setupPanel(Canvas c, String title, String sub, float wf, float hf) {
        float w = W * wf, h = H * hf;
        panel.set((W - w) / 2f, (H - h) / 2f, (W + w) / 2f, (H + h) / 2f);
        if (landscape) panel.inset(W * 0.03f, H * 0.02f);
        closeRect.set(panel.right - ps(44), panel.top + ps(14), panel.right - ps(14), panel.top + ps(44));

        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setShadowLayer(ps(18), 0, ps(12), 0xB0000000);
        p.setColor(0xFF202024);
        c.drawRoundRect(panel, ps(16), ps(16), p);
        p.clearShadowLayer();

        LinearGradient lg = new LinearGradient(0, panel.top, 0, panel.bottom,
                0xFF313136, 0xFF232327, Shader.TileMode.CLAMP);
        p.setShader(lg);
        c.drawRoundRect(panel, ps(16), ps(16), p);
        p.setShader(null);
        strokeRound(c, panel, ps(16), 0xFF787882, ps(0.9f));

        RectF head = new RectF(panel.left + ps(1.5f), panel.top + ps(1.5f), panel.right - ps(1.5f), panel.top + ps(62));
        p.setColor(0x0EFFFFFF);
        c.drawRoundRect(head, ps(14), ps(14), p);
        c.drawLine(panel.left + ps(16), panel.top + ps(62), panel.right - ps(16), panel.top + ps(62), linePaint(0x1AFFFFFF, 0.7f));

        spacedText(c, title, panel.left + ps(18), panel.top + ps(30), ps(11.6f), 0xFFD6D6DE, boldFace, Paint.Align.LEFT, 0.14f);
        spacedText(c, sub, panel.left + ps(18), panel.top + ps(47), ps(5.4f), 0xFFA99BFF, medium, Paint.Align.LEFT, 0.06f);

        // close key
        RectF cr = new RectF(closeRect);
        float r = ps(7);
        p.setColor(0xFF17171A);
        c.drawRoundRect(new RectF(cr.left + ps(1), cr.top + ps(3), cr.right - ps(1), cr.bottom + ps(3)), r, r, p);
        shaderRect(c, cr, r, 0xFF4B4B52, 0xFF303036);
        strokeRound(c, cr, r, 0x33FFFFFF, ps(0.8f));
        text(c, "×", cr.centerX(), cr.centerY() + ps(5.5f), ps(15), 0xFFF0F0F5, lightFace, Paint.Align.CENTER);
    }

    private Paint linePaint(int color, float width) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(ps(width));
        textPaint.setColor(color);
        return textPaint;
    }

    private void card(Canvas c, RectF r, int fill, int stroke, float radius, boolean active) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawRoundRect(r, radius, radius, p);
        if (stroke != 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(ps(active ? 1.2f : 0.7f));
            p.setColor(stroke);
            c.drawRoundRect(r, radius, radius, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private void spacedText(Canvas c, String s, float x, float y, float size, int color, Typeface tf, Paint.Align align, float spacing) {
        textPaint.reset();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(tf);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        textPaint.setTextAlign(align);
        textPaint.setLetterSpacing(spacing);
        c.drawText(s, x, y, textPaint);
        textPaint.setLetterSpacing(0);
    }

    private void button(Canvas c, RectF r, String label, int color, int fill, int stroke) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setShadowLayer(ps(3), 0, ps(2), 0x70000000);
        p.setColor(fill);
        c.drawRoundRect(r, ps(8), ps(8), p);
        p.clearShadowLayer();
        strokeRound(c, r, ps(8), stroke, ps(0.8f));
        text(c, label, r.centerX(), r.centerY() + ps(3.4f), ps(8.2f), color, boldFace, Paint.Align.CENTER);
    }

    private void drawHistory(Canvas c) {
        int visible = landscape ? 5 : 8;
        int wanted = Math.max(2, Math.min(visible, history.size()));
        float hf = (78f + wanted * 45f + 56f) / (landscape ? 412f : 915f);
        hf = Math.max(landscape ? 0.55f : 0.34f, Math.min(landscape ? 0.92f : 0.64f, hf));
        setupPanel(c, "RESULT HISTORY", "50 RESULTS · MOST RECENT FIRST", 0.82f, hf);
        int rows = wanted;
        overlayRows = rows;
        float listTop = panel.top + ps(74);
        float footerTop = panel.bottom - ps(56);
        float rowH = Math.max(ps(26), (footerTop - ps(8) - listTop) / rows);
        if (history.isEmpty()) {
            text(c, "NO CALCULATIONS YET", panel.centerX(), panel.centerY(), ps(9), theme.muted, medium, Paint.Align.CENTER);
        }
        int max = Math.max(0, history.size() - rows);
        historyScroll = Math.max(0, Math.min(max, historyScroll));
        for (int i = 0; i < rows; i++) {
            int idx = historyScroll + i;
            if (idx >= history.size()) break;
            HistoryItem h = history.get(idx);
            float y = listTop + i * rowH;
            if (i % 2 == 1) {
                card(c, new RectF(panel.left + ps(10), y, panel.right - ps(10), y + rowH - ps(2)), 0x0AFFFFFF, 0, ps(6), false);
            }
            text(c, String.format(java.util.Locale.US, "%02d", idx + 1), panel.left + ps(20), y + rowH * 0.56f, ps(7.6f), 0xFFA99BFF, boldFace, Paint.Align.LEFT);
            String val = h.value;
            textPaint.reset();
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(dseg);
            textPaint.setTextSize(ps(16.5f));
            textPaint.setColor(0xFFEDEDF2);
            textPaint.setTextAlign(Paint.Align.LEFT);
            while (textPaint.measureText(val) > panel.width() - ps(84) && textPaint.getTextSize() > ps(9)) {
                textPaint.setTextSize(textPaint.getTextSize() - ps(0.8f));
            }
            c.drawText(val, panel.left + ps(44), y + rowH * 0.56f, textPaint);
            text(c, ellipsizeLeft(showNumbers(h.expression), panel.width() - ps(86), ps(6.0f)), panel.left + ps(44), y + rowH * 0.86f, ps(6.0f), theme.muted, medium, Paint.Align.LEFT);
            text(c, "→", panel.right - ps(22), y + rowH * 0.62f, ps(8), 0xFF6E6E78, sans, Paint.Align.RIGHT);
            c.drawLine(panel.left + ps(18), y + rowH - ps(1), panel.right - ps(18), y + rowH - ps(1), linePaint(0x1AFFFFFF, 0.6f));
        }
        footerRect.set(panel.left + ps(16), footerTop, panel.right - ps(16), footerTop + ps(38));
        button(c, footerRect, "CLEAR ALL", 0xFFE7A0A0, 0xFF3D3D42, 0x80B05A5A);
    }

    private boolean historyTouch(float x, float y) {
        if (footerRect.contains(x, y)) {
            history.clear();
            prefs.clearHistory();
            historyScroll = 0;
            invalidate();
            return true;
        }
        float listTop = panel.top + ps(74);
        float footerTop = panel.bottom - ps(56);
        int rows = overlayRows;
        float rowH = Math.max(ps(26), (footerTop - ps(8) - listTop) / rows);
        int idx = (int) ((y - listTop) / rowH) + historyScroll;
        if (y >= listTop && idx >= 0 && idx < history.size()) {
            try {
                insertValue(new BigDecimal(history.get(idx).raw));
                mode = Mode.CALC;
                invalidate();
            } catch (Exception ignored) {}
            return true;
        }
        return true;
    }

    private void drawMemory(Canvas c) {
        setupPanel(c, "MEMORY", "MAIN M + NINE STORAGE SLOTS", 0.82f, 0.66f);
        float tabTop = panel.top + ps(70);
        float tabW = (panel.width() - ps(50)) / 2;
        RectF recall = new RectF(panel.left + ps(16), tabTop, panel.left + ps(16) + tabW, tabTop + ps(28));
        RectF store = new RectF(recall.right + ps(10), tabTop, panel.right - ps(16), tabTop + ps(28));
        card(c, recall, memoryAction == 0 ? 0x33D9B64B : 0x0AFFFFFF, memoryAction == 0 ? 0x99D9B64B : 0x1AFFFFFF, ps(6), memoryAction == 0);
        card(c, store, memoryAction == 1 ? 0x33D9B64B : 0x0AFFFFFF, memoryAction == 1 ? 0x99D9B64B : 0x1AFFFFFF, ps(6), memoryAction == 1);
        text(c, "RECALL", recall.centerX(), recall.centerY() + ps(3), ps(7.4f), memoryAction == 0 ? 0xFFF5E7B8 : theme.muted, boldFace, Paint.Align.CENTER);
        text(c, "STORE", store.centerX(), store.centerY() + ps(3), ps(7.4f), memoryAction == 1 ? 0xFFF5E7B8 : theme.muted, boldFace, Paint.Align.CENTER);

        float top = tabTop + ps(42);
        float footerTop = panel.bottom - ps(54);
        float gap = ps(8);
        float cw = (panel.width() - ps(32) - gap) / 2;
        float chh = (footerTop - ps(8) - top) / 5;
        for (int i = 0; i < 10; i++) {
            int col = i % 2, row = i / 2;
            float x = panel.left + ps(16) + col * (cw + gap);
            float y = top + row * chh;
            RectF r = new RectF(x, y, x + cw, y + chh - gap);
            card(c, r, 0x0AFFFFFF, 0x14FFFFFF, ps(8), false);
            String label = i == 0 ? "M" : String.valueOf(i);
            text(c, label, r.left + ps(10), r.top + ps(16), ps(8.0f), i == 0 ? 0xFFD9B64B : 0xFFA99BFF, boldFace, Paint.Align.LEFT);
            String v = format(BigDecimal.valueOf(memories[i])).replace(",", " ");
            textPaint.reset();
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(dseg);
            textPaint.setTextSize(ps(16.0f));
            textPaint.setColor(0xFFEDEDF2);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            while (textPaint.measureText(v) > r.width() - ps(36) && textPaint.getTextSize() > ps(8)) {
                textPaint.setTextSize(textPaint.getTextSize() - ps(0.7f));
            }
            c.drawText(v, r.right - ps(10), r.centerY() + ps(5), textPaint);
        }
        footerRect.set(panel.left + ps(16), footerTop, panel.right - ps(16), footerTop + ps(34));
        button(c, footerRect, "CLEAR ALL MEMORIES", 0xFFE7A0A0, 0xFF3D3D42, 0x80B05A5A);
    }

    private boolean memoryTouch(float x, float y) {
        float tabTop = panel.top + ps(70);
        float tabW = (panel.width() - ps(50)) / 2;
        RectF recall = new RectF(panel.left + ps(16), tabTop, panel.left + ps(16) + tabW, tabTop + ps(28));
        RectF store = new RectF(recall.right + ps(10), tabTop, panel.right - ps(16), tabTop + ps(28));
        if (recall.contains(x, y)) { memoryAction = 0; invalidate(); return true; }
        if (store.contains(x, y)) { memoryAction = 1; invalidate(); return true; }
        if (footerRect.contains(x, y)) {
            memories = new double[10];
            prefs.clearMemories();
            invalidate();
            return true;
        }
        float top = tabTop + ps(42);
        float footerTop = panel.bottom - ps(54);
        float gap = ps(8);
        float cw = (panel.width() - ps(32) - gap) / 2;
        float chh = (footerTop - ps(8) - top) / 5;
        for (int i = 0; i < 10; i++) {
            int col = i % 2, row = i / 2;
            float rx = panel.left + ps(16) + col * (cw + gap);
            float ry = top + row * chh;
            RectF r = new RectF(rx, ry, rx + cw, ry + chh - gap);
            if (r.contains(x, y)) {
                if (memoryAction == 1) {
                    memories[i] = currentDouble();
                    prefs.setMemory(i, memories[i]);
                } else {
                    insertValue(BigDecimal.valueOf(memories[i]));
                    mode = Mode.CALC;
                }
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void drawConverter(Canvas c) {
        setupPanel(c, "UNIT CONVERTER", UnitConverter.CATEGORIES.size() + " CATEGORIES · 79 UNITS", 0.9f, 0.78f);
        java.util.List<String> cats = UnitConverter.names();
        int chipCols = 5;
        float gap = ps(5);
        float chipW = (panel.width() - ps(32) - (chipCols - 1) * gap) / chipCols;
        float chipH = ps(25);
        for (int i = 0; i < cats.size(); i++) {
            int col = i % chipCols, row = i / chipCols;
            float x = panel.left + ps(16) + col * (chipW + gap);
            float y = panel.top + ps(72) + row * (chipH + ps(5));
            boolean active = i == convCat;
            card(c, new RectF(x, y, x + chipW, y + chipH), active ? 0xFFE8BC31 : 0x0AFFFFFF,
                    active ? 0xFFE8BC31 : 0x1AFFFFFF, ps(6), active);
            text(c, cats.get(i), x + chipW / 2, y + chipH / 2 + ps(3), ps(6.0f),
                    active ? 0xFF211B06 : 0xFFC9C9D2, active ? boldFace : medium, Paint.Align.CENTER);
        }

        UnitConverter.Category cat = UnitConverter.CATEGORIES.get(cats.get(convCat));
        convFrom = Math.max(0, Math.min(cat.units.size() - 1, convFrom));
        convTo = Math.max(0, Math.min(cat.units.size() - 1, convTo));
        double base = currentDouble();
        if (base == 0) base = 1;

        float cardsTop = panel.top + ps(72) + 2 * (chipH + ps(5)) + ps(8);
        float cardH = ps(82);
        RectF fromCard = new RectF(panel.left + ps(16), cardsTop, panel.right - ps(16), cardsTop + cardH);
        card(c, fromCard, 0x0EFFFFFF, 0x1AFFFFFF, ps(9), false);
        text(c, "FROM", fromCard.left + ps(12), fromCard.top + ps(18), ps(5.4f), 0xFFA99BFF, boldFace, Paint.Align.LEFT);
        String fromVal = UnitConverter.formatValue(base, prefs.decimalComma());
        text(c, fromVal, fromCard.left + ps(12), fromCard.top + ps(50), ps(20), 0xFFF4F4F8, medium, Paint.Align.LEFT);
        rectCaret(c, fromCard.right - ps(26), fromCard.top + ps(22), ps(2), ps(40));
        text(c, cat.units.get(convFrom).name, fromCard.left + ps(12), fromCard.bottom - ps(10), ps(8.4f), 0xFFD2D2DA, medium, Paint.Align.LEFT);
        text(c, cat.units.get(convFrom).symbol, fromCard.right - ps(14), fromCard.bottom - ps(10), ps(8.4f), theme.muted, sans, Paint.Align.RIGHT);

        float swapY = fromCard.bottom + ps(10);
        RectF swap = new RectF(panel.centerX() - ps(16), swapY, panel.centerX() + ps(16), swapY + ps(32));
        pCircle(c, swap.centerX(), swap.centerY(), ps(16), 0xFF3E3E45, 0xFF8A8A94);
        text(c, "⇅", swap.centerX(), swap.centerY() + ps(6), ps(14), 0xFFE8E8EE, sans, Paint.Align.CENTER);

        float toTop = swap.bottom + ps(10);
        double converted = UnitConverter.convert(cat, convFrom, convTo, base);
        RectF toCard = new RectF(panel.left + ps(16), toTop, panel.right - ps(16), toTop + cardH);
        card(c, toCard, 0x1A2E3E31, 0x88B9D6B0, ps(9), false);
        text(c, "TO", toCard.left + ps(12), toCard.top + ps(18), ps(5.4f), 0xFFB7D5AE, boldFace, Paint.Align.LEFT);
        text(c, UnitConverter.formatValue(converted, prefs.decimalComma()), toCard.right - ps(14), toCard.top + ps(50), ps(20), 0xFFDCEBD7, medium, Paint.Align.RIGHT);
        text(c, cat.units.get(convTo).name, toCard.left + ps(12), toCard.bottom - ps(10), ps(8.4f), 0xFFD2E2CD, medium, Paint.Align.LEFT);
        text(c, cat.units.get(convTo).symbol, toCard.right - ps(14), toCard.bottom - ps(10), ps(8.4f), 0xFF9BB295, sans, Paint.Align.RIGHT);

        float listTop = toCard.bottom + ps(20);
        text(c, "TAP NAME FOR FROM · TAP VALUE FOR TO", panel.left + ps(18), listTop, ps(5.2f), theme.footer, medium, Paint.Align.LEFT);
        float rowsTop = listTop + ps(10);
        float footerTop = panel.bottom - ps(16);
        float rowGap = ps(6);
        float colW = (panel.width() - ps(32) - ps(8)) / 2;
        float rowH = (footerTop - rowsTop) / 5;
        int shown = Math.min(10, cat.units.size());
        for (int i = 0; i < shown; i++) {
            int col = i % 2, row = i / 2;
            float x = panel.left + ps(16) + col * (colW + ps(8));
            float y = rowsTop + row * rowH;
            RectF r = new RectF(x, y, x + colW, y + rowH - rowGap);
            boolean isFrom = i == convFrom, isTo = i == convTo;
            card(c, r, isTo ? 0x152E3E31 : 0x08FFFFFF,
                    isFrom ? 0x99D9B64B : (isTo ? 0x88B9D6B0 : 0x14FFFFFF), ps(7), isFrom || isTo);
            text(c, cat.units.get(i).name, r.left + ps(9), r.top + ps(17), ps(6.8f), 0xFFE2E2E8, medium, Paint.Align.LEFT);
            text(c, cat.units.get(i).symbol, r.right - ps(9), r.top + ps(17), ps(6.2f), 0xFFA0A0FF, boldFace, Paint.Align.RIGHT);
            double v = UnitConverter.convert(cat, convFrom, i, base);
            text(c, UnitConverter.formatValue(v, prefs.decimalComma()), r.left + ps(9), r.bottom - ps(7), ps(7.0f), 0xFF9D9DA7, sans, Paint.Align.LEFT);
        }
    }

    private void rectCaret(Canvas c, float x, float y, float w, float h) {
        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFD9B64B);
        c.drawRoundRect(new RectF(x, y, x + w, y + h), w / 2f, w / 2f, p);
    }

    private void pCircle(Canvas c, float x, float y, float r, int fill, int stroke) {        Paint p = paint;
        p.setStyle(Paint.Style.FILL);
        p.setShadowLayer(ps(3), 0, ps(2), 0x70000000);
        p.setColor(fill);
        c.drawCircle(x, y, r, p);
        p.clearShadowLayer();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(ps(0.8f));
        p.setColor(stroke);
        c.drawCircle(x, y, r, p);
        p.setStyle(Paint.Style.FILL);
    }

    private boolean converterTouch(float x, float y) {
        java.util.List<String> cats = UnitConverter.names();
        int chipCols = 5;
        float gap = ps(5);
        float chipW = (panel.width() - ps(32) - (chipCols - 1) * gap) / chipCols;
        float chipH = ps(25);
        for (int i = 0; i < cats.size(); i++) {
            int col = i % chipCols, row = i / chipCols;
            float cx = panel.left + ps(16) + col * (chipW + gap);
            float cy = panel.top + ps(72) + row * (chipH + ps(5));
            if (x >= cx && x <= cx + chipW && y >= cy && y <= cy + chipH) {
                convCat = i;
                convFrom = 0;
                convTo = Math.min(1, UnitConverter.CATEGORIES.get(cats.get(i)).units.size() - 1);
                invalidate();
                return true;
            }
        }
        float cardsTop = panel.top + ps(72) + 2 * (chipH + ps(5)) + ps(8);
        float cardH = ps(82);
        RectF fromCard = new RectF(panel.left + ps(16), cardsTop, panel.right - ps(16), cardsTop + cardH);
        RectF swap = new RectF(panel.centerX() - ps(16), fromCard.bottom + ps(10), panel.centerX() + ps(16), fromCard.bottom + ps(42));
        if (swap.contains(x, y)) {
            int t = convFrom; convFrom = convTo; convTo = t; invalidate(); return true;
        }
        UnitConverter.Category cat = UnitConverter.CATEGORIES.get(cats.get(convCat));
        float toTop = swap.bottom + ps(10);
        RectF toCard = new RectF(panel.left + ps(16), toTop, panel.right - ps(16), toTop + cardH);
        float listTop = toCard.bottom + ps(20);
        float rowsTop = listTop + ps(10);
        float footerTop = panel.bottom - ps(16);
        float rowGap = ps(6);
        float colW = (panel.width() - ps(32) - ps(8)) / 2;
        float rowH = (footerTop - rowsTop) / 5;
        int shown = Math.min(10, cat.units.size());
        for (int i = 0; i < shown; i++) {
            int col = i % 2, row = i / 2;
            float cx = panel.left + ps(16) + col * (colW + ps(8));
            float cy = rowsTop + row * rowH;
            RectF r = new RectF(cx, cy, cx + colW, cy + rowH - rowGap);
            if (r.contains(x, y)) {
                if (x < r.centerX()) convFrom = i; else convTo = i;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void drawConstants(Canvas c) {
        setupPanel(c, "CONSTANTS", "MATHEMATICS & PHYSICS", 0.86f, 0.82f);
        java.util.List<String> groups = ConstantsCatalog.groupNames();
        float tabTop = panel.top + ps(70);
        float tabW = (panel.width() - ps(50)) / 2;
        RectF g1 = new RectF(panel.left + ps(16), tabTop, panel.left + ps(16) + tabW, tabTop + ps(27));
        RectF g2 = new RectF(g1.right + ps(10), tabTop, panel.right - ps(16), tabTop + ps(27));
        card(c, g1, constGroup == 0 ? 0x33D9B64B : 0x0AFFFFFF, constGroup == 0 ? 0x99D9B64B : 0x1AFFFFFF, ps(6), constGroup == 0);
        card(c, g2, constGroup == 1 ? 0x33D9B64B : 0x0AFFFFFF, constGroup == 1 ? 0x99D9B64B : 0x1AFFFFFF, ps(6), constGroup == 1);
        text(c, groups.get(0), g1.centerX(), g1.centerY() + ps(3), ps(7.0f), constGroup == 0 ? 0xFFF5E7B8 : theme.muted, boldFace, Paint.Align.CENTER);
        text(c, groups.get(1), g2.centerX(), g2.centerY() + ps(3), ps(7.0f), constGroup == 1 ? 0xFFF5E7B8 : theme.muted, boldFace, Paint.Align.CENTER);

        java.util.List<ConstantsCatalog.Constant> list = ConstantsCatalog.GROUPS.get(groups.get(constGroup));
        float top = tabTop + ps(38);
        float footerTop = panel.bottom - ps(14);
        float gap = ps(7);
        float cw = (panel.width() - ps(32) - gap) / 2;
        float chh = (footerTop - top) / 8;
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, row = i / 2;
            float x = panel.left + ps(16) + col * (cw + gap);
            float y = top + row * chh;
            RectF r = new RectF(x, y, x + cw, y + chh - gap);
            card(c, r, 0x08FFFFFF, 0x14FFFFFF, ps(7), false);
            ConstantsCatalog.Constant k = list.get(i);
            text(c, k.name, r.left + ps(8), r.top + ps(13), ps(5.9f), 0xFFDCDCE3, medium, Paint.Align.LEFT);
            text(c, k.symbol, r.right - ps(8), r.top + ps(13), ps(6.2f), 0xFFA0A0FF, boldFace, Paint.Align.RIGHT);
            text(c, ellipsizeLeft(k.value + (k.unit.isEmpty() ? "" : " " + k.unit), r.width() - ps(16), ps(5.2f)),
                    r.left + ps(8), r.bottom - ps(7), ps(5.2f), 0xFF9D9DA7, sans, Paint.Align.LEFT);
        }
    }

    private boolean constantsTouch(float x, float y) {
        java.util.List<String> groups = ConstantsCatalog.groupNames();
        float tabTop = panel.top + ps(70);
        float tabW = (panel.width() - ps(50)) / 2;
        RectF g1 = new RectF(panel.left + ps(16), tabTop, panel.left + ps(16) + tabW, tabTop + ps(27));
        RectF g2 = new RectF(g1.right + ps(10), tabTop, panel.right - ps(16), tabTop + ps(27));
        if (g1.contains(x, y)) { constGroup = 0; invalidate(); return true; }
        if (g2.contains(x, y)) { constGroup = 1; invalidate(); return true; }
        java.util.List<ConstantsCatalog.Constant> list = ConstantsCatalog.GROUPS.get(groups.get(constGroup));
        float top = tabTop + ps(38);
        float footerTop = panel.bottom - ps(14);
        float gap = ps(7);
        float cw = (panel.width() - ps(32) - gap) / 2;
        float chh = (footerTop - top) / 8;
        for (int i = 0; i < list.size(); i++) {
            int col = i % 2, row = i / 2;
            float cx = panel.left + ps(16) + col * (cw + gap);
            float cy = top + row * chh;
            RectF r = new RectF(cx, cy, cx + cw, cy + chh - gap);
            if (r.contains(x, y)) {
                insert(list.get(i).value);
                mode = Mode.CALC;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void drawSettings(Canvas c) {
        boolean en = prefs.settingsEnglish();
        setupPanel(c, en ? "SETTINGS" : "PENGATURAN",
                en ? "INSTRUMENT PREFERENCES" : "PREFERENSI ALAT", 0.86f, 0.90f);
        String[] labels = en
                ? new String[]{"Language", "Theme", "Angle mode", "Number format", "Decimal mark",
                "Digits", "Grouping", "Vibration", "Vibration strength", "Test vibration",
                "Key sound", "Click volume", "Keep awake", "Reset instrument", "About C.O CALCULATOR"}
                : new String[]{"Bahasa", "Tema", "Mode sudut", "Format angka", "Tanda desimal",
                "Jumlah digit", "Pemisah ribuan", "Getar", "Kekuatan getar", "Tes getar",
                "Suara tombol", "Volume klik", "Layar tetap menyala", "Reset alat", "Tentang C.O CALCULATOR"};
        String[] values = {
                en ? "ENGLISH" : "INDONESIA",
                en ? (prefs.theme() == 0 ? "GRAPHITE" : prefs.theme() == 1 ? "SILVER" : "MIDNIGHT")
                        : (prefs.theme() == 0 ? "GRAFIT" : prefs.theme() == 1 ? "PERAK" : "TENGAH MALAM"),
                en ? (prefs.angle() == 0 ? "DEGREES" : prefs.angle() == 1 ? "RADIANS" : "GRADS")
                        : (prefs.angle() == 0 ? "DERAJAT" : prefs.angle() == 1 ? "RADIAN" : "GRADIAN"),
                prefs.format() == 0 ? "AUTO" : prefs.format() == 1 ? "FIX" : prefs.format() == 2 ? "SCI" : "ENG",
                en ? (prefs.decimalComma() ? "COMMA" : "DOT") : (prefs.decimalComma() ? "KOMA" : "TITIK"),
                String.valueOf(prefs.decimals()),
                en ? (prefs.grouping() ? "ON" : "OFF") : (prefs.grouping() ? "AKTIF" : "NONAKTIF"),
                en ? (prefs.haptics() ? "ON" : "OFF") : (prefs.haptics() ? "AKTIF" : "NONAKTIF"),
                prefs.hapticIntensity() + "%",
                en ? "TOUCH TO TEST" : "KETUK UNTUK TES",
                en ? (prefs.sound() ? "ON" : "OFF") : (prefs.sound() ? "AKTIF" : "NONAKTIF"),
                prefs.clickVolume() + "%",
                en ? (prefs.keepAwake() ? "ON" : "OFF") : (prefs.keepAwake() ? "AKTIF" : "NONAKTIF"),
                en ? "TOUCH TO RESET" : "KETUK UNTUK RESET",
                "SC-100 · 2.1"
        };
        float top = panel.top + ps(74);
        float footer = panel.bottom - ps(14);
        float rowH = (footer - top) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float y = top + i * rowH;
            if (i % 2 == 0) card(c, new RectF(panel.left + ps(8), y, panel.right - ps(8), y + rowH - ps(2)), 0x06FFFFFF, 0, ps(5), false);
            text(c, labels[i], panel.left + ps(18), y + rowH * 0.60f, ps(11.0f), 0xFFDCDCE3, medium, Paint.Align.LEFT);
            text(c, values[i], panel.right - ps(20), y + rowH * 0.60f, ps(9.5f),
                    i == 0 ? 0xFFD9B64B : i == 13 ? 0xFFE7A0A0 : 0xFFA99BFF, boldFace, Paint.Align.RIGHT);
            c.drawLine(panel.left + ps(18), y + rowH - ps(1), panel.right - ps(18), y + rowH - ps(1), linePaint(0x14FFFFFF, 0.6f));
        }
    }

    private boolean settingsTouch(float x, float y) {
        float top = panel.top + ps(74);
        float footer = panel.bottom - ps(14);
        int n = 15;
        float rowH = (footer - top) / n;
        int idx = (int) ((y - top) / rowH);
        if (idx < 0 || idx >= n) return true;
        switch (idx) {
            case 0:
                prefs.setSettingsEnglish(!prefs.settingsEnglish());
                break;
            case 1:
                prefs.setTheme((prefs.theme() + 1) % 3);
                theme = Theme.byIndex(prefs.theme());
                buildChrome();
                break;
            case 2: prefs.setAngle((prefs.angle() + 1) % 3); break;
            case 3: prefs.setFormat((prefs.format() + 1) % 4); break;
            case 4:
                prefs.setDecimalComma(!prefs.decimalComma());
                toast(prefs.decimalComma()
                        ? s("Desimal: koma (0,5)", "Decimal: comma (0,5)")
                        : s("Desimal: titik (0.5)", "Decimal: dot (0.5)"));
                break;
            case 5: prefs.setDecimals(nextDecimals()); break;
            case 6: prefs.setGrouping(!prefs.grouping()); break;
            case 7: {
                boolean on = !prefs.haptics();
                prefs.setHaptics(on);
                if (on) {
                    if (vibrator == null || !vibrator.hasVibrator()) {
                        toast(s("Motor getar tidak terdeteksi di perangkat ini", "No vibration motor detected on this device"));
                    } else {
                        vibrateTest();
                        toast(s("Getar aktif — tes pola 1,5 detik", "Vibration on — 1.5-second test pattern"));
                    }
                } else {
                    toast(s("Getar dimatikan", "Vibration off"));
                }
                break;
            }
            case 8: {
                int v = nextHapticIntensity();
                prefs.setHapticIntensity(v);
                if (prefs.haptics()) vibrateTest();
                toast(s("Kekuatan getar: ", "Vibration strength: ") + v + "%");
                break;
            }
            case 9: {
                if (vibrator == null || !vibrator.hasVibrator()) {
                    toast(s("Motor getar tidak terdeteksi di perangkat ini", "No vibration motor detected on this device"));
                } else {
                    if (!prefs.haptics()) prefs.setHaptics(true);
                    vibrateTest();
                    toast(s("Tes getar dikirim — pola 1,5 detik", "Vibration test sent — 1.5-second pattern"));
                }
                break;
            }
            case 10: {
                boolean on = !prefs.sound();
                prefs.setSound(on);
                if (on) playClick();
                toast(on ? s("Suara tombol aktif", "Key sound on") : s("Suara tombol dimatikan", "Key sound off"));
                break;
            }
            case 11: {
                int v = nextClickVolume();
                prefs.setClickVolume(v);
                if (prefs.sound()) playClick();
                toast(s("Volume klik: ", "Click volume: ") + v + "%");
                break;
            }
            case 12: prefs.setKeepAwake(!prefs.keepAwake()); setKeepScreenOn(prefs.keepAwake()); break;
            case 13:
                clearAll();
                memories = new double[10];
                prefs.clearMemories();
                history.clear();
                prefs.clearHistory();
                prefs.setAngle(0); prefs.setFormat(0); prefs.setDecimals(4);
                prefs.setDecimalComma(true);
                prefs.setGrouping(true); prefs.setHaptics(true); prefs.setHapticIntensity(100);
                prefs.setSound(true); prefs.setClickVolume(30);
                prefs.setKeepAwake(true); prefs.setTheme(0);
                theme = Theme.byIndex(0);
                buildChrome();
                break;
            case 14:
                toast(s("C.O CALCULATOR SC-100 · kalkulator ilmiah presisi",
                        "C.O CALCULATOR SC-100 · a precision scientific calculator"));
                break;
            default: break;
        }
        invalidate();
        return true;
    }

    private String s(String id, String en) {
        return prefs.settingsEnglish() ? en : id;
    }

    private int nextHapticIntensity() {
        int v = prefs.hapticIntensity();
        if (v >= 100) return 20;
        if (v >= 80) return 100;
        if (v >= 60) return 80;
        if (v >= 40) return 60;
        return 40;
    }

    private int nextClickVolume() {
        int v = prefs.clickVolume();
        if (v >= 100) return 10;
        if (v >= 70) return 100;
        if (v >= 50) return 70;
        if (v >= 30) return 50;
        return 30;
    }

    private void drawRadix(Canvas c) {
        setupPanel(c, "RADIX VIEW", "CURRENT VALUE IN FOUR BASES", 0.84f, 0.52f);
        double v = currentDouble();
        boolean whole = Math.abs(v - Math.rint(v)) < 1e-9;
        long n = whole ? (long) Math.rint(v) : 0;
        String[] names = {"DECIMAL", "HEXADECIMAL", "OCTAL", "BINARY"};
        String[] values = whole
                ? new String[]{String.valueOf(n), Long.toHexString(n).toUpperCase(), Long.toOctalString(n), Long.toBinaryString(n)}
                : new String[]{"—", "—", "—", "—"};
        float top = panel.top + ps(78);
        float rowH = (panel.bottom - ps(18) - top) / 4;
        for (int i = 0; i < 4; i++) {
            float y = top + i * rowH;
            RectF r = new RectF(panel.left + ps(16), y, panel.right - ps(16), y + rowH - ps(6));
            card(c, r, i == ("dec".equals(radixMode) ? 0 : "hex".equals(radixMode) ? 1 : "oct".equals(radixMode) ? 2 : 3) ? 0x22D9B64B : 0x08FFFFFF,
                    i == ("dec".equals(radixMode) ? 0 : "hex".equals(radixMode) ? 1 : "oct".equals(radixMode) ? 2 : 3) ? 0x99D9B64B : 0x14FFFFFF,
                    ps(7), false);
            text(c, names[i], r.left + ps(10), r.top + ps(17), ps(6.4f), 0xFFA99BFF, boldFace, Paint.Align.LEFT);
            text(c, values[i], r.right - ps(10), r.top + ps(19), ps(8.0f), 0xFFEDEDF2, sans, Paint.Align.RIGHT);
        }
        footerRect.set(panel.left + ps(16), panel.bottom - ps(44), panel.right - ps(16), panel.bottom - ps(12));
        button(c, footerRect, "INSERT DECIMAL VALUE", 0xFFEDEDF2, 0xFF3D3D42, 0x1AFFFFFF);
    }

    private boolean radixTouch(float x, float y) {
        if (footerRect.contains(x, y)) {
            insertValue(BigDecimal.valueOf(currentDouble()));
            mode = Mode.CALC;
            invalidate();
            return true;
        }
        return true;
    }

    private void toast(String s) {
        try { android.widget.Toast.makeText(ctx, s, android.widget.Toast.LENGTH_SHORT).show(); } catch (Exception ignored) {}
    }

    private static final class Noise {
        private static Bitmap bmp;
        static Bitmap get() {
            if (bmp == null) {
                int n = 96;
                bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
                Random r = new Random(7);
                int[] px = new int[n * n];
                for (int i = 0; i < px.length; i++) {
                    int a = 8 + r.nextInt(24);
                    int c = r.nextBoolean() ? 255 : 0;
                    px[i] = (a << 24) | (c << 16) | (c << 8) | c;
                }
                bmp.setPixels(px, 0, n, 0, 0, n, n);
            }
            return bmp;
        }
    }
}



















































































