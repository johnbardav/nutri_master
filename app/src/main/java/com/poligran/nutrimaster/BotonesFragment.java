package com.poligran.nutrimaster;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.poligran.nutrimaster.ui.AppViewModel;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.domain.*;
import com.poligran.nutrimaster.model.*;
import com.poligran.nutrimaster.ui.*;
import com.poligran.nutrimaster.util.*;
public final class BotonesFragment extends Fragment {
    private EditText edtReceta;
    private Button btnMostrarReceta, btnMostrarMensaje, btnLimpiar;
    private TextView txtResultadoBotones;
    private EditText notes,portions;private boolean binding=false;private long boundId=-1;
    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        View vista = inflater.inflate(R.layout.fragment_botones, container, false);
        this.vista = vista;
        destroyed = false; binding = false; boundId = -1;
        edtReceta = vista.findViewById(R.id.edtReceta);
        btnMostrarReceta = vista.findViewById(R.id.btnMostrarReceta);
        btnMostrarMensaje = vista.findViewById(R.id.btnMostrarMensaje);
        btnLimpiar = vista.findViewById(R.id.btnLimpiar);
        txtResultadoBotones = vista.findViewById(R.id.txtResultadoBotones);
        edtReceta.setText(vm.textState("demo_receta", ""));
        txtResultadoBotones.setText(vm.textState("demo_resultado", getString(R.string.botones_resultado_inicial)));
        edtReceta.addTextChangedListener(new TextWatcherAdapter(s -> vm.state.set("demo_receta", s)));
        btnMostrarReceta.setOnClickListener(v -> mostrarReceta());
        btnMostrarMensaje.setOnClickListener(v -> mostrarMensaje());
        btnLimpiar.setOnClickListener(v -> limpiar());
        notes = find(R.id.recipe_notes);
        portions = find(R.id.portions_input);
        click(R.id.actions_select_recipe,vm::openRecipeLibrary);
        notes.addTextChangedListener(new TextWatcherAdapter(s->{if(!binding&&boundId>0)vm.state.set("notes_"+boundId,s);}));
        portions.addTextChangedListener(new TextWatcherAdapter(s->{if(!binding&&boundId>0)vm.state.set("portions_"+boundId,s);}));
        click(R.id.save_recipe,()->{Recipe r=recipe();if(r!=null)vm.toggleSaved(r.id);});
        click(R.id.favorite_recipe,()->{Recipe r=recipe();if(r!=null)vm.toggleFavorite(r.id);});
        click(R.id.save_notes,()->{Recipe r=recipe();if(r!=null)vm.saveNotes(r.id,notes.getText().toString());});
        click(R.id.calculate,this::calculate);
        click(R.id.share_recipe,this::share);
        click(R.id.edit_recipe,()->{Recipe r=recipe();if(r!=null)Editors.recipe(this,vm,r);});
        click(R.id.delete_recipe,()->{
            Recipe r=recipe();if(r==null)return;
            new MaterialAlertDialogBuilder(context()).setTitle("\u00bfEliminar esta receta?")
                .setMessage(r.title+"\n\nSe eliminar\u00e1 del recetario, Favoritas y Guardadas, junto con sus notas. Esta acci\u00f3n no se puede deshacer.")
                .setNegativeButton(R.string.cancel,null).setPositiveButton("Eliminar",(d,w)->vm.deleteRecipe(r.id)).show();
        });
        click(R.id.add_to_shopping,()->{Recipe r=recipe();if(r!=null)vm.addIngredients(r);});
        click(R.id.open_shopping,()->CollectionDialogs.shopping(this,vm));
        click(R.id.open_history,()->CollectionDialogs.history(this,vm));
        click(R.id.export_data,()->new MaterialAlertDialogBuilder(context()).setTitle("Exportar datos")
            .setMessage("El JSON incluye tu perfil, notas, recetas y listas. No incluye los archivos de foto o video elegidos desde el dispositivo. Esta versi\u00f3n exporta datos; no importa copias. Elige un destino privado.")
            .setNegativeButton(R.string.cancel,null).setPositiveButton("Continuar",(d,w)->((MainActivity)requireActivity()).exportData()).show());
        restoreScroll();
    
