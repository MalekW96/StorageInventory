package com.malek.storageinventory

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** One worksheet: a header row, data rows (String or Number cells) and column widths. */
class XlsxSheet(
    val name: String,
    val header: List<String>,
    val rows: List<List<Any?>>,
    val widths: List<Int>,
)

/**
 * Minimal Excel (.xlsx) writer: bold coloured header row, frozen header, filter buttons,
 * column widths. No external library needed.
 */
object Xlsx {
    private const val MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    fun write(out: File, sheets: List<XlsxSheet>) {
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            fun put(path: String, text: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes(sheets.size))
            put(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                    """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    """<Relationship Id="rId1" Type="$REL/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
            )
            put("xl/workbook.xml", workbook(sheets))
            put("xl/_rels/workbook.xml.rels", workbookRels(sheets.size))
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheet(s)) }
        }
    }

    private fun contentTypes(n: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        for (i in 1..n) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        append("</Types>")
    }

    private fun workbook(sheets: List<XlsxSheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="$MAIN" xmlns:r="$REL"><sheets>""")
        val used = HashSet<String>()
        sheets.forEachIndexed { i, s ->
            var name = safeSheetName(s.name).ifEmpty { "Sheet${i + 1}" }
            while (!used.add(name.lowercase())) name = name.take(28) + "_${i + 1}"
            append("""<sheet name="${esc(name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(n: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..n) append("""<Relationship Id="rId$i" Type="$REL/worksheet" Target="worksheets/sheet$i.xml"/>""")
        append("""<Relationship Id="rId${n + 1}" Type="$REL/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun sheet(s: XlsxSheet): String = buildString {
        val cols = maxOf(s.header.size, s.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
        val lastRow = s.rows.size + 1
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="$MAIN" xmlns:r="$REL">""")
        append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        append("<cols>")
        for (c in 0 until cols) {
            val w = s.widths.getOrNull(c) ?: 14
            append("""<col min="${c + 1}" max="${c + 1}" width="$w" customWidth="1"/>""")
        }
        append("</cols><sheetData>")
        appendRow(this, 1, s.header, header = true)
        s.rows.forEachIndexed { i, r -> appendRow(this, i + 2, r, header = false) }
        append("</sheetData>")
        append("""<autoFilter ref="A1:${col(cols - 1)}$lastRow"/>""")
        append("</worksheet>")
    }

    private fun appendRow(sb: StringBuilder, r: Int, cells: List<Any?>, header: Boolean) {
        sb.append("""<row r="$r">""")
        cells.forEachIndexed { c, v ->
            val ref = col(c) + r
            val style = if (header) """ s="1"""" else ""
            when (v) {
                null -> {}
                is Number -> sb.append("""<c r="$ref"$style><v>$v</v></c>""")
                else -> sb.append(
                    """<c r="$ref"$style t="inlineStr"><is><t xml:space="preserve">${esc(v.toString())}</t></is></c>"""
                )
            }
        }
        sb.append("</row>")
    }

    private fun col(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val m = (n - 1) % 26
            sb.insert(0, ('A' + m))
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun safeSheetName(name: String) = name.replace(Regex("""[\[\]:*?/\\]"""), " ").trim().take(31)

    private fun esc(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch == '\t' || ch == '\n' || ch == '\r' -> sb.append(ch)
                ch < ' ' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
        """<styleSheet xmlns="$MAIN">""" +
        """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>""" +
        """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FF1F4E5F"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
        """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs>""" +
        """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
        """</styleSheet>"""
}
