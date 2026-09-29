package com.poligran.nutrimaster.domain;
import com.poligran.nutrimaster.model.Recipe;

/** Shared UI/domain validation. Decimal comma and decimal point are both accepted. */
public final class Validation {
    private Validation() { }
    public static double number(String value, double min, double max, String label) {
        String s=value==null?"":value.trim();
        if (!s.matches("[0-9]+([.,][0-9]+)?"))
            throw new IllegalArgumentException(label+": escribe un n\u00famero v\u00e1lido.");
        double n;
        try { n=Double.parseDouble(s.replace(',','.')); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(label+": valor fuera de rango."); }
        if (Double.isNaN(n) || Double.isInfinite(n) || n<min || n>max)
            throw new IllegalArgumentException(label+": usa un valor entre "+min+" y "+max+".");
        return n;
    }
    public static String required(String s, int max, String label) {
        String v=s==null?"":s.trim();
        if(v.isEmpty()) throw new IllegalArgumentException(label+": este campo es obligatorio.");
        if(v.length()>max) throw new IllegalArgumentException(label+": m\u00e1ximo "+max+" caracteres.");
        return v;
    }
    public static void recipe(Recipe r) {
        required(r.title,120,"Nombre"); required(r.description,2000,"Descripci\u00f3n");
        required(r.ingredients,12000,"Ingredientes"); required(r.steps,12000,"Preparaci\u00f3n");
        if(r.minutes<1 || r.minutes>1440) throw new IllegalArgumentException("Tiempo: entre 1 y 1440 minutos.");
        finite(r.baseServings,0.25,24,"Rendimiento"); finite(r.kcal,0,5000,"Calor\u00edas");
        finite(r.protein,0,1000,"Prote\u00ednas"); finite(r.carbs,0,1000,"Carbohidratos");
        finite(r.fat,0,1000,"Grasas");
    }
    public static void finite(double n,double min,double max,String label) {
        if(Double.isNaN(n)||Double.isInfinite(n)||n<min||n>max)
            throw new IllegalArgumentException(label+": valor fuera de rango.");
    }
}
