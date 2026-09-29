package com.poligran.nutrimaster.model;

public final class Recipe {
    public long id;
    public String title = "", category = "Almuerzo", description = "";
    public String ingredients = "", steps = "", image = "food_bowl", video = "";
    public String videoUri = "", notes = "";
    public int minutes = 20;
    public double baseServings = 2, kcal = 0, protein = 0, carbs = 0, fat = 0;
    public boolean favorite = false, saved = false, builtIn = false;
    public Recipe copy() {
        Recipe r = new Recipe();
        r.id=id; r.title=title; r.category=category; r.description=description;
        r.ingredients=ingredients; r.steps=steps; r.image=image; r.video=video;
        r.videoUri=videoUri; r.notes=notes; r.minutes=minutes; r.baseServings=baseServings;
        r.kcal=kcal; r.protein=protein; r.carbs=carbs; r.fat=fat;
        r.favorite=favorite; r.saved=saved; r.builtIn=builtIn;
        return r;
    }
}
