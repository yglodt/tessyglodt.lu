#!/usr/bin/env python3
"""
One-off clean-up of page.content: apostrophes, quotation marks, dashes, dots and spaces, HTML
entities, and the leftovers of texts pasted from Word, LibreOffice and older editors.

Reads all pages from the database and changes nothing there. It writes:

  target/punctuation-report.html   every changed paragraph before/after, and the cases to check by hand
  target/punctuation.sql           the updates, to run with psql once the report looks right
  target/punctuation-undo.sql      puts the previous texts back

Quotation marks become „…“ and, inside another quote, ‚…‘. They are only converted in paragraphs
where every mark pairs up; the other paragraphs are listed in the report. Entities are written as
the characters they stand for ("&eacute;" → "é"), except &amp; &lt; &gt; &nbsp; &shy;. Of the
tags, only the leftovers listed under "leftovers" go; the text inside them stays.

Connection: PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD (defaults: the local dev database).
"""
import difflib, hashlib, html, os, re, sys
from collections import Counter
from pathlib import Path
import psycopg2

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "target"

# --- HTML in and out ------------------------------------------------------------------------

TAG = re.compile(r"<!--.*?-->|<(/?)([a-zA-Z][a-zA-Z0-9:]*)[^>]*>|<[!?][^>]*>", re.S)
ENTITY = re.compile(r"&(?:#\d+|#[xX][0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")
INLINE = {"a", "b", "cite", "em", "font", "i", "small", "span", "strong", "sub", "sup", "u"}
SKIPPED = {"iframe", "script", "style", "xml"}
# The only characters still written as entities; everything else is written as itself
KEPT = {"&": "&amp;", "<": "&lt;", ">": "&gt;", " ": "&nbsp;", "­": "&shy;"}

def spell(ch):
	return KEPT.get(ch, ch)

def decode(raw):
	return ENTITY.sub(lambda m: html.unescape(m.group(0)), raw)

def tokenize(content):
	"""Splits content into ("text", raw) and ("tag", raw, name, closing) tokens."""
	tokens, pos = [], 0
	for m in TAG.finditer(content):
		if m.start() > pos:
			tokens.append(("text", content[pos:m.start()]))
		tokens.append(("tag", m.group(0), (m.group(2) or "").lower(), bool(m.group(1))))
		pos = m.end()
	if pos < len(content):
		tokens.append(("text", content[pos:]))
	return tokens

def cells_of(raw, tok):
	"""One cell [character, token index] per visible character."""
	return [[ch, tok] for ch in decode(raw)]

def blocks_of(tokens):
	"""Groups the text tokens into paragraphs: inline tags don't interrupt one, all others do."""
	block, skipping = [], None
	for i, t in enumerate(tokens):
		if t[0] == "text":
			if not skipping:
				block.append(i)
			continue
		name, closing = t[2], t[3]
		if skipping:
			if closing and name == skipping:
				skipping = None
			continue
		if name in SKIPPED and not closing:
			skipping = name
		if name not in INLINE and ":" not in name:
			if block:
				yield block
			block = []
	if block:
		yield block

def text_of(cells):
	return "".join(c[0] for c in cells)

def apply(cells, edits):
	"""Applies (start, end, replacement) edits."""
	for a, b, rep in sorted(edits, reverse=True):
		tok = cells[a][1] if a < len(cells) else cells[-1][1]
		if a == b and a > 0:
			tok = cells[a - 1][1]
		cells[a:b] = [[ch, tok] for ch in rep]

# --- leftovers ------------------------------------------------------------------------------
# Tags that came in with pasted text. They go, the text inside them stays.

BROKEN = re.compile(r"""\s(?:href|src)="%\(link\d+\)\"""")	# links lost in a translation tool
P_JUNK = re.compile(r"""\s(?:lang|class)="[^"]*"|\sstyle="(?:\s*margin-[a-z]+:[^;"]*;?)*\s*\"""")

def is_open(t, name):
	return t[0] == "tag" and t[2] == name and not t[3]

def is_close(t, name):
	return t[0] == "tag" and t[2] == name and t[3]

