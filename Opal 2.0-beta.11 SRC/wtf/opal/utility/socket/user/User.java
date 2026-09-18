package wtf.opal.utility.socket.user;

public class User {

    private final String name;
    private final UserRole role;

    User(final String name, final UserRole role) {
        if (name == null || role == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return this.name;
    }

    public UserRole getRole() {
        return this.role;
    }

}
