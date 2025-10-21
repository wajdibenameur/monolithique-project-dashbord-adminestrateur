package tn.iteams.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iteams.entities.Role;
import tn.iteams.entities.TypeRole;

import java.lang.reflect.Type;

@Repository("roleRepository")
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByRole(TypeRole role);
}