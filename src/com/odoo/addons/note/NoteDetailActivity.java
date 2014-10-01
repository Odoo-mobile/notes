package com.odoo.addons.note;

import java.util.List;
import java.util.TimeZone;

import odoo.controls.misc.ONoteAttachmentView;
import odoo.controls.misc.ONoteAttachmentView.AttachmentViewListener;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.addons.note.dialogs.NoteColorDialog.OnColorSelectListener;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.addons.note.reminder.NoteReminder;
import com.odoo.base.ir.Attachments;
import com.odoo.base.ir.Attachments.Types;
import com.odoo.base.ir.IrAttachment;
import com.odoo.notes.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.util.OControls;
import com.odoo.util.ODate;

public class NoteDetailActivity extends FragmentActivity implements
		AttachmentViewListener {
	public static final String ACTION_REMINDER_CALL = "com.odoo.addons.note.NoteDetailActivity.REMINDER_CALL";
	private NoteNote mNote;
	private NoteStage mStage;
	private Cursor note_cr = null;
	private EditText /* name, */memo;
	private TextView last_update_on;
	private Boolean isDirty = false;
	private Integer mStageId = 0;
	private Integer color = 0;
	private String /* note_name, */note_memo;
	private Attachments attachment;
	private Menu mMenu;
	private Boolean open = true;
	private Integer trashed = 0;
	private ONoteAttachmentView mAttachmentView;
	private Context mContext;
	private NoteReminder mReminder;
	private String reminderDate = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
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
		Integer note_id = 0;
		note_id = (extra.containsKey(Note.KEY_NOTE_ID)) ? extra
				.getInt(Note.KEY_NOTE_ID) : null;
		initData(note_id, extra);
		if (getIntent().getAction() != null) {
			if (getIntent().getType().equals("text/plain")) {
				initData(note_id, extra);
				isDirty = true;
			} else {
				List<OValues> attachments = attachment
						.handleIntentRequest(getIntent());
				note_id = null;
				if (attachments.size() > 0) {
					for (OValues v : attachments) {
						note_id = mNote.addAttachment(v, mStageId, note_id);
					}
					initData(note_id, extra);
					isDirty = true;
				}
			}
		}
		initReminderControls();
	}

	private void initReminderControls() {
		mReminder = new NoteReminder(this, getSupportFragmentManager());
		mReminder.initControls(findViewById(R.id.reminder_controls),
				reminderDate);
	}

	private void initData(Integer note_id, Bundle extra) {
		if (note_id != null) {
			note_cr = mNote.resolver().query(note_id);
			note_cr.moveToFirst();
			mStageId = note_cr.getInt(note_cr.getColumnIndex("stage_id"));
			open = (note_cr.getString(note_cr.getColumnIndex("open"))
					.equals("true")) ? true : false;
			trashed = note_cr.getInt(note_cr.getColumnIndex("trashed"));
			color = note_cr.getInt(note_cr.getColumnIndex("color"));
			initControls(color);

			String reminder = note_cr.getString(note_cr
					.getColumnIndex("reminder"));
			if (!reminder.equals("0"))
				reminderDate = reminder;
			Cursor cr = mNote.getAttachments(note_cr.getInt(note_cr
					.getColumnIndex(OColumn.ROW_ID)));
			mAttachmentView.removeAllViews();
			mAttachmentView.setVisibility(View.GONE);
			if (cr.getCount() > 0) {
				mAttachmentView.setVisibility(View.VISIBLE);
				mAttachmentView.createView(cr);
			}
			String edited_date = note_cr.getString(note_cr
					.getColumnIndex("local_write_date"));
			edited_date = ODate.getDate(this, edited_date, TimeZone
					.getDefault().getID(), "d MMM, h:m a");
			last_update_on.setText("Edited " + edited_date);
			createView();
		}

		if (extra.containsKey(Intent.EXTRA_SUBJECT)
				|| extra.containsKey(Intent.EXTRA_TEXT)) {
			initControls(color);
			String content = "";
			if (extra.containsKey(Intent.EXTRA_SUBJECT))
				content = extra.getString(Intent.EXTRA_SUBJECT);
			memo.setText(content + "\n" + extra.getString(Intent.EXTRA_TEXT));
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
				initControls(color);
			} else {
				Toast.makeText(this, getString(R.string.no_stage_found),
						Toast.LENGTH_LONG).show();
				finish();
			}
		}
		note_memo = memo.getText().toString();
	}

	private void initControls(int color) {
		int background_color = NoteUtil.getBackgroundColor(color);
		last_update_on = (TextView) findViewById(R.id.last_update_on);
		findViewById(R.id.note_detail_view)
				.setBackgroundColor(background_color);
		// name = (EditText) findViewById(R.id.note_name);
		memo = (EditText) findViewById(R.id.note_memo);
		// name.setTextColor(NoteUtil.getTextColor(color));
		memo.setTextColor(NoteUtil.getTextColor(color));
		last_update_on.setTextColor(NoteUtil.getTextColor(color));
		mAttachmentView = (ONoteAttachmentView) findViewById(R.id.note_attachments);
		mAttachmentView.setMaximumCols(3);
		mAttachmentView.setAttachmentViewListener(this);
	}

	private void createView() {
		/*
		 * OControls.setText(findViewById(R.id.note_detail_view),
		 * R.id.note_name, note_cr.getString(note_cr.getColumnIndex("name")));
		 */
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
			mMenu.findItem(R.id.menu_note_delete).setTitle(getString(R.string.restore));
		}
		return true;
	}

	private void saveNote() {
		isDirty = false;
		String toast = "Note created";
		// note_name = name.getText().toString();
		String html_content = Html.toHtml(memo.getText());
		note_memo = memo.getText().toString();
		OValues values = new OValues();
		values.put("name", "");
		values.put("memo", html_content);
		values.put("stage_id", mStageId);
		values.put("short_memo", mNote.storeShortMemo(values));
		values.put("color", color);
		values.put("open", open + "");
		String reminder = "0";
		if (mReminder.hasReminder()) {
			reminder = mReminder.getDateString();
		}
		values.put("reminder", reminder);

		if (note_cr == null) {
			// creating new note
			values.put("sequence", 0);
			int newNote_id = mNote.resolver().insert(values);
			if (mReminder.hasReminder()) {
				mReminder.setReminder(newNote_id, mReminder.getCal());
			}
		} else {
			// Updating note
			toast = "Note updated";
			int note_id = note_cr
					.getInt(note_cr.getColumnIndex(OColumn.ROW_ID));
			if (mReminder.hasReminder()) {
				if (reminderDate == null) {
					reminderDate = "";
				}
				if (!reminderDate.equals(mReminder.getDateString())) {
					mReminder.setReminder(note_id, mReminder.getCal());
				}
			}
			mNote.resolver().update(note_id, values);
		}
		Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
		mReminder.setHasReminder(false);
		reminderDate = null;
		onBackPressed();
	}

	private boolean isDirty() {
		if (TextUtils.isEmpty(memo.getText())) {
			isDirty = false;
			Toast.makeText(this, getString(R.string.note_discarded),
					Toast.LENGTH_LONG).show();
		} else {
			if (note_memo.length() != memo.getText().toString().length()) {
				isDirty = true;
			}
			if (!isDirty) {
				isDirty = (mReminder.hasReminder() || (reminderDate != null));
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
			attachment.newAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
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
			extras.putString(Intent.EXTRA_SUBJECT, /* name.getText().toString() */
					"");
			extras.putString(Intent.EXTRA_TEXT, Html.toHtml(memo.getText()));
			extras.putInt(Note.KEY_STAGE_ID, mStageId);
			Intent intent = new Intent(this, NoteDetailActivity.class);
			intent.putExtras(extras);
			startActivity(intent);
			finish();
			break;
		case R.id.menu_note_share:
			extras = new Bundle();
			extras.putString(Intent.EXTRA_SUBJECT, /* name.getText().toString() */
					"");
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
				Integer note_id = null;
				if (note_cr == null) {
					// creating quick note...
					note_id = mNote.addAttachment(vals, mStageId);
				} else {
					note_id = note_cr.getInt(note_cr
							.getColumnIndex(OColumn.ROW_ID));
					mNote.addAttachment(vals, mStageId, note_id);
					updateNote(note_id);
				}
				initData(note_id, getIntent().getExtras());
			}
		}
	}

	@Override
	public View getView(Cursor cr, int position, ViewGroup parent) {
		cr.moveToPosition(position);
		final int attachment_id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
		final int note_id = cr.getInt(cr.getColumnIndex("res_id"));
		String type = cr.getString(cr.getColumnIndex("file_type"));
		String file_uri = cr.getString(cr.getColumnIndex("file_uri"));
		View v = getLayoutInflater().inflate(
				R.layout.note_detail_attachment_item, parent, false);
		ImageView img = (ImageView) v.findViewById(R.id.attachment_image);
		OControls.setText(v, R.id.file_name,
				cr.getString(cr.getColumnIndex("name")));
		if (type.contains("image") && !file_uri.equals("false")
				&& !type.contains("svg")) {
			img.setImageURI(Uri.parse(file_uri));
		} else {
			img.setColorFilter(Color.parseColor("#66000000"));
			img.setImageResource(R.drawable.attachment);
		}
		img.setClickable(true);
		img.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				attachment.downloadAttachment(attachment_id);
			}
		});
		v.findViewById(R.id.remove_attachment).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						Builder dialog = new Builder(mContext);
						dialog.setMessage(getString(R.string.delete_attachment));
						dialog.setPositiveButton(getString(R.string.delete),
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										IrAttachment attachment = new IrAttachment(
												mContext);
										attachment.delete(attachment_id);
										updateNote(note_id);
										Toast.makeText(
												mContext,
												getString(R.string.attachment_removed),
												Toast.LENGTH_LONG).show();
										initData(note_id, getIntent()
												.getExtras());
									}
								});
						dialog.setNegativeButton(getString(R.string.cancel),
								null);
						dialog.show();
					}
				});
		return v;
	}

	private void updateNote(int note_id) {
		OValues values = new OValues();
		values.put(OColumn.ROW_ID, note_id);
		mNote.update(values, note_id);
	}
}
