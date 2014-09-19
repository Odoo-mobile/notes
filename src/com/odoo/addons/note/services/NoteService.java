package com.odoo.addons.note.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import odoo.OArguments;
import odoo.ODomain;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.base.ir.Attachments;
import com.odoo.base.ir.IrAttachment;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OSyncHelper;
import com.odoo.support.OUser;
import com.odoo.support.service.OSyncAdapter;
import com.odoo.support.service.OSyncFinishListener;
import com.odoo.support.service.OSyncService;
import com.odoo.util.JSONUtils;
import com.odoo.util.ODate;

public class NoteService extends OSyncService implements OSyncFinishListener {

	public static final String TAG = NoteService.class.getSimpleName();
	private OSyncService service;

	@Override
	public OSyncAdapter getSyncAdapter() {
		service = this;
		NoteNote note = new NoteNote(getApplicationContext());
		Cursor cr = note.resolver().query("trashed = ? ", new String[] { "1" },
				null);
		if (cr.moveToFirst()) {
			Date cur_date = ODate.convertToDate(
					ODate.getUTCDate(ODate.DEFAULT_FORMAT),
					ODate.DEFAULT_FORMAT, false);
			do {
				String rec_date = cr.getString(cr
						.getColumnIndex("trashed_date"));
				if (!rec_date.equals("false")) {
					Date r_date = ODate.convertToDate(rec_date,
							ODate.DEFAULT_FORMAT, false);
					int days = ODate.getDateDiff(cur_date, r_date);
					if (days >= 7) {
						note.delete(cr.getInt(cr.getColumnIndex(OColumn.ROW_ID)));
					}
				}
			} while (cr.moveToNext());
		}
		return new OSyncAdapter(getApplicationContext(), note, this, true)
				.syncDataLimit(50).onSyncFinish(this);
	}

	@Override
	public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
	}

	@Override
	public OSyncAdapter performSync(SyncResult syncResult) {
		return new OSyncAdapter(getApplicationContext(), new NoteStage(
				getApplicationContext()), this, true).onSyncFinish(syncFinish);
	}

	OSyncFinishListener syncFinish = new OSyncFinishListener() {

		@Override
		public OSyncAdapter performSync(SyncResult syncResult) {
			/**
			 * Uploading attachment to server for notes
			 */
			HashMap<Integer, List<Integer>> note_attachments = new HashMap<Integer, List<Integer>>();
			IrAttachment attachments = new IrAttachment(getApplicationContext());
			NoteNote note = new NoteNote(getApplicationContext());
			Attachments attachment = new Attachments(getApplicationContext());
			List<ODataRow> cr = attachments.select("res_model = ?",
					new String[] { "note.note" });
			for (ODataRow row : cr) {
				int note_id = row.getInt("res_id");
				int attachment_id = row.getInt("id");
				if (attachment_id == 0) {
					row.put("res_id", note.selectServerId(note_id));
					attachment_id = attachment.pushToServer(row);
					if (note_attachments.containsKey(note_id)) {
						List<Integer> old_ids = new ArrayList<Integer>();
						old_ids.addAll(note_attachments.get(note_id));
						old_ids.add(attachment_id);
						note_attachments.put(note_id, old_ids);
					} else {
						note_attachments.put(note_id,
								Arrays.asList(new Integer[] { attachment_id }));
					}
				}
			}
			if (note_attachments.size() > 0) {
				sendAttachmentMessage(note, note_attachments);
			}

			/**
			 * Downloading attachments from server
			 */
			ODomain domain = new ODomain();
			domain.add("res_model", "=", "note.note");
			domain.add("res_id", "in", JSONUtils.toArray(note.ids()));
			return new OSyncAdapter(getApplicationContext(), attachments,
					service, true).onSyncFinish(attachment_syncFinish)
					.setDomain(domain);
		}
	};

	OSyncFinishListener attachment_syncFinish = new OSyncFinishListener() {

		@Override
		public OSyncAdapter performSync(SyncResult syncResult) {
			return null;
		}
	};

	private void sendAttachmentMessage(NoteNote note,
			HashMap<Integer, List<Integer>> ids) {
		try {
			OSyncHelper helper = note.getSyncHelper();
			for (int note_id : ids.keySet()) {
				if (ids.get(note_id).size() > 0) {
					OArguments args = new OArguments();
					args.add(note.selectServerId(note_id));
					JSONObject kwargs = new JSONObject();
					kwargs.put("body", "File attached");
					kwargs.put("subject", false);
					kwargs.put("parent_id", false);
					kwargs.put("attachment_ids", new JSONArray(ids.get(note_id)
							.toString()));
					kwargs.put("partner_ids", new JSONArray());
					JSONObject ctx = new JSONObject();
					ctx.put("mail_read_set_read", true);
					ctx.put("default_res_id", note_id);
					ctx.put("default_model", note.getModelName());
					ctx.put("mail_post_autofollow", true);
					ctx.put("mail_post_autofollow_partner_ids", new JSONArray());
					kwargs.put("context", ctx);
					kwargs.put("type", "comment");
					kwargs.put("content_subtype", "plaintext");
					kwargs.put("subtype", "mail.mt_comment");
					helper.callMethod("message_post", args, null, kwargs);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
