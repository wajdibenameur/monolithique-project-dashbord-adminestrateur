package tn.iteams.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iteams.entities.Role;
import tn.iteams.repositories.RoleRepository;

import java.util.List;

@Service
public class RoleService {

    @Autowired  // Spring va se charger de créer un objet qui implémente cette interface
    RoleRepository roleRepository ;

    public Role addRole(Role role)
    {
        return roleRepository.save(role);  // sauvegarder dans la base

    }

    public List<Role> listRole()
    {
        return (List<Role>) roleRepository.findAll();	 // lister tous les roles de la base
    }
}
