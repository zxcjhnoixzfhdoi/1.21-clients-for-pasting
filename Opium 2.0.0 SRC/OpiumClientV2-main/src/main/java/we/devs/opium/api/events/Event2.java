package we.devs.opium.api.events;

public class Event2 {
    private final Stage stage;
    private boolean cancel;
    public Event2(Stage stage) {
        this.cancel = false;
        this.stage = stage;
    }

    public void cancel() {
        setCancelled(true);
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isCancelled() {
        return cancel;
    }
    public Stage getStage() {
        return stage;
    }

    public boolean isPost() {
        return stage == Stage.Post;
    }

    public boolean isPre() {
        return stage == Stage.Pre;
    }

    public enum Stage{
        Pre, Post
    }
}
