package com.freelancehub.spec;

import com.freelancehub.model.Project;
import com.freelancehub.model.ProjectStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Combined filters; an unset filter is ignored (never compared against null). */
public final class ProjectSpecs {

    private ProjectSpecs() { }

    public static Specification<Project> filter(Long categoryId, ProjectStatus status, String keyword,
                                                BigDecimal minBudget, BigDecimal maxBudget) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(root.<String>get("description")), like)));
            }
            if (minBudget != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("budget"), minBudget));
            }
            if (maxBudget != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("budget"), maxBudget));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
