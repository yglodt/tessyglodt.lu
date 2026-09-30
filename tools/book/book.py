"""Builds a PDF book of all published tessyglodt.lu pages with ReportLab:
cover, clickable two-column index with page numbers, PDF outline, one article per page start,
and the About and Author pages at the end.

Reads the pages from the local database (PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD, defaults
to the dev DB on port 5433) and the about/author text from the Thymeleaf templates.

	tools/book/make-book.sh [output.pdf]   # default: target/kierchtuermspromenaden.pdf

make-book.sh creates the Python venv and installs requirements.txt on first run.
"""
import os, re, sys, tempfile, unicodedata, urllib.request
from html import escape
from html.parser import HTMLParser
from pathlib import Path
import psycopg2
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, Table, TableStyle, Flowable, Frame, HRFlowable, NextPageTemplate,
	PageBreak, PageTemplate, Paragraph, Spacer)

WORK = Path(__file__).resolve().parent
REPO = WORK.parents[1]
OUT = Path(sys.argv[1] if len(sys.argv) > 1 else REPO / "target" / "kierchtuermspromenaden.pdf").resolve()
OUT.parent.mkdir(parents=True, exist_ok=True)

# ReportLab needs TTF files: Lora comes as static TTFs (the site only has the regular weight),
# the display fonts are converted from the site's own woff2 files. Both are cached in fonts/.
F = WORK / "fonts"
F.mkdir(exist_ok=True)
for style in ("Regular", "Italic", "Bold", "BoldItalic"):
	target = F / f"Lora-{style}.ttf"
	if not target.exists():
		urllib.request.urlretrieve(f"https://github.com/cyrealtype/Lora-Cyrillic/raw/main/fonts/ttf/Lora-{style}.ttf", target)
for name in ("smythe-latin", "italianno-latin"):
	target = F / f"{name}.ttf"
	if not target.exists():
		from fontTools.ttLib import TTFont as FTFont
		font = FTFont(REPO / "src/main/resources/static/fonts" / f"{name}.woff2")
		font.flavor = None
		font.save(target)
for name, file in [("Lora", "Lora-Regular"), ("Lora-I", "Lora-Italic"), ("Lora-B", "Lora-Bold"),
		("Lora-BI", "Lora-BoldItalic"), ("Smythe", "smythe-latin"), ("Italianno", "italianno-latin")]:
	pdfmetrics.registerFont(TTFont(name, str(F / f"{file}.ttf")))
pdfmetrics.registerFontFamily("Lora", normal="Lora", italic="Lora-I", bold="Lora-B", boldItalic="Lora-BI")

GREY = HexColor("#777777")
INK = HexColor("#222222")

# --- data -----------------------------------------------------------------------------------

env = os.environ.get
conn = psycopg2.connect(host=env("PGHOST", "localhost"), port=env("PGPORT", "5433"), dbname=env("PGDATABASE", "tessyglodt"),
	user=env("PGUSER", "tessyglodt"), password=env("PGPASSWORD", "tessyglodt"))
cur = conn.cursor()
cur.execute("""
	select p.title, p.content, m.name, c.name, d.name, p.date_published
	from page p
	left join municipality m on m.id = p.municipality
	left join canton c on c.id = m.canton
	left join district d on d.id = c.district
	where p.published""")
rows = cur.fetchall()

def fold(s):
	return unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()

rows.sort(key=lambda r: (fold(r[0]), r[2] or ""))

# --- styles ---------------------------------------------------------------------------------

BODY = ParagraphStyle("body", fontName="Lora", fontSize=10.5, leading=15.5, alignment=TA_JUSTIFY,
	textColor=INK, spaceAfter=5, allowWidows=0, allowOrphans=0)
SUBHEAD = ParagraphStyle("subhead", parent=BODY, fontName="Lora-B", alignment=0, spaceBefore=6, spaceAfter=3,
	keepWithNext=1)
