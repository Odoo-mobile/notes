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
 * Created on 25/2/15 11:48 AM
 */
package com.odoo.addons.notes.providers;

import android.database.Cursor;
import android.net.Uri;

import com.odoo.addons.notes.models.NoteNote;
import com.odoo.core.orm.provider.BaseModelProvider;

import java.util.Locale;

public class NoteProvider extends BaseModelProvider {
    public static final String TAG = NoteProvider.class.getSimpleName();
    public static final int TAG_FILTER = 159;

    @Override
    public boolean onCreate() {
        String path = new NoteNote(getContext(), null).getModelName().toLowerCase(Locale.getDefault());
        matcher.addURI(authority(), path + "/tag_filter", TAG_FILTER);
        return super.onCreate();
    }

    @Override
    public void setModel(Uri uri) {
        super.setModel(uri);
        mModel = new NoteNote(getContext(), getUser(uri));
    }

    @Override
    public Cursor query(Uri uri, String[] base_projection, String selection, String[] selectionArgs, String sortOrder) {
        int match = matcher.match(uri);
        if (match != TAG_FILTER) {
            return super.query(uri, base_projection, selection, selectionArgs, sortOrder);
        } else {
            NoteNote note = new NoteNote(getContext(), getUser(uri));
            return note.getTagNotes(Integer.parseInt(selectionArgs[0]));
        }
    }

    @Override
    public String authority() {
        return NoteNote.AUTHORITY;
    }
}
