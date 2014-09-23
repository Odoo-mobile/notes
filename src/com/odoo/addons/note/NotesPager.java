package com.odoo.addons.note;

import java.util.HashMap;
import java.util.List;

import odoo.controls.OControlHelper;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.note.R;
import com.odoo.orm.OColumn;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.util.drawer.DrawerItem;

public class NotesPager extends BaseFragment implements OnPageChangeListener {
	private Note note;
	private NoteStage noteStage;
	private ViewPager mPagger;
	private NoteStagePagerAdapter mAdapter;
	private Note.Keys mKey = Note.Keys.Note;
	private DataObserver observer;
	private Handler handler;
	private Cursor cursor = null;
	private String[] projection = new String[] { "name" };
	private Context mContext;
	private HashMap<String, Fragment> mFragments = new HashMap<String, Fragment>();
	private PagerTabStrip mTabStrip;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContext = getActivity();
		note = new Note();
		noteStage = new NoteStage(getActivity());
		handler = new Handler();
		observer = new DataObserver(handler);
		return inflater.inflate(R.layout.note_pagger, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Bundle bundle = getArguments();
		if (bundle.containsKey(Note.KEY_NOTE_FILTER)) {
			mKey = Note.Keys.valueOf(bundle.getString(Note.KEY_NOTE_FILTER));
		}
		initPagger(view);
	}

	private void initPagger(View view) {
		getActivity().getContentResolver().registerContentObserver(
				new NoteStage(getActivity()).uri(), true, observer);
		if (db().isEmptyTable()) {
			scope = new AppScope(getActivity());
			scope.main().requestSync(NoteProvider.AUTHORITY);
		}
		initCR();
		mPagger = (ViewPager) view.findViewById(R.id.pager);
		mTabStrip = (PagerTabStrip) view.findViewById(R.id.pager_title_strip);
		mTabStrip.setTabIndicatorColor(_c(R.color.odoo_purple));
		mPagger.setOnPageChangeListener(this);
		mPagger.setOffscreenPageLimit(2);
		mAdapter = new NoteStagePagerAdapter(mKey, cursor,
				getChildFragmentManager());
		mPagger.setAdapter(mAdapter);
		
		for (int i = 0; i < mTabStrip.getChildCount(); ++i) {
		    View nextChild = mTabStrip.getChildAt(i);
		    if (nextChild instanceof TextView) {
		       TextView textViewToConvert = (TextView) nextChild;
		       textViewToConvert.setAllCaps(true);
		       textViewToConvert.setTypeface(OControlHelper.lightFont());
		    }
		}
	}

	private void initCR() {
		cursor = mContext.getContentResolver().query(noteStage.uri(),
				projection, null, null, "sequence");
	}

	private class NoteStagePagerAdapter extends FragmentStatePagerAdapter {

		private Note.Keys note_filter;

		public NoteStagePagerAdapter(Note.Keys key, Cursor cursor,
				FragmentManager fm) {
			super(fm);
			note_filter = key;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			cursor.moveToPosition(position);
			return cursor.getString(cursor.getColumnIndex("name"));
		}

		@Override
		public Fragment getItem(int index) {
			Note note = new Note();
			cursor.moveToPosition(index);
			int stage_id = cursor.getInt(cursor.getColumnIndex(OColumn.ROW_ID));
			Bundle bundle = new Bundle();
			bundle.putInt(Note.KEY_STAGE_ID, stage_id);
			bundle.putString(Note.KEY_NOTE_FILTER, note_filter.toString());
			bundle.putInt("index", index);
			note.setArguments(bundle);
			mFragments.put("index_" + index, note);
			return note;
		}

		@Override
		public int getCount() {
			return cursor.getCount();
		}

	}

	@Override
	public Object databaseHelper(Context context) {
		return new NoteNote(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		note = new Note();
		return note.drawerMenus(context);
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
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPageScrollStateChanged(int index) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int index) {
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		int current = mPagger.getCurrentItem();
		Fragment note = mFragments.get("index_" + current);
		if (note != null)
			note.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		cursor.close();
	}
}
