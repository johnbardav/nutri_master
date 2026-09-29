package com.poligran.nutrimaster.domain;
import com.poligran.nutrimaster.model.Recipe;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
public final class RecipeFilter {
    private RecipeFilter() { }
    private static String normalized(String s) {
        return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    public static List<Recipe> apply(List<Recipe> recipes,String query,String category,int collection) {
        List<Recipe> result=new ArrayList<>(); String q=normalized(query.trim());
        for(Recipe r:recipes) {
            if(!"Todas".equals(category)&&!category.equals(r.category)) continue;
            if(collection==1&&!r.favorite || collection==2&&!r.saved || collection==3&&r.builtIn) continue;
            if(!normalized(r.title+" "+r.ingredients+" "+r.category).contains(q)) continue;
            result.add(r);
        }
        return result;
    }
}
