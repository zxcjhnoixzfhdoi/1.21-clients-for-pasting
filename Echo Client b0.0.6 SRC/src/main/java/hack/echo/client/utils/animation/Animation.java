package hack.echo.client.utils.animation;

import lombok.Getter;
import lombok.Setter;

public class Animation {

    @Setter
    private Easing easing;
    @Setter
    private double duration;
    private long startTime;

    @Getter
    private double from, to;

    public Animation(final Easing easing, double duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public boolean isFinished() {
        return (System.currentTimeMillis() - startTime) >= duration;
    }

    public void updateTo(double to) {
        if (!this.isFinished()) {
            this.to = to;
        } else {
            this.start(this.to, to);
        }
    }

    public void start(double from, double to) {
        this.from = from;
        this.to = to;
        this.startTime = System.currentTimeMillis();
    }

    public double getDelta() {
        if (isFinished())
            return to;

        long cur = System.currentTimeMillis();
        float elapsed = cur - startTime;
        double progress = elapsed / duration;
        double eased = Math.clamp(easing.getFunction().apply(progress), 0.0, 1.0);
        return from + (to - from) * eased;
    }

}