TITLE = ParagraphStyle("title", fontName="Smythe", fontSize=30, leading=34, textColor=INK)
LOC = ParagraphStyle("loc", fontName="Lora", fontSize=9.5, leading=13, textColor=GREY, spaceBefore=3)
INDEX_HEAD = ParagraphStyle("indexhead", fontName="Smythe", fontSize=30, leading=34)
INDEX = ParagraphStyle("index", fontName="Lora", fontSize=8.6, leading=11, textColor=INK)
COVER = ParagraphStyle("cover", fontName="Italianno", fontSize=64, leading=70, alignment=TA_CENTER)
COVER_SUB = ParagraphStyle("coversub", fontName="Smythe", fontSize=22, leading=26, alignment=TA_CENTER)
LIST = ParagraphStyle("list", parent=BODY, fontSize=9.5, leading=12.5, alignment=0, spaceAfter=1.5,
	leftIndent=9, bulletIndent=0, bulletFontName="Lora")
SIGNATURE = ParagraphStyle("signature", fontName="Italianno", fontSize=30, leading=36, leftIndent=12 * mm,
	spaceBefore=4 * mm)

# --- HTML (CKEditor content) to paragraphs --------------------------------------------------

class ContentParser(HTMLParser):
	"""Turns the page HTML into (style, reportlab-markup) blocks. Keeps em/strong/u/sup/links,
	drops spans/fonts/Word-paste junk, iframes and images."""
	INLINE = {"em": "i", "i": "i", "cite": "i", "strong": "b", "b": "b", "u": "u", "sup": "super"}
	BLOCK = {"p", "h1", "h2", "h3", "h4", "h5", "div", "li", "blockquote"}
	SKIP = {"style", "xml", "script", "iframe", "title"}

	def __init__(self):
		super().__init__(convert_charrefs=True)
		self.blocks, self.buf, self.stack, self.skip, self.style = [], [], [], 0, BODY

	def flush(self):
		for tag in reversed(self.stack):
			self.buf.append(f"</{tag}>")
		text = re.sub(r"\s+", " ", "".join(self.buf)).strip()
		text = re.sub(r"^(<br/>\s*)+|(\s*<br/>)+$", "", text)
		if re.sub(r"<[^>]+>", "", text).strip():
			self.blocks.append((self.style, text))
		self.buf = [f"<{t}>" for t in self.stack]
		self.style = BODY

	def handle_starttag(self, tag, attrs):
		if tag in self.SKIP:
			self.skip += 1
		elif self.skip:
			return
		elif tag in self.BLOCK:
			self.flush()
			if tag.startswith("h"):
				self.style = SUBHEAD
			elif tag == "li":
				self.style = LIST
			elif "italianno" in (dict(attrs).get("style") or "").lower():
				self.style = SIGNATURE
		elif tag == "br":
			self.buf.append("<br/>")
		elif tag in self.INLINE:
			self.open(self.INLINE[tag])
		elif tag == "a":
			href = dict(attrs).get("href") or ""
			if href.startswith(("http://", "https://", "mailto:")):
				self.open(f'a href="{escape(href)}" color="#444444"', "a")

	def open(self, markup, tag=None):
		self.buf.append(f"<{markup}>")
		self.stack.append(tag or markup)

	def handle_endtag(self, tag):
		if tag in self.SKIP:
			self.skip = max(0, self.skip - 1)
		elif self.skip:
			return
		elif tag in self.BLOCK:
			self.flush()
		else:
			rl = "a" if tag == "a" else self.INLINE.get(tag)
			if rl and rl in self.stack:
				while self.stack:
					t = self.stack.pop()
					self.buf.append(f"</{t}>")
					if t == rl:
						break

	def handle_data(self, data):
		if not self.skip:
			self.buf.append(escape(data, quote=False))

def parse_blocks(content):
	p = ContentParser()
	p.feed(content or "")
	p.close()
	p.stack = []
	p.flush()
	return p.blocks

def content_flowables(content):
	return [Paragraph(text, style) for style, text in parse_blocks(content)]

# --- custom flowables -----------------------------------------------------------------------

