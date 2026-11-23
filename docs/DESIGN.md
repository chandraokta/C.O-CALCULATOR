# C.O CALCULATOR SC-100 — design notes

## Canvas

| | Portrait | Landscape |
|---|---|---|
| Frame | 412 × 915 dp | 915 × 412 dp |
| Outer body | 0,0–412,915, r 36 | 0,0–915,412, r 28 |
| Face plate | 9,9–403,906, r 29 | 8,8–907,404, r 23 |
| LCD module | 20,54–392,212 | 44,48–871,146 |
| LCD screen | 30,64–382,202 | 52,56–863,138 |
| Key block | x 18–394; utility y 226; scientific y 266–464; numeric y 496–858 | x 44–871; 10 × 5 grid, y 164–398 |

The app computes these positions proportionally from the real screen size. Text scales from
`S = min(width/412, height/915)` so the instrument keeps its proportions on tall phones.

## Palette

| Token | Value | Use |
|---|---|---|
| `bodyTop` | `#3C3C41` | upper rail |
| `bodyMid` | `#2C2C31` | centre rail |
| `bodyBottom` | `#1C1C1F` | lower rail |
| `faceTop` | `#4A4A50` | face plate light |
| `faceBottom` | `#303035` | face plate shade |
| `keyTop` | `#67676D` | key cap light |
| `keyMid` | `#57575D` | key cap middle |
| `keyBottom` | `#444449` | key cap shade |
| `keyEdge` | `#18181B` | extruded lower edge |
| `keyText` | `#F7F7F9` | key legends |
| `lcdTop` | `#B3CDB1` | LCD backlight |
| `lcdMid` | `#95B197` | LCD middle |
| `lcdBottom` | `#7E9C81` | LCD lower |
| `lcdInk` | `#111A12` | seven-segment digits |
| `lcdDim` | `#354736` | expression / status |
| `shift` | `#F2CC42 → #B78810` | SHIFT key |
| `clear` | `#E02A2A → #8E0606` | DEL / AC |
| `memory` | `#A0A0FF` | MR / MS / M+ |
| `legend` | `#D9B64B` | secondary legends |
| `accent` | `#A99BFF` | dialog titles, indexes |

## Type

- **Display:** DSEG7 Classic Bold Italic. Real seven-segment geometry, with a slight italic
  lean; digits are redrawn at a size that fits the LCD.
- **Labels:** Roboto / Android `sans-serif`; light weight for large digits and operators,
  medium for legends, bold for SHIFT/DEL/AC.
- **Dialog titles:** Roboto Bold with 0.12 em tracking.
- **Scale:** base sizes are in design dp; the app multiplies by `S`. Key labels are centred
  with `FontMetrics` and legends sit in the gap above each key.

## Physical details

- Each key has three layers: a soft cast shadow, a 3 dp darker extrusion, and a vertical
  gradient cap. A 1 px top sheen and a low-alpha rim complete the bevel.
- The face has a subtle repeating noise texture (a 96 × 96 generated bitmap) and a 2 px
  gradient bevel at top and bottom.
- Four hex-slot screws sit inside the face plate.
- The LCD has a recessed black well, a green screen, a 30% white glass band, 8% scanlines,
  and a fine light rim.
- Overlays are dark machined panels with a 62% scrim, a 1 px cool highlight and a purple
  index/title accent.

## FigCraft source

`design/cokalku-sc100.figcraft` contains four frames:

1. `01 · Calculator`
2. `02 · Result History`
3. `03 · Unit Converter`
4. `04 · Landscape`

`design/build_design.py` is the repeatable generator. It writes staged JSON command scripts,
runs `figcraft-cli run`, and exports with `figcraft-cli export --scale 2`.


