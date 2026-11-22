#!/usr/bin/env python3
"""Build the COKALKU SC-100 design in FigCraft, in staged headless runs."""
import json, os, re, subprocess, sys

ROOT = r"C:\CO\pemrograman\real calc"
DESIGN = os.path.join(ROOT, "design")
CLI = r"C:\Users\Pongo\AppData\Local\Programs\FigCraft\figcraft-cli.exe"
FILE = os.path.join(DESIGN, "cokalku-sc100.figcraft")

# ---------- palette ----------
OUTER   = "#141416"
BODY_1  = "#404045"
BODY_2  = "#232327"
FACE_1  = "#4A4A50"
FACE_2  = "#303035"
INK     = "#F6F6F8"
MUTED   = "#A7A7AF"
YELLOW  = "#E8BC31"
YELLOW2 = "#A87C0C"
YELLOW_T= "#D9B64B"
RED_1   = "#DA2424"
RED_2   = "#8C0707"
BLUE    = "#A0A0FF"
VIOLET  = "#A99BFF"
LCD_1   = "#B3CDB1"
LCD_2   = "#7E9C81"
LCD_INK = "#111A12"
LCD_DIM = "#354736"

def rgb(hexs, alpha=None):
    return hexs

cmds = []

def add(command, **params):
    cmds.append({"command": command, "params": params})

def rect(x, y, w, h, r=0, fill="#FFFFFF", parent=None, name="rect",
         stroke=None, sw=1.0, stroke_align="inside", grad=None, effects=None,
         opacity=None, rotation=None, clip=None):
    props = {"cornerRadius": r}
    if grad:
        props["fills"] = [grad]
    else:
        props["fill"] = fill
    if stroke:
        props["stroke"] = stroke
        props["strokeWeight"] = sw
        props["strokeAlign"] = stroke_align
    if effects:
        props["effects"] = effects
    if opacity is not None:
        props["opacity"] = opacity
    if rotation is not None:
        props["rotation"] = rotation
    if clip is not None:
        props["clip"] = clip
    add("node.create", type="rectangle", x=x, y=y, width=w, height=h,
        parent=parent, name=name, props=props)

def vgrad(c1, c2, mid=None, y1=0.0, y2=1.0):
    stops = [c1, c2] if mid is None else [c1, mid, c2]
    return {"type": "linearGradient", "stops": stops,
            "from": [0.5, y1], "to": [0.5, y2]}

def hgrad(c1, c2, mid=None):
    stops = [c1, c2] if mid is None else [c1, mid, c2]
    return {"type": "linearGradient", "stops": stops,
            "from": [0.0, 0.5], "to": [1.0, 0.5]}

def diag(c1, c2):
    return {"type": "linearGradient", "stops": [c1, c2],
            "from": [0.0, 0.0], "to": [1.0, 1.0]}

def shadow(color="#000000B0", oy=6, blur=10, spread=0, ox=0):
    return {"type": "dropShadow", "color": color, "offset": [ox, oy],
            "radius": blur, "spread": spread, "blend": "normal", "visible": True}

def inner(color="#00000080", oy=3, blur=6, spread=0, ox=0):
    return {"type": "innerShadow", "color": color, "offset": [ox, oy],
            "radius": blur, "spread": spread, "blend": "normal", "visible": True}

def text(x, y, w, h, s, size=12, color=INK, parent=None, name="text",
         font="Roboto", style="Regular", align="left", valign="middle",
         ls=None, lh=None, opacity=None, case=None, auto=False):
    props = {"fontFamily": font, "fontStyle": style, "fontSize": size,
             "fill": color, "textAlign": align, "textAlignVertical": valign,
             "textAutoResize": "height" if auto else "none"}
    if ls is not None:
        props["letterSpacing"] = ls
    if lh is not None:
        props["lineHeight"] = lh
    if opacity is not None:
        props["opacity"] = opacity
    if case:
        props["textCase"] = case
    add("node.create", type="text", x=x, y=y, width=w, height=h,
        parent=parent, name=name, text=s, props=props)

def noise(density=0.10, size=1.0, color="#FFFFFF16", blend="overlay"):
    return {"type": "noise", "size": size, "density": density,
            "color": color, "blend": blend, "visible": True}

