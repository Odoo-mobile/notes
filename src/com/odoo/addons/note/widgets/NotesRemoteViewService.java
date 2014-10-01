package com.odoo.addons.note.widgets;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class NotesRemoteViewService extends RemoteViewsService {
	public static final String TAG = "com.odoo.addons.mail.widgets.MailRemoteViewService";

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.d(TAG, "NotesRemoteViewService->onGetViewFactory()");
		NotesRemoteViewFactory rvFactory = new NotesRemoteViewFactory(
				getApplicationContext(), intent);
		return rvFactory;

	}

}
