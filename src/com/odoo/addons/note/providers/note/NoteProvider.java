package com.odoo.addons.note.providers.note;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class NoteProvider extends OContentProvider {

	public static String AUTHORITY = "com.odoo.addons.note.providers.note";
	public static final String PATH = "note_note";
	public static final Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY,
			PATH);

	@Override
	public OModel model(Context context) {
		return new NoteNote(context);
	}

	@Override
	public String authority() {
		return NoteProvider.AUTHORITY;
	}

	@Override
	public String path() {
		return NoteProvider.PATH;
	}

	@Override
	public Uri uri() {
		return NoteProvider.CONTENT_URI;
	}

}
