package com.poligran.nutrimaster.util;
import android.text.Editable;
import android.text.TextWatcher;
import java.util.function.Consumer;
public final class TextWatcherAdapter implements TextWatcher {
    private final Consumer<String> listener;
    public TextWatcherAdapter(Consumer<String> listener){this.listener=listener;}
    @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
    @Override public void onTextChanged(CharSequence s,int start,int before,int count) { }
    @Override public void afterTextChanged(Editable s) {listener.accept(s.toString());}
}
