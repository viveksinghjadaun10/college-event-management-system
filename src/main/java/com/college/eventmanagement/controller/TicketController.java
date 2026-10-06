package com.college.eventmanagement.controller;

import com.college.eventmanagement.service.TicketPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketPdfService ticketPdfService;

    public TicketController(TicketPdfService ticketPdfService) {
        this.ticketPdfService = ticketPdfService;
    }

    @GetMapping("/{ticketCode}")
    public ResponseEntity<byte[]> download(@PathVariable String ticketCode) {
        byte[] pdf = ticketPdfService.generateTicket(ticketCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("event-ticket.pdf")
                .build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}