class ArticleStart(Flowable):
	"""Zero-size marker: records the article's page, bookmark and outline entry."""
	def __init__(self, i, title, letter):
		super().__init__()
		self.i, self.title, self.letter = i, title, letter
	def wrap(self, aw, ah):
		return 0, 0
	def draw(self):
		c = self.canv
		key = f"a{self.i}"
		c.bookmarkPage(key)
		if self.letter is False:
			c.addOutlineEntry(self.title, key, level=0)
		elif self.letter:
			c.addOutlineEntry(self.letter, key, level=0, closed=True)
		if self.letter is not False:
			c.addOutlineEntry(self.title, key, level=1)
		doc = c._doctemplate
		if isinstance(self.i, int):
			doc.pages[self.i] = c.getPageNumber()
		doc.current = (self.title, c.getPageNumber())

class IndexEntry(Flowable):
	"""One index line: title (may wrap), dotted leader, right-aligned page number, all clickable."""
	NUM_W = 9 * mm
	def __init__(self, i, text, page):
		super().__init__()
		self.i, self.page = i, page
		self.para = Paragraph(text, ParagraphStyle("ie", parent=INDEX, rightIndent=self.NUM_W))
	def wrap(self, aw, ah):
		self.aw = aw
		self.w, self.h = self.para.wrap(aw, ah)
		return aw, self.h + 1.2
	def draw(self):
		c = self.canv
		self.para.drawOn(c, 0, 1.2)
		line = self.para.blPara.lines[-1]
		extra = line[0] if isinstance(line, tuple) else line.extraSpace
		x0 = self.aw - self.NUM_W - extra + 2
		x1 = self.aw - pdfmetrics.stringWidth(self.page, "Lora", INDEX.fontSize) - 2
		c.setFont("Lora", INDEX.fontSize)
		c.setFillColor(GREY)
		dot = pdfmetrics.stringWidth(". ", "Lora", INDEX.fontSize)
		x = x0 + (dot - (x0 % dot))  # align dots across lines
		while x + dot <= x1:
			c.drawString(x, 1.2 + 2.2, ".")
			x += dot
		c.setFillColor(INK)
		c.drawRightString(self.aw, 1.2 + 2.2, self.page)
		c.linkRect("", f"a{self.i}", (0, 0, self.aw, self.h + 1.2), relative=1, thickness=0)

# --- document -------------------------------------------------------------------------------

W, H = A4
M_IN, M_OUT, M_TOP, M_BOT = 22 * mm, 22 * mm, 22 * mm, 24 * mm

class Book(BaseDocTemplate):
	def __init__(self, path):
		super().__init__(str(path), pagesize=A4, title="Kierchtuermspromenaden", author="tessyglodt.lu",
			leftMargin=M_IN, rightMargin=M_OUT, topMargin=M_TOP, bottomMargin=M_BOT)
		fw, fh = W - M_IN - M_OUT, H - M_TOP - M_BOT
		gap = 8 * mm
		cw = (fw - gap) / 2
		head = 18 * mm
		self.addPageTemplates([
			PageTemplate("cover", [Frame(M_IN, M_BOT, fw, fh, id="c")]),
			PageTemplate("index", [Frame(M_IN, M_BOT + fh - head, fw, head, id="ih", leftPadding=0, bottomPadding=0),
				Frame(M_IN, M_BOT, cw, fh - head, id="l1"), Frame(M_IN + cw + gap, M_BOT, cw, fh - head, id="r1")],
				onPageEnd=self.footer),
			PageTemplate("index2", [Frame(M_IN, M_BOT, cw, fh, id="l"), Frame(M_IN + cw + gap, M_BOT, cw, fh, id="r")],
				onPageEnd=self.footer),
			PageTemplate("article", [Frame(M_IN, M_BOT, fw, fh, id="a")], onPageEnd=self.article_page),
		])
		self.pages, self.current = {}, None

	def footer(self, c, doc):
		c.saveState()
		c.setFont("Lora", 8.5)
		c.setFillColor(GREY)
		c.drawCentredString(W / 2, 13 * mm, str(c.getPageNumber()))
		c.restoreState()

	def article_page(self, c, doc):
		self.footer(c, doc)
		if self.current and self.current[1] != c.getPageNumber():
			c.saveState()
			c.setFont("Lora-I", 8.5)
			c.setFillColor(GREY)
			c.drawCentredString(W / 2, H - 13 * mm, self.current[0])
			c.setStrokeColor(HexColor("#cccccc"))
			c.setLineWidth(0.4)
			c.line(M_IN, H - 15.5 * mm, W - M_OUT, H - 15.5 * mm)
			c.restoreState()