def leftovers(tokens, res):
	"""Removes the leftovers from the token list; text inside them stays (except in <style>/<xml>)."""
	# 1. comments (Word's <!--[if gte mso 9]><xml>…), <style> and <xml> blocks, Office tags
	#    (<o:p>), <span> and <font>; <p> loses lang, class and margin styles
	out, skipping, para, touched = [], None, None, []
	for t in tokens:
		if skipping:
			if is_close(t, skipping):
				skipping = None
			continue
		if t[0] == "tag":
			name = t[2]
			if name in ("style", "xml") and not t[3]:
				skipping = name
				res.counts["styleblock"] += 1
				touched.append(para)
				continue
			if name == "" or ":" in name:
				res.counts["comment" if name == "" else "office"] += 1
				touched.append(para)
				continue
			if name in ("span", "font"):
				if not t[3]:
					res.counts[name] += 1
				continue
			if is_open(t, "p"):
				raw = P_JUNK.sub("", t[1])
				if raw != t[1]:
					res.counts["pattrs"] += 1
					t = ("tag", raw, "p", False)
				para = t
			elif is_close(t, "p"):
				para = None
		out.append(t)

	# 2. <u> around a link (Word underlined the links itself); <u> elsewhere is the author's
	tokens, out = out, []
	drop = set()
	for i, t in enumerate(tokens):
		if is_open(t, "u") and i + 1 < len(tokens) and is_open(tokens[i + 1], "a"):
			j = next((k for k in range(i + 1, len(tokens)) if is_close(tokens[k], "u")), None)
			if j is not None and is_close(tokens[j - 1], "a"):
				drop.update((i, j))
				res.counts["ulink"] += 1
	tokens = [t for i, t in enumerate(tokens) if i not in drop]

	# 3. links and images pointing to %(link0): the link goes, its text stays and is listed
	out, link = [], None
	for t in tokens:
		if is_open(t, "a") and BROKEN.search(t[1]):
			link = []
			res.counts["brokenlink"] += 1
			continue
		if link is not None:
			if is_close(t, "a"):
				res.broken.append(decode("".join(x[1] for x in link if x[0] == "text")).strip())
				link = None
				continue
			link.append(t)
		if is_open(t, "img") and BROKEN.search(t[1]):
			res.counts["brokenimg"] += 1
			continue
		out.append(t)

	# 4. paragraphs left empty by step 1 (they only held a <style> block or Word's comments)
	tokens, out, i = out, [], 0
	while i < len(tokens):
		t = tokens[i]
		if is_open(t, "p") and any(t is p for p in touched):
			j = i + 1
			while j < len(tokens) and tokens[j][0] == "text" and not tokens[j][1].strip():
				j += 1
			if j < len(tokens) and is_close(tokens[j], "p"):
				res.counts["emptied"] += 1
				i = j + 1
				continue
		out.append(t)
		i += 1
	return out

# --- rules ----------------------------------------------------------------------------------
# Each rule gets the visible text of one paragraph and returns edits.

def invisibles(s):
	return [(m.start(), m.end(), "") for m in re.finditer("[﻿‎​]", s)]

def spaces(s):
	return [(m.start(), m.end(), " ") for m in re.finditer("[  ]{2,}| ", s)]

SINGLE, DOUBLE, GUILLEMETS = "'‘’´`‚", "\"“”„", "«»"	# not ‹ ›: only used once, for a letter inside a word
OPEN_ONLY, CLOSE_ONLY = "„‚«", "»"
ELIDED = re.compile(r"(?:t|s|n|m|mol|rëm)(?![^\W\d_])(?!['‘’´`])", re.I)	# ’t, ’s, ’mol, but not a quoted letter: ‘n‘
YEAR = re.compile(r"\d\d(?![\d'‘’\"“”])")						# ’45
SPACED_T = re.compile(r" +t(?![^\W\d_])")
NO_OPEN_AFTER = ".,;:!?)]"	# an opening mark doesn't follow these
NO_OPEN_BEFORE = ".,;:!?)]"

def letter(ch):
	return ch is not None and ch.isalpha()

def word_before(s, i):
	j = i
	while j > 0 and s[j - 1].isalpha():
		j -= 1
	return s[j:i]

def word_after(s, i):
	j = i
	while j < len(s) and s[j].isalpha():
		j += 1
	return s[i:j]

class Mark:
	def __init__(self, a, b, fam, ch):
		self.a, self.b, self.fam, self.ch = a, b, fam, ch
		self.role = None		# apostrophe / open / close / comma / drop, None = leave as it is
		self.optional = False	# a comma that may be an opening mark
		self.unsure = False		# between two long words: apostrophe or closing mark
		self.partner = None
		self.space = None		# (start, end) of a misplaced space next to an apostrophe

