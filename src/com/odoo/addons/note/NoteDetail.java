package com.odoo.addons.note;

import java.util.List;

import odoo.controls.OForm;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.odoo.addons.note.Note.Keys;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.base.ir.Attachments;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.controls.ExpandableHeightGridView;
import com.odoo.util.drawer.DrawerItem;

public class NoteDetail extends BaseFragment {
	private View mView = null;
	private Keys mKey = null;
	private Integer mId = null;
	private OForm mForm = null;
	private Boolean mEditMode = true;
	private ODataRow mRecord = null;
	private Menu mMenu = null;
	String str = null;
	Attachments mAttachment = null;
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
		mAttachment = new Attachments(mContext);
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
				mRecord = notes.select(mId);
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

	@SuppressWarnings("static-access")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_GET_CONTENT);
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
			// mAttachment.requestAttachment(Types.AUDIO);
			intent.setType("audio/*");
			// startActivityForResult(intent, mAttachment.REQUEST_AUDIO);
			break;
		case R.id.menu_note_image:
			// mAttachment.requestAttachment(Types.IMAGE_OR_CAPTURE_IMAGE);
			intent.setType("image/*");
			// startActivityForResult(intent, mAttachment.REQUEST_IMAGE);
			break;
		case R.id.menu_note_file:
			// mAttachment.requestAttachment(Types.FILE);
			intent.setType("application/file");
			// startActivityForResult(intent, mAttachment.REQUEST_FILE);
			break;
		case R.id.menu_note_detail_save:
			mEditMode = false;
			OValues values = mForm.getFormValues();
			if (values != null) {
				updateMenu(mEditMode);
				if (mId != null) {
					switch (mKey) {
					case Note:
						new NoteNote(getActivity()).update(values, mId);
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
		// if (resultCode == Activity.RESULT_OK) {
		// ODataRow newAttachment = mAttachment
		// .handleResult(requestCode, data);
		// if (newAttachment.getString("content").equals("false")) {
		// // mNoteAttachmentList.add(newAttachment);
		// // mNoteListAdapterAttach
		// // .notifiyDataChange(mNoteAttachmentList);
		// }
		// Log.e("Result Detail ", "new  :" + newAttachment);
		// }
	}
}