def screw(cx, cy, parent, r=4.2):
    rect(cx-r, cy-r, 2*r, 2*r, r, fill="#0C0C0E", parent=parent, name="screw")
    rect(cx-r+0.8, cy-r+0.8, 2*r-1.6, 2*r-1.6, r-1, fill="#3A3A40",
         parent=parent, name="screw head")
    rect(cx-r+1.6, cy-r+1.6, 2*r-3.2, 2*r-3.2, r-1.6, fill="#1B1B1F",
         parent=parent, name="screw slot", stroke="#66666E", sw=0.4)

def key_label(x, y, w, h, label, size=22, color=INK, style="Regular",
              font="Roboto", align="center", ls=None):
    text(x, y, w, h, label, size=size, color=color, font=font, style=style,
         align=align, valign="middle", ls=ls, parent=FRAME, name="label " + label)

def key(x, y, w, h, label, kind="num", size=28, legend=None, legend_x=None,
        legend_w=None, parent=None, style="Regular", font="Roboto", label_color=None):
    """A physical keycap: extruded base, gradient cap, top sheen, label."""
    if parent is None:
        parent = FRAME
    if kind == "yellow":
        top, bot, txt, border, sheen = "#F2CC42", "#B78810", "#1C1604", "#FFE37A38", "#FFFFFF3A"
    elif kind == "red":
        top, bot, txt, border, sheen = "#E02A2A", "#8E0606", "#FFFFFF", "#FF7A7A40", "#FFFFFF26"
    elif kind == "dark":
        top, bot, txt, border, sheen = "#3D3D42", "#2A2A2E", "#D9D9DF", "#FFFFFF10", "#FFFFFF12"
    else:
        top, bot, txt, border, sheen = "#67676D", "#444449", "#F7F7F9", "#FFFFFF18", "#FFFFFF1C"

    # cast shadow + extruded lower edge
    rect(x + 1, y + 4, w - 2, h, min(12, h * 0.22), fill="#08080A9A",
         parent=parent, name="key shadow", effects=[shadow("#00000070", 3, 5)])
    rect(x, y + 3, w, h, min(12, h * 0.22), fill="#18181B", parent=parent,
         name="key edge", stroke="#0B0B0D", sw=0.6)
    # cap
    rect(x, y, w, h, min(12, h * 0.22), parent=parent, name="key " + label,
         grad=vgrad(top, bot, "#57575D" if kind == "num" else None),
         stroke=border, sw=0.8)
    # top sheen (upper half only)
    rect(x + 1.2, y + 1.2, w - 2.4, h * 0.44, min(11, h * 0.20),
         parent=parent, name="key sheen",
         grad=vgrad(sheen, "#FFFFFF00"), opacity=0.9)
    # inner rim
    rect(x + 1.4, y + 1.4, w - 2.8, h - 2.8, min(11, h * 0.20),
         fill="#FFFFFF00", parent=parent, name="key inner",
         stroke="#00000022", sw=0.6)

    if legend:
        lx = legend_x if legend_x is not None else x
        lw = legend_w if legend_w is not None else w
        lsize = 11.5 if w > 60 else 9.6
        text(lx, y - 12.5, lw, 12, legend, size=lsize, color=YELLOW_T,
             style="Bold", align="center", valign="top", parent=parent,
             name="legend " + legend, ls=0.2, auto=True)
    if label:
        ly = y + (h - size * 0.95) / 2 + 3
        text(x, ly, w, size * 1.35, label,
             size=size, color=(label_color or txt), font=font, style=style, align="center",
             valign="top", parent=parent, name="label " + label, auto=True)

def brand(parent):
    text(44, 21, 160, 10, "COKALKU", size=12, color="#C9C9D1", style="Bold",
         ls=4.2, parent=parent, name="brand")
    text(44, 33, 190, 8, "SCIENTIFIC INSTRUMENT  SC-100", size=5.6,
         color="#85858E", style="Medium", ls=1.35, parent=parent, name="model")
    text(292, 25, 96, 6, "32-DIGIT ENGINE", size=5.0, color="#777780",
         style="Medium", align="right", ls=0.8, parent=parent, name="engine")

