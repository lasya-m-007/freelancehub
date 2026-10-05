package com.freelancehub.config;

import com.freelancehub.model.Category;
import com.freelancehub.model.Role;
import com.freelancehub.model.User;
import com.freelancehub.repository.CategoryRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import com.freelancehub.service.ProjectService;
import com.freelancehub.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Idempotent seed: safe to run on every restart. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ProjectService projectService;

    @Value("${app.admin.email}")
    private String adminEmail;
    @Value("${app.admin.password}")
    private String adminPassword;
    @Value("${app.seed-demo:true}")
    private boolean seedDemo;

    public DataSeeder(CategoryRepository categoryRepository, UserRepository userRepository,
                      ProjectRepository projectRepository, UserService userService,
                      ProjectService projectService) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.projectService = projectService;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List.of("Web Development", "Mobile Apps", "Graphic Design", "Content Writing",
                    "Data Science", "Digital Marketing")
                    .forEach(n -> categoryRepository.save(new Category(n)));
            log.info("Seeded default categories");
        }

        if (!userRepository.existsByEmail(adminEmail.toLowerCase())) {
            userService.register("Administrator", adminEmail, adminPassword, Role.ADMIN);
            log.info("Seeded admin account {}", adminEmail);
            if ("Admin@123".equals(adminPassword)) {
                log.warn("Admin is using the DEFAULT password - set ADMIN_PASSWORD before going live!");
            }
        }

        if (seedDemo) {
            User client = userRepository.findByEmail("client@freelancehub.com")
                    .orElseGet(() -> userService.register("Demo Client", "client@freelancehub.com",
                            "Client@123", Role.CLIENT));
            if (!userRepository.existsByEmail("freelancer@freelancehub.com")) {
                userService.register("Demo Freelancer", "freelancer@freelancehub.com",
                        "Freelancer@123", Role.FREELANCER);
            }
            if (projectRepository.count() == 0) {
                Category web = categoryRepository.findAllByOrderByNameAsc().stream()
                        .filter(c -> c.getName().equals("Web Development")).findFirst()
                        .orElse(categoryRepository.findAll().get(0));
                projectService.create(client.getId(), "Portfolio website for a photographer",
                        "Need a clean, responsive 5-page portfolio site with a contact form and image gallery.",
                        new BigDecimal("15000"), LocalDate.now().plusDays(30), web.getId());
                log.info("Seeded demo users and a sample project");
            }
        }
    }
}
