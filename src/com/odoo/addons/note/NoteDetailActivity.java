package com.odoo.addons.note;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.odoo.addons.note.dialogs.NoteColorDialog.OnColorSelectListener;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.base.ir.Attachments;
import com.odoo.base.ir.Attachments.Types;
import com.odoo.note.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.util.OControls;
import com.odoo.util.ODate;
import com.odoo.util.logger.OLog;

public class NoteDetailActivity extends Activity {

	private NoteNote mNote;
	private NoteStage mStage;
	private Cursor note_cr = null;
	private EditText name, memo;
	private Boolean isDirty = false;
	private Integer mStageId = 0;
	private Integer color = 0;
	private String note_name, note_memo;
	private Attachments attachment;
	private Menu mMenu;
	private Boolean open = true;
	private Integer trashed = 0;

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
		attachment = new Attachments(this);
		mStage = new NoteStage(this);
		Bundle extra = getIntent().getExtras();
		if (extra.containsKey(Note.KEY_NOTE_ID)) {
			note_cr = mNote.resolver().query(extra.getInt(Note.KEY_NOTE_ID));
			note_cr.moveToFirst();
			mStageId = note_cr.getInt(note_cr.getColumnIndex("stage_id"));
			open = (note_cr.getString(note_cr.getColumnIndex("open"))
					.equals("true")) ? true : false;
			trashed = note_cr.getInt(note_cr.getColumnIndex("trashed"));
			color = note_cr.getInt(note_cr.getColumnIndex("color"));
			initControls(color);
			createView();
		}

		if (extra.containsKey(Intent.EXTRA_SUBJECT)
				|| extra.containsKey(Intent.EXTRA_TEXT)) {
			initControls(color);
			if (extra.containsKey(Intent.EXTRA_SUBJECT))
				name.setText(extra.getString(Intent.EXTRA_SUBJECT));
			memo.setText(extra.getString(Intent.EXTRA_TEXT));
			isDirty = true;
		}

		if (extra.containsKey(Note.KEY_STAGE_ID)) {
			mStageId = extra.getInt(Note.KEY_STAGE_ID);
			initControls(color);
		}
		if (mStageId == 0) {
			Cursor cr = mStage.resolver().query(null, null, "sequence");
			if (cr.moveToFirst()) {
				mStageId = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
			} else {
				Toast.makeText(this, "Sorry No stages found !",
						Toast.LENGTH_LONG).show();
				finish();
			}
		}

		note_name = name.getText().toString();
		note_memo = memo.getText().toString();
	}

	private void initControls(int color) {
		int background_color = NoteUtil.getBackgroundColor(color);
		findViewById(R.id.note_detail_view)
				.setBackgroundColor(background_color);
		name = (EditText) findViewById(R.id.note_name);
		memo = (EditText) findViewById(R.id.note_memo);
		name.setTextColor(NoteUtil.getTextColor(color));
		memo.setTextColor(NoteUtil.getTextColor(color));
	}

	private void createView() {
		OControls.setText(findViewById(R.id.note_detail_view), R.id.note_name,
				note_cr.getString(note_cr.getColumnIndex("name")));
		String content = note_cr.getString(note_cr.getColumnIndex("memo"));
		SpannableStringBuilder b = new SpannableStringBuilder(
				Html.fromHtml(content));
		memo.setText(b);
	}

	@Override
	public void onBackPressed() {
		if (isDirty()) {
			saveNote();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_note_detail, menu);
		mMenu = menu;
		if (note_cr == null) {
			mMenu.findItem(R.id.menu_note_archive).setVisible(false);
			mMenu.findItem(R.id.menu_note_operation).setVisible(false);
		}
		if (trashed == 1) {
			mMenu.findItem(R.id.menu_note_delete).setTitle("Restore");
		}
		return true;
	}

	private void saveNote() {
		isDirty = false;
		String toast = "Note created";
		note_name = name.getText().toString();
		String html_content = Html.toHtml(memo.getText());
		note_memo = memo.getText().toString();
		OValues values = new OValues();
		values.put("name", note_name);
		values.put("memo", html_content);
		values.put("stage_id", mStageId);
		values.put("short_memo", mNote.storeShortMemo(values));
		values.put("color", color);
		values.put("open", open + "");

		if (note_cr == null) {
			// creating new note
			values.put("sequence", 0);
			mNote.resolver().insert(values);
		} else {
			// Updating note
			toast = "Note updated";
			int note_id = note_cr
					.getInt(note_cr.getColumnIndex(OColumn.ROW_ID));
			mNote.resolver().update(note_id, values);
		}
		Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
		onBackPressed();
	}

	private boolean isDirty() {
		if (TextUtils.isEmpty(name.getText())
				&& TextUtils.isEmpty(memo.getText())) {
			isDirty = false;
			Toast.makeText(this, "Empty note discarded", Toast.LENGTH_LONG)
					.show();
		} else {
			if (note_name.length() != name.getText().toString().length()
					|| note_memo.length() != memo.getText().toString().length()) {
				isDirty = true;
			}
		}
		return isDirty;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.menu_note_color:
			String selected = NoteUtil.getBackgroundColors()[color];
			NoteUtil.colorDialog(this, selected, new OnColorSelectListener() {

				@Override
				public void colorSelected(ODataRow color_data) {
					int old_color = color;
					color = color_data.getInt("index");
					if (old_color != color) {
						isDirty = true;
					}
					initControls(color);
				}
			}).show();
			break;
		case R.id.menu_note_attachment:
			attachment.newAttachment(Types.FILE);
			break;
		case R.id.menu_note_archive:
			isDirty = true;
			int iconRes = (open) ? R.drawable.ic_action_unarchive
					: R.drawable.ic_action_archive;
			open = !open;
			mMenu.findItem(R.id.menu_note_archive).setIcon(iconRes);
			break;
		case R.id.menu_note_delete:
			int note_id = note_cr
					.getInt(note_cr.getColumnIndex(OColumn.ROW_ID));
			OValues values = new OValues();
			values.put("trashed", (trashed == 1) ? 0 : 1);
			values.put("is_dirty", false);
			values.put("trashed_date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
			mNote.resolver().update(note_id, values);
			String toast = (trashed == 1) ? "Note restored"
					: "Note moved to trash";
			Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
			finish();
			break;
		case R.id.menu_note_make_copy:
			Bundle extras = new Bundle();
			extras.putString(Intent.EXTRA_SUBJECT, name.getText().toString());
			extras.putString(Intent.EXTRA_TEXT, Html.toHtml(memo.getText()));
			extras.putInt(Note.KEY_STAGE_ID, mStageId);
			Intent intent = new Intent(this, NoteDetailActivity.class);
			intent.putExtras(extras);
			startActivity(intent);
			finish();
			break;
		case R.id.menu_note_share:
			extras = new Bundle();
			extras.putString(Intent.EXTRA_SUBJECT, name.getText().toString());
			extras.putString(Intent.EXTRA_TEXT, memo.getText().toString());
			intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtras(extras);
			startActivity(intent);
			break;
		/*
		 * case R.id.menu_note_followers: break;
		 */
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			OValues vals = attachment.handleResult(requestCode, data);
			if (vals != null) {
				OLog.log(vals.toString());
				// mNote.addAttachment(vals, mStageId);
			}
		}
	}

}
