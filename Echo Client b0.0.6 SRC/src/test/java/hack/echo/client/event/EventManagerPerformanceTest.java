package hack.echo.client.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EventManagerPerformanceTest {

    private static final int LISTENER_COUNT = 32;
    private static final int WARMUP_POSTS = 25_000;
    private static final int MEASURED_POSTS = 250_000;

    @AfterEach
    void tearDown() {
        EventManager.reset();
    }

    @Test
    void measuresDispatchSpeed() {
        List<BenchmarkListener> listeners = new ArrayList<>(LISTENER_COUNT);
        for (int index = 0; index < LISTENER_COUNT; index++) {
            BenchmarkListener listener = new BenchmarkListener();
            listeners.add(listener);
            EventManager.register(listener);
        }

        BenchmarkEvent event = new BenchmarkEvent();

        for (int iteration = 0; iteration < WARMUP_POSTS; iteration++) {
            EventManager.post(event);
        }

        boolean cancelled = false;
        long start = System.nanoTime();
        for (int iteration = 0; iteration < MEASURED_POSTS; iteration++) {
            cancelled = EventManager.post(event);
        }
        long elapsedNanos = System.nanoTime() - start;
        assertFalse(cancelled);

        int expectedDispatches = (WARMUP_POSTS + MEASURED_POSTS) * LISTENER_COUNT;
        int actualDispatches = listeners.stream().mapToInt(BenchmarkListener::dispatchCount).sum();
        assertEquals(expectedDispatches, actualDispatches);

        double averageNanosPerPost = (double) elapsedNanos / MEASURED_POSTS;
        double postsPerSecond = 1_000_000_000d / averageNanosPerPost;

        System.out.printf(
                Locale.ROOT,
                "Event dispatch benchmark: %d listeners, %.2f ns/post, %.2f posts/sec%n",
                LISTENER_COUNT,
                averageNanosPerPost,
                postsPerSecond
        );
    }

    private static final class BenchmarkListener {

        private int dispatchCount;

        @EventSubscribe
        private void onEvent(BenchmarkEvent event) {
            dispatchCount++;
        }

        private int dispatchCount() {
            return dispatchCount;
        }
    }

    private static final class BenchmarkEvent extends Event {
    }
}
