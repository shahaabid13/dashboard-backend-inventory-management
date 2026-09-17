package com.sscl.sdnetmonitor.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sscl.sdnetmonitor.dto.DowntimeIncidentDto;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a DowntimeReportService result to a PDF byte array. Kept
 * deliberately separate from DowntimeReportService itself: that class
 * answers "what happened", this one only answers "how does it look on
 * paper" -- a future CSV/Excel export would sit next to this one instead
 * of inside it.
 */
@Service
public class DowntimeReportPdfService {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter
            .ofPattern("dd-MMM-yyyy HH:mm:ss")
            .withZone(ZoneId.of("Asia/Kolkata"));

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0x15, 0x22, 0x38));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SUMMARY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font BODY_FONT_OPEN = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(0xE0, 0x34, 0x2A));
    private static final Color HEADER_BG = new Color(0x22, 0x31, 0x4a);

    public byte[] render(List<DowntimeIncidentDto> incidents, Instant from, Instant to, String categoryFilter) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("SDNET Fibre & Device Downtime Report", TITLE_FONT));
            document.add(new Paragraph("Period: " + TS_FORMAT.format(from) + "  to  " + TS_FORMAT.format(to) + " IST", SUBTITLE_FONT));
            document.add(new Paragraph("Category: " + (categoryFilter != null ? categoryFilter : "All"), SUBTITLE_FONT));
            document.add(new Paragraph("Generated: " + TS_FORMAT.format(Instant.now()) + " IST", SUBTITLE_FONT));
            document.add(Chunk.NEWLINE);
            document.add(summaryParagraph(incidents));
            document.add(Chunk.NEWLINE);

            if (incidents.isEmpty()) {
                document.add(new Paragraph("No downtime incidents recorded in this period.", BODY_FONT));
            } else {
                document.add(incidentTable(incidents));
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render downtime report PDF", e);
        }
        return out.toByteArray();
    }

    private Paragraph summaryParagraph(List<DowntimeIncidentDto> incidents) {
        long deviceIncidents = incidents.stream().filter(i -> i.entityType().equals("DEVICE")).count();
        long linkIncidents = incidents.stream().filter(i -> i.entityType().equals("FIBRE_LINK")).count();
        long stillDown = incidents.stream().filter(i -> i.upAt() == null).count();
        long totalDownSeconds = incidents.stream().mapToLong(DowntimeIncidentDto::durationSeconds).sum();

        Paragraph summary = new Paragraph();
        summary.add(new Chunk("Total incidents: " + incidents.size() + "    ", SUMMARY_FONT));
        summary.add(new Chunk("Device outages: " + deviceIncidents + "    ", SUMMARY_FONT));
        summary.add(new Chunk("Fibre-link outages: " + linkIncidents + "    ", SUMMARY_FONT));
        summary.add(new Chunk("Currently down: " + stillDown + "    ", SUMMARY_FONT));
        summary.add(new Chunk("Total downtime (sum of all incidents): " + formatDuration(totalDownSeconds), SUMMARY_FONT));
        return summary;
    }

    private PdfPTable incidentTable(List<DowntimeIncidentDto> incidents) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 1f, 2.3f, 1.1f, 1.7f, 1.7f, 1.3f, 1f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        for (String header : new String[]{"Type", "ID", "Label", "Category", "Down At (IST)", "Up At (IST)", "Duration", "Source"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        for (DowntimeIncidentDto incident : incidents) {
            boolean stillOpen = incident.upAt() == null;
            Font font = stillOpen ? BODY_FONT_OPEN : BODY_FONT;

            addCell(table, incident.entityType().equals("DEVICE") ? "Device" : "Fibre link", font);
            addCell(table, incident.entityId(), font);
            addCell(table, incident.entityLabel(), font);
            addCell(table, incident.category() != null ? incident.category() : "-", font);
            addCell(table, TS_FORMAT.format(incident.downAt()), font);
            addCell(table, stillOpen ? "STILL DOWN" : TS_FORMAT.format(incident.upAt()), font);
            addCell(table, formatDuration(incident.durationSeconds()) + (stillOpen ? " (so far)" : ""), font);
            addCell(table, incident.source(), font);
        }
        return table;
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh %dm", h, m);
        if (m > 0) return String.format("%dm %ds", m, s);
        return s + "s";
    }
}
