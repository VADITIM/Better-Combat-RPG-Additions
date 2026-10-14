package dev.vadim.bcenchantments.lab;

import dev.vadim.bcenchantments.ui.Ctx;
import dev.vadim.bcenchantments.ui.Palette;
import dev.vadim.bcenchantments.ui.Sheet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The properties window: every number, colour and switch of the selected part by name, and every property
 * in the sheet with a filter. Optional; the canvas and its right-click menu cover the everyday edits.
 */
final class Inspector extends JDialog {
	private final Stage stage;
	private final History history;
	private final Supplier<Ctx.Part> selection;
	private final JPanel selected = column();
	private final JPanel all = column();
	private final JTextField filter = new JTextField();
	private final JTabbedPane tabs = new JTabbedPane();

	Inspector(JFrame owner, Stage stage, History history, Supplier<Ctx.Part> selection) {
		super(owner, "Properties", false);
		this.stage = stage;
		this.history = history;
		this.selection = selection;
		JPanel allTab = new JPanel(new BorderLayout());
		allTab.add(filter, BorderLayout.NORTH);
		allTab.add(new JScrollPane(all), BorderLayout.CENTER);
		filter.setToolTipText("Filter by key, such as stat.label or palette");
		filter.getDocument().addDocumentListener(listener(this::rebuildAll));
		tabs.addTab("Selected", new JScrollPane(selected));
		tabs.addTab("Every property", allTab);
		tabs.addChangeListener(e -> refresh());
		add(tabs, BorderLayout.CENTER);
		setSize(420, 640);
		setLocation(owner.getX() + owner.getWidth() - 440, owner.getY() + 90);
	}

