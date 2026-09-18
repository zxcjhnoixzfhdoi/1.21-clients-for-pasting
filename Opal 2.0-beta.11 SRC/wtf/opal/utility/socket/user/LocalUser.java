package wtf.opal.utility.socket.user;

public final class LocalUser extends User {

    private final int numericId;

    public LocalUser(final int numericId, final String name, final UserRole role) {
        super(name, role);
        if (numericId < 1) {
            throw new IllegalArgumentException();
        }
        this.numericId = numericId;
    }

    public int getNumericId() {
        return numericId;
    }

}
