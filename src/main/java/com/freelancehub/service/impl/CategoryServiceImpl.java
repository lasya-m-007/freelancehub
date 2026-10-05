package com.freelancehub.service.impl;

import com.freelancehub.exception.BusinessRuleException;
import com.freelancehub.exception.DuplicateResourceException;
import com.freelancehub.exception.ResourceNotFoundException;
import com.freelancehub.model.Category;
import com.freelancehub.repository.CategoryRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.service.CategoryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProjectRepository projectRepository) {
        this.categoryRepository = categoryRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() { return categoryRepository.findAllByOrderByNameAsc(); }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
    }

    @Override
    @Transactional
    public Category create(String name) {
        if (name == null || name.isBlank()) throw new BusinessRuleException("Category name is required");
        String trimmed = name.trim();
        if (trimmed.length() > 80) throw new BusinessRuleException("Category name is too long");
        if (categoryRepository.existsByNameIgnoreCase(trimmed)) {
            throw new DuplicateResourceException("Category already exists");
        }
        return categoryRepository.save(new Category(trimmed));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (projectRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Category is used by existing projects and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() { return categoryRepository.count(); }
}
