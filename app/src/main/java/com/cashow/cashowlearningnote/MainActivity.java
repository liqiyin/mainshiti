package com.cashow.cashowlearningnote;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.layout_note_list)
    ViewGroup layoutNoteList;

    private List<String> noteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        listAssetFiles("");

        for (String noteName : noteList) {
            layoutNoteList.addView(getNoteButtonView(noteName));
        }
    }

    private View getNoteButtonView(String noteName) {
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.include_note_button, layoutNoteList, false);
        TextView textView = (TextView) view;
        textView.setText(noteName.substring(0, noteName.length() - 3));

        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra("noteName", noteName);
            startActivity(intent);
        });
        return view;
    }

    private void listAssetFiles(String path) {
        noteList = new ArrayList<>();
        try {
            String [] list = getAssets().list(path);
            if (list.length > 0) {
                for (String file : list) {
                    if (file.endsWith(".md") && !file.equals("empty.md")) {
                        noteList.add(file);
                    }
                }
            }
        } catch (IOException e) {
        }
    }
}