        return vista;
    }
    public void render() {
        Snapshot s=vm.data.getValue();if(s==null)return;Recipe r=recipe();
        text(R.id.shopping_count,s.pending()+" pendientes de "+s.shopping.size()+" productos\n"+s.calculations.size()+" c\u00e1lculos registrados");
        find(R.id.recipe_actions_container).setVisibility(r==null?View.GONE:View.VISIBLE);
        find(R.id.actions_empty).setVisibility(r==null?View.VISIBLE:View.GONE);
        text(R.id.actions_recipe,r==null?"Sin receta seleccionada":r.title);if(r==null){boundId=-1;return;}
        ((MaterialButton)find(R.id.save_recipe)).setText(r.saved?"Quitar de Guardadas":"Guardar receta");
        ((MaterialButton)find(R.id.favorite_recipe)).setText(r.favorite?"Quitar de Favoritas":"Marcar favorita");
        binding=true;boundId=r.id;
        String note=vm.textState("notes_"+r.id,r.notes);if(!notes.getText().toString().equals(note))notes.setText(note);
        String p=vm.textState("portions_"+r.id,"1");if(!portions.getText().toString().equals(p))portions.setText(p);
        text(R.id.calorie_result,vm.textState("calculation_"+r.id,"Ingresa una cantidad para ver el resultado."));binding=false;
    }
    /** Exact example in numeral 9.7; does not overwrite the selected SQLite recipe. */
    private void mostrarReceta() {
        String receta = edtReceta.getText().toString().trim();
        if (receta.isEmpty()) txtResultadoBotones.setText(getString(R.string.botones_vacio));
        else txtResultadoBotones.setText(getString(R.string.botones_receta) + " " + receta);
        vm.state.set("demo_resultado", txtResultadoBotones.getText().toString());
    }
    private void mostrarMensaje() {
        Toast.makeText(requireContext(), R.string.botones_toast, Toast.LENGTH_SHORT).show();
        txtResultadoBotones.setText(getString(R.string.botones_mensaje));
        vm.state.set("demo_resultado", txtResultadoBotones.getText().toString());
    }
    private void limpiar() {
        edtReceta.setText("");
        txtResultadoBotones.setText(getString(R.string.botones_resultado_inicial));
        vm.state.set("demo_resultado", txtResultadoBotones.getText().toString());
    }
    private void calculate() {
        Recipe r=recipe();if(r==null)return;
        try {
            double p=Validation.number(portions.getText().toString(),0.25,24,"Porciones");
            NutritionCalculator.Result result=NutritionCalculator.calculate(r,p);
            String summary=Format.result(result);vm.state.set("calculation_"+r.id,summary);text(R.id.calorie_result,summary);
            portions.setError(null);vm.calculate(r,result);
        } catch(IllegalArgumentException e) {portions.setError(e.getMessage());portions.requestFocus();}
    }
    private void share() {
        Recipe r=recipe();if(r==null)return;
        Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,r.title).putExtra(Intent.EXTRA_TEXT,Format.share(r));
        try {this.startActivity(Intent.createChooser(send,"Compartir receta"));}
        catch(ActivityNotFoundException e){vm.message("No hay una aplicaci\u00f3n disponible para compartir texto.");}
    }

    public void start() { }

    public void stop() { saveState(); }

    public void saveState() { saveScrollState(); }

    public void destroy() { saveState(); destroyed = true; }

    // State shared through the activity; views and events belong to this Fragment.
    private AppViewModel vm;
    private View vista;
    private boolean destroyed = true;

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm.data.observe(getViewLifecycleOwner(), snapshot -> actualizarEstado());
        vm.selectedId.observe(getViewLifecycleOwner(), id -> {
            if (!destroyed && vm.data.getValue() != null) render();
        });
        vm.loadError.observe(getViewLifecycleOwner(), error -> actualizarEstado());
        vista.findViewById(R.id.loading_retry).setOnClickListener(v -> vm.reload());
        actualizarEstado();
    }

    private void actualizarEstado() {
        if (destroyed || vista == null) return;
        boolean loaded = vm.data.getValue() != null;
        vista.findViewById(R.id.actions_scroll).setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
        vista.findViewById(R.id.loading_overlay).setVisibility(loaded ? View.GONE : View.VISIBLE);
        if (loaded) {
            render();
        } else {
            boolean failed = Boolean.TRUE.equals(vm.loadError.getValue());
            ((TextView)vista.findViewById(R.id.loading_text)).setText(failed ? R.string.load_error : R.string.loading);
            vista.findViewById(R.id.loading_progress).setVisibility(failed ? View.GONE : View.VISIBLE);
            vista.findViewById(R.id.loading_retry).setVisibility(failed ? View.VISIBLE : View.GONE);
        }
    }

    public void refreshContent() { actualizarEstado(); }
    private android.content.Context context() { return requireContext(); }
    private <T extends View> T find(int id) { return vista.findViewById(id); }
    private void text(int id, String value) { ((TextView)find(id)).setText(value); }
    private void click(int id, Runnable action) { find(id).setOnClickListener(v -> action.run()); }
    private com.poligran.nutrimaster.model.Recipe recipe() { return vm.selected(); }
    private void saveScrollState() {
        if (vista != null) vm.state.set("scroll_" + R.id.actions_scroll, ((ScrollView)find(R.id.actions_scroll)).getScrollY());
    }
    private void restoreScroll() {
        int y = vm.intState("scroll_" + R.id.actions_scroll, 0);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current) ((ScrollView)find(R.id.actions_scroll)).scrollTo(0, y);
        });
    }
    @Override public void onStart() { super.onStart(); if (!destroyed) start(); }
    @Override public void onStop() { if (!destroyed) stop(); super.onStop(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        if (!destroyed) saveState();
        super.onSaveInstanceState(outState);
    }
    @Override public void onDestroyView() {
        if (!destroyed) destroy();
        destroyed = true;
        vista = null;
        edtReceta = notes = portions = null; btnMostrarReceta = btnMostrarMensaje = btnLimpiar = null; txtResultadoBotones = null;
        super.onDestroyView();
    }
}
