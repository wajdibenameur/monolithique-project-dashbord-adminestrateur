
package tn.iteams.controllers;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tn.iteams.entities.Role;
import tn.iteams.entities.User;
import tn.iteams.repositories.RoleRepository;
import tn.iteams.repositories.UserRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/accounts/")

public class AccountController {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    public AccountController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping("list")
    public String listUsers(Model model) {

        List<User> users = (List<User>) userRepository.findAll();
        long nbr = userRepository.count();
        if (users.size() == 0)
            users = null;
        model.addAttribute("users", users);
        model.addAttribute("nbr", nbr);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/listUsers";
    }

    @GetMapping("enable/{id}/{email}")
    //@ResponseBody
    public String enableUserAcount(@PathVariable("id") Long id,
                                   @PathVariable("email") String email, RedirectAttributes redirectAttributes) {


        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        user.setActive(1);
        userRepository.save(user);
        sendEmail(user.getEmail(), "mail_activation", email);
        redirectAttributes.addFlashAttribute("message", "Compte activé avec succès !");
        System.out.println(">>> Email envoyé avec succès à  activation compte: " + user.getEmail());
        return "redirect:../../list";
    }

    @GetMapping("disable/{id}/{email}")
    //@ResponseBody
    public String disableUserAcount(@PathVariable("id") Long id,
                                    @PathVariable("email") String email,RedirectAttributes redirectAttributes) {


        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        user.setActive(0);
        userRepository.save(user);
        sendEmail(user.getEmail(), "mail_deactivation", email);
        redirectAttributes.addFlashAttribute("message", "Compte desactivé avec succès !");
        System.out.println(">>> Email envoyé avec succès desactivation compte : " + user.getEmail());
        return "redirect:../../list";
    }


    @PostMapping("updateRole")
    public String UpdateUserRole(@RequestParam("id") Long id,
                                 @RequestParam("newrole") String newRole,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        String oldRole = user.getRoles().isEmpty() ? "Aucun" :
                user.getRoles().iterator().next().getRole();
        Role userRole = roleRepository.findByRole(newRole);
        user.setRoles(new HashSet<>(Arrays.asList(userRole)));
        userRepository.save(user);
        sendEmail(user.getEmail(), "role_change", newRole);
        redirectAttributes.addFlashAttribute("message", "Rôle modifié de " + oldRole + " à " + newRole);
        System.out.println(">>> Email envoyé avec succès modification role : " + user.getEmail());
        return "redirect:list";
    }
    @GetMapping("add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/addUser"; // page Thymeleaf à créer
    }

    @PostMapping("add")
    public String addUser(
            @ModelAttribute @Valid User user,
            BindingResult bindingResult,
            Model model,
            @RequestParam("role") String roleName,
            RedirectAttributes redirectAttributes)
    {
        // Vérifier erreurs de validation classiques
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleRepository.findAll());
            return "user/addUser"; // Affiche le formulaire avec erreurs
        }

        // Vérifier si l'email existe déjà
        if (userRepository.findByEmail(user.getEmail()) != null) {
            bindingResult.rejectValue("email", "error.user", "Cet email est déjà utilisé.");
            model.addAttribute("allRoles", roleRepository.findAll());
            return "user/addUser"; // Retour au formulaire avec message d'erreur
        }

        // Encoder le mot de passe
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Récupérer et attribuer le rôle
        Role role = roleRepository.findByRole(roleName);
        user.setRoles(new HashSet<>(Arrays.asList(role)));

        // Sauvegarder l'utilisateur
        userRepository.save(user);

        // Envoyer l'email de notification
        sendEmail(user.getEmail(), "create", "motDePasseTemporaire");
        System.out.println(">>> Email envoyé avec succès à : " + user.getEmail());
        redirectAttributes.addFlashAttribute("message", "Utilisateur créé et mail envoyé !");

        return "redirect:/accounts/list";
    }

    @GetMapping("edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "user/editUser"; // page Thymeleaf à créer
    }

    @PostMapping("edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             @RequestParam(value = "roles", required = false) List<String> roleNames,RedirectAttributes redirectAttributes) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));

        existingUser.setName(user.getName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        user.setRoles(user.getRoles());

        // Garder l'ancien mot de passe s'il n'a pas été modifié
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Gérer les rôles
        if (roleNames != null && !roleNames.isEmpty()) {
            HashSet<Role> roles = new HashSet<>();
            for (String roleName : roleNames) {
                Role r = roleRepository.findByRole(roleName);
                if (r != null) roles.add(r);
            }
            existingUser.setRoles(roles);
        }

        existingUser.setActive(user.getActive());

        userRepository.save(existingUser);
        sendEmail(existingUser.getEmail(), "update", null);
        redirectAttributes.addFlashAttribute("message", "Utilisateur modifié avec succès !");
        System.out.println(">>> Email envoyé avec succès update compte : " + user.getEmail());
        return "redirect:/accounts/list";
    }


    @GetMapping("delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes ) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        sendEmail(user.getEmail(), "delete_user", user.getRoles().iterator().next().getRole());
        userRepository.delete(user);
        System.out.println(">>> Email envoyé avec succès de suprime compte : " + user.getEmail());
        redirectAttributes.addFlashAttribute("message", "Utilisateur supprimé avec succès !");
        System.out.println(">>> Email envoyé avec succès Compte supprimé  : " + user.getEmail());

        return "redirect:../list";
    }


    // Modifier la méthode sendEmail
    void sendEmail(String email, String actionType, String additionalInfo) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        String baseUrl = "http://127.0.0.1:81";

        switch (actionType) {
            case "mail_activation":
                msg.setSubject("Activation de compte");
                msg.setText("Votre compte a été activé avec succès !");
                break;

            case "mail_deactivation":
                msg.setSubject("Désactivation de compte");
                msg.setText("Votre compte a été désactivé.");
                break;

            case "role_change":
                msg.setSubject("Changement de rôle");
                msg.setText("Votre rôle a été modifié vers : " + additionalInfo);
                break;

            case "create":
                msg.setSubject("Création de compte");
                msg.setText("Votre compte a été créé avec succès !\n"
                        + "Identifiant : " + email + "\n"
                        + "Mot de passe : " + additionalInfo);
                break;

            case "update":
                msg.setSubject("Mise à jour de compte");
                msg.setText("Vos informations de compte ont été mises à jour.");
                break;

            case "delete_user":
                msg.setSubject("Suppression de compte");
                msg.setText("Votre compte a été supprimé. Rôle : " + additionalInfo);
                break;

            default:
                msg.setSubject("Notification");
                msg.setText("Une action a été effectuée sur votre compte.");
                break;
        }

        javaMailSender.send(msg);
    }

}
//    void sendEmail(String email, boolean state) {
//
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setTo(email);
//        if(state == true)
//        {
//            msg.setSubject("Account Has Been Activated");
//            msg.setText("Hello, Your account has been activated. "
//                    +
//                    "You can log in : http://127.0.0.1:81/login"
//                    + " \n Best Regards!");
//        }
//        else
//        {
//            msg.setSubject("Account Has Been disactivated");
//            msg.setText("Hello, Your account has been disactivated.");
//        }
//        javaMailSender.send(msg);

