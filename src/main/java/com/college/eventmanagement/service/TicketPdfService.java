package com.college.eventmanagement.service;

import com.college.eventmanagement.exception.ResourceNotFoundException;
import com.college.eventmanagement.model.Event;
import com.college.eventmanagement.model.Registration;
import com.college.eventmanagement.model.Student;
import com.college.eventmanagement.repository.RegistrationRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class TicketPdfService {

    private static final DateTimeFormatter TICKET_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final RegistrationRepository registrationRepository;

    public TicketPdfService(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    public byte[] generateTicket(String ticketCode) {
        Registration registration = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        Event event = registration.getEvent();
        Student student = registration.getStudent();

        String date = event.getEventDate().format(TICKET_DATE).toUpperCase(Locale.ENGLISH);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(500, 300));
            document.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                // Border
                stroke(content, 31, 58, 95);
                content.addRect(1, 1, 498, 298);
                content.stroke();

                // Header band
                fill(content, 31, 58, 95);
                content.addRect(0, 240, 500, 60);
                content.fill();

                fill(content, 255, 255, 255);
                writeText(content, bold, 18, 24, 262, "COLLEGE EVENT TICKET");

                // Event details
                fill(content, 0, 0, 0);
                writeText(content, bold, 20, 24, 200, fit(event.getName(), 36));

                fill(content, 60, 60, 60);
                writeText(content, regular, 13, 24, 170, "Date: " + date);
                writeText(content, regular, 13, 24, 150, "Venue: " + fit(event.getVenue(), 50));
                writeText(content, regular, 13, 24, 130, "Category: " + fit(event.getCategory(), 30));

                // Attendee details
                fill(content, 0, 0, 0);
                writeText(content, bold, 14, 24, 95, "Attendee: " + fit(student.getName(), 40));
                writeText(content, regular, 12, 24, 75, "Registration ID: " + registration.getId());

                // Footer
                fill(content, 100, 100, 100);
                writeText(content, regular, 8, 24, 40, "Ticket code: " + ticketCode);
                writeText(content, regular, 10, 24, 22, "Please show this ticket at the venue entrance.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate the ticket", e);
        }
    }

    // PDFBox expects colour values between 0 and 1, so we convert from the usual 0-255
    private void stroke(PDPageContentStream content, int r, int g, int b) throws IOException {
        content.setStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    private void fill(PDPageContentStream content, int r, int g, int b) throws IOException {
        content.setNonStrokingColor(r / 255f, g / 255f, b / 255f);
    }

    private void writeText(PDPageContentStream content, PDType1Font font, float size,
                           float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    // The built-in PDF fonts only support basic characters, so anything else becomes "?"
    private String safe(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(c >= 32 && c <= 126 ? c : '?');
        }
        return result.toString();
    }

    private String fit(String text, int maxLength) {
        String clean = safe(text);
        return clean.length() > maxLength
                ? clean.substring(0, maxLength - 3) + "..."
                : clean;
    }
}