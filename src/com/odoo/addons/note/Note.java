package com.odoo.addons.note;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.note.R;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnRowViewClickListener;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;

public class Note extends BaseFragment implements OnItemClickListener,
		OnRowViewClickListener, LoaderCallbacks<Cursor>, OnRefreshListener,
		SyncStatusObserverListener {

	public static final String TAG = Note.class.getSimpleName();
	private View mView = null;
	private Menu mMenu = null;
	private Keys mCurrentKey = Keys.Note;
	private ListView mListControl = null;
	private OCursorListAdapter mAdapter = null;
	private ListView mListStage = null;

	// public static final int REQUEST_SPEECH_TO_TEXT = 333;
	enum Keys {
		Note, Archive, Reminders
	}

	//

	// List<ODataRow> mListRecords = new ArrayList<ODataRow>();
	// EditText edtTitle = null;
	// ImageView mImgBtnShowQuickNote = null;
	// PackageManager mPackageManager = null;
	Context mContext = null;

	// Attachments mAttachment = null;
	// private OViewPager mViewPagger = null;
	// ListView oListStage = null;
	// ListView mListControl = null;
	// Integer mLastPosition = -1;
	// Integer mLimit = 3;
	// private SwipeRefreshLayout mSwipeRefresh = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mContext = getActivity();
		scope = new AppScope(mContext);

		mView = inflater.inflate(R.layout.note_layout, container, false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// setHasSwipeRefreshView(view, R.id.swipe_container, this);
		setHasSyncStatusObserver(TAG, this, db());
		mListControl = (ListView) view.findViewById(R.id.listRecords);
		mAdapter = new OCursorListAdapter(mContext, null,
				R.layout.note_custom_view_note);
		// mAdapter.setOnViewCreateListener(this);
		mListControl.setAdapter(mAdapter);
		mListControl.setOnItemClickListener(this);
		mListControl.setEmptyView(mView.findViewById(R.id.loadingProgress));
		mAdapter.setOnRowViewClickListener(R.id.imgOpen, this);
		getLoaderManager().initLoader(0, null, this);
	}

	// void init(ListView ListControl, Context context, int stage_id) {
	// mListControl = ListControl;
	// scope = new AppScope(context);
	// checkArguments();
	//
	// }
	//
	// void initControl() {
	// mImgBtnShowQuickNote = (ImageView) mView
	// .findViewById(R.id.imgShowQuickNote);
	// mImgBtnShowQuickNote.setOnClickListener(this);
	// mView.findViewById(R.id.imgCreateQuickNote).setOnClickListener(this);
	// mView.findViewById(R.id.imgAttachImage).setOnClickListener(this);
	// mView.findViewById(R.id.imgAttachAudio).setOnClickListener(this);
	// mView.findViewById(R.id.imgAttachSpeechToText).setOnClickListener(this);
	// edtTitle = (EditText) mView.findViewById(R.id.edtNoteQuickTitle);
	// edtTitle.addTextChangedListener(new TextWatcher() {
	// @Override
	// public void onTextChanged(CharSequence s, int start, int before,
	// int count) {
	// if (s.length() != 0) {
	// mImgBtnShowQuickNote.setVisibility(View.VISIBLE);
	// } else {
	// mImgBtnShowQuickNote.setVisibility(View.GONE);
	// }
	// }
	//
	// @Override
	// public void beforeTextChanged(CharSequence s, int start, int count,
	// int after) {
	// }
	//
	// @Override
	// public void afterTextChanged(Editable s) {
	// }
	// });
	// }

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
			count = new NoteNote(context).select("open = ? AND reminder = ?",
					new Object[] { true, "" }).size();
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.menu_note, menu);
		mMenu = menu;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		OLog.log("open clicked" + arg2);
	}

	@Override
	public void onRowViewClick(int position, Cursor cursor, View view,
			View parent) {
		OLog.log("Item clicked" + position);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(mContext, db().uri(), db().projection(), null,
				null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		OControls.setGone(mView, R.id.loadingProgress);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onRefresh() {
		if (app().inNetwork()) {
			scope.main().requestSync(NoteProvider.AUTHORITY);
		} else {
			hideRefreshingProgress();
			Toast.makeText(getActivity(), _s(R.string.no_connection),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing)
			hideRefreshingProgress();
		else
			setSwipeRefreshing(true);
	}

	// @Override
	// public View paggerGetView(Context context, View view, ODataRow object,
	// int position) {
	// ListView mList = (ListView) LayoutInflater.from(context).inflate(
	// R.layout.note_list_layout, null);
	// mListControl = mList;
	// // init(mList, context, object.getInt("id"));
	// return mList;
	// }
	//
	// @Override
	// public FragmentManager getPaggerFragmentManager() {
	// return null;
	// }
}
