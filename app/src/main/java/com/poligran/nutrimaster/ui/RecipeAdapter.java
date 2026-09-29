package com.poligran.nutrimaster.ui;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.model.Recipe;
import com.poligran.nutrimaster.util.Format;
import com.poligran.nutrimaster.util.ImageLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public final class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.Holder> {
    private List<Recipe> recipes=new ArrayList<>();private long selected=-1;
    private final Consumer<Recipe> onClick;
    public RecipeAdapter(Consumer<Recipe> onClick){this.onClick=onClick;setHasStableIds(true);}
    public void submit(List<Recipe> values,long id){recipes=values;selected=id;notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder h,int p) {
        Recipe r=recipes.get(p);h.title.setText(r.title);h.state.setText(r.favorite?"\u2605 Favorita":Format.n(r.kcal)+" kcal");
        h.itemView.setBackgroundResource(r.id==selected?R.drawable.bg_chip:android.R.color.transparent);
        h.itemView.setContentDescription(r.title+", "+r.minutes+" minutos"+(r.id==selected?", seleccionada":""));
        h.itemView.setOnClickListener(v->onClick.accept(r));ImageLoader.load(h.image,r.image);
    }
    @Override public int getItemCount(){return recipes.size();}
    @Override public long getItemId(int p){return recipes.get(p).id;}
    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;final TextView title,state;
        Holder(View v){super(v);image=v.findViewById(R.id.thumbnail);title=v.findViewById(R.id.thumbnail_title);state=v.findViewById(R.id.thumbnail_state);}
    }
}
