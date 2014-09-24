package com.odoo.addons.note.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.odoo.addons.note.Note;
import com.odoo.addons.note.NoteDetailActivity;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.notes.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.util.notification.NotificationBuilder;

public class ReminderReceiver extends BroadcastReceiver {

	private NoteNote mNote = null;
	private NoteReminder mReminder;
	private NotificationBuilder builder;

	@Override
	public void onReceive(Context context, Intent intent) {
		mNote = new NoteNote(context);
		builder = new NotificationBuilder(context);
		mReminder = new NoteReminder(context, null);
		Bundle bundle = intent.getExtras();
		int note_id = bundle.getInt(OColumn.ROW_ID);
		ODataRow note = mNote.select(note_id);
		if (!note.getString("reminder").equals("0")) {
			builder.setAutoCancel(true);
			builder.setIcon(R.drawable.ic_action_notes);
			builder.setTitle(note.getString("short_memo"));
			builder.setText(note.getString("short_memo"));
			builder.setBigText(note.getString("short_memo"));
			// Setting result intent
			Intent rIntent = new Intent(context, NoteDetailActivity.class);
			rIntent.setAction(NoteDetailActivity.ACTION_REMINDER_CALL);
			rIntent.putExtra(Note.KEY_NOTE_ID, note_id);
			builder.setResultIntent(rIntent);
			// Showing notification
			builder.build().show();
		}
	}

}
