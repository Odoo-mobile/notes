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
 * Created on 25/2/15 11:12 AM
 */
package com.odoo.addons.notes.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.base.addons.ir.IrAttachment;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OHtml;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.StringUtils;

import org.json.JSONArray;

import java.util.List;

import odoo.ODomain;

public class NoteNote extends OModel {
    Context mContext = null;
    public static final String TAG = NoteNote.class.getSimpleName();
    public static final String AUTHORITY = "com.odoo.addons.notes.providers.note_note";
    OColumn name = new OColumn("Title", OVarchar.class).setSize(64).setRequired();
    OColumn message_follower_ids = new OColumn("Name", ResPartner.class, OColumn.RelationType.ManyToMany);
    OColumn memo = new OColumn("Description", OHtml.class);
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(0);
    OColumn stage_id = new OColumn("Select Stage", NoteStage.class,
            OColumn.RelationType.ManyToOne).setDefaultValue(1);
    OColumn open = new OColumn("Open", OBoolean.class).setDefaultValue(true);
    OColumn date_done = new OColumn("Date", OVarchar.class).setSize(64)
            .setDefaultValue(false);
    OColumn reminder = new OColumn("Reminder", OVarchar.class).setSize(64)
            .setLocalColumn().setDefaultValue("0");
    OColumn color = new OColumn("Color", OInteger.class).setDefaultValue(0);
    OColumn tag_ids = new OColumn("Tags", NoteTag.class,
            OColumn.RelationType.ManyToMany);
    OColumn trashed = new OColumn("Trashed", OInteger.class).setSize(5)
            .setLocalColumn().setDefaultValue(0);
    OColumn trashed_date = new OColumn("Trashed date", ODateTime.class)
            .setLocalColumn()
            .setDefaultValue("false");
    @Odoo.Functional(store = true, depends = {"memo"}, method = "storeShortMemo")
    OColumn short_memo = new OColumn("Short Memo", OVarchar.class).setDefaultValue(100)
            .setLocalColumn();
    @Odoo.Functional(store = true, depends = {"stage_id"}, method = "storeStageId")
    OColumn stage_id_name = new OColumn("Select Stage Name", OVarchar.class)
            .setDefaultValue("false").setLocalColumn();

    public NoteNote(Context context, OUser user) {
        super(context, "note.note", user);
        mContext = context;
        setHasMailChatter(true);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }

    @Override
    public ODomain defaultDomain() {
        ODomain domain = new ODomain();
        domain.add("message_follower_ids", "in", new JSONArray().put(getUser().getPartner_id()));
        return domain;
    }

    public int quickCreateNote(String note, int stage_id) {
        return _quickCreateNote(note, note, stage_id);
    }

    private int _quickCreateNote(String note, String memo, int stage_id) {
        OValues values = new OValues();
        values.put("name", note);
        values.put("memo", memo);
        values.put("short_memo", storeShortMemo(values));
        values.put("stage_id", stage_id);
        values.put("sequence", 0);
        values.put("color", 0);
        return insert(values);
    }

    public int addAttachment(OValues values, Integer stage_id) {
        return addAttachment(values, stage_id, null);
    }

    public int addAttachment(OValues values, Integer stage_id, Integer noteId) {
        String file_type = " ";
        if (values.getString("file_type").contains("audio")) {
            file_type = "audio ";
        }
        if (values.getString("file_type").contains("image")) {
            file_type = "image ";
        }
        String name = "attached " + file_type + "file ";
        Integer note_id = noteId;
        if (noteId == null)
            note_id = _quickCreateNote(name,
                    name + "<b>" + values.getString("name") + "</b>", stage_id);
        IrAttachment attachment = new IrAttachment(mContext, null);
        values.put("res_id", note_id);
        values.put("res_model", getModelName());
        values.put("company_id", OUser.current(mContext).getCompany_id());
        values.put("is_active", "true");
        attachment.insert(values);
        return note_id;
    }

    public String storeShortMemo(OValues vals) {
        String body = StringUtils.htmlToString(vals.getString("memo"));
        int end = (body.length() > 150) ? 150 : body.length();
        return body.substring(0, end);
    }

    public String storeStageId(OValues value) {
        try {
            if (!value.getString("stage_id").equals("false")) {
                JSONArray stage_id = (JSONArray) value.get("stage_id");
                return stage_id.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }

    public List<ODataRow> getAttachments(int note_id) {
        IrAttachment attachment = new IrAttachment(mContext, null);
        List<ODataRow> cr = attachment.select(null, "res_model = ? and (res_id = ? or res_id = ?) and res_id != ?",
                new String[]{getModelName(), note_id + "", selectServerId(note_id) + "", "0"}, "id DESC");
        return cr;
    }

    public boolean hasAttachment(int note_id) {
        if (getAttachments(note_id).size() > 0)
            return true;
        return false;
    }

}