def find_marks(s):
	marks, i, n = [], 0, len(s)
	while i < n:
		ch = s[i]
		prev = s[i - 1] if i else None
		nxt = s[i + 1] if i + 1 < n else None
		if ch in SINGLE:
			marks.append(Mark(i, i + 1, "s", ch))
		elif ch in DOUBLE:
			marks.append(Mark(i, i + 1, "d", ch))
		elif ch in GUILLEMETS:
			marks.append(Mark(i, i + 1, "g", ch))
		elif ch == "," and (prev is None or prev in " (") and i + 1 < n:
			if nxt == "," and letter(s[i + 2] if i + 2 < n else None):	# ,,Wuert
				marks.append(Mark(i, i + 2, "d", "„"))
				i += 1
			elif letter(nxt):										# ,Wuert‘ (or a misplaced comma)
				m = Mark(i, i + 1, "s", "‚")
				m.optional = True
				marks.append(m)
		i += 1
	return marks

def quotes(s):
	"""Returns (edits, problems, mixed). With problems, only the apostrophes are corrected."""
	marks = find_marks(s)
	edits, problems = [], []
	at = {m.a: m for m in marks}

	# 1. apostrophes
	def spaces_after(i):
		end = i
		while end < len(s) and s[end] == " ":
			end += 1
		return end
	for m in marks:
		if m.fam != "s" or m.optional:
			continue
		prev = s[m.a - 1] if m.a else None
		nxt = s[m.b] if m.b < len(s) else None
		before = word_before(s, m.a).lower()
		if m.ch == "‚":
			if prev is not None and (prev.isalnum() or prev == ")") and (nxt is None or nxt == " "):
				m.role = "comma"	# a low quotation mark typed for a comma
			continue
		if prev is not None and prev.isdigit() and nxt is not None and nxt.isdigit():
			m.role = "skip"		# 7’52, 5°49'59
		elif not letter(prev) and ELIDED.match(s, m.b):
			m.role = "apostrophe"	# ’t, ’mol
		elif not letter(prev) and YEAR.match(s, m.b):
			m.role = "apostrophe"	# 1944-’45
		elif before == "d":
			m.role = "apostrophe"	# d’Kierch, d’1920er, d’„Cité“, and d’ Kierch
			end = spaces_after(m.b)
			if end > m.b and end < len(s) and (s[end].isalnum() or s[end] == "(" or end in at):
				m.space = (m.b, end)
		elif before in ("z", "s", "l") and nxt == " " and letter(s[spaces_after(m.b)] if spaces_after(m.b) < len(s) else None):
			m.role = "apostrophe"	# z’ erreechen, s’ entworf, l’ église
			m.space = (m.b, spaces_after(m.b))
		elif before == "t" and nxt == " ":
			m.role = "skip"			# an t’ gouf: the apostrophe is on the wrong side
			edits.append((m.a - 1, m.b, "’" + s[m.a - 1]))
		elif letter(prev) and letter(nxt):
			m.role = "apostrophe"
			m.unsure = len(before) >= 4 and len(word_after(s, m.b)) >= 3
		elif letter(prev) and before in ("l", "z", "s") and nxt is not None and (nxt.isdigit() or m.b in at):
			m.role = "apostrophe"
		elif prev == " " and letter(nxt) and word_before(s, m.a - 1).lower() == "d":
			m.role = "apostrophe"	# d ‘Kierch
			m.space = (m.a - 1, m.a)
		elif prev in (None, " ") and nxt == " " and SPACED_T.match(s, m.b):
			m.role = "apostrophe"	# ‘ t ass
			m.space = (m.b, spaces_after(m.b))
	for m in marks:
		if m.space:
			edits.append((m.space[0], m.space[1], ""))

	# two single marks typed for a double one: ‘‘Text’’
	merged = []
	for m in marks:
		last = merged[-1] if merged else None
		if last and last.fam == "s" and m.fam == "s" and last.b == m.a and not last.role and not m.role \
				and not last.optional and last.ch != "‚" and m.ch != "‚":
			last.b, last.fam, last.ch = m.b, "d", "\""
			del at[m.a]
		else:
			merged.append(m)
	marks = merged

	# 2. pair up what is left
	stack, mixed = [], False
	def pair(opener, closer):
		opener.role, closer.role = "open", "close"
		opener.partner, closer.partner = closer, opener
	for m in marks:
		if m.role in ("comma", "skip") or (m.role == "apostrophe" and not m.unsure):
			continue
		top = stack[-1] if stack else None
		if m.unsure:
			if top and top.fam == "s" and not top.optional:
				problems.append((m.a, "apostrophe or closing mark?"))
			continue
		prev = s[m.a - 1] if m.a else None
		nxt = s[m.b] if m.b < len(s) else None
		can_open = nxt is not None and nxt != " " and nxt not in NO_OPEN_AFTER \
			and not (prev is not None and (prev.isalnum() or prev in NO_OPEN_BEFORE))
		can_close = prev is not None and prev not in " ([" and not (nxt is not None and nxt.isalnum())
		if m.ch in OPEN_ONLY:
			can_open, can_close = True, False
		elif m.ch in CLOSE_ONLY:
			can_open, can_close = False, True
		elif can_open == can_close:
			# no space on either side, or one on both: closes what is open, else opens
			if prev is not None and prev.isdigit() and not (top and top.fam == m.fam):
				m.role = "skip"
				continue
			can_close = bool(top and top.fam == m.fam)
			can_open = not can_close
		if can_close:
			while stack and stack[-1].optional and stack[-1].fam != m.fam:
				stack.pop()
			top = stack[-1] if stack else None
			if top and top.optional and m.a - top.b > 25:
				stack.pop()			# too far away: that one was a comma
				top = stack[-1] if stack else None
			if prev is not None and prev.isdigit() and not any(o.fam == m.fam for o in stack):
				m.role = "skip"		# 49'59" – minutes and seconds
			elif top:
				mixed = mixed or top.fam != m.fam	# ‘Text“ – closes whatever is open
				pair(stack.pop(), m)
			else:
				problems.append((m.a, "closing mark without an opening one"))
		else:
			stack.append(m)
	for m in stack:
		prev = at.get(m.a - 1)
		if m.optional:
			pass						# just a comma
		elif prev and prev.role == "apostrophe" and m.fam == "s":
			m.role = "drop"				# d’’Barrière
		else:
			problems.append((m.a, "opening mark without a closing one"))

	# a stray mark can pair with one far away: a long quote full of other quotes is suspect
	if not problems:
		outer, inside = None, 0
		for m in marks:
			if m.role == "open":
				if outer is None:
					outer, inside = m, 0
				elif m.partner:
					inside += 1
			elif m.role == "close" and m.partner is outer:
				length = m.a - outer.b
				if inside >= 2 or (inside and length > 500) or length > 600:
					problems.append((outer.a, "long quote with other quotes inside: does it really end where it seems to?"))
				outer = None

	for m in marks:
		if m.role == "apostrophe" and not (m.unsure and problems):
			if m.ch != "’":
				edits.append((m.a, m.b, "’"))
		elif m.role == "comma":
			edits.append((m.a, m.b, ","))
		elif m.role == "drop" and not problems:
			edits.append((m.a, m.b, ""))
	if problems:
		return edits, problems, False

	# 3. „…“ for a quote, ‚…‘ for a quote inside a quote
	depth = 0
	for m in marks:
		if m.role == "open":
			new = "„‚"[depth % 2]
			depth += 1
			end = m.b
			while end < len(s) and s[end] == " ":
				end += 1
			if s[m.a:end] != new:
				edits.append((m.a, end, new))
		elif m.role == "close":
			depth -= 1
			new = "“‘"[depth % 2]
			start = m.a
			while start > 0 and s[start - 1] == " " and start - 1 >= m.partner.b:
				start -= 1
			if s[start:m.b] != new:
				edits.append((start, m.b, new))
	return edits, problems, mixed

