package ro.unibuc.prodeng.service;

import ro.unibuc.prodeng.response.TicketDetailResponse;
import java.util.List;

public interface TicketService {
    TicketDetailResponse createTicket(String title, String description, String repositoryId, String assignedById);

    TicketDetailResponse changeStatus(String ticketId, String newStatusEnumName);

    List<TicketDetailResponse> getTicketsByRepository(String repositoryId);
}
