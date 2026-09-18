package hack.echo.client.utils.animation;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

import static java.lang.Math.*;

@Getter
public enum Easing {
    LINEAR(0, x -> x),
    EASE_IN_SINE(1, x -> (1 - cos(x * PI / 2))),
    EASE_OUT_SINE(2, x -> sin(x * PI / 2)),
    EASE_IN_OUT_SINE(3, x -> (-(cos(PI * x) - 1) / 2)),
    EASE_IN_QUAD(4, x -> x * x),
    EASE_OUT_QUAD(5, x -> 1 - (1 - x) * (1 - x)),
    EASE_IN_OUT_QUAD(6, x -> x < 0.5f ? 2 * x * x : (1 - pow(-2 * x + 2, 2) / 2)),
    EASE_IN_CUBIC(7, x -> x * x * x),
    EASE_OUT_CUBIC(8, x -> (1 - pow(1 - x, 3))),
    EASE_IN_OUT_CUBIC(9, x -> (x < 0.5 ? 4 * x * x * x : 1 - pow(-2 * x + 2, 3) / 2)),
    EASE_IN_QUART(10, x -> x * x * x * x),
    EASE_OUT_QUART(11, x -> (1 - pow(1 - x, 4))),
    EASE_IN_OUT_QUART(12, x -> (x < 0.5 ? 8 * x * x * x * x : 1 - pow(-2 * x + 2, 4) / 2)),
    EASE_IN_QUINT(13, x -> x * x * x * x * x),
    EASE_OUT_QUINT(14, x -> (1 - pow(1 - x, 5))),
    EASE_IN_OUT_QUINT(15, x -> (x < 0.5 ? 16 * x * x * x * x * x : 1 - pow(-2 * x + 2, 5) / 2)),
    EASE_IN_EXPO(16, x -> x == 0 ? 0 : pow(2, 10 * x - 10)),
    EASE_OUT_EXPO(17, x -> x == 1 ? 1 : (1 - pow(2, -10 * x))),
    EASE_IN_OUT_EXPO(18, x -> x == 0 ? 0 : x == 1 ? 1 : (x < 0.5 ? pow(2, 20 * x - 10) / 2 : (2 - pow(2, -20 * x + 10)) / 2)),
    EASE_IN_CIRC(19, x -> (1 - sqrt(1 - pow(x, 2)))),
    EASE_OUT_CIRC(20, x -> sqrt(1 - pow(x - 1, 2))),
    EASE_IN_OUT_CIRC(21, x -> (x < 0.5 ? (1 - sqrt(1 - pow(2 * x, 2))) / 2 : (sqrt(1 - pow(-2 * x + 2, 2)) + 1) / 2)),
    EASE_IN_BACK(22, x -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    }),
    EASE_OUT_BACK(23, x -> {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        return (1 + c3 * pow(x - 1, 3) + c1 * pow(x - 1, 2));
    }),
    EASE_IN_OUT_BACK(24, x -> {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;
        return (x < 0.5
                ? (pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
                : (pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2);
    }),
    EASE_IN_ELASTIC(25, x -> {
        double c4 = ((2 * PI) / 3);
        return x == 0 ? 0 : x == 1 ? 1 : (-pow(2, 10 * x - 10) * sin((x * 10 - 10.75) * c4));
    }),
    EASE_OUT_ELASTIC(26, x -> {
        double c4 = ((2 * PI) / 3);
        return x == 0 ? 0 : x == 1 ? 1 : (pow(2, -10 * x) * sin((x * 10 - 0.75) * c4) + 1);
    }),
    EASE_IN_OUT_ELASTIC(27, x -> {
        double c5 = ((2 * PI) / 4.5);
        double sin = sin((20 * x - 11.125) * c5);
        return x == 0 ? 0 : x == 1 ? 1 : (x < 0.5
                ? -(pow(2, 20 * x - 10) * sin) / 2
                : (pow(2, -20 * x + 10) * sin) / 2 + 1);
    }),
    EASE_IN_BOUNCE(28, x -> {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5 / d1) * x + 0.75;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25 / d1) * x + 0.9375;
        } else {
            return n1 * (x -= 2.625 / d1) * x + 0.984375;
        }
    }),
    EASE_OUT_BOUNCE(29, x -> {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5f / d1) * x + 0.75;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25f / d1) * x + 0.9375;
        } else {
            return n1 * (x -= 2.625f / d1) * x + 0.984375;
        }
    }),
    EASE_IN_OUT_BOUNCE(30, x -> x < 0.5f
            ? (1 - EASE_OUT_BOUNCE.getFunction().apply(1 - 2 * x)) / 2
            : (1 + EASE_OUT_BOUNCE.getFunction().apply(2 * x - 1)) / 2);

    private final int index;
    private final Function<Double, Double> function;

    Easing(final int index, final Function<Double, Double> function) {
        this.index = index;
        this.function = function;
    }

}