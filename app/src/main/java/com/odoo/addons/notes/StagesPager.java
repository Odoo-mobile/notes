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
 * Created on 11/3/15 3:24 PM
 */
package com.odoo.addons.notes;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
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
import com.odoo.core.support.addons.fragment.ISyncStatusObserverListener;
import com.odoo.core.support.drawer.ODrawerItem;
import com.odoo.core.support.list.OListAdapter;
import com.odoo.core.utils.OControls;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.sys.IOnBackPressListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import odoo.controls.OControlHelper;

public class StagesPager extends BaseFragment implements AdapterView.OnItemSelectedListener,
        ViewPager.OnPageChangeListener, ISyncStatusObserverListener, IOnBackPressListener {
    public static final String TAG = StagesPager.class.getSimpleName();
    private View mView;
    private DataObserver dataObserver;
    private Spinner navSpinner;
    private List<Object> spinnerItems = new ArrayList<>();
    private OListAdapter spinnerAdapter;
    private Cursor stageCursor = null;
    private ViewPager viewPager;
    private PagerTabStrip pagerTabStrip;
    private StagePagerAdapter stagePagerAdapter;
    private int stagePosition = 0;
    private boolean updated = false;
    private HashMap<String, Fragment> mFragments = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.stages_pager, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        parent().setOnBackPressListener(this);
        setHasOptionsMenu(true);
        initHandler();
    }

    private void initHandler() {
        parent().setHasActionBarSpinner(true);
        navSpinner = parent().getActionBarSpinner();
        dataObserver = new DataObserver(new Handler());
        setHasSyncStatusObserver(TAG, this, db());
        getActivity().getContentResolver().registerContentObserver(db().uri(), true, dataObserver);
        updateCursor();
    }

    private void updateSpinner() {
        spinnerItems.clear();
        spinnerItems.addAll(db().select(null, null, null, "sequence"));
        if (spinnerItems.isEmpty()) {
            parent().sync().requestSync(NoteNote.AUTHORITY);
            parent().setHasActionBarSpinner(false);
            viewPager.setVisibility(View.GONE);
            OControls.setVisible(mView, R.id.dashboard_no_item_view);
            OControls.setText(mView, R.id.title, OResource.string(getActivity(),
                    R.string.label_no_notes_found));
            OControls.setImage(mView, R.id.icon, R.drawable.ic_action_notes);
        } else {
            viewPager.setVisibility(View.VISIBLE);
            OControls.setGone(mView, R.id.dashboard_no_item_view);
        }
        spinnerAdapter = new OListAdapter(getActivity(),
                R.layout.base_simple_list_item_1, spinnerItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(R.layout.base_simple_list_item_1_selected
                                    , parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity())
                            .inflate(getResource(), parent, false);
                }
                return getSpinnerView(getItem(position), position, convertView, parent);
            }
        };
        navSpinner.setAdapter(spinnerAdapter);
        navSpinner.setOnItemSelectedListener(this);
    }

    private View getSpinnerView(Object row, int pos, View view, ViewGroup parent) {
        ODataRow r = (ODataRow) row;
        OControls.setText(view, android.R.id.text1, r.getString("name"));
        return view;
    }

    private void updatePager() {
        viewPager = (ViewPager) mView.findViewById(R.id.pager);
        pagerTabStrip = (PagerTabStrip) mView.findViewById(R.id.pager_title_strip);
        viewPager.setOnPageChangeListener(this);
        viewPager.setOffscreenPageLimit(2);
        pagerTabStrip.setTabIndicatorColor(_c(R.color.theme_primary));
        stagePagerAdapter = new StagePagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(stagePagerAdapter);
        for (int i = 0; i < pagerTabStrip.getChildCount(); ++i) {
            View nextChild = pagerTabStrip.getChildAt(i);
            if (nextChild instanceof TextView) {
                TextView textViewToConvert = (TextView) nextChild;
                textViewToConvert.setAllCaps(true);
                textViewToConvert.setTextColor(_c(R.color.body_text_2));
                textViewToConvert.setTypeface(OControlHelper.boldFont());
            }
        }
        viewPager.setCurrentItem(stagePosition);
    }

    @Override
    public List<ODrawerItem> drawerMenus(Context context) {
        List<ODrawerItem> items = new ArrayList<>();
        items.add(new ODrawerItem(TAG).setTitle("Notes")
                .setIcon(R.drawable.ic_action_notes)
                .setExtra(extra(Notes.Type.Notes))
                .setInstance(new StagesPager()));
        items.add(new ODrawerItem(TAG).setTitle("Reminders")
                .setIcon(R.drawable.ic_action_reminder)
                .setExtra(extra(Notes.Type.Reminders))
                .setInstance(new Notes()));
        items.add(new ODrawerItem(TAG).setTitle("Archive")
                .setIcon(R.drawable.ic_action_archive)
                .setExtra(extra(Notes.Type.Archive))
                .setInstance(new Notes()));
        items.add(new ODrawerItem(TAG).setTitle("Deleted")
                .setIcon(R.drawable.ic_action_trash)
                .setExtra(extra(Notes.Type.Deleted))
                .setInstance(new Notes()));
        return items;
    }

    public Bundle extra(Notes.Type type) {
        Bundle extra = new Bundle();
        extra.putString(Notes.KEY_NOTE_FILTER, type.toString());
        return extra;
    }

    @Override
    public Class<NoteStage> database() {
        return NoteStage.class;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        viewPager.setCurrentItem(position, true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to do
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        navSpinner.setSelection(i);
        stagePosition = i;
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public void onStatusChange(Boolean refreshing) {
        if (!updated && !db().isEmptyTable()) {
            updateCursor();
            updated = true;
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mFragments.size() > 0) {
            return ((IOnBackPressListener) mFragments.get("index_" + navSpinner.getSelectedItemPosition())
            ).onBackPressed();
        }
        return true;
    }

    private class DataObserver extends ContentObserver {

        public DataObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateCursor();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        parent().setOnBackPressListener(null);
        getActivity().getContentResolver()
                .unregisterContentObserver(dataObserver);
    }

    private void updateCursor() {
        stageCursor = getActivity().getContentResolver()
                .query(db().uri(), null, null, null, "sequence");
        updatePager();
        updateSpinner();
        viewPager.setCurrentItem(stagePosition);
    }

    private class StagePagerAdapter extends FragmentStatePagerAdapter {


        public StagePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            super.restoreState(null, loader);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            stageCursor.moveToPosition(position);
            return stageCursor.getString(stageCursor.getColumnIndex("name"));
        }

        @Override
        public Fragment getItem(int i) {
            Notes notes = new Notes();
            stageCursor.moveToPosition(i);
            int stage_id = stageCursor.getInt(stageCursor.getColumnIndex(OColumn.ROW_ID));
            Bundle bundle = new Bundle();
            bundle.putInt(Notes.KEY_STAGE_ID, stage_id);
            bundle.putString(Notes.KEY_NOTE_FILTER, Notes.Type.Notes.toString());
            notes.setArguments(bundle);
            mFragments.put("index_" + i, notes);
            return notes;
        }

        @Override
        public int getCount() {
            return stageCursor.getCount();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", stagePosition);
    }
}
