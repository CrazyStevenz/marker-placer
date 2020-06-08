package com.crazystevenz.markerplacer;

import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.DocumentReference;

public class MyMarker {
    private Marker marker;
    private DocumentReference ref;
    private String color;

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean equals(Marker marker) {
        return this.marker.getPosition().latitude == marker.getPosition().latitude
            && this.marker.getPosition().longitude == marker.getPosition().longitude;
    }
}
