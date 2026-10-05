package com.freelancehub.repository;

import com.freelancehub.model.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Category> findAllByOrderByNameAsc();
}
