package com.odoo.addons.notes.reminder;


import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimePicker extends DialogFragment implements
        TimePickerDialog.OnTimeSetListener {

    private PickerCallBack mCallback;
    private boolean called = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(getActivity(), this,
                hour, minute, false);
        dialog.setTitle("Pick time");
        return dialog;
    }

    @Override
    public void onTimeSet(android.widget.TimePicker view, int hourOfDay,
                          int minute) {
        if (mCallback != null && !called) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.MILLISECOND, 0);
            Date now = cal.getTime();
            String time = new SimpleDateFormat(NoteReminder.TIME_FORMAT)
                    .format(now);
            mCallback.onTimePick(time);
            called = true;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    public void setPickerCallback(PickerCallBack callback) {
        mCallback = callback;
    }
}
