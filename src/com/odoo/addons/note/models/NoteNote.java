package com.odoo.addons.note.models;

import odoo.ODomain;
import android.content.Context;
import android.database.Cursor;

import com.odoo.addons.note.providers.note.NoteProvider;
import com.odoo.addons.note.providers.note.NoteStageProvider;
import com.odoo.addons.note.providers.note.NoteTagProvider;
import com.odoo.base.ir.IrAttachment;
import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OHtml;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.support.OUser;
import com.odoo.support.provider.OContentProvider;
import com.odoo.util.ODate;
import com.odoo.util.StringUtils;

public class NoteNote extends OModel {
	Context mContext = null;
	OColumn name = new OColumn("Title", OVarchar.class, 64).setRequired(true);
	OColumn message_follower_ids = new OColumn("Name", ResPartner.class,
			RelationType.ManyToMany);
	OColumn memo = new OColumn("Desription", OHtml.class);
	OColumn sequence = new OColumn("Sequence", OInteger.class).setDefault(0);
	OColumn stage_id = new OColumn("Select Stage", NoteStage.class,
			RelationType.ManyToOne).setDefault(1);
	OColumn open = new OColumn("Open", OBoolean.class).setDefault(true);
	OColumn date_done = new OColumn("Date", OVarchar.class, 64)
			.setDefault(false);
	// OColumn note_pad_url = new OColumn("Note Pad Url", OText.class);
	OColumn reminder = new OColumn("Reminder", OVarchar.class, 64)
			.setLocalColumn().setDefault("0");
	OColumn color = new OColumn("Color", OInteger.class).setDefault(0);
	OColumn tag_ids = new OColumn("Tags", NoteTag.class,
			RelationType.ManyToMany);
	@Odoo.Functional(store = true, depends = { "memo" }, method = "storeShortMemo")
	OColumn short_memo = new OColumn("Short Memo", OVarchar.class, 100)
			.setLocalColumn();
	OColumn trashed = new OColumn("Trashed", OInteger.class, 5)
			.setLocalColumn().setDefault(0);
	OColumn trashed_date = new OColumn("Trashed date", ODateTime.class)
			.setParsePattern(ODate.DEFAULT_FORMAT).setLocalColumn()
			.setDefault("false");

	public NoteNote(Context context) {
		super(context, "note.note");
		mContext = context;
	}

	@Override
	public ODomain defaultDomain() {
		ODomain domain = new ODomain();
		domain.add("user_id", "=", OUser.current(mContext).getUser_id());
		return domain;
	}

	public int quickCreateNote(String note, int stage_id) {
		return _quickCreateNote(note, note, stage_id);
	}

	private int _quickCreateNote(String note, String memo, int stage_id) {
		OValues values = new OValues();
		values.put("name", note);
		values.put("memo", memo);
		values.put("short_memo", storeShortMemo(values));
		values.put("stage_id", stage_id);
		values.put("sequence", 0);
		values.put("color", 0);
		return resolver().insert(values);
	}

	public int addAttachment(OValues values, Integer stage_id) {
		return addAttachment(values, stage_id, null);
	}

	public int addAttachment(OValues values, Integer stage_id, Integer noteId) {
		String file_type = " ";
		if (values.getString("file_type").contains("audio")) {
			file_type = "audio ";
		}
		if (values.getString("file_type").contains("image")) {
			file_type = "image ";
		}
		String name = "attached " + file_type + "file ";
		Integer note_id = noteId;
		if (noteId == null)
			note_id = _quickCreateNote(name,
					name + "<b>" + values.getString("name") + "</b>", stage_id);
		IrAttachment attachment = new IrAttachment(mContext);
		values.put("res_id", note_id);
		values.put("res_model", getModelName());
		values.put("company_id", user().getCompany_id());
		values.put("is_active", "true");
		attachment.resolver().insert(values);
		return note_id;
	}

	public String storeShortMemo(OValues vals) {
		String body = StringUtils.htmlToString(vals.getString("memo"));
		int end = (body.length() > 150) ? 150 : body.length();
		return body.substring(0, end);
	}

	public Cursor getAttachments(int note_id) {
		IrAttachment attachment = new IrAttachment(mContext);
		Cursor cr = attachment.resolver().query("res_model = ? and res_id = ?",
				new String[] { getModelName(), note_id + "" }, "id DESC");
		return cr;
	}

	public boolean hasAttachment(int note_id) {
		if (getAttachments(note_id).getCount() > 0)
			return true;
		return false;
	}

	@Override
	public OContentProvider getContentProvider() {
		return new NoteProvider();
	}

	public static class NoteStage extends OModel {
		Context mContext = null;
		OColumn name = new OColumn("Name", OText.class).setRequired(true);
		OColumn sequence = new OColumn("Sequence", OInteger.class);

		public NoteStage(Context context) {
			super(context, "note.stage");
			mContext = context;
		}

		@Override
		public ODomain defaultDomain() {
			ODomain domain = new ODomain();
			domain.add("user_id", "=", OUser.current(mContext).getUser_id());
			return domain;
		}

		@Override
		public OContentProvider getContentProvider() {
			return new NoteStageProvider();
		}
	}

	public static class NoteTag extends OModel {
		OColumn name = new OColumn("Name", OText.class).setRequired(true);

		public NoteTag(Context context) {
			super(context, "note.tag");
		}

		@Override
		public OContentProvider getContentProvider() {
			return new NoteTagProvider();
		}
	}
}
