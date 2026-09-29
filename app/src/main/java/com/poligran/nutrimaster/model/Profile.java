package com.poligran.nutrimaster.model;
public final class Profile {
    public String name="", role="", education="", experience="", bio="", photo="";
    public Profile copy() {
        Profile p=new Profile(); p.name=name; p.role=role; p.education=education;
        p.experience=experience; p.bio=bio; p.photo=photo; return p;
    }
}
