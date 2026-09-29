package com.poligran.nutrimaster.ui;
import android.app.Application;
import android.net.Uri;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.data.*;
import com.poligran.nutrimaster.domain.NutritionCalculator;
import com.poligran.nutrimaster.model.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Activity-scoped data. MainActivity handles menu callbacks and Fragment transactions. */
public final class AppViewModel extends AndroidViewModel {
    public static final String PROFILE="Perfil",GALLERY="Fotos",VIDEO="Video",WEB="Web",ACTIONS="Botones";
    public final SavedStateHandle state;
    public final MutableLiveData<Snapshot> data=new MutableLiveData<>();
    public final MutableLiveData<String> module;
    public final MutableLiveData<Long> selectedId;
    public final MutableLiveData<Boolean> loadError=new MutableLiveData<>(false);
    public final MutableLiveData<Event> messages=new MutableLiveData<>();
    private final Repository repository;
    public static final class Event {
        private boolean used=false;private final String text;
        Event(String text) {this.text=text;}
        public String consume() {if(used)return null;used=true;return text;}
    }
    public AppViewModel(Application application,SavedStateHandle handle) {
        super(application);state=handle;repository=new Repository(application);
        module=state.getLiveData("module",PROFILE);selectedId=state.getLiveData("selected_id",1L);reload();
    }
    public void reload() {
        loadError.setValue(false);
        run(db->null,(value,s)->{});
    }
    private <T> void run(Repository.Work<T> operation,Repository.Success<T> after) {
        repository.submit(operation,(value,snapshot)->{
            loadError.setValue(false);data.setValue(snapshot);
            Long id=selectedId.getValue();
            if(id==null||snapshot.find(id)==null)selectedId.setValue(snapshot.recipes.isEmpty()?-1L:snapshot.recipes.get(0).id);
            after.accept(value,snapshot);
        },e->{if(data.getValue()==null)loadError.setValue(true);message("No se pudo completar la acci\u00f3n. Reintenta; tus datos no se han borrado.");});
    }
    public void message(String text) {messages.setValue(new Event(text));}
    public void navigate(String target) {
        if(PROFILE.equals(target)||GALLERY.equals(target)||VIDEO.equals(target)||WEB.equals(target)||ACTIONS.equals(target))module.setValue(target);
    }
    /** Opens the complete recipe library without changing the selected recipe or SQLite. */
    public void openRecipeLibrary() {
        state.set("gallery_query", "");
        state.set("gallery_category", 0);
        state.set("gallery_collection", 0);
        state.set("scroll_" + R.id.gallery_scroll, 0);
        state.set("gallery_list_first", 0);
        state.set("gallery_list_top", 0);
        navigate(GALLERY);
    }
    public void select(long id) {selectedId.setValue(id);}
    public Recipe selected() {
        Snapshot s=data.getValue();Long id=selectedId.getValue();return s==null||id==null?null:s.find(id);
    }
    public String textState(String key,String fallback) {String s=state.get(key);return s==null?fallback:s;}
    public int intState(String key,int fallback) {Integer n=state.get(key);return n==null?fallback:n;}
    public void saveRecipe(Recipe recipe) {
        Recipe copy=recipe.copy();run(db->db.saveRecipe(copy),(id,s)->{state.remove("calculation_"+id);select(id);state.set("gallery_query","");state.set("gallery_category",0);state.set("gallery_collection",0);navigate(GALLERY);message("Receta guardada en SQLite.");});
    }
    public void deleteRecipe(long id) {
        run(db->{db.deleteRecipe(id);return null;},(v,s)->{state.remove("notes_"+id);navigate(GALLERY);message("Receta eliminada. El historial y la lista de compras se conservan.");});
    }
    public void toggleSaved(long id) {run(db->db.toggle(id,false),(value,s)->message(value?"Receta guardada en tu colecci\u00f3n.":"Receta retirada de Guardadas; sigue en el recetario."));}
    public void toggleFavorite(long id) {run(db->db.toggle(id,true),(value,s)->message(value?"Receta marcada como favorita.":"Receta retirada de Favoritas."));}
    public void saveNotes(long id,String notes) {
        run(db->{db.saveNotes(id,notes);return null;},(v,s)->{state.remove("notes_"+id);message("Notas guardadas en SQLite.");});
    }
    public void saveProfile(Profile p) {Profile copy=p.copy();run(db->{db.saveProfile(copy);return null;},(v,s)->message("Perfil actualizado."));}
    public void setPhoto(Uri uri) {run(db->{db.setPhoto(uri.toString());return null;},(v,s)->message("Foto de perfil actualizada."));}
    public void setVideo(long id,String uri) {state.set("video_position_"+id,0);run(db->{db.setVideo(id,uri);return null;},(v,s)->message(uri.isEmpty()?"Gu\u00eda local seleccionada.":"Video vinculado a la receta."));}
    public void addIngredients(Recipe r) {Recipe copy=r.copy();run(db->db.addIngredients(copy),(count,s)->message(count==0?"Los ingredientes ya estaban en tu lista.":count+" ingredientes agregados para "+copy.baseServings+" porciones de la receta base."));}
    public void addShopping(String text) {run(db->{db.addShopping(text);return null;},(v,s)->message("Producto agregado."));}
    public void checkShopping(long id,boolean checked) {run(db->{db.checkShopping(id,checked);return null;},(v,s)->{});}
    public void clearShopping() {run(db->{db.clearShopping();return null;},(v,s)->message("Productos comprados retirados de la lista."));}
    public void calculate(Recipe r,NutritionCalculator.Result result) {Recipe copy=r.copy();run(db->{db.addCalculation(copy,result);return null;},(v,s)->message("C\u00e1lculo guardado en el historial."));}
    public void clearHistory() {run(db->{db.clearHistory();return null;},(v,s)->message("Historial eliminado."));}
    public void export(Uri uri) {
        run(db->{
            String json=JsonExporter.export(db.snapshot());
            try(OutputStream out=getApplication().getContentResolver().openOutputStream(uri,"wt")) {
                if(out==null)throw new IllegalStateException("No se pudo abrir el destino");
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        },(v,s)->message("Datos exportados. El archivo puede contener tus notas y perfil: gu\u00e1rdalo de forma privada."));
    }
    @Override protected void onCleared() {repository.close();}
}