def meta(r):
	items = [("Gemeng", r[2]), ("Kanton", r[3]), ("Distrikt", r[4]),
		("Publizéiert den", r[5].strftime("%d.%m.%Y") if r[5] else None)]
	return " · ".join(f'{label}: <font color="#444444">{escape(value)}</font>' for label, value in items if value)

TEMPLATES = REPO / "src/main/resources/templates"

def extra_page(template):
	"""The about/author page: first h4 becomes the title, list items go into two columns."""
	html_src = (TEMPLATES / template).read_text(encoding="utf-8")
	article = re.search(r"<article[^>]*>(.*?)</article>", html_src, re.S).group(1)
	blocks = parse_blocks(article)
	title = re.sub(r"<[^>]+>", "", blocks.pop(0)[1])
	flow, items = [], []
	def flush_list():
		if items:
			half = (len(items) + 1) // 2
			cells = [Paragraph(t, LIST, bulletText="•") for t in items]
			left, right = cells[:half], cells[half:] + [""] * (half - len(cells[half:]))
			t = Table(list(zip(left, right)), colWidths=["50%", "50%"], hAlign="LEFT")
			t.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 0),
				("RIGHTPADDING", (0, 0), (-1, -1), 6), ("TOPPADDING", (0, 0), (-1, -1), 0),
				("BOTTOMPADDING", (0, 0), (-1, -1), 1.5)]))
			flow.append(t)
			flow.append(Spacer(1, 4))
			items.clear()
	for style, text in blocks:
		if style is LIST:
			items.append(text)
			continue
		flush_list()
		flow.append(Paragraph(text, style))
	flush_list()
	return title, flow

def story(pages):
	s = [Spacer(1, 70 * mm), Paragraph("Kierchtuermspromenaden", COVER), Spacer(1, 8 * mm),
		Paragraph('<a href="https://www.tessyglodt.lu">www.tessyglodt.lu</a>', COVER_SUB),
		NextPageTemplate("index"), PageBreak(),
		Paragraph("Index", INDEX_HEAD), NextPageTemplate("index2")]
	for i, r in enumerate(rows):
		text = escape(r[0]) + (f' <font color="#777777" size="7.6">({escape(r[2])})</font>' if r[2] and r[2] != r[0] else "")
		s.append(IndexEntry(i, text, str(pages.get(i, "000"))))
	s.append(NextPageTemplate("article"))
	letter = None
	for i, r in enumerate(rows):
		first = fold(r[0])[:1].upper()
		new_letter = first if first != letter else None
		letter = first
		s += [PageBreak(), ArticleStart(i, r[0], new_letter), Paragraph(escape(r[0]), TITLE),
			Paragraph(meta(r), LOC), HRFlowable(width="100%", thickness=0.5, color=HexColor("#cccccc"),
				spaceBefore=3 * mm, spaceAfter=6 * mm)]
		s += content_flowables(r[1])
	for key, template in (("apropos", "about.html"), ("auteur", "author.html")):
		title, flow = extra_page(template)
		s += [PageBreak(), ArticleStart(key, title, False), Paragraph(escape(title), TITLE),
			HRFlowable(width="100%", thickness=0.5, color=HexColor("#cccccc"), spaceBefore=3 * mm, spaceAfter=6 * mm)]
		s += flow
	return s

def build(path, pages):
	doc = Book(path)
	doc.build(story(pages))
	return doc.pages

# Pass 1 finds each article's page, pass 2 prints those numbers in the index
with tempfile.TemporaryDirectory() as tmp:
	pass1 = build(Path(tmp) / "pass1.pdf", {})
assert len(pass1) == len(rows), f"placed {len(pass1)} of {len(rows)} articles"
final = build(OUT, pass1)
assert final == pass1, "page numbers shifted between passes"
print(f"{OUT}: {len(rows)} articles")
