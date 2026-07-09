#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_audit_pdf.py — renders App_Audit_Report.md into a styled App_Audit_Report.pdf
using ReportLab (pure Python, no system dependencies).

Supports a pragmatic subset of Markdown tailored to this report:
  #..#### headings, paragraphs, - / * bullet lists, 1. ordered lists,
  fenced ```code``` blocks, > blockquotes, --- rules, GitHub pipe tables,
  inline **bold**, *italic*, `code`, and severity keyword coloring in tables.

Usage:  py build_audit_pdf.py [input.md] [output.pdf]
"""
import re
import sys
import html
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, PageBreak, KeepTogether, Preformatted, ListFlowable, ListItem,
)

INPUT = sys.argv[1] if len(sys.argv) > 1 else "App_Audit_Report.md"
OUTPUT = sys.argv[2] if len(sys.argv) > 2 else "App_Audit_Report.pdf"

# ---- palette -----------------------------------------------------------------
INK      = colors.HexColor("#0B0B0D")
BODY     = colors.HexColor("#1C1C22")
MUTE     = colors.HexColor("#5B5B66")
ACCENT   = colors.HexColor("#B88A2E")   # champagne
ACCENT2  = colors.HexColor("#2E6F6B")
RULE     = colors.HexColor("#D8D8DE")
CODEBG   = colors.HexColor("#F4F4F6")
QUOTEBG  = colors.HexColor("#FBF7EE")
HEADBG   = colors.HexColor("#14141A")
ZEBRA    = colors.HexColor("#F6F6F8")
SEV = {
    "CRITICAL": colors.HexColor("#B3261E"),
    "HIGH":     colors.HexColor("#C4661A"),
    "MEDIUM":   colors.HexColor("#B88A2E"),
    "LOW":      colors.HexColor("#3A7D44"),
    "CONFIRMED":colors.HexColor("#B3261E"),
    "SUSPICIOUS":colors.HexColor("#C4661A"),
}

# ---- styles ------------------------------------------------------------------
ss = getSampleStyleSheet()
def mk(name, **kw):
    kw.setdefault("parent", ss["Normal"])
    return ParagraphStyle(name, **kw)

S_BODY = mk("body", fontName="Helvetica", fontSize=9.5, leading=14, textColor=BODY,
            spaceBefore=2, spaceAfter=6, alignment=TA_LEFT)
S_H1 = mk("h1", fontName="Helvetica-Bold", fontSize=19, leading=23, textColor=INK,
          spaceBefore=18, spaceAfter=6, keepWithNext=True)
S_H2 = mk("h2", fontName="Helvetica-Bold", fontSize=15, leading=19, textColor=ACCENT2,
          spaceBefore=15, spaceAfter=4, keepWithNext=True)
S_H3 = mk("h3", fontName="Helvetica-Bold", fontSize=12, leading=16, textColor=INK,
          spaceBefore=11, spaceAfter=3, keepWithNext=True)
S_H4 = mk("h4", fontName="Helvetica-BoldOblique", fontSize=10.5, leading=14, textColor=MUTE,
          spaceBefore=8, spaceAfter=2, keepWithNext=True)
S_CODE = mk("code", fontName="Courier", fontSize=8, leading=10.5, textColor=colors.HexColor("#22303A"))
S_QUOTE = mk("quote", parent=S_BODY, leftIndent=8, textColor=colors.HexColor("#5A4A22"),
             fontName="Helvetica-Oblique")
S_TCELL = mk("tcell", fontName="Helvetica", fontSize=8, leading=10.5, textColor=BODY)
S_THEAD = mk("thead", fontName="Helvetica-Bold", fontSize=8, leading=10.5, textColor=colors.white)
S_LI = mk("li", parent=S_BODY, spaceBefore=1, spaceAfter=1)
S_COVER_T = mk("coverT", fontName="Helvetica-Bold", fontSize=34, leading=40, textColor=INK, alignment=TA_CENTER)
S_COVER_S = mk("coverS", fontName="Helvetica", fontSize=13, leading=19, textColor=MUTE, alignment=TA_CENTER)
S_COVER_M = mk("coverM", fontName="Helvetica", fontSize=9.5, leading=15, textColor=MUTE, alignment=TA_CENTER)

# ---- inline markdown ---------------------------------------------------------
def inline(text):
    t = html.escape(text, quote=False)
    t = re.sub(r"`([^`]+)`", r'<font face="Courier" size="8" color="#22303A">\1</font>', t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", t)
    t = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<i>\1</i>", t)
    t = re.sub(r"~~([^~]+)~~", r"<strike>\1</strike>", t)
    t = re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<link href="\2" color="#2E6F6B">\1</link>', t)
    return t

def sev_color(s):
    key = s.strip().upper()
    for k, c in SEV.items():
        if key.startswith(k):
            return c
    return None

# ---- table -------------------------------------------------------------------
def parse_table(lines, widths_total):
    rows = []
    for ln in lines:
        cells = [c.strip() for c in ln.strip().strip("|").split("|")]
        rows.append(cells)
    if len(rows) >= 2 and re.match(r"^:?-{2,}:?$", rows[1][0].replace(" ", "") or "-"):
        header = rows[0]
        body = rows[2:]
    else:
        header = rows[0]; body = rows[1:]
    ncol = len(header)
    data = []
    data.append([Paragraph(inline(h), S_THEAD) for h in header])
    sev_rows = []
    for r in body:
        r = (r + [""] * ncol)[:ncol]
        prow = []
        rowsev = None
        for cell in r:
            sc = sev_color(cell)
            if sc and rowsev is None:
                rowsev = sc
            st = S_TCELL
            if sc:
                st = mk("sevcell", parent=S_TCELL, fontName="Helvetica-Bold", textColor=sc)
            prow.append(Paragraph(inline(cell), st))
        data.append(prow)
        sev_rows.append(rowsev)
    # column widths: weight first col a bit less, spread rest
    avail = widths_total
    if ncol == 1:
        w = [avail]
    else:
        base = avail / ncol
        w = [base] * ncol
    tbl = Table(data, colWidths=w, repeatRows=1)
    stylecmds = [
        ("BACKGROUND", (0, 0), (-1, 0), HEADBG),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("GRID", (0, 0), (-1, -1), 0.4, RULE),
        ("LINEBELOW", (0, 0), (-1, 0), 0.8, ACCENT),
    ]
    for i, rs in enumerate(sev_rows, start=1):
        bg = ZEBRA if i % 2 == 0 else colors.white
        stylecmds.append(("BACKGROUND", (0, i), (-1, i), bg))
    tbl.setStyle(TableStyle(stylecmds))
    return tbl

# ---- block parser ------------------------------------------------------------
def render(md, frame_width):
    flow = []
    lines = md.split("\n")
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        # fenced code
        if stripped.startswith("```"):
            i += 1
            buf = []
            while i < n and not lines[i].strip().startswith("```"):
                buf.append(lines[i]); i += 1
            i += 1
            code = "\n".join(buf) if buf else " "
            box = Preformatted(code, S_CODE)
            tbl = Table([[box]], colWidths=[frame_width])
            tbl.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), CODEBG),
                ("BOX", (0, 0), (-1, -1), 0.4, RULE),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]))
            flow.append(Spacer(1, 3)); flow.append(tbl); flow.append(Spacer(1, 5))
            continue

        # table
        if "|" in line and stripped.startswith("|"):
            tb = []
            while i < n and lines[i].strip().startswith("|"):
                tb.append(lines[i]); i += 1
            if len(tb) >= 2:
                flow.append(Spacer(1, 3))
                flow.append(parse_table(tb, frame_width))
                flow.append(Spacer(1, 6))
                continue

        # headings
        m = re.match(r"^(#{1,6})\s+(.*)$", stripped)
        if m:
            level = len(m.group(1)); txt = m.group(2).strip()
            if level == 1:
                flow.append(HRFlowable(width="100%", thickness=1.1, color=ACCENT,
                                       spaceBefore=10, spaceAfter=3))
                flow.append(Paragraph(inline(txt), S_H1))
            elif level == 2:
                flow.append(Paragraph(inline(txt), S_H2))
            elif level == 3:
                flow.append(Paragraph(inline(txt), S_H3))
            else:
                flow.append(Paragraph(inline(txt), S_H4))
            i += 1
            continue

        # hr
        if re.match(r"^(-{3,}|\*{3,}|_{3,})$", stripped):
            flow.append(HRFlowable(width="100%", thickness=0.5, color=RULE,
                                   spaceBefore=6, spaceAfter=6))
            i += 1
            continue

        # blockquote
        if stripped.startswith(">"):
            buf = []
            while i < n and lines[i].strip().startswith(">"):
                buf.append(re.sub(r"^\s*>\s?", "", lines[i])); i += 1
            para = Paragraph(inline(" ".join(buf)), S_QUOTE)
            tbl = Table([[para]], colWidths=[frame_width])
            tbl.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), QUOTEBG),
                ("LINEBEFORE", (0, 0), (0, -1), 2.2, ACCENT),
                ("LEFTPADDING", (0, 0), (-1, -1), 9),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
            ]))
            flow.append(Spacer(1, 2)); flow.append(tbl); flow.append(Spacer(1, 5))
            continue

        # lists (grouped)
        if re.match(r"^\s*([-*+]|\d+\.)\s+", line):
            items = []
            ordered = bool(re.match(r"^\s*\d+\.\s+", line))
            while i < n and re.match(r"^\s*([-*+]|\d+\.)\s+", lines[i]):
                txt = re.sub(r"^\s*([-*+]|\d+\.)\s+", "", lines[i])
                items.append(ListItem(Paragraph(inline(txt), S_LI), leftIndent=12,
                                      value=None))
                i += 1
            lf = ListFlowable(items, bulletType="1" if ordered else "bullet",
                              start="1" if ordered else None,
                              bulletColor=ACCENT, bulletFontSize=7,
                              leftIndent=14, bulletFontName="Helvetica-Bold")
            flow.append(lf); flow.append(Spacer(1, 4))
            continue

        # blank
        if stripped == "":
            i += 1
            continue

        # paragraph (gather until blank/structural)
        buf = [line]
        i += 1
        while i < n and lines[i].strip() != "" and not re.match(
                r"^(#{1,6}\s|>|\s*([-*+]|\d+\.)\s|```|\|)", lines[i]) and not re.match(
                r"^(-{3,}|\*{3,}|_{3,})$", lines[i].strip()):
            buf.append(lines[i]); i += 1
        flow.append(Paragraph(inline(" ".join(b.strip() for b in buf)), S_BODY))
    return flow

# ---- doc template with header/footer ----------------------------------------
PAGE_W, PAGE_H = A4
MARGIN = 18 * mm

def on_page(canvas, doc):
    canvas.saveState()
    # footer
    canvas.setStrokeColor(RULE); canvas.setLineWidth(0.4)
    canvas.line(MARGIN, 13 * mm, PAGE_W - MARGIN, 13 * mm)
    canvas.setFont("Helvetica", 7.5); canvas.setFillColor(MUTE)
    canvas.drawString(MARGIN, 9 * mm, "JARVIS — Life OS · Codebase Audit Report")
    canvas.drawRightString(PAGE_W - MARGIN, 9 * mm, "Page %d" % doc.page)
    canvas.restoreState()

def on_cover(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(INK)
    canvas.rect(0, PAGE_H - 15 * mm, PAGE_W, 15 * mm, fill=1, stroke=0)
    canvas.setFillColor(INK)
    canvas.rect(0, 0, PAGE_W, 15 * mm, fill=1, stroke=0)
    canvas.setStrokeColor(ACCENT); canvas.setLineWidth(1.4)
    canvas.line(MARGIN, PAGE_H - 15 * mm, PAGE_W - MARGIN, PAGE_H - 15 * mm)
    canvas.line(MARGIN, 15 * mm, PAGE_W - MARGIN, 15 * mm)
    canvas.restoreState()

def build(cover_flow, body_flow):
    doc = BaseDocTemplate(OUTPUT, pagesize=A4,
                          leftMargin=MARGIN, rightMargin=MARGIN,
                          topMargin=MARGIN, bottomMargin=20 * mm,
                          title="JARVIS Life OS — Codebase Audit Report",
                          author="Automated Audit")
    fw = PAGE_W - 2 * MARGIN
    cover_frame = Frame(MARGIN, 20 * mm, fw, PAGE_H - 40 * mm, id="cover")
    body_frame = Frame(MARGIN, 20 * mm, fw, PAGE_H - 2 * MARGIN - 6 * mm, id="body")
    doc.addPageTemplates([
        PageTemplate(id="Cover", frames=[cover_frame], onPage=on_cover),
        PageTemplate(id="Body", frames=[body_frame], onPage=on_page),
    ])
    story = []
    story += cover_flow
    story.append(PageBreak())
    story.append(_NextTemplate("Body"))
    story += body_flow
    doc.build(story)

from reportlab.platypus import NextPageTemplate as _NextTemplate

def cover(frame_width):
    f = []
    f.append(Spacer(1, 42 * mm))
    f.append(Paragraph("CODEBASE AUDIT", mk("kick", fontName="Helvetica-Bold", fontSize=11,
             textColor=ACCENT, alignment=TA_CENTER, spaceAfter=10)))
    f.append(Paragraph("JARVIS — Life OS", S_COVER_T))
    f.append(Spacer(1, 5))
    f.append(Paragraph("Ungeschönter Deep-Audit: Design &amp; UI/UX · Architektur · Funktionalität",
             S_COVER_S))
    f.append(Spacer(1, 16))
    f.append(HRFlowable(width="55%", thickness=1.0, color=ACCENT, hAlign="CENTER"))
    f.append(Spacer(1, 16))
    f.append(Paragraph("Native Android · Kotlin 1.9.24 · Jetpack Compose · com.ascend.lifeos<br/>"
                       "~43k LOC · 14 Module · offline-first", S_COVER_M))
    return f

if __name__ == "__main__":
    with open(INPUT, "r", encoding="utf-8") as fh:
        md = fh.read()
    fw = PAGE_W - 2 * MARGIN
    body_flow = render(md, fw)
    build(cover(fw), body_flow)
    print("OK wrote", OUTPUT, "from", INPUT, "(%d body flowables)" % len(body_flow))
