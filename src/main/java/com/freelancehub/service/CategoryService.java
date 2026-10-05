package com.freelancehub.service;

import com.freelancehub.model.Category;
import java.util.List;

public interface CategoryService {
    List<Category> findAll();
    Category findById(Long id);
    Category create(String name);
    void delete(Long id);
    long count();
}
