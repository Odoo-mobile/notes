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
 * Created on 3/4/15 2:51 PM
 */
package com.odoo.addons.notes;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.odoo.R;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteTag;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OCursorUtils;

import java.util.ArrayList;
import java.util.List;

public class ManageTags extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, OCursorListAdapter.OnViewBindListener, AdapterView.OnItemClickListener, TextWatcher, View.OnClickListener {
    public static final String TAG = ManageTags.class.getSimpleName();
    private NoteTag tags;
    private NoteNote notes;
    private Bundle extra;
    private OCursorListAdapter adapter;
    private ListView tagListView;
    private List<Integer> noteTags = new ArrayList<>();
    private EditText edtSearchTag;
    private String mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_manage_tags);
        tags = new NoteTag(this, null);
        notes = new NoteNote(this, null);
        extra = getIntent().getExtras();
        ODataRow note = notes.browse(extra.getInt("note_id"));
        for (ODataRow tag : note.getM2MRecord("tag_ids").browseEach()) {
            noteTags.add(tag.getInt(OColumn.ROW_ID));
        }
        setResult(RESULT_OK);
        edtSearchTag = (EditText) findViewById(R.id.edt_searchable_input);
        edtSearchTag.addTextChangedListener(this);
        findViewById(R.id.back_icon).setOnClickListener(this);
        init();
    }

    private void init() {
        tagListView = (ListView) findViewById(R.id.tagListView);
        adapter = new OCursorListAdapter(this, null, R.layout.note_tag_item_view);
        adapter.setOnViewBindListener(this);
        tagListView.setAdapter(adapter);
        tagListView.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String where = null;
        String[] args = null;
        if (mFilter != null) {
            where = " name like ?";
            args = new String[]{"%" + mFilter + "%"};
        }
        return new CursorLoader(this, tags.uri(), null, where, args, "name");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.changeCursor(null);
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        OControls.setText(view, R.id.tagName, row.getString("name"));
        ImageView checkBox = (ImageView) view.findViewById(R.id.tagChecked);
        if (noteTags.indexOf(row.getInt(OColumn.ROW_ID)) > -1) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setTag(true);
        } else {
            checkBox.setTag(false);
            checkBox.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ImageView checkBox = (ImageView) view.findViewById(R.id.tagChecked);
        ODataRow row = OCursorUtils.toDatarow((Cursor) adapter.getItem(position));
        boolean checked = (boolean) checkBox.getTag();
        if (checked) {
            int index = noteTags.indexOf(row.getInt(OColumn.ROW_ID));
            if (index > -1) {
                noteTags.remove(index);
            }
            checkBox.setTag(false);
            checkBox.setVisibility(View.GONE);
        } else {
            noteTags.add(row.getInt(OColumn.ROW_ID));
            checkBox.setTag(true);
            checkBox.setVisibility(View.VISIBLE);
        }
        OValues values = new OValues();
        values.put("tag_ids", noteTags);
        notes.update(extra.getInt("note_id"), values);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0) {
            mFilter = s.toString();
        } else {
            mFilter = null;
        }
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
