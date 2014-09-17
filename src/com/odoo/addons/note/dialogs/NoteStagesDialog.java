package com.odoo.addons.note.dialogs;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.odoo.addons.note.models.NoteNote.NoteStage;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.util.OControls;

public class NoteStagesDialog implements OnClickListener {
	private Context mContext;
	private NoteStage mStages;
	private Builder builder;
	private AlertDialog dialog;
	private List<ODataRow> stages = new ArrayList<ODataRow>();
	private OnStageSelectListener mOnStageSelectListener;

	public NoteStagesDialog(Context context, OnStageSelectListener listener) {
		mContext = context;
		mOnStageSelectListener = listener;
		mStages = new NoteStage(mContext);
		builder = new Builder(mContext);
		initStages();
	}

	private void initStages() {
		stages.clear();
		Cursor cr = mStages.resolver().query(null, null, "sequence");
		if (cr.moveToFirst()) {
			do {
				ODataRow row = new ODataRow();
				row.put(OColumn.ROW_ID,
						cr.getInt(cr.getColumnIndex(OColumn.ROW_ID)));
				row.put("name", cr.getString(cr.getColumnIndex("name")));
				stages.add(row);
			} while (cr.moveToNext());
		}
	}

	public AlertDialog build() {
		builder.setTitle("Move to stage");
		ArrayAdapter<ODataRow> adapter = new ArrayAdapter<ODataRow>(mContext,
				android.R.layout.simple_list_item_1, stages) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ODataRow row = stages.get(position);
				View v = convertView;
				if (v == null)
					v = LayoutInflater.from(mContext).inflate(
							android.R.layout.simple_list_item_1, parent, false);
				OControls.setText(v, android.R.id.text1, row.getString("name"));
				return v;
			}
		};
		builder.setAdapter(adapter, this);
		dialog = builder.create();
		return dialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (mOnStageSelectListener != null)
			mOnStageSelectListener.stageSelected(stages.get(which));
		dialog.dismiss();
	}

	public interface OnStageSelectListener {
		public void stageSelected(ODataRow row);
	}
}
