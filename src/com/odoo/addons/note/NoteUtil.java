package com.odoo.addons.note;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import com.odoo.addons.note.dialogs.NoteColorDialog;
import com.odoo.addons.note.dialogs.NoteColorDialog.OnColorSelectListener;
import com.odoo.addons.note.dialogs.NoteStagesDialog;
import com.odoo.addons.note.dialogs.NoteStagesDialog.OnStageSelectListener;

public class NoteUtil {
	private static String[] background_colors = { "#ffffff", "#9fb5b3",
			"#ffa600", "#fffb00", "#cfff3e", "#3eff6e", "#2affff", "#799aff",
			"#c68cff", "#ff79dd" };
	private static String[] text_colors = { "#414141", "#364645", "#4e1f00",
			"#514900", "#3d5100", "#005114", "#005151", "#001965", "#5000a0",
			"#3e002e" };

	public static String[] getBackgroundColors() {
		return background_colors;
	}

	public static String[] getTextColors() {
		return text_colors;
	}

	public static int getBackgroundColor(int color_number) {
		if (color_number < background_colors.length) {
			return Color.parseColor(background_colors[color_number]);
		}
		return Color.parseColor("#ffffff");
	}

	public static int getTextColor(int color_number) {
		if (color_number < text_colors.length) {
			return Color.parseColor(text_colors[color_number]);
		}
		return Color.parseColor("#000000");
	}

	public static AlertDialog noteStages(Context context,
			OnStageSelectListener listener) {
		NoteStagesDialog dialog = new NoteStagesDialog(context, listener);
		return dialog.build();
	}

	public static AlertDialog colorDialog(Context context, String selected,
			OnColorSelectListener listener) {
		NoteColorDialog dialog = new NoteColorDialog(context, selected,
				listener);
		return dialog.build();
	}
}
