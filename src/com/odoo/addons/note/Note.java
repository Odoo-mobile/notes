package com.odoo.addons.note;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.undobar.UndoBar;
import odoo.controls.undobar.UndoBar.UndoBarListener;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.widgets.SwipeRefreshLayout.OnRefreshListener;

import com.odoo.addons.note.dialogs.NoteColorDialog.OnColorSelectListener;
import com.odoo.addons.note.dialogs.NoteStagesDialog.OnStageSelectListener;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.note.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.util.OControls;
import com.odoo.util.StringUtils;
import com.odoo.util.controls.HeaderGridView;
import com.odoo.util.drawer.DrawerItem;

public class Note extends BaseFragment implements OnItemClickListener,
		LoaderCallbacks<Cursor>, OnRefreshListener, SyncStatusObserverListener,
		OnViewBindListener, UndoBarListener {

	public static final String KEY_STAGE_ID = "stage_id";
	public static final String KEY_NOTE_ID = "note_id";
	public static final String KEY_NOTE_FILTER = "note_filter";
	public static final String TAG = Note.class.getSimpleName();
	public static final int REQUEST_SPEECH_TO_TEXT = 333;
	private View mView = null;
	private Keys mCurrentKey = Keys.Note;
	private Integer mStageId = 0;
	private ODataRow stage;
	private HeaderGridView mListControl = null;
	private OCursorListAdapter mAdapter = null;
	Context mContext = null;

	public enum Keys {
		Note, Archive, Reminders, Trash
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		mContext = getActivity();
		scope = new AppScope(mContext);
		checkArguments();
		mView = inflater.inflate(R.layout.note, container, false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasSwipeRefreshView(view, R.id.swipe_container, this);
		setHasSyncStatusObserver(TAG, this, db());
		mListControl = (HeaderGridView) view.findViewById(R.id.listRecords);
		// Adding header view
		switch (mCurrentKey) {
		case Note:
		case Reminders:
			View header = getActivity().getLayoutInflater().inflate(
					R.layout.note_quick_controls, null, false);
			mListControl.addHeaderView(header, null, true);
			break;
		}
		mAdapter = new OCursorListAdapter(mContext, null,
				R.layout.note_custom_view_note);
		mAdapter.setOnViewBindListener(this);
		mListControl.setAdapter(mAdapter);
		mListControl.setOnItemClickListener(this);
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
		menu.add(new DrawerItem(TAG, "Trash", count(context, Keys.Trash),
				R.drawable.ic_action_trash, object(Keys.Trash)));
		return menu;
	}

	private void checkArguments() {
		Bundle arg = getArguments();
		mCurrentKey = Keys.valueOf(arg.getString(KEY_NOTE_FILTER));
		mStageId = arg.getInt(KEY_STAGE_ID);
		stage = new NoteStage(getActivity()).select(mStageId);
	}

	private int count(Context context, Keys key) {
		int count = 0;
		NoteNote db = new NoteNote(context);
		switch (key) {
		case Note:
			count = db.count("open = ? AND trashed = ?", new Object[] { true,
					"0" });
			break;
		case Archive:
			count = db.count("open = ? and trashed = ?", new Object[] { false,
					"0" });
			break;
		case Reminders:
			count = db.count("reminder != ?", new Object[] { "" });
			break;
		case Trash:
			count = db.count("trashed = ?", new Object[] { "1" });
			break;
		default:
			break;
		}
		return count;
	}

	private Fragment object(Keys value) {
		Fragment f = (value == Keys.Note) ? new NotesPager() : new Note();
		Bundle args = new Bundle();
		args.putString(KEY_NOTE_FILTER, value.toString());
		f.setArguments(args);
		return f;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.menu_note, menu);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		Cursor cr = mAdapter.getCursor();
		cr.moveToPosition(position - mListControl.getNumColumns());
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_NOTE_ID, cr.getInt(cr.getColumnIndex(OColumn.ROW_ID)));
		Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		String selection = null;
		String[] arguments = null;
		List<String> args = new ArrayList<String>();
		switch (mCurrentKey) {
		case Note:
			selection = "stage_id = ? and open = ? and trashed = ?";
			args.add(mStageId + "");
			args.add("true");
			args.add("0");
			if (stage.getInt("sequence") == 0) {
				selection += " or stage_id = ?";
				args.add("0");
			}
			arguments = args.toArray(new String[args.size()]);
			break;
		case Archive:
			selection = "open = ? and trashed = ?";
			args.add("false");
			args.add("0");
			arguments = args.toArray(new String[args.size()]);
			break;
		case Trash:
			selection = " trashed = ?";
			args.add("1");
			arguments = args.toArray(new String[args.size()]);
			break;
		}
		return new CursorLoader(mContext, db().uri(), new String[] { "name",
				"short_memo", "color", "open", "trashed" }, selection,
				arguments, "sequence");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		mAdapter.changeCursor(cursor);
		if (db().isEmptyTable()) {
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
		bindRowControls(view, row);
	}

	private void bindRowControls(final View view, final ODataRow row) {
		final int row_id = row.getInt(OColumn.ROW_ID);
		/*
		 * Updating note color
		 */
		view.findViewById(R.id.note_bg_color_selector).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						int color = row.getInt("color");
						String selected_color = NoteUtil.getBackgroundColors()[color];
						NoteUtil.colorDialog(getActivity(), selected_color,
								new OnColorSelectListener() {

									@Override
									public void colorSelected(
											ODataRow color_data) {
										int index = color_data.getInt("index");
										OValues values = new OValues();
										values.put("color", index);
										values.put("is_dirty", true);
										db().resolver().update(row_id, values);
										restartLoader();
									}
								}).show();
					}
				});
		/*
		 * Moving note to stage
		 */
		view.findViewById(R.id.note_move).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						NoteUtil.noteStages(getActivity(),
								new OnStageSelectListener() {

									@Override
									public void stageSelected(ODataRow row) {
										int stage_id = row
												.getInt(OColumn.ROW_ID);
										OValues values = new OValues();
										values.put("stage_id", stage_id);
										values.put("is_dirty", true);
										db().resolver().update(row_id, values);
										restartLoader();
									}
								}).show();
					}
				});
		view.findViewById(R.id.note_archive).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						String open = (row.getString("open").equals("false")) ? "true"
								: "false";
						showArchiveUndoBar(row_id, open);
					}
				});
		/**
		 * Trashed
		 */
		view.findViewById(R.id.note_delete).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {
						// Undobar
						int trashed = (row.getInt("trashed") == 1) ? 0 : 1;
						showTrashUndoBar(row_id, trashed);
					}
				});
	}

	private void showTrashUndoBar(int note_id, int trashed) {
		UndoBar undoBar = new UndoBar(getActivity());
		if (mCurrentKey == Keys.Trash) {
			undoBar.setMessage("Note restored");
		} else {
			undoBar.setMessage("Note moved to trash");
		}
		undoBar.setDuration(7000);
		undoBar.setListener(this);
		undoBar.show(true);
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_NOTE_ID, note_id);
		bundle.putInt("trashed", trashed);
		Parcelable undoToken = bundle;
		undoBar.setUndoToken(undoToken);
		toggleTrashNote(note_id, trashed);
	}

	private void showArchiveUndoBar(int note_id, String open) {
		UndoBar undoBar = new UndoBar(getActivity());
		if (mCurrentKey == Keys.Archive) {
			undoBar.setMessage("Note unarchived");
		} else {
			undoBar.setMessage("Note archived");
		}
		undoBar.setDuration(7000);
		undoBar.setListener(this);
		undoBar.show(true);
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_NOTE_ID, note_id);
		bundle.putString("open", open);
		Parcelable undoToken = bundle;
		undoBar.setUndoToken(undoToken);
		toggleArchiveNote(note_id, open);
	}

	private void toggleArchiveNote(int row_id, String open) {
		OValues values = new OValues();
		values.put("open", open);
		values.put("is_dirty", true);
		db().resolver().update(row_id, values);
		restartLoader();
	}

	private void toggleTrashNote(int row_id, int trashed) {
		OValues values = new OValues();
		values.put("trashed", trashed);
		values.put("is_dirty", false);
		db().resolver().update(row_id, values);
		restartLoader();
	}

	private void restartLoader() {
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onHide() {
		// Nothing to do
	}

	@Override
	public void onUndo(Parcelable token) {
		Bundle bundle = (Bundle) token;
		int note_id = bundle.getInt(KEY_NOTE_ID);
		if (bundle.containsKey("trashed")) {
			int trashed = (bundle.getInt("trashed") == 1) ? 0 : 1;
			toggleTrashNote(note_id, trashed);
		} else {
			String open = (bundle.getString("open").equals("false")) ? "true"
					: "false";
			toggleArchiveNote(note_id, open);
		}
	}

}
