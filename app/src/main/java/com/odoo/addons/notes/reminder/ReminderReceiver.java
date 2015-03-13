package com.odoo.addons.notes.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.odoo.R;
import com.odoo.addons.notes.NoteDetail;
import com.odoo.addons.notes.Notes;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.utils.notification.ONotificationBuilder;


public class ReminderReceiver extends BroadcastReceiver {

    private NoteNote mNote = null;
    private NoteReminder mReminder;
    private ONotificationBuilder builder;

    @Override
    public void onReceive(Context context, Intent intent) {
        mNote = new NoteNote(context, null);
        builder = new ONotificationBuilder(context, 0);
        mReminder = new NoteReminder(context, null);
        Bundle bundle = intent.getExtras();
        int note_id = bundle.getInt(OColumn.ROW_ID);
        ODataRow note = mNote.browse(note_id);
        if (!note.getString("reminder").equals("0")) {
            builder.setAutoCancel(true);
            builder.setIcon(R.drawable.ic_action_notes);
            builder.setTitle(note.getString("short_memo"));
            builder.setText(note.getString("short_memo"));
            builder.setBigText(note.getString("short_memo"));
            // Setting result intent
            Intent rIntent = new Intent(context, NoteDetail.class);
            rIntent.setAction(NoteDetail.ACTION_REMINDER_CALL);
            rIntent.putExtra(Notes.KEY_NOTE_ID, note_id);
            builder.setResultIntent(rIntent);
            // Showing notification
            builder.build().show();
        }
    }

}