def dashes(s):
	return [(m.start(), m.end(), "–") for m in re.finditer(r"(?<=[^\s\-] )-(?= [^\s\-])", s)]

def dots(s):
	return [(m.start(), m.end(), "…") for m in re.finditer(r"[.…]*(?:…|\.{3})[.…]*", s) if m.group(0) != "…"]

def two_dots(s):
	"""The author's pause (Baueren.. an) becomes an ellipsis; inside an abbreviation (z..B.) it is a typo."""
	edits = []
	for m in re.finditer(r"(?<![.…])\.\.(?![.…])", s):
		abbreviation = len(word_before(s, m.start())) == 1 and letter(s[m.end()] if m.end() < len(s) else None)
		edits.append((m.start(), m.end(), "." if abbreviation else "…"))
	return edits

def brackets(s):
	return [(m.start(), m.end(), "") for m in re.finditer(r"(?<=[(\[]) +(?=\S)|(?<=\S) +(?=[)\]])", s)]

def space_before(s):
	return [(m.start(), m.end(), "") for m in re.finditer(r"(?<=[\w)…“‘’%€]) +(?=[:;!?]+(?:[\s)“‘]|$))", s)]

def comma_spacing(s):
	"""kaaft , restauréiert → kaaft, restauréiert; 1973“ .An → 1973“. An; Marber,Glas → Marber, Glas"""
	edits = []
	for m in re.finditer(r"(?<=\S)( +)([,.])(?![.,\d…])( ?)", s):
		prev = s[m.start() - 1]
		nxt = s[m.end()] if m.end() < len(s) else None
		if prev in ",(":
			continue
		mark = "" if m.group(2) == "." and prev in ".!?" else m.group(2)	# a second full stop
		after = " " if m.group(3) or not (nxt is None or nxt in ")]“‘,.;:!?") else ""
		edits.append((m.start(), m.end(), mark + after))
	for m in re.finditer(r"(?<=[^\W\d_]{3})[,.](?=[^\W\d_]{3})", s):
		start, end = m.start(), m.end()
		while start > 0 and not s[start - 1].isspace():
			start -= 1
		while end < len(s) and not s[end].isspace():
			end += 1
		word = s[start:end]
		if URL.search(word) or word[0] in "„‚" or (m.group(0) == "." and not s[m.end()].isupper()):
			continue		# a web address, a name like „Mini.Groussherzogtum“, or unclear: stays on the list
		edits.append((m.end(), m.end(), " "))
	return edits

