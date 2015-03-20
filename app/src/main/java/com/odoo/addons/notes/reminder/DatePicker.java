package com.odoo.addons.notes.reminder;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatePicker extends DialogFragment implements
		DatePickerDialog.OnDateSetListener {

	private PickerCallBack mCallback;
	private boolean called = false;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		Dialog dialog = new DatePickerDialog(getActivity(), this, year, month,
				day);
		return dialog;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
	}

	@Override
	public void onDateSet(android.widget.DatePicker view, int year,
			int monthOfYear, int dayOfMonth) {
		if (mCallback != null && !called) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, monthOfYear);
			cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			cal.set(Calendar.YEAR, year);
			Date now = cal.getTime();
			String date = new SimpleDateFormat(NoteReminder.DATE_FORMAT)
					.format(now);
			mCallback.onDatePick(date);
			called = true;
		}
	}

	public void setPickerCallback(PickerCallBack callback) {
		mCallback = callback;
	}
}
