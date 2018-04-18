package com.cashow.cashowlearningnote;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import br.tiagohm.markdownview.MarkdownView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteActivity extends AppCompatActivity {
    @BindView(R.id.markdown_view)
    MarkdownView markdownView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        ButterKnife.bind(this);

        String noteName = getIntent().getStringExtra("noteName");

        setTitle(noteName.substring(0, noteName.length() - 3));

        markdownView.addStyleSheet(new MyGithub());
        markdownView.loadMarkdownFromAsset(noteName);
    }
}
