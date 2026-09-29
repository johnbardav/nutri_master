package com.poligran.nutrimaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.poligran.nutrimaster.domain.RecipeFilter;
import com.poligran.nutrimaster.model.*;
import com.poligran.nutrimaster.ui.*;
import com.poligran.nutrimaster.util.*;
import java.util.ArrayList;
import java.util.List;

/** Numerals 3-10: ListView + ArrayAdapter + selection; the previous grid remains optional. */
public final class FotosFragment extends Fragment implements AdapterView.OnItemClickListener {
    private ListView lstFotos;
    private ImageView imgFotoDetalle;
    private TextView txtDescripcionFoto;
    private String[] recetas = new String[0];
    private List<Recipe> recetasDisponibles = new ArrayList<>();
    private String[] descripciones = new String[0];
    private int[] imagenes = new int[0];
    private ArrayAdapter<String> adapter;
    private FotosAdapter adaptadorConMiniaturas;
    private EditText query;
    private Spinner category, collection;
    private RecyclerView grid;
    private RecipeAdapter gridAdapter;
    private RadioGroup displayMode;
    private boolean binding;
    private float lastTouchY;

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        View vista = inflater.inflate(R.layout.fragment_fotos, container, false);
        this.vista = vista;
        destroyed = false;
        binding = true;
        lstFotos = vista.findViewById(R.id.lstFotos);
        imgFotoDetalle = vista.findViewById(R.id.imgFotoDetalle);
        txtDescripcionFoto = vista.findViewById(R.id.txtDescripcionFoto);
        query = vista.findViewById(R.id.recipe_search);
        category = vista.findViewById(R.id.category_filter);
        collection = vista.findViewById(R.id.collection_filter);
        displayMode = vista.findViewById(R.id.gallery_display_mode);
        grid = vista.findViewById(R.id.recipe_grid);

        ArrayAdapter<CharSequence> cats = ArrayAdapter.createFromResource(
                requireContext(), R.array.categories, android.R.layout.simple_spinner_item);
        cats.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(cats);
        ArrayAdapter<CharSequence> filters = ArrayAdapter.createFromResource(
                requireContext(), R.array.collections, android.R.layout.simple_spinner_item);
        filters.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        collection.setAdapter(filters);
        adaptadorConMiniaturas = new FotosAdapter(requireContext());
        ArrayAdapter<String> adapter = adaptadorConMiniaturas;
        this.adapter = adapter; // Referencia para liberar el adaptador en onDestroyView.
        lstFotos.setAdapter(adapter);
        lstFotos.setOnItemClickListener((parent, view, position, id) ->
                onItemClick(parent, view, position, id));
        configurarScrollLista();

        gridAdapter = new RecipeAdapter(r -> seleccionarReceta(r));
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        grid.setAdapter(gridAdapter);
        grid.setItemAnimator(null);
        grid.setNestedScrollingEnabled(false);
        query.setText(vm.textState("gallery_query", ""));
        category.setSelection(vm.intState("gallery_category", 0));
        collection.setSelection(vm.intState("gallery_collection", 0));
        boolean gridMode = vm.intState("gallery_display", 0) == 1;
        displayMode.check(gridMode ? R.id.gallery_mode_grid : R.id.gallery_mode_list);
        actualizarModo(gridMode);
        binding = false;

