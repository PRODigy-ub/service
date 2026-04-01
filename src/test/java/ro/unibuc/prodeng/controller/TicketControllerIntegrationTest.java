package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.CommentRepository;
import ro.unibuc.prodeng.repository.RepositoryRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.request.CreateRepositoryRequest;
import ro.unibuc.prodeng.request.CreateTicketRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TicketController Integration Tests")
class TicketControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        repositoryRepository.deleteAll();
    }

    @Test
    void testAssignAndResolveTicket_complexBusinessRules_validatedAndPersisted() throws Exception {
        String repositoryId = createRepository("repo-ticket-complex");
        String ticketId = createTicket(repositoryId, "critical bug", "fix memory leak", "lead-1");

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentUserId":"owner-1",
                                  "assignedToId":"dev-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("IN_EXECUTION"))
                .andExpect(jsonPath("$.assignedToId").value("dev-1"));

        TicketEntity ticketAfterAssign = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatusEnum.IN_EXECUTION, ticketAfterAssign.status());
        assertEquals("dev-1", ticketAfterAssign.assignedToId());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"dev-1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Ticket cannot be resolved without at least one comment explaining the solution."));

        TicketEntity ticketAfterFailedResolve = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatusEnum.IN_EXECUTION, ticketAfterFailedResolve.status());

        addComment(ticketId, "dev-1", "Root cause fixed and regression tested.");
        assertTrue(commentRepository.countByTicketId(ticketId) > 0, "A solution comment should be persisted");

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"dev-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.assignedToId").value("dev-1"));

        TicketEntity ticketAfterResolve = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatusEnum.RESOLVED, ticketAfterResolve.status());
        assertEquals("dev-1", ticketAfterResolve.assignedToId());
    }

    @Test
    void testAssignTicket_unauthorizedUser_rejectedAndRepositoryUnchanged() throws Exception {
        String repositoryId = createRepository("repo-ticket-unauthorized");
        String ticketId = createTicket(repositoryId, "feature request", "export csv", "lead-1");

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentUserId":"outsider-1",
                                  "assignedToId":"dev-2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unauthorized to reassign this ticket."));

        TicketEntity persistedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertEquals(TicketStatusEnum.OPEN, persistedTicket.status());
        assertNull(persistedTicket.assignedToId());
    }

    private String createRepository(String key) throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest(
                "Repository " + key,
                "Repository for ticket integration tests",
                key,
                "owner-1");

        String response = mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createTicket(String repositoryId, String title, String description, String assignedById) throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(title, description, repositoryId, assignedById);

        String response = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedById").value(assignedById))
                .andExpect(jsonPath("$.targetVersion").value("1.0.0"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private void addComment(String ticketId, String userId, String content) throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketId":"%s",
                                  "userId":"%s",
                                  "content":"%s"
                                }
                                """.formatted(ticketId, userId, content)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.userId").value(userId));
    }
}