def lcd(parent, x=20, y=54, w=372, h=158, expression="π × 392.9 =",
        digits="1234.5678", status_left="DEG", mode="AUTO", show_mem=True):
    # recess shadow
    rect(x + 2, y + 6, w - 4, h, 18, fill="#00000070", parent=parent,
         name="lcd shadow", effects=[shadow("#000000A0", 9, 16)])
    # bezel
    rect(x, y, w, h, 18, parent=parent, name="lcd bezel",
         grad=vgrad("#26262A", "#0B0B0D"), stroke="#74747C", sw=0.8,
         effects=[shadow("#00000090", 7, 12)])
    rect(x + 3, y + 3, w - 6, h - 6, 15, fill="#00000000", parent=parent,
         name="bezel rim", stroke="#000000AA", sw=1.2)
    # recessed screen well
    rect(x + 8, y + 8, w - 16, h - 16, 10, parent=parent, name="well",
         grad=vgrad("#0C0E0D", "#1B1F1C"), stroke="#000000", sw=1.2,
         effects=[inner("#000000C0", 4, 7)])
    # green LCD
    sx, sy, sw, sh = x + 10, y + 10, w - 20, h - 20
    rect(sx, sy, sw, sh, 8, parent=parent, name="lcd",
         grad=vgrad(LCD_1, LCD_2, "#95B197"), stroke="#E7F4E2", sw=0.5,
         effects=[inner("#1D2A1E38", 2, 3), noise(0.05, 0.7, "#FFFFFF10")])
    # glass reflection upper band
    rect(sx + 1, sy + 1, sw - 2, sh * 0.52, 7, parent=parent, name="glass",
         grad=vgrad("#FFFFFF22", "#FFFFFF00", "#FFFFFF08"), opacity=0.75)
    # status strip
    text(sx + 9, sy + 6, 56, 10, status_left, size=8.2, color=LCD_INK,
         style="Bold", ls=1.0, parent=parent, name="status angle")
    text(sx + 66, sy + 6, 80, 10, "12 DIGIT", size=5.8, color=LCD_DIM,
         style="Medium", ls=0.7, parent=parent, name="status digits")
    text(sx + sw - 82, sy + 6, 73, 10, mode, size=5.8, color=LCD_DIM,
         style="Medium", align="right", ls=0.7, parent=parent, name="status mode")
    # expression
    text(sx + 9, sy + 20, sw - 18, 11, expression, size=7.8, color=LCD_DIM,
         style="Medium", align="left", valign="top", parent=parent, name="expression", auto=True)
    # main result
    text(sx + 8, sy + 41, sw - 16, 60, digits, size=35, color=LCD_INK,
         font="DSEG7 Classic", style="Bold Italic", align="right",
         valign="top", parent=parent, name="display", ls=-0.5, auto=True)
    # bottom indicators
    if show_mem:
        text(sx + 9, sy + sh - 15, 24, 10, "M", size=7.4, color=LCD_INK,
             style="Bold", parent=parent, name="mem indicator")
    text(sx + 36, sy + sh - 15, 52, 10, "2nd", size=7.0, color="#4A5F4B",
         style="Medium", parent=parent, name="second indicator")
    text(sx + sw - 100, sy + sh - 15, 91, 10, "ENGINE READY", size=5.2,
         color=LCD_DIM, style="Medium", align="right", ls=0.6,
         parent=parent, name="engine ready")

def calculator_body(parent, w=412, h=915):
    rect(0, 0, w, h, 36, parent=parent, name="body shadow",
         fill="#08080A", effects=[shadow("#000000C0", 10, 24)])
    rect(0, 0, w, h, 36, parent=parent, name="body",
         grad=vgrad("#3C3C41", "#1C1C1F", "#2C2C31"), stroke="#57575E", sw=1.0)
    # machined side rails
    rect(1.2, 1.2, w - 2.4, h - 2.4, 35, parent=parent, name="body rim",
         fill="#FFFFFF00", stroke="#7B7B8428", sw=1.1)
    # face plate
    rect(9, 9, w - 18, h - 18, 29, parent=parent, name="face",
         grad=vgrad(FACE_1, FACE_2, "#3B3B40"), stroke="#101012", sw=1.4,
         effects=[noise(0.08, 1.1, "#FFFFFF12")])
    rect(12.5, 12.5, w - 25, h - 25, 26, parent=parent, name="face inner",
         grad=vgrad("#45454B", "#323237"), stroke="#FFFFFF10", sw=0.7,
         effects=[noise(0.05, 0.8, "#00000014", "multiply")])
    # bevel highlights
    rect(10.5, 10.5, w - 21, 2.2, 1, parent=parent, name="top bevel",
         grad=hgrad("#FFFFFF00", "#FFFFFF42", "#FFFFFF00"))
    rect(10.5, h - 12.7, w - 21, 2.2, 1, parent=parent, name="bottom bevel",
         grad=hgrad("#00000000", "#00000066", "#00000000"))
    # corner screws
    for cx, cy in [(24, 26), (w - 24, 26), (24, h - 26), (w - 24, h - 26)]:
        screw(cx, cy, parent, 4.0)

