package com.odoo.addons.note;

import java.util.List;

import odoo.controls.OForm;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.addons.note.Note.Keys;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.base.ir.Attachment;
import com.odoo.base.ir.Attachment.Types;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.controls.ExpandableHeightGridView;
import com.odoo.util.drawer.DrawerItem;

public class NoteDetail extends BaseFragment {
	private View mView = null;
	private Keys mKey = null;
	private Integer mId = null;
	private Boolean mLocalRecord = false;
	private OForm mForm = null;
	private Boolean mEditMode = true;
	private ODataRow mRecord = null;
	private Menu mMenu = null;
	String str = null;
	Attachment mAttachment = null;
	PackageManager mPackageManager = null;
	Context mContext = null;
	public static final String TAG = NoteDetail.class.getSimpleName();
	ExpandableHeightGridView mNoteAttachmentGrid = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		initArgs();
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.note_detail, container, false);
		mContext = getActivity();
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mAttachment = new Attachment(mContext);
		init();
	}

	private void init() {
		updateMenu(mEditMode);
		switch (mKey) {
		case Note:
		case Archive:
		case Reminders:
			OControls.setVisible(mView, R.id.odooFormNote);
			mForm = (OForm) mView.findViewById(R.id.odooFormNote);
			NoteNote notes = new NoteNote(getActivity());
			if (mId != null) {
				mRecord = notes.select(mId, mLocalRecord);
				mForm.initForm(mRecord);
			} else {
				mForm.setModel(notes);
			}
			mForm.setEditable(mEditMode);
			break;
		}
		mNoteAttachmentGrid = (ExpandableHeightGridView) mView
				.findViewById(R.id.noteAttachmentGrid);
		mNoteAttachmentGrid.setExpanded(true);
	}

	private void initArgs() {
		Bundle args = getArguments();
		mKey = Note.Keys.valueOf(args.getString("key"));
		if (mKey == Keys.Archive || mKey == Keys.Reminders)
			mKey = Keys.Note;
		if (args.containsKey("id")) {
			mLocalRecord = args.getBoolean("local_record");
			if (mLocalRecord) {
				mId = args.getInt("local_id");
			} else
				mId = args.getInt("id");
		} else
			mEditMode = true;
	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

	private void updateMenu(boolean edit_mode) {
		mMenu.findItem(R.id.menu_note_detail_save).setVisible(edit_mode);
		mMenu.findItem(R.id.menu_note_detail_edit).setVisible(!edit_mode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_note_detail_edit:
			mEditMode = !mEditMode;
			updateMenu(mEditMode);
			mForm.setEditable(mEditMode);
			break;
		case R.id.menu_note_detail_delete:
			if (mId != null) {
				new NoteNote(getActivity()).delete(mId);
				getActivity().getSupportFragmentManager().popBackStack();
			}

			break;
		case R.id.menu_note_audio:
			mAttachment.requestAttachment(Types.AUDIO);
			break;
		case R.id.menu_note_image:
			mAttachment.requestAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
			break;
		case R.id.menu_note_file:
			mAttachment.requestAttachment(Types.FILE);
			break;
		case R.id.menu_note_detail_save:
			mEditMode = false;
			OValues values = mForm.getFormValues();
			if (values != null) {
				updateMenu(mEditMode);
				if (mId != null) {
					switch (mKey) {
					case Note:
						new NoteNote(getActivity()).update(values, mId,
								mLocalRecord);
						break;
					}
				} else {
					switch (mKey) {
					case Note:
						new NoteNote(getActivity()).create(values);
						break;
					}
				}
				getActivity().getSupportFragmentManager().popBackStack();
			}
			break;
		case R.id.menu_note_forward_asmail:
			break;
		case R.id.menu_note_followers:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_note_detail, menu);
		mMenu = menu;
		updateMenu(mEditMode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e("Result Detail", "" + resultCode);
		if (resultCode == Activity.RESULT_OK) {

			ODataRow newAttachment = mAttachment
					.handleResult(requestCode, data);
			if (newAttachment.getString("content").equals("false")) {
				// mNoteAttachmentList.add(newAttachment);
				// mNoteListAdapterAttach
				// .notifiyDataChange(mNoteAttachmentList);
			}
		}
	}
}
