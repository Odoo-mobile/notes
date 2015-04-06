package com.odoo.addons.notes;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.notes.dialogs.NoteColorDialog;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteStage;
import com.odoo.addons.notes.reminder.NoteReminder;
import com.odoo.addons.notes.utils.NoteUtil;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.ir.feature.OFileManager;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OActionBarUtils;
import com.odoo.core.utils.OAlert;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.ODateUtils;
import com.odoo.core.utils.OResource;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OForm;
import odoo.controls.misc.ONoteAttachmentView;
import odoo.controls.misc.OTagsFlowView;

public class NoteDetail extends ActionBarActivity implements ONoteAttachmentView.AttachmentViewListener, View.OnClickListener {
    public static final String ACTION_REMINDER_CALL = "com.odoo.addons.note.NoteDetailActivity.REMINDER_CALL";
    public static final String REQUEST_FILE_ATTACHMENT = "request_file_attachment";
    public static final int REQUEST_SPEECH_TO_TEXT = 333;
    public static final int REQUEST_MANAGE_TAGS = 444;
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
    private String note_memo;
    private EditText memo;
    private ActionBar actionBar;
    private OFileManager fileManager;
    private Bundle extra;
    private List<OValues> attachments = new ArrayList<>();
    private List<ODataRow> attachmentLists = new ArrayList<>();
    private int dbAttachments = 0;
    private OForm mNoteForm;
    private ONoteAttachmentView attachmentView;
    private IrAttachment irAttachment;
    private OTagsFlowView tagsFlowView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_detail_view);
        OActionBarUtils.setActionBar(this, false);
        actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(
                new ColorDrawable(Color.parseColor("#22000000")));
        fileManager = new OFileManager(this);
        irAttachment = new IrAttachment(this, null);
        mNoteForm = (OForm) findViewById(R.id.noteForm);
        tagsFlowView = (OTagsFlowView) findViewById(R.id.noteTags);
        init();
    }

    private void init() {
        mNote = new NoteNote(this, null);
        mStage = new NoteStage(this, null);
        extra = getIntent().getExtras();
        Integer note_id = (extra.containsKey(Notes.KEY_NOTE_ID)) ? extra
                .getInt(Notes.KEY_NOTE_ID) : null;
        if (OUser.current(this) == null) {
            Toast.makeText(this, "No active account found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String action = getIntent().getAction();
        if (action != null && (action.equals(Intent.ACTION_SEND) ||
                action.equals(Intent.ACTION_SEND_MULTIPLE))) {
            if (action.equals(Intent.ACTION_SEND)) {
                OValues singleAttachment = fileManager.handleResult(OFileManager.SINGLE_ATTACHMENT_STREAM,
                        RESULT_OK, getIntent());
                attachments.add(singleAttachment);
            } else {
                List<OValues> attachmentList = fileManager.handleMultipleRequest(getIntent());
                attachments.addAll(attachmentList);
            }
        }
        initData(note_id, extra);

        if (extra.containsKey(REQUEST_FILE_ATTACHMENT)) {
            fileManager.requestForFile(OFileManager.RequestType.IMAGE_OR_CAPTURE_IMAGE);
        }

        initAttachmentView();
        initReminderControls();
    }

    private void initAttachmentView() {
        attachmentLists.clear();
        attachmentView = (ONoteAttachmentView) findViewById(R.id.note_attachments);
        attachmentView.setMaximumCols(3);
        attachmentView.setAttachmentViewListener(this);
        attachmentView.removeAllViews();
        attachmentView.setVisibility(View.GONE);
        for (OValues value : attachments) {
            attachmentLists.add(value.toDataRow());
        }
        if (note_cr != null) {
            List<ODataRow> cr = mNote.getAttachments(note_cr.getInt(OColumn.ROW_ID));
            dbAttachments = cr.size();
            attachmentLists.addAll(cr);
        }
        if (attachmentLists.size() > 0) {
            attachmentView.setVisibility(View.VISIBLE);
            attachmentView.createView(attachmentLists);
        }
    }

    @Override
    public View getView(final ODataRow attachment, final int position, ViewGroup parent) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.note_detail_attachment_item, parent, false);
        view.setTag(attachment);
        OControls.setText(view, R.id.file_name, attachment.getString("name"));
        String type = attachment.getString("file_type");
        String file_uri = attachment.getString("file_uri");
        ImageView img = (ImageView) view.findViewById(R.id.attachment_image);
        if (type.contains("image")) {
            if (!file_uri.equals("false")) {
                img.setImageURI(Uri.parse(file_uri));
            } else {
                img.setImageResource(R.drawable.image);
            }
        } else {
            img.setImageResource(R.drawable.file);
        }
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (attachment.contains(OColumn.ROW_ID))
                    fileManager.downloadAttachment(attachment.getInt(OColumn.ROW_ID));
                else
                    fileManager.downloadAttachment(attachment);
            }
        });
        view.findViewById(R.id.remove_attachment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OAlert.showConfirm(NoteDetail.this,
                        OResource.string(NoteDetail.this, R.string.toast_delete_attachment),
                        new OAlert.OnAlertConfirmListener() {
                            @Override
                            public void onConfirmChoiceSelect(OAlert.ConfirmType type) {
                                switch (type) {
                                    case POSITIVE:
                                        if (attachment.contains(OColumn.ROW_ID))
                                            irAttachment.delete(attachment.getInt(OColumn.ROW_ID));
                                        else
                                            attachmentLists.remove(position);
                                        initAttachmentView();
                                        break;
                                    case NEGATIVE:
                                        break;
                                }
                            }
                        });
            }
        });
        return view;
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

            mNoteForm.initForm(note_cr);
            mStageId = note_cr.getInt("stage_id");
            open = note_cr.getString("open").equals("true");
            trashed = note_cr.getInt("trashed");
            color = note_cr.getInt("color");
            initControls(color);

            String reminder = note_cr.getString("reminder");
            if (!reminder.equals("0"))
                reminderDate = reminder;
            String edited_date = ODateUtils.convertToDefault(note_cr.getString("_write_date"),
                    ODateUtils.DEFAULT_FORMAT, "d MMM, hh:mm a");
            last_update_on.setText("Edited " + edited_date);
            createView();
            updateTagView();
        }
        if (extra.containsKey(Intent.EXTRA_TEXT)) {
            initControls(color);
            memo.setText(extra.getString(Intent.EXTRA_TEXT));
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
        if (extra.containsKey("type")) {
            requestSpeechToText();
        }
    }

    private void updateTagView() {
        List<String> tags = new ArrayList<>();
        for (ODataRow tag : note_cr.getM2MRecord("tag_ids").browseEach()) {
            tags.add(tag.getString("name"));
        }
        tagsFlowView.notifyTagsChange(this, tags);
        tagsFlowView.setOnClickListener(this);

    }

    private void initControls(int color) {
        int background_color = NoteUtil.getBackgroundColor(color);
        last_update_on = (TextView) findViewById(R.id.last_update_on);
        findViewById(R.id.note_detail_view)
                .setBackgroundColor(background_color);
        memo = (EditText) findViewById(R.id.note_memo);
        memo.setTextColor(NoteUtil.getTextColor(color));
        last_update_on.setTextColor(NoteUtil.getTextColor(color));
    }

    private void createView() {
        String content = note_cr.getString("memo");
        SpannableStringBuilder b = new SpannableStringBuilder(
                Html.fromHtml(content));
        memo.setText(b);
    }

    private void requestSpeechToText() {
        PackageManager mPackageManager = this.getPackageManager();
        List<ResolveInfo> activities = mPackageManager.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            Toast.makeText(this, "No audio recorder present.",
                    Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "speak now...");
            intent.putExtra("stage_id", mStageId);
            startActivityForResult(intent, REQUEST_SPEECH_TO_TEXT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SPEECH_TO_TEXT) {
                ArrayList<String> matches = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (!TextUtils.isEmpty(memo.getText()))
                    memo.setText(memo.getText() + " " + matches.get(0));
                else
                    memo.setText(matches.get(0));
                memo.setSelection(memo.getText().length());
            } else if (requestCode == REQUEST_MANAGE_TAGS) {
                updateTagView();
            } else {
                OValues values = fileManager.handleResult(requestCode, resultCode, data);
                if (values != null && !values.contains("size_limit_exceed")) {
                    attachments.add(values);
                    initAttachmentView();
                } else if (values != null) {
                    Toast.makeText(this, R.string.toast_image_size_too_large, Toast.LENGTH_LONG).show();
                }
            }
        }
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
            if (dbAttachments != attachmentLists.size()) {
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
        dbAttachments = attachmentLists.size();
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
        if (extra.containsKey("tag_id")) {
            List<Integer> tag_ids = new ArrayList<>();
            tag_ids.add(extra.getInt("tag_id"));
            values.put("tag_ids", tag_ids);
        }
        String reminder = "0";
        if (mReminder.hasReminder()) {
            reminder = mReminder.getDateString();
        }
        values.put("reminder", reminder);
        int note_id;
        if (note_cr == null) {
            // creating new note
            values.put("sequence", 0);
            note_id = mNote.insert(values);
            if (mReminder.hasReminder()) {
                mReminder.setReminder(note_id, mReminder.getCal());
            }
        } else {
            // Updating note
            toast = getString(R.string.note_updated);
            note_id = note_cr.getInt(OColumn.ROW_ID);
            if (mReminder.hasReminder()) {
                if (reminderDate == null) {
                    reminderDate = "";
                }
                if (!reminderDate.equals(mReminder.getDateString())) {
                    mReminder.setReminder(note_id, mReminder.getCal());
                }
            }
            if (!mReminder.hasReminder() && reminderDate != null) {
                values.put("reminder", reminderDate);
            }
            mNote.update(note_id, values);
        }
        if (!attachments.isEmpty()) {
            IrAttachment irAttachment = new IrAttachment(this, null);
            for (OValues attachment : attachments) {
                irAttachment.createAttachment(attachment, mNote.getModelName(), note_id);
            }
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
        } else {
            if (note_cr.getM2MRecord("tag_ids").getRelIds().size() > 0) {
                mMenu.findItem(R.id.menu_manage_tags).setTitle("Manage tags");
            }
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

    private void manageTags() {
        Bundle extra = new Bundle();
        extra.putInt("note_id", note_cr.getInt(OColumn.ROW_ID));
        Intent intent = new Intent(this, ManageTags.class);
        intent.putExtras(extra);
        startActivityForResult(intent, REQUEST_MANAGE_TAGS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_manage_tags:
                manageTags();
                break;
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
            case R.id.menu_note_attachment:
                fileManager.requestForFile(OFileManager.RequestType.IMAGE_OR_CAPTURE_IMAGE);
                break;
            case R.id.menu_note_speech_to_text:
                requestSpeechToText();
                break;
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


    @Override
    public void onClick(View v) {
        if (note_cr != null)
            manageTags();
    }
}
