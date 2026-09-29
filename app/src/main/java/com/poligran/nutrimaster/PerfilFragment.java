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

import android.widget.ImageView;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.model.Snapshot;
import com.poligran.nutrimaster.ui.*;
import com.poligran.nutrimaster.util.ImageLoader;
/** Perfil: photo, education, experience and editable local profile (numerals 3-10). */
public final class PerfilFragment extends Fragment {
    private ImageView imgPerfil;
    private TextView txtNombre, txtEstudios, txtExperiencia, txtInfoAdicional;
    private ScrollView scrollPerfil;

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        View vista = inflater.inflate(R.layout.fragment_perfil, container, false);
        this.vista = vista;
        destroyed = false;
        imgPerfil = vista.findViewById(R.id.imgPerfil);
        txtNombre = vista.findViewById(R.id.txtNombre);
        txtEstudios = vista.findViewById(R.id.txtEstudios);
        txtExperiencia = vista.findViewById(R.id.txtExperiencia);
        txtInfoAdicional = vista.findViewById(R.id.txtInfoAdicional);
        scrollPerfil = vista.findViewById(R.id.scrollPerfil);
        imgPerfil.setImageResource(R.drawable.perfil);


        click(R.id.edit_profile,()->Editors.profile(this,vm));
        click(R.id.change_photo,() -> ((MainActivity)requireActivity()).pickPhoto());
        click(R.id.profile_explore,vm::openRecipeLibrary);restoreScroll();
    
        return vista;
    }
    public void render() {
        Snapshot s=vm.data.getValue();if(s==null)return;
        txtNombre.setText(s.profile.name.isEmpty() ? getString(R.string.perfil_nombre) : s.profile.name);text(R.id.profile_role,s.profile.role);
        txtInfoAdicional.setText(s.profile.bio.isEmpty() ? getString(R.string.perfil_info) : s.profile.bio);
        txtEstudios.setText(s.profile.education.isEmpty() ? getString(R.string.perfil_estudios) : s.profile.education);
        txtExperiencia.setText(s.profile.experience.isEmpty() ? getString(R.string.perfil_experiencia) : s.profile.experience);
        text(R.id.profile_stats,s.recipes.size()+" recetas disponibles\n"+s.saved()+" guardadas  \u00b7  "+s.favorites()+" favoritas\n"+s.pending()+" productos pendientes");
        ImageLoader.load(imgPerfil,s.profile.photo);
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
        vista.findViewById(R.id.scrollPerfil).setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
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
        if (vista != null) vm.state.set("scroll_" + R.id.scrollPerfil, ((ScrollView)find(R.id.scrollPerfil)).getScrollY());
    }
    private void restoreScroll() {
        int y = vm.intState("scroll_" + R.id.scrollPerfil, 0);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current) ((ScrollView)find(R.id.scrollPerfil)).scrollTo(0, y);
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
        imgPerfil = null; txtNombre = txtEstudios = txtExperiencia = txtInfoAdicional = null; scrollPerfil = null;
        super.onDestroyView();
    }
}
