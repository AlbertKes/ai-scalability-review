package app.aiscalabilityreview.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks the runs this process has in flight.
 *
 * <p>Two jobs it does that nothing used to: it keeps a handle on the Gemini CLI subprocess so a run
 * can actually be stopped — a fifteen-minute run started against the wrong environment was
 * previously fifteen minutes of tokens nobody could reclaim — and it serialises runs so that only
 * {@code max_concurrent_runs} are active at once, with the rest visibly queued rather than racing.
 */
public class LocalReviewRunRegistry {
    private final Logger logger = LoggerFactory.getLogger(LocalReviewRunRegistry.class);
    private final Map<String, RunHandle> handles = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition slotFree = lock.newCondition();
    private final Deque<String> waiting = new ArrayDeque<>();
    private int running;

    public RunHandle register(String jobId, String serviceId) {
        RunHandle handle = new RunHandle(serviceId);
        handles.put(jobId, handle);
        return handle;
    }

    public void unregister(String jobId) {
        handles.remove(jobId);
    }

    public Optional<RunHandle> handle(String jobId) {
        return Optional.ofNullable(handles.get(jobId));
    }

    public boolean isTracked(String jobId) {
        return handles.containsKey(jobId);
    }

    /** True when this service already has a run in flight — the single-flight rule of FR-1.10. */
    public boolean hasRunFor(String serviceId) {
        return handles.values().stream().anyMatch(handle -> handle.serviceId.equals(serviceId));
    }

    /**
     * Marks the run cancelled and kills its subprocess if one is attached.
     *
     * @return false when the run is not tracked by this process, so the caller can fall back to
     *         marking a stale document instead of pretending it stopped something
     */
    public boolean cancel(String jobId) {
        RunHandle handle = handles.get(jobId);
        if (handle == null) return false;
        handle.cancelled = true;
        removeFromQueue(jobId);
        Process process = handle.process;
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            logger.info("Destroyed the Gemini CLI subprocess of cancelled run {}", jobId);
        }
        return true;
    }

    /** Blocks until a slot is free and this run is at the head of the queue. */
    public void acquireSlot(String jobId, int maxConcurrent) throws InterruptedException {
        int limit = Math.max(1, maxConcurrent);
        lock.lock();
        try {
            waiting.addLast(jobId);
            while (running >= limit || !jobId.equals(waiting.peekFirst())) {
                slotFree.await();
            }
            waiting.removeFirst();
            running++;
        } finally {
            lock.unlock();
        }
    }

    public void releaseSlot() {
        lock.lock();
        try {
            running = Math.max(0, running - 1);
            slotFree.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** 1-based position in the queue, or 0 when the run is not waiting. */
    public int queuePosition(String jobId) {
        lock.lock();
        try {
            int position = 1;
            for (String waitingJobId : waiting) {
                if (waitingJobId.equals(jobId)) return position;
                position++;
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    private void removeFromQueue(String jobId) {
        lock.lock();
        try {
            if (waiting.remove(jobId)) slotFree.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static final class RunHandle {
        final String serviceId;
        private volatile Process process;
        private volatile boolean cancelled;

        RunHandle(String serviceId) {
            this.serviceId = serviceId;
        }

        /** Called as soon as a stage's subprocess starts, so a cancel arriving later can reach it. */
        public void attach(Process process) {
            this.process = process;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
