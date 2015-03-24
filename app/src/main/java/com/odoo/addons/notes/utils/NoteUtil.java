package com.odoo.addons.notes.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

import com.odoo.addons.notes.dialogs.NoteColorDialog;
import com.odoo.addons.notes.dialogs.NoteStagesDialog;


public class NoteUtil {
    private static String[] background_colors = {"#AC6DAD", "#6C6DEC",
            "#EB6D6D", "#9ACD32", "#35D374", "#4ED2BE", "#FFA500", "#EBBF6D",
            "#EBEC6D", "#6C6D6D"};
    private static String[] text_colors = {"#270A28", "#171844", "#291010",
            "#2F3E11", "#0A361B", "#0C352E", "#4B3A1C", "#493615", "#3D3E12",
            "#0E1818"};

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
