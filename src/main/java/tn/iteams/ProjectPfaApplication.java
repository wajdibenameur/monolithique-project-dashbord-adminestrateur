package tn.iteams;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import tn.iteams.entities.Role;
import tn.iteams.entities.TypeRole;
import tn.iteams.entities.User;
import tn.iteams.repositories.RoleRepository;
import tn.iteams.services.UserService;

import java.util.Arrays;

@SpringBootApplication
public class ProjectPfaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectPfaApplication.class, args);
    }
    //@Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            Arrays.stream(TypeRole.values()).forEach(type -> {
                if (roleRepository.findByRole(type) == null) {
                    roleRepository.save(new Role(type));
                }
            });
        };
    }

    @Bean
    CommandLineRunner run(UserService userService, RoleRepository roleRepository) {
        return args -> {
            if (userService.findUserByEmail("ali1@gmail.com") == null) {
                Role teacherRole = roleRepository.findByRole(TypeRole.ROLE_TEACHER);
                User us1 = User.builder()
                        .email("ali1@gmail.com")
                        .password("123456")
                        .name("Ali")
                        .lastName("Salah")
                        .active(1)
                        .role(teacherRole)
                        .build();
                userService.saveUser(us1);
            }

            if (userService.findUserByEmail("mahmoud1@gmail.com") == null) {
                Role studentRole = roleRepository.findByRole(TypeRole.ROLE_STUDENT);
                User us2 = User.builder()
                        .email("mahmoud@gmail1.com")
                        .password("123456")
                        .name("Mahmoud")
                        .lastName("Mohsen")
                        .active(1)
                        .role(studentRole)
                        .build();
                userService.saveUser(us2);
            }

        };
    }



}
