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
import tn.iteams.entities.TypeRole;
import tn.iteams.entities.User;
import tn.iteams.repositories.RoleRepository;
import tn.iteams.repositories.UserRepository;
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
        if (users.isEmpty())
            users = null;
        model.addAttribute("users", users);
        model.addAttribute("nbr", nbr);
        model.addAttribute("allRoles", TypeRole.values());
        return "user/listUsers";
    }

    @GetMapping("enable/{id}/{email}")
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
    public String disableUserAcount(@PathVariable("id") Long id,
                                    @PathVariable("email") String email, RedirectAttributes redirectAttributes) {
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
                                 @RequestParam("newrole") String newRoleName,
                                 RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));

        // Obtenir l'ancien rôle (s'il existe)
        String oldRole = (user.getRole() != null) ? user.getRole().getRole().name() : "Aucun";

        // Trouver et assigner le nouveau rôle
        TypeRole typeRole = TypeRole.valueOf(newRoleName);
        Role newRole = roleRepository.findByRole(typeRole);
        user.setRole(newRole);

        userRepository.save(user);

        sendEmail(user.getEmail(), "role_change", newRoleName);

        redirectAttributes.addFlashAttribute("message",
                "Rôle modifié de [" + oldRole + "] à [" + newRoleName + "]");
        System.out.println(">>> Email envoyé avec succès modification rôle : " + user.getEmail());

        return "redirect:list";
    }

    @GetMapping("add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", TypeRole.values());
        return "user/addUser";
    }

    @PostMapping("add")
    public String addUser(
            @ModelAttribute @Valid User user,
            BindingResult bindingResult,
            Model model,
            @RequestParam("roleType") String roleName,
            RedirectAttributes redirectAttributes) {

        try {
            // les rôles au modèle affiche
            model.addAttribute("allRoles", TypeRole.values());
            System.out.println(">>> roleName reçu = " + roleName);

            // Validation des champs
            if (bindingResult.hasErrors()) {
                System.out.println(">>> Erreurs de validation détectées : " + bindingResult.getAllErrors());
                return "user/addUser";
            }

            // Vérifier  l'email
            if (userRepository.findByEmail(user.getEmail()) != null) {
                bindingResult.rejectValue("email", "error.user", "Cet email est déjà utilisé.");
                return "user/addUser";
            }

            // Validation du rôle (obligatoire)
            if (roleName == null || roleName.isEmpty()) {
                model.addAttribute("error", "Veuillez sélectionner un rôle.");
                return "user/addUser";
            }

            // Encoder le mot de passe
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // Gestion du rôle
            try {
                System.out.println(">>> Parsing rôle : " + roleName);
                TypeRole typeRole = TypeRole.valueOf(roleName);

                System.out.println(">>> TypeRole converti : " + typeRole);
                Role role = roleRepository.findByRole(typeRole);

                System.out.println(">>> Rôle trouvé : " + role);

                if (role == null) {
                    System.out.println(">>> Création d'un nouveau rôle");
                    role = new Role(typeRole);
                    roleRepository.save(role);
                    System.out.println(">>> Nouveau rôle sauvegardé");
                }

                user.setRole(role);

            } catch (IllegalArgumentException e) {
                model.addAttribute("error", "Rôle invalide : " + roleName);
                return "user/addUser";
            }


            user.setActive(0);

            // Sauvegarder l'utilisateur
            userRepository.save(user);

            // Envoyer un email de notification
            sendEmail(user.getEmail(), "create", "Bienvenue sur notre plateforme !");
            System.out.println(">>> Email envoyé avec succès à : " + user.getEmail());

            redirectAttributes.addFlashAttribute("message",
                    "Utilisateur créé avec succès ! Un email a été envoyé.");

            return "redirect:/accounts/list";

        } catch (Exception e) {
            System.err.println("Erreur lors de la création : " + e.getMessage());
            model.addAttribute("error", "Erreur technique : " + e.getMessage());
            return "user/addUser";
        }
    }

    @GetMapping("edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", TypeRole.values());
        return "user/editUser";
    }

    @PostMapping("edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User user,
                             @RequestParam(value = "roleType") String roleName,
                             RedirectAttributes redirectAttributes) {

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id:" + id));

        existingUser.setName(user.getName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setActive(user.getActive());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Assign le nouveau rôle
        TypeRole typeRole = TypeRole.valueOf(roleName);
        Role role = roleRepository.findByRole(typeRole);
        existingUser.setRole(role);

        userRepository.save(existingUser);
        sendEmail(existingUser.getEmail(), "update", null);
        redirectAttributes.addFlashAttribute("message", "Utilisateur modifié avec succès un mail information envoyé !");
        System.out.println(">>> Email envoyé avec succès update compte : " + existingUser.getEmail());

        return "redirect:/accounts/list";
    }

    @GetMapping("delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid User Id: " + id));

        // Préparer le rôle pour le mail
        String roleName = (user.getRole() != null) ? user.getRole().getRole().name() : "Aucun rôle";

        // Envoi l'email de notification
        sendEmail(user.getEmail(), "delete_user", roleName);
        System.out.println(">>> Email envoyé avec succès de suppression de compte : " + user.getEmail());

        // Suppr user
        userRepository.delete(user);
        redirectAttributes.addFlashAttribute("message", "Utilisateur supprimé avec succès !");

        return "redirect:../list";
    }

    // Méthode sendEmail inchangée
    void sendEmail(String email, String actionType, String additionalInfo) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        String baseUrl = "http://127.0.0.1:81";

        switch (actionType) {
            case "mail_activation":
                msg.setSubject("Activation de compte");
                msg.setText("Votre compte a été activé avec succès !");
                baseUrl = baseUrl + "/activation";
                break;

            case "mail_deactivation":
                msg.setSubject("Désactivation de compte");
                msg.setText("Votre compte a été désactivé.");
                baseUrl = baseUrl + "/deactivation";
                break;

            case "role_change":
                msg.setSubject("Changement de rôle");
                msg.setText("Votre rôle a été modifié vers : " + additionalInfo);
                baseUrl = baseUrl + "/change";
                break;

            case "create":
                msg.setSubject("Création de compte");
                msg.setText("Votre compte a été créé avec succès !\n"
                        + "Identifiant : " + email + "\n"
                        + "Mot de passe : " + additionalInfo);
                baseUrl = baseUrl + "/create";
                break;

            case "update":
                msg.setSubject("Mise à jour de compte");
                msg.setText("Vos informations de compte ont été mises à jour.");
                baseUrl = baseUrl + "/update";
                break;

            case "delete_user":
                msg.setSubject("Suppression de compte");
                msg.setText("Votre compte a été supprimé. Rôle : " + additionalInfo);
                baseUrl = baseUrl + "/delete";
                break;

            default:
                msg.setSubject("Notification");
                msg.setText("Une action a été effectuée sur votre compte.");
                baseUrl = baseUrl + "/";
                break;
        }

        javaMailSender.send(msg);
    }
}