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

import android.net.Uri;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.Button;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.model.Recipe;
import com.poligran.nutrimaster.ui.*;
import com.poligran.nutrimaster.util.Format;
/** VideoView + native seek controls, with lifecycle-aware release and saved playback position. */
public final class VideoFragment extends Fragment {
    private VideoView videoView;private MediaController controles;
    private Button btnReproducir, btnPausar;
    private boolean playWhenReady = false;
    private Uri videoDocumento;
    private long boundId=-1;private String source="";private boolean active=false,loaded=false,ready=false;
    private int generation=0;
    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        View vista = inflater.inflate(R.layout.fragment_video, container, false);
        this.vista = vista;
        destroyed = false;
        boundId = -1; source = ""; active = false; loaded = false; ready = false; playWhenReady = false;

        click(R.id.video_select_recipe,vm::openRecipeLibrary);
        videoView=vista.findViewById(R.id.videoView);
        btnReproducir = vista.findViewById(R.id.btnReproducir);
        btnPausar = vista.findViewById(R.id.btnPausar);
        MediaController controles = new MediaController(requireContext());
        this.controles = controles;
        controles.setAnchorView(videoView);
        videoView.setMediaController(controles);
        Uri uri = Uri.parse("android.resource://" + requireContext().getPackageName()
                + "/" + R.raw.video_nutrimaster);
        videoDocumento = uri;
        btnReproducir.setOnClickListener(v -> reproducirVideo());
        btnPausar.setOnClickListener(v -> pausarVideo());
        click(R.id.video_stop, this::detenerVideo);
        click(R.id.video_import,()->{Recipe r=recipe();if(r!=null)((MainActivity)requireActivity()).pickVideo(r.id);});
        click(R.id.video_reset,()->{Recipe r=recipe();if(r!=null)vm.setVideo(r.id,"");});restoreScroll();
    
