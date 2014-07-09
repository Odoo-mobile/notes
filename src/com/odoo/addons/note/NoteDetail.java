package com.odoo.addons.note;

import java.util.Calendar;
import java.util.List;

import odoo.controls.OForm;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.odoo.addons.note.Note.Keys;
import com.odoo.addons.note.models.NoteNote;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.BaseFragment;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;

public class NoteDetail extends BaseFragment {
	private View mView = null;
	private Keys mKey = null;
	private Integer mId = null;
	private Boolean mLocalRecord = false;
	private OForm mForm = null;
	private Boolean mEditMode = false;
	private ODataRow mRecord = null;
	private Menu mMenu = null;
	String str = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		initArgs();
		setHasOptionsMenu(true);
		mView = inflater.inflate(R.layout.note_detail, container, false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		init();
	}

	private void init() {
		updateMenu(mEditMode);
		switch (mKey) {
		case Note:
		case Archive:
		case Reminders:
			OControls.setVisible(mView, R.id.odooFormNote);
			mForm = (OForm) mView.findViewById(R.id.odooFormNote);
			NoteNote notes = new NoteNote(getActivity());
			if (mId != null) {
				mRecord = notes.select(mId, mLocalRecord);
				mForm.initForm(mRecord);
			} else {
				mForm.setModel(notes);
				mForm.setEditable(mEditMode);
			}
			break;
		}

	}

	private void initArgs() {
		Bundle args = getArguments();
		mKey = Note.Keys.valueOf(args.getString("key"));
		if (mKey == Keys.Archive || mKey == Keys.Reminders)
			mKey = Keys.Note;
		if (args.containsKey("id")) {
			mLocalRecord = args.getBoolean("local_record");
			if (mLocalRecord) {
				mId = args.getInt("local_id");
			} else
				mId = args.getInt("id");
		} else
			mEditMode = true;
	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

	private void updateMenu(boolean edit_mode) {
		mMenu.findItem(R.id.menu_note_detail_save).setVisible(edit_mode);
		mMenu.findItem(R.id.menu_note_detail_edit).setVisible(!edit_mode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_note_detail_edit:
			mEditMode = !mEditMode;
			updateMenu(mEditMode);
			mForm.setEditable(mEditMode);
			break;
		case R.id.menu_note_detail_delete:
			// if(mId!=null)
			// new NoteNote(getActivity()).;
			// getActivity().getSupportFragmentManager().popBackStack();
			DialogFragment FrgDate = new DatePickerFragment();
			FrgDate.show(getFragmentManager(), "Date");
			setTime();
			break;
		case R.id.menu_note_mark_asread:
			// OValues values = mForm.getFormValues();
			// if(mId!=null)
			// new NoteNote(getActivity()).update(values, mId);
			getActivity().getSupportFragmentManager().popBackStack();
			break;
		case R.id.menu_note_detail_save:
			mEditMode = false;
			OValues values = mForm.getFormValues();
			if (values != null) {
				updateMenu(mEditMode);
				if (mId != null) {
					switch (mKey) {
					case Note:
						new NoteNote(getActivity()).update(values, mId,
								mLocalRecord);
						break;
					}
				} else {
					switch (mKey) {
					case Note:
						new NoteNote(getActivity()).create(values);
						break;
					}
				}
				getActivity().getSupportFragmentManager().popBackStack();
			}
			break;
		case R.id.menu_note_forward_asmail:
			break;
		case R.id.menu_note_followers:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_note_detail, menu);
		mMenu = menu;
		updateMenu(mEditMode);
	}

	@SuppressLint("ValidFragment")
	class DatePickerFragment extends DialogFragment implements
			DatePickerDialog.OnDateSetListener {

		public Dialog onCreateDialog(Bundle savedInstanceState) {

			final Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR);
			int month = c.get(Calendar.MONTH);
			int day = c.get(Calendar.DAY_OF_MONTH);
			return new DatePickerDialog(getActivity(), this, year, month, day);
		}

		public void onDateSet(DatePicker arg0, int year, int month, int day) {
			month += 1;
			String strDay = day + "", strMonth = month + "";
			if (day < 10)
				strDay = "0" + day;
			if (month < 10)
				strMonth = "0" + month;
			// date_order.setText(String.valueOf(year) + "-" + strMonth + "-"
			// + strDay);
			str = strDay + "-" + strMonth + "-" + String.valueOf(year);

		}
	}

	public void setTime() {
		Calendar mcurrentTime = Calendar.getInstance();
		int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
		int minute = mcurrentTime.get(Calendar.MINUTE);
		TimePickerDialog mTimePicker;
		mTimePicker = new TimePickerDialog(getActivity(),
				new TimePickerDialog.OnTimeSetListener() {
					@Override
					public void onTimeSet(TimePicker timePicker,
							int selectedHour, int selectedMinute) {
						str = str
								+ " "
								+ (selectedHour < 10 ? "0" + selectedHour
										: selectedHour)
								+ ":"
								+ (selectedMinute < 10 ? "0" + selectedMinute
										: selectedMinute) + ":" + "00";
						Log.e("date", str);
					}
				}, hour, minute, true);
		mTimePicker.setTitle("Select Time");
		mTimePicker.show();
	}
}
