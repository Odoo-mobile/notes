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
import android.os.Bundle;

import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteStage;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.service.ISyncFinishListener;
import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.ODateUtils;

import java.util.Date;
import java.util.List;

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
        if (adapter.getModel().equals("note.note"))
            adapter.syncDataLimit(50).onSyncFinish(this);
    }


    @Override
    public OSyncAdapter performNextSync(OUser user, SyncResult syncResult) {
        return new OSyncAdapter(getApplicationContext(), NoteStage.class, this, true);
    }
}