	private static DocumentListener listener(Runnable r) {
		return new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				r.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				r.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				r.run();
			}
		};
	}

	private static JPanel column() {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		return p;
	}

	private static JComponent note(String text) {
		JTextArea area = new JTextArea(text);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
		area.setOpaque(false);
		area.setAlignmentX(Component.LEFT_ALIGNMENT);
		area.setMaximumSize(new Dimension(Integer.MAX_VALUE, area.getPreferredSize().height * 6));
		return area;
	}

	private static JLabel heading(String text) {
		JLabel l = new JLabel(text);
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		l.setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	/** Builds the open tab again from the selection and the sheet; does nothing while hidden. */
	void refresh() {
		if (!isVisible()) {
			return;
		}
		if (tabs.getSelectedIndex() == 1) {
			rebuildAll();
		} else {
			rebuildSelected();
		}
	}

	private void rebuildSelected() {
		selected.removeAll();
		Ctx.Part part = selection.get();
		if (part == null) {
			selected.add(note("Select something on the screen to list its properties here."));
		} else {
			boolean row = part.kind().equals("row");
			JLabel title = new JLabel(row ? "row " + Edits.anchorOf(part.ref()) : part.id());
			title.setFont(title.getFont().deriveFont(Font.BOLD, 15.0F));
			title.setAlignmentX(Component.LEFT_ALIGNMENT);
			selected.add(title);
			selected.add(note(row ? "Space above and below every " + part.id().substring(4) + " row." : "These apply to every " + Edits.target(part) + "."));
			List<String> ids = row ? List.of(part.id()) : Edits.styleIds(part);
			for (String id : ids) {
				List<Sheet.Entry> props = stage.sheet.of(id);
				if (!props.isEmpty()) {
					if (ids.size() > 1) {
						selected.add(heading(id));
					}
					selected.add(grid(props, false));
				}
			}
			if (part.ref() != null && !row) {
				List<String> own = new ArrayList<>();
				for (String key : stage.sheet.keys(Edits.target(part) + "@" + part.ref())) {
					own.add(key);
				}
				if (!own.isEmpty()) {
					selected.add(heading("In this row only"));
					for (String key : own) {
						selected.add(note(key.substring(key.lastIndexOf('.') + 1) + " = " + stage.sheet.raw(key)));
					}
				}
			}
		}
		selected.add(Box.createVerticalGlue());
		selected.revalidate();
		selected.repaint();
	}

	private JPanel grid(List<Sheet.Entry> entries, boolean fullKeys) {
		JPanel grid = new JPanel(new GridBagLayout());
		grid.setAlignmentX(Component.LEFT_ALIGNMENT);
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.insets = new Insets(2, 0, 2, 4);
		c.anchor = GridBagConstraints.WEST;
		for (Sheet.Entry entry : entries) {
			String key = entry.key;
			String name = fullKeys ? key : key.substring(key.lastIndexOf('.') + 1);
			JLabel label = new JLabel(name);
			label.setToolTipText(key + "  (default " + entry.def + ")");
			JButton reset = new JButton("↺");
			reset.setMargin(new Insets(0, 4, 0, 4));
			reset.setToolTipText("Back to the default, " + entry.def);
			reset.setEnabled(entry.isSet());
			JComponent editor = editor(entry, reset);
			reset.addActionListener(e -> {
				history.set(key, null);
				refresh();
			});
			c.gridx = 0;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			grid.add(label, c);
			c.gridx = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			grid.add(editor, c);
			c.gridx = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			grid.add(reset, c);
			c.gridy++;
		}
		return grid;
	}

	private JComponent editor(Sheet.Entry entry, JButton reset) {
		String key = entry.key;
		switch (entry.type) {
			case Sheet.INT -> {
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt(entry.value().split("\\.")[0]), -999, 999, 1));
				spinner.addChangeListener(e -> {
					history.set(key, String.valueOf(spinner.getValue()), true);
					reset.setEnabled(true);
				});
				return spinner;
			}
			case Sheet.FLOAT -> {
				JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.parseDouble(entry.value()), -100.0, 100.0, 0.05));
				spinner.addChangeListener(e -> {
					history.set(key, String.format(Locale.ROOT, "%.3f", ((Number) spinner.getValue()).doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", ""), true);
					reset.setEnabled(true);
				});
				return spinner;
			}
			case Sheet.BOOL -> {
				JCheckBox box = new JCheckBox("", Boolean.parseBoolean(entry.value()));
				box.addActionListener(e -> {
					history.set(key, String.valueOf(box.isSelected()));
					reset.setEnabled(true);
				});
				return box;
			}
			default -> {
				return colorEditor(entry, reset);
			}
		}
	}

	private JComponent colorEditor(Sheet.Entry entry, JButton reset) {
		String key = entry.key;
		JPanel panel = new JPanel(new BorderLayout(3, 0));
		JTextField text = new JTextField(entry.value());
		JButton swatch = new JButton();
		swatch.setPreferredSize(new Dimension(26, 22));
		JComboBox<String> palette = new JComboBox<>();
		palette.addItem("palette");
		for (String name : Palette.defaults().keySet()) {
			palette.addItem("$" + name);
		}
		Runnable paint = () -> swatch.setBackground(new Color(stage.sheet.resolve(text.getText()) & 0xFFFFFF));
		paint.run();
		Runnable apply = () -> {
			String value = text.getText().trim();
			if (value.startsWith("$") || value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
				history.set(key, value, true);
				reset.setEnabled(true);
				paint.run();
			}
		};
		text.getDocument().addDocumentListener(listener(apply));
		swatch.addActionListener(e -> {
			Color picked = JColorChooser.showDialog(this, key, new Color(stage.sheet.resolve(text.getText()) & 0xFFFFFF));
			if (picked != null) {
				history.group(() -> text.setText(Sheet.hex(picked.getRGB())));
			}
		});
		palette.addActionListener(e -> {
			if (palette.getSelectedIndex() > 0) {
				String value = (String) palette.getSelectedItem();
				palette.setSelectedIndex(0);
				history.group(() -> text.setText(value));
			}
		});
		JPanel right = new JPanel(new BorderLayout(3, 0));
		right.add(swatch, BorderLayout.WEST);
		right.add(palette, BorderLayout.CENTER);
		panel.add(text, BorderLayout.CENTER);
		panel.add(right, BorderLayout.EAST);
		return panel;
	}

	/** Lists every property asked for so far that matches the filter. */
	private void rebuildAll() {
		all.removeAll();
		String needle = filter.getText().trim().toLowerCase(Locale.ROOT);
		List<Sheet.Entry> shown = new ArrayList<>();
		for (Sheet.Entry e : stage.sheet.entries()) {
			if (needle.isEmpty() || e.key.toLowerCase(Locale.ROOT).contains(needle)) {
				shown.add(e);
			}
		}
		if (shown.size() > 120) {
			all.add(note(shown.size() + " properties: type in the box above to narrow them down, such as \"stat\" or \"palette\"."));
			shown = shown.subList(0, Math.min(shown.size(), needle.isEmpty() ? 0 : 120));
		}
		all.add(grid(shown, true));
		all.revalidate();
		all.repaint();
	}
}
