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
 * Created on 3/4/15 2:44 PM
 */
package odoo.controls.misc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.odoo.R;
import com.odoo.core.utils.OResource;

import java.util.List;

public class OTagsFlowView extends LinearLayout {
    public static final String TAG = OTagsFlowView.class.getSimpleName();
    private Context mContext;

    public OTagsFlowView(Context context) {
        super(context);
        init(context);
    }

    public OTagsFlowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OTagsFlowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OTagsFlowView(Context context, AttributeSet attrs, int defStyleAttr,
                         int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
    }


    public void notifyTagsChange(Context context, final List<String> tags) {
        removeAllViews();
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        final Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                generateView(display, tags);
            }
        };
        post(runnable);
    }

    private void generateView(Display display, List<String> tags) {
        Point point = new Point();
        display.getSize(point);
        int maxWidth = point.x;
        int widthSoFar = 20;
        LinearLayout layout = getRowView();
        for (String tag : tags) {
            TextView tagView = getTag(tag);
            widthSoFar += tagView.getMeasuredWidth();
            if (widthSoFar >= maxWidth) {
                addView(layout);
                layout = getRowView();
                layout.addView(tagView);
                widthSoFar = tagView.getMeasuredWidth();
            } else {
                layout.addView(tagView);
            }
        }
        addView(layout);
    }

    private LinearLayout getRowView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(0, 0, 0, OResource.dimen(mContext, R.dimen.default_4dp));
        layout.setOrientation(HORIZONTAL);
        layout.setGravity(Gravity.END);
        return layout;
    }

    private TextView getTag(String value) {
        TextView textView = new TextView(mContext);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int padd = OResource.dimen(mContext, R.dimen.default_4dp);
        params.leftMargin = padd;
        textView.setPadding(padd, padd, padd, padd);
        textView.setLayoutParams(params);
        textView.setText(value);
        textView.setTag(value);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, OResource.dimen(mContext,
                R.dimen.text_size_xsmall));
        textView.measure(0, 0);
        textView.setBackgroundColor(Color.parseColor("#22000000"));
        textView.setTextColor(Color.BLACK);
        return textView;
    }

}
