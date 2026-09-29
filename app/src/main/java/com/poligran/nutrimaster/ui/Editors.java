package com.poligran.nutrimaster.ui;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.poligran.nutrimaster.R;
import com.poligran.nutrimaster.domain.Validation;
import com.poligran.nutrimaster.model.Profile;
import com.poligran.nutrimaster.model.Recipe;
import com.poligran.nutrimaster.util.Format;

/** XML forms with explicit IDs and click handlers, suitable for the second assignment. */
public final class Editors {
    private Editors() {}
    private static void value(View v,int id,String s) {((EditText)v.findViewById(id)).setText(s);}
    private static String input(View v,int id) {return ((EditText)v.findViewById(id)).getText().toString().trim();}
    private static Spinner spinner(View v,int id,int values,String selected) {
        Spinner s=v.findViewById(id);
        ArrayAdapter<CharSequence> a=ArrayAdapter.createFromResource(v.getContext(),values,android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);
        for(int i=0;i<a.getCount();i++)if(String.valueOf(a.getItem(i)).equals(selected))s.setSelection(i);
        return s;
    }
    public static void profile(Fragment host,AppViewModel vm) {
        if(vm.data.getValue()==null)return;
        Profile p=vm.data.getValue().profile.copy();
        View v=LayoutInflater.from(host.requireContext()).inflate(R.layout.dialog_profile,null);
        value(v,R.id.edit_name,p.name);value(v,R.id.edit_role,p.role);value(v,R.id.edit_education,p.education);
        value(v,R.id.edit_experience,p.experience);value(v,R.id.edit_bio,p.bio);
        AlertDialog d=new MaterialAlertDialogBuilder(host.requireContext()).setTitle("Editar perfil").setView(v)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            try {
                p.name=Validation.required(input(v,R.id.edit_name),120,"Nombre");
                p.role=Validation.required(input(v,R.id.edit_role),200,"Rol");
                p.education=input(v,R.id.edit_education);p.experience=input(v,R.id.edit_experience);p.bio=input(v,R.id.edit_bio);
                vm.saveProfile(p);d.dismiss();
            } catch(IllegalArgumentException e) {TextView error=v.findViewById(R.id.profile_edit_error);error.setText(e.getMessage());error.setVisibility(View.VISIBLE);error.requestFocus();}
        }));d.show();
    }
    public static void recipe(Fragment host,AppViewModel vm,Recipe original) {
        Recipe r=original==null?new Recipe():original.copy();
        View v=LayoutInflater.from(host.requireContext()).inflate(R.layout.dialog_recipe,null);
        value(v,R.id.edit_title,r.title);value(v,R.id.edit_description,r.description);
        Spinner category=spinner(v,R.id.edit_category,R.array.recipe_categories,r.category);
        Spinner image=spinner(v,R.id.edit_image,R.array.image_names,"");
        String[] keys={"bowl","oats","toast","rice","soup","wrap","salad","yogurt","pasta"};
        for(int i=0;i<keys.length;i++)if(r.image.equals("food_"+keys[i]))image.setSelection(i);
        value(v,R.id.edit_minutes,String.valueOf(r.minutes));value(v,R.id.edit_servings,Format.n(r.baseServings));
        value(v,R.id.edit_kcal,Format.n(r.kcal));value(v,R.id.edit_protein,Format.n(r.protein));
        value(v,R.id.edit_carbs,Format.n(r.carbs));value(v,R.id.edit_fat,Format.n(r.fat));
        value(v,R.id.edit_ingredients,r.ingredients);value(v,R.id.edit_steps,r.steps);
        AlertDialog d=new MaterialAlertDialogBuilder(host.requireContext()).setTitle(original==null?"Nueva receta":"Editar receta").setView(v)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{
            TextView error=v.findViewById(R.id.edit_error);
            try {
                r.title=input(v,R.id.edit_title);r.description=input(v,R.id.edit_description);
                r.category=String.valueOf(category.getSelectedItem());r.image="food_"+keys[image.getSelectedItemPosition()];
                double minutes=Validation.number(input(v,R.id.edit_minutes),1,1440,"Minutos");
                if(minutes!=Math.floor(minutes))throw new IllegalArgumentException("Minutos debe ser un n\u00famero entero.");
                r.minutes=(int)minutes;
                r.baseServings=Validation.number(input(v,R.id.edit_servings),0.25,24,"Porciones base");
                r.kcal=Validation.number(input(v,R.id.edit_kcal),0,5000,"Calor\u00edas por porci\u00f3n");
                r.protein=Validation.number(input(v,R.id.edit_protein),0,1000,"Prote\u00edna");
                r.carbs=Validation.number(input(v,R.id.edit_carbs),0,1000,"Carbohidratos");
                r.fat=Validation.number(input(v,R.id.edit_fat),0,1000,"Grasa");
                r.ingredients=input(v,R.id.edit_ingredients);r.steps=input(v,R.id.edit_steps);Validation.recipe(r);
                // An included guide must never silently describe an edited/different recipe.
                if(original!=null&&(!r.title.equals(original.title)||!r.steps.equals(original.steps)||!r.ingredients.equals(original.ingredients)))r.video="";
                vm.saveRecipe(r);d.dismiss();
            } catch(IllegalArgumentException e) {error.setText(e.getMessage());error.setVisibility(View.VISIBLE);error.requestFocus();error.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT);}
        }));d.show();
    }
}
