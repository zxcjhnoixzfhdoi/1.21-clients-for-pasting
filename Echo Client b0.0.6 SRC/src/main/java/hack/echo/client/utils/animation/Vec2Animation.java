package hack.echo.client.utils.animation;

import lombok.Setter;
import org.joml.Vector2f;

public class Vec2Animation extends Animation {
    public Vec2Animation(Easing easing, float duration) {
        super(easing, duration);
    }

    private float fromx, fromy;
    @Setter
    private float tox, toy;


    public void start(float fromx, float fromy, float tox, float toy) {
        this.fromx = fromx;
        this.fromy = fromy;
        this.tox = tox;
        this.toy = toy;
        super.start(0f, 1f);
    }

    @Override
    public void updateTo(double to) {

    }

    public void updateToX(float x) {
        if (isFinished()) {
            this.start(this.tox, this.toy, x, this.toy);
        } else {
            this.tox = x;
        }
    }

    public void updateTo(float x, float y) {
        if (isFinished()) {
            this.start(this.tox, this.toy, x, y);
        } else {
            this.tox = x;
            this.toy = y;
        }
    }

    public float getX() {
        return (float) (fromx + (tox - fromx) * super.getDelta());
    }

    public float getY() {
        return (float) (fromy + (toy - fromy) * super.getDelta());
    }
}
