package com.developerjp.jieungoalsettingapp.data;

public class Goal {
    private final String specificText;
    private int id;

    public Goal(String specificText) {
        this.specificText = specificText;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSpecificText() {
        return specificText;
    }

}