RULES = [
	("invisible", "Invisible characters removed", invisibles),
	("spaces", "Double and non-breaking spaces", spaces),
	("quotes", "Apostrophes and quotation marks", quotes),
	("dash", "Hyphen used as a dash", dashes),
	("dots", "Three or more dots", dots),
	("twodots", "Two dots", two_dots),
	("brackets", "Space inside brackets", brackets),
	("before", "Space before : ; ! ?", space_before),
	("comma", "Spaces around commas and full stops", comma_spacing),
]

LEFTOVERS = [
	("entities", "Entities written as characters (&eacute; → é)"),
	("span", "<span> removed (lang, Word styles)"),
	("font", "<font> removed"),
	("ulink", "<u> around a link removed"),
	("pattrs", "<p> without lang, class and margins"),
	("styleblock", "<style> and <xml> blocks removed"),
	("comment", "Comments removed (Word)"),
	("office", "Office tags removed (<o:p>)"),
	("emptied", "Paragraphs left empty by that, removed"),
	("brokenimg", "Pasted editor button image removed"),
]

# Not changed, only listed
URL = re.compile(r"(?:https?://|www\.)\S+|\S+\.(?:lu|com|de|org|eu|fr|be|net|info|pdf)\b\S*", re.I)
CHECKS = [
	("nospace", "Missing space after a comma or full stop", re.compile(r"[^\W\d_]{3,}[,.][^\W\d_]{3,}")),
	("spacecomma", "Space before a comma or full stop", re.compile(r"(?<=\S) +[,.](?![.\d])")),
]

# --- processing -----------------------------------------------------------------------------

class Change:
	def __init__(self, before, after, rules):
		self.before, self.after, self.rules = before, after, rules

class Result:
	def __init__(self):
		self.changes, self.unpaired, self.checks, self.broken = [], [], [], []
		self.counts = Counter()

# Empty paragraphs before the text: they also keep the drop cap off the first real paragraph
LEADING = re.compile(r"\s*(?:<p[^>]*>(?:\s|&nbsp;|<br ?/?>)*</p>\s*)+")

