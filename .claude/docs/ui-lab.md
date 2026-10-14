# UI lab

A designer for the smithing panel and the Field Guide, run without Minecraft: select, drag, reorder, reword, recolour, add and remove right on the screen.

Run it from the project folder:

```bash
./gradlew uiLab
```

It draws the mod's own `ui` code with Minecraft's font and textures, read from the client jar in Loom's cache (so run `./gradlew build` once first). The vanilla smithing table, its `<` and `?` buttons and the dimmed background are drawn too.

## Using it

The window is the screen plus two toolbars; the status line at the bottom always says what you can do next.

- **Pick a view** in the first box: seven smithing table setups (empty, refining, fully refined, a power waiting, a blocked upgrade, a shield, a lesser weapon) or one of the Field Guide's four tabs.
- **Design / Play** (Ctrl+P): Design edits; Play clicks through it as in game (open trims, switch tabs, `<`, `?`, Esc).

In Design:

- **Click** anything to select it; the hover tag names what is under the mouse. Click the selection again (or Esc) to select the row around it.
- **Drag a part** (label, icon, box) to move it. It snaps to the edges and centres of other parts (cyan line); hold Ctrl to place it freely. Arrow keys nudge by 1, Shift+arrows by 5.
- **Drag a row** by its grip (the dots at its left edge when you hover it), its empty space, or anything in it with Shift held. An orange line shows where it will land. A heading or title takes its section with it (the rows under it up to the next one); hold Alt as you grab it to move it alone. Alt+↑ / Alt+↓ move the selected row one place.
- **Changes: Every one alike / Only this one** decides what a part move, a nudge and Del change: every part with that name (all stat labels, dashed outlines), or only the one picked, in its row. Alt flips it for one drag.
- **Double-click text** (or F2 / Enter) to reword it in place: the mod's lang line (`%s` and `%d` are filled in by the game) or an added row's text. Enter keeps it, Esc cancels. Minecraft's own and game-filled text can't be changed here.
- **Right-click** for everything else: colours (palette or any colour), text size (whole multiples only), shadow, opacity, slot size, an added row's type, text colour, alignment and height, *Insert row above / below*, *Move row up / down*, *Duplicate*, *Put back in place*, *Reset look*, and remove.
- **Insert bar:** drag Label, Heading, Title, Stat, Icon, Icons, Entry, Rule or Space onto the screen between two rows, or click one to add it under the selected row. A new row opens for typing at once. Ctrl+D copies a row you added.
- **Del** removes what is selected: a row, or a part (everywhere or only in its row, by the Changes switch; the right-click menu offers both). A removed row is gone and the rows close up; a removed part is no longer drawn. Deleting an added row deletes it outright.
- **Removed (n)** lists everything removed to bring it back, and *Put this view's rows back in their built-in order* undoes every row move in the view.
- **Properties** opens a window with every number, colour and switch of the selected part by name, and every property in the sheet with a filter: the spacing values the canvas has no handle for.
- **Ctrl+Z / Ctrl+Y** (or Ctrl+Shift+Z) undo and redo every edit; a drag, a reword or a reorder is one step. *Reload files* clears the history.
- **Ctrl+S** writes `src/main/resources/assets/bcenchantments/ui/style.json` and the changed lines of `lang/en_us.json`. Build the mod to ship them.

## Layout in `style.json`

Removals, moves and added rows sit next to the look, so the game reads them from the same file (`ui/Layout`):

- `"remove.<id>": true`: a part (`stat.arrow`), one part in one row (`stat.arrow@panel/stat#0`), every row of a kind (`row.stat`), or one row by its reference, `view/anchor`. The old name `hide.` still works.
- `"move.<view/anchor>": "after:<anchor>"` (or `before:`): a row moved next to another. If that one is not shown (another weapon, another state), the row stays where the code puts it.
- `"<part>@<view/anchor>.dx"` / `.dy`: one part moved in one row, on top of the part's own `dx`/`dy`.
- `"add.<n>:<field>"`: an added row. `view` is `panel` or `guide.0` to `guide.3`; `at` is the anchor it sits `after` or `before` (`place`); then `type`, `text`, `sub`, `items`, `tone`, `align`, `size`, `overlay`. Its parts are `add.<n>.text`, `add.<n>.slot` and so on, each with its own look. Added text is literal, not a lang key.
- An anchor is `kind[lang key]` for a row whose text has a key (`stat[panel.bcenchantments.stat.speed]`), `kind#i` for one without, `add.<n>` for an added row, `^` the top, `$` the bottom. A row added next to a row the game does not show at the moment is not shown either.

Rows whose number changes with the weapon (`trim#0`, `trim#1`, ...) are anchored by position, so a move or a one-row edit stays with the position, not with the trim.

## Part names

`part.property`, where the part is what the code draws: `stat.label`, `header.slot`, `icon.title.muted`, `power_button.box`, `tab.selected`, `panel.box`. `row.<kind>.top` / `.bottom` add space around a whole row. A `recipes` row (the Field Guide's emerald and emerald block recipes) puts its recipes side by side, centred, `recipes.gap` apart. Colours are `#RRGGBB` or `$NAME` from the palette (`palette.*`). Names and defaults are in `ui/Rows.java`, `SmithingView` and `GuideView`.

## Adding to the UI

- A new row kind: a record in `ui/Rows` with its parts drawn through `Ctx` (`text`, `box`, `fill`, `slot`), then use it from `SmithingPanel` or `GuideScreen` and add a sample to `lab/Scenarios`. It can be selected, moved, reordered and removed in the lab with no lab code.
- New parts show up in the lab on their own. Run `./gradlew uiLab "-PlabArgs=--dump-style"` to rewrite `style.json` with every property (it sweeps the mouse over every view so hover-only parts register).
- The lab's samples are built by hand from the same rows and the real lang; the mod decides which rows a real weapon gets. If you change that logic in `SmithingPanel`, update the sample in `Scenarios` if the picture should change.

## Command line

```bash
./gradlew uiLab "-PlabArgs=--shot out.png --view 2 --scale 2 --size 854x480 --mouse 150,120"
```

Renders one frame and exits: `--view` 0-6 is a smithing setup, `-1` the Field Guide (`--tab 0-3`); `--expand cold_edge` opens a trim row; `--outline` and `--select <part>` draw outlines. `--jar <path>` uses another client jar.

`--window-shot out.png` opens the whole lab, selects a stat label, hovers a row's grip and saves the window as an image.

`--smoke` opens the window and, in memory (nothing is saved), moves a part everywhere and in one row, moves rows and a section, removes a part and a row, adds, copies and deletes rows, undoes everything back to the start and redoes a step; it exits non-zero if anything is off.

## Limits

- Items without a flat sprite in the jar (the shield) show a placeholder; the glint is a purple tint, not the animation.
- The HUD (combo count, health bars, difficulty) is not in the lab.
- `style.json` is read once at game start; there is no in-game hot reload.
- A part moves inside its row; rows keep the height the code gives them, so a part moved past its row's edge overlaps the next row.
