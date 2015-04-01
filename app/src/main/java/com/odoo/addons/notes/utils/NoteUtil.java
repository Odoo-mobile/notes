package com.odoo.addons.notes.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import com.odoo.addons.notes.dialogs.NoteColorDialog;
import com.odoo.addons.notes.dialogs.NoteStagesDialog;


public class NoteUtil {
    private static String[] background_colors = {"#c5c5c5", "#736784",
            "#EB6D6D", "#FFA500", "#9ACD32", "#4ED2BE", "#4ed2be", "#6C6DEC",
            "#AC6DAD", "#f75b9f"};
    private static String[] text_colors = {"#131313", "#1d1a21", "#291010",
            "#4B3A1C", "#2F3E11", "#0C352E", "#071a17", "#171844", "#270A28",
            "#310217"};

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

    public static AlertDialog noteStages(Context context, int stage_id,
                                         NoteStagesDialog.OnStageSelectListener listener) {
        NoteStagesDialog dialog = new NoteStagesDialog(context, stage_id, listener);
        return dialog.build();
    }

    public static AlertDialog colorDialog(Context context, String selected,
                                          NoteColorDialog.OnColorSelectListener listener) {
        NoteColorDialog dialog = new NoteColorDialog(context, selected,
                listener);
        return dialog.build();
    }
}
