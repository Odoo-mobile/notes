/**
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 *
 * Created on 25/2/15 11:53 AM
 */
package com.odoo.addons.notes.services;

import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.odoo.R;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteStage;
import com.odoo.base.addons.config.BaseConfigSettings;
import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.service.ISyncFinishListener;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.BitmapUtils;
import com.odoo.core.utils.ODateUtils;

import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import odoo.ODomain;

public class NoteService extends OSyncService implements ISyncFinishListener {
    public static final String TAG = NoteService.class.getSimpleName();

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        NoteNote note = new NoteNote(context, null);
        if (!note.isEmptyTable()) {
            List<ODataRow> rows = note.select(null, "trashed = ? ", new String[]{"1"});
            Date cur_date = ODateUtils.createDateObject(
                    ODateUtils.getUTCDate(ODateUtils.DEFAULT_FORMAT),
                    ODateUtils.DEFAULT_FORMAT, false);
            for (ODataRow row : rows) {
                String rec_date = row.getString("trashed_date");
                if (!rec_date.equals("false")) {
                    Date r_date = ODateUtils.createDateObject(rec_date,
                            ODateUtils.DEFAULT_FORMAT, false);
                    int days = ODateUtils.getDateDiff(cur_date, r_date);
                    if (days >= 7) {
                        note.delete(row.getInt(OColumn.ROW_ID));
                    }
                }
            }
        }
        return new OSyncAdapter(context, NoteNote.class, this, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        NoteStage noteStage = new NoteStage(getApplicationContext(), user);
        BaseConfigSettings configSettings = new BaseConfigSettings(getApplicationContext(), user);
        if (adapter.getModel().getModelName().equals("note.note")) {
            adapter.syncDataLimit(50).onSyncFinish(this);
        } else if (adapter.getModel().getModelName().equals(noteStage.getModelName())) {

            // Checking for stages on server
            // If no stages found on server creating default stages
            if (adapter.getModel().isEmptyTable()) {
                try {
                    Log.d(TAG, "Creating stages to server");
                    String[] defaultStages = getApplicationContext().getResources()
                            .getStringArray(R.array.default_stages);
                    int i = 0;
                    for (String stage : defaultStages) {
                        JSONObject record = new JSONObject();
                        record.put("name", stage);
                        record.put("sequence", i++);
                        record.put("user_id", user.getUser_id());
                        adapter.getModel().getServerDataHelper().createOnServer(record);
                    }
                    adapter.getModel().quickSyncRecords(new ODomain());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            adapter.onSyncFinish(finishListener);
        } else if (adapter.getModel().getModelName().equals(configSettings.getModelName())) {
            // Updating attachment's res_id for notes
            NoteNote note = new NoteNote(getApplicationContext(), user);
            IrAttachment irAttachment = new IrAttachment(getApplicationContext(), user);
            List<ODataRow> noteAttachments = irAttachment.select(null, "res_model = ? and (id = ? or id = ?)",
                    new String[]{note.getModelName(), "0", "false"});
            for (ODataRow attachment : noteAttachments) {
                try {
                    int note_id = note.selectServerId(attachment.getInt("res_id"));
                    attachment.put("res_id", note_id);
                    attachment.put("res_model", note.getModelName());
                    Uri uri = Uri.parse(attachment.getString("file_uri"));
                    attachment.put("datas", BitmapUtils.uriToBase64(uri, getApplication().getContentResolver()));
                    JSONObject datas = IrAttachment.valuesToData(irAttachment, attachment.toValues());
                    int newId = irAttachment.getServerDataHelper().createOnServer(datas);
                    datas.put("db_datas", "");
                    OValues values = new OValues();
                    values.put("res_model", note.getModelName());
                    values.put("res_id", attachment.getInt("res_id"));
                    values.put("id", newId);
                    irAttachment.update(attachment.getInt(OColumn.ROW_ID), values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, noteAttachments.size() + " attachments updated for notes");
            adapter.onSyncFinish(attachmentSync);
        } else if (adapter.getModel().getModelName().equals("ir.attachment")) {
            ODomain domain = new ODomain();
            domain.add("res_model", "=", "note.note");
            adapter.setDomain(domain);
        }
    }

    ISyncFinishListener finishListener = new ISyncFinishListener() {
        @Override
        public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
            return new OSyncAdapter(getApplicationContext(), BaseConfigSettings.class,
                    NoteService.this, true);
        }
    };

    ISyncFinishListener attachmentSync = new ISyncFinishListener() {
        @Override
        public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
            return new OSyncAdapter(getApplicationContext(), IrAttachment.class, NoteService.this, true);
        }
    };

    @Override
    public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
        return new OSyncAdapter(getApplicationContext(), NoteStage.class, this, true);
    }
}
