package com.odoo.addons.notes;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.notes.dialogs.NoteColorDialog;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteStage;
import com.odoo.addons.notes.reminder.NoteReminder;
import com.odoo.addons.notes.utils.NoteUtil;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.utils.OActionBarUtils;
import com.odoo.core.utils.ODateUtils;

public class NoteDetail extends ActionBarActivity {
    public static final String ACTION_REMINDER_CALL = "com.odoo.addons.note.NoteDetailActivity.REMINDER_CALL";
    private NoteNote mNote;
    private NoteStage mStage;
    private NoteReminder mReminder;
    private String reminderDate = null;
    private ODataRow note_cr;
    private Menu mMenu;
    private Boolean open = true;
    private Integer trashed = 0;
    private TextView last_update_on;
    private Boolean isDirty = false;
    private Integer mStageId = 0;
    private Integer color = 0;
    private String /* note_name, */note_memo;
    private EditText /* name, */memo;
    private ActionBar actionBar;

    //    public static final String ACTION_ATTACH_FILE = "action_attach_file";
    //    private Attachments attachment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail_view);
        OActionBarUtils.setActionBar(this, false);
        actionBar = getSupportActionBar();
        actionBar.setTitle("Notes");
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(
                new ColorDrawable(Color.parseColor("#22000000")));
        init();
    }

    private void init() {
        mNote = new NoteNote(this, null);
//        attachment = new Attachments(this);
        mStage = new NoteStage(this, null);
        Bundle extra = getIntent().getExtras();
        Integer note_id;
        note_id = (extra.containsKey(Notes.KEY_NOTE_ID)) ? extra
                .getInt(Notes.KEY_NOTE_ID) : null;
        initData(note_id, extra);
//        String action = getIntent().getAction();
//        if (action != null && !action.equals(ACTION_ATTACH_FILE)) {
//            if (getIntent().getType() != null
//                    && getIntent().getType().equals("text/plain")) {
//                initData(note_id, extra);
//                isDirty = true;
//            } else {
//                List<OValues> attachments = attachment
//                        .handleIntentRequest(getIntent());
//                note_id = null;
//                if (attachments.size() > 0) {
//                    for (OValues v : attachments) {
//                        note_id = mNote.addAttachment(v, mStageId, note_id);
//                    }
//                    initData(note_id, extra);
//                    isDirty = true;
//                }
//            }
//        }
        initReminderControls();

//        if (action != null) {
//            if (action.equals(ACTION_ATTACH_FILE)) {
//                attachment.newAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
//            }
//        }
    }

    private void initReminderControls() {
        mReminder = new NoteReminder(this, getSupportFragmentManager());
        mReminder.initControls(findViewById(R.id.reminder_controls),
                reminderDate);
        mReminder.setHasReminder(false);
    }

    private void initData(Integer note_id, Bundle extra) {
        if (note_id != null) {
            note_cr = mNote.browse(note_id);
            mStageId = note_cr.getInt("stage_id");
            open = note_cr.getString("open").equals("true");
            trashed = note_cr.getInt("trashed");
            color = note_cr.getInt("color");
            initControls(color);

            String reminder = note_cr.getString("reminder");
            if (!reminder.equals("0"))
                reminderDate = reminder;
//            Cursor cr = mNote.getAttachments(note_cr.get(0).getInt(OColumn.ROW_ID+""));
//            mAttachmentView.removeAllViews();
//            mAttachmentView.setVisibility(View.GONE);
//            if (cr.getCount() > 0) {
//                mAttachmentView.setVisibility(View.VISIBLE);
//                mAttachmentView.createView(cr);
//            }
            String edited_date = ODateUtils.convertToDefault(note_cr.getString("_write_date"),
                    ODateUtils.DEFAULT_FORMAT, "d MMM, hh:mm a");
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
        if (extra.containsKey(Notes.KEY_STAGE_ID)) {
            mStageId = extra.getInt(Notes.KEY_STAGE_ID);
            initControls(color);
        }
        if (mStageId == 0) {
            mStageId = mStage.getDefaultNoteStageId();
            if (mStageId != 0) {
                initControls(color);
            } else {
                Toast.makeText(this, getString(R.string.label_no_stage_found),
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
        memo = (EditText) findViewById(R.id.note_memo);
        memo.setTextColor(NoteUtil.getTextColor(color));
        last_update_on.setTextColor(NoteUtil.getTextColor(color));


//        mAttachmentView = (ONoteAttachmentView) findViewById(R.id.note_attachments);
//        mAttachmentView.setMaximumCols(3);
//        mAttachmentView.setAttachmentViewListener(this);
    }

    private void createView() {
        /*
         * OControls.setText(findViewById(R.id.note_detail_view),
		 * R.id.note_name, note_cr.getString(note_cr.getColumnIndex("name")));
		 */
        String content = note_cr.getString("memo");
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
                isDirty = mReminder.hasReminder();
            }
        }
        return isDirty;
    }

    private void saveNote() {
        isDirty = false;
        String toast = getString(R.string.note_created);
        // note_name = name.getText().toString();
        String html_content = Html.toHtml(memo.getText());
        note_memo = memo.getText().toString();
        OValues values = new OValues();
        values.put("name", "");
        values.put("memo", html_content);
        values.put("stage_id", mStageId);
        values.put("short_memo", mNote.storeShortMemo(values));
        values.put("color", color);
        values.put("trashed", 0);
        values.put("open", open + "");
        String reminder = "0";
        if (mReminder.hasReminder()) {
            reminder = mReminder.getDateString();
        }
        values.put("reminder", reminder);

        if (note_cr == null) {
            // creating new note
            values.put("sequence", 0);
            int newNote_id = mNote.insert(values);
            if (mReminder.hasReminder()) {
                mReminder.setReminder(newNote_id, mReminder.getCal());
            }
        } else {
            // Updating note
            toast = getString(R.string.note_updated);
            int note_id = note_cr
                    .getInt(OColumn.ROW_ID);
            if (mReminder.hasReminder()) {
                if (reminderDate == null) {
                    reminderDate = "";
                }
                if (!reminderDate.equals(mReminder.getDateString())) {
                    mReminder.setReminder(note_id, mReminder.getCal());
                }
            }
            mNote.update(note_id, values);
        }
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
        mReminder.setHasReminder(false);
        reminderDate = null;
        onBackPressed();
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
            mMenu.findItem(R.id.menu_note_delete).setTitle(
                    getString(R.string.restore));
        }
        if (open) {
            mMenu.findItem(R.id.menu_note_archive).setIcon(R.drawable.ic_action_archive);
        } else {
            mMenu.findItem(R.id.menu_note_archive).setIcon(R.drawable.ic_action_unarchive);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_note_color:
                String selected = NoteUtil.getBackgroundColors()[color];
                NoteUtil.colorDialog(this, selected, new NoteColorDialog.OnColorSelectListener() {

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
//            case R.id.menu_note_attachment:
//                attachment.newAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
//                break;
            case R.id.menu_note_archive:
                isDirty = true;
                int iconRes = (open) ? R.drawable.ic_action_unarchive : R.drawable.ic_action_archive;
                open = !open;
                mMenu.findItem(R.id.menu_note_archive).setIcon(iconRes);
                break;
            case R.id.menu_note_delete:
                int note_id = note_cr
                        .getInt(OColumn.ROW_ID);
                OValues values = new OValues();
                values.put("trashed", (trashed == 1) ? 0 : 1);
                values.put("is_dirty", false);
//                values.put("trashed_date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
                mNote.update(note_id, values);
                String toast = (trashed == 1) ? getString(R.string.note_restore)
                        : getString(R.string.move_to_trash);
                Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
                finish();
                break;
            case R.id.menu_note_make_copy:
                Bundle extras = new Bundle();
                extras.putString(Intent.EXTRA_SUBJECT, /* name.getText().toString() */
                        "");
                extras.putString(Intent.EXTRA_TEXT, memo.getText().toString());
                extras.putInt(Notes.KEY_STAGE_ID, mStageId);
                Intent intent = new Intent(this, NoteDetail.class);
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
}