def process(content, res):
	content = "".join(t[1] for t in leftovers(tokenize(content), res))
	m = LEADING.match(content)
	if m:
		content = content[m.end():]
		res.counts["leading"] += 1
		res.changes.append(Change("¶ (empty paragraph before the text)", "", ["leading"]))
	tokens = tokenize(content)
	out = [t[1] for t in tokens]
	for block in blocks_of(tokens):
		for i in block:
			res.counts["entities"] += sum(1 for m in ENTITY.finditer(tokens[i][1])
				if any(spell(ch) != m.group(0) for ch in html.unescape(m.group(0))))
		cells = [c for i in block for c in cells_of(tokens[i][1], i)]
		write(out, block, cells)
		before = text_of(cells)
		if not before.strip("  \r\n\t"):
			continue			# empty paragraphs (<p>&nbsp;</p>) stay as they are
		fired = []
		for key, _, rule in RULES:
			s = text_of(cells)
			if rule is quotes:
				edits, problems, mixed = quotes(s)
				if problems:
					res.unpaired.append((s, problems))
					res.counts["unpaired"] += 1
				if mixed:
					fired.append("mixed")
					res.counts["mixed"] += 1
			else:
				edits = rule(s)
			if edits:
				apply(cells, edits)
				fired.append(key)
				res.counts[key] += len(edits)
		after = text_of(cells)
		for key, _, pattern in CHECKS:
			for m in pattern.finditer(after):
				if key == "nospace" and any(u.start() <= m.start() < u.end() for u in URL.finditer(after)):
					continue
				res.checks.append((key, after, m.start(), m.end()))
				res.counts[key] += 1
		if after != before:
			res.changes.append(Change(before, after, fired))
			write(out, block, cells)
	return "".join(out)

def write(out, block, cells):
	"""Writes the paragraph's characters back into its text tokens."""
	for i in block:
		out[i] = ""
	for ch, tok in cells:
		out[tok] += spell(ch)

# Tags the leftovers step may remove or change; all others must come out exactly as they went in
REMOVABLE = {"", "a", "font", "img", "p", "span", "style", "u", "xml"}

def check(old, new):
	"""Safety net: every letter and digit, and every tag that isn't a leftover, must come out as it went in."""
	a, b = tokenize(old), tokenize(new)
	kept = lambda tokens: [t[1] for t in tokens if t[0] == "tag" and t[2] not in REMOVABLE and ":" not in t[2]]
	if kept(a) != kept(b):
		return False
	# links, images and <u> may only disappear, not change; a link or image only if it was broken
	tags = lambda tokens: Counter(t[1] for t in tokens if t[0] == "tag" and t[2] in ("a", "img", "u"))
	old_tags, new_tags = tags(a), tags(b)
	if new_tags - old_tags:
		return False
	for raw in old_tags - new_tags:
		if raw.startswith(("<a", "<img")) and not BROKEN.search(raw):
			return False
	letters = lambda tokens: [ch for block in blocks_of(tokens) for i in block for ch in decode(tokens[i][1]) if ch.isalnum()]
	return letters(a) == letters(b)

# --- output ---------------------------------------------------------------------------------

def diff_html(before, after):
	a, b = [], []
	for op, i1, i2, j1, j2 in difflib.SequenceMatcher(None, before, after, autojunk=False).get_opcodes():
		old, new = html.escape(before[i1:i2]), html.escape(after[j1:j2])
		if op == "equal":
			a.append(old)
			b.append(new)
		else:
			if old:
				a.append("<del>" + old.replace(" ", "·").replace(" ", "·") + "</del>")
			if new:
				b.append("<ins>" + new + "</ins>")
			elif old:
				b.append("<ins class=gone></ins>")
	return "".join(a), "".join(b)

def excerpt(s, a, b, width=70):
	start, end = max(0, a - width), min(len(s), b + width)
	return ("…" if start else "") + html.escape(s[start:a]) + "<mark>" + html.escape(s[a:b]) + "</mark>" \
		+ html.escape(s[b:end]) + ("…" if end < len(s) else "")

def marked(s, problems):
	"""The paragraph with every quotation mark highlighted, the unpaired ones in red."""
	bad = {pos for pos, _ in problems}
	out = []
	for i, ch in enumerate(s):
		if i in bad:
			out.append("<mark class=bad>" + html.escape(ch) + "</mark>")
		elif ch in SINGLE + DOUBLE + GUILLEMETS:
			out.append("<mark>" + html.escape(ch) + "</mark>")
		else:
			out.append(html.escape(ch))
	return "".join(out)

