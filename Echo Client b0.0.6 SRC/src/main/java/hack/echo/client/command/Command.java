package hack.echo.client.command;

import lombok.Getter;

@Getter
public abstract class Command {

    private final String name;
    private final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public abstract void execute(String[] args);
}
