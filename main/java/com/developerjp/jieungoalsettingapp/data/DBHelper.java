package com.developerjp.jieungoalsettingapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "goals.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_GOAL = "goal_table";
    public static final String TABLE_GOAL_DETAIL = "goal_detail_table";

    // Columns of goal_table
    public static final String SPECIFIC_COLUMN_ID = "id";
    public static final String SPECIFIC_COLUMN_TEXT = "specific_text";

    // Columns of goal_detail_table
    public static final String GOAL_DETAIL_COLUMN_ID = "id";
    public static final String GOAL_DETAIL_COLUMN_SPECIFIC_ID = "specific_id";
    public static final String GOAL_DETAIL_COLUMN_MEASURABLE = "measurable";
    public static final String GOAL_DETAIL_COLUMN_TIME_BOUND = "time_bound";
    public static final String GOAL_DETAIL_COLUMN_TIMESTAMP = "timestamp";

    // Date format for storing/retrieving datetime in SQLite
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    // SQL statement to create goal_table
    private static final String SQL_CREATE_SPECIFIC_TABLE =
            "CREATE TABLE " + TABLE_GOAL + " (" +
                    SPECIFIC_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SPECIFIC_COLUMN_TEXT + " TEXT)";

    // SQL statement to create goal_detail_table
    private static final String SQL_CREATE_GOAL_DETAIL_TABLE =
            "CREATE TABLE " + TABLE_GOAL_DETAIL + " (" +
                    GOAL_DETAIL_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    GOAL_DETAIL_COLUMN_SPECIFIC_ID + " INTEGER," +
                    GOAL_DETAIL_COLUMN_MEASURABLE + " INTEGER," +
                    GOAL_DETAIL_COLUMN_TIME_BOUND + " TEXT," +
                    GOAL_DETAIL_COLUMN_TIMESTAMP + " DATETIME," +
                    "FOREIGN KEY (" + GOAL_DETAIL_COLUMN_SPECIFIC_ID + ") REFERENCES " +
                    TABLE_GOAL + "(" + SPECIFIC_COLUMN_ID + "))";

    // SQL statement to drop goal_table
    private static final String SQL_DELETE_SPECIFIC_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_GOAL;

    // SQL statement to drop goal_detail_table
    private static final String SQL_DELETE_GOAL_DETAIL_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_GOAL_DETAIL;

    // Singleton instance
    private static volatile DBHelper instance;

    // Private constructor to prevent direct instantiation
    private DBHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    // getInstance method for Singleton
    public static DBHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DBHelper.class) {
                if (instance == null) {
                    instance = new DBHelper(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables
        db.execSQL(SQL_CREATE_SPECIFIC_TABLE);
        db.execSQL(SQL_CREATE_GOAL_DETAIL_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop existing tables if they exist and recreate
        db.execSQL(SQL_DELETE_GOAL_DETAIL_TABLE);
        db.execSQL(SQL_DELETE_SPECIFIC_TABLE);
        onCreate(db);
    }


    public boolean isSpecificExists(String specificText) {
        SQLiteDatabase db = getWritableDatabase();
        boolean exists = false;

        try {
            String query = "SELECT * FROM " + TABLE_GOAL + " WHERE " + SPECIFIC_COLUMN_TEXT + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{specificText});

            if (cursor.moveToFirst()) {
                exists = true; // Duplicate found
            }

            cursor.close(); // Close the cursor after use
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close(); // Close database connection
        }

        return exists;
    }


    public long insertSpecific(String specificText) {
        SQLiteDatabase db = getWritableDatabase();
        long specificId = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(SPECIFIC_COLUMN_TEXT, specificText);

            specificId = db.insert(TABLE_GOAL, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close(); // Close database connection
        }

        return specificId;
    }


    public long insertGoalDetail(int specificId, int measurable, String timeBound, long timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        long goalDetailId = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(GOAL_DETAIL_COLUMN_SPECIFIC_ID, specificId);
            values.put(GOAL_DETAIL_COLUMN_MEASURABLE, measurable);
            values.put(GOAL_DETAIL_COLUMN_TIME_BOUND, timeBound);
            values.put(GOAL_DETAIL_COLUMN_TIMESTAMP, DATE_FORMAT.format(new Date(timestamp)));

            goalDetailId = db.insert(TABLE_GOAL_DETAIL, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close(); // Close database connection
        }

        return goalDetailId;
    }


    public List<GoalDetail> getAllGoalDetailsWithSpecificText() {
        SQLiteDatabase db = getReadableDatabase();
        List<GoalDetail> goalDetailList = new ArrayList<>();

        try {
            // Define the projection to fetch specific_id, measurable, time_bound, timestamp,
            // and specific_text (joining specific_table with goal_detail_table)
            String[] projection = {
                    TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_ID,
                    TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_SPECIFIC_ID,
                    TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_MEASURABLE,
                    TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_TIME_BOUND,
                    TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_TIMESTAMP
            };

            // Perform a JOIN query on goal_detail_table and goal_table
            String query = "SELECT " + TextUtils.join(",", projection) +
                    " FROM " + TABLE_GOAL_DETAIL +
                    " INNER JOIN " + TABLE_GOAL +
                    " ON " + TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_SPECIFIC_ID +
                    " = " + TABLE_GOAL + "." + SPECIFIC_COLUMN_ID;

            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst()) {
                do {
                    // Retrieve column indices
                    int idIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_ID);
                    int specificIdIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_SPECIFIC_ID);
                    int measurableIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_MEASURABLE);
                    int timeBoundIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIME_BOUND);
                    int timestampIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIMESTAMP);

                    // Check if indices are valid
                    if (idIndex == -1 || specificIdIndex == -1 || measurableIndex == -1 ||
                            timeBoundIndex == -1 || timestampIndex == -1) {
                        // Handle the case where one or more indices are missing
                        continue; // or log an error, skip processing, etc.
                    }

                    // Retrieve values
                    int id = cursor.getInt(idIndex);
                    int specificId = cursor.getInt(specificIdIndex);
                    int measurable = cursor.getInt(measurableIndex);
                    String timeBound = cursor.getString(timeBoundIndex);
                    String timestampStr = cursor.getString(timestampIndex);

                    // Parse timestamp string to Date object
                    Date timestamp = DATE_FORMAT.parse(timestampStr);

                    // Fetch specific text using DBHelper
                    String specificText = getSpecificText(specificId);

                    // Create GoalDetail object and add to list
                    GoalDetail goalDetail = new GoalDetail(id, specificId, measurable, timeBound, timestamp, specificText);
                    goalDetail.setId(id);
                    goalDetailList.add(goalDetail);

                } while (cursor.moveToNext());
            }

            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close(); // Close database connection
        }

        return goalDetailList;
    }


    private String getSpecificText(int specificId) {
        SQLiteDatabase db = getReadableDatabase();
        String specificText = "";

        // Define columns to retrieve
        String[] columns = {SPECIFIC_COLUMN_TEXT};

        // Define selection criteria
        String selection = SPECIFIC_COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(specificId)};

        Cursor cursor = db.query(
                TABLE_GOAL,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        // Check if the cursor contains any rows
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    // Retrieve specific text
                    int columnIndex = cursor.getColumnIndex(SPECIFIC_COLUMN_TEXT);
                    if (columnIndex != -1) {
                        specificText = cursor.getString(columnIndex);
                    } else {
                        // Handle the case where the column index is missing
                        Log.e("DBHelper", "Column '" + SPECIFIC_COLUMN_TEXT + "' not found in the database.");
                    }
                }
            } finally {
                cursor.close();
            }
        }

        // Close database connection
        db.close();

        return specificText;
    }


    public GoalDetail getGoalDetailBySpecificId(int specificId) {
        SQLiteDatabase db = getReadableDatabase();
        GoalDetail goalDetail = null;

        String[] projection = {
                GOAL_DETAIL_COLUMN_ID,
                GOAL_DETAIL_COLUMN_MEASURABLE,
                GOAL_DETAIL_COLUMN_TIME_BOUND,
                GOAL_DETAIL_COLUMN_TIMESTAMP
        };

        String selection = GOAL_DETAIL_COLUMN_SPECIFIC_ID + " = ?";
        String[] selectionArgs = {String.valueOf(specificId)};

        Cursor cursor = db.query(
                TABLE_GOAL_DETAIL,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int idIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_ID);
                    int measurableIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_MEASURABLE);
                    int timeBoundIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIME_BOUND);
                    int timestampIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIMESTAMP);

                    // Check if indices are valid
                    if (idIndex == -1 || measurableIndex == -1 || timeBoundIndex == -1 || timestampIndex == -1) {
                        // Handle the case where one or more indices are missing
                        return null; // or throw an exception, log an error, etc.
                    }

                    // Retrieve values
                    int id = cursor.getInt(idIndex);
                    int measurable = cursor.getInt(measurableIndex);
                    String timeBound = cursor.getString(timeBoundIndex);
                    String timestampStr = cursor.getString(timestampIndex);

                    Date timestamp = null;
                    try {
                        timestamp = DATE_FORMAT.parse(timestampStr);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    // Fetch specific text using DBHelper
                    String specificText = getSpecificText(specificId);

                    // Create GoalDetail object
                    goalDetail = new GoalDetail(id, specificId, measurable, timeBound, timestamp, specificText);
                }
            } finally {
                cursor.close();
            }
        }

        db.close();

        return goalDetail;
    }


    public void deleteGoalsBySpecificId(int specificId) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();

            // Delete rows from goal_detail_table with the given specific_id
            String goalDetailSelection = GOAL_DETAIL_COLUMN_SPECIFIC_ID + " = ?";
            String[] goalDetailSelectionArgs = {String.valueOf(specificId)};
            db.delete(TABLE_GOAL_DETAIL, goalDetailSelection, goalDetailSelectionArgs);

            // Delete the row from goal_table with the given specific_id
            String specificSelection = SPECIFIC_COLUMN_ID + " = ?";
            String[] specificSelectionArgs = {String.valueOf(specificId)};
            db.delete(TABLE_GOAL, specificSelection, specificSelectionArgs);

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
            db.close(); // Close database connection
        }
    }


    public void updateGoalDetail(int specificId, @NotNull String specificText, int measurable, @NotNull String timeBound) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Check if specificId already exists in goal_table
            String[] specificProjection = { SPECIFIC_COLUMN_ID };
            String specificSelection = SPECIFIC_COLUMN_ID + " = ?";
            String[] specificSelectionArgs = { String.valueOf(specificId) };

            Cursor cursor = db.query(TABLE_GOAL, specificProjection, specificSelection, specificSelectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                // specificId exists, update the specificText
                ContentValues specificValues = new ContentValues();
                specificValues.put(SPECIFIC_COLUMN_TEXT, specificText);

                db.update(TABLE_GOAL, specificValues, specificSelection, specificSelectionArgs);
            } else {
                // specificId does not exist, insert it
                ContentValues specificValues = new ContentValues();
                specificValues.put(SPECIFIC_COLUMN_ID, specificId);
                specificValues.put(SPECIFIC_COLUMN_TEXT, specificText);

                db.insert(TABLE_GOAL, null, specificValues);
            }
            cursor.close();

            // Insert new row in goal_detail_table with timestamp
            ContentValues goalDetailValues = new ContentValues();
            goalDetailValues.put(GOAL_DETAIL_COLUMN_SPECIFIC_ID, specificId);
            goalDetailValues.put(GOAL_DETAIL_COLUMN_MEASURABLE, measurable);
            goalDetailValues.put(GOAL_DETAIL_COLUMN_TIME_BOUND, timeBound);
            goalDetailValues.put(GOAL_DETAIL_COLUMN_TIMESTAMP, DATE_FORMAT.format(new Date())); // Current timestamp

            db.insert(TABLE_GOAL_DETAIL, null, goalDetailValues);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    public List<GoalDetail> getGoalsByMeasurable(int measurable) {
        List<GoalDetail> goalList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        try {
            String query = "SELECT * FROM " + TABLE_GOAL_DETAIL +
                    " INNER JOIN " + TABLE_GOAL +
                    " ON " + TABLE_GOAL_DETAIL + "." + GOAL_DETAIL_COLUMN_SPECIFIC_ID +
                    " = " + TABLE_GOAL + "." + SPECIFIC_COLUMN_ID +
                    " WHERE " + GOAL_DETAIL_COLUMN_MEASURABLE + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(measurable)});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_ID);
                    int id = (idIndex >= 0) ? cursor.getInt(idIndex) : -1; // Use a default value or handle appropriately

                    int specificIdIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_SPECIFIC_ID);
                    int specificId = (specificIdIndex >= 0) ? cursor.getInt(specificIdIndex) : -1;

                    int timeBoundIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIME_BOUND);
                    String timeBound = (timeBoundIndex >= 0) ? cursor.getString(timeBoundIndex) : null;

                    int timestampIndex = cursor.getColumnIndex(GOAL_DETAIL_COLUMN_TIMESTAMP);
                    String timestampStr = (timestampIndex >= 0) ? cursor.getString(timestampIndex) : null;


                    Date timestamp = null;
                    try {
                        timestamp = DATE_FORMAT.parse(timestampStr);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    String specificText = getSpecificText(specificId);

                    GoalDetail goalDetail = new GoalDetail(id, specificId, measurable, timeBound, timestamp, specificText);
                    goalList.add(goalDetail);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }

        return goalList;
    }
}
