package com.odoo.addons.note.providers.note;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.note.models.NoteNote;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class NoteTagProvider extends OContentProvider {

	public static String AUTHORITY = "com.odoo.addons.note.providers.note.notetag";
	public static final String PATH = "note_tag";
	public static final Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY,
			PATH);

	@Override
	public OModel model(Context context) {
		return new NoteNote.NoteTag(context);
	}

	@Override
	public String authority() {
		return NoteTagProvider.AUTHORITY;
	}

	@Override
	public String path() {
		return NoteTagProvider.PATH;
	}

	@Override
	public Uri uri() {
		return NoteTagProvider.CONTENT_URI;
	}

}
