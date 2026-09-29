package com.poligran.nutrimaster;

import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.poligran.nutrimaster.ui.AppViewModel;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Requires a real Android test run. The offline checks do not execute these tests. */
@RunWith(AndroidJUnit4.class)
public class NombresDocumentoInstrumentedTest {
    private void awaitData(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean loaded = new AtomicBoolean();
        long end = SystemClock.uptimeMillis() + 15000;
        while (!loaded.get() && SystemClock.uptimeMillis() < end) {
            scenario.onActivity(a -> loaded.set(new ViewModelProvider(a).get(AppViewModel.class).data.getValue() != null));
            SystemClock.sleep(50);
        }
        assertTrue("SQLite must be ready", loaded.get());
    }

    @Test public void labelsAndIdsMatchTheDocument() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitData(scenario);
            int[] ids = {R.id.btnPerfil, R.id.btnFotos, R.id.btnVideo, R.id.btnWeb, R.id.btnBotones};
            String[] names = {"btnPerfil", "btnFotos", "btnVideo", "btnWeb", "btnBotones"};
            String[] labels = {"Perfil", "Fotos", "Video", "Web", "Botones"};
            for (int i = 0; i < ids.length; i++) {
                final int id = ids[i]; final String name = names[i]; final String label = labels[i];
                scenario.onActivity(a -> {
                    Button button = a.findViewById(id);
                    assertEquals(name, a.getResources().getResourceEntryName(button.getId()));
                    assertEquals(label, button.getText().toString());
                    assertTrue(button.hasOnClickListeners());
                });
            }
            onView(withId(R.id.profile_title_heading)).check(matches(withText("Perfil profesional")));
            onView(withId(R.id.btnFotos)).perform(scrollTo(), click());
            onView(withId(R.id.gallery_title_heading)).check(matches(withText("Galer\u00eda de recetas")));
            scenario.onActivity(a -> {
                ListView lstFotos = a.findViewById(R.id.lstFotos);
                assertTrue(lstFotos.getAdapter() instanceof ArrayAdapter);
                assertNotNull(lstFotos.getOnItemClickListener());
                if (lstFotos.getAdapter().getCount() > 0) assertTrue(lstFotos.getAdapter().getItem(0) instanceof String);
            });
            onView(withId(R.id.btnVideo)).perform(scrollTo(), click());
            onView(withId(R.id.video_title_heading)).check(matches(withText("Video: preparaci\u00f3n saludable")));
            onView(withId(R.id.btnReproducir)).check(matches(withText("Reproducir")));
            onView(withId(R.id.btnPausar)).check(matches(withText("Pausar")));
            onView(withId(R.id.btnWeb)).perform(scrollTo(), click());
            onView(withId(R.id.btnCargar)).check(matches(withText("Cargar")));
            onView(withId(R.id.btnBotones)).perform(scrollTo(), click());
            onView(withId(R.id.btnMostrarReceta)).check(matches(withText("Mostrar receta")));
            onView(withId(R.id.btnMostrarMensaje)).check(matches(withText("Mostrar mensaje")));
            onView(withId(R.id.btnLimpiar)).check(matches(withText("Limpiar")));
            onView(withId(R.id.btnMostrarMensaje)).perform(scrollTo(), click());
            onView(withId(R.id.txtResultadoBotones)).check(matches(withText("Se mostr\u00f3 el mensaje de ejemplo.")));
        }
    }

    @Test public void namedFieldsAndMethodsAreRealJavaMembers() throws Exception {
        assertEquals(String[].class, FotosFragment.class.getDeclaredField("recetas").getType());
        assertEquals(String[].class, FotosFragment.class.getDeclaredField("descripciones").getType());
        assertEquals(int[].class, FotosFragment.class.getDeclaredField("imagenes").getType());
        assertEquals("android.widget.ArrayAdapter<java.lang.String>", FotosFragment.class.getDeclaredField("adapter").getGenericType().getTypeName());
        for (String name : new String[]{"mostrarPerfil", "mostrarFotos", "mostrarVideo", "mostrarWeb", "mostrarBotones"}) {
            assertNotNull(MainActivity.class.getDeclaredMethod(name));
        }
        assertNotNull(VideoFragment.class.getDeclaredMethod("reproducirVideo"));
        assertNotNull(VideoFragment.class.getDeclaredMethod("pausarVideo"));
        assertNotNull(WebFragment.class.getDeclaredMethod("cargarPagina"));
        assertNotNull(BotonesFragment.class.getDeclaredMethod("mostrarReceta"));
        assertNotNull(BotonesFragment.class.getDeclaredMethod("mostrarMensaje"));
        assertNotNull(BotonesFragment.class.getDeclaredMethod("limpiar"));
    }
}
