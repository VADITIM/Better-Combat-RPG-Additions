package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.Layout;
import dev.vadim.bcenchantments.ui.Palette;
import dev.vadim.bcenchantments.ui.Sheet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The fake game screen, edited in place. Click to select, drag to move, drag a row (by its grip on the left,
 * its empty space, or with Shift) to reorder, double-click text to reword it, right-click for everything
 * else. In Play mode clicks go to the screen as in game.
 */
final class Designer extends JComponent {
	/** What the window around the designer hears about. */
	interface Host {
		void message(String text);

		/** The selection changed, or what it shows did. */
		void selected();

		void openProperties();
	}

	private static final Color PICK = new Color(0xFF9A1F);
	private static final Color PICK_SOFT = new Color(0x80FF9A1F, true);
	private static final Color HOVER = new Color(0xE0FFFFFF, true);
	private static final Color GUIDE = new Color(0x3FD0FF);
	private static final Color TAG = new Color(0xE0101010, true);
	private static final int THRESHOLD = 4;
	private static final int SNAP = 3;
	private static final int GRIP = 6;

	/** A selection that outlives the frame: a part by id, kind, row and which of its like in that row. */
	record Sel(String id, String kind, String ref, int nth) {
	}

	private enum Mode {
		NONE, PENDING_PART, PENDING_ROWS, PART, ROWS, INSERT
	}

	final Stage stage;
	final Edits edits;
	private final Host host;
	int scale = 3;
	boolean play;
	/** Whether a move or removal of a part changes only the one in its row, not every part of its name. */
	boolean only;
	private boolean alt;

	private int mx = -100;
	private int my = -100;
	private Sel sel;

	private Mode mode = Mode.NONE;
	private int pressX;
	private int pressY;
	private boolean cycleOnRelease;
	private Ctx.Part pressPart;
	private boolean pressOnly;
	// A part being dragged.
	private String keyX;
	private String keyY;
	private String rawX;
	private String rawY;
	private int startX;
	private int startY;
	private int moveX;
	private int moveY;
	private float[] grab;
	private final List<Float> snapsX = new ArrayList<>();
	private final List<Float> snapsY = new ArrayList<>();
	private float guideX = Float.NaN;
	private float guideY = Float.NaN;
	// Rows being dragged, or a new row from the Insert bar.
	private List<Layout.Placed> dragRows = List.of();
	private String insertType;
	private String dropAfter;
	private float dropY;
	private float dropX0;
	private float dropX1;
	// Typing into a part.
	private JTextField editor;
	private Edits.Words editing;
	private String editAfterFrame;