def calculator_keys(parent):
    # utility strip
    ux, uy, uw, uh, ug = 18, 226, 376, 26, 7.0
    ucw = (uw - 5 * ug) / 6
    utils = ["☰", "DRG▸", "TAB", "RCL", "STO", "M−"]
    for i, s in enumerate(utils):
        key(ux + i * (ucw + ug), uy, ucw, uh, s, kind="dark", size=8.4)

    # function block
    fx, fy, fw, fh, fg = 18, 266, 376, 58, 8.0
    fcw = (fw - 5 * fg) / 6
    funcs = [
        [("SHIFT", "", "yellow", 12.5), ("DRG", "", "num", 15),
         ("FSE", "", "num", 15), ("MR", "", "num", 15),
         ("MS", "", "num", 15), ("M+", "", "num", 15)],
        [("hyp", "hyp⁻¹", "num", 16), ("sin", "sin⁻¹", "num", 19),
         ("cos", "cos⁻¹", "num", 19), ("tan", "tan⁻¹", "num", 19),
         ("ln", "eˣ", "num", 19), ("log", "10ˣ", "num", 19)],
        [("yˣ", "ˣ√y", "num", 19), ("√", "∛", "num", 22),
         ("x²", "x³", "num", 18), ("%", "1/x", "num", 20),
         ("(", "", "num", 23), (")", "", "num", 23)],
    ]
    for r, row in enumerate(funcs):
        y = fy + r * (fh + 14)
        for i, (label, leg, kind, fs) in enumerate(row):
            x = fx + i * (fcw + fg)
            style = "Bold" if label == "SHIFT" else "Regular"
            lc = BLUE if label in ("MR", "MS", "M+") else None
            key(x, y, fcw, fh, label, kind=kind, size=fs,
                legend=leg if leg else None, style=style, label_color=lc)

    # numeric block
    nx, ny0, nw, nh, ng = 18, 496, 376, 80, 9.0
    ncw = (nw - 4 * ng) / 5
    rows = [
        [("7", "n!", "num"), ("8", "nCr", "num"), ("9", "nPr", "num"),
         ("DEL", "DEC", "red"), ("AC", "HEX", "red")],
        [("4", "π", "num"), ("5", "e", "num"), ("6", "OCT", "num"),
         ("×", "BIN", "num"), ("÷", "", "num")],
        [("1", "CONV", "num"), ("2", "CNST", "num"), ("3", "MOD", "num"),
         ("+", "", "num"), ("−", "", "num")],
        [("0", "", "num"), ("±", "", "num"), (".", "", "num"),
         ("EXP", "", "num"), ("=", "", "num")],
    ]
    for r, row in enumerate(rows):
        y = ny0 + r * (nh + 14)
        for i, (label, leg, kind) in enumerate(row):
            x = nx + i * (ncw + ng)
            if label in ("DEL", "AC"):
                fs, style = 20, "Bold"
            elif label == "EXP":
                fs, style = 16, "Bold"
            elif label == "=":
                fs, style = 34, "Light"
            elif label in ("×", "÷", "+", "−", "±"):
                fs, style = 31, "Light"
            else:
                fs, style = 31, "Regular"
            lc = BLUE if label in ("MR", "MS", "M+") else None
            key(x, y, ncw, nh, label, kind=kind, size=fs,
                legend=leg if leg else None, style=style, label_color=lc)

    # footer
    text(18, 875, 210, 8, "COKALKU · PRECISION INSTRUMENTS", size=5.2,
         color="#85858E", style="Medium", ls=0.8, parent=parent, name="footer left")
    text(290, 875, 104, 8, "SC–100  ·  12 DIGIT", size=5.2,
         color="#85858E", style="Medium", align="right", ls=0.8,
         parent=parent, name="footer right")
    # speaker slots
    for i in range(7):
        rect(176 + i * 9, 884, 4, 4, 2, parent=parent,
             name="speaker", fill="#101012", stroke="#5C5C64", sw=0.4)

def draw_calculator(parent, w=412, h=915, expression="π × 392.9 =",
                    digits="1234.5678", dim=0.0, history_peek=False):
    calculator_body(parent, w, h)
    brand(parent)
    lcd(parent, expression=expression, digits=digits)
    calculator_keys(parent)
    if dim:
        rect(0, 0, w, h, 36, parent=parent, name="dim", fill="#07070A", opacity=dim)

