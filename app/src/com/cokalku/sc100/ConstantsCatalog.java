package com.cokalku.sc100;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Physical and mathematical constants shown by the CNST dialog. */
public final class ConstantsCatalog {
    public static final class Constant {
        public final String name;
        public final String symbol;
        public final String value;
        public final String unit;
        public Constant(String n, String s, String v, String u) {
            name = n; symbol = s; value = v; unit = u;
        }
    }

    public static final Map<String, List<Constant>> GROUPS = new LinkedHashMap<>();

    static {
        List<Constant> math = new ArrayList<>();
        math.add(new Constant("Pi", "π", "3.14159265358979323846264338327950", ""));
        math.add(new Constant("Euler's number", "e", "2.71828182845904523536028747135266", ""));
        math.add(new Constant("Golden ratio", "φ", "1.61803398874989484820458683436564", ""));
        math.add(new Constant("Pythagoras", "√2", "1.41421356237309504880168872420970", ""));
        math.add(new Constant("Natural log of 2", "ln 2", "0.69314718055994530941723212145818", ""));
        math.add(new Constant("Natural log of 10", "ln 10", "2.30258509299404568401799145468436", ""));
        GROUPS.put("MATHEMATICS", math);

        List<Constant> phys = new ArrayList<>();
        phys.add(new Constant("Speed of light", "c", "299792458", "m/s"));
        phys.add(new Constant("Gravitational constant", "G", "6.67430E-11", "m³/kg·s²"));
        phys.add(new Constant("Planck constant", "h", "6.62607015E-34", "J·s"));
        phys.add(new Constant("Reduced Planck", "ħ", "1.054571817E-34", "J·s"));
        phys.add(new Constant("Boltzmann constant", "k", "1.380649E-23", "J/K"));
        phys.add(new Constant("Avogadro constant", "Nₐ", "6.02214076E23", "1/mol"));
        phys.add(new Constant("Gas constant", "R", "8.314462618", "J/mol·K"));
        phys.add(new Constant("Elementary charge", "e⁻", "1.602176634E-19", "C"));
        phys.add(new Constant("Electron mass", "mₑ", "9.1093837015E-31", "kg"));
        phys.add(new Constant("Proton mass", "mₚ", "1.67262192369E-27", "kg"));
        phys.add(new Constant("Vacuum permittivity", "ε₀", "8.8541878128E-12", "F/m"));
        phys.add(new Constant("Vacuum permeability", "μ₀", "1.25663706212E-6", "H/m"));
        phys.add(new Constant("Standard gravity", "g", "9.80665", "m/s²"));
        phys.add(new Constant("Standard atmosphere", "atm", "101325", "Pa"));
        phys.add(new Constant("Rydberg constant", "R∞", "10973731.568160", "1/m"));
        phys.add(new Constant("Stefan–Boltzmann", "σ", "5.670374419E-8", "W/m²·K⁴"));
        GROUPS.put("PHYSICS", phys);
    }

    public static List<String> groupNames() { return new ArrayList<>(GROUPS.keySet()); }
}

