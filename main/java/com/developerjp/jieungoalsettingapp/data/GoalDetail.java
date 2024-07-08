package com.developerjp.jieungoalsettingapp.data;

import androidx.annotation.Nullable;

import java.util.Date;

public class GoalDetail {
    private int id;
    private String specificText;
    private int specificId;
    private int measurable;
    private String timeBound;
    private Date timestamp;

    public GoalDetail(int id, int specificId, int measurable, String timeBound, Date timestamp, String specificText) {
        this.id = id;
        this.specificId = specificId;
        this.measurable = measurable;
        this.timeBound = timeBound;
        this.timestamp = timestamp;
        this.specificText = specificText;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSpecificId() {
        return specificId;
    }

    public void setSpecificId(int specificId) {
        this.specificId = specificId;
    }

    public int getMeasurable() {
        return measurable;
    }

    public void setMeasurable(int measurable) {
        this.measurable = measurable;
    }

    public String getTimeBound() {
        return timeBound;
    }

    public void setTimeBound(String timeBound) {
        this.timeBound = timeBound;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    public String getSpecificText() {
        return specificText;
    }

    public void setSpecificText(String specificText) {
        this.specificText = specificText;
    }
}
