package com.freelancehub.controller.api;

import com.freelancehub.dto.CategoryDto;
import com.freelancehub.dto.CategoryRequest;
import com.freelancehub.model.Role;
import com.freelancehub.security.AuthHelper;
import com.freelancehub.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {

    private final CategoryService categoryService;
    private final AuthHelper auth;

    public CategoryApiController(CategoryService categoryService, AuthHelper auth) {
        this.categoryService = categoryService;
        this.auth = auth;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return categoryService.findAll().stream().map(CategoryDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryRequest req, HttpServletRequest request) {
        auth.requireRole(request, Role.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryDto.from(categoryService.create(req.name())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        auth.requireRole(request, Role.ADMIN);
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
