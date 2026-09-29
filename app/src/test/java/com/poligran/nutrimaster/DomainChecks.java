package com.poligran.nutrimaster;
import com.poligran.nutrimaster.domain.*;
import com.poligran.nutrimaster.model.Recipe;
import java.util.Arrays;
import java.util.Collections;
/** Can run without Android, Gradle, JUnit or network. Throws on the first failed assertion. */
public final class DomainChecks {
    private static int assertions;
    private static void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    private static void near(double a,double b,String name){check(Math.abs(a-b)<0.000001,name);}
    private static void rejects(Runnable work,String name){boolean rejected=false;try{work.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,name);}
    private static Recipe recipe(){Recipe r=new Recipe();r.title="Bowl de vegetales";r.description="Receta de prueba";r.ingredients="1 taza de arroz\n1 tomate";r.steps="Lavar\nMezclar";r.kcal=390;r.protein=14;r.carbs=48;r.fat=15;r.baseServings=2;return r;}
    public static int runSuite(){
        assertions=0;
        near(Validation.number("1,5",.25,24,"Porciones"),1.5,"decimal comma");
        near(Validation.number(" 2.5 ",.25,24,"Porciones"),2.5,"decimal point");
        near(Validation.number("0.25",.25,24,"Porciones"),.25,"lower bound");
        near(Validation.number("24",.25,24,"Porciones"),24,"upper bound");
        for(String bad:Arrays.asList("","NaN","Infinity","-1","0","25","1,2.3","1e2","1..2"," ","1 2"))
            rejects(()->Validation.number(bad,.25,24,"Porciones"),"reject invalid number "+bad);
        rejects(()->Validation.required("  ",120,"Nombre"),"blank required");
        rejects(()->Validation.required("abcd",3,"Nombre"),"required max length");
        check(Validation.required(" nombre ",120,"Nombre").equals("nombre"),"required trims");
        Recipe r=recipe();Validation.recipe(r);check(true,"valid recipe");
        NutritionCalculator.Result result=NutritionCalculator.calculate(r,1.5);
        near(result.kcal,585,"kcal scaled");near(result.protein,21,"protein scaled");
        near(result.carbs,72,"carbs scaled");near(result.fat,22.5,"fat scaled");
        near(result.ingredientFactor,.75,"ingredients use base yield");
        rejects(()->NutritionCalculator.calculate(null,1),"missing recipe");
        rejects(()->NutritionCalculator.calculate(r,Double.NaN),"nonfinite portions");
        Recipe invalid=r.copy();invalid.baseServings=0;
        rejects(()->NutritionCalculator.calculate(invalid,1),"zero base yield");
        invalid.baseServings=2;invalid.kcal=Double.POSITIVE_INFINITY;
        rejects(()->NutritionCalculator.calculate(invalid,1),"nonfinite recipe");
        Recipe blank=r.copy();blank.title="";rejects(()->Validation.recipe(blank),"empty title");
        check(UrlPolicy.normalize("example.org").equals("https://example.org/"),"add HTTPS");
        check(UrlPolicy.normalize("HTTPS://EXAMPLE.ORG/a").equals("https://example.org/a"),"normalize case");
        check(UrlPolicy.normalize("https://example.org/?q=a%26b&x=%2F#c%23d").equals("https://example.org/?q=a%26b&x=%2F#c%23d"),"preserve escaped query");
        check(UrlPolicy.normalize("https://example.org:8443/path").equals("https://example.org:8443/path"),"valid port");
        for(String bad:Arrays.asList("http://example.org","javascript:alert(1)","file:///etc/passwd","content://secret/data","https://user:pass@example.org","https://example.org:0","https://example.org:99999","https://localhost","https://bad host.org","","https://nutrimaster.invalid/","https://example.org/\nfoo"))
            rejects(()->UrlPolicy.normalize(bad),"reject unsafe URL "+bad);
        check(!UrlPolicy.allowed("https://user@example.org"),"block credential redirect");
        Recipe a=recipe();a.title="Ensalada de at\u00fan";a.favorite=true;a.builtIn=true;
        Recipe b=recipe();b.title="Avena";b.category="Desayuno";b.saved=true;
        check(RecipeFilter.apply(Arrays.asList(a,b),"atun","Todas",0).size()==1,"accent insensitive search");
        check(RecipeFilter.apply(Arrays.asList(a,b),"AVENA","Todas",0).get(0)==b,"case insensitive search");
        check(RecipeFilter.apply(Arrays.asList(a,b),"","Desayuno",0).size()==1,"category filter");
        check(RecipeFilter.apply(Arrays.asList(a,b),"","Todas",1).get(0)==a,"favorites filter");
        check(RecipeFilter.apply(Arrays.asList(a,b),"","Todas",2).get(0)==b,"saved filter");
        check(RecipeFilter.apply(Arrays.asList(a,b),"","Todas",3).get(0)==b,"custom recipes filter");
        check(RecipeFilter.apply(Arrays.asList(a,b),"tomate","Todas",0).size()==2,"ingredient search");
        check(RecipeFilter.apply(Collections.emptyList(),"","Todas",0).isEmpty(),"empty collection");
        Recipe copy=a.copy();copy.title="Otra";check(!a.title.equals(copy.title),"editing a copy preserves original");
        return assertions;
    }
    public static void main(String[] args){System.out.println("PASS: "+runSuite()+" domain assertions (Java, no Android runtime).");}
}
