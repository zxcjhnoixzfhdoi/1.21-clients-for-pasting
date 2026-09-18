package wtf.opal.utility.socket.user;

import net.minecraft.text.TextColor;

public enum UserRole {

    DEVELOPER("developer", 0xFF8583FF),
    BETA("beta", 0xFF1ABC9C),
    USER("user", 0xFF55FFFF);

    private final String name;
    private final TextColor color;
    private final int argb;

    UserRole(final String name, final int argb) {
        this.name = name;
        this.argb = argb;
        this.color = TextColor.fromRgb(argb);
    }

    public static UserRole fromName(final String name) {
        for (final UserRole role : values()) {
            if (role.name.equals(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getName() {
        return this.name;
    }

    public TextColor getColor() {
        return this.color;
    }

    /**
     * TextColor#getRgb returns the 24-bit version of the RGB value, which does not have an alpha channel (0).
     *
     * @return The full 32-bit ARGB value.
     */
    public int getArgb() {
        return this.argb;
    }

}
