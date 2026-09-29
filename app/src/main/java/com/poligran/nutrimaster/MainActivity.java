package com.poligran.nutrimaster;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.poligran.nutrimaster.ui.AppViewModel;
import java.util.Locale;

/** Numeral 3: one Activity, persistent menu and a real Fragment for each content option. */
public final class MainActivity extends AppCompatActivity
        implements MenuFragment.OnOpcionSeleccionadaListener {
    private static final String COMPONENT_STATE="nutrimaster_component_state";
    private static final int COMPONENT_VERSION=3;
    private AppViewModel vm;
    private String requestedTag;

    // Stable activity-owned launchers retain file selection when the content Fragment changes.
    private final ActivityResultLauncher<String[]> photoPicker=registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),uri->{
                if(uri!=null&&persist(uri))vm.setPhoto(uri);
            });
    private final ActivityResultLauncher<String[]> videoPicker=registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),uri->{
                if(uri==null||!persist(uri))return;
                Long id=vm.state.get("video_picker_recipe");
                if(id!=null&&vm.data.getValue()!=null&&vm.data.getValue().find(id)!=null)
                    vm.setVideo(id,uri.toString());
                else vm.message("La receta seleccionada ya no existe.");
            });
    private final ActivityResultLauncher<String> exportPicker=registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),uri->{
                if(uri!=null)vm.export(uri);
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        // Old 1.1 task state refers to the retired DetailFragment/ui.MenuFragment.
        // Discard ONLY that transient UI state on upgrade; SQLite is never cleared.
        if(savedInstanceState!=null&&savedInstanceState.getInt(COMPONENT_STATE,0)!=COMPONENT_VERSION)
            savedInstanceState=null;
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        setContentView(R.layout.activity_main);
        if(android.os.Build.VERSION.SDK_INT>=29)findViewById(R.id.app_root).setForceDarkAllowed(false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_root),(view,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            Insets keyboard=insets.getInsets(WindowInsetsCompat.Type.ime());
            findViewById(R.id.inset_content).setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.app_root));
        vm=new ViewModelProvider(this).get(AppViewModel.class);
        vm.messages.observe(this,event->{
            if(event==null)return;String message=event.consume();if(message==null)return;
            Snackbar bar=Snackbar.make(findViewById(R.id.inset_content),message,Snackbar.LENGTH_LONG);
            TextView label=bar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            label.setMaxLines(5);bar.show();
        });
        findViewById(R.id.about_button).setOnClickListener(v->new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about)
            .setMessage("NutriMaster 2.1.0\nHerramientas de Programaci\u00f3n M\u00f3vil I\nSubgrupo 12\nPolit\u00e9cnico Grancolombiano\n\nJava + XML + SQLite\nDos paneles simult\u00e1neos (30/70).\n\nLos datos, el avatar, las ilustraciones y las gu\u00edas en video son material de demostraci\u00f3n. Los valores nutricionales no han sido validados.\n\nLos datos se almacenan solo en este dispositivo. Desinstalar o borrar los datos elimina tu colecci\u00f3n local.")
            .setPositiveButton(R.string.close,null).show());
        if(savedInstanceState==null) {
            getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.contenedorMenu,new MenuFragment(),"menu")
                .replace(R.id.contenedorContenido,new PerfilFragment(),"PerfilFragment").commit();
            requestedTag="PerfilFragment";
        }
        // Also handles existing cross-section actions: choose recipe, save/edit/delete recipe.
        vm.module.observe(this,this::mostrarModulo);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Fragment content=getSupportFragmentManager().findFragmentById(R.id.contenedorContenido);
                if(content instanceof WebFragment && ((WebFragment)content).handleBack()) return;
                setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);
            }
        });
    }

    /** MenuFragment emits this callback; only the Activity routes navigation. */
    @Override public void onOpcionSeleccionada(String opcion) {
        if(opcion==null)return;
        String target;
        switch(opcion.toLowerCase(Locale.ROOT)) {
            case "perfil": target=AppViewModel.PROFILE;break;
            case "fotos": target=AppViewModel.GALLERY;break;
            case "video": target=AppViewModel.VIDEO;break;
            case "web": target=AppViewModel.WEB;break;
            case "botones": target=AppViewModel.ACTIONS;break;
            default:return;
        }
        if(!target.equals(vm.module.getValue()))vm.navigate(target);
        else mostrarModulo(target);
    }
    private void mostrarModulo(String opcion) {
        if(opcion==null)return;
        switch(opcion) {
            case AppViewModel.PROFILE: mostrarPerfil();break;
            case AppViewModel.GALLERY: mostrarFotos();break;
            case AppViewModel.VIDEO: mostrarVideo();break;
            case AppViewModel.WEB: mostrarWeb();break;
            case AppViewModel.ACTIONS: mostrarBotones();break;
            default:break;
        }
    }
    private void mostrarPerfil() {cambiarContenido(new PerfilFragment());}
    private void mostrarFotos() {cambiarContenido(new FotosFragment());}
    private void mostrarVideo() {cambiarContenido(new VideoFragment());}
    private void mostrarWeb() {cambiarContenido(new WebFragment());}
    private void mostrarBotones() {cambiarContenido(new BotonesFragment());}

    /** Replaces only the right panel, as specified in the supplied document. */
    private void cambiarContenido(Fragment fragmento) {
        FragmentManager manager=getSupportFragmentManager();
        // Async data may arrive while another app (e.g. the file picker) is visible.
        // onPostResume applies the latest requested module instead of committing state loss.
        if(manager.isStateSaved())return;
        String tag=fragmento.getClass().getSimpleName();
        Fragment current=manager.findFragmentById(R.id.contenedorContenido);
        if(tag.equals(requestedTag)) {
            if(current != null && current.getClass()==fragmento.getClass()) refreshContent(current);
            return;
        }
        if(current!=null&&current.getClass()==fragmento.getClass()&&requestedTag==null) {
            requestedTag=tag;
            refreshContent(current);
            return;
        }
        requestedTag=tag;
        manager.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.contenedorContenido,fragmento,tag).commit();
    }
    private void refreshContent(Fragment current) {
        if(current instanceof PerfilFragment) ((PerfilFragment)current).refreshContent();
        else if(current instanceof FotosFragment) ((FotosFragment)current).refreshContent();
        else if(current instanceof VideoFragment) ((VideoFragment)current).refreshContent();
        else if(current instanceof WebFragment) ((WebFragment)current).refreshContent();
        else if(current instanceof BotonesFragment) ((BotonesFragment)current).refreshContent();
    }
    @Override protected void onPostResume() {
        super.onPostResume();
        if(vm!=null)mostrarModulo(vm.module.getValue());
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(COMPONENT_STATE,COMPONENT_VERSION);
        super.onSaveInstanceState(outState);
    }

    private boolean persist(Uri uri) {
        try {
            if(!"content".equals(uri.getScheme()))throw new SecurityException();
            getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
        } catch(SecurityException e) {
            vm.message("No se pudo conservar el acceso al archivo. Selecci\u00f3nalo desde el proveedor Archivos del dispositivo.");
            return false;
        }
    }
    public void pickPhoto() {photoPicker.launch(new String[]{"image/*"});}
    public void pickVideo(long recipeId) {
        vm.state.set("video_picker_recipe",recipeId);videoPicker.launch(new String[]{"video/*"});
    }
    public void exportData() {exportPicker.launch("NutriMaster-datos.json");}
}
