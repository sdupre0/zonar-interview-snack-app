package com.example.snackapp;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // permanent snack list to provide data to menu
    private List<Snack> snackList = new ArrayList<>();
    // snack order list
    private List<Snack> order = new ArrayList<>();
    // list filters
    private boolean vegFilter = true, nonFilter = true;
    private CheckBox vegFilterCheck, nonFilterCheck;
    // snack list view and adapter
    private ListView snackListView;
    private SnackListAdapter snackAdapter;

    // setup add snack button with the following two overrides
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_snack_button, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_snack_button) {

            final AlertDialog.Builder diaBuilder = new AlertDialog.Builder(MainActivity.this);
            // use custom dialog layout
            final View layout = getLayoutInflater().inflate(R.layout.dialog_add_snack, null);
            final EditText snackName = layout.findViewById(R.id.snack_name);
            final CheckBox vegCheck = layout.findViewById(R.id.veg_checkbox);

            diaBuilder.setView(layout)
                    .setPositiveButton(R.string.add_dialog_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // add the new snack to the snack list
                                    Snack newSnack = new Snack(snackName.getText().toString(), vegCheck.isChecked());
                                    addSnack(newSnack);

                                    dialog.dismiss();
                                }
                            })
                    .setNegativeButton(R.string.dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            diaBuilder.show();
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String snackString;
        // load list of snacks from shared preferences
        // preference file read -> string -> JSON object -> JSON array
        SharedPreferences prefs = getSharedPreferences("snack-prefs", MODE_PRIVATE);
        if (prefs.contains("snacks")) {
            snackString = prefs.getString("snacks", "");
            if (snackString.isEmpty()) {
                throw new Error("Snack list exists but is empty");
            }
        } else {
            // no existing prefs, so do first time setup of default snacks
            // reading from a raw json resource file
            InputStream inStream = getResources().openRawResource(R.raw.snacks);
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
                int i;
                while ((i = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, i);
                }
                inStream.close();
            } catch (IOException ioe) {
                Log.e(TAG, ioe.getMessage());
            }
            snackString = writer.toString();
            // now that the string is loaded from json, save to prefs for next load
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putString("snacks", snackString);
            prefEdit.apply();
        }
        try {
            JSONObject jsonObject = new JSONObject(snackString);
            JSONArray jsonSnackArray = jsonObject.getJSONArray("snacks");

            // make each Snack object and load into global snackList ArrayList
            for (int i = 0; i < jsonSnackArray.length(); i++) {
                JSONObject currObj = jsonSnackArray.getJSONObject(i);
                Snack newSnack = new Snack(currObj.getString("name"), currObj.getBoolean("veg"));
                snackList.add(newSnack);
            }
        } catch (JSONException jsone) {
            // preference issue, delete them
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.clear().apply();
            Log.e(TAG, jsone.getMessage());
        }

        // initialize list view
        snackListView = findViewById(R.id.snack_listview);
        snackListView.setDivider(null);
        snackListView.setDividerHeight(0);
        // attach snack list adapter
        List<Snack> adapterList = new ArrayList<>(snackList);
        snackAdapter = new SnackListAdapter(this, adapterList);
        snackListView.setAdapter(snackAdapter);

        // load and display list of snacks
        //loadSnackListView();

        // setup filters
        vegFilterCheck = findViewById(R.id.veggie_filter);
        nonFilterCheck = findViewById(R.id.non_veggie_filter);

        vegFilterCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                vegFilter = isChecked;
                fillAdapter();
            }
        });

        nonFilterCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                nonFilter = isChecked;
                fillAdapter();
            }
        });

        // setup submit order button
        TextView submitOrderButton = findViewById(R.id.submit_order_button);
        submitOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // build string of order
                StringBuilder sb = new StringBuilder();
                sb.append(getString(R.string.order_dialog_text));
                for (int i = 0; i < order.size(); i++) {
                    sb.append("\n");
                    sb.append(order.get(i).name);
                }

                AlertDialog.Builder diaBuilder = new AlertDialog.Builder(MainActivity.this);
                diaBuilder.setTitle(getString(R.string.order_dialog_title))
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.order_dialog_confirm,
                                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // empty function that sends order to API
                        submitOrder();
                        // reset order form for next customer
                        resetOrderForm();
                        dialog.dismiss();
                    }
                })
                        .setNegativeButton(R.string.dialog_cancel,
                                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                diaBuilder.show();
            }
        });
    }

    /*private void loadSnackListView() {
        snackAdapter.clear();
        for (int i = 0; i < snackList.size(); i++) {
            final Snack currentSnack = snackList.get(i);
            // filter out veggies or non-veggies according to filter
            if ((vegFilter && currentSnack.veggie) || (nonFilter && !currentSnack.veggie)) {
                final LinearLayout newSnackView = new LinearLayout(this);

                // add CheckBox with onCheckedChanged event for adding snack to order
                CheckBox newSnackCheck = new CheckBox(this);
                newSnackCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        snackToggle(currentSnack, isChecked);
                    }
                });
                newSnackView.addView(newSnackCheck);

                // add TextView with name of snack
                TextView newText = new TextView(this);
                newText.setText(currentSnack.name);

                // color snack name according to veggie or not
                if (snackList.get(i).veggie) {
                    newText.setTextColor(getResources().getColor(R.color.colorVeggie));
                } else {
                    newText.setTextColor(getResources().getColor(R.color.colorNonVeggie));
                }
                newSnackView.addView(new TextView(this));

                // add snack to list
                snackListView.addView(newSnackView);
            }
        }
    }*/

    public void snackToggle(Snack snack, boolean add) {
        // add or remove snack from order depending on whether snack in list is checked or not
        if (add) {
            order.add(snack);
        } else {
            order.remove(snack);
        }
    }

    private void submitOrder() {
        // this is where the function to make an API call and submit the order would be
    }

    private void addSnack(Snack newSnack) {
        // first, adds new snack to existing list
        snackList.add(newSnack);
        snackAdapter.add(newSnack);
        snackAdapter.notifyDataSetChanged();

        // this is where the function to make an API call and submit the new snack would be
        // instead, I'm saving it to the list in preferences of existing default snacks
        SharedPreferences prefs = getSharedPreferences("snack-prefs", MODE_PRIVATE);
        if (prefs.contains("snacks")) {
            try {
                // get existing
                String snackString = prefs.getString("snacks", "");
                JSONObject json = new JSONObject(snackString);
                JSONArray jarray = json.getJSONArray("snacks");

                // insert new snack
                JSONObject newObj = new JSONObject();
                newObj.put("name", newSnack.name);
                newObj.put("veg", newSnack.veggie);
                jarray.put(newObj);

                // update data
                json.put("snacks", jarray);
                snackString = json.toString();

                SharedPreferences.Editor prefEdit = prefs.edit();
                prefEdit.putString("snacks", snackString);
                prefEdit.apply();

            } catch (JSONException jsone) {
                Log.e(TAG, jsone.getMessage());
            }
        }
    }

    private void fillAdapter() {
        snackAdapter.clear();
        for (int i = 0; i < snackList.size(); i++) {
            Snack currSnack = snackList.get(i);
            if ((vegFilter && currSnack.veggie) || (nonFilter && !currSnack.veggie)) {
                snackAdapter.add(currSnack);
            }
        }
        snackAdapter.notifyDataSetChanged();
    }

    private void resetOrderForm() {
        order.clear();
        vegFilter = true;
        vegFilterCheck.setChecked(vegFilter);
        nonFilter = true;
        nonFilterCheck.setChecked(nonFilter);
        fillAdapter();
    }
}
