package com.odoo.addons.notes.dialogs;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;

import com.odoo.addons.notes.models.NoteStage;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.utils.OCursorUtils;

public class NoteStagesDialog implements OnClickListener {
    private Context mContext;
    private NoteStage mStages;
    private Builder builder;
    private AlertDialog dialog;
    private Cursor stageCursor;
    private OnStageSelectListener mOnStageSelectListener;
    private int selectedPosition = -1;
    private int stage_id = 0;

    public NoteStagesDialog(Context context, int stage_id,
                            OnStageSelectListener listener) {
        mContext = context;
        mOnStageSelectListener = listener;
        this.stage_id = stage_id;
        mStages = new NoteStage(mContext, null);
        builder = new Builder(mContext);
        initStages();
    }

    private void initStages() {
        stageCursor = mContext.getContentResolver().query(
                mStages.uri(), null, null, null, "sequence"
        );
        if (stageCursor.moveToFirst()) {
            do {
                int row_id = stageCursor.getInt(stageCursor.getColumnIndex(OColumn.ROW_ID));
                if (row_id == stage_id) {
                    selectedPosition = stageCursor.getPosition();
                }
            } while (stageCursor.moveToNext());
        }
    }

    public AlertDialog build() {
        builder.setTitle("Move to stage");
        builder.setSingleChoiceItems(stageCursor, selectedPosition, "name", this);
        dialog = builder.create();
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mOnStageSelectListener != null) {
            stageCursor.moveToPosition(which);
            mOnStageSelectListener.stageSelected(OCursorUtils.toDatarow(stageCursor));
        }
        dialog.dismiss();
    }

    public interface OnStageSelectListener {
        public void stageSelected(ODataRow row);
    }
}
