package com.odoo.addons.note.providers.note;

import com.odoo.support.provider.OContentProvider;

public class NoteProvider extends OContentProvider {

	public static String CONTENTURI = "com.odoo.addons.note.providers.note.NoteProvider";
	public static String AUTHORITY = "com.odoo.addons.note.providers.note";

	@Override
	public String authority() {
		return AUTHORITY;
	}

	@Override
	public String contentUri() {
		return CONTENTURI;
	}

}