        query.addTextChangedListener(new TextWatcherAdapter(s -> {
            if (!binding) { vm.state.set("gallery_query", s); filter(); }
        }));
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (binding) return;
                vm.state.set("gallery_category", category.getSelectedItemPosition());
                vm.state.set("gallery_collection", collection.getSelectedItemPosition());
                filter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        category.setOnItemSelectedListener(listener);
        collection.setOnItemSelectedListener(listener);
        displayMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean selectedGrid = checkedId == R.id.gallery_mode_grid;
            vm.state.set("gallery_display", selectedGrid ? 1 : 0);
            actualizarModo(selectedGrid);
        });
        click(R.id.create_recipe, () -> Editors.recipe(this, vm, null));
        click(R.id.recipe_actions, () -> vm.navigate(AppViewModel.ACTIONS));
        click(R.id.recipe_video, () -> vm.navigate(AppViewModel.VIDEO));
        restoreScroll();
        return vista;
    }

    /** Same event and view variables as the assignment; rows now come from SQLite. */
    @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= recetas.length) return;
        imgFotoDetalle.setImageResource(imagenes[position]);
        txtDescripcionFoto.setText(descripciones[position]);
        seleccionarReceta(recetasDisponibles.get(position));
    }
    private void seleccionarReceta(Recipe receta) {
        vm.select(receta.id);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current)
                ((ScrollView)find(R.id.gallery_scroll)).smoothScrollTo(0, find(R.id.recipe_detail_card).getTop());
        });
    }
    private void actualizarModo(boolean gridMode) {
        lstFotos.setVisibility(gridMode ? View.GONE : View.VISIBLE);
        grid.setVisibility(gridMode ? View.VISIBLE : View.GONE);
    }
    private void filter() {
        Snapshot s = vm.data.getValue();
        if (s == null) return;
        String cat = category.getSelectedItem() == null ? "Todas" : category.getSelectedItem().toString();
        recetasDisponibles = RecipeFilter.apply(s.recipes, query.getText().toString(), cat, collection.getSelectedItemPosition());
        recetas = new String[recetasDisponibles.size()];
        descripciones = new String[recetasDisponibles.size()];
        imagenes = new int[recetasDisponibles.size()];
        for (int i = 0; i < recetasDisponibles.size(); i++) {
            recetas[i] = recetasDisponibles.get(i).title;
            descripciones[i] = recetasDisponibles.get(i).description;
            imagenes[i] = imagenReceta(recetasDisponibles.get(i));
        }
        int first = lstFotos.getFirstVisiblePosition();
        View firstChild = lstFotos.getChildAt(0);
        int top = firstChild == null ? 0 : firstChild.getTop();
        Recipe selected = recipe();
        long selectedId = selected == null ? -1 : selected.id;
        adaptadorConMiniaturas.submit(recetasDisponibles, selectedId);
        gridAdapter.submit(recetasDisponibles, selectedId);
        if (recetas.length > 0) lstFotos.setSelectionFromTop(Math.min(first, recetas.length - 1), top);
        text(R.id.gallery_count, recetas.length + " de " + s.recipes.size() + " recetas \u00b7 selecciona una imagen");
        find(R.id.gallery_empty).setVisibility(recetas.length == 0 ? View.VISIBLE : View.GONE);
    }
    private int imagenReceta(Recipe receta) {
        // These aliases preserve the existing illustrations and the names cited in the PDF.
        switch (receta.image) {
            case "food_salad": return R.drawable.receta1;
            case "food_oats": return R.drawable.receta2;
            case "food_bowl": return R.drawable.receta3;
            case "food_wrap": return R.drawable.receta4;
            default:
                int id = getResources().getIdentifier(receta.image, "drawable", requireContext().getPackageName());
                return id == 0 ? R.drawable.ic_leaf : id;
        }
    }
    public void render() {
        binding = true;
        String search = vm.textState("gallery_query", "");
        if (!query.getText().toString().equals(search)) query.setText(search);
        category.setSelection(vm.intState("gallery_category", 0));
        collection.setSelection(vm.intState("gallery_collection", 0));
        binding = false;
        filter();
        Recipe r = recipe();
        find(R.id.recipe_detail_card).setVisibility(r == null ? View.GONE : View.VISIBLE);
        if (r == null) return;
        text(R.id.recipe_title, r.title);
        text(R.id.recipe_meta, r.category + " \u00b7 " + r.minutes + " min \u00b7 base: " + Format.n(r.baseServings) + " porciones");
        txtDescripcionFoto.setText(r.description);
        text(R.id.recipe_nutrition, Format.macros(r));
        text(R.id.recipe_ingredients, Format.bullets(r.ingredients));
        text(R.id.recipe_steps, Format.steps(r.steps));
        ImageLoader.load(imgFotoDetalle, r.image);
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void configurarScrollLista() {
        lstFotos.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchY = event.getY();
                view.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (action == MotionEvent.ACTION_MOVE) {
                int direction = event.getY() < lastTouchY ? 1 : -1;
                view.getParent().requestDisallowInterceptTouchEvent(view.canScrollVertically(direction));
                lastTouchY = event.getY();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false; // ListView still receives the actual click event.
        });
    }
    public void start() {
        if (lstFotos != null) lstFotos.setSelectionFromTop(vm.intState("gallery_list_first", 0), vm.intState("gallery_list_top", 0));
    }
    public void stop() { saveState(); }
    public void saveState() {
        saveScrollState();
        if (lstFotos != null) {
            vm.state.set("gallery_list_first", lstFotos.getFirstVisiblePosition());
            View child = lstFotos.getChildAt(0);
            vm.state.set("gallery_list_top", child == null ? 0 : child.getTop());
        }
    }
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
        vista.findViewById(R.id.gallery_scroll).setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
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
        if (vista != null) vm.state.set("scroll_" + R.id.gallery_scroll, ((ScrollView)find(R.id.gallery_scroll)).getScrollY());
    }
    private void restoreScroll() {
        int y = vm.intState("scroll_" + R.id.gallery_scroll, 0);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current) ((ScrollView)find(R.id.gallery_scroll)).scrollTo(0, y);
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
        if (lstFotos != null) lstFotos.setAdapter(null);
        if (grid != null) grid.setAdapter(null);
        lstFotos = null; grid = null; adapter = null; adaptadorConMiniaturas = null; gridAdapter = null;
        imgFotoDetalle = null; txtDescripcionFoto = null; query = null;
        category = collection = null; displayMode = null;
        super.onDestroyView();
    }
}
