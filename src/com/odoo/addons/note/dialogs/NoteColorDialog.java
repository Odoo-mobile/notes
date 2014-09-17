package com.odoo.addons.note.dialogs;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.BezelImageView;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.odoo.addons.note.NoteUtil;
import com.odoo.note.R;
import com.odoo.orm.ODataRow;

public class NoteColorDialog implements OnItemClickListener {
	private Builder builder = null;
	private Context mContext;
	private ArrayAdapter<ODataRow> mAdapter;
	private List<ODataRow> colors = new ArrayList<ODataRow>();
	private String selectedColor;
	private OnColorSelectListener mOnColorSelectListener;
	private AlertDialog alertDialog;

	public NoteColorDialog(Context context, String selected_color,
			OnColorSelectListener listener) {
		mContext = context;
		selectedColor = selected_color;
		mOnColorSelectListener = listener;
		String[] bg_colors = NoteUtil.getBackgroundColors();
		String[] font_colors = NoteUtil.getTextColors();
		for (int i = 0; i < bg_colors.length; i++) {
			ODataRow clr = new ODataRow();
			clr.put("index", i);
			clr.put("code", bg_colors[i]);
			clr.put("font_code", font_colors[i]);
			colors.add(clr);
		}
	}

	public AlertDialog build() {
		builder = new Builder(mContext);
		builder.setTitle("Note color");
		builder.setView(getColorGrid());
		alertDialog = builder.create();
		return alertDialog;
	}

	private View getColorGrid() {
		LinearLayout layout = (LinearLayout) LayoutInflater.from(mContext)
				.inflate(R.layout.note_color_grid, null, false);
		initGrid((GridView) layout.findViewById(R.id.color_grid));
		return layout;
	}

	private void initGrid(GridView view) {
		mAdapter = new ArrayAdapter<ODataRow>(mContext,
				R.layout.note_color_chooser_item, colors) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ODataRow row = colors.get(position);
				View view = convertView;
				if (view == null) {
					view = LayoutInflater.from(mContext).inflate(
							R.layout.note_color_chooser_item, parent, false);
				}
				BezelImageView v = (BezelImageView) view
						.findViewById(R.id.note_color);
				v.setImageDrawable(new ColorDrawable(Color.parseColor(row
						.getString("code"))));

				ImageView selected_color = (ImageView) view
						.findViewById(R.id.selected_color);
				int selected_clr = (selectedColor.equals(row.getString("code"))) ? Color
						.parseColor("#eeeeee") : Color.parseColor(row
						.getString("code"));
				selected_color.setColorFilter(selected_clr);
				return view;
			}
		};
		view.setAdapter(mAdapter);
		view.setOnItemClickListener(this);
	}

	public interface OnColorSelectListener {
		public void colorSelected(ODataRow color_data);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mOnColorSelectListener != null) {
			mOnColorSelectListener.colorSelected(colors.get(position));
		}
		alertDialog.dismiss();
	}
}
