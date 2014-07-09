package com.odoo.addons.note.services;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Handler;

import com.odoo.MainActivity;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.support.service.OService;
import com.odoo.util.OENotificationHelper;

public class NotificationService extends OService {
	private Timer timer = new Timer();
	TimerTask scanTask = null;
	Handler handler = new Handler();
	Context mContext = null;
	OENotificationHelper oeNotificationHelper = null;

	@Override
	public Service getService() {
		return this;
	}

	public NotificationService() {
		super();
		this.mContext = this;
	}

	@Override
	public void performSync(Context context, Account account, Bundle extras,
			String authority, ContentProviderClient provider,
			SyncResult syncResult) {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		scanTask = new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						notificationSync();
					}
				});
			}
		};
//		timer.schedule(scanTask, 0, 60 * 1000);
	}

	@SuppressLint("SimpleDateFormat")
	void notificationSync() {
		AccountManager accMan = AccountManager.get(mContext);
		Account[] acc = accMan.getAccountsByType("com.odoo.auth");
		NoteNote note = new NoteNote(mContext);
		if (acc.length > 0) {
			oeNotificationHelper = new OENotificationHelper();
			Intent intent = new Intent(mContext, MainActivity.class);
			oeNotificationHelper.setResultIntent(intent, mContext);
			Calendar c = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			String formattedDate = df.format(c.getTime())+":00";
			List<ODataRow> list = note.select("reminder=?",
					new Object[] { formattedDate });
			for (int i = 0; i < list.size(); i++)
				oeNotificationHelper.showNotification(mContext, list.get(i)
						.getString("name"), list.get(i).getString("memo"), list
						.get(i).getString("reminder"), R.drawable.ic_launcher);
		}
	}
}
