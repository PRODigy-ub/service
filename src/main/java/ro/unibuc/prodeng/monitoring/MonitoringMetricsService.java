package ro.unibuc.prodeng.monitoring;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.CommentRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.repository.TodoRepository;
import ro.unibuc.prodeng.repository.UserRepository;

@Service
public class MonitoringMetricsService {

    private final MeterRegistry registry;
    private final Counter usersCreatedCounter;
    private final Counter ticketsCreatedCounter;
    private final Counter commentsCreatedCounter;
    private final Counter todosCreatedCounter;
    private final AtomicInteger requestsInFlight;

    public MonitoringMetricsService(MeterRegistry registry,
            TicketRepository ticketRepository,
            TodoRepository todoRepository,
            CommentRepository commentRepository,
            UserRepository userRepository) {
        this.registry = registry;
        this.usersCreatedCounter = Counter.builder("app_users_created")
                .description("Total number of users created")
                .register(registry);
        this.ticketsCreatedCounter = Counter.builder("app_tickets_created")
                .description("Total number of tickets created")
                .register(registry);
        this.commentsCreatedCounter = Counter.builder("app_comments_created")
                .description("Total number of comments added to tickets")
                .register(registry);
        this.todosCreatedCounter = Counter.builder("app_todos_created")
                .description("Total number of todos created")
                .register(registry);
        this.requestsInFlight = registry.gauge("app_requests_in_flight",
                new AtomicInteger(0));

        Gauge.builder("app_open_tickets", ticketRepository, repo -> repo.countByStatusNot(TicketStatusEnum.CLOSED))
                .description("Number of tickets that are not closed")
                .register(registry);
        Gauge.builder("app_pending_todos", todoRepository, TodoRepository::countByDoneFalse)
                .description("Number of todos that are not marked as done")
                .register(registry);
        Gauge.builder("app_registered_users", userRepository, UserRepository::count)
                .description("Number of users currently stored in the application")
                .register(registry);
        Gauge.builder("app_ticket_comments", commentRepository, CommentRepository::count)
                .description("Number of comments currently stored for all tickets")
                .register(registry);
    }

    public void incrementRequestsInFlight() {
        requestsInFlight.incrementAndGet();
    }

    public void decrementRequestsInFlight() {
        requestsInFlight.decrementAndGet();
    }

    public void recordRequest(String method, String uri, int status, long durationNanos) {
        Timer.builder("app_request_duration_seconds")
                .description("Custom request duration metric for the ProdEng application")
                .publishPercentileHistogram()
                .tags(List.of(
                        Tag.of("method", method),
                        Tag.of("uri", normalizeUri(uri)),
                        Tag.of("status", Integer.toString(status))))
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordError(String exceptionName, int status) {
        Counter.builder("app_errors_total")
                .description("Total number of application errors grouped by exception and HTTP status")
                .tags(List.of(
                        Tag.of("exception", exceptionName),
                        Tag.of("status", Integer.toString(status))))
                .register(registry)
                .increment();
    }

    public void recordUserCreated() {
        usersCreatedCounter.increment();
    }

    public void recordTicketCreated() {
        ticketsCreatedCounter.increment();
    }

    public void recordCommentCreated() {
        commentsCreatedCounter.increment();
    }

    public void recordTodoCreated() {
        todosCreatedCounter.increment();
    }

    private String normalizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "unknown";
        }
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }
}