# ---------- frame 2: history ----------
def history_screen(parent):
    draw_calculator(parent, expression="π × 392.9 =", digits="1234.5678", dim=0.62)
    px, py, pw, ph = 38, 172, 336, 574
    rect(px + 2, py + 10, pw - 4, ph, 18, fill="#00000088", parent=parent,
         name="panel shadow", effects=[shadow("#000000C0", 14, 30)])
    rect(px, py, pw, ph, 16, parent=parent, name="history panel",
         grad=vgrad("#303035", "#232327"), stroke="#787882", sw=0.9,
         effects=[noise(0.04, 0.8, "#FFFFFF10")])
    rect(px + 1.5, py + 1.5, pw - 3, 62, 14, parent=parent, name="panel header",
         fill="#FFFFFF06")
    text(px + 18, py + 18, 230, 18, "RESULT HISTORY", size=12.2, color="#D6D6DE",
         style="Bold", ls=2.6, parent=parent, name="history title")
    text(px + 18, py + 35, 230, 9, "50 RESULTS · MOST RECENT FIRST", size=5.2,
         color=VIOLET, style="Medium", ls=0.7, parent=parent, name="history sub")
    # close key
    key(px + pw - 44, py + 18, 26, 26, "×", kind="dark", size=15)
    # rows
    values = ["1234.5678", "6.02214076", "0.0001", "42", "3.14159265",
              "299792458", "9.99999999", "1.61803399", "0.70710678", "2.71828182"]
    labels = ["π × 392.9", "6.022 × 10²³", "1 ÷ 10000", "6 × 7", "π", "c (m/s)",
              "10 − 1e-8", "φ", "sin 45°", "e"]
    ry = py + 76
    rh = 44
    for i, (v, lab) in enumerate(zip(values, labels)):
        y = ry + i * rh
        if i == 1:
            rect(px + 8, y, pw - 16, rh, 6, parent=parent, name="row hi",
                 fill="#FFFFFF07")
        text(px + 18, y + 8, 22, 10, f"{i+1:02d}", size=7.6, color=VIOLET,
             style="Bold", parent=parent, name="row index")
        text(px + 44, y + 4, 174, 26, v, size=17.5, color="#EDEDF2",
             font="DSEG7 Classic", style="Bold Italic", parent=parent,
             name="row value")
        text(px + 44, y + 27, 190, 9, lab, size=5.6, color="#93939C",
             style="Medium", parent=parent, name="row label")
        text(px + pw - 50, y + 31, 34, 8, "→", size=8, color="#6E6E78",
             align="right", parent=parent, name="row insert")
        rect(px + 16, y + rh - 0.5, pw - 32, 0.6, 0, parent=parent,
             name="row divider", fill="#FFFFFF10")
    # footer
    by = py + ph - 52
    rect(px + 16, by, pw - 32, 36, 8, parent=parent, name="clear all",
         grad=vgrad("#3D3D42", "#2A2A2E"), stroke="#B05A5A80", sw=0.8)
    text(px + 16, by, pw - 32, 36, "CLEAR ALL", size=8.4, color="#E7A0A0",
         style="Bold", align="center", ls=2.0, parent=parent, name="clear label")

