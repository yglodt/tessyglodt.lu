# Clean-up of the page texts

A one-off clean-up of `page.content`, in one pass:

- **punctuation:** apostrophes, quotation marks, dashes, dots and spaces;
- **entities:** written as the characters they stand for (`&eacute;` → `é`, `&rsquo;` → `’`);
- **leftovers** of texts pasted from Word, LibreOffice and older editors (`<span lang>`, `<font>`,
  `<style>` blocks, Word comments, `class="western"`, dead `%(link0)` links).

No word and no spelling changes, and apart from the leftovers no tag and no attribute is touched.

## Status (28 October 2026)

**Applied** to the local database and to production on 28 October (562 texts, none skipped).
Checksum over all texts (`select md5(string_agg(content, '' order by id)) from page`):
`f2353f5e…` before, `941dfe56…` after, the same in both. CKEditor now saves characters instead of
entities (`config.entities = false` in `static/ckeditor/config.js`). Still open: whether to do the
page titles. New texts can bring the old problems back; the script can be run again.

## Decisions taken

- **Quotation marks:** German style, „…“ and ‚…‘ for a quote inside a quote. Apostrophe: ’.
- **Paragraphs whose quotation marks don't pair up** (482) keep their quotation marks as written.
  They are not fixed by hand. Everything else is still corrected in them.
- **Two dots** (`Baueren.. an`) become `…`, whatever follows. A single full stop before a lowercase
  letter was considered and dropped, because the sentence continues there (`gewielt.. well en`).
  Inside an abbreviation the second dot is a typo: `z..B.` → `z.B.`
- **Spacing around commas and full stops** is fixed automatically.
- **Empty paragraphs before the text** (`<p>&nbsp;</p>`) are removed. Empty paragraphs further
  down stay.
- **Page titles are not touched** (47 have ` - `, two have `d'`). Still open whether to do them.
- **Entities:** only `&amp; &lt; &gt; &nbsp; &shy;` stay. Search needs this: `to_tsvector` splits
  `L&euml;tzebuerg` into `l`, `euml`, `tzebuerg`. In a trial run (rolled back) a search for
  `Lëtzebuerg` went from 217 to 376 pages, `Musée` from 47 to 84, `Ëmgéigend` from 21 to 47.
- **Dead links** (`<a href="%(link0)">`, the addresses were lost in a translation tool) go, their
  text stays. The report lists them per page, to link again by hand if needed.
- **`<u>`** goes only where Word put it around a link; elsewhere it is the author's (`<u>ie</u>`).
- **Kept:** `<cite>` (pasted from Google results, but visible as italics), `<i>`, `<b>`, `<h4>`,
  the two Facebook `<iframe>`s, and `<p style="text-align: center;">`.

## How to run

```bash
tools/punctuation/fix-punctuation.sh
```

This reads the local database (same `PG*` environment variables and defaults as `tools/book`) and
**never writes to it**. It writes three files to `target/`:

| File | What it is |
|---|---|
| `punctuation-report.html` | Counts, the cases left alone, and every changed paragraph before/after, with a filter per rule |
| `punctuation.sql` | The updates |
| `punctuation-undo.sql` | Puts the previous texts back |

Applying, once the report looks right:

```bash
psql -d tessyglodt -f target/punctuation.sql
```

- Each update only applies if the text in the database is still identical to the one the script
  read (compared by md5). Texts that differ are skipped and listed at the end, so the file made
  from the local copy is safe to run on production.
- `date_modified` is not changed, so the sitemap and the feeds don't report 562 updated texts.
- The app caches the texts: restart it after the update.
- The SQL files contain all texts. They are in `target/`, which is ignored by git; keep them out of
  the repository.

## What it changes

Counts from the dry run of 27 October 2026, all 562 texts (the punctuation counts are the same as
on 3 October):

| Rule | Changes | Example |
|---|---|---|
| Apostrophes and quotation marks | 22,623 | `d'Ansembourg` → `d’Ansembourg`, `‘Text’` and `"Text"` → `„Text“` |
| Hyphen used as a dash | 2,061 | `Fändel - an` → `Fändel – an` |
| Double and non-breaking spaces | 1,257 | |
| Three or more dots | 1,116 | `Biller...` → `Biller…` |
| Two dots | 776 | `Baueren.. an` → `Baueren… an` |
| Space inside brackets | 69 | `( 1618 )` → `(1618)` |
| Spaces around commas and full stops | 65 | `kaaft , restauréiert`, `Marber,Glas`, `„1973“ .An` |
| Empty paragraph before the text | 29 | |
| Space before : ; ! ? | 27 | `240 Joer : Eng` → `240 Joer: Eng` |
| Invisible characters | 5 | |

HTML only, no visible change:

| What | Changes |
|---|---|
| Entities written as characters | 74,257 |
| `<span>` removed (`lang="de-DE"`, Word styles) | 1,766 |
| `<p>` without `lang`, `class="western"` and `margin-bottom` | 466 |
| Dead links removed, text kept | 179 |
| `<font color="#0000ff">` and `<u>` around links | 15 + 15 |
| `<style>` blocks, Word comments (`<!--[if gte mso 9]><xml>…`) | 4 + 3 |
| Paragraphs that only held those, removed | 4 |
| A pasted FCKeditor button image (`<img src="%(link0)">`) | 1 |

The `margin-bottom` styles were the only ones with a visible effect: those paragraphs now get the
site's normal spacing.

Left alone and listed at the top of the report: the 482 unpaired paragraphs and five spacing cases
that can't be decided (`boards.ancestry`, `Mini.Groussherzogtum`, `dëst.verréit`,
`Zäitspronk.weist`, one `,,`).

## How the script works

- The HTML is split into tags and text. Rules only see the visible text of one paragraph at a
  time, with inline tags (`em`, `span`, `a`, …) made transparent, so `d'<em>Kierch</em>` is handled
  like `d'Kierch`.
- **The leftovers go first**, before the paragraphs are read, so the rules never see them.
- Every character is written as itself except `& < >`, the non-breaking space and the soft hyphen.
- **Apostrophes first:** a mark between two letters, `d’`/`z’`/`s’`/`l’` with a stray space
  (`d' Kierch`, `d ‘Kierch`, `z' erreechen`), `’t`/`’s`/`’mol` at the start of a word, a misplaced
  `t’ gouf` → `’t gouf`, and years (`1944-’45`). Digits on both sides are left alone (`7’52`,
  coordinates).
- **Then the quotation marks are paired** with a stack, by position (space before = opening, space
  after = closing), whatever character was typed. A closing mark closes whatever is open, so
  `‘Text“` pairs too; the report has a filter for those 60-odd paragraphs. Commas typed as opening
  marks (`,Nonnegewand‘`) and two single marks typed for a double one are recognised.
- **A paragraph is only converted if every mark pairs up.** It is also left alone when a quote
  comes out implausibly long with other quotes inside, which is the sign of a stray mark pairing
  with one far away.
- Single guillemets `‹ ›` are not treated as quotation marks (used once, for a letter inside a
  word).

## Checks built in and done

- The script stops if anything other than punctuation and leftovers would change: every letter
  and digit, and every tag other than the leftovers, must come out exactly as they went in. Links,
  images and `<u>` may only disappear, not change, and a link or image only if it was dead.
- Running it on its own output changes nothing.
- The update followed by the undo, in a transaction that was rolled back, restored the identical
  checksum over all texts (checked again on 27 October).
- New texts can bring these problems back; the clean-up can be repeated.
