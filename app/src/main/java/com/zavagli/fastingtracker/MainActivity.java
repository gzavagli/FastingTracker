// TODO: put a decoration on the app icon with the current status and number of hours
// TODO: settings
// TODO: stats window
// TODO: remove log output
// TODO: create timer thread to update display once per minute
// TODO: create background timer thread to update app icon decoration once per hour

package com.zavagli.fastingtracker;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FastingTracker";

    public enum FastingState {Eating, Fasting, Unknown}
    public class CompleteState {
        FastingState fastingState;
        Date date;
    }

    private CompleteState currentState;
    private TextView textViewCurrentStatus;
    private TextView textViewDuration;
    private Button buttonStartEating;
    private Button buttonStartFasting;
    private Button buttonStats;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        textViewCurrentStatus = findViewById(R.id.textViewCurrentStatus);
        textViewDuration = findViewById(R.id.textViewDuration);

        buttonStartEating = findViewById(R.id.buttonStartEating);
        buttonStartEating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewCurrentStatus.setText("Eating");
                storeStatus(FastingState.Eating);
            }
        });

        buttonStartFasting = findViewById(R.id.buttonStopEating);
        buttonStartFasting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textViewCurrentStatus.setText("Fasting");
                storeStatus(FastingState.Fasting);
            }
        });

        buttonStats = findViewById(R.id.buttonStats);
        buttonStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
            }
        });
        buttonStats.setEnabled(true);

        // debug: output all entries to logcat
        //readAllFromDB();

        // at start, read last status update from DB and update display
        updateStatusAndDuration();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        updateStatusAndDuration();
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart()");
        updateStatusAndDuration();
        super.onRestart();
    }

    private void updateStatusAndDuration() {
        currentState = readCompleteStateFromDB();
        Log.d(TAG, "Complete state: date: " + currentState.date + ", state: " + currentState.fastingState);
        // update state display
        textViewCurrentStatus.setText(currentState.fastingState.toString());

        // update button activation status based on actual status
        switch (currentState.fastingState) {
            case Eating:
                buttonStartEating.setEnabled(false);
                buttonStartFasting.setEnabled(true);
                break;
            case Fasting:
                buttonStartEating.setEnabled(true);
                buttonStartFasting.setEnabled(false);
                break;
            case Unknown:
                buttonStartEating.setEnabled(true);
                buttonStartFasting.setEnabled(true);
                break;
        }

        // calculate how long we have been in this state
        Date currentDate = new Date();
        long duration = currentDate.getTime() - currentState.date.getTime();

        long hours = duration/3600/1000;
        Log.d(TAG, "hours: " + hours);

        long seconds = (duration - (hours * 3600000))/1000;
        Log.d(TAG, "seconds: " + seconds);
        long minutes = seconds / 60;
        Log.d(TAG, "minutes: " + minutes);

        // update duration display
        if (hours == 0) {
            if (minutes == 0) {
                textViewDuration.setText("< 1 min");
            } else {
                textViewDuration.setText(minutes + " min");
            }
        } else {
            if (minutes == 0) {
                textViewDuration.setText(hours + " h");
            } else {
                textViewDuration.setText(hours + " h " + minutes + " min");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void storeStatus(FastingState status) {
        Log.d(TAG, "storeStatus(" + status.toString() + ")");

        if (status == FastingState.Unknown) {
            // we are not allowed to store the status "Unknown"
            return;
        }

        saveToDB(new Date(), status);

        updateStatusAndDuration();
    }

    private void saveToDB(Date date, FastingState event) {
        Log.d(TAG, "saveToDB with date: " + date.toString() + ", state: " + event.toString());

        SQLiteDatabase database = new SQLiteDBHelper(this).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SQLiteDBHelper.EVENTS_COLUMN_DATE, date.toString());
        values.put(SQLiteDBHelper.EVENTS_COLUMN_EVENT, event.toString());
        long newRowId = database.insert(SQLiteDBHelper.EVENTS_TABLE_NAME, null, values);

        Log.d(TAG, "saveToDB newRowId: " + newRowId);
    }

    private void readAllFromDB() {
        Log.d(TAG, "readAllFromDB()");

        SQLiteDatabase database = new SQLiteDBHelper(this).getReadableDatabase();

        String[] projection = {
                SQLiteDBHelper.EVENTS_COLUMN_ID,
                SQLiteDBHelper.EVENTS_COLUMN_DATE,
                SQLiteDBHelper.EVENTS_COLUMN_EVENT
        };

        String selection =
                SQLiteDBHelper.EVENTS_COLUMN_DATE + " like ? and " +
                        SQLiteDBHelper.EVENTS_COLUMN_EVENT + " like ?";

        //String[] selectionArgs = {"%" + date + "%", "%" + event + "%"};

        Cursor cursor = database.query(
                SQLiteDBHelper.EVENTS_TABLE_NAME,   // The table to query
                projection,                         // The columns to return
                //selection,                        // The columns for the WHERE clause
                null,                       // The columns for the WHERE clause
                //selectionArgs,                    // The values for the WHERE clause
                null,                    // The values for the WHERE clause
                null,                       // don't group the rows
                null,                        // don't filter by row groups
                null                        // don't sort
        );

        Log.d(TAG, "The total cursor count is " + cursor.getCount());

        int dateIndex = cursor.getColumnIndex(SQLiteDBHelper.EVENTS_COLUMN_DATE);
        int eventIndex = cursor.getColumnIndex(SQLiteDBHelper.EVENTS_COLUMN_EVENT);
        int indexIndex = cursor.getColumnIndex(SQLiteDBHelper.EVENTS_COLUMN_ID);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Log.d(TAG, "ID: " + cursor.getString(indexIndex) + ", date: " + cursor.getString(dateIndex) + ", event: " + cursor.getString(eventIndex));
        }
    }

    private CompleteState readCompleteStateFromDB() {
        Log.d(TAG, "readCompleteStateFromDB()");

        SQLiteDatabase database = new SQLiteDBHelper(this).getReadableDatabase();

        String[] projection = {
                SQLiteDBHelper.EVENTS_COLUMN_ID,
                SQLiteDBHelper.EVENTS_COLUMN_DATE,
                SQLiteDBHelper.EVENTS_COLUMN_EVENT
        };

        String selection =
                SQLiteDBHelper.EVENTS_COLUMN_DATE + " like ? and " +
                        SQLiteDBHelper.EVENTS_COLUMN_EVENT + " like ?";

        //String[] selectionArgs = {"%" + date + "%", "%" + event + "%"};

        // TODO: improve the query search for the most recent event

        Cursor cursor = database.query(
                SQLiteDBHelper.EVENTS_TABLE_NAME,   // The table to query
                projection,                         // The columns to return
                //selection,                        // The columns for the WHERE clause
                null,                       // The columns for the WHERE clause
                //selectionArgs,                    // The values for the WHERE clause
                null,                    // The values for the WHERE clause
                null,                       // don't group the rows
                null,                        // don't filter by row groups
                null                        // don't sort
        );

        CompleteState cs = new CompleteState();

        if (cursor.getCount() == 0) {
            cs.date = new Date();
            cs.fastingState = FastingState.Unknown;
        } else {
            int dateIndex = cursor.getColumnIndex(SQLiteDBHelper.EVENTS_COLUMN_DATE);
            int eventIndex = cursor.getColumnIndex(SQLiteDBHelper.EVENTS_COLUMN_EVENT);
            cursor.moveToLast();
            FastingState fs = FastingState.valueOf(cursor.getString(eventIndex));
            cs.date = new Date(cursor.getString(dateIndex));
            cs.fastingState = fs;
        }

        return cs;
    }

}