# ---------- frame 3: converter ----------
def converter_screen(parent):
    draw_calculator(parent, expression="CONVERT", digits="1", dim=0.52)
    px, py, pw, ph = 20, 86, 372, 742
    rect(px + 2, py + 10, pw - 4, ph, 20, fill="#00000088", parent=parent,
         name="panel shadow", effects=[shadow("#000000C0", 14, 30)])
    rect(px, py, pw, ph, 18, parent=parent, name="convert panel",
         grad=vgrad("#313136", "#242428"), stroke="#787882", sw=0.9,
         effects=[noise(0.035, 0.8, "#FFFFFF10")])
    text(px + 18, py + 18, 250, 18, "UNIT CONVERTER", size=12.0, color="#D6D6DE",
         style="Bold", ls=2.4, parent=parent, name="convert title")
    text(px + 18, py + 34, 250, 8, "9 CATEGORIES · 84 UNITS", size=5.0,
         color=VIOLET, style="Medium", ls=0.8, parent=parent, name="convert sub")
    key(px + pw - 44, py + 18, 26, 26, "×", kind="dark", size=15)

    # category chips
    cats = ["LENGTH", "MASS", "TEMP", "AREA", "SPEED"]
    cw = 64
    for i, c in enumerate(cats):
        x = px + 16 + i * (cw + 4)
        active = i == 0
        rect(x, py + 58, cw, 25, 6, parent=parent, name="chip " + c,
             fill=YELLOW if active else "#FFFFFF08",
             stroke=YELLOW if active else "#FFFFFF18", sw=0.7)
        text(x, py + 58, cw, 25, c, size=6.2, color="#211B06" if active else "#C9C9D2",
             style="Bold" if active else "Medium", align="center", ls=0.7,
             parent=parent, name="chip label")

    # from card
    fy = py + 96
    rect(px + 16, fy, pw - 32, 100, 10, parent=parent, name="from card",
         grad=vgrad("#3A3A40", "#2B2B30"), stroke="#FFFFFF18", sw=0.7)
    text(px + 28, fy + 9, 80, 8, "FROM", size=5.6, color=VIOLET, style="Bold",
         ls=1.4, parent=parent, name="from label")
    text(px + 24, fy + 32, 220, 44, "1.0", size=27, color="#F4F4F8",
         font="DSEG7 Classic", style="Bold Italic", align="left",
         parent=parent, name="from value")
    rect(px + pw - 42, fy + 30, 2, 42, 1, parent=parent, name="caret", fill=YELLOW)
    text(px + 28, fy + 78, 160, 12, "metre", size=9.2, color="#D2D2DA",
         style="Medium", parent=parent, name="from unit")
    text(px + pw - 92, fy + 78, 64, 12, "m", size=9.2, color="#8E8E98",
         style="Regular", align="right", parent=parent, name="from symbol")

    # swap button
    cy = fy + 112
    rect(px + pw / 2 - 18, cy, 36, 36, 18, parent=parent, name="swap circle",
         grad=vgrad("#5A5A61", "#3A3A40"), stroke="#8A8A94", sw=0.8,
         effects=[shadow("#00000080", 3, 6)])
    text(px + pw / 2 - 18, cy, 36, 36, "⇅", size=15, color="#E8E8EE",
         align="center", parent=parent, name="swap glyph")

    # to card
    ty = cy + 44
    rect(px + 16, ty, pw - 32, 92, 10, parent=parent, name="to card",
         grad=vgrad("#3A4A3C", "#263128"), stroke="#B9D6B0", sw=1.0,
         effects=[inner("#FFFFFF12", 1, 2)])
    text(px + 28, ty + 9, 80, 8, "TO", size=5.6, color="#B7D5AE", style="Bold",
         ls=1.4, parent=parent, name="to label")
    text(px + 24, ty + 21, pw - 48, 48, "3.28084", size=30, color="#DCEBD7",
         font="DSEG7 Classic", style="Bold Italic", align="right",
         parent=parent, name="to value")
    text(px + 28, ty + 70, 160, 12, "foot", size=9.2, color="#D2E2CD",
         style="Medium", parent=parent, name="to unit")
    text(px + pw - 92, ty + 70, 64, 12, "ft", size=9.2, color="#9BB295",
         style="Regular", align="right", parent=parent, name="to symbol")

    # quick list
    qy = ty + 108
    text(px + 18, qy, 180, 10, "COMMON LENGTH UNITS", size=5.4, color="#8E8E98",
         style="Bold", ls=1.1, parent=parent, name="quick title")
    units = [("millimetre", "1 000", "mm"), ("centimetre", "100", "cm"),
             ("kilometre", "0.001", "km"), ("inch", "39.3701", "in"),
             ("yard", "1.09361", "yd"), ("mile", "0.000621371", "mi"),
             ("nautical mile", "0.000539957", "nmi"), ("light year", "1.057e-16", "ly"),
             ("furlong", "0.00497097", "fur"), ("astronomical unit", "6.6846e-12", "au")]
    uy = qy + 14
    for i, (name, val, sym) in enumerate(units):
        col = i % 2
        row = i // 2
        x = px + 16 + col * ((pw - 32) / 2 + 4)
        y = uy + row * 54
        w = (pw - 32) / 2 - 4
        rect(x, y, w, 48, 8, parent=parent, name="unit row " + name,
             fill="#FFFFFF06" if i != 2 else "#FFFFFF10",
             stroke="#FFFFFF12" if i != 2 else "#D9B64B66", sw=0.7)
        text(x + 10, y + 8, w - 20, 10, name, size=7.6, color="#DCDCE3",
             style="Medium", parent=parent, name="unit name")
        text(x + 10, y + 24, w - 20, 14, val, size=8.8, color="#9D9DA7",
             font="Roboto", style="Regular", parent=parent,
             name="unit value")
        text(x + w - 34, y + 8, 26, 10, sym, size=6.2, color=VIOLET,
             style="Bold", align="right", parent=parent, name="unit sym")
    # footer note
    text(px + 16, py + ph - 30, pw - 32, 12,
         "TAP A UNIT TO INSERT THE CONVERTED VALUE", size=5.0, color="#7F7F89",
         style="Medium", align="center", ls=0.8, parent=parent, name="convert note")

