package com.poligran.nutrimaster.model;
import java.util.Collections;
import java.util.List;
public final class Snapshot {
    public final List<Recipe> recipes;
    public final Profile profile;
    public final List<ShoppingItem> shopping;
    public final List<Calculation> calculations;
    public Snapshot(List<Recipe> recipes, Profile profile, List<ShoppingItem> shopping,
                    List<Calculation> calculations) {
        this.recipes=Collections.unmodifiableList(recipes); this.profile=profile;
        this.shopping=Collections.unmodifiableList(shopping);
        this.calculations=Collections.unmodifiableList(calculations);
    }
    public Recipe find(long id) {
        for (Recipe r : recipes) if (r.id==id) return r;
        return null;
    }
    public int favorites() { int n=0; for(Recipe r:recipes) if(r.favorite)n++; return n; }
    public int saved() { int n=0; for(Recipe r:recipes) if(r.saved)n++; return n; }
    public int pending() { int n=0; for(ShoppingItem s:shopping) if(!s.checked)n++; return n; }
}
