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
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.util.OControls;
import com.odoo.util.StringUtils;
import com.odoo.util.controls.HeaderGridView;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.util.logger.OLog;

public class Note extends BaseFragment implements OnItemClickListener,
		LoaderCallbacks<Cursor>, OnRefreshListener, SyncStatusObserverListener,
		OnViewBindListener {

	public static final String TAG = Note.class.getSimpleName();
	public static final int REQUEST_SPEECH_TO_TEXT = 333;
	private View mView = null;
	private Menu mMenu = null;
	private Keys mCurrentKey = Keys.Note;
	private HeaderGridView mListControl = null;
	private OCursorListAdapter mAdapter = null;
	Context mContext = null;

	public enum Keys {
		Note, Archive, Reminders
	}

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
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		setHasSyncStatusObserver(TAG, this, db());
		mListControl = (HeaderGridView) view.findViewById(R.id.listRecords);
		// Adding header view
		View header = getActivity().getLayoutInflater().inflate(
				R.layout.note_quick_controls, null);
		mListControl.addHeaderView(header, null, true);
		mAdapter = new OCursorListAdapter(mContext, null,
				R.layout.note_custom_view_note);
		mAdapter.setOnViewBindListener(this);
		mListControl.setAdapter(mAdapter);
		mListControl.setOnItemClickListener(this);
		// mListControl.setEmptyView(mView.findViewById(R.id.loadingProgress));
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new NoteNote(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(TAG, "Notes", count(context, Keys.Note),
				R.drawable.ic_action_notes, object(Keys.Note)));
		menu.add(new DrawerItem(TAG, "Reminders",
				count(context, Keys.Reminders), R.drawable.ic_action_reminder,
				object(Keys.Reminders)));
		menu.add(new DrawerItem(TAG, "Archive", count(context, Keys.Archive),
				R.drawable.ic_action_archive, object(Keys.Archive)));
		menu.add(new DrawerItem(TAG, "Trash", count(context, Keys.Note),
				R.drawable.ic_action_trash, object(Keys.Note)));
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
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		Cursor cr = mAdapter.getCursor();
		cr.moveToPosition(position - 2); // -2 because of header
		OLog.log("open clicked : " + cr.getPosition() + " : "
				+ cr.getString(cr.getColumnIndex("name")));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(mContext, db().uri(), new String[] { "name",
				"short_memo", "color" }, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		if (cursor.getCount() == 0) {
			setSwipeRefreshing(true);
			scope.main().requestSync(NoteProvider.AUTHORITY);
		}
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

	@Override
	public void onViewBind(View view, Cursor cursor, ODataRow row) {
		int color_number = row.getInt("color");
		view.findViewById(R.id.note_bg_color).setBackgroundColor(
				NoteUtil.getBackgroundColor(color_number));
		OControls.setTextViewsColor(view, new int[] { R.id.note_name,
				R.id.note_memo }, NoteUtil.getTextColor(color_number));
		OControls.setText(view, R.id.note_name, row.getString("name"));
		OControls.setText(view, R.id.note_memo,
				StringUtils.htmlToString(row.getString("short_memo")));
	}

}
