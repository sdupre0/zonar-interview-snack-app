package com.example.snackapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

public class SnackListAdapter extends ArrayAdapter<Snack> {

    List<Snack> data;
    Context context;
    boolean vegFilter = true, nonFilter = true;

    public SnackListAdapter(Context c, List<Snack> d) {
        super(c, 0, d);
        this.context = c;
        this.data = d;
    }

    public int getCount() {
        return data.size();
    }

    public Snack getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        final Snack snack = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_snackview, parent, false);
        }
        CheckBox snackCheck = convertView.findViewById(R.id.snack_check);
        TextView snackName = convertView.findViewById(R.id.snack_name);

        snackCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((MainActivity) context).snackToggle(snack, isChecked);
            }
        });

        // color snack name according to veggie or not
        if (snack.veggie) {
            snackName.setTextColor(context.getResources().getColor(R.color.colorVeggie));
        } else {
            snackName.setTextColor(context.getResources().getColor(R.color.colorNonVeggie));
        }
        snackName.setText(snack.name);

        return convertView;
    }
}