package odoo.controls.misc;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class ONoteAttachmentView extends LinearLayout {

	private static String TAG = ONoteAttachmentView.class.getSimpleName();
	private Context mContext = null;
	private Integer maximum_columns = 1;
	private Cursor cursor = null;
	private AttachmentViewListener mAttachmentViewListener = null;

	public ONoteAttachmentView(Context context) {
		this(context, null, 0);
	}

	public ONoteAttachmentView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ONoteAttachmentView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		setOrientation(VERTICAL);
		setTag(TAG);
	}

	public void createView(Cursor cr) {
		cursor = cr;
		_generateViews();
	}

	private void _generateViews() {
		if (mAttachmentViewListener != null) {
			int items = cursor.getCount();
			if (items > 0) {
				int rows = getRows();
				int item_index = 0;
				for (int i = 0; i < rows; i++) {
					LinearLayout row = (LinearLayout) rowView();
					row.setTag("row_" + i);
					int item_end = ((items - item_index) > maximum_columns) ? maximum_columns
							: items - item_index;
					for (int j = 0; j < item_end; j++) {
						View itemView = mAttachmentViewListener.getView(cursor,
								item_index, row);
						itemView.setTag("item_" + item_index);
						if (itemView != null) {
							ViewGroup container = (ViewGroup) rowItemContainer();
							container.setTag("container_" + item_index);
							container.addView(itemView);
							row.addView(container);
						}
						item_index++;
					}
					addView(row, 0);
				}
			}
		} else {
			throw new NullPointerException("No attachment view handler found");
		}
	}

	private View rowItemContainer() {
		LinearLayout layout = new LinearLayout(mContext);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
				LayoutParams.MATCH_PARENT, 1);
		layout.setLayoutParams(params);
		return layout;
	}

	private View rowView() {
		LinearLayout row = new LinearLayout(mContext);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 0, 1);
		row.setLayoutParams(params);
		row.setOrientation(LinearLayout.HORIZONTAL);
		return row;
	}

	private int getRows() {
		int max = maximum_columns;
		int items = cursor.getCount();
		int rem = items % max;
		int rows = (max > items) ? 1 : (int) (Math.floor(items / max) + rem);
		return rows;
	}

	public ONoteAttachmentView setMaximumCols(int columns) {
		maximum_columns = columns;
		return this;
	}

	public void setAttachmentViewListener(AttachmentViewListener listener) {
		mAttachmentViewListener = listener;
	}

	public interface AttachmentViewListener {
		public View getView(Cursor cr, int position, ViewGroup parent);
	}
}
