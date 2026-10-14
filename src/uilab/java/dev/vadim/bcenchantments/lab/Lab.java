package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.Layout;
import dev.vadim.bcenchantments.ui.Rich;
import dev.vadim.bcenchantments.ui.Sheet;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * The UI lab: the smithing panel and the Field Guide drawn by the mod's own code, with Minecraft's font and
 * textures, no game needed, and edited in place: select, drag, reorder, reword, recolour, add and remove on
 * the screen itself ({@link Designer}). Ctrl+S writes {@code style.json} and the lang file, which the next
 * build ships.
 * <p>
 * Run with {@code ./gradlew uiLab}. {@code --shot out.png} renders one frame and exits; {@code --dump-style}
 * rewrites {@code style.json} with every property the UI asks for; {@code --smoke} checks the editing.
 */
public final class Lab {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));
	private static final Path STYLE = ROOT.resolve("src/main/resources/assets/bcenchantments/ui/style.json");
	private static final Path LANG = ROOT.resolve("src/main/resources/assets/bcenchantments/lang/en_us.json");
	private static final Color PICK = new Color(0xFF9A1F);
	private static final Color FAINT = new Color(0x55A0E0FF, true);
	private static final long MESSAGE_MS = 6000;

	private final Stage stage;
	private final History history;
	private final Edits edits;
	private final Designer designer;
	private final Inspector inspector;
	private final JFrame frame = new JFrame();
	private final JLabel status = new JLabel(" ");
	private final JButton removed = new JButton("Removed");
	private final JComboBox<String> views = new JComboBox<>();
	private final JToggleButton design = new JToggleButton("Design", true);
	private final JToggleButton play = new JToggleButton("Play  (Ctrl+P)");
	private boolean syncing;
	private String message;
	private long messageAt;
	private int savedRevision;

	private Lab(Stage stage) {
		this.stage = stage;
		this.history = new History(stage.sheet, stage.lang);
		this.edits = new Edits(stage, history);
		this.designer = new Designer(stage, edits, new Designer.Host() {
			@Override
			public void message(String text) {
				Lab.this.message(text);
			}

			@Override
			public void selected() {
				refresh();
			}

			@Override
			public void openProperties() {
				inspector.setVisible(true);
				inspector.refresh();
			}
		});
		this.inspector = new Inspector(frame, stage, history, designer::selected);
		savedRevision = stage.sheet.revision();
		buildFrame();
	}

	public static void main(String[] args) throws Exception {
		String jar = null;
		String shot = null;
		boolean dump = false;
		int view = -2;
		int scale = 3;
		int w = 854;
		int h = 480;
		int tab = 0;
		int mx = -100;
		int my = -100;
		String expand = null;
		String select = null;
		boolean outline = false;
		boolean smokeTest = false;
		String windowShotPath = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--window-shot" -> windowShotPath = args[++i];
				case "--jar" -> jar = args[++i];
				case "--shot" -> shot = args[++i];
				case "--dump-style" -> dump = true;
				case "--smoke" -> smokeTest = true;
				case "--view" -> view = Integer.parseInt(args[++i]);
				case "--scale" -> scale = Integer.parseInt(args[++i]);
				case "--size" -> {
					String[] s = args[++i].split("x");
					w = Integer.parseInt(s[0]);
					h = Integer.parseInt(s[1]);
				}
				case "--tab" -> tab = Integer.parseInt(args[++i]);
				case "--mouse" -> {
					String[] s = args[++i].split(",");
					mx = Integer.parseInt(s[0]);
					my = Integer.parseInt(s[1]);
				}
				case "--expand" -> expand = args[++i];
				case "--select" -> select = args[++i];
				case "--outline" -> outline = true;
				default -> throw new IllegalArgumentException("Unknown option " + args[i]);
			}
		}
		Assets assets = Assets.open(jar);
		McFont font = new McFont(assets);
		Lang lang = new Lang(assets, LANG);
		Sheet sheet = Files.exists(STYLE) ? Sheet.parse(Files.readString(STYLE, StandardCharsets.UTF_8)) : new Sheet();
		Stage stage = new Stage(assets, font, lang, sheet);
		long now = System.currentTimeMillis();
		stage.replay(now - 10_000);
		if (dump) {
			dumpStyle(stage);
			return;
		}
		if (shot != null) {
			System.setProperty("java.awt.headless", "true");
			stage.select(view == -2 ? stage.view : view < 0 ? stage.tables.size() : view, now - 10_000);
			stage.state.tab = tab;
			stage.state.expanded = expand;
			BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			stage.render(g, scale, w, h, mx, my, now);
			if (select != null || outline) {
				paintOverlay(g, stage, scale, outline, select);
			}
			g.dispose();
			ImageIO.write(image, "png", Path.of(shot).toFile());
			System.out.println("Wrote " + shot + " (" + w + "x" + h + " at scale " + scale + ")");
			return;
		}
		boolean smoke = smokeTest;
		String windowShot = windowShotPath;
		darkMode();
		SwingUtilities.invokeLater(() -> {
			Lab lab = new Lab(stage);
			if (smoke || windowShot != null) {
				Timer timer = new Timer(1500, e -> {
					if (smoke) {
						lab.smoke();
					} else {
						lab.windowShot(windowShot);
					}
				});
				timer.setRepeats(false);
				timer.start();
			}
		});
	}

	/** Selects a stat label, hovers a row and paints the whole window to {@code path}, then exits. */
	private void windowShot(String path) {
		frameOnce();
		Ctx.Part label = find("stat.label");
		Ctx.Part row = null;
		for (Ctx.Part p : stage.ctx.parts) {
			if (p.kind().equals("row") && row == null && p.ref() != null && p.ref().contains("heading")) {
				row = p;
			}
		}
		designer.select(label);
		if (row != null) {
			designer.pointAt((int) row.x0() - 3, (int) (row.y0() + row.y1()) / 2);
		}
		BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		frame.getRootPane().paint(g);
		g.dispose();
		try {
			ImageIO.write(image, "png", Path.of(path).toFile());
			System.out.println("Wrote " + path);
			System.exit(0);
		} catch (IOException e) {
			System.out.println("Could not write " + path + ": " + e);
			System.exit(1);
		}
	}

	/** Nimbus with a near-black palette and the lab's orange as its focus colour, set before any component exists. */
	private static void darkMode() {
		Color ground = new Color(0x1E1E1E);
		Color control = new Color(0x2B2B2B);
		Color text = new Color(0xE0E0E0);
		Color accent = new Color(0xFF9A1F);
		UIManager.put("control", control);
		UIManager.put("info", control);
		UIManager.put("nimbusBase", new Color(0x242424));
		UIManager.put("nimbusBlueGrey", new Color(0x3A3A3A));
		UIManager.put("nimbusLightBackground", ground);
		UIManager.put("nimbusFocus", accent);
		UIManager.put("nimbusSelectionBackground", new Color(0x6B4A1A));
		UIManager.put("nimbusSelectedText", Color.WHITE);
		UIManager.put("nimbusDisabledText", new Color(0x777777));
		UIManager.put("nimbusOrange", accent);
		UIManager.put("text", text);
		UIManager.put("Label.foreground", text);
		UIManager.put("TextArea.foreground", text);
		UIManager.put("TextField.foreground", text);
		UIManager.put("ComboBox.foreground", text);
		UIManager.put("Button.foreground", text);
		UIManager.put("ToggleButton.foreground", text);
		UIManager.put("CheckBox.foreground", text);
		UIManager.put("MenuItem.foreground", text);
		UIManager.put("Menu.foreground", text);
		UIManager.put("CheckBoxMenuItem.foreground", text);
		UIManager.put("TabbedPane.foreground", text);
		UIManager.put("Spinner.foreground", text);
		UIManager.put("Panel.background", control);
		UIManager.put("TextArea.background", ground);
		UIManager.put("TextField.background", ground);
		UIManager.put("SplitPane.background", control);
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if (info.getName().equals("Nimbus")) {
					UIManager.setLookAndFeel(info.getClassName());
				}
			}
		} catch (Exception e) {
			System.err.println("Dark mode unavailable: " + e);
		}
	}

	/** Renders every view with the mouse swept across it, so every property the UI can ask for is registered, then saves them. */
	private static void dumpStyle(Stage stage) throws IOException {
		int w = 427;
		int h = 240;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		long now = System.currentTimeMillis();
		List<String> expansions = List.of("", "cold_edge");
		for (int view = 0; view <= stage.tables.size(); view++) {
			stage.select(view, now - 10_000);
			for (String expansion : expansions) {
				for (int tab = 0; tab < (view == stage.tables.size() ? 4 : 1); tab++) {
					stage.state.tab = tab;
					stage.state.expanded = expansion.isEmpty() ? null : expansion;
					for (int y = 0; y < h; y += 4) {
						for (int x = 0; x < w; x += 4) {
							Graphics2D g = image.createGraphics();
							stage.render(g, 1, w, h, x, y, now);
							g.dispose();
						}
					}
				}
			}
		}
		Files.createDirectories(STYLE.getParent());
		Files.writeString(STYLE, stage.sheet.toJson(), StandardCharsets.UTF_8);
		System.out.println("Wrote " + STYLE + " with " + stage.sheet.entries().size() + " properties");
	}

	// ---- Window ----

	private void buildFrame() {
		frame.setTitle("Trim Enchantments UI Lab");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				if (unsaved() && JOptionPane.showConfirmDialog(frame, "Quit without saving?", "Unsaved changes", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
					return;
				}
				System.exit(0);
			}
		});

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		for (Scenarios.Smithing s : stage.tables) {
			views.addItem("Smithing table: " + s.name());
		}
		for (Rich tab : stage.scenarios.tabLabels()) {
			views.addItem("Field Guide: " + tab.plain());
		}
		views.setMaximumRowCount(16);
		views.setSelectedIndex(stage.selected());
		views.addActionListener(e -> {
			if (!syncing) {
				designer.commitEdit();
				stage.select(views.getSelectedIndex(), System.currentTimeMillis());
				designer.clearSelection();
			}
		});
		JComboBox<String> scales = new JComboBox<>(new String[] {"GUI scale 1", "GUI scale 2", "GUI scale 3", "GUI scale 4"});
		scales.setSelectedIndex(designer.scale - 1);
		scales.addActionListener(e -> designer.scale = scales.getSelectedIndex() + 1);
		ButtonGroup modes = new ButtonGroup();
		modes.add(design);
		modes.add(play);
		design.setToolTipText("Select, drag, reorder and edit what is on the screen");
		play.setToolTipText("Click through the screen as in game: open trims, switch tabs, the < and ? buttons");
		design.addActionListener(e -> setPlay(false));
		play.addActionListener(e -> setPlay(true));
		JToggleButton every = new JToggleButton("Every one alike", true);
		JToggleButton one = new JToggleButton("Only this one");
		ButtonGroup scope = new ButtonGroup();
		scope.add(every);
		scope.add(one);
		String scopeTip = "What dragging a part, the arrow keys and Del change: every part with the same name (all stat labels), "
			+ "or only the one you picked, in its row. Hold Alt while you grab to flip it for one drag.";
		every.setToolTipText(scopeTip);
		one.setToolTipText(scopeTip);
		every.addActionListener(e -> designer.only = false);
		one.addActionListener(e -> designer.only = true);
		removed.setToolTipText("Everything removed, to bring back; and putting rows back in their order");
		removed.addActionListener(e -> removedMenu());
		JButton properties = new JButton("Properties");
		properties.setToolTipText("Every number and colour of the selected part, and every property by name");
		properties.addActionListener(e -> {
			inspector.setVisible(true);
			inspector.refresh();
		});
		JButton undo = new JButton("Undo");
		undo.setToolTipText("Ctrl+Z");
		undo.addActionListener(e -> undo());
		JButton redo = new JButton("Redo");
		redo.setToolTipText("Ctrl+Y");
		redo.addActionListener(e -> redo());
		JButton save = new JButton("Save  (Ctrl+S)");
		save.addActionListener(e -> save());
		JButton reload = new JButton("Reload files");
		reload.addActionListener(e -> reload());
		JCheckBox table = new JCheckBox("Vanilla table", true);
		table.addActionListener(e -> stage.showTable = table.isSelected());
		JButton replay = new JButton("Replay animation");
		replay.addActionListener(e -> stage.replay(System.currentTimeMillis()));
		for (JToggleButton toggle : List.of(design, play, every, one)) {
			String label = toggle.getText();
			Runnable mark = () -> {
				toggle.setText(toggle.isSelected() ? "● " + label : label);
				toggle.setForeground(toggle.isSelected() ? PICK : new Color(0xE0E0E0));
			};
			toggle.addItemListener(e -> mark.run());
			mark.run();
		}
		top.add(views);
		top.add(scales);
		top.add(gap());
		top.add(design);
		top.add(play);
		top.add(gap());
		top.add(new JLabel("Changes:"));
		top.add(every);
		top.add(one);
		top.add(gap());
		top.add(undo);
		top.add(redo);
		top.add(save);
		top.add(reload);

		JPanel insert = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel insertLabel = new JLabel("Insert:");
		insertLabel.setToolTipText("Drag a block onto the screen between two rows, or click it to add it under the selected row");
		insert.add(insertLabel);
		for (String type : Layout.TYPES) {
			insert.add(insertButton(type));
		}
		insert.add(gap());
		insert.add(removed);
		insert.add(properties);
		insert.add(table);
		insert.add(replay);

		JPanel bars = new JPanel(new GridLayout(2, 1));
		bars.add(top);
		bars.add(insert);
		JPanel foot = new JPanel(new BorderLayout());
		foot.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
		foot.add(status, BorderLayout.CENTER);
		frame.add(bars, BorderLayout.NORTH);
		frame.add(designer, BorderLayout.CENTER);
		frame.add(foot, BorderLayout.SOUTH);
		frame.setSize(1500, 900);
		frame.setLocationRelativeTo(null);
		key(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, this::save);
		key(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, this::undo);
		key(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK, this::redo);
		key(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, this::redo);
		key(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK, () -> setPlay(!designer.play));
		key(KeyEvent.VK_ESCAPE, 0, () -> stage.closeGuide(System.currentTimeMillis()));
		new Timer(16, e -> tick()).start();
		frame.setVisible(true);
		designer.requestFocusInWindow();
	}

	private static JComponent gap() {
		JLabel l = new JLabel("  ");
		return l;
	}

	private void key(int code, int modifiers, Runnable action) {
		frame.getRootPane().registerKeyboardAction(e -> action.run(), KeyStroke.getKeyStroke(code, modifiers), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/** A block of the Insert bar: drag it onto the screen, or click it to add under the selection. */
	private JButton insertButton(String type) {
		JButton b = new JButton(Edits.title(type));
		b.setMargin(new Insets(2, 8, 2, 8));
		b.setToolTipText("Drag onto the screen between two rows, or click to add under the selected row");
		MouseAdapter drag = new MouseAdapter() {
			private Point start;
			private boolean dragging;

			@Override
			public void mousePressed(MouseEvent e) {
				start = e.getPoint();
				dragging = false;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (!dragging && start != null && start.distance(e.getPoint()) > 4) {
					dragging = true;
					setPlay(false);
					designer.beginInsert(type);
				}
				if (dragging) {
					designer.insertMove(SwingUtilities.convertPoint(b, e.getPoint(), designer));
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (dragging) {
					designer.insertEnd(SwingUtilities.convertPoint(b, e.getPoint(), designer), false);
				} else if (b.contains(e.getPoint())) {
					setPlay(false);
					designer.beginInsert(type);
					designer.insertEnd(null, true);
				}
				dragging = false;
				start = null;
			}
		};
		b.addMouseListener(drag);
		b.addMouseMotionListener(drag);
		return b;
	}

	private void setPlay(boolean on) {
		designer.commitEdit();
		designer.play = on;
		(on ? play : design).setSelected(true);
		if (on) {
			designer.clearSelection();
		}
		designer.requestFocusInWindow();
	}

	private void tick() {
		designer.repaint();
		frame.setTitle("Trim Enchantments UI Lab" + (unsaved() ? " *" : ""));
		boolean fresh = message != null && System.currentTimeMillis() - messageAt < MESSAGE_MS;
		status.setText(fresh ? message : designer.hint());
		status.setForeground(fresh ? PICK : new Color(0xB0B0B0));
		int count = edits.removals().size();
		removed.setText("Removed (" + count + ")");
		if (views.getSelectedIndex() != stage.selected()) {
			syncing = true;
			views.setSelectedIndex(stage.selected());
			syncing = false;
		}
	}

	private void refresh() {
		inspector.refresh();
		designer.repaint();
	}

	private void removedMenu() {
		JPopupMenu m = new JPopupMenu();
		List<String> keys = edits.removals();
		if (keys.isEmpty()) {
			JMenuItem none = new JMenuItem("Nothing removed");
			none.setEnabled(false);
			m.add(none);
		}
		for (String key : keys) {
			JMenuItem item = new JMenuItem("Bring back " + Edits.describe(key));
			item.addActionListener(e -> {
				edits.restore(key);
				message("Brought back " + Edits.describe(key) + ".");
			});
			m.add(item);
		}
		if (keys.size() > 1) {
			JMenuItem all = new JMenuItem("Bring everything back");
			all.addActionListener(e -> {
				history.group(() -> keys.forEach(edits::restore));
				message("Brought back " + keys.size() + " things.");
			});
			m.addSeparator();
			m.add(all);
		}
		m.addSeparator();
		JMenuItem order = new JMenuItem("Put this view's rows back in their built-in order");
		order.addActionListener(e -> {
			edits.unmoveAll(stage.viewName());
			message("Rows of " + stage.viewName() + " are back in the order the code gives them. Added rows stay where they are.");
		});
		m.add(order);
		m.show(removed, 0, removed.getHeight());
	}

	private boolean unsaved() {
		return stage.sheet.revision() != savedRevision || stage.lang.isDirty();
	}

	private void message(String text) {
		message = text;
		messageAt = System.currentTimeMillis();
	}

	private void undo() {
		if (designer.editing()) {
			return;
		}
		message(history.undo() ? "Undone. " + history.undoable() + " more to undo, " + history.redoable() + " to redo (Ctrl+Y)." : "Nothing to undo.");
		refresh();
	}

	private void redo() {
		if (designer.editing()) {
			return;
		}
		message(history.redo() ? "Redone. " + history.redoable() + " more to redo." : "Nothing to redo.");
		refresh();
	}

	private void save() {
		designer.commitEdit();
		try {
			Files.createDirectories(STYLE.getParent());
			Files.writeString(STYLE, stage.sheet.toJson(), StandardCharsets.UTF_8);
			int lines = stage.lang.save();
			savedRevision = stage.sheet.revision();
			message("Saved style.json and " + lines + " lang line" + (lines == 1 ? "" : "s") + ". Build the mod to ship it.");
		} catch (IOException e) {
			message("Could not save: " + e.getMessage());
		}
	}

	private void reload() {
		designer.commitEdit();
		try {
			Sheet fresh = Files.exists(STYLE) ? Sheet.parse(Files.readString(STYLE, StandardCharsets.UTF_8)) : new Sheet();
			stage.sheet.load(fresh);
			stage.lang.reload();
			history.clear();
			savedRevision = stage.sheet.revision();
			designer.clearSelection();
			message("Reloaded from disk.");
			refresh();
		} catch (IOException | RuntimeException e) {
			message("Could not reload: " + e.getMessage());
		}
	}

	// ---- Smoke test ----

	/** Draws one frame off screen, so the parts and rows reflect the sheet. */
	private void frameOnce() {
		BufferedImage image = new BufferedImage(designer.getWidth(), designer.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		stage.render(g, designer.scale, image.getWidth(), image.getHeight(), -100, -100, System.currentTimeMillis());
		g.dispose();
	}

	private Ctx.Part find(String id) {
		for (Ctx.Part p : stage.ctx.parts) {
			if (p.id().equals(id) && !p.removed()) {
				return p;
			}
		}
		return null;
	}

	private List<String> refs() {
		List<String> out = new ArrayList<>();
		for (Layout.Placed p : edits.order()) {
			out.add(p.ref());
		}
		return out;
	}

	/** Moves, reorders, removes, adds, copies and deletes in memory, undoes it all and redoes a step; exits non-zero if anything is off. */
	private void smoke() {
		List<String> failures = new ArrayList<>();
		frameOnce();
		int keys = stage.sheet.keys("").size();
		List<String> start = refs();
		Ctx.Part label = find("stat.label");
		if (label == null || start.size() < 3) {
			System.out.println("Smoke test FAILED: nothing to edit in view " + stage.viewName());
			System.exit(1);
		}
		float x0 = label.x0();
		edits.nudge(label, false, 2, 0, false);
		frameOnce();
		check(failures, "move everywhere", find("stat.label") != null && Math.abs(find("stat.label").x0() - x0 - 2) < 0.01F);
		edits.nudge(find("stat.label"), true, 0, 3, false);
		check(failures, "move one", stage.sheet.raw("stat.label@" + label.ref() + ".dy") != null);

		Layout.Placed second = edits.order().get(1);
		edits.moveRows(List.of(second), "^");
		frameOnce();
		check(failures, "move row to top", refs().getFirst().equals(second.ref()) && refs().size() == start.size());
		edits.step(edits.placed(second.ref()), 1);
		frameOnce();
		check(failures, "step row down", refs().equals(start));

		edits.remove(find("stat.label"), false);
		frameOnce();
		check(failures, "remove part", find("stat.label") == null);
		Ctx.Part row = null;
		for (Ctx.Part p : stage.ctx.parts) {
			if (p.kind().equals("row") && p.ref().equals(start.get(2))) {
				row = p;
			}
		}
		edits.remove(row, true);
		frameOnce();
		check(failures, "remove row", !refs().contains(start.get(2)) && refs().size() == start.size() - 1);

		String added = edits.addRow(stage.viewName(), Edits.anchorOf(start.getFirst()), "heading");
		frameOnce();
		check(failures, "add row", refs().indexOf(stage.viewName() + "/" + added) == 1);
		String copy = edits.duplicate(added);
		frameOnce();
		check(failures, "duplicate", refs().indexOf(stage.viewName() + "/" + copy) == 2);
		edits.moveRows(edits.group(edits.placed(stage.viewName() + "/" + added)), "^");
		frameOnce();
		check(failures, "move section", refs().getFirst().endsWith("/" + added));
		edits.deleteAdded(copy);
		frameOnce();
		check(failures, "delete added", !refs().contains(stage.viewName() + "/" + copy));

		while (history.undo()) {
			// All the way back.
		}
		frameOnce();
		check(failures, "undo all", stage.sheet.keys("").size() == keys && refs().equals(start) && find("stat.label") != null && Math.abs(find("stat.label").x0() - x0) < 0.01F);
		history.redo();
		frameOnce();
		check(failures, "redo", Math.abs(find("stat.label").x0() - x0 - 2) < 0.01F);
		boolean ok = failures.isEmpty();
		System.out.println("Smoke test " + (ok ? "ok" : "FAILED: " + failures) + " (" + stage.ctx.parts.size() + " parts, " + start.size() + " rows in " + stage.viewName() + ")");
		System.exit(ok ? 0 : 1);
	}

	private static void check(List<String> failures, String what, boolean ok) {
		if (!ok) {
			failures.add(what);
		}
	}

	// ---- Shots ----

	/** Draws the outline of every part and the selection, in device pixels over the finished frame, for {@code --shot}. */
	private static void paintOverlay(Graphics2D g, Stage stage, int scale, boolean all, String selected) {
		g.setClip(null);
		g.setTransform(new java.awt.geom.AffineTransform());
		g.setStroke(new BasicStroke(1.0F));
		for (Ctx.Part p : stage.ctx.parts) {
			if (p.removed()) {
				continue;
			}
			if (all && !p.kind().equals("row")) {
				g.setColor(FAINT);
				outline(g, p, scale, 0);
			}
			if (p.id().equals(selected)) {
				g.setColor(PICK);
				outline(g, p, scale, 1);
			}
		}
	}

	private static void outline(Graphics2D g, Ctx.Part p, int scale, int grow) {
		int x = Math.round(p.x0() * scale) - grow;
		int y = Math.round(p.y0() * scale) - grow;
		int w = Math.round((p.x1() - p.x0()) * scale) + 2 * grow;
		int h = Math.round((p.y1() - p.y0()) * scale) + 2 * grow;
		if (p.clip() != null) {
			g.setClip(p.clip()[0] * scale, p.clip()[1] * scale, (p.clip()[2] - p.clip()[0]) * scale, (p.clip()[3] - p.clip()[1]) * scale);
		}
		g.drawRect(x, y, Math.max(1, w - 1), Math.max(1, h - 1));
		g.setClip(null);
	}
}
