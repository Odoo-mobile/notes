package com.odoo.addons.note.services;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.support.service.OSyncAdapter;
import com.odoo.support.service.OSyncFinishListener;
import com.odoo.support.service.OSyncService;

public class NoteService extends OSyncService implements OSyncFinishListener {

	public static final String TAG = NoteService.class.getSimpleName();

	@Override
	public OSyncAdapter getSyncAdapter() {
		return new OSyncAdapter(getApplicationContext(), new NoteNote(
				getApplicationContext()), true).syncDataLimit(50).onSyncFinish(
				this);
	}

	@Override
	public OSyncAdapter performSync() {
		return new OSyncAdapter(getApplicationContext(), new NoteStage(
				getApplicationContext()), true);
	}

	// @Override
	// public void performSync(Context context, Account account, Bundle extras,
	// String authority, ContentProviderClient provider,
	// SyncResult syncResult) {
	// Log.v(TAG, "NoteService:performSync()");
	// try {
	// Intent intent = new Intent();
	// intent.setAction(SyncFinishReceiver.SYNC_FINISH);
	// NoteNote db = new NoteNote(context);
	// OSyncHelper sync = db.getSyncHelper();
	// if (sync.syncWithServer())
	// context.sendBroadcast(intent);
	//
	// ResPartner resPartner = new ResPartner(context);
	// OSyncHelper syncRes = resPartner.getSyncHelper();
	// if (syncRes.syncWithServer())
	// context.sendBroadcast(intent);
	//
	// NoteStage noteStage = new NoteStage(context);
	// OSyncHelper syncStage = noteStage.getSyncHelper();
	// if (syncStage.syncWithServer())
	// context.sendBroadcast(intent);
	//
	// // IrAttachment attch = new IrAttachment(context);
	// // OSyncHelper syncAttch = attch.getSyncHelper();
	// // if (syncAttch.syncWithServer())
	// // context.sendBroadcast(intent);
	//
	// // MailFollowers maFollowers = new MailFollowers(context);
	// // OSyncHelper syncMail = maFollowers.getSyncHelper();
	// // if (syncMail.syncWithServer())
	// // context.sendBroadcast(intent);
	//
	// // NoteTag mNoteTag = new NoteTag(context);
	// // OSyncHelper syncTag = mNoteTag.getSyncHelper();
	// // if (syncTag.syncWithServer())
	// // context.sendBroadcast(intent);
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
}
