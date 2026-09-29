package com.poligran.nutrimaster.ui;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.domain.Validation;
import com.poligran.nutrimaster.model.*;
import com.poligran.nutrimaster.util.Format;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
public final class CollectionDialogs {
    private CollectionDialogs() {}
    public static void shopping(Fragment host,AppViewModel vm) {
        View v=LayoutInflater.from(host.requireContext()).inflate(R.layout.dialog_shopping,null);
        RecyclerView list=v.findViewById(R.id.shopping_list);list.setLayoutManager(new LinearLayoutManager(host.requireContext()));
        ShoppingAdapter adapter=new ShoppingAdapter(vm);list.setAdapter(adapter);
        EditText input=v.findViewById(R.id.shopping_manual);
        v.findViewById(R.id.shopping_add).setOnClickListener(b->{
            try {String text=Validation.required(input.getText().toString(),300,"Producto");vm.addShopping(text);input.setText("");}
            catch(IllegalArgumentException e){input.setError(e.getMessage());}
        });
        v.findViewById(R.id.shopping_clear).setOnClickListener(b->vm.clearShopping());
        AlertDialog d=new MaterialAlertDialogBuilder(host.requireContext()).setTitle("Lista de compras").setView(v).setPositiveButton("Cerrar",null).create();
        Observer<Snapshot> observer=s->{if(s!=null){adapter.set(s.shopping);v.findViewById(R.id.shopping_empty).setVisibility(s.shopping.isEmpty()?View.VISIBLE:View.GONE);}};
        vm.data.observe(host.getViewLifecycleOwner(),observer);d.setOnDismissListener(x->vm.data.removeObserver(observer));d.show();
    }
    public static void history(Fragment host,AppViewModel vm) {
        Snapshot s=vm.data.getValue();if(s==null)return;
        if(s.calculations.isEmpty()) {new MaterialAlertDialogBuilder(host.requireContext()).setTitle("Historial").setMessage("A\u00fan no hay c\u00e1lculos. Usa Calcular en Botones.").setPositiveButton("Cerrar",null).show();return;}
        CharSequence[] rows=new CharSequence[s.calculations.size()];
        for(int i=0;i<rows.length;i++){
            Calculation c=s.calculations.get(i);
            rows[i]=c.title+"\n"+Format.n(c.portions)+" porciones \u00b7 "+Format.n(c.kcal)+" kcal\n"+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(c.createdAt));
        }
        new MaterialAlertDialogBuilder(host.requireContext()).setTitle("Historial de c\u00e1lculos").setItems(rows,(d,i)->{
            Calculation c=s.calculations.get(i);
            new MaterialAlertDialogBuilder(host.requireContext()).setTitle(c.title).setMessage(
                Format.n(c.portions)+" porciones\n"+Format.n(c.kcal)+" kcal\nProte\u00edna: "+Format.n(c.protein)+" g\nCarbohidratos: "+Format.n(c.carbs)+" g\nGrasa: "+Format.n(c.fat)+" g\n\nEstimaci\u00f3n a partir de los datos registrados; no es una recomendaci\u00f3n m\u00e9dica.")
                .setPositiveButton("Cerrar",null).show();
        }).setPositiveButton("Cerrar",null).setNeutralButton("Borrar historial",(d,w)->new MaterialAlertDialogBuilder(host.requireContext())
            .setTitle("\u00bfBorrar todos los c\u00e1lculos?").setMessage("No se borran las recetas. Esta acci\u00f3n no se puede deshacer.")
            .setNegativeButton(R.string.cancel,null).setPositiveButton("Borrar",(a,b)->vm.clearHistory()).show()).show();
    }
    private static final class ShoppingAdapter extends RecyclerView.Adapter<ShoppingAdapter.Holder> {
        private final AppViewModel vm;private List<ShoppingItem> items=new ArrayList<>();
        ShoppingAdapter(AppViewModel vm) {this.vm=vm;setHasStableIds(true);}
        void set(List<ShoppingItem> items){this.items=new ArrayList<>(items);notifyDataSetChanged();}
        @Override public long getItemId(int position){return items.get(position).id;}
        @Override public int getItemCount(){return items.size();}
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping,parent,false));}
        @Override public void onBindViewHolder(@NonNull Holder h,int p){ShoppingItem i=items.get(p);h.check.setOnCheckedChangeListener(null);h.check.setText(i.text);h.check.setChecked(i.checked);h.check.setOnCheckedChangeListener((b,on)->vm.checkShopping(i.id,on));}
        static final class Holder extends RecyclerView.ViewHolder {final CheckBox check;Holder(View v){super(v);check=v.findViewById(R.id.shopping_check);}}
    }
}