Lcd_dark = "#DCEBD7"

# ---------- frame 4: landscape ----------
def landscape_screen(parent):
    w, h = 915, 412
    rect(0, 0, w, h, 28, parent=parent, name="body shadow", fill="#08080A",
         effects=[shadow("#000000C0", 10, 24)])
    rect(0, 0, w, h, 28, parent=parent, name="body",
         grad=vgrad("#3C3C41", "#1C1C1F"), stroke="#57575E", sw=1.0)
    rect(8, 8, w - 16, h - 16, 23, parent=parent, name="face",
         grad=vgrad("#46464C", "#303035"), stroke="#101012", sw=1.3,
         effects=[noise(0.06, 1.0, "#FFFFFF12")])
    for cx, cy in [(24, 24), (w - 24, 24), (24, h - 24), (w - 24, h - 24)]:
        screw(cx, cy, parent, 3.6)
    text(44, 18, 140, 10, "COKALKU", size=11, color="#C9C9D1", style="Bold",
         ls=3.6, parent=parent, name="brand")
    text(44, 29, 180, 7, "SC-100 · LANDSCAPE CONSOLE", size=4.8,
         color="#85858E", style="Medium", ls=1.2, parent=parent, name="model")
    # LCD
    lx, ly, lw, lh = 44, 48, 827, 98
    rect(lx + 2, ly + 5, lw - 4, lh, 13, fill="#00000070", parent=parent,
         name="lcd shadow", effects=[shadow("#000000A0", 8, 14)])
    rect(lx, ly, lw, lh, 13, parent=parent, name="lcd bezel",
         grad=vgrad("#26262A", "#0B0B0D"), stroke="#74747C", sw=0.8)
    sx, sy, sw, sh = lx + 8, ly + 8, lw - 16, lh - 16
    rect(sx, sy, sw, sh, 7, parent=parent, name="lcd",
         grad=vgrad(LCD_1, LCD_2), stroke="#E7F4E2", sw=0.5)
    rect(sx + 1, sy + 1, sw - 2, sh * 0.5, 6, parent=parent, name="glass",
         grad=vgrad("#FFFFFF22", "#FFFFFF00"), opacity=0.7)
    text(sx + 10, sy + 4, 60, 9, "DEG", size=7.4, color=LCD_INK, style="Bold",
         ls=1.0, parent=parent, name="status angle")
    text(sx + 68, sy + 4, 100, 9, "LANDSCAPE · AUTO", size=5.2, color=LCD_DIM,
         style="Medium", ls=0.6, parent=parent, name="status mode")
    text(sx + 10, sy + 16, sw - 20, 12, "π × 392.9 =", size=7.6,
         color=LCD_DIM, style="Medium", align="left", valign="top",
         parent=parent, name="expression", auto=True)
    text(sx + 10, sy + 30, sw - 20, 46, "1234.5678", size=30, color=LCD_INK,
         font="DSEG7 Classic", style="Bold Italic", align="right",
         valign="top", parent=parent, name="display", auto=True)
    text(sx + 10, sy + sh - 10, 40, 8, "M", size=6.4, color=LCD_INK,
         style="Bold", parent=parent, name="mem indicator")
    text(sx + sw - 100, sy + sh - 10, 90, 8, "32-DIGIT ENGINE", size=5.0,
         color=LCD_DIM, style="Medium", align="right", ls=0.6,
         parent=parent, name="engine")

    # 10 x 5 keypad
    kx, ky, kw, kh, gap = 44, 164, 827, 42, 6.0
    cw = (kw - 9 * gap) / 10
    row1 = [("SHIFT", "yellow"), ("DRG", "num"), ("FSE", "num"), ("MR", "num"),
            ("MS", "num"), ("M+", "num"), ("hyp", "num"), ("sin", "num"),
            ("cos", "num"), ("tan", "num")]
    row2 = [("yˣ", "num"), ("√", "num"), ("x²", "num"), ("%", "num"),
            ("(", "num"), (")", "num"), ("ln", "num"), ("log", "num"),
            ("π", "num"), ("e", "num")]
    row3 = [("7", "num"), ("8", "num"), ("9", "num"), ("DEL", "red"),
            ("AC", "red"), ("4", "num"), ("5", "num"), ("6", "num"),
            ("×", "num"), ("÷", "num")]
    row4 = [("1", "num"), ("2", "num"), ("3", "num"), ("+", "num"),
            ("−", "num"), ("0", "num"), ("±", "num"), (".", "num"),
            ("EXP", "num"), ("=", "num")]
    row5 = [("CONV", "dark"), ("CNST", "dark"), ("MOD", "dark"),
            ("ANS", "dark"), ("STO", "dark"), ("RCL", "dark"),
            ("DEC", "dark"), ("HEX", "dark"), ("OCT", "dark"),
            ("BIN", "dark")]
    for r, row in enumerate([row1, row2, row3, row4, row5]):
        y = ky + r * (kh + 6)
        for i, (label, kind) in enumerate(row):
            x = kx + i * (cw + gap)
            if label in ("DEL", "AC"):
                fs, style = 15, "Bold"
            elif label in ("SHIFT",):
                fs, style = 10.5, "Bold"
            elif label in ("CONV", "CNST", "MOD", "ANS", "STO", "RCL", "DEC", "HEX", "OCT", "BIN"):
                fs, style = 7.6, "Medium"
            elif label in ("×", "÷", "+", "−", "±", "="):
                fs, style = 22, "Light"
            elif label in ("EXP",):
                fs, style = 11, "Bold"
            else:
                fs, style = 19, "Regular"
            key(x, y, cw, kh, label, kind=kind, size=fs, style=style)

