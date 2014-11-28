package com.odoo.addons.note.widgets;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.odoo.MainActivity;
import com.odoo.addons.note.Note;
import com.odoo.notes.R;
import com.odoo.support.OUser;
import com.odoo.widgets.WidgetHelper;

public class NotesWidget extends AppWidgetProvider {

	public static final String TAG = "com.odoo.addons.note.widgets.NotesWidget";
	public static final String ACTION_NOTES_WIDGET_UPDATE = "com.odoo.addons.widgets.ACTION_NOTES_WIDGET_UPDATE";
	public static final String ACTION_NOTES_WIDGET_CALL = "com.odoo.addons.widgets.ACTION_NOTES_WIDGET_CALL";
	public static final int REQUEST_COMPOSE_NOTE = 112;
	public static final int REQUEST_SPEECH_TO_TEXT = 333;
	public static final int REQUEST_ATTACHMENT = 209;
	public static final String KEY_NOTE_COMPOSE = "note_compose";
	public static final String KEY_NOTE_DETAIL = "note_detail";
	public static final String KEY_NOTE_FILE_ATTACH = "note_file_attach";
	public static final String KEY_NOTE_VOICE_TO_TEXT = "note_voice_to_text";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "NoteWidget->onReceive()");
		if (intent.getAction().equals(ACTION_NOTES_WIDGET_CALL)) {
			Intent intentMain = new Intent(context, MainActivity.class);
			intentMain.setAction(ACTION_NOTES_WIDGET_CALL);
			intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentMain.putExtras(intent.getExtras());
			intentMain.putExtra(WidgetHelper.EXTRA_WIDGET_ITEM_KEY,
					KEY_NOTE_DETAIL);
			context.startActivity(intentMain);
		}
		if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
				&& intent.hasExtra(NotesWidget.ACTION_NOTES_WIDGET_UPDATE)
				&& intent.getExtras().getBoolean(
						NotesWidget.ACTION_NOTES_WIDGET_UPDATE)) {
			Log.v(TAG, "ACTION_NOTES_WIDGET_UPDATE");
			AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(context.getApplicationContext());
			ComponentName component = new ComponentName(context,
					NotesWidget.class);
			int[] ids = appWidgetManager.getAppWidgetIds(component);
			onUpdate(context, appWidgetManager, ids);
			appWidgetManager.notifyAppWidgetViewDataChanged(ids,
					R.id.widgetNoteList);
		}
		super.onReceive(context, intent);

	}

	@SuppressLint({ "InlinedApi", "DefaultLocale" })
	private static RemoteViews initNotesWidgetListView(Context context,
			int widgetId) {
		Log.d(TAG, "NoteWidget->initWidgetListView()");
		RemoteViews mView = new RemoteViews(context.getPackageName(),
				R.layout.widget_notes_layout);

		Intent svcIntent = new Intent(context, NotesRemoteViewService.class);
		svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		List<Integer> filter = NotesWidgetConfigure.getPref(context, widgetId,
				Note.KEY_NOTE_FILTER);
		svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
				filter.toArray(new Integer[filter.size()]));
		svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
		mView.setRemoteAdapter(R.id.widgetNoteList, svcIntent);
		return mView;
	}

	static void updateNoteWidget(Context context, AppWidgetManager manager,
			int[] widgetIds) {
		if (OUser.current(context) == null)
			return;
		for (int widget : widgetIds) {
			// Setting title
			RemoteViews mView = initNotesWidgetListView(context, widget);

			final Intent onItemClick = new Intent(context, NotesWidget.class);
			onItemClick.setAction(ACTION_NOTES_WIDGET_CALL);
			onItemClick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget);
			onItemClick.setData(Uri.parse(onItemClick
					.toUri(Intent.URI_INTENT_SCHEME)));
			final PendingIntent onClickPendingIntent = PendingIntent
					.getBroadcast(context, 1, onItemClick,
							PendingIntent.FLAG_UPDATE_CURRENT);
			mView.setPendingIntentTemplate(R.id.widgetNoteList,
					onClickPendingIntent);

			// compose Note
			Intent intent = new Intent(context, MainActivity.class);
			intent.putExtra(WidgetHelper.EXTRA_WIDGET_ITEM_KEY,
					KEY_NOTE_COMPOSE);
			intent.setAction(ACTION_NOTES_WIDGET_CALL);
			PendingIntent pIntent = PendingIntent.getActivity(context,
					REQUEST_COMPOSE_NOTE, intent, 0);
			mView.setOnClickPendingIntent(R.id.imgCreateQuickNote, pIntent);

			// Attachment Note
			Intent attachmentIntent = new Intent(context, MainActivity.class);
			attachmentIntent.setAction(ACTION_NOTES_WIDGET_CALL);
			attachmentIntent.putExtra(WidgetHelper.EXTRA_WIDGET_ITEM_KEY,
					KEY_NOTE_FILE_ATTACH);
			PendingIntent mPendingIntent = PendingIntent.getActivity(context,
					REQUEST_ATTACHMENT, attachmentIntent, 0);
			mView.setOnClickPendingIntent(R.id.imgAttachImage, mPendingIntent);

			// Speech to text
			Intent speechIntent = new Intent(context, MainActivity.class);
			speechIntent.setAction(ACTION_NOTES_WIDGET_CALL);
			speechIntent.putExtra(WidgetHelper.EXTRA_WIDGET_ITEM_KEY,
					KEY_NOTE_VOICE_TO_TEXT);
			PendingIntent mrPendingIntent = PendingIntent.getActivity(context,
					REQUEST_SPEECH_TO_TEXT, speechIntent, 0);
			mView.setOnClickPendingIntent(R.id.imgAttachSpeechToText,
					mrPendingIntent);

			// Updating widget
			manager.updateAppWidget(widget, mView);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

}
