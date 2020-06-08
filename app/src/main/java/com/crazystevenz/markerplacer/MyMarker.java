package com.crazystevenz.markerplacer;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.DocumentReference;

public class MyMarker {
    private Marker marker;
    private DocumentReference ref;
    private int color;

    public MyMarker(Marker marker) {
        this.marker = marker;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public DocumentReference getRef() {
        return ref;
    }

    public void setRef(DocumentReference ref) {
        this.ref = ref;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
