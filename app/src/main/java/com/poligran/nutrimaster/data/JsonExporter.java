package com.poligran.nutrimaster.data;
import com.poligran.nutrimaster.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Portable textual export; external media are URI references, not embedded binaries. */
public final class JsonExporter {
    private JsonExporter() { }
    public static String export(Snapshot s) throws Exception {
        JSONObject root=new JSONObject();root.put("application","NutriMaster");root.put("schema_version",1);
        root.put("exported_at",System.currentTimeMillis());
        root.put("notice","Valores nutricionales de ejemplo. Los archivos multimedia externos no se incluyen.");
        JSONArray recipes=new JSONArray();
        for(Recipe r:s.recipes) {
            JSONObject j=new JSONObject();j.put("id",r.id);j.put("title",r.title);j.put("category",r.category);
            j.put("description",r.description);j.put("ingredients",r.ingredients);j.put("steps",r.steps);
            j.put("minutes",r.minutes);j.put("base_servings",r.baseServings);j.put("kcal",r.kcal);j.put("protein",r.protein);j.put("carbs",r.carbs);j.put("fat",r.fat);
            j.put("favorite",r.favorite);j.put("saved",r.saved);j.put("notes",r.notes);j.put("built_in",r.builtIn);j.put("image",r.image);j.put("video",r.video);j.put("video_uri",r.videoUri);recipes.put(j);
        }
        root.put("recipes",recipes);Profile p=s.profile;
        root.put("profile",new JSONObject().put("name",p.name).put("role",p.role).put("education",p.education).put("experience",p.experience).put("bio",p.bio).put("photo",p.photo));
        JSONArray shopping=new JSONArray();for(ShoppingItem x:s.shopping)shopping.put(new JSONObject().put("text",x.text).put("checked",x.checked));root.put("shopping",shopping);
        JSONArray history=new JSONArray();for(Calculation c:s.calculations)history.put(new JSONObject().put("title",c.title).put("recipe_id",c.recipeId).put("portions",c.portions).put("kcal",c.kcal).put("protein",c.protein).put("carbs",c.carbs).put("fat",c.fat).put("created_at",c.createdAt));root.put("calculations",history);
        return root.toString(2);
    }
}
