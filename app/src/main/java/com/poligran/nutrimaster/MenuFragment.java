package com.poligran.nutrimaster;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.poligran.nutrimaster.ui.AppViewModel;

/** Menu persistente: nombres, variables y callback de los numerales 3, 5, 6 y 9.2. */
public final class MenuFragment extends Fragment {
    public interface OnOpcionSeleccionadaListener {
        void onOpcionSeleccionada(String opcion);
    }
    private OnOpcionSeleccionadaListener listener;
    private Button btnPerfil, btnFotos, btnVideo, btnWeb, btnBotones;
    private ImageView imgLogo;
    private TextView txtMenuTitulo;

    @Override public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(context instanceof OnOpcionSeleccionadaListener)) {
            throw new IllegalStateException("MainActivity debe implementar OnOpcionSeleccionadaListener");
        }
        listener = (OnOpcionSeleccionadaListener) context;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_menu, container, false);
        imgLogo = vista.findViewById(R.id.imgLogo);
        txtMenuTitulo = vista.findViewById(R.id.txtMenuTitulo);
        btnPerfil = vista.findViewById(R.id.btnPerfil);
        btnFotos = vista.findViewById(R.id.btnFotos);
        btnVideo = vista.findViewById(R.id.btnVideo);
        btnWeb = vista.findViewById(R.id.btnWeb);
        btnBotones = vista.findViewById(R.id.btnBotones);
        imgLogo.setImageResource(R.drawable.logo_nutrimaster);
        txtMenuTitulo.setText(R.string.menu_titulo);
        btnPerfil.setOnClickListener(v -> listener.onOpcionSeleccionada("perfil"));
        btnFotos.setOnClickListener(v -> listener.onOpcionSeleccionada("fotos"));
        btnVideo.setOnClickListener(v -> listener.onOpcionSeleccionada("video"));
        btnWeb.setOnClickListener(v -> listener.onOpcionSeleccionada("web"));
        btnBotones.setOnClickListener(v -> listener.onOpcionSeleccionada("botones"));
        return vista;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppViewModel vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        vm.module.observe(getViewLifecycleOwner(), opcion -> {
            btnPerfil.setSelected(AppViewModel.PROFILE.equals(opcion));
            btnFotos.setSelected(AppViewModel.GALLERY.equals(opcion));
            btnVideo.setSelected(AppViewModel.VIDEO.equals(opcion));
            btnWeb.setSelected(AppViewModel.WEB.equals(opcion));
            btnBotones.setSelected(AppViewModel.ACTIONS.equals(opcion));
        });
    }

    @Override public void onDestroyView() {
        btnPerfil = btnFotos = btnVideo = btnWeb = btnBotones = null;
        imgLogo = null;
        txtMenuTitulo = null;
        super.onDestroyView();
    }
    @Override public void onDetach() { listener = null; super.onDetach(); }
}
