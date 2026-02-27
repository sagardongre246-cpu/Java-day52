import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class AdaptiveThreadEngine {

    private ThreadPoolExecutor executor;
    private final Queue<Long> executionHistory = new LinkedList<>();
    private final int HISTORY_LIMIT = 20;
    private final int MIN_THREADS = 2;
    private final int MAX_THREADS = 10;

    public AdaptiveThreadEngine() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MIN_THREADS);
    }

    public void submitTask(Runnable task) {
        executor.submit(() -> {
            long start = System.nanoTime();
            task.run();
            long end = System.nanoTime();

            long duration = end - start;
            recordExecution(duration);
            adjustThreadPool();
        });
    }

    private synchronized void recordExecution(long duration) {
        if (executionHistory.size() >= HISTORY_LIMIT) {
            executionHistory.poll();
        }
        executionHistory.add(duration);
    }

    private synchronized void adjustThreadPool() {
        if (executionHistory.size() < HISTORY_LIMIT) return;

        long avgTime = (long) executionHistory.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        int currentSize = executor.getCorePoolSize();

        // If average execution time high → increase threads
        if (avgTime > 50_000_000 && currentSize < MAX_THREADS) {
            executor.setCorePoolSize(currentSize + 1);
            System.out.println("⚡ Increasing threads to: " + (currentSize + 1));
        }

        // If average execution time low → decrease threads
        else if (avgTime < 20_000_000 && currentSize > MIN_THREADS) {
            executor.setCorePoolSize(currentSize - 1);
            System.out.println("🔻 Decreasing threads to: " + (currentSize - 1));
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}

public class SelfTuningTaskEngine {

    public static void main(String[] args) throws InterruptedException {

        AdaptiveThreadEngine engine = new AdaptiveThreadEngine();
        Random random = new Random();
        AtomicInteger taskCounter = new AtomicInteger();

        // Simulating dynamic workload
        for (int i = 0; i < 100; i++) {
            engine.submitTask(() -> {
                try {
                    int workload = random.nextInt(100);
                    Thread.sleep(workload); // Simulated variable workload
                    System.out.println("Task " + taskCounter.incrementAndGet()
                            + " executed with load: " + workload + " ms");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread.sleep(50);
        }

        Thread.sleep(5000);
        engine.shutdown();
    }
}