# ---------- run helpers ----------
def run_script(name, script, out=FILE, input_file=None):
    path = os.path.join(DESIGN, name)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(script, f, ensure_ascii=False, indent=1)
    args = [CLI, "run"]
    if input_file:
        args += ["--in", input_file]
    args += [path, "--out", out]
    p = subprocess.run(args, capture_output=True, text=True, encoding="utf-8")
    print(p.stdout[-4000:])
    if p.returncode != 0:
        print(p.stderr[-4000:], file=sys.stderr)
        raise SystemExit(p.returncode)
    return p.stdout

def main():
    # Stage 1: frames
    script = [
        {"command": "file.new", "params": {"name": "COKALKU SC-100"}},
        {"command": "page.setBackground", "params": {"color": "#17181A"}},
    ]
    frames = [
        ("01 · Calculator", 0, 0, 412, 915),
        ("02 · Result History", 462, 0, 412, 915),
        ("03 · Unit Converter", 924, 0, 412, 915),
        ("04 · Landscape", 1386, 0, 915, 412),
    ]
    for name, x, y, w, h in frames:
        script.append({"command": "frame.create", "params": {
            "x": x, "y": y, "width": w, "height": h, "name": name,
            "props": {"fill": "#1D1E21", "cornerRadius": 36 if w < 500 else 28,
                      "clip": True}}})
    out = run_script("stage1.json", script)
    ids = [json.loads(line)["result"]["id"] for line in out.splitlines()
           if '"ok":true' in line and '"frame.create"' in line]
    print("frame ids", ids)
    # save id map for later
    with open(os.path.join(DESIGN, "frame_ids.json"), "w") as f:
        json.dump(dict(zip([f[0] for f in frames], ids)), f, indent=1)

    # Stage 2: build each frame
    global cmds, FRAME
    FRAME = ids[0]
    cmds = []
    draw_calculator(FRAME)
    run_script("stage2_main.json", cmds, input_file=FILE)

    FRAME = ids[1]
    cmds = []
    history_screen(FRAME)
    run_script("stage2_history.json", cmds, input_file=FILE)

    FRAME = ids[2]
    cmds = []
    converter_screen(FRAME)
    run_script("stage2_converter.json", cmds, input_file=FILE)

    FRAME = ids[3]
    cmds = []
    landscape_screen(FRAME)
    run_script("stage2_landscape.json", cmds, input_file=FILE)

    print("done", FILE)

if __name__ == "__main__":
    main()












