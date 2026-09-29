package com.poligran.nutrimaster.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.poligran.nutrimaster.model.*;
import com.poligran.nutrimaster.domain.NutritionCalculator;
import com.poligran.nutrimaster.domain.Validation;
import com.poligran.nutrimaster.util.Streams;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** All methods are invoked exclusively by Repository's single background executor. */
public final class NutriDatabase extends SQLiteOpenHelper {
    public static final String NAME="nutrimaster.db";
    public static final int VERSION=1;
    private final Context context;
    public NutriDatabase(Context context) { this(context,NAME); }
    public NutriDatabase(Context context,String name) {
        super(context.getApplicationContext(),name,null,VERSION);
        this.context=context.getApplicationContext();
        setWriteAheadLoggingEnabled(true);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        try {
            String sql=Streams.text(context.getAssets().open("schema.sql"));
            for(String statement:sql.split(";")) if(!statement.trim().isEmpty()) db.execSQL(statement);
            JSONArray seed=new JSONArray(Streams.text(context.getAssets().open("recipes.json")));
            for(int i=0;i<seed.length();i++) {
                JSONObject j=seed.getJSONObject(i); Recipe r=new Recipe();
                r.id=j.getLong("id"); r.title=j.getString("title"); r.category=j.getString("category");
                r.description=j.getString("description"); r.ingredients=j.getString("ingredients");
                r.steps=j.getString("steps"); r.image=j.getString("image"); r.video=j.getString("video");
                r.minutes=j.getInt("minutes"); r.baseServings=j.getDouble("base_servings");
                r.kcal=j.getDouble("kcal"); r.protein=j.getDouble("protein"); r.carbs=j.getDouble("carbs"); r.fat=j.getDouble("fat");
                r.builtIn=true;
                ContentValues values=values(r); values.put("id",r.id); values.put("created_at",System.currentTimeMillis());
                db.insertOrThrow("recipes",null,values);
            }
            Profile profile=new Profile(); profile.name="Mi perfil"; profile.role="Cocina casera y curiosidad";
            profile.education="Escribe aqu\u00ed tu formaci\u00f3n, estudios o intereses culinarios.";
            profile.experience="Comparte qu\u00e9 te gusta preparar y lo que has aprendido en la cocina.";
            profile.bio="Este es tu espacio en NutriMaster. Edita tu perfil, agrega tu foto y empieza tu colecci\u00f3n de recetas.";
            ContentValues p=profileValues(profile); p.put("id",1); db.insertOrThrow("profile",null,p);
        } catch(Exception e) { throw new IllegalStateException("No se pudo inicializar NutriMaster",e); }
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        // Version 1 has no predecessor. Future versions must add incremental migrations.
        // Never use DROP TABLE as an upgrade shortcut: that would erase the user's data.
        throw new IllegalStateException("Agrega la migraci\u00f3n incremental de "+oldVersion+" a "+newVersion);
    }
    private static ContentValues values(Recipe r) {
        ContentValues v=new ContentValues();
        v.put("title",r.title.trim()); v.put("category",r.category); v.put("description",r.description.trim());
        v.put("ingredients",r.ingredients.trim()); v.put("steps",r.steps.trim()); v.put("image",r.image);
        v.put("video",r.video); v.put("video_uri",r.videoUri); v.put("minutes",r.minutes); v.put("base_servings",r.baseServings);
        v.put("kcal",r.kcal); v.put("protein",r.protein); v.put("carbs",r.carbs); v.put("fat",r.fat);
        v.put("favorite",r.favorite?1:0); v.put("saved",r.saved?1:0); v.put("built_in",r.builtIn?1:0); v.put("notes",r.notes);
        return v;
    }
    private static ContentValues profileValues(Profile p) {
        ContentValues v=new ContentValues(); v.put("name",p.name.trim()); v.put("role",p.role.trim());
        v.put("education",p.education.trim()); v.put("experience",p.experience.trim()); v.put("bio",p.bio.trim()); v.put("photo",p.photo); return v;
    }
    public long saveRecipe(Recipe r) {
        Validation.recipe(r); SQLiteDatabase db=getWritableDatabase(); ContentValues v=values(r);
        if(r.id>0) {
            // Preserve flags, notes and imported video if a concurrent UI action changed them.
            v.remove("favorite"); v.remove("saved"); v.remove("notes"); v.remove("video_uri");
            if(db.update("recipes",v,"id=?",new String[]{Long.toString(r.id)})!=1)
                throw new IllegalStateException("La receta ya no existe");
            return r.id;
        }
        v.put("built_in",0); v.put("created_at",System.currentTimeMillis());
        return db.insertOrThrow("recipes",null,v);
    }
    public void deleteRecipe(long id) { getWritableDatabase().delete("recipes","id=?",new String[]{Long.toString(id)}); }
    public boolean toggle(long id,boolean favorite) {
        String col=favorite?"favorite":"saved"; // Never supplied by user input.
        SQLiteDatabase db=getWritableDatabase(); db.execSQL("UPDATE recipes SET "+col+"=1-"+col+" WHERE id=?",new Object[]{id});
        try(Cursor c=db.rawQuery("SELECT "+col+" FROM recipes WHERE id=?",new String[]{Long.toString(id)})) {
            if(!c.moveToFirst()) throw new IllegalStateException("Selecciona una receta existente");
            return c.getInt(0)==1;
        }
    }
    public void saveNotes(long id,String notes) {
        if(notes.length()>4000)throw new IllegalArgumentException("Notas demasiado largas");
        ContentValues v=new ContentValues(); v.put("notes",notes);
        getWritableDatabase().update("recipes",v,"id=?",new String[]{Long.toString(id)});
    }
    public void setVideo(long id,String uri) {
        ContentValues v=new ContentValues();v.put("video_uri",uri);
        getWritableDatabase().update("recipes",v,"id=?",new String[]{Long.toString(id)});
    }
    public void saveProfile(Profile p) {
        Validation.required(p.name,120,"Nombre");
        ContentValues values=profileValues(p); values.remove("photo");
        getWritableDatabase().update("profile",values,"id=1",null);
    }
    public void setPhoto(String uri) {
        ContentValues v=new ContentValues();v.put("photo",uri);getWritableDatabase().update("profile",v,"id=1",null);
    }
    public int addIngredients(Recipe r) {
        SQLiteDatabase db=getWritableDatabase(); int count=0; db.beginTransaction();
        try {
            for(String line:r.ingredients.split("\\r?\\n")) {
                String text=line.trim();if(text.isEmpty())continue;
                ContentValues v=new ContentValues();v.put("recipe_id",r.id);v.put("text",text);
                if(db.insertWithOnConflict("shopping",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1)count++;
            }
            db.setTransactionSuccessful();return count;
        } finally { db.endTransaction(); }
    }
    public void addShopping(String text) {
        ContentValues v=new ContentValues(); v.put("text",Validation.required(text,300,"Producto"));
        getWritableDatabase().insertOrThrow("shopping",null,v);
    }
    public void checkShopping(long id,boolean checked) {
        ContentValues v=new ContentValues();v.put("checked",checked?1:0);
        getWritableDatabase().update("shopping",v,"id=?",new String[]{Long.toString(id)});
    }
    public void clearShopping() { getWritableDatabase().delete("shopping","checked=1",null); }
    public void addCalculation(Recipe r,NutritionCalculator.Result result) {
        ContentValues v=new ContentValues(); v.put("recipe_id",r.id); v.put("title",r.title);
        v.put("portions",result.portions);v.put("kcal",result.kcal);v.put("protein",result.protein);
        v.put("carbs",result.carbs);v.put("fat",result.fat);v.put("created_at",System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("calculations",null,v);
    }
    public void clearHistory() { getWritableDatabase().delete("calculations",null,null); }
    private static String str(Cursor c,String name) { return c.getString(c.getColumnIndexOrThrow(name)); }
    private static double num(Cursor c,String name) { return c.getDouble(c.getColumnIndexOrThrow(name)); }
    public Snapshot snapshot() {
        SQLiteDatabase db=getReadableDatabase(); List<Recipe> recipes=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT * FROM recipes ORDER BY id",null)) {
            while(c.moveToNext()) {
                Recipe r=new Recipe();r.id=c.getLong(c.getColumnIndexOrThrow("id"));
                r.title=str(c,"title");r.category=str(c,"category");r.description=str(c,"description");
                r.ingredients=str(c,"ingredients");r.steps=str(c,"steps");r.image=str(c,"image");r.video=str(c,"video");
                r.videoUri=str(c,"video_uri");r.notes=str(c,"notes");r.minutes=(int)num(c,"minutes");
                r.baseServings=num(c,"base_servings");r.kcal=num(c,"kcal");r.protein=num(c,"protein");r.carbs=num(c,"carbs");r.fat=num(c,"fat");
                r.favorite=num(c,"favorite")==1;r.saved=num(c,"saved")==1;r.builtIn=num(c,"built_in")==1;recipes.add(r);
            }
        }
        Profile p=new Profile();
        try(Cursor c=db.rawQuery("SELECT * FROM profile WHERE id=1",null)) {
            if(c.moveToFirst()) {p.name=str(c,"name");p.role=str(c,"role");p.education=str(c,"education");p.experience=str(c,"experience");p.bio=str(c,"bio");p.photo=str(c,"photo");}
        }
        List<ShoppingItem> shopping=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT id,text,checked FROM shopping ORDER BY id",null)) {
            while(c.moveToNext())shopping.add(new ShoppingItem(c.getLong(0),c.getString(1),c.getInt(2)==1));
        }
        List<Calculation> calculations=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT * FROM calculations ORDER BY id DESC",null)) {
            while(c.moveToNext()) {
                Calculation x=new Calculation();x.id=c.getLong(c.getColumnIndexOrThrow("id"));
                x.recipeId=c.getLong(c.getColumnIndexOrThrow("recipe_id"));x.title=str(c,"title");
                x.portions=num(c,"portions");x.kcal=num(c,"kcal");x.protein=num(c,"protein");x.carbs=num(c,"carbs");x.fat=num(c,"fat");
                x.createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));calculations.add(x);
            }
        }
        return new Snapshot(recipes,p,shopping,calculations);
    }
}
