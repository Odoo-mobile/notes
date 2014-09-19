package com.odoo.base.ir.providers.attachments;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.base.ir.IrAttachment;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class AttachmentProvider extends OContentProvider {
	public static final String AUTHORITY = "com.odoo.note.base.ir.providers.attachments";
	public static final String PATH = "ir_attachment";
	public static final Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY,
			PATH);

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Updating res_id to its local id
		if (initialValues.containsKey("id")
				&& initialValues.getAsInteger("id") != 0) {
			int res_id = initialValues.getAsInteger("res_id");
			NoteNote note = new NoteNote(getContext());
			initialValues.put("res_id", note.selectRowId(res_id));
		}
		return super.insert(uri, initialValues);
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		// Updating res_id to its local id
		if (values.containsKey("id") && values.getAsInteger("id") != 0) {
			int res_id = values.getAsInteger("res_id");
			NoteNote note = new NoteNote(getContext());
			values.put("res_id", note.selectRowId(res_id));
			values.put("is_dirty", "false");
		}
		return super.update(uri, values, where, whereArgs);
	}

	@Override
	public String authority() {
		return AttachmentProvider.AUTHORITY;
	}

	@Override
	public String path() {
		return AttachmentProvider.PATH;
	}

	@Override
	public Uri uri() {
		return AttachmentProvider.CONTENT_URI;
	}

	@Override
	public OModel model(Context context) {
		return new IrAttachment(context);
	}

}
