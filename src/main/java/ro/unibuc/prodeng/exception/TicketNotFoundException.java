package ro.unibuc.prodeng.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String id) {
        super(String.format("Ticket with ID '%s' was not found", id));
    }
}
