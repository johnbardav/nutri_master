package com.poligran.nutrimaster.util;
import com.poligran.nutrimaster.model.Recipe;
import com.poligran.nutrimaster.domain.NutritionCalculator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
public final class Format {
    private Format() { }
    public static String n(double value) {
        return new DecimalFormat("0.##",new DecimalFormatSymbols(new Locale("es","CO"))).format(value);
    }
    public static String macros(Recipe r) {
        return n(r.kcal)+" kcal por porci\u00f3n\nProte\u00ednas "+n(r.protein)+" g\nCarbohidratos "+n(r.carbs)+" g\nGrasas "+n(r.fat)+" g";
    }
    public static String result(NutritionCalculator.Result r) {
        return n(r.kcal)+" kcal en "+n(r.portions)+" porciones\nProte\u00ednas: "+n(r.protein)+" g\nCarbohidratos: "+n(r.carbs)+" g\nGrasas: "+n(r.fat)+" g\n\nPara los ingredientes de la receta base, multiplica cada cantidad por "+n(r.ingredientFactor)+".";
    }
    public static String bullets(String lines) {
        StringBuilder b=new StringBuilder();
        for(String s:lines.split("\\r?\\n"))if(!s.trim().isEmpty()) { if(b.length()>0)b.append("\n\n"); b.append("\u2022 ").append(s.trim()); }
        return b.toString();
    }
    public static String steps(String lines) {
        StringBuilder b=new StringBuilder(); int n=1;
        for(String s:lines.split("\\r?\\n"))if(!s.trim().isEmpty()) { if(b.length()>0)b.append("\n\n"); b.append(n++).append(". ").append(s.trim()); }
        return b.toString();
    }
    public static String share(Recipe r) {
        return r.title+" | NutriMaster\n"+r.minutes+" min | Receta base: "+n(r.baseServings)+" porciones\n\n"+r.description+
            "\n\nINGREDIENTES\n"+bullets(r.ingredients)+"\n\nPREPARACI\u00d3N\n"+steps(r.steps)+"\n\n"+macros(r)+
            "\nValores de ejemplo no validados nutricionalmente.";
    }
}
