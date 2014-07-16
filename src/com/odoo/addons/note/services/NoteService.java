package com.odoo.addons.note.services;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteTag;
import com.odoo.base.ir.IrAttachment;
import com.odoo.orm.OSyncHelper;
import com.odoo.receivers.SyncFinishReceiver;
import com.odoo.support.service.OService;

public class NoteService extends OService {

	public static final String TAG = NoteService.class.getSimpleName();

	@Override
	public Service getService() {
		return this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
		Log.v(TAG, "NoteService:performSync()");
		try {
			Intent intent = new Intent();
			intent.setAction(SyncFinishReceiver.SYNC_FINISH);
			NoteNote db = new NoteNote(context);
			OSyncHelper sync = db.getSyncHelper();
			if (sync.syncWithServer())
				context.sendBroadcast(intent);

			IrAttachment attch = new IrAttachment(context);
			OSyncHelper syncAttch = attch.getSyncHelper();
			if (syncAttch.syncWithServer())
				context.sendBroadcast(intent);

//			MailFollowers maFollowers = new MailFollowers(context);
//			OSyncHelper syncMail = maFollowers.getSyncHelper();
//			if (syncMail.syncWithServer())
//				context.sendBroadcast(intent);

			NoteTag mNoteTag = new NoteTag(context);
			OSyncHelper syncTag = mNoteTag.getSyncHelper();
			if (syncTag.syncWithServer())
				context.sendBroadcast(intent);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
