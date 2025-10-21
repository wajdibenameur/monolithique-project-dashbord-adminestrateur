package tn.iteams.entities;

public enum TypeRole {
    ROLE_ADMIN("ADMIN"),
    ROLE_SUPERADMIN("SUPERADMIN"),
    ROLE_STUDENT("ETUDIANT"),
    ROLE_TEACHER("ENSEIGNANT"),
    ROLE_USER("USER");

    private final String roleName;

    TypeRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDisplayName() {
        return switch (this) {
            case ROLE_ADMIN -> "Administrateur";
            case ROLE_STUDENT -> "Étudiant";
            case ROLE_TEACHER -> "Enseignant";
            case ROLE_USER -> "Utilisateur";
            case ROLE_SUPERADMIN -> "Super administrateur";
        };
    }


    public static TypeRole fromRoleName(String roleName) {
        for (TypeRole role : values()) {
            if (role.roleName.equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rôle inconnu : " + roleName);
    }
}