package com.poligran.nutrimaster.domain;
import com.poligran.nutrimaster.model.Recipe;

/** No dietary recommendation: scales the recipe's illustrative per-serving values. */
public final class NutritionCalculator {
    private NutritionCalculator() { }
    public static final class Result {
        public final double portions,kcal,protein,carbs,fat,ingredientFactor;
        private Result(Recipe r,double p) {
            portions=p; kcal=r.kcal*p; protein=r.protein*p; carbs=r.carbs*p; fat=r.fat*p;
            ingredientFactor=p/r.baseServings;
        }
    }
    public static Result calculate(Recipe recipe,double portions) {
        if(recipe==null) throw new IllegalArgumentException("Selecciona una receta.");
        Validation.finite(portions,0.25,24,"Porciones");
        Validation.finite(recipe.baseServings,0.25,24,"Rendimiento");
        Validation.finite(recipe.kcal,0,5000,"Calor\u00edas");
        Validation.finite(recipe.protein,0,1000,"Prote\u00ednas");
        Validation.finite(recipe.carbs,0,1000,"Carbohidratos");
        Validation.finite(recipe.fat,0,1000,"Grasas");
        return new Result(recipe,portions);
    }
}