CSS = """
body { font: 15px/1.5 Georgia, serif; max-width: 62em; margin: 2em auto; padding: 0 1em; color: #222; }
h1 { font-size: 1.6em; } h2 { font-size: 1.25em; margin-top: 2em; border-bottom: 1px solid #ccc; }
h3 { font-size: 1em; margin: 1.6em 0 .3em; } h3 a { color: #8a1f1f; }
table { border-collapse: collapse; } td, th { padding: .15em .8em .15em 0; text-align: left; } td.n { text-align: right; }
.c { margin: .4em 0; padding: .4em .6em; background: #f6f3ee; border-left: 3px solid #ccc; }
.c p { margin: 0; } .c p + p { margin-top: .3em; }
.why { font: 12px sans-serif; color: #8a1f1f; }
del { background: #f9c9c9; text-decoration: none; } ins { background: #bfe6bf; text-decoration: none; }
ins.gone { border-left: 2px solid #2a7a2a; }
mark { background: #ffe58a; } mark.bad { background: #e0443a; color: #fff; }
.filter { position: sticky; top: 0; background: #fff; padding: .5em 0; border-bottom: 1px solid #ccc; font: 13px sans-serif; }
.filter label { margin-right: 1em; white-space: nowrap; }
"""

JS = """
const boxes = document.querySelectorAll('.filter input');
function update() {
	const on = new Set([...boxes].filter(b => b.checked).map(b => b.value));
	document.querySelectorAll('section .c').forEach(c => {
		c.hidden = !c.dataset.rules.split(' ').some(r => on.has(r));
	});
	document.querySelectorAll('section').forEach(s => { s.hidden = !s.querySelector('.c:not([hidden])'); });
}
boxes.forEach(b => b.addEventListener('change', update));
"""

def write_report(path, pages, total):
	w = []
	w.append("<!doctype html><html lang=lb><meta charset=utf-8><title>Punctuation clean-up</title><style>%s</style>" % CSS)
	w.append("<h1>Punctuation clean-up: what would change</h1>")
	changed = sum(1 for p in pages if p[3] != p[2])
	w.append("<p>%d of %d texts would change. Nothing has been changed yet.</p>" % (changed, len(pages)))
	w.append("<table><tr><th>Corrected automatically<th>Changes</tr>")
	for key, label, _ in RULES:
		w.append("<tr><td>%s<td class=n>%d</tr>" % (label, total[key]))
	w.append("<tr><td>Empty paragraph before the text removed<td class=n>%d</tr>" % total["leading"])
	w.append("<tr><th>HTML (no visible change)<th></tr>")
	for key, label in LEFTOVERS:
		w.append("<tr><td>%s<td class=n>%d</tr>" % (html.escape(label), total[key]))
	w.append("<tr><td><a href='#broken'>Dead links removed, their text kept</a><td class=n>%d</tr>" % total["brokenlink"])
	w.append("<tr><th>To check by hand (not changed)<th></tr>")
	w.append("<tr><td><a href='#unpaired'>Paragraphs whose quotation marks don’t pair up</a><td class=n>%d</tr>" % total["unpaired"])
	for key, label, _ in CHECKS:
		w.append("<tr><td><a href='#%s'>%s</a><td class=n>%d</tr>" % (key, label, total[key]))
	w.append("</table>")

	w.append("<h2 id=broken>Dead links</h2>")
	w.append("<p>These links pointed to <code>%(link0)</code>, <code>%(link1)</code>… (the addresses were lost in a "
		"translation tool), so they led nowhere. The link goes, the text stays. Link them again by hand if needed.</p>")
	for name, title, _, _, res in pages:
		if res.broken:
			w.append("<h3><a href='http://localhost:8017/page/%s'>%s</a></h3>" % (html.escape(name), html.escape(title)))
			w.append("<div class=c><p>%s</p></div>" % " · ".join(html.escape(s) or "<i>(no text)</i>" for s in res.broken))

	w.append("<h2 id=unpaired>Quotation marks that don’t pair up</h2>")
	w.append("<p>These paragraphs keep their quotation marks as they are (apostrophes are still corrected). "
		"The mark that doesn’t fit is red.</p>")
	for name, title, _, _, res in pages:
		if res.unpaired:
			w.append("<h3><a href='http://localhost:8017/page/%s'>%s</a></h3>" % (html.escape(name), html.escape(title)))
			for s, problems in res.unpaired:
				why = "; ".join(sorted({reason for _, reason in problems}))
				w.append("<div class=c><p>%s</p><p class=why>%s</p></div>" % (marked(s, problems), why))

	for key, label, _ in CHECKS:
		w.append("<h2 id=%s>%s</h2>" % (key, label))
		for name, title, _, _, res in pages:
			found = [c for c in res.checks if c[0] == key]
			if found:
				w.append("<h3><a href='http://localhost:8017/page/%s'>%s</a></h3>" % (html.escape(name), html.escape(title)))
				for _, s, a, b in found:
					w.append("<div class=c><p>%s</p></div>" % excerpt(s, a, b))

	w.append("<h2 id=changes>All changes, text by text</h2>")
	w.append("<p class=filter>Show paragraphs with: ")
	for key, label, _ in RULES:
		w.append("<label><input type=checkbox value=%s checked> %s</label>" % (key, label))
	w.append("<label><input type=checkbox value=leading checked> Empty paragraph before the text</label>")
	w.append("<label><input type=checkbox value=mixed> Pairs of two different marks, like ‘…“ (%d)</label>" % total["mixed"])
	w.append("</p>")
	for name, title, _, _, res in pages:
		if res.changes:
			w.append("<section><h3><a href='http://localhost:8017/page/%s'>%s</a></h3>" % (html.escape(name), html.escape(title)))
			for c in res.changes:
				a, b = diff_html(c.before, c.after)
				w.append("<div class=c data-rules='%s'><p>%s</p><p>%s</p></div>" % (" ".join(c.rules), a, b))
			w.append("</section>")
	w.append("<script>%s</script>" % JS)
	path.write_text("\n".join(w), encoding="utf-8")

