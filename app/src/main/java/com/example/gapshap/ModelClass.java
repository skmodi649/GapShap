package com.example.gapshap;

import android.graphics.ColorSpace;

public class ModelClass {
    private String name;
    private String text;

    public ModelClass(){

    }

    public ModelClass(String name, String text, String photoUrl) {
        this.name = name;
        this.text = text;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    private String photoUrl;

}
