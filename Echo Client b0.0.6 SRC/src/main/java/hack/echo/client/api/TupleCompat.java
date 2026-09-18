package hack.echo.client.api;

public final class TupleCompat<A, B> {
    private final A a;
    private final B b;

    public TupleCompat(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }
}
