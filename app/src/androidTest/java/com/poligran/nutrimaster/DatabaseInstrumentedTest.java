package com.poligran.nutrimaster;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.poligran.nutrimaster.data.NutriDatabase;
import com.poligran.nutrimaster.model.*;
import com.poligran.nutrimaster.domain.NutritionCalculator;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Uses an isolated test DB, not the user's production database. Requires a device. */
@RunWith(AndroidJUnit4.class)
public class DatabaseInstrumentedTest {
    private static final String NAME="nutrimaster-instrumented.db";
    private Context context;private NutriDatabase db;
    @Before public void before(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.deleteDatabase(NAME);db=new NutriDatabase(context,NAME);}
    @After public void after(){db.close();context.deleteDatabase(NAME);}
    @Test public void seedAndPersistence(){
        assertEquals(9,db.snapshot().recipes.size());Recipe r=db.snapshot().recipes.get(0);
        assertTrue(db.toggle(r.id,true));assertTrue(db.toggle(r.id,false));db.saveNotes(r.id,"Prueba persistente");
        db.close();db=new NutriDatabase(context,NAME);Recipe loaded=db.snapshot().find(r.id);
        assertTrue(loaded.favorite);assertTrue(loaded.saved);assertEquals("Prueba persistente",loaded.notes);
        assertEquals(9,db.snapshot().recipes.size());
    }
    @Test public void crudShoppingAndHistory(){
        Recipe r=db.snapshot().recipes.get(0).copy();r.id=0;r.title="Receta de prueba";
        long id=db.saveRecipe(r);r=db.snapshot().find(id);assertFalse(r.builtIn);
        r.title="Receta editada";db.saveRecipe(r);assertEquals(r.title,db.snapshot().find(id).title);
        int added=db.addIngredients(r);assertTrue(added>0);assertEquals(0,db.addIngredients(r));
        db.addCalculation(r,NutritionCalculator.calculate(r,2));db.deleteRecipe(id);
        Snapshot s=db.snapshot();assertNull(s.find(id));assertEquals(added,s.shopping.size());assertEquals(1,s.calculations.size());
        ShoppingItem item=s.shopping.get(0);db.checkShopping(item.id,true);db.clearShopping();assertEquals(added-1,db.snapshot().shopping.size());
    }
    @Test public void editPreservesIndependentFlagsAndProfile(){
        Recipe before=db.snapshot().recipes.get(0).copy();db.toggle(before.id,true);db.saveRecipe(before);
        assertTrue(db.snapshot().find(before.id).favorite);
        Profile p=db.snapshot().profile.copy();p.name="Perfil de prueba";db.saveProfile(p);assertEquals(p.name,db.snapshot().profile.name);
    }
}
