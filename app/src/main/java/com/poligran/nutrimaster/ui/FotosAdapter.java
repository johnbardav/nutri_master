package com.poligran.nutrimaster.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.model.Recipe;
import com.poligran.nutrimaster.util.Format;
import com.poligran.nutrimaster.util.ImageLoader;
import java.util.List;
import java.util.ArrayList;

/** ArrayAdapter real para lstFotos; las miniaturas tambien existen en el XML. */
public final class FotosAdapter extends ArrayAdapter<String> {
    private long selectedId = -1;
    private final List<Recipe> recetasDisponibles = new ArrayList<>();
    public FotosAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        setNotifyOnChange(false);
    }
    public void submit(List<Recipe> recetas, long selectedId) {
        this.selectedId = selectedId;
        setNotifyOnChange(false); // notifyDataSetChanged() restores automatic notifications.
        recetasDisponibles.clear();
        recetasDisponibles.addAll(recetas);
        clear();
        for (Recipe receta : recetasDisponibles) add(receta.title);
        notifyDataSetChanged();
    }
    @NonNull @Override public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View vista = convertView;
        if (vista == null) vista = LayoutInflater.from(getContext()).inflate(R.layout.item_foto, parent, false);
        Recipe receta = recetaEn(position);
        if (receta == null) return vista;
        ((TextView)vista.findViewById(R.id.foto_titulo)).setText(receta.title);
        ((TextView)vista.findViewById(R.id.foto_resumen)).setText(receta.minutes + " min \u00b7 " + Format.n(receta.kcal) + " kcal");
        vista.setBackgroundResource(receta.id == selectedId ? R.drawable.bg_chip : android.R.color.transparent);
        vista.setContentDescription(receta.title + (receta.id == selectedId ? ", seleccionada" : ""));
        ImageLoader.load((ImageView)vista.findViewById(R.id.foto_miniatura), receta.image);
        return vista;
    }
    @Override public long getItemId(int position) { Recipe r = recetaEn(position); return r == null ? -1 : r.id; }
    private Recipe recetaEn(int position) {
        return position >= 0 && position < recetasDisponibles.size() ? recetasDisponibles.get(position) : null;
    }
    @Override public boolean hasStableIds() { return true; }
}
