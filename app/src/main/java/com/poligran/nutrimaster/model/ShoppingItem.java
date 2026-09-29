package com.poligran.nutrimaster.model;
public final class ShoppingItem {
    public final long id;
    public final String text;
    public final boolean checked;
    public ShoppingItem(long id, String text, boolean checked) {
        this.id=id; this.text=text; this.checked=checked;
    }
}
