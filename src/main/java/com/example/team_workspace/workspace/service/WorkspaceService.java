package com.example.team_workspace.workspace.service;

import java.util.List;
import java.util.Locale;

import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import com.example.team_workspace.workspace.domain.Organization;
import com.example.team_workspace.workspace.domain.Workspace;
import com.example.team_workspace.workspace.domain.WorkspaceMember;
import com.example.team_workspace.workspace.domain.WorkspaceRole;
import com.example.team_workspace.workspace.dto.AddMemberRequest;
import com.example.team_workspace.workspace.dto.CreateWorkspaceRequest;
import com.example.team_workspace.workspace.dto.MemberResponse;
import com.example.team_workspace.workspace.dto.OrganizationResponse;
import com.example.team_workspace.workspace.dto.WorkspaceResponse;
import com.example.team_workspace.workspace.exception.OrganizationNotFoundException;
import com.example.team_workspace.workspace.exception.WorkspaceAccessDeniedException;
import com.example.team_workspace.workspace.exception.WorkspaceMemberAlreadyExistsException;
import com.example.team_workspace.workspace.exception.WorkspaceNotFoundException;
import com.example.team_workspace.workspace.exception.WorkspaceUserNotFoundException;
import com.example.team_workspace.workspace.repository.OrganizationRepository;
import com.example.team_workspace.workspace.repository.WorkspaceMemberRepository;
import com.example.team_workspace.workspace.repository.WorkspaceRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private static final String ORGANIZATION_SLUG_FALLBACK = "organization";
    private static final String WORKSPACE_SLUG_FALLBACK = "workspace";

    private final OrganizationRepository organizationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final SlugService slugService;
    private final MembershipGuard membershipGuard;

    public WorkspaceService(
            OrganizationRepository organizationRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            SlugService slugService,
            MembershipGuard membershipGuard
    ) {
        this.organizationRepository = organizationRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.slugService = slugService;
        this.membershipGuard = membershipGuard;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long workspaceId, User actor) {
        membershipGuard.requireMembership(workspaceId, actor);
        return workspaceMemberRepository.findAllByWorkspaceIdOrderByIdAsc(workspaceId)
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationResponse createOrganization(String name, User actor) {
        User owner = requireExistingUser(actor);
        String slug = slugService.generateUnique(
                name,
                ORGANIZATION_SLUG_FALLBACK,
                organizationRepository::existsBySlug
        );
        Organization organization = organizationRepository.save(new Organization(name, slug, owner));
        return OrganizationResponse.from(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations(User actor) {
        User currentUser = requireExistingUser(actor);
        return organizationRepository.findAllByOwnerIdOrderByNameAsc(currentUser.getId())
                .stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional
    public WorkspaceResponse createWorkspace(
            Long organizationId,
            CreateWorkspaceRequest request,
            User actor
    ) {
        User currentUser = requireExistingUser(actor);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(OrganizationNotFoundException::new);

        if (!organization.getOwner().getId().equals(currentUser.getId())) {
            throw new WorkspaceAccessDeniedException();
        }

        String slug = slugService.generateUnique(
                request.name(),
                WORKSPACE_SLUG_FALLBACK,
                workspaceRepository::existsBySlug
        );
        Workspace workspace = workspaceRepository.save(new Workspace(request.name(), slug, organization));
        workspaceMemberRepository.save(new WorkspaceMember(workspace, currentUser, WorkspaceRole.OWNER));

        return WorkspaceResponse.from(workspace, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(User actor) {
        User currentUser = requireExistingUser(actor);
        return workspaceRepository.findAllWithRoleByUserId(currentUser.getId())
                .stream()
                .map(row -> WorkspaceResponse.from((Workspace) row[0], (WorkspaceRole) row[1]))
                .toList();
    }

    @Transactional
    public MemberResponse addMember(Long workspaceId, AddMemberRequest request, User actor) {
        User currentUser = requireExistingUser(actor);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(WorkspaceNotFoundException::new);

        WorkspaceRole actorRole = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, currentUser.getId())
                .map(WorkspaceMember::getRole)
                .orElseThrow(WorkspaceAccessDeniedException::new);

        if (actorRole != WorkspaceRole.OWNER && actorRole != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException();
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User invitee = userRepository.findByEmail(email)
                .orElseThrow(WorkspaceUserNotFoundException::new);

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitee.getId())) {
            throw new WorkspaceMemberAlreadyExistsException();
        }

        WorkspaceMember member = workspaceMemberRepository.save(
                new WorkspaceMember(workspace, invitee, WorkspaceRole.MEMBER)
        );
        return MemberResponse.from(member);
    }

    private User requireExistingUser(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
    }
}
