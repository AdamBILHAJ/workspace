package com.example.team_workspace.workspace.repository;

import java.util.List;
import java.util.Optional;

import com.example.team_workspace.workspace.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByIdAndOwnerId(Long id, Long ownerId);

    List<Organization> findAllByOwnerIdOrderByNameAsc(Long ownerId);

    boolean existsBySlug(String slug);
}
