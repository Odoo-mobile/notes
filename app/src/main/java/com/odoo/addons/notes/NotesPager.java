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
 * Created on 27/2/15 4:24 PM
 */
package com.odoo.addons.notes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.addons.notes.models.NoteStage;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.support.addons.fragment.BaseFragment;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OListAdapter;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.sys.IOnBackPressListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.controls.OControlHelper;

public class NotesPager extends BaseFragment implements ViewPager.OnPageChangeListener, AdapterView.OnItemSelectedListener, IOnBackPressListener {
    public static final String TAG = NotesPager.class.getSimpleName();
    private Notes mNotes;
    private NoteStage mStage;
    private Context mContext;
    private View mView;
    private Handler handler;
    private DataObserver observer;
    private Notes.Type mType = Notes.Type.Note;
    private Spinner mNavSpinner = null;
    private ViewPager mPager;
    private PagerTabStrip mTabStrip;
    private StagePagerAdapter mAdapter;
    private Cursor cursor = null;
    private String[] projection = new String[]{"name"};
    private HashMap<String, Fragment> mFragments = new HashMap<>();
    private List<Object> spinnerItems = new ArrayList<>();
    private OListAdapter mNavSpinnerAdapter = null;
    private int selectedPagerPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notes_pager, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContext = getActivity();
        mNotes = new Notes();
        mStage = new NoteStage(getActivity(), null);
        handler = new Handler();
        observer = new DataObserver(handler);
        mView = view;
        mContext = getActivity();
        parent().setOnBackPressListener(this);
        parent().setHasActionBarSpinner(true);
        Bundle bundle = getArguments();
        if (bundle.containsKey(Notes.KEY_NOTE_FILTER)) {
            mType = Notes.Type.valueOf(bundle.getString(Notes.KEY_NOTE_FILTER));
        }
        mNavSpinner = parent().getActionBarSpinner();
        initPager(view);
        initSpinner();
    }

    private void initPager(View view) {
        getActivity().getContentResolver().registerContentObserver(
                mStage.uri(), true, observer);
        initCR();
        mPager = (ViewPager) view.findViewById(R.id.pager);
//        mPager.setOnPageChangeListener(this);
        mTabStrip = (PagerTabStrip) view.findViewById(R.id.pager_title_strip);
        mTabStrip.setTabIndicatorColor(Color.WHITE);
        mPager.setOffscreenPageLimit(2);
        mAdapter = new StagePagerAdapter(cursor, getChildFragmentManager(), mType);
        mPager.setAdapter(mAdapter);
        for (int i = 0; i < mTabStrip.getChildCount(); ++i) {
            View nextChild = mTabStrip.getChildAt(i);
            if (nextChild instanceof TextView) {
                TextView textViewToConvert = (TextView) nextChild;
                textViewToConvert.setAllCaps(true);
                textViewToConvert.setTextColor(Color.WHITE);
                textViewToConvert.setTypeface(OControlHelper.boldFont());
            }
        }
    }

    private void initSpinner() {
        if (getActivity() == null) {
            return;
        }
        spinnerItems.clear();
        spinnerItems.addAll(mStage.select(null, null, null, "sequence"));
        if (spinnerItems.isEmpty()) {
            parent().setHasActionBarSpinner(false);
            mPager.setVisibility(View.GONE);
            OControls.setVisible(mView, R.id.dashboard_no_item_view);
            OControls.setText(mView, R.id.title, OResource.string(getActivity(),
                    R.string.label_no_notes_found));
            OControls.setText(mView, R.id.subTitle, "");
            OControls.setImage(mView, R.id.icon, R.drawable.ic_action_notes);
            return;
        } else {
            mPager.setVisibility(View.VISIBLE);
            OControls.setGone(mView, R.id.dashboard_no_item_view);
        }
        mNavSpinnerAdapter = new OListAdapter(getActivity(), R.layout.base_simple_list_item_1, spinnerItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.base_simple_list_item_1_selected
                            , parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(getResource(), parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }
        };
        mNavSpinner.setAdapter(mNavSpinnerAdapter);
        mNavSpinner.setOnItemSelectedListener(this);
    }

    private View getSpinnerView(Object row, int pos, View view, ViewGroup parent) {
        ODataRow r = (ODataRow) row;
        OControls.setText(view, android.R.id.text1, r.getString("name"));
        return view;
    }

    private void initCR() {
        cursor = mContext.getContentResolver().query(mStage.uri(),
                projection, null, null, "sequence");
    }

    private class StagePagerAdapter extends FragmentStatePagerAdapter {

        private Notes.Type key_filter;

        public StagePagerAdapter(Cursor cursor, FragmentManager fm, Notes.Type key) {
            super(fm);
            key_filter = key;
        }

        @Override
        public CharSequence getPageTitle(int position) {
//            cursor.moveToPosition(position);
//            String name = cursor.getString(cursor.getColumnIndex("name"));
//            int row_id = cursor.getInt(cursor.getColumnIndex(OColumn.ROW_ID));
//            String where = "stage_id = ?";
//            List<String> args = new ArrayList<>();
//            args.add(row_id + "");
//
//            int count = db().count(where, args.toArray(new String[args.size()]));
//            if (count > 0)
//                name += " (" + count + ")";
//            return name;

            cursor.moveToPosition(position);
            String name = cursor.getString(cursor.getColumnIndex("name"));
            int row_id = cursor.getInt(cursor.getColumnIndex(OColumn.ROW_ID));
            String selection = "stage_id = ? and";
            List<String> args = new ArrayList<>();
            args.add(row_id + "");
            switch (key_filter) {
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
            String[] arguments = args.toArray(new String[args.size()]);
            int count = db().count(selection, arguments);
            if (count > 0)
                name += " (" + count + ")";
            return name;
        }

        @Override
        public Fragment getItem(int index) {
            Notes note = new Notes();
            cursor.moveToPosition(index);
            int stage_id = cursor.getInt(cursor.getColumnIndex(OColumn.ROW_ID));
            Bundle bundle = new Bundle();
            bundle.putInt(Notes.KEY_STAGE_ID, stage_id);
            bundle.putString(Notes.KEY_NOTE_FILTER, key_filter.toString());
            bundle.putInt("index", index);
            note.setArguments(bundle);
            mFragments.put("index_" + index, note);
            return note;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            super.restoreState(null, loader);
        }

        @Override
        public int getCount() {
            return cursor.getCount();
        }

    }

    @Override
    public boolean onBackPressed() {
        return ((IOnBackPressListener) mFragments.get("index_" + mNavSpinner.getSelectedItemPosition())
        ).onBackPressed();
    }

    private class DataObserver extends ContentObserver {

        public DataObserver(Handler handler) {
            super(handler);
        }

        @SuppressLint("NewApi")
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @SuppressLint("NewApi")
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updatePager();
        }
    }

    public void updatePager() {
        initCR();
        initSpinner();
        mAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(selectedPagerPosition);
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        mNotes = new Notes();
        return mNotes.drawerMenus(context);
    }

    @Override
    public Class<NoteNote> database() {
        return NoteNote.class;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        selectedPagerPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mPager.setCurrentItem(position, true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

}
