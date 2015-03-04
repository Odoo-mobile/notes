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
 * Created on 25/2/15 6:30 PM
 */
package com.odoo.addons.notes;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.odoo.R;
import com.odoo.addons.notes.dialogs.NoteColorDialog;
import com.odoo.addons.notes.dialogs.NoteStagesDialog;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.utils.NoteUtil;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OValues;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OCursorListAdapter;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Notes extends BaseFragment implements ISyncStatusObserverListener, SwipeRefreshLayout.OnRefreshListener, LoaderManager.LoaderCallbacks<Cursor>, OCursorListAdapter.OnViewBindListener, AdapterView.OnItemClickListener {
    public static final String TAG = Notes.class.getSimpleName();
    //    public static final String EXTRA_KEY_TYPE = "extra_key_type";
    public static final String KEY_STAGE_ID = "stage_id";
    public static final String KEY_NOTE_ID = "note_id";
    public static final String KEY_NOTE_FILTER = "note_filter";
    public static final String ACTION_SPEECH_TO_NOTE = "action_speech_to_note";
    private Context mContext = null;
    private Type mCurrentKey = Type.Note;
    private int mStageId = 0;
    private OCursorListAdapter mAdapter = null;
    private GridView mList = null;
    private View mView;
    private ODataRow stage;

    public enum Type {
        Note, Archive, Reminders, Trash
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mContext = getActivity();
        return inflater.inflate(R.layout.notes_listview, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasSwipeRefreshView(view, R.id.swipe_container, this);
        mView = view;
        parent().setHasActionBarSpinner(true);
//        parent().setOnActivityResultListener(this);
        Bundle extra = getArguments();
        if (extra != null) {
            if (extra.containsKey(KEY_STAGE_ID)) {
                mStageId = extra.getInt(KEY_STAGE_ID);
            }
        }
        setHasSyncStatusObserver(TAG, this, db());
        initAdapter();
    }

    private void initAdapter() {
        if (getActivity() != null) {
            mCurrentKey = Type.valueOf(getArguments().getString(KEY_NOTE_FILTER));
            mList = (GridView) mView.findViewById(R.id.gridview);
            mAdapter = new OCursorListAdapter(mContext, null,
                    R.layout.note_custom_view_note);
            mAdapter.setOnViewBindListener(this);
            mList.setAdapter(mAdapter);
            mList.setOnItemClickListener(this);
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();
        items.add(new ODrawerItem(TAG).setTitle("Note")
                .setIcon(R.drawable.ic_action_notes)
                .setExtra(extra(Type.Note))
                .setInstance(new NotesPager()));
        items.add(new ODrawerItem(TAG).setTitle("Reminders")
                .setIcon(R.drawable.ic_action_reminder)
                .setExtra(extra(Type.Reminders))
                .setInstance(new NotesPager()));
        items.add(new ODrawerItem(TAG).setTitle("Archive")
                .setIcon(R.drawable.ic_action_archive)
                .setExtra(extra(Type.Archive))
                .setInstance(new NotesPager()));
        items.add(new ODrawerItem(TAG).setTitle("Trash")
                .setIcon(R.drawable.ic_action_trash)
                .setExtra(extra(Type.Trash))
                .setInstance(new NotesPager()));
        return items;
    }

    public Bundle extra(Type type) {
        Bundle extra = new Bundle();
        extra.putString(KEY_NOTE_FILTER, type.toString());
        return extra;
    }

    @Override
    public Class<NoteNote> database() {
        return NoteNote.class;
    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        String selection = "stage_id = ? and ";
        List<String> args = new ArrayList<>();
        args.add(mStageId + "");
        switch (mCurrentKey) {
            case Note:
                selection += " open = ? and trashed = ?";
                args.add("true");
                args.add("0");
                break;
            case Reminders:
                selection += " reminder != ?";
                args.add("0");
                break;
            case Archive:
                selection += " open = ? and trashed = ?";
                args.add("false");
                args.add("0");
                break;
            case Trash:
                selection += " trashed = ?";
                args.add("1");
                break;
        }
//        if (mCurSearch != null) {
//            selection += " and name like ?";
//            args.add("%" + mCurSearch + "%");
//        }
        String[] arguments = args.toArray(new String[args.size()]);
        return new CursorLoader(mContext, db().uri(), new String[]{"name",
                "short_memo", "color", "open", "trashed"}, selection,
                arguments, OColumn.ROW_ID + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        if (data.getCount() > 0) {
            OControls.setGone(mView, R.id.loadingProgress);
            OControls.setVisible(mView, R.id.swipe_container);
            OControls.setGone(mView, R.id.notes_no_items);
            setHasSwipeRefreshView(mView, R.id.swipe_container, Notes.this);
        } else {
            onRefresh();
            OControls.setGone(mView, R.id.loadingProgress);
            OControls.setGone(mView, R.id.swipe_container);
            OControls.setVisible(mView, R.id.notes_no_items);
            setHasSwipeRefreshView(mView, R.id.notes_no_items, Notes.this);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    @Override
    public void onRefresh() {
        if (inNetwork()) {
            parent().sync().requestSync(NoteNote.AUTHORITY);
            setSwipeRefreshing(true);
        } else {
            hideRefreshingProgress();
            Toast.makeText(getActivity(), _s(R.string.toast_network_required), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onViewBind(View view, Cursor cursor, ODataRow row) {
        int color_number = row.getInt("color");
        view.findViewById(R.id.note_bg_color).setBackgroundColor(
                NoteUtil.getBackgroundColor(color_number));
        OControls.setText(view, R.id.note_memo,
                StringUtils.htmlToString(row.getString("short_memo")));
        OControls.setTextColor(view, R.id.note_memo, NoteUtil.getTextColor(color_number));
//        OControls.setTextViewsColor(view, new int[]{
//                        R.id.note_attachment_counter, R.id.note_memo},
//                NoteUtil.getTextColor(color_number));
        bindRowControls(view, row);
    }

    private void bindRowControls(final View view, final ODataRow row) {
        final int row_id = row.getInt(OColumn.ROW_ID);
        view.findViewById(R.id.note_overflow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(mContext, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_notes_color:
                                chooseColor(row_id, row);
                                break;
                            case R.id.menu_notes_archive:
                                String open = (row.getString("open").equals("false")) ? "true"
                                        : "false";
                                showArchiveUndoBar(row_id, open);
                                break;
                            case R.id.menu_notes_delete:
                                int trashed = (row.getInt("trashed") == 1) ? 0 : 1;
                                showTrashUndoBar(row_id, trashed);
                                break;
                            case R.id.menu_notes_move:
                                moveTo(row_id);
                                break;
                        }
                        return false;
                    }
                });
                if (mCurrentKey == Type.Trash)
                    popupMenu.inflate(R.menu.menu_notes_trash);
                else if (mCurrentKey == Type.Archive)
                    popupMenu.inflate(R.menu.menu_notes_archive);
                else
                    popupMenu.inflate(R.menu.menu_notes_note);
                popupMenu.show();
            }
        });
    }

    private void chooseColor(final int row_id, ODataRow row) {
        int color = row.getInt("color");
        String selected_color = NoteUtil
                .getBackgroundColors()[color];
        NoteUtil.colorDialog(getActivity(), selected_color,
                new NoteColorDialog.OnColorSelectListener() {
                    @Override
                    public void colorSelected(
                            ODataRow color_data) {
                        int index = color_data
                                .getInt("index");
                        OValues values = new OValues();
                        values.put("color", index);
                        values.put("is_dirty", true);
                        db().update(row_id,
                                values);
                        restartLoader();
                    }
                }).show();
    }

    private void showArchiveUndoBar(int row_id, String open) {
        OValues values = new OValues();
        values.put("open", open);
        values.put("is_dirty", true);
        db().update(row_id, values);
        restartLoader();
    }

    private void showTrashUndoBar(int row_id, int trashed) {
        OValues values = new OValues();
        values.put("trashed", trashed);
        values.put("is_dirty", false);
//        values.put("trashed_date", ODate.getUTCDate(ODate.DEFAULT_FORMAT));
        if (mCurrentKey == Type.Trash) {
            values.put("open", "false");
        }
        db().update(row_id, values);
        restartLoader();
    }

    private void moveTo(final int row_id) {
        NoteUtil.noteStages(getActivity(),
                new NoteStagesDialog.OnStageSelectListener() {
                    @Override
                    public void stageSelected(ODataRow row) {
                        int stage_id = row
                                .getInt(OColumn.ROW_ID);
                        OValues values = new OValues();
                        values.put("stage_id", stage_id);
                        values.put("is_dirty", true);
                        db().update(row_id,
                                values);
                        restartLoader();
                    }
                }).show();
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cr = mAdapter.getCursor();
//        int offset = mListControl.getNumColumns();
        cr.moveToPosition(position);//- offset
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_NOTE_ID, cr.getInt(cr.getColumnIndex(OColumn.ROW_ID)));
        Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
