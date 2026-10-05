package com.freelancehub.repository;

import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    List<Project> findByClientIdOrderByCreatedAtDesc(Long clientId);

    long countByStatus(ProjectStatus status);

    boolean existsByCategoryId(Long categoryId);

    /** Row lock: serialises accept-bid / place-bid / close on the same project. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") Long id);
}
