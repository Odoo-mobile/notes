package com.odoo.addons.note.widgets;

import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.odoo.addons.note.Note;
import com.odoo.addons.note.NoteUtil;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.notes.R;
import com.odoo.orm.OColumn;
import com.odoo.support.OUser;
import com.odoo.widgets.WidgetHelper;

public class NotesRemoteViewFactory implements RemoteViewsFactory {

	public static final String TAG = "com.odoo.addons.note.widgets.NotesRemoteViewFactory";
	private Context mContext = null;
	private int mAppWidgetId = -1;
	Cursor mCursor = null;
	List<Integer> ids = null;
	int[] mFilter = null;

	public NotesRemoteViewFactory(Context context, Intent intent) {
		Log.d(TAG, "MessageRemoteViewFactory->constructor()");
		mContext = context;
		mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		ids = NotesWidgetConfigure.getPref(context, mAppWidgetId,
				Note.KEY_NOTE_FILTER);
	}

	@Override
	public int getCount() {
		return mCursor.getCount();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public RemoteViews getLoadingView() {
		RemoteViews mView = new RemoteViews(mContext.getPackageName(),
				R.layout.listview_data_loading_progress);
		return mView;
	}

	@Override
	public RemoteViews getViewAt(int position) {
		Log.d(TAG, "getViewAt()");
		RemoteViews mView = new RemoteViews(mContext.getPackageName(),
				R.layout.widget_note_item_layout);
		mCursor.moveToPosition(position);
		int color = mCursor.getInt(mCursor.getColumnIndex("color"));
		int background = NoteUtil.getBackgroundColor(color);
		int font_color = NoteUtil.getTextColor(color);

		mView.setTextColor(R.id.note_memo, font_color);
		mView.setInt(R.id.notesListViewItem, "setBackgroundColor", background);
		mView.setTextViewText(R.id.note_memo,
				mCursor.getString(mCursor.getColumnIndex("short_memo")));
		mView.setTextViewText(R.id.stage,
				mCursor.getString(mCursor.getColumnIndex("stage_id_name")));
		mView.setTextColor(R.id.stage, font_color);
		final Intent fillInIntent = new Intent();
		fillInIntent.setAction(NotesWidget.ACTION_NOTES_WIDGET_CALL);
		final Bundle bundle = new Bundle();
		bundle.putInt(WidgetHelper.EXTRA_WIDGET_DATA_VALUE,
				mCursor.getInt(mCursor.getColumnIndex(OColumn.ROW_ID)));
		fillInIntent.putExtras(bundle);
		mView.setOnClickFillInIntent(R.id.notesListViewItem, fillInIntent);
		return mView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void onCreate() {
		if (OUser.current(mContext) == null)
			return;
		NoteNote note = new NoteNote(mContext);
		String selection = "stage_id in (" + TextUtils.join(", ", ids) + ") and (open = 'true' or open = 1)";
		String[] selectionArgs = {};
		mCursor = mContext.getContentResolver().query(
				note.uri(),
				new String[] { "name", "short_memo", "color", "open",
						"trashed", "stage_id.name" }, selection, selectionArgs,
				null);
	}

	@Override
	public void onDataSetChanged() {
	}

	@Override
	public void onDestroy() {
		mCursor.close();
	}
}
