package com.poligran.nutrimaster;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.poligran.nutrimaster.ui.AppViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Real device/emulator tests. Not claimed as executed by the offline source checks. */
@RunWith(AndroidJUnit4.class)
public class NavigationInstrumentedTest {
    private void awaitData(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean loaded = new AtomicBoolean();
        long until = SystemClock.uptimeMillis() + 15000;
        while (!loaded.get() && SystemClock.uptimeMillis() < until) {
            scenario.onActivity(a -> loaded.set(new ViewModelProvider(a).get(AppViewModel.class).data.getValue() != null));
            SystemClock.sleep(50);
        }
        assertTrue("SQLite must load", loaded.get());
    }
    @Test public void exactFragmentNamesAndTwoPanels() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitData(scenario);
            int[] buttons = {R.id.btnPerfil, R.id.btnFotos, R.id.btnVideo, R.id.btnWeb, R.id.btnBotones};
            Class<?>[] expected = {PerfilFragment.class, FotosFragment.class, VideoFragment.class, WebFragment.class, BotonesFragment.class};
            for (int i = 0; i < buttons.length; i++) {
                onView(withId(buttons[i])).perform(scrollTo(), click());
                Class<?> type = expected[i];
                scenario.onActivity(a -> {
                    a.getSupportFragmentManager().executePendingTransactions();
                    assertEquals(type, a.getSupportFragmentManager().findFragmentById(R.id.contenedorContenido).getClass());
                    assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.contenedorMenu) instanceof MenuFragment);
                    assertEquals(2, a.getSupportFragmentManager().getFragments().size());
                });
                onView(withId(R.id.contenedorMenu)).check(matches(isDisplayed()));
                onView(withId(R.id.contenedorContenido)).check(matches(isDisplayed()));
            }
            scenario.recreate();
            awaitData(scenario);
            scenario.onActivity(a -> assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.contenedorContenido) instanceof BotonesFragment));
        }
    }
    @Test public void menuHasFiveRealButtonsNoRecipeList() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitData(scenario);
            scenario.onActivity(a -> {
                a.getSupportFragmentManager().executePendingTransactions();
                ViewGroup menu = a.findViewById(R.id.contenedorMenu);
                assertEquals(5, countButtons(menu));
                assertNull(menu.findViewById(R.id.lstFotos));
                AppViewModel vm = new ViewModelProvider(a).get(AppViewModel.class);
                vm.select(2L);
                vm.state.set("gallery_query", "avena");
                vm.state.set("gallery_collection", 1);
                vm.openRecipeLibrary();
                assertEquals(AppViewModel.GALLERY, vm.module.getValue());
                assertEquals(Long.valueOf(2L), vm.selectedId.getValue());
                assertEquals("", vm.textState("gallery_query", "missing"));
                assertEquals(0, vm.intState("gallery_collection", -1));
            });
        }
    }
    @Test public void listSelectionUpdatesSharedRecipe() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitData(scenario);
            onView(withId(R.id.btnFotos)).perform(scrollTo(), click());
            scenario.onActivity(a -> {
                ListView lstFotos = a.findViewById(R.id.lstFotos);
                assertNotNull(lstFotos);
                assertTrue(lstFotos.getAdapter().getCount() > 1);
                long selected = lstFotos.getAdapter().getItemId(1);
                lstFotos.performItemClick(lstFotos.getChildAt(1), 1, selected);
                assertEquals(Long.valueOf(selected), new ViewModelProvider(a).get(AppViewModel.class).selectedId.getValue());
            });
        }
    }
    @Test public void threeDocumentButtonsAndRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitData(scenario);
            onView(withId(R.id.btnBotones)).perform(scrollTo(), click());
            onView(withId(R.id.edtReceta)).perform(scrollTo(), replaceText("Ensalada de quinoa"), closeSoftKeyboard());
            onView(withId(R.id.btnMostrarReceta)).perform(scrollTo(), click());
            onView(withId(R.id.txtResultadoBotones)).check(matches(withText("Receta seleccionada: Ensalada de quinoa")));
            scenario.recreate();
            awaitData(scenario);
            onView(withId(R.id.edtReceta)).check(matches(withText("Ensalada de quinoa")));
            onView(withId(R.id.btnMostrarMensaje)).perform(scrollTo(), click());
            onView(withId(R.id.txtResultadoBotones)).check(matches(withText(R.string.botones_mensaje)));
            onView(withId(R.id.btnLimpiar)).perform(scrollTo(), click());
            onView(withId(R.id.edtReceta)).check(matches(withText("")));
            onView(withId(R.id.txtResultadoBotones)).check(matches(withText(R.string.botones_resultado_inicial)));
        }
    }
    private int countButtons(View v) {
        int count = v instanceof Button ? 1 : 0;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); i++) count += countButtons(g.getChildAt(i));
        }
        return count;
    }
}
