package we.devs.opium.api.manager.event;

public class Event {
    private boolean cancelled;

    /**
     * Checks if the event has been cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancels the event.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Sets the cancellation state of the event.
     *
     * @param cancelled true to cancel the event, false to un-cancel it.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