        return vista;
    }
    public void render() {
        Recipe r=recipe();String next="";
        if(r!=null){
            if(!r.videoUri.isEmpty())next=r.videoUri;
            else if(!r.video.isEmpty()){
                int id = "video_bowl".equals(r.video) ? R.raw.video_nutrimaster
                    : context().getResources().getIdentifier(r.video,"raw",context().getPackageName());
                if (id != 0) next = "video_bowl".equals(r.video)
                        ? videoDocumento.toString()
                        : "android.resource://" + context().getPackageName() + "/" + id;
            }
        }
        long nextId=r==null?-1:r.id;
        text(R.id.video_recipe,r==null?"Selecciona o crea una receta":r.title);
        text(R.id.video_steps,r==null?"":Format.steps(r.steps));
        text(R.id.video_description,r!=null&&!r.videoUri.isEmpty()?"Video elegido desde tu dispositivo. Su disponibilidad depende del archivo y del c\u00f3dec.":context().getString(R.string.video_notice));
        find(R.id.video_import).setEnabled(r!=null);find(R.id.video_reset).setEnabled(r!=null&&!r.videoUri.isEmpty());
        if(nextId!=boundId||!source.equals(next)) {
            boolean differentSourceForSameRecipe=nextId==boundId&&!source.equals(next);
            release();boundId=nextId;source=next;
            if(differentSourceForSameRecipe&&boundId>0)vm.state.set("video_position_"+boundId,0);
            if(active&&!source.isEmpty())load();
        }
        if(source.isEmpty()) {text(R.id.video_state,"Esta receta no tiene un video vinculado. Puedes seleccionar uno desde Archivos.");find(R.id.video_loading).setVisibility(View.GONE);}
        buttons();
    }
    /** Numeral 7: btnReproducir -> reproducirVideo() -> VideoView.start(). */
    private void reproducirVideo() {
        if (ready) {
            playWhenReady = false;
            videoView.start();
            videoView.setKeepScreenOn(true);
            controles.show();
            text(R.id.video_state, "Reproduciendo. Toca el video para avanzar o retroceder.");
        } else if (active && !source.isEmpty()) {
            playWhenReady = true;
            if (!loaded) load();
        }
    }
    /** Numeral 7: btnPausar -> pausarVideo() -> VideoView.pause(). */
    private void pausarVideo() {
        playWhenReady = false;
        if (ready) {
            videoView.pause(); videoView.setKeepScreenOn(false); remember();
            text(R.id.video_state, "En pausa.");
        }
    }
    private void detenerVideo() {
        playWhenReady = false;
        if (ready) {
            videoView.pause(); videoView.seekTo(0); videoView.setKeepScreenOn(false);
            vm.state.set("video_position_" + boundId, 0);
            text(R.id.video_state, "Detenido. Posici\u00f3n reiniciada.");
        }
    }
    private void buttons() {
        find(R.id.btnReproducir).setEnabled(!source.isEmpty());find(R.id.btnPausar).setEnabled(ready);find(R.id.video_stop).setEnabled(ready);
    }
    private void load() {
        if(destroyed||!active||source.isEmpty())return;
        int token=++generation;loaded=true;ready=false;buttons();
        find(R.id.video_loading).setVisibility(View.VISIBLE);text(R.id.video_state,"Preparando video...");
        videoView.setOnPreparedListener(mp->{
            if(destroyed||token!=generation)return;ready=true;find(R.id.video_loading).setVisibility(View.GONE);
            int position=vm.intState("video_position_"+boundId,0);
            videoView.seekTo(position>0?Math.min(position,Math.max(0,mp.getDuration()-100)):1);
            text(R.id.video_state,source.startsWith("android.resource:")?"Listo. Pulsa Reproducir. La gu\u00eda incluida no tiene audio.":"Listo. Pulsa Reproducir para ver tu video.");buttons();
            if (playWhenReady) reproducirVideo();
        });
        videoView.setOnCompletionListener(mp->{if(!destroyed&&token==generation){vm.state.set("video_position_"+boundId,0);videoView.seekTo(0);videoView.setKeepScreenOn(false);text(R.id.video_state,"Video finalizado. Puedes reproducirlo de nuevo.");}});
        videoView.setOnErrorListener((mp,what,extra)->{if(!destroyed&&token==generation){ready=false;loaded=false;find(R.id.video_loading).setVisibility(View.GONE);videoView.setKeepScreenOn(false);text(R.id.video_state,"No se pudo reproducir. Reintenta, elige otro archivo compatible o vuelve a la gu\u00eda local.");buttons();}return true;});
        try {Uri uri = Uri.parse(source); videoView.setVideoURI(uri);}
        catch(RuntimeException e){loaded=false;ready=false;find(R.id.video_loading).setVisibility(View.GONE);text(R.id.video_state,"Archivo no disponible. Selecciona otro video.");buttons();}
    }
    private void remember(){if(ready&&boundId>0)vm.state.set("video_position_"+boundId,videoView.getCurrentPosition());}
    private void release(){playWhenReady=false;remember();generation++;controles.hide();videoView.stopPlayback();videoView.setKeepScreenOn(false);ready=false;loaded=false;}
    public void start(){active=true;if(!loaded&&!source.isEmpty())load();}
    public void stop(){saveScrollState();active=false;release();}
    public void saveState(){saveScrollState();remember();}
    public void destroy(){release();saveScrollState();destroyed=true;}

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
        vista.findViewById(R.id.video_scroll).setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
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
        if (vista != null) vm.state.set("scroll_" + R.id.video_scroll, ((ScrollView)find(R.id.video_scroll)).getScrollY());
    }
    private void restoreScroll() {
        int y = vm.intState("scroll_" + R.id.video_scroll, 0);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current) ((ScrollView)find(R.id.video_scroll)).scrollTo(0, y);
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
        videoView = null; controles = null; btnReproducir = btnPausar = null;
        super.onDestroyView();
    }
}
