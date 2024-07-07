package com.developerjp.jieungoalsettingapp.ui.home;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "goal_table")
public class Goal {
        @PrimaryKey(autoGenerate = true)
        private int id;
        private String specific;
        private String measurable;
        private String timeBound;

        public Goal(String specific, String measurable, String timeBound) {
                this.specific = specific;
                this.measurable = measurable;
                this.timeBound = timeBound;
        }

        public int getId() {
                return id;
        }

        public void setId(int id) {
                this.id = id;
        }

        public String getSpecific() {
                return specific;
        }

        public void setSpecific(String specific) {
                this.specific = specific;
        }

        public String getMeasurable() {
                return measurable;
        }

        public void setMeasurable(String measurable) {
                this.measurable = measurable;
        }

        public String getTimeBound() {
                return timeBound;
        }

        public void setTimeBound(String timeBound) {
                this.timeBound = timeBound;
        }
}
