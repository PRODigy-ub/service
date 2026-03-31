package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.RepositoryEntity;
import ro.unibuc.prodeng.model.TicketEntity;
import ro.unibuc.prodeng.model.enums.TicketStatusEnum;
import ro.unibuc.prodeng.repository.RepositoryRepository;
import ro.unibuc.prodeng.repository.RepositoryUpdateRepository;
import ro.unibuc.prodeng.repository.TicketRepository;
import ro.unibuc.prodeng.request.CreateRepositoryRequest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RepositoryController Integration Tests")
class RepositoryControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RepositoryUpdateRepository repositoryUpdateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        ticketRepository.deleteAll();
        repositoryUpdateRepository.deleteAll();
        repositoryRepository.deleteAll();
    }

    @ParameterizedTest(name = "update to {0} blocked when ticket status is {1}")
    @MethodSource("provideBlockedVersionUpdates")
    void testUpdateVersion_blockedAndRepositoryStateUnchanged(
            String requestedVersion,
            TicketStatusEnum openTicketStatus,
            String expectedErrorMessage) throws Exception {

        String repositoryId = createRepository("repo-key-" + requestedVersion.replace('.', '-'));
        ticketRepository.save(new TicketEntity(
                null,
                "Blocking ticket",
                "must be handled first",
                repositoryId,
                "lead-1",
                "dev-1",
                openTicketStatus,
                "1.0.0"));

        mockMvc.perform(patch("/api/repositories/" + repositoryId + "/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newVersion":"%s",
                                  "releaseNotes":"integration test update",
                                  "updatedById":"release-manager-1"
                                }
                                """.formatted(requestedVersion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(expectedErrorMessage));

        RepositoryEntity persistedRepository = repositoryRepository.findById(repositoryId).orElseThrow();
        assertEquals("1.0.0", persistedRepository.currentVersion(), "Version should remain unchanged in DB");
        assertTrue(repositoryUpdateRepository.findByRepositoryIdOrderByUpdateDateDesc(repositoryId).isEmpty(),
                "No update history should be written when validation fails");
    }

    @Test
    void testUpdateVersion_patchAllowedWithOpenTickets_persistsVersionAndHistory() throws Exception {
        String repositoryId = createRepository("repo-key-patch-success");
        ticketRepository.save(new TicketEntity(
                null,
                "Open ticket",
                "does not block patch updates",
                repositoryId,
                "lead-1",
                "dev-1",
                TicketStatusEnum.OPEN,
                "1.0.0"));

        mockMvc.perform(patch("/api/repositories/" + repositoryId + "/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newVersion":"1.0.1",
                                  "releaseNotes":"hotfix",
                                  "updatedById":"release-manager-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(repositoryId))
                .andExpect(jsonPath("$.currentVersion").value("1.0.1"))
                .andExpect(jsonPath("$.unresolvedTicketsCount").value(1))
                .andExpect(jsonPath("$.versionHistory.length()").value(1))
                .andExpect(jsonPath("$.versionHistory[0].previousVersion").value("1.0.0"))
                .andExpect(jsonPath("$.versionHistory[0].newVersion").value("1.0.1"));

        RepositoryEntity persistedRepository = repositoryRepository.findById(repositoryId).orElseThrow();
        assertEquals("1.0.1", persistedRepository.currentVersion());
        assertEquals(1, repositoryUpdateRepository.findByRepositoryIdOrderByUpdateDateDesc(repositoryId).size());
    }

    private String createRepository(String key) throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest(
                "Repo " + key,
                "Repository used in integration tests",
                key,
                "owner-1");

        String response = mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.currentVersion").value("1.0.0"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private static Stream<Arguments> provideBlockedVersionUpdates() {
        return Stream.of(
                Arguments.of("2.0.0", TicketStatusEnum.OPEN,
                        "Cannot perform MAJOR update. There are pending unclosed tickets."),
                Arguments.of("1.1.0", TicketStatusEnum.IN_EXECUTION,
                        "Cannot perform MINOR update. Close tickets on version 1.0.0 first."));
    }
}
