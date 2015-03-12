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
 * Created on 25/2/15 5:38 PM
 */
package com.odoo.addons.notes.models;

import android.content.Context;

import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OInteger;
import com.odoo.core.orm.fields.types.OText;
import com.odoo.core.support.OUser;

import java.util.List;

import odoo.ODomain;

public class NoteStage extends OModel {
    OColumn name = new OColumn("Name", OText.class).setRequired();
    OColumn sequence = new OColumn("Sequence", OInteger.class).setDefaultValue(0);

    public NoteStage(Context context, OUser user) {
        super(context, "note.stage", user);
    }

    @Override
    public ODomain defaultDomain() {
        ODomain domain = new ODomain();
        domain.add("user_id", "=", getUser().getUser_id());
        return domain;
    }

    public int getDefaultNoteStageId() {
        List<ODataRow> cr = select(null, null, null, "sequence");
        return cr.get(0).getInt(OColumn.ROW_ID);
    }
}