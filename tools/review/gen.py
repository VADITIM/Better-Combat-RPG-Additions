import re, html, json, sys

src = open(sys.argv[1], encoding="utf-8").read().replace("\r\n", "\n").split("\n")

def inline(s):
    s = s.replace("\\*", "\u0001")
    s = html.escape(s, quote=False)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", s)
    s = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2" target="_blank" rel="noopener">\1</a>', s)
    return s.replace("\u0001", "*")

def clean_heading(s):
    return re.sub(r"^[^\w]+", "", s).strip()

def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")

out = []
section = ""
sub = ""
total = 0
i = 0
while i < len(src):
    l = src[i]
    if l.startswith("# "):
        i += 1; continue
    if l.startswith("## "):
        section = clean_heading(l[3:]); sub = ""
        out.append(f'<h2>{inline(section)}</h2>'); i += 1; continue
    if l.startswith("### "):
        sub = clean_heading(l[4:])
        out.append(f'<h3>{inline(sub)}</h3>'); i += 1; continue
    if re.fullmatch(r"\*\*[^*]+\*\*", l.strip()):
        grp = l.strip()[2:-2]
        out.append(f'<h4>{inline(grp)}</h4>'); i += 1; continue
    if l.startswith("```"):
        j = i + 1; buf = []
        while not src[j].startswith("```"):
            buf.append(src[j]); j += 1
        out.append(f'<pre><code>{html.escape(chr(10).join(buf))}</code></pre>'); i = j + 1; continue
    if l.startswith("|"):
        rows = []
        while i < len(src) and src[i].startswith("|"):
            rows.append([c.strip() for c in src[i].strip().strip("|").split("|")]); i += 1
        head, body = rows[0], rows[2:]
        planned = sub == "Planned"
        draft_power = section == "All Trim Powers" and sub == "" and head[0] == "Power"
        if planned or draft_power:
            items = []
            for r in body:
                name = r[0].replace("\\*", "")
                is_draft = planned or r[0].endswith("\\*")
                if not is_draft:
                    continue
                key = ("power:" if draft_power else "combo:") + slug(name)
                total += 1
                rest = r[1:]
                meta = rest[0]
                eff = " · ".join(f"<span class='lbl'>{html.escape(head[k+1])}</span> {inline(v)}" if k > 0 and len(rest) > 2 else inline(v) for k, v in enumerate(rest[1:], 1)) if False else None
                if draft_power:
                    body_html = f"<p class='eff'>{inline(rest[1])}</p><p class='num'>{inline(rest[2])}</p>"
                    meta_html = f"trim: {inline(meta)}"
                else:
                    body_html = f"<p class='eff'>{inline(rest[1])}</p>"
                    meta_html = inline(meta)
                items.append(
                    f'<div class="item" data-key="{key}" data-name="{html.escape(name)}">'
                    f'<div class="head"><span class="name">{html.escape(name)}</span><span class="meta">{meta_html}</span></div>'
                    f'{body_html}'
                    f'<div class="rv"><button type="button" data-s="good">Good</button>'
                    f'<button type="button" data-s="adjust">Needs adjustment</button>'
                    f'<input type="text" class="note" id="n-{slug(key)}" placeholder="What should change?" aria-label="Note for {html.escape(name)}" hidden></div></div>')
            # non-draft rows of a power table stay a plain table
            plain = [r for r in body if draft_power and not r[0].endswith("\\*")]
            if plain:
                th = "".join(f"<th>{inline(h)}</th>" for h in head)
                tb = "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in plain)
                out.append(f'<div class="tw"><table><thead><tr>{th}</tr></thead><tbody>{tb}</tbody></table></div>')
            out.append('<div class="items">' + "".join(items) + '</div>')
        else:
            th = "".join(f"<th>{inline(h)}</th>" for h in head)
            tb = "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in body)
            out.append(f'<div class="tw"><table><thead><tr>{th}</tr></thead><tbody>{tb}</tbody></table></div>')
        continue
    if l.strip() == "---" or l.strip() == "":
        i += 1; continue
    # paragraph
    buf = [l]; i += 1
    while i < len(src) and src[i].strip() and not re.match(r"^(#|\||```|---|\*\*[^*]+\*\*$)", src[i]):
        buf.append(src[i]); i += 1
    out.append(f'<p>{inline(" ".join(buf))}</p>')

template = open(sys.argv[2], encoding="utf-8").read()
page = template.replace("%%CONTENT%%", "\n".join(out)).replace("%%TOTAL%%", str(total))
open(sys.argv[3], "w", encoding="utf-8").write(page)
print("items", total)
