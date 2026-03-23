package ro.unibuc.prodeng.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.prodeng.request.CreateTicketRequest;
import ro.unibuc.prodeng.response.TicketDetailResponse;
import ro.unibuc.prodeng.service.TicketService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse response = ticketService.createTicket(
                request.title(),
                request.description(),
                request.repositoryId(),
                request.assignedById());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketDetailResponse> changeStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        TicketDetailResponse response = ticketService.changeStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repository/{repositoryId}")
    public ResponseEntity<List<TicketDetailResponse>> getTicketsByRepository(@PathVariable String repositoryId) {
        List<TicketDetailResponse> responses = ticketService.getTicketsByRepository(repositoryId);
        return ResponseEntity.ok(responses);
    }
}
