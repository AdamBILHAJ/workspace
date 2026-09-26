package com.example.team_workspace.workspace;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workspace-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "app.jwt.secret=integration-test-signing-key-with-more-than-32-bytes",
        "app.jwt.expiration=1h"
})
@AutoConfigureMockMvc
@Transactional
class WorkspaceIntegrationTest {

    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void organizationWorkspaceAndMembershipFlow() throws Exception {
        JsonNode owner = signUp("owner@example.com");
        String ownerToken = owner.path("accessToken").asText();
        String teammateToken = signUp("teammate@example.com").path("accessToken").asText();

        JsonNode organization = createOrganization(ownerToken, "Engineering Team");
        assertThat(organization.path("slug").asText()).isEqualTo("engineering-team");

        mockMvc.perform(get("/api/v1/organizations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("engineering-team"));

        JsonNode workspace = createWorkspace(ownerToken, organization.path("id").asLong(), "Platform Engineering");
        assertThat(workspace.path("slug").asText()).isEqualTo("platform-engineering");
        assertThat(workspace.path("role").asText()).isEqualTo("OWNER");
        assertThat(workspace.path("organization").path("slug").asText()).isEqualTo("engineering-team");

        mockMvc.perform(get("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("platform-engineering"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspace.path("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("teammate@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.user.email").value("teammate@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        mockMvc.perform(get("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teammateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("platform-engineering"))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    void workspaceListingIsScopedToMemberships() throws Exception {
        JsonNode first = signUp("first@example.com");
        JsonNode second = signUp("second@example.com");

        JsonNode firstWorkspace = createWorkspace(
                first.path("accessToken").asText(),
                createOrganization(first.path("accessToken").asText(), "First Org").path("id").asLong(),
                "First Workspace"
        );
        JsonNode secondWorkspace = createWorkspace(
                second.path("accessToken").asText(),
                createOrganization(second.path("accessToken").asText(), "Second Org").path("id").asLong(),
                "Second Workspace"
        );

        assertThat(firstWorkspace.path("slug").asText()).isEqualTo("first-workspace");
        assertThat(secondWorkspace.path("slug").asText()).isEqualTo("second-workspace");

        mockMvc.perform(get("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + first.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("first-workspace"));

        mockMvc.perform(get("/api/v1/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + second.path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("second-workspace"));
    }

    @Test
    void duplicateNamesReceiveUniqueSlugs() throws Exception {
        String token = signUp("slug-user@example.com").path("accessToken").asText();

        JsonNode firstOrganization = createOrganization(token, "Engineering Team");
        JsonNode secondOrganization = createOrganization(token, "Engineering Team");
        JsonNode firstWorkspace = createWorkspace(token, firstOrganization.path("id").asLong(), "Platform");
        JsonNode secondWorkspace = createWorkspace(token, secondOrganization.path("id").asLong(), "Platform");

        assertThat(firstOrganization.path("slug").asText()).isEqualTo("engineering-team");
        assertThat(secondOrganization.path("slug").asText()).isEqualTo("engineering-team-2");
        assertThat(firstWorkspace.path("slug").asText()).isEqualTo("platform");
        assertThat(secondWorkspace.path("slug").asText()).isEqualTo("platform-2");
    }

    @Test
    void nonOwnerCannotCreateWorkspaceInOrganization() throws Exception {
        String ownerToken = signUp("org-owner@example.com").path("accessToken").asText();
        String intruderToken = signUp("intruder@example.com").path("accessToken").asText();
        long organizationId = createOrganization(ownerToken, "Private Org").path("id").asLong();

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody("Sneaky Workspace")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You do not have permission to perform this action"));
    }

    @Test
    void memberWithoutAdminRoleCannotAddMembers() throws Exception {
        String ownerToken = signUp("admin-owner@example.com").path("accessToken").asText();
        String memberToken = signUp("plain-member@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(
                ownerToken,
                createOrganization(ownerToken, "Shared Org").path("id").asLong(),
                "Shared Workspace"
        ).path("id").asLong();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("plain-member@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("stranger@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberCannotAddMembers() throws Exception {
        String ownerToken = signUp("closed-owner@example.com").path("accessToken").asText();
        String outsiderToken = signUp("outsider@example.com").path("accessToken").asText();
        long workspaceId = createWorkspace(
                ownerToken,
                createOrganization(ownerToken, "Closed Org").path("id").asLong(),
                "Closed Workspace"
        ).path("id").asLong();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("outsider@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void addingUnknownOrExistingMemberIsRejected() throws Exception {
        String ownerToken = signUp("conflict-owner@example.com").path("accessToken").asText();
        signUp("existing-member@example.com");
        long workspaceId = createWorkspace(
                ownerToken,
                createOrganization(ownerToken, "Conflict Org").path("id").asLong(),
                "Conflict Workspace"
        ).path("id").asLong();

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("ghost@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No registered user matches the provided email"));

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("existing-member@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/members", workspaceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("Existing-Member@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User is already a member of this workspace"));
    }

    @Test
    void unknownOrganizationAndWorkspaceReturnNotFound() throws Exception {
        String token = signUp("missing@example.com").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/organizations/999999/workspaces")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody("Orphan Workspace")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found"));

        mockMvc.perform(post("/api/v1/workspaces/999999/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emailBody("someone@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workspace not found"));
    }

    @Test
    void invalidWorkspaceNameReturnsValidationError() throws Exception {
        String token = signUp("validation@example.com").path("accessToken").asText();
        long organizationId = createOrganization(token, "Validation Org").path("id").asLong();

        mockMvc.perform(post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void unauthenticatedWorkspaceRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody("Anonymous Org")))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode signUp(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "firstName", "Flow",
                                "lastName", "User"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode createOrganization(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/organizations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode createWorkspace(String token, long organizationId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/organizations/{organizationId}/workspaces", organizationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String nameBody(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of("name", name));
    }

    private String emailBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email));
    }
}
