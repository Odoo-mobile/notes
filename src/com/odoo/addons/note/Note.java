package com.odoo.addons.note;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.OList;
import odoo.controls.OList.OnListRowViewClickListener;
import odoo.controls.OList.OnRowClickListener;
import odoo.controls.OListDragDropListener;
import odoo.controls.OViewPager;
import odoo.controls.OViewPager.OnPaggerGetView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.base.ir.Attachment;
import com.odoo.base.ir.Attachment.Types;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.AppScope;
import com.odoo.support.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.openerp.OETouchListener;
import com.openerp.OETouchListener.OnPullListener;

public class Note extends BaseFragment implements OnPullListener,
		OnRowClickListener, OnClickListener, OnPaggerGetView,
		OnListRowViewClickListener, OListDragDropListener {

	public static final String TAG = Note.class.getSimpleName();
	public static final int REQUEST_SPEECH_TO_TEXT = 333;

	enum Keys {
		Note, Archive, Reminders
	}

	View mView = null;
	Keys mCurrentKey = Keys.Note;
	OETouchListener mTouchListener = null;
	DataLoader mDataLoader = null;
	List<ODataRow> mListRecords = new ArrayList<ODataRow>();
	EditText edtTitle = null;
	ImageView mImgBtnShowQuickNote = null;
	private Menu mMenu = null;
	PackageManager mPackageManager = null;
	Context mContext = null;
	Attachment mAttachment = null;
	private OViewPager mViewPagger = null;
	OList oListStage = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mContext = getActivity();
		scope = new AppScope(this);
		mView = inflater.inflate(R.layout.note_layout, container, false);
		mAttachment = new Attachment(mContext);
		mViewPagger = (OViewPager) mView.findViewById(R.id.viewPagger);
		mViewPagger.setOnPaggerGetView(this);
		oListStage = (OList) mView.findViewById(R.id.listStageRecords);
		initControl();
		return mView;
	}

	void init(OList mListControl, Context context, int stage_id) {
		oListStage.setRowDroppable(true, mListControl);
		scope = new AppScope(context);
		checkArguments();
		mTouchListener = scope.main().getTouchAttacher();
		mListControl.setOnRowClickListener(this);
		mListControl.setOnListRowViewClickListener(R.id.imgOpen,
				(OnListRowViewClickListener) this);
		mListControl.setRowDraggable(true);
		mListControl.setDragDropListener(this);
		mTouchListener.setPullableView(mListControl, this);
		NoteStage noteStage = new NoteStage(mContext);
		oListStage.initListControl(noteStage.select(null, null, null, null,
				"sequence"));
		mDataLoader = new DataLoader(mListControl, context, stage_id);
		mDataLoader.execute();
	}

	void initControl() {
		mImgBtnShowQuickNote = (ImageView) mView
				.findViewById(R.id.imgShowQuickNote);
		mImgBtnShowQuickNote.setOnClickListener(this);
		mView.findViewById(R.id.imgCreateQuickNote).setOnClickListener(this);
		mView.findViewById(R.id.imgAttachImage).setOnClickListener(this);
		mView.findViewById(R.id.imgAttachAudio).setOnClickListener(this);
		mView.findViewById(R.id.imgAttachSpeechToText).setOnClickListener(this);
		edtTitle = (EditText) mView.findViewById(R.id.edtNoteQuickTitle);
		edtTitle.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (s.length() != 0) {
					mImgBtnShowQuickNote.setVisibility(View.VISIBLE);
				} else {
					mImgBtnShowQuickNote.setVisibility(View.GONE);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
	}

	class DataLoader extends AsyncTask<Void, Void, Void> {

		OList mListControl = null;
		Context mContext = null;
		int mStageId = 0;
		List<ODataRow> list = null;

		public DataLoader(OList listControl, Context context, int stage_id) {
			mListControl = listControl;
			mContext = context;
			scope = new AppScope(context);
			mStageId = stage_id;
		}

		@Override
		protected Void doInBackground(Void... params) {
			scope.main().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (db().isEmptyTable()) {
						scope.main().requestSync(NoteProvider.AUTHORITY);
					}
					mListRecords.clear();

					switch (mCurrentKey) {
					case Note:
						list = db().select(
								"stage_id = ? and open = ? and reminder = ?",
								new Object[] { mStageId, true, "" }, null,
								null, "sequence");
						mListRecords.addAll(list);
						break;
					case Archive:
						list = db().select("stage_id = ? and open = ?",
								new Object[] { mStageId, false }, null, null,
								"sequence");
						mListRecords.addAll(list);
						break;
					case Reminders:
						list = db().select("stage_id = ? and reminder != ?",
								new Object[] { mStageId, "" }, null, null,
								"sequence");
						mListRecords.addAll(list);
						break;
					}
				}
			});
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			updateMenu(mListRecords.size()); // Next Count
			mListControl.initListControl(mListRecords);
			OControls.setGone(mView, R.id.loadingProgress);
		}
	}

	@Override
	public Object databaseHelper(Context context) {

		return new NoteNote(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Notes", true));
		menu.add(new DrawerItem(TAG, "Note", count(context, Keys.Note), 0,
				object(Keys.Note)));
		menu.add(new DrawerItem(TAG, "Archive", count(context, Keys.Archive),
				0, object(Keys.Archive)));
		menu.add(new DrawerItem(TAG, "Reminders",
				count(context, Keys.Reminders), 0, object(Keys.Reminders)));
		return menu;
	}

	private void checkArguments() {
		Bundle arg = getArguments();
		mCurrentKey = Keys.valueOf(arg.getString("note"));
	}

	private int count(Context context, Keys key) {
		int count = 0;
		switch (key) {
		case Note:
			count = new NoteNote(context).count();
			break;
		case Archive:
			count = new NoteNote(context).select("open = ?",
					new Object[] { false }).size();
			break;
		case Reminders:
			count = new NoteNote(context).select("reminder != ?",
					new Object[] { "" }).size();
			break;
		default:
			break;
		}
		return count;
	}

	private Fragment object(Keys value) {
		Note note = new Note();
		Bundle args = new Bundle();
		args.putString("note", value.toString());
		note.setArguments(args);
		return note;
	}

	@Override
	public void onPullStarted(View arg0) {
		scope.main().requestSync(NoteProvider.AUTHORITY);
	}

	@Override
	public void onResume() {
		super.onResume();
		scope.main().registerReceiver(mSyncFinishReceiver,
				new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
	}

	@Override
	public void onPause() {
		super.onPause();
		scope.main().unregisterReceiver(mSyncFinishReceiver);
	}

	private SyncFinishReceiver mSyncFinishReceiver = new SyncFinishReceiver() {
		@Override
		public void onReceive(Context context, android.content.Intent intent) {
			scope.main().refreshDrawer(TAG);
			mTouchListener.setPullComplete();
		}
	};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_note, menu);
		mMenu = menu;
	}

	private void updateMenu(int noteCount) {
		if (noteCount != 0)
			mMenu.findItem(R.id.menu_note_count).setTitle(noteCount + "");
		else
			mMenu.findItem(R.id.menu_note_count).setTitle("");
	}

	@Override
	public void onRowItemClick(int position, View view, final ODataRow row) {
		NoteDetail note = new NoteDetail();
		Bundle bundle = new Bundle();
		bundle.putString("key", mCurrentKey.toString());
		bundle.putAll(row.getPrimaryBundleData());
		note.setArguments(bundle);
		startFragment(note, true);
	}

	@Override
	public void onRowViewClick(ViewGroup view_group, View view, int position,
			ODataRow row) {
		OValues values = new OValues();
		values.put("open", !row.getBoolean("open"));
		new NoteNote(getActivity()).update(values, "local_id=?",
				new Object[] { row.getInt("local_id") });
		// id 0 or More
	}

	@Override
	public void onClick(View v) {
		NoteDetail note = new NoteDetail();
		Bundle bundle = new Bundle();
		bundle.putString("key", mCurrentKey.toString());
		switch (v.getId()) {
		case R.id.imgShowQuickNote:
			OValues oValues = new OValues();
			oValues.put("name", edtTitle.getText().toString());
			oValues.put("memo", edtTitle.getText().toString());
			oValues.put("stage_id", 1); // Static id how get get current stage
										// id
			edtTitle.setText("");
			new NoteNote(getActivity()).create(oValues);
			break;
		case R.id.imgCreateQuickNote:
			note.setArguments(bundle);
			startFragment(note, true);
			break;
		case R.id.imgAttachImage:
			mAttachment.requestAttachment(Types.CAPTURE_IMAGE);
			break;
		case R.id.imgAttachAudio:
			mAttachment.requestAttachment(Types.AUDIO);
			break;
		case R.id.imgAttachSpeechToText:
			requestSpeechToText();
			break;
		}
	}

	private void requestSpeechToText() {
		mPackageManager = mContext.getPackageManager();
		List<ResolveInfo> activities = mPackageManager.queryIntentActivities(
				new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			Toast.makeText(mContext, "No audio recorder present.",
					Toast.LENGTH_LONG).show();
		} else {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
					RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "speak now...");
			startActivityForResult(intent, REQUEST_SPEECH_TO_TEXT);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.e("Result", resultCode + "");
		if (resultCode == Activity.RESULT_OK) {
			Log.e("OnActivity", "ResultOK");
			if (requestCode == REQUEST_SPEECH_TO_TEXT) {
				ArrayList<String> matches = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				Log.e("Speech TO test", matches.get(0) + "");
			} else {
				ODataRow newAttachment = mAttachment.handleResult(requestCode,
						data);
				if (newAttachment.getString("content").equals("false")) {
					// mNoteAttachmentList.add(newAttachment);
					// mNoteListAdapterAttach.notifiyDataChange(mNoteAttachmentList);
					Log.e("Attachment", newAttachment + "");
				}
			}
		}
	}

	@Override
	public View paggerGetView(Context context, View view, ODataRow object,
			int position) {
		OList mList = (OList) LayoutInflater.from(context).inflate(
				R.layout.note_list_layout, null);
		init(mList, context, object.getInt("id"));
		return mList;
	}

	@Override
	public FragmentManager getPaggerFragmentManager() {
		return getActivity().getSupportFragmentManager();
	}

	@Override
	public void onItemDragStart(View drag_view, int position, Object data) {
		oListStage.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemDrop(View drop_view, Object drag_view_data,
			Object drop_view_data) {

	}

	@Override
	public void onItemDragEnd(View drop_view, int position, Object data) {
		oListStage.setVisibility(View.GONE);
	}
}
