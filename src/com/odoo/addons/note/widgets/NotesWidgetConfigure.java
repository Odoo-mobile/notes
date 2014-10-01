package com.odoo.addons.note.widgets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import odoo.controls.OControlHelper;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.notes.R;
import com.odoo.support.OUser;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.util.OControls;

public class NotesWidgetConfigure extends ListActivity implements
		LoaderCallbacks<Cursor> {

	private static final String PREFS_NAME = "com.odoo.widgetsWidgetProvider";

	List<String> mOptionsList = new ArrayList<String>();
	private static Context mContext = null;
	private OCursorListAdapter mAdapter = null;
	ListView mListView = null;
	private NoteStage note_stage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.widget_note_configure_layout);
		setTitle("Widget Configure");
		setResult(RESULT_CANCELED);
		if (OUser.current(this) == null) {
			Toast.makeText(this, "No account found", Toast.LENGTH_LONG).show();
			finish();
		}
		note_stage = new NoteNote.NoteStage(this);
		mListView = getListView();
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mAdapter = new OCursorListAdapter(this, null,
				android.R.layout.simple_list_item_multiple_choice) {
			@Override
			public void bindView(View view, Context context, Cursor cursor) {
				TextView txv = (TextView) view.findViewById(android.R.id.text1);
				int padd = (int) getResources().getDimension(
						R.dimen.odoo_padding);
				txv.setPadding(padd, padd, padd, padd);
				txv.setTextAppearance(mContext,
						android.R.attr.textAppearanceMedium);
				txv.setTypeface(OControlHelper.lightFont());
				OControls.setText(view, android.R.id.text1,
						cursor.getString(cursor.getColumnIndex("name")));
			}
		};
		mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);
		getLoaderManager().initLoader(0, null, (LoaderCallbacks<Cursor>) this);
	}

	static void savePref(Context context, int appWidgetId, String key,
			Set<String> value) {
		mContext = context;
		SharedPreferences.Editor prefs = context.getSharedPreferences(
				PREFS_NAME, 0).edit();
		prefs.putStringSet(key + "_" + appWidgetId, value);
		prefs.commit();
	}

	public static List<Integer> getPref(Context context, int appWidgetId,
			String key) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		Set<String> selectedIdSet = prefs.getStringSet(key + "_" + appWidgetId,
				null);
		List<Integer> selectedIdList = new ArrayList<Integer>();
		for (String id : selectedIdSet)
			selectedIdList.add(Integer.parseInt(id));

		return selectedIdList;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		SparseBooleanArray checkedItems = mListView.getCheckedItemPositions();
		Set<String> selected_ids = new HashSet<String>();
		List<String> selectedIds = new ArrayList<String>();
		Cursor c = mAdapter.getCursor();
		for (int i = 0; i < checkedItems.size(); i++) {
			int position = checkedItems.keyAt(i);
			c.moveToPosition(position);
			selectedIds.add(c.getString(c.getColumnIndex("_id")));
		}
		selected_ids.addAll(selectedIds);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		int mAppWidgetId = 0;
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		savePref(this, mAppWidgetId, "note_filter", selected_ids);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		NotesWidget.updateNoteWidget(this, appWidgetManager,
				new int[] { mAppWidgetId });
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_widget_configure, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, note_stage.uri(), new String[] { "name",
				"id" }, null, null, "sequence");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		mAdapter.changeCursor(arg1);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

}