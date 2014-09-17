package com.odoo.addons.note;

import odoo.controls.OWebTextView;
import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.note.R;
import com.odoo.util.OControls;

public class NoteDetailActivity extends Activity {

	private NoteNote mNote;
	private Cursor note_cr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.note_detail_view);
		setTitle("");
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(
				new ColorDrawable(Color.parseColor("#22000000")));
		init();
	}

	private void init() {
		mNote = new NoteNote(this);
		Bundle extra = getIntent().getExtras();
		if (extra.containsKey(Note.KEY_NOTE_ID)) {
			note_cr = mNote.resolver().query(extra.getInt(Note.KEY_NOTE_ID));
			note_cr.moveToFirst();
			createView();
		}
	}

	private void createView() {
		int color = note_cr.getInt(note_cr.getColumnIndex("color"));
		int background_color = NoteUtil.getBackgroundColor(color);
		findViewById(R.id.note_detail_view)
				.setBackgroundColor(background_color);
		OControls.setText(findViewById(R.id.note_detail_view), R.id.note_name,
				note_cr.getString(note_cr.getColumnIndex("name")));
		OWebTextView memo = (OWebTextView) findViewById(R.id.note_memo);
		memo.setTextColor(NoteUtil.getTextColor(color));
		memo.setHtmlContent(note_cr.getString(note_cr.getColumnIndex("memo")));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_note_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