	Designer(Stage stage, Edits edits, Host host) {
		this.stage = stage;
		this.edits = edits;
		this.host = host;
		setLayout(null);
		setPreferredSize(new Dimension(1000, 700));
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				track(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				track(e);
				dragged(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				track(e);
				pressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				track(e);
				released(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (!play && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
					Ctx.Part p = selected();
					if (p != null && !p.kind().equals("row")) {
						startEdit(p);
					}
				}
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				track(e);
				stage.scroll(mx, my, -e.getPreciseWheelRotation());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (mode == Mode.NONE) {
					mx = -100;
					my = -100;
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
		keys();
	}

	private void track(MouseEvent e) {
		mx = e.getX() / scale;
		my = e.getY() / scale;
		alt = e.isAltDown();
	}

	// ---- Selection ----

	List<Ctx.Part> parts() {
		return stage.ctx.parts;
	}

	private Sel selOf(Ctx.Part p) {
		int nth = 0;
		for (Ctx.Part q : parts()) {
			if (q == p) {
				break;
			}
			if (q.id().equals(p.id()) && q.kind().equals(p.kind()) && Objects.equals(q.ref(), p.ref())) {
				nth++;
			}
		}
		return new Sel(p.id(), p.kind(), p.ref(), nth);
	}

	private Ctx.Part resolve(Sel s) {
		if (s == null) {
			return null;
		}
		int nth = 0;
		Ctx.Part last = null;
		for (Ctx.Part q : parts()) {
			if (!q.removed() && q.id().equals(s.id) && q.kind().equals(s.kind) && Objects.equals(q.ref(), s.ref)) {
				if (nth++ == s.nth) {
					return q;
				}
				last = q;
			}
		}
		return last;
	}

	/** The part selected, as drawn in the last frame, or null. */
	Ctx.Part selected() {
		return resolve(sel);
	}

	void select(Ctx.Part p) {
		Sel next = p == null ? null : selOf(p);
		if (!Objects.equals(next, sel)) {
			sel = next;
			host.selected();
		}
	}

	void clearSelection() {
		select(null);
	}

	/** Puts the mouse at a GUI pixel, for {@code --window-shot}. */
	void pointAt(int x, int y) {
		mx = x;
		my = y;
	}

	private static float area(Ctx.Part p) {
		return (p.x1() - p.x0()) * (p.y1() - p.y0());
	}

	/** The smallest drawn part under a point that is not a row; among equals the one on top. */
	private Ctx.Part leafAt(int x, int y) {
		Ctx.Part best = null;
		for (Ctx.Part p : parts()) {
			if (!p.removed() && !p.kind().equals("row") && p.contains(x, y) && (best == null || area(p) <= area(best))) {
				best = p;
			}
		}
		return best;
	}

	/** The smallest row under a point, or null. */
	private Ctx.Part rowAt(int x, int y) {
		Ctx.Part best = null;
		for (Ctx.Part p : parts()) {
			if (p.kind().equals("row") && p.ref() != null && p.y1() > p.y0() && p.contains(x, y) && (best == null || area(p) < area(best))) {
				best = p;
			}
		}
		return best;
	}

	/** What a click at a point means: the part, or the row when the point is on the row's empty space. */
	private Ctx.Part hitAt(int x, int y) {
		Ctx.Part leaf = leafAt(x, y);
		Ctx.Part row = rowAt(x, y);
		if (row != null && (leaf == null || area(leaf) > area(row))) {
			return row;
		}
		return leaf;
	}

	/** The row whose grip (the strip just left of it) is under a point. */
	private Ctx.Part gripAt(int x, int y) {
		for (Ctx.Part p : parts()) {
			if (p.kind().equals("row") && p.ref() != null && p.y1() > p.y0() && x >= p.x0() - GRIP && x < p.x0() && y >= p.y0() && y < p.y1()
				&& (p.clip() == null || y >= p.clip()[1] && y < p.clip()[3])) {
				return p;
			}
		}
		return null;
	}

	/** The row part {@code p} sits in, as drawn. */
	private Ctx.Part rowPart(Ctx.Part p) {
		if (p == null || p.kind().equals("row")) {
			return p;
		}
		for (Ctx.Part q : parts()) {
			if (q.kind().equals("row") && Objects.equals(q.ref(), p.ref())) {
				return q;
			}
		}
		return null;
	}

	// ---- Mouse ----

	private void pressed(MouseEvent e) {
		commitEdit();
		if (play) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				stage.click(mx, my, System.currentTimeMillis());
			}
			return;
		}
		if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
			Ctx.Part p = hitAt(mx, my);
			Ctx.Part current = selected();
			if (p != null && (current == null || !current.contains(mx, my))) {
				select(p);
			} else if (p == null) {
				select(null);
			}
			Ctx.Part target = selected();
			if (target != null) {
				menu(target, e.getX(), e.getY());
			}
			return;
		}
		if (e.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		pressX = e.getX();
		pressY = e.getY();
		pressOnly = only ^ e.isAltDown();
		cycleOnRelease = false;
		Ctx.Part grip = gripAt(mx, my);
		Ctx.Part hit = hitAt(mx, my);
		if (grip != null) {
			select(grip);
			pendingRows(grip);
		} else if (e.isShiftDown() && hit != null && rowPart(hit) != null) {
			Ctx.Part row = rowPart(hit);
			select(row);
			pendingRows(row);
		} else if (hit != null && hit.kind().equals("row")) {
			select(hit);
			pendingRows(hit);
		} else if (hit != null) {
			Ctx.Part current = selected();
			cycleOnRelease = current != null && current.id().equals(hit.id()) && Objects.equals(current.ref(), hit.ref()) && rowPart(hit) != null;
			select(hit);
			if (Edits.movable(hit)) {
				mode = Mode.PENDING_PART;
				pressPart = hit;
			}
		} else {
			select(null);
			stage.click(mx, my, System.currentTimeMillis());
		}
	}

	private void pendingRows(Ctx.Part row) {
		Layout.Placed placed = edits.placed(row.ref());
		if (placed != null) {
			mode = Mode.PENDING_ROWS;
			dragRows = alt ? List.of(placed) : edits.group(placed);
		}
	}

	private void dragged(MouseEvent e) {
		boolean far = Math.abs(e.getX() - pressX) >= THRESHOLD || Math.abs(e.getY() - pressY) >= THRESHOLD;
		if (mode == Mode.PENDING_PART && far) {
			startPartDrag();
		} else if (mode == Mode.PENDING_ROWS && far) {
			mode = Mode.ROWS;
			cycleOnRelease = false;
		}
		if (mode == Mode.PART) {
			dragPart(e);
		} else if (mode == Mode.ROWS) {
			findDrop(dragRows);
		}
	}

	private void released(MouseEvent e) {
		if (play) {
			return;
		}
		switch (mode) {
			case PART -> endPartDrag(true);
			case ROWS -> {
				if (dropAfter != null && !dropAfter.equals(currentAfter(dragRows))) {
					List<Layout.Placed> rows = dragRows;
					if (edits.moveRows(rows, dropAfter)) {
						host.message("Moved " + (rows.size() == 1 ? "the row " + rows.getFirst().anchor() : "the section " + rows.getFirst().anchor() + " (" + rows.size() + " rows)")
							+ ". Ctrl+Z puts it back.");
					}
				}
			}
			default -> {
				if (cycleOnRelease) {
					select(rowPart(selected()));
				}
			}
		}
		mode = Mode.NONE;
		dragRows = List.of();
		dropAfter = null;
		cycleOnRelease = false;
		pressPart = null;
		repaint();
	}

	// ---- Moving a part ----

	private void startPartDrag() {
		mode = Mode.PART;
		cycleOnRelease = false;
		Ctx.Part p = pressPart;
		keyX = Edits.offsetKey(p, pressOnly, "dx");
		keyY = Edits.offsetKey(p, pressOnly, "dy");
		rawX = stage.sheet.raw(keyX);
		rawY = stage.sheet.raw(keyY);
		startX = edits.offset(keyX);
		startY = edits.offset(keyY);
		moveX = 0;
		moveY = 0;
		grab = new float[] {p.x0(), p.y0(), p.x1(), p.y1()};
		snapsX.clear();
		snapsY.clear();
		String target = Edits.target(p);
		for (Ctx.Part q : parts()) {
			boolean alike = Edits.target(q).equals(target) && (!pressOnly || Objects.equals(q.ref(), p.ref()));
			if (q.removed() || alike || q.id().equals("tip.box")) {
				continue;
			}
			snapsX.add(q.x0());
			snapsX.add(q.x1());
			snapsX.add((q.x0() + q.x1()) / 2);
			if (!q.kind().equals("row")) {
				snapsY.add(q.y0());
				snapsY.add(q.y1());
				snapsY.add((q.y0() + q.y1()) / 2);
			}
		}
	}

	private void dragPart(MouseEvent e) {
		int dx = Math.round((e.getX() - pressX) / (float) scale);
		int dy = Math.round((e.getY() - pressY) / (float) scale);
		guideX = Float.NaN;
		guideY = Float.NaN;
		if (!e.isControlDown()) {
			float[] sx = snap(new float[] {grab[0] + dx, grab[2] + dx, (grab[0] + grab[2]) / 2 + dx}, snapsX);
			if (sx != null) {
				dx += Math.round(sx[0]);
				guideX = sx[1];
			}
			float[] sy = snap(new float[] {grab[1] + dy, grab[3] + dy, (grab[1] + grab[3]) / 2 + dy}, snapsY);
			if (sy != null) {
				dy += Math.round(sy[0]);
				guideY = sy[1];
			}
		}
		if (dx != moveX) {
			moveX = dx;
			stage.sheet.put(keyX, value(keyX, startX + dx));
		}
		if (dy != moveY) {
			moveY = dy;
			stage.sheet.put(keyY, value(keyY, startY + dy));
		}
	}

	private static String value(String key, int v) {
		return v == 0 && key.indexOf('@') >= 0 ? null : Integer.toString(v);
	}

	/** The nearest line within {@link #SNAP} of any of {@code edges}, as {shift, line}, or null. */
	private static float[] snap(float[] edges, List<Float> lines) {
		float best = SNAP + 0.5F;
		float[] out = null;
		for (float edge : edges) {
			for (float line : lines) {
				float d = line - edge;
				if (Math.abs(d) < best && Math.abs(d - Math.round(d)) < 0.01F) {
					best = Math.abs(d);
					out = new float[] {d, line};
				}
			}
		}
		return out;
	}

	/** Ends a part drag: the move becomes one undo step, or with {@code keep} false is taken back. */
	private void endPartDrag(boolean keep) {
		stage.sheet.put(keyX, rawX);
		stage.sheet.put(keyY, rawY);
		guideX = Float.NaN;
		guideY = Float.NaN;
		if (keep && (moveX != 0 || moveY != 0)) {
			int fx = startX + moveX;
			int fy = startY + moveY;
			edits.history.group(() -> {
				edits.setOffset(keyX, fx, false);
				edits.setOffset(keyY, fy, false);
			});
			host.message("Moved " + Edits.target(pressPart) + (pressOnly ? " in this row only" : " everywhere it appears") + " to " + fx + ", " + fy + ".");
		}
		host.selected();
	}

	// ---- Dropping rows ----

	/** The rows of the view on show as drawn, one per row, top to bottom, leaving out {@code moving}. */
	private List<Ctx.Part> rowParts(Set<String> moving) {
		String prefix = stage.viewName() + "/";
		List<Ctx.Part> out = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (Ctx.Part p : parts()) {
			if (p.kind().equals("row") && p.ref() != null && p.ref().startsWith(prefix) && seen.add(p.ref()) && !moving.contains(Edits.anchorOf(p.ref()))) {
				out.add(p);
			}
		}
		out.sort((a, b) -> Float.compare(a.y0(), b.y0()));
		return out;
	}

	/** Where rows dropped at the mouse would go: {@link #dropAfter} and the line to show. */
	private void findDrop(List<Layout.Placed> moving) {
		Set<String> anchors = new HashSet<>();
		for (Layout.Placed p : moving) {
			anchors.add(p.anchor());
		}
		List<Ctx.Part> rows = rowParts(anchors);
		dropAfter = null;
		int[] view = stage.rowsView;
		if (view != null) {
			dropX0 = view[0];
			dropX1 = view[2];
		}
		if (rows.isEmpty()) {
			dropAfter = "^";
			dropY = view != null ? view[1] : my;
			return;
		}
		List<Layout.Placed> order = new ArrayList<>(edits.order());
		order.removeIf(p -> anchors.contains(p.anchor()));
		for (Ctx.Part r : rows) {
			if ((r.y0() + r.y1()) / 2 > my) {
				String anchor = Edits.anchorOf(r.ref());
				String after = "^";
				for (int i = 0; i < order.size(); i++) {
					if (order.get(i).anchor().equals(anchor)) {
						after = i > 0 ? order.get(i - 1).anchor() : "^";
						break;
					}
				}
				dropAfter = after;
				dropY = r.y0();
				dropX0 = r.x0();
				dropX1 = r.x1();
				return;
			}
		}
		Ctx.Part last = rows.getLast();
		dropAfter = Edits.anchorOf(last.ref());
		dropY = last.y1();
		dropX0 = last.x0();
		dropX1 = last.x1();
	}

	/** The anchor {@code rows} sit after now, so a drop there changes nothing. */
	private String currentAfter(List<Layout.Placed> rows) {
		if (rows.isEmpty()) {
			return null;
		}
		List<Layout.Placed> order = edits.order();
		int i = order.indexOf(rows.getFirst());
		return i > 0 ? order.get(i - 1).anchor() : "^";
	}

	/** Autoscrolls while a row is dragged near the top or bottom of the rows. */
	private void autoscroll() {
		int[] v = stage.rowsView;
		if ((mode == Mode.ROWS || mode == Mode.INSERT) && v != null && mx >= v[0] - 20 && mx < v[2] + 20) {
			if (my < v[1] + 8) {
				stage.scroll(mx, Math.max(my, v[1]), 0.15);
			} else if (my > v[3] - 8) {
				stage.scroll(mx, Math.min(my, v[3] - 1), -0.15);
			}
		}
	}

	// ---- The Insert bar ----

	void beginInsert(String type) {
		commitEdit();
		mode = Mode.INSERT;
		insertType = type;
		dropAfter = null;
	}

	/** The mouse, in this component's pixels, while a new row is dragged in. */
	void insertMove(Point p) {
		mx = p.x / scale;
		my = p.y / scale;
		if (contains(p)) {
			findDrop(List.of());
		} else {
			dropAfter = null;
		}
	}

	/** Drops the row being dragged in; {@code click} adds it under the selection instead. */
	void insertEnd(Point p, boolean click) {
		String type = insertType;
		mode = Mode.NONE;
		insertType = null;
		if (click) {
			Ctx.Part row = rowPart(selected());
			String after = row != null && row.ref().startsWith(stage.viewName() + "/") ? Edits.anchorOf(row.ref()) : "$";
			insert(type, after);
		} else if (dropAfter != null && contains(p)) {
			insert(type, dropAfter);
		}
		dropAfter = null;
		requestFocusInWindow();
	}

	private void insert(String type, String after) {
		String view = stage.viewName();
		String id = edits.addRow(view, after, type);
		sel = new Sel("row." + id, "row", view + "/" + id, 0);
		host.selected();
		if (!List.of("icons", "rule", "space").contains(type)) {
			editAfterFrame = id;
		}
		host.message("Added a " + type + " row. Type its text, then Enter. Drag it to move it; Del deletes it.");
	}

	// ---- Typing ----

	/** Opens a text box over {@code p} to reword it, live. */
	void startEdit(Ctx.Part p) {
		commitEdit();
		Edits.Words w = edits.words(p);
		if (w == null) {
			String key = p.text() != null ? p.text().key() : null;
			host.message(p.text() == null ? p.id() + " has no text." : key != null ? "\"" + p.text().plain() + "\" is Minecraft's own text (" + key + "), not the mod's."
				: "\"" + p.text().plain() + "\" is filled in by the game; change the line it comes from instead.");
			return;
		}
		editing = w;
		editor = new JTextField(w.value());
		editor.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
		editor.setForeground(Color.WHITE);
		editor.setBackground(new Color(0x101010));
		editor.setCaretColor(PICK);
		editor.setBorder(javax.swing.BorderFactory.createLineBorder(PICK, 2));
		int x = Math.round(p.x0() * scale) - 4;
		int y = Math.round(p.y0() * scale) - 6;
		int width = Math.max(320, Math.round((p.x1() - p.x0()) * scale) + 60);
		x = Math.max(0, Math.min(x, getWidth() - width));
		editor.setBounds(x, Math.max(0, y), width, 30);
		editor.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				live();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				live();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				live();
			}
		});
		editor.addActionListener(e -> {
			commitEdit();
			requestFocusInWindow();
		});
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		editor.getActionMap().put("cancel", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelEdit();
				requestFocusInWindow();
			}
		});
		editor.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commitEdit();
			}
		});
		add(editor);
		editor.selectAll();
		editor.requestFocusInWindow();
		host.message(w.lang() ? "Editing " + w.key() + " in the lang file: %s and %d are filled in by the game. Enter keeps it, Esc cancels."
			: w.key().endsWith(":items") ? "Item ids, comma separated (diamond_sword, emerald). Enter keeps it, Esc cancels." : "Enter keeps it, Esc cancels.");
	}

	private void live() {
		if (editor != null) {
			edits.preview(editing, editor.getText());
		}
	}

	void commitEdit() {
		if (editor != null) {
			JTextField field = editor;
			Edits.Words w = editing;
			editor = null;
			editing = null;
			remove(field);
			edits.commit(w, field.getText());
			repaint();
			host.selected();
		}
	}

	private void cancelEdit() {
		if (editor != null) {
			JTextField field = editor;
			Edits.Words w = editing;
			editor = null;
			editing = null;
			remove(field);
			edits.commit(w, null);
			repaint();
		}
	}

	boolean editing() {
		return editor != null;
	}

	// ---- Keys ----

	private void keys() {
		bind(KeyEvent.VK_DELETE, 0, this::removeSelected);
		bind(KeyEvent.VK_BACK_SPACE, 0, this::removeSelected);
		bind(KeyEvent.VK_F2, 0, () -> {
			Ctx.Part p = selected();
			if (p != null && !p.kind().equals("row")) {
				startEdit(p);
			}
		});
		bind(KeyEvent.VK_ENTER, 0, () -> {
			Ctx.Part p = selected();
			if (p != null && !p.kind().equals("row")) {
				startEdit(p);
			}
		});
		bind(KeyEvent.VK_ESCAPE, 0, this::escape);
		bind(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK, this::duplicateSelected);
		for (int shift : new int[] {0, InputEvent.SHIFT_DOWN_MASK}) {
			int step = shift == 0 ? 1 : 5;
			bind(KeyEvent.VK_LEFT, shift, () -> arrow(-step, 0));
			bind(KeyEvent.VK_RIGHT, shift, () -> arrow(step, 0));
			bind(KeyEvent.VK_UP, shift, () -> arrow(0, -step));
			bind(KeyEvent.VK_DOWN, shift, () -> arrow(0, step));
		}
		bind(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK, () -> stepRow(-1));
		bind(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK, () -> stepRow(1));
	}

	private void bind(int key, int modifiers, Runnable action) {
		String name = "lab." + key + "." + modifiers;
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, modifiers), name);
		getActionMap().put(name, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!play) {
					action.run();
					repaint();
				} else if (key == KeyEvent.VK_ESCAPE) {
					stage.closeGuide(System.currentTimeMillis());
				}
			}
		});
	}

	private void escape() {
		if (mode == Mode.PART) {
			endPartDrag(false);
			mode = Mode.NONE;
		} else if (mode == Mode.ROWS || mode == Mode.INSERT) {
			mode = Mode.NONE;
			dropAfter = null;
		} else if (selected() != null && !selected().kind().equals("row") && rowPart(selected()) != null) {
			select(rowPart(selected()));
		} else {
			select(null);
		}
	}

	void removeSelected() {
		Ctx.Part p = selected();
		if (p == null) {
			host.message("Select something first: click it.");
			return;
		}
		if (Edits.window(p)) {
			host.message("The window itself can't be removed; recolour it from the right-click menu.");
			return;
		}
		String did = edits.remove(p, only);
		select(null);
		host.message(did + " Ctrl+Z brings it back; so does Removed in the toolbar.");
	}

	private void duplicateSelected() {
		Ctx.Part p = selected();
		String added = p == null ? null : Edits.addedOf(p);
		if (added == null) {
			host.message("Ctrl+D copies a row you added. To get another row like a built-in one, drag one in from Insert.");
			return;
		}
		String copy = edits.duplicate(added);
		sel = new Sel("row." + copy, "row", Edits.viewOf(p.ref()) + "/" + copy, 0);
		host.selected();
		host.message("Copied " + added + " as " + copy + ".");
	}

	private void arrow(int dx, int dy) {
		Ctx.Part p = selected();
		if (p == null) {
			return;
		}
		if (p.kind().equals("row")) {
			if (dy != 0) {
				stepRow(Integer.signum(dy));
			}
			return;
		}
		if (!Edits.movable(p)) {
			return;
		}
		edits.nudge(p, only, dx, dy, true);
		host.selected();
	}

	private void stepRow(int by) {
		Ctx.Part row = rowPart(selected());
		Layout.Placed placed = row == null ? null : edits.placed(row.ref());
		if (placed == null) {
			host.message("Select a row (or something in it) to move it up or down.");
			return;
		}
		if (!edits.step(placed, by)) {
			host.message("It is already at the " + (by < 0 ? "top." : "bottom."));
		}
	}

	// ---- Context menu ----

	private void menu(Ctx.Part p, int x, int y) {
		JPopupMenu m = new JPopupMenu();
		JMenuItem head = new JMenuItem(tag(p));
		head.setEnabled(false);
		m.add(head);
		m.addSeparator();
		boolean row = p.kind().equals("row");
		String added = Edits.addedOf(p);
		Sheet sheet = stage.sheet;
		if (!row) {
			Edits.Words w = edits.words(p);
			if (w != null) {
				item(m, w.key().endsWith(":items") ? "Change items…   F2" : "Edit text…   F2", () -> startEdit(p));
			} else if (p.item() != null && !p.item().empty()) {
				item(m, "Preview another item…", () -> previewItem(p));
			}
			for (String id : Edits.styleIds(p)) {
				for (Sheet.Entry entry : sheet.of(id)) {
					styleItem(m, entry);
				}
			}
		}
		if (added != null) {
			addedMenu(m, added);
		}
		m.addSeparator();
		if (!row && Edits.movable(p)) {
			item(m, "Put back in place (this row)", () -> edits.resetPosition(p, true));
			item(m, "Put back in place (everywhere)", () -> edits.resetPosition(p, false));
		}
		if (!row) {
			item(m, "Reset look", () -> edits.resetLook(p));
		}
		Ctx.Part rowPart = rowPart(p);
		Layout.Placed placed = rowPart == null ? null : edits.placed(rowPart.ref());
		if (placed != null) {
			m.addSeparator();
			List<Layout.Placed> order = edits.order();
			int i = order.indexOf(placed);
			String above = i > 0 ? order.get(i - 1).anchor() : "^";
			m.add(typeMenu("Insert row above", type -> insert(type, above)));
			m.add(typeMenu("Insert row below", type -> insert(type, placed.anchor())));
			item(m, "Move row up   Alt+↑", () -> edits.step(placed, -1));
			item(m, "Move row down   Alt+↓", () -> edits.step(placed, 1));
			if (added != null) {
				item(m, "Duplicate row   Ctrl+D", this::duplicateSelected);
			}
			if (!row) {
				item(m, "Select its row   Esc", () -> select(rowPart));
			}
		}
		m.addSeparator();
		if (row) {
			item(m, added != null ? "Delete this row   Del" : "Remove this row   Del", () -> host.message(edits.remove(p, true)));
			if (added == null) {
				String kind = p.id().substring("row.".length());
				item(m, "Remove every " + kind + " row", () -> {
					edits.removeKind(p);
					host.message("Removed every " + kind + " row. Removed in the toolbar brings them back.");
				});
			}
		} else if (!Edits.window(p)) {
			if (p.ref() != null && added == null) {
				item(m, "Remove from this row" + (only ? "   Del" : ""), () -> host.message(edits.remove(p, true)));
			}
			item(m, (added != null ? "Remove" : "Remove everywhere") + (only && added == null ? "" : "   Del"), () -> host.message(edits.remove(p, false)));
		}
		m.addSeparator();
		item(m, "All properties…", host::openProperties);
		m.show(this, x, y);
	}

	private void item(JComponent menu, String label, Runnable action) {
		JMenuItem item = new JMenuItem(label);
		item.addActionListener(e -> {
			action.run();
			host.selected();
			repaint();
		});
		menu.add(item);
	}

	private JMenu typeMenu(String label, java.util.function.Consumer<String> action) {
		JMenu menu = new JMenu(label);
		for (String type : Layout.TYPES) {
			item(menu, Edits.title(type), () -> action.accept(type));
		}
		return menu;
	}

	private static String propName(String key) {
		String prop = key.substring(key.lastIndexOf('.') + 1).replace('_', ' ').replace("color", "colour");
		return prop.substring(0, 1).toUpperCase(Locale.ROOT) + prop.substring(1);
	}

	/** A menu entry for one look property, or nothing for those the canvas handles itself (position). */
	private void styleItem(JPopupMenu m, Sheet.Entry entry) {
		String key = entry.key;
		String prop = key.substring(key.lastIndexOf('.') + 1);
		if (prop.equals("dx") || prop.equals("dy")) {
			return;
		}
		switch (entry.type) {
			case Sheet.COLOR -> m.add(colourMenu(propName(key), key, entry.value()));
			case Sheet.BOOL -> {
				JCheckBoxMenuItem box = new JCheckBoxMenuItem(propName(key), Boolean.parseBoolean(entry.value()));
				box.addActionListener(e -> {
					edits.history.set(key, Boolean.toString(box.isSelected()));
					host.selected();
				});
				m.add(box);
			}
			case Sheet.FLOAT -> {
				if (prop.equals("scale")) {
					m.add(choices("Text size", key, entry.value(), List.of("1", "2", "3", "4"), v -> v + "×"));
				} else if (prop.contains("opacity")) {
					m.add(choices(propName(key), key, entry.value(), List.of("1", "0.85", "0.7", "0.5", "0.25", "0.12", "0"), v -> Math.round(Float.parseFloat(v) * 100) + "%"));
				}
			}
			case Sheet.INT -> {
				if (prop.equals("size")) {
					m.add(choices("Size", key, entry.value(), List.of("14", "16", "18", "20", "22", "24", "28", "32"), v -> v + " px"));
				} else {
					m.add(numberItem(propName(key), key, entry.value()));
				}
			}
			default -> {
			}
		}
	}

	private JMenuItem numberItem(String label, String key, String value) {
		JMenuItem item = new JMenuItem(label + ": " + value + "…");
		item.addActionListener(e -> {
			String answer = JOptionPane.showInputDialog(this, label + " (" + key + ")", value);
			if (answer != null) {
				try {
					edits.history.set(key, Integer.toString(Integer.parseInt(answer.trim())));
					host.selected();
				} catch (NumberFormatException ex) {
					host.message("\"" + answer + "\" is not a whole number.");
				}
			}
		});
		return item;
	}

	private JMenu choices(String label, String key, String value, List<String> options, java.util.function.UnaryOperator<String> name) {
		JMenu menu = new JMenu(label);
		for (String option : options) {
			boolean on;
			try {
				on = Math.abs(Float.parseFloat(option) - Float.parseFloat(value)) < 0.001F;
			} catch (NumberFormatException e) {
				on = false;
			}
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(name.apply(option), on);
			item.addActionListener(e -> {
				edits.history.set(key, option);
				host.selected();
			});
			menu.add(item);
		}
		item(menu, "Default", () -> edits.history.set(key, null));
		return menu;
	}

	private JMenu colourMenu(String label, String key, String value) {
		Sheet sheet = stage.sheet;
		JMenu menu = new JMenu(label);
		menu.setIcon(swatch(sheet.resolve(value)));
		JMenuItem custom = new JMenuItem("Pick a colour…");
		custom.addActionListener(e -> {
			Color picked = JColorChooser.showDialog(this, key, new Color(sheet.resolve(value) & 0xFFFFFF));
			if (picked != null) {
				edits.history.set(key, Sheet.hex(picked.getRGB()));
				host.selected();
			}
		});
		menu.add(custom);
		item(menu, "Default", () -> edits.history.set(key, null));
		menu.addSeparator();
		for (String name : Palette.defaults().keySet()) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(name, value.equals("$" + name));
			item.setIcon(swatch(0xFF000000 | sheet.palette(name)));
			item.addActionListener(e -> {
				edits.history.set(key, "$" + name);
				host.selected();
			});
			menu.add(item);
		}
		return menu;
	}

	private static Icon swatch(int argb) {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				g.setColor(new Color(argb & 0xFFFFFF));
				g.fillRect(x, y, 14, 14);
				g.setColor(Color.GRAY);
				g.drawRect(x, y, 13, 13);
			}

			@Override
			public int getIconWidth() {
				return 14;
			}

			@Override
			public int getIconHeight() {
				return 14;
			}
		};
	}

	private void addedMenu(JPopupMenu m, String id) {
		Sheet sheet = stage.sheet;
		String type = Layout.field(sheet, id, "type", "label");
		m.addSeparator();
		JMenu types = new JMenu("Row type: " + type);
		for (String t : Layout.TYPES) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(Edits.title(t), t.equals(type));
			item.addActionListener(e -> {
				edits.field(id, "type", t);
				host.selected();
			});
			types.add(item);
		}
		m.add(types);
		if (!List.of("icons", "rule", "space").contains(type)) {
			m.add(fieldChoices("Text colour", id, "tone", Layout.field(sheet, id, "tone", type.equals("heading") ? "muted" : "body"), Layout.TONES));
		}
		if (List.of("label", "heading", "title", "icons").contains(type)) {
			m.add(fieldChoices("Align", id, "align", Layout.field(sheet, id, "align", "left"), Layout.ALIGNS));
		}
		if (List.of("rule", "space").contains(type)) {
			m.add(fieldChoices("Height", id, "size", Layout.field(sheet, id, "size", type.equals("rule") ? "5" : "6"), List.of("1", "2", "4", "6", "8", "12", "16", "24")));
		}
		if (List.of("stat", "icon", "entry").contains(type)) {
			String name = type.equals("stat") ? "Edit value…" : "Edit text under it…";
			item(m, name, () -> editField(id, "sub"));
		}
		if (List.of("icon", "icons", "entry").contains(type)) {
			item(m, "Change items…", () -> editField(id, "items"));
		}
		JCheckBoxMenuItem overlay = new JCheckBoxMenuItem("Float over the row above (takes no space)", Layout.field(sheet, id, "overlay", "false").equals("true"));
		overlay.addActionListener(e -> {
			edits.field(id, "overlay", overlay.isSelected() ? "true" : null);
			host.selected();
		});
		m.add(overlay);
	}

	private JMenu fieldChoices(String label, String id, String field, String value, List<String> options) {
		JMenu menu = new JMenu(label + ": " + value);
		for (String option : options) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(option, option.equals(value));
			item.addActionListener(e -> {
				edits.field(id, field, option);
				host.selected();
			});
			menu.add(item);
		}
		return menu;
	}

	/** Edits a field of an added row that may have no part on screen yet (an empty text under it). */
	private void editField(String id, String field) {
		String suffix = field.equals("items") ? ".slot" : field.equals("sub") ? (Layout.field(stage.sheet, id, "type", "label").equals("stat") ? ".value" : ".sub") : ".text";
		for (Ctx.Part q : parts()) {
			if (q.id().equals(id + suffix) || field.equals("items") && q.id().equals(id + ".slot.item")) {
				startEdit(q);
				return;
			}
		}
		String now = Layout.field(stage.sheet, id, field, "");
		String answer = JOptionPane.showInputDialog(this, field.equals("items") ? "Item ids, comma separated" : "Text", now);
		if (answer != null) {
			edits.field(id, field, answer);
		}
	}

	private void previewItem(Ctx.Part p) {
		String id = p.item().id();
		String answer = JOptionPane.showInputDialog(this, "Show another item here (preview only; the game decides which item it is), such as netherite_sword:",
			stage.canvas.swaps.getOrDefault(id, id));
		if (answer != null) {
			if (answer.isBlank() || answer.trim().equals(id)) {
				stage.canvas.swaps.remove(id);
			} else {
				stage.canvas.swaps.put(id, answer.trim());
			}
		}
	}

	// ---- Drawing ----

	@Override
	protected void paintComponent(Graphics graphics) {
		autoscroll();
		Graphics2D g = (Graphics2D) graphics.create();
		stage.render(g, scale, getWidth(), getHeight(), mx, my, System.currentTimeMillis());
		g.dispose();
		if (editAfterFrame != null) {
			String id = editAfterFrame;
			editAfterFrame = null;
			javax.swing.SwingUtilities.invokeLater(() -> {
				for (Ctx.Part q : parts()) {
					if (q.id().equals(id + ".text")) {
						startEdit(q);
						break;
					}
				}
			});
		}
		Graphics2D o = (Graphics2D) graphics.create();
		overlay(o);
		o.dispose();
	}

	private void overlay(Graphics2D g) {
		if (play) {
			badge(g, "PLAY  ·  clicks go to the screen  ·  Ctrl+P to design");
			return;
		}
		Ctx.Part selected = selected();
		if (mode == Mode.NONE || mode == Mode.PENDING_PART || mode == Mode.PENDING_ROWS) {
			Ctx.Part grip = gripAt(mx, my);
			Ctx.Part hover = grip != null ? grip : hitAt(mx, my);
			Ctx.Part hoverRow = rowPart(hover);
			if (hoverRow != null) {
				g.setColor(new Color(0x30FFFFFF, true));
				outline(g, hoverRow, 0);
				grip(g, hoverRow, grip != null);
			}
			if (hover != null && (selected == null || !selOf(hover).equals(sel))) {
				g.setColor(HOVER);
				outline(g, hover, 0);
				tag(g, hover, tag(hover));
			}
			setCursor(Cursor.getPredefinedCursor(grip != null ? Cursor.MOVE_CURSOR : hover != null && (Edits.movable(hover) || hover.kind().equals("row")) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		}
		if (selected != null) {
			if (!selected.kind().equals("row")) {
				g.setColor(PICK_SOFT);
				Stroke old = g.getStroke();
				g.setStroke(new BasicStroke(1.0F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0F, new float[] {3.0F, 3.0F}, 0.0F));
				String target = Edits.target(selected);
				for (Ctx.Part q : parts()) {
					if (q != selected && !q.removed() && Edits.target(q).equals(target) && (!only || Objects.equals(q.ref(), selected.ref()))) {
						outline(g, q, 1);
					}
				}
				g.setStroke(old);
			}
			g.setColor(PICK);
			g.setStroke(new BasicStroke(2.0F));
			outline(g, selected, 1);
			g.setStroke(new BasicStroke(1.0F));
			if (mode != Mode.PART) {
				tag(g, selected, tag(selected));
			}
		}
		if (mode == Mode.PART) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			g.setColor(GUIDE);
			if (!Float.isNaN(guideX)) {
				g.drawLine(Math.round(guideX * scale), 0, Math.round(guideX * scale), getHeight());
			}
			if (!Float.isNaN(guideY)) {
				g.drawLine(0, Math.round(guideY * scale), getWidth(), Math.round(guideY * scale));
			}
			if (selected != null) {
				tag(g, selected, Edits.target(pressPart) + "  x " + signed(startX + moveX) + "  y " + signed(startY + moveY) + (pressOnly ? "  (this row)" : "  (everywhere)"));
			}
		}
		if (mode == Mode.ROWS || mode == Mode.INSERT) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			Set<String> moving = new HashSet<>();
			for (Layout.Placed p : dragRows) {
				moving.add(p.ref());
			}
			for (Ctx.Part q : parts()) {
				if (q.kind().equals("row") && moving.contains(q.ref())) {
					g.setColor(new Color(0x40FF9A1F, true));
					fill(g, q);
				}
			}
			if (dropAfter != null) {
				int y = Math.round(dropY * scale);
				int x0 = Math.round(dropX0 * scale);
				int x1 = Math.round(dropX1 * scale);
				g.setColor(PICK);
				g.fillRect(x0, y - 1, x1 - x0, 3);
				g.fillOval(x0 - 4, y - 4, 8, 8);
				g.fillOval(x1 - 4, y - 4, 8, 8);
			}
			if (mode == Mode.INSERT) {
				label(g, "+ " + Edits.title(insertType), mx * scale + 14, my * scale + 4);
			} else if (dragRows.size() > 1) {
				label(g, dragRows.size() + " rows (Alt: just the heading)", mx * scale + 14, my * scale + 4);
			}
		}
	}

	private static String signed(int v) {
		return v > 0 ? "+" + v : Integer.toString(v);
	}

	/** A part's name with its text, as the hover tag and menu title show it. */
	private String tag(Ctx.Part p) {
		String text = p.text() != null ? p.text().plain().trim() : "";
		if (text.length() > 28) {
			text = text.substring(0, 27) + "…";
		}
		String name = p.kind().equals("row") ? p.id().substring("row.".length()) + " row" : p.id();
		return name + (text.isEmpty() ? "" : "  \"" + text + "\"");
	}

	private void clip(Graphics2D g, Ctx.Part p) {
		if (p.clip() != null) {
			g.setClip(p.clip()[0] * scale - 2, p.clip()[1] * scale - 2, (p.clip()[2] - p.clip()[0]) * scale + 4, (p.clip()[3] - p.clip()[1]) * scale + 4);
		}
	}

	private void outline(Graphics2D g, Ctx.Part p, int grow) {
		clip(g, p);
		int x = Math.round(p.x0() * scale) - grow;
		int y = Math.round(p.y0() * scale) - grow;
		int w = Math.round((p.x1() - p.x0()) * scale) + 2 * grow;
		int h = Math.round((p.y1() - p.y0()) * scale) + 2 * grow;
		g.drawRect(x, y, Math.max(1, w - 1), Math.max(1, h - 1));
		g.setClip(null);
	}

	private void fill(Graphics2D g, Ctx.Part p) {
		clip(g, p);
		g.fillRect(Math.round(p.x0() * scale), Math.round(p.y0() * scale), Math.round((p.x1() - p.x0()) * scale), Math.round((p.y1() - p.y0()) * scale));
		g.setClip(null);
	}

	/** The dotted handle left of a row that drags it. */
	private void grip(Graphics2D g, Ctx.Part row, boolean hot) {
		if (row.clip() != null) {
			int[] c = row.clip();
			g.setClip((c[0] - GRIP) * scale, c[1] * scale, (c[2] - c[0] + GRIP) * scale, (c[3] - c[1]) * scale);
		}
		int x = Math.round((row.x0() - GRIP + 1) * scale);
		int y0 = Math.round(row.y0() * scale);
		int y1 = Math.round(row.y1() * scale);
		int mid = (y0 + y1) / 2;
		g.setColor(hot ? PICK : new Color(0xA0FFFFFF, true));
		for (int dy = -6; dy <= 6; dy += 4) {
			if (mid + dy > y0 && mid + dy < y1) {
				g.fillRect(x, mid + dy, 2, 2);
				g.fillRect(x + 4, mid + dy, 2, 2);
			}
		}
		g.setClip(null);
	}

	private void tag(Graphics2D g, Ctx.Part p, String text) {
		int x = Math.max(0, Math.round(p.x0() * scale));
		int y = Math.round(p.y0() * scale) - 5;
		if (y < 16) {
			y = Math.round(p.y1() * scale) + 18;
		}
		label(g, text, x, y);
	}

	private void label(Graphics2D g, String text, int x, int y) {
		g.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		int w = g.getFontMetrics().stringWidth(text) + 10;
		x = Math.min(x, getWidth() - w);
		g.setColor(TAG);
		g.fillRect(x, y - 13, w, 18);
		g.setColor(Color.WHITE);
		g.drawString(text, x + 5, y);
	}

	private void badge(Graphics2D g, String text) {
		g.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
		int w = g.getFontMetrics().stringWidth(text) + 16;
		g.setColor(new Color(0xD02E7D32, true));
		g.fillRect(8, 8, w, 22);
		g.setColor(Color.WHITE);
		g.drawString(text, 16, 23);
	}

	/** What the status line suggests when nothing else has been said lately. */
	String hint() {
		if (play) {
			return "Play: click, scroll and open things as in game. Ctrl+P goes back to Design.";
		}
		if (editor != null) {
			return "Type, then Enter to keep it or Esc to cancel.";
		}
		switch (mode) {
			case PART -> {
				return "Moving " + Edits.target(pressPart) + (pressOnly ? " in this row only" : " everywhere it appears") + ". Snaps to edges and centres; hold Ctrl to place freely, Esc cancels.";
			}
			case ROWS -> {
				return "Drop between rows to reorder. A heading takes its section with it (hold Alt as you grab to move it alone). Esc cancels.";
			}
			case INSERT -> {
				return "Drop between rows to add it there.";
			}
			default -> {
			}
		}
		Ctx.Part p = selected();
		if (p == null) {
			return "Click anything to select it. Drag to move, drag a row by its grip (left edge) to reorder, double-click text to edit, right-click for colours and more. Drag a block from Insert to add one.";
		}
		if (p.kind().equals("row")) {
			return "Row " + Edits.anchorOf(p.ref()) + ": drag to reorder, Alt+↑/↓ moves it, Del removes it, right-click inserts rows. Click a part inside to select it.";
		}
		return p.id() + ": drag or use the arrows (Shift ×5) to move" + (only ? " this one" : " every " + Edits.target(p)) + ", Del removes, double-click edits text, right-click for colour and size, Esc selects its row.";
	}
}