def write_sql(path, rows):
	"""rows: (id, text to expect in the database, text to write)."""
	quote = "$punctuation$"
	w = ["\\set ON_ERROR_STOP on", "begin;", "create temp table punctuation (id varchar(32), expected text, content text) on commit drop;"]
	for id, expected, content in rows:
		assert quote not in content
		w.append("insert into punctuation values ('%s', '%s', %s%s%s);" % (id.replace("'", "''"),
			hashlib.md5(expected.encode("utf-8")).hexdigest(), quote, content, quote))
	w.append("update page p set content = x.content from punctuation x where p.id = x.id and md5(p.content) = x.expected;")
	w.append("select p.name as \"NOT updated: the text in this database is different\" from page p join punctuation x on x.id = p.id "
		"where p.content <> x.content order by 1;")
	w.append("commit;")
	with open(path, "w", encoding="utf-8", newline="") as f:	# the texts contain CRLF, keep it
		f.write("\n".join(w) + "\n")

def main():
	env = os.environ.get
	conn = psycopg2.connect(host=env("PGHOST", "localhost"), port=env("PGPORT", "5417"), dbname=env("PGDATABASE", "tessyglodt"),
		user=env("PGUSER", "tessyglodt"), password=env("PGPASSWORD", "tessyglodt"))
	cur = conn.cursor()
	cur.execute("select id, name, title, content from page where content is not null order by lower(title), name")
	pages, total, updates, undo = [], Counter(), [], []
	for id, name, title, content in cur.fetchall():
		res = Result()
		new = process(content, res)
		if not check(content, new):
			sys.exit("%s: more than punctuation would change, stopping" % name)
		pages.append((name, title, content, new, res))
		total.update(res.counts)
		if new != content:
			updates.append((id, content, new))
			undo.append((id, new, content))
	conn.close()

	OUT.mkdir(exist_ok=True)
	write_report(OUT / "punctuation-report.html", pages, total)
	write_sql(OUT / "punctuation.sql", updates)
	write_sql(OUT / "punctuation-undo.sql", undo)

	print("%d texts, %d would change" % (len(pages), len(updates)))
	for key, label, _ in RULES:
		print("  %-45s %6d" % (label, total[key]))
	print("  %-45s %6d" % ("Empty paragraph before the text removed", total["leading"]))
	print("HTML:")
	for key, label in LEFTOVERS:
		print("  %-45s %6d" % (label, total[key]))
	print("  %-45s %6d" % ("Dead links removed, their text kept", total["brokenlink"]))
	print("To check by hand:")
	print("  %-45s %6d" % ("Paragraphs with unpaired quotation marks", total["unpaired"]))
	for key, label, _ in CHECKS:
		print("  %-45s %6d" % (label, total[key]))
	print("Report: %s" % (OUT / "punctuation-report.html"))

if __name__ == "__main__":
	main()
