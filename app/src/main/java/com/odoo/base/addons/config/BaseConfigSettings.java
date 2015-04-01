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
 * Created on 31/3/15 10:53 AM
 */
package com.odoo.base.addons.config;

import android.content.Context;

import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.support.OUser;

import java.util.List;

public class BaseConfigSettings extends OModel {
    public static final String TAG = BaseConfigSettings.class.getSimpleName();

    OColumn module_note_pad = new OColumn("Use collaborative pads (etherpad)", OBoolean.class);

    public BaseConfigSettings(Context context, OUser user) {
        super(context, "base.config.settings", user);
    }


    public static boolean padInstalled(Context context) {
        BaseConfigSettings configSettings = new BaseConfigSettings(context, null);
        List<ODataRow> rows = configSettings.select();
        if (rows.size() > 0) {
            return rows.get(0).getBoolean("module_note_pad");
        }
        return false;
    }
}
