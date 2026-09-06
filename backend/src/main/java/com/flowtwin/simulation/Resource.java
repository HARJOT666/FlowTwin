package com.flowtwin.simulation;

public class Resource {

    private String id;
    private String type;
    private boolean available;

    public Resource(String id, String type) {
        this.id = id;
        this.type = type;
        this.available = true;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}