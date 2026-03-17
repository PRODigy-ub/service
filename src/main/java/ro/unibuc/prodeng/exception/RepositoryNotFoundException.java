package ro.unibuc.prodeng.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String id) {
        super(String.format("Repository with ID '%s' was not found", id));
    }
}
