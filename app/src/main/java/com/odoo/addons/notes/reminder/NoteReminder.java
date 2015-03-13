package com.odoo.addons.notes.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.addons.notes.models.NoteNote;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.utils.ODateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class NoteReminder implements PickerCallBack, OnClickListener,
        OnMenuItemClickListener {

    public static final String DATE_FORMAT = "d MMMM";
    public static final String TIME_FORMAT = "hh:mm a";
    public static final String FULL_FORMAT = DATE_FORMAT + " yyyy "
            + TIME_FORMAT;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private FragmentManager mFragMgr;
    private String mDate = "", mTime = "";
    private NoteNote mNote;
    private Context mContext;
    private View mView;
    private TextView dayPopup, timePopup;
    private PopupMenu mDayPopup, mTimePopup;
    private boolean hasReminder = false;

    public enum QuickDayType {
        Today, Tomorrow, NextWeek, SelectDay
    }

    public enum QuickTimeType {
        Morning, Afternoon, Evening, Night, SelectTime
    }

    public NoteReminder(Context context, FragmentManager fm) {
        mContext = context;
        mNote = new NoteNote(context, null);
        mFragMgr = fm;
    }

    public void setReminder(int note_id, Calendar cal) {
        ODataRow note = mNote.browse(note_id);
        Intent myIntent = new Intent(mContext, ReminderReceiver.class);
        myIntent.putExtras(note.getPrimaryBundleData());
        AlarmManager alarmManager = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0,
                myIntent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                pendingIntent);
    }

    public void setDay(QuickDayType day) {
        switch (day) {
            case Today:
                mDate = ODateUtils.getUTCDate(DATE_FORMAT);
                break;
            case Tomorrow:
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DATE, 1);
                mDate = ODateUtils.getUTCDate(cal.getTime(), DATE_FORMAT);
                break;
            case NextWeek:
                cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_YEAR, 7);
                mDate = ODateUtils.getUTCDate(cal.getTime(), DATE_FORMAT);
                break;
            case SelectDay:
                datePicker = new DatePicker();
                datePicker.setCancelable(false);
                datePicker.show(mFragMgr, day.toString());
                datePicker.setPickerCallback(this);
                break;
        }
    }

    public void setTime(QuickTimeType time) {
        switch (time) {
            case Morning:
                mTime = "9:00 AM";
                break;
            case Afternoon:
                mTime = "1:00 PM";
                break;
            case Evening:
                mTime = "5:00 PM";
                break;
            case Night:
                mTime = "8:00 PM";
                break;
            case SelectTime:
                timePicker = new TimePicker();
                timePicker.setCancelable(false);
                timePicker.show(mFragMgr, time.toString());
                timePicker.setPickerCallback(this);
                break;
        }
    }

    @Override
    public void onDatePick(String date) {
        mDate = date;
        dayPopup.setText(date);
    }

    @Override
    public void onTimePick(String time) {
        mTime = time;
        timePopup.setText(time);
    }

    public String getTime() {
        return mTime;
    }

    public String getDate() {
        return mDate;
    }

    public Calendar getCal() {
        Calendar cal = Calendar.getInstance();
        String date = getDateString();
        String format = DATE_FORMAT + " yyyy " + TIME_FORMAT;
        cal.setTime(ODateUtils.createDateObject(date, format, false));
        return cal;
    }

    public <T> List<Object> toObjectList(HashMap<T, String> map) {
        List<Object> list = new ArrayList<Object>();
        for (T key : map.keySet()) {
            list.add(map.get(key));
        }
        return list;
    }

    public void initControls(View view, String defaultDateTime) {
        mView = view;
        view.findViewById(R.id.reminder_label).setOnClickListener(this);
        if (defaultDateTime != null) {
            onClick(view.findViewById(R.id.reminder_label));
            Date date = ODateUtils
                    .createDateObject(defaultDateTime, FULL_FORMAT, false);
            mDate = new SimpleDateFormat(DATE_FORMAT).format(date);
            mTime = new SimpleDateFormat(TIME_FORMAT).format(date);
            dayPopup.setText(mDate);
            timePopup.setText(mTime);
        }
    }

    private void toggleViews(boolean addReminder) {
        if (addReminder) {
            mView.findViewById(R.id.reminder_ctrls).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.reminder_label).setVisibility(View.GONE);
            mView.findViewById(R.id.cancel_reminder)
                    .setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.reminder_label).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.reminder_ctrls).setVisibility(View.GONE);
            mView.findViewById(R.id.cancel_reminder).setVisibility(View.GONE);
        }
    }

    private void initSpinners() {
        mView.findViewById(R.id.cancel_reminder).setOnClickListener(this);
        initDaySpinner();
        initTimeSpinner();
    }

    private void initDaySpinner() {
        dayPopup = (TextView) mView.findViewById(R.id.txv_reminder_date);
        setDay(QuickDayType.Tomorrow);
        mView.findViewById(R.id.reminder_date).setOnClickListener(this);
        dayPopup.setText("Tomorrow");
    }

    private void initTimeSpinner() {
        timePopup = (TextView) mView.findViewById(R.id.txv_reminder_time);
        setTime(QuickTimeType.Morning);
        mView.findViewById(R.id.reminder_time).setOnClickListener(this);
        timePopup.setText("09:00 AM");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reminder_label:
                toggleViews(true);
                initSpinners();
                hasReminder = true;
                break;
            case R.id.cancel_reminder:
                toggleViews(false);
                hasReminder = false;
                break;
            case R.id.reminder_date:
                mDayPopup = new PopupMenu(mContext, v);
                mDayPopup.getMenuInflater().inflate(R.menu.menu_note_days_popup,
                        mDayPopup.getMenu());
                mDayPopup.setOnMenuItemClickListener(this);
                mDayPopup.show();
                break;
            case R.id.reminder_time:
                mTimePopup = new PopupMenu(mContext, v);
                mTimePopup.getMenuInflater().inflate(R.menu.menu_note_time_popup,
                        mTimePopup.getMenu());
                mTimePopup.setOnMenuItemClickListener(this);
                mTimePopup.show();
                break;
        }
    }

    public boolean hasReminder() {
        return hasReminder;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.day_today:
                setDay(QuickDayType.Today);
                dayPopup.setText(item.getTitle());
                break;
            case R.id.day_tomorrow:
                setDay(QuickDayType.Tomorrow);
                dayPopup.setText(item.getTitle());
                break;
            case R.id.day_next_week:
                setDay(QuickDayType.NextWeek);
                dayPopup.setText(item.getTitle());
                break;
            case R.id.day_select:
                setDay(QuickDayType.SelectDay);
                break;
            case R.id.time_morning:
                setTime(QuickTimeType.Morning);
                timePopup.setText(item.getTitle());
                break;
            case R.id.time_afternoon:
                setTime(QuickTimeType.Afternoon);
                timePopup.setText(item.getTitle());
                break;
            case R.id.time_evening:
                setTime(QuickTimeType.Evening);
                timePopup.setText(item.getTitle());
                break;
            case R.id.time_night:
                setTime(QuickTimeType.Night);
                timePopup.setText(item.getTitle());
                break;
            case R.id.time_select_time:
                setTime(QuickTimeType.SelectTime);
                break;
        }
        hasReminder = true;
        return true;
    }

    public String getDateString() {
        String year = ODateUtils.getUTCDate("yyyy");
        String date = mDate + " " + year + " " + mTime;
        return date;
    }

    public void setHasReminder(Boolean reminder) {
        hasReminder = reminder;
    }
}
