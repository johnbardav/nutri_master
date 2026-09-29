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

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ProgressBar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.domain.UrlPolicy;
import com.poligran.nutrimaster.ui.*;
import com.poligran.nutrimaster.util.TextWatcherAdapter;
/** Untrusted web content is never given a Java bridge, file access, or invalid TLS bypass. */
public final class WebFragment extends Fragment {
    private static final String LOCAL="https://nutrimaster.invalid/";
    private WebView webView;private EditText edtUrl;private ProgressBar progress;
    private Button btnCargar;
    private TextView txtEstadoWeb;
    private boolean failed=false;
    private float lastTouchY;
    @SuppressLint({"SetJavaScriptEnabled","ClickableViewAccessibility"})
    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        View vista = inflater.inflate(R.layout.fragment_web, container, false);
        this.vista = vista;
        destroyed = false; failed = false; lastTouchY = 0;

        webView=vista.findViewById(R.id.webView);
        edtUrl=vista.findViewById(R.id.edtUrl);
        btnCargar=vista.findViewById(R.id.btnCargar);
        txtEstadoWeb=vista.findViewById(R.id.txtEstadoWeb);
        progress=vista.findViewById(R.id.web_progress);
        txtEstadoWeb.setText(R.string.web_estado_inicial);
        WebSettings settings=webView.getSettings();
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(true);settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(false);settings.setJavaScriptEnabled(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);
        // Let the web page scroll inside its viewport, then hand gestures at its edges
        // back to the outer ScrollView. Return false so WebView still handles clicks.
        webView.setOnTouchListener((view,event)->{
            int action=event.getActionMasked();
            if(action==android.view.MotionEvent.ACTION_DOWN) {
                lastTouchY=event.getY();view.getParent().requestDisallowInterceptTouchEvent(true);
            } else if(action==android.view.MotionEvent.ACTION_MOVE) {
                int direction=event.getY()<lastTouchY?1:-1;
                view.getParent().requestDisallowInterceptTouchEvent(view.canScrollVertically(direction));lastTouchY=event.getY();
            } else if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
        if(android.os.Build.VERSION.SDK_INT>=26)settings.setSafeBrowsingEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return block(request.getUrl().toString());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return block(url);}
            @Override public void onPageStarted(WebView view,String url,Bitmap favicon){if(destroyed)return;failed=false;progress.setVisibility(View.VISIBLE);status(getString(R.string.web_cargando));navigation();}
            @Override public void onPageFinished(WebView view,String url){if(destroyed)return;progress.setVisibility(View.GONE);if(!failed)status(url.startsWith(LOCAL)?"Gu\u00eda local: disponible sin internet.":"P\u00e1gina cargada. Verifica la direcci\u00f3n y la fuente.");navigation();}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())error("No se pudo cargar. Revisa la conexi\u00f3n y la URL; el sitio podr\u00eda no admitir este WebView.");}
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){if(request.isForMainFrame())error("El servidor respondi\u00f3 con el c\u00f3digo "+response.getStatusCode()+". Puedes probar otra p\u00e1gina.");}
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError sslError){handler.cancel();error("Certificado de seguridad no v\u00e1lido. La conexi\u00f3n fue bloqueada.");}
        });
        webView.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int n){if(!destroyed)progress.setProgress(n);}});
        webView.setDownloadListener((url,agent,disposition,mime,length)->vm.message("Las descargas no est\u00e1n habilitadas aqu\u00ed. Usa Abrir en navegador para decidir fuera de la app."));
        edtUrl.setText(vm.textState("web_input", getString(R.string.web_url_defecto)));
        edtUrl.addTextChangedListener(new TextWatcherAdapter(s->vm.state.set("web_input",s)));
        edtUrl.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_GO){cargarPagina();return true;}return false;});
        btnCargar.setOnClickListener(v -> cargarPagina());click(R.id.web_home,this::home);
        click(R.id.web_back,()->{if(webView.canGoBack())webView.goBack();});click(R.id.web_forward,()->{if(webView.canGoForward())webView.goForward();});
        click(R.id.web_external,this::external);
        SwitchMaterial js=find(R.id.web_javascript);Boolean enabled=vm.state.get("web_js");js.setChecked(Boolean.TRUE.equals(enabled));
        settings.setJavaScriptEnabled(js.isChecked());settings.setDomStorageEnabled(js.isChecked());
        js.setOnCheckedChangeListener((button,on)->{settings.setJavaScriptEnabled(on);settings.setDomStorageEnabled(on);vm.state.set("web_js",on);webView.reload();});
        Bundle previous=vm.state.get("web_history");
        if(previous==null||webView.restoreState(previous)==null)home();restoreScroll();
        return vista;
    }
    private boolean block(String url){
        if(url.startsWith(LOCAL))return false;
        if(UrlPolicy.allowed(url))return false;
        error("Enlace bloqueado. Solo se permite HTTPS sin credenciales.");return true;
    }
    private void status(String message){if(!destroyed)txtEstadoWeb.setText(message);}
    private void error(String message){if(destroyed)return;failed=true;progress.setVisibility(View.GONE);status(message);navigation();}
    private void navigation(){if(destroyed)return;find(R.id.web_back).setEnabled(webView.canGoBack());find(R.id.web_forward).setEnabled(webView.canGoForward());}
    private void cargarPagina(){
        String url = edtUrl.getText().toString().trim();
        if (url.isEmpty()) {
            edtUrl.setError(getString(R.string.web_error));
            txtEstadoWeb.setText(R.string.web_error);
            return;
        }
        txtEstadoWeb.setText(R.string.web_cargando);
        try {url=UrlPolicy.normalize(url);edtUrl.setError(null);edtUrl.setText(url);webView.loadUrl(url);}
        catch(IllegalArgumentException e){edtUrl.setError(e.getMessage());txtEstadoWeb.setText(e.getMessage());edtUrl.requestFocus();}
    }
    private void external(){
        String url=webView.getUrl();if(url==null||url.startsWith(LOCAL))url=edtUrl.getText().toString();
        try {String target=UrlPolicy.normalize(url);this.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(target)));}
        catch(IllegalArgumentException e){edtUrl.setError(e.getMessage());}
        catch(ActivityNotFoundException e){vm.message("No hay un navegador instalado.");}
    }
    private void home(){
        String html="<!doctype html><html lang='es'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{margin:0;background:#f3f6fb;color:#112746;font:16px sans-serif;line-height:1.6}header{background:#0b1932;color:white;padding:24px 18px}main{padding:18px}h1{font-size:26px;line-height:1.2}h2{font-size:19px}a{color:#194e91}article{background:white;padding:18px;border-radius:14px;margin-bottom:16px}small{color:#53677d}</style></head><body><header><small style='color:#82c8f5'>NUTRIMASTER / BIBLIOTECA</small><h1>Aprender tambi&eacute;n<br>es parte de cocinar.</h1></header><main><article><h2>Tu biblioteca local</h2><p>Esta p&aacute;gina est&aacute; integrada en la aplicaci&oacute;n. No necesita conexi&oacute;n.</p><p>En Fotos, selecciona una receta. En Botones, guarda tus favoritas, ajusta las porciones y prepara tu lista de compras.</p></article><article><h2>Consulta otras fuentes</h2><p>Los siguientes enlaces requieren internet. No son contenido creado ni validado por NutriMaster.</p><p><a href='https://www.who.int/es/news-room/fact-sheets/detail/healthy-diet'>OMS: alimentaci&oacute;n saludable</a></p><p><a href='https://www.fao.org/nutrition/es/'>FAO: nutrici&oacute;n</a></p></article><article><h2>Navegaci&oacute;n segura</h2><p>Escribe una URL HTTPS en el campo superior. JavaScript est&aacute; desactivado por defecto; puedes activarlo para una p&aacute;gina de confianza.</p><small>Los datos de las recetas son demostrativos y editables. Esta app acad&eacute;mica no ofrece asesor&iacute;a m&eacute;dica.</small></article></main></body></html>";
        webView.loadDataWithBaseURL(LOCAL,html,"text/html","UTF-8",LOCAL);
    }
    public void render(){navigation();}
    public boolean handleBack(){if(destroyed || webView == null)return false;if(webView.canGoBack()){webView.goBack();return true;}return false;}
    public void start(){webView.onResume();}
    public void stop(){saveState();webView.onPause();}
    public void saveState(){saveScrollState();Bundle b=new Bundle();webView.saveState(b);vm.state.set("web_history",b);}
    public void destroy(){saveState();destroyed=true;webView.stopLoading();webView.setWebChromeClient(null);webView.setWebViewClient(new WebViewClient());((ViewGroup)webView.getParent()).removeView(webView);webView.destroy();}

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
        vista.findViewById(R.id.web_scroll).setVisibility(loaded ? View.VISIBLE : View.INVISIBLE);
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
        if (vista != null) vm.state.set("scroll_" + R.id.web_scroll, ((ScrollView)find(R.id.web_scroll)).getScrollY());
    }
    private void restoreScroll() {
        int y = vm.intState("scroll_" + R.id.web_scroll, 0);
        View current = vista;
        current.post(() -> {
            if (!destroyed && vista == current) ((ScrollView)find(R.id.web_scroll)).scrollTo(0, y);
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
        webView = null; edtUrl = null; progress = null; btnCargar = null; txtEstadoWeb = null;
        super.onDestroyView();
    }
}
