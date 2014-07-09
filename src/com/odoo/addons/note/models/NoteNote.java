package com.odoo.addons.note.models;

import android.content.Context;

import com.odoo.base.res.ResPartner;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.OModel;
import com.odoo.orm.types.OHtml;
import com.odoo.orm.types.OInteger;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;

public class NoteNote extends OModel {
	OColumn name = new OColumn("Title", OVarchar.class, 64);
	OColumn message_follower_ids = new OColumn("Name", ResPartner.class,
			RelationType.ManyToMany);
	OColumn memo = new OColumn("Desription", OHtml.class);
	OColumn sequence = new OColumn("Sequence", OInteger.class).setDefault(0);
	OColumn stage_ids = new OColumn("Name", NoteStage.class,
			RelationType.ManyToOne);
	OColumn open = new OColumn("Open", OVarchar.class, 64).setDefault(true);
	OColumn date_done = new OColumn("Date", OVarchar.class, 64)
			.setDefault(false);
	// OColumn note_pad_url = new OColumn("Note Pad Url", OText.class);
	OColumn reminder = new OColumn("Reminder", OVarchar.class, 64)
			.setLocalColumn().setDefault("");
	OColumn color = new OColumn("Color", OInteger.class).setDefault(0);
	OColumn tag_ids = new OColumn("Tags", NoteTag.class,
			RelationType.ManyToMany);

	public NoteNote(Context context) {
		super(context, "note.note");
	}

	public static class NoteStage extends OModel {
		OColumn name = new OColumn("Name", OText.class).setRequired(true);
		OColumn sequence = new OColumn("Sequence", OInteger.class);

		// OColumn stage_color = new OColumn("Color", OInteger.class)
		// .setLocalColumn();

		public NoteStage(Context context) {
			super(context, "note.stage");
		}
	}

	public static class NoteTag extends OModel {
		OColumn name = new OColumn("Name", OText.class).setRequired(true);

		public NoteTag(Context context) {
			super(context, "note.tag");
		}
	}
}
