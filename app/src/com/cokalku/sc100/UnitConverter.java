package com.cokalku.sc100;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Unit conversion catalogue and maths. */
public final class UnitConverter {
    public static final class Unit {
        public final String name;
        public final String symbol;
        public final double scale;
        public final double offset; // base = value * scale + offset
        public Unit(String name, String symbol, double scale, double offset) {
            this.name = name; this.symbol = symbol; this.scale = scale; this.offset = offset;
        }
        public double toBase(double v) { return v * scale + offset; }
        public double fromBase(double b) { return (b - offset) / scale; }
    }

    public static final class Category {
        public final String name;
        public final List<Unit> units = new ArrayList<>();
        Category(String name) { this.name = name; }
        Category add(String n, String s, double f) { units.add(new Unit(n, s, f, 0)); return this; }
        Category addAffine(String n, String s, double f, double o) { units.add(new Unit(n, s, f, o)); return this; }
    }

    public static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();

    static {
        Category length = new Category("LENGTH")
                .add("metre", "m", 1)
                .add("kilometre", "km", 1000)
                .add("centimetre", "cm", 0.01)
                .add("millimetre", "mm", 0.001)
                .add("micrometre", "µm", 1e-6)
                .add("nanometre", "nm", 1e-9)
                .add("mile", "mi", 1609.344)
                .add("yard", "yd", 0.9144)
                .add("foot", "ft", 0.3048)
                .add("inch", "in", 0.0254)
                .add("nautical mile", "nmi", 1852)
                .add("light year", "ly", 9.4607304725808e15);
        CATEGORIES.put(length.name, length);

        Category mass = new Category("MASS")
                .add("kilogram", "kg", 1)
                .add("gram", "g", 0.001)
                .add("milligram", "mg", 1e-6)
                .add("tonne", "t", 1000)
                .add("pound", "lb", 0.45359237)
                .add("ounce", "oz", 0.028349523125)
                .add("stone", "st", 6.35029318)
                .add("carat", "ct", 0.0002);
        CATEGORIES.put(mass.name, mass);

        Category temp = new Category("TEMP")
                .addAffine("Celsius", "°C", 1, 273.15)
                .addAffine("Fahrenheit", "°F", 5.0 / 9.0, 273.15 - 32 * 5.0 / 9.0)
                .addAffine("Kelvin", "K", 1, 0);
        CATEGORIES.put(temp.name, temp);

        Category area = new Category("AREA")
                .add("square metre", "m²", 1)
                .add("square kilometre", "km²", 1e6)
                .add("square centimetre", "cm²", 1e-4)
                .add("hectare", "ha", 1e4)
                .add("acre", "ac", 4046.8564224)
                .add("square foot", "ft²", 0.09290304)
                .add("square inch", "in²", 0.00064516)
                .add("square yard", "yd²", 0.83612736)
                .add("square mile", "mi²", 2589988.110336);
        CATEGORIES.put(area.name, area);

        Category volume = new Category("VOLUME")
                .add("litre", "L", 1)
                .add("millilitre", "mL", 0.001)
                .add("cubic metre", "m³", 1000)
                .add("cubic centimetre", "cm³", 0.001)
                .add("US gallon", "gal", 3.785411784)
                .add("US quart", "qt", 0.946352946)
                .add("US pint", "pt", 0.473176473)
                .add("US cup", "cup", 0.2365882365)
                .add("fluid ounce", "fl oz", 0.0295735295625)
                .add("tablespoon", "tbsp", 0.01478676478125);
        CATEGORIES.put(volume.name, volume);

        Category speed = new Category("SPEED")
                .add("metre/second", "m/s", 1)
                .add("kilometre/hour", "km/h", 1.0 / 3.6)
                .add("mile/hour", "mph", 0.44704)
                .add("knot", "kn", 0.514444444444)
                .add("foot/second", "ft/s", 0.3048)
                .add("mach", "Ma", 340.29);
        CATEGORIES.put(speed.name, speed);

        Category time = new Category("TIME")
                .add("second", "s", 1)
                .add("minute", "min", 60)
                .add("hour", "h", 3600)
                .add("day", "d", 86400)
                .add("week", "wk", 604800)
                .add("year", "yr", 31557600)
                .add("millisecond", "ms", 0.001);
        CATEGORIES.put(time.name, time);

        Category data = new Category("DATA")
                .add("byte", "B", 1)
                .add("bit", "b", 0.125)
                .add("kilobyte", "kB", 1000)
                .add("kibibyte", "KiB", 1024)
                .add("megabyte", "MB", 1e6)
                .add("mebibyte", "MiB", 1048576)
                .add("gigabyte", "GB", 1e9)
                .add("gibibyte", "GiB", 1073741824)
                .add("terabyte", "TB", 1e12)
                .add("tebibyte", "TiB", 1099511627776.0);
        CATEGORIES.put(data.name, data);

        Category energy = new Category("ENERGY")
                .add("joule", "J", 1)
                .add("kilojoule", "kJ", 1000)
                .add("calorie", "cal", 4.184)
                .add("kilocalorie", "kcal", 4184)
                .add("watt-hour", "Wh", 3600)
                .add("kilowatt-hour", "kWh", 3.6e6)
                .add("BTU", "BTU", 1055.05585262)
                .add("electronvolt", "eV", 1.602176634e-19);
        CATEGORIES.put(energy.name, energy);

        Category pressure = new Category("PRESSURE")
                .add("pascal", "Pa", 1)
                .add("kilopascal", "kPa", 1000)
                .add("bar", "bar", 100000)
                .add("atmosphere", "atm", 101325)
                .add("psi", "psi", 6894.757293168)
                .add("torr", "Torr", 133.322368421);
        CATEGORIES.put(pressure.name, pressure);
    }

    public static List<String> names() {
        return new ArrayList<>(CATEGORIES.keySet());
    }

    public static double convert(Category c, int from, int to, double value) {
        if (c == null || c.units.isEmpty()) return value;
        from = Math.max(0, Math.min(c.units.size() - 1, from));
        to = Math.max(0, Math.min(c.units.size() - 1, to));
        return c.units.get(to).fromBase(c.units.get(from).toBase(value));
    }

    public static String formatValue(double v) {
        return formatValue(v, false);
    }

    public static String formatValue(double v, boolean decimalComma) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        if (v == 0) return "0";
        java.math.BigDecimal bd = java.math.BigDecimal.valueOf(v)
                .round(new java.math.MathContext(7, java.math.RoundingMode.HALF_UP));
        String s = Formatter.format(bd, Formatter.Mode.AUTO, 7, false, decimalComma);
        return s.replace("E", "e");
    }
}




