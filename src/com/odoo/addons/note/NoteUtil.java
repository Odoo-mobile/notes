package com.odoo.addons.note;

import android.graphics.Color;

public class NoteUtil {
	private static String[] background_colors = { "#ffffff", "#cccccc",
			"#ffc7c7", "#fff1c7", "#e3ffc7", "#c7ffd5", "#c7ffff", "#c7d5ff",
			"#e3c7ff", "#ffc7f1" };
	private static String[] text_colors = { "#5a5a5a", "#424242", "#7a3737",
			"#756832", "#5d6937", "#1a7759", "#1a5d83", "#3b3e75", "#4c3668",
			"#6d2c70" };

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
}
