import java.lang.management.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.LockSupport;

public class JvmLab {
    private static final List<Object> KEEP = new ArrayList<>();
    private static final long MB = 1024L * 1024L;

    static class BigObject {
        private final byte[] payload;

        BigObject(int mb) {
            this.payload = new byte[mb * 1024 * 1024];
        }
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "idle" : args[0];

        System.out.println("PID = " + ProcessHandle.current().pid());
        System.out.println("mode = " + mode);
        System.out.println("availableProcessors = " + Runtime.getRuntime().availableProcessors());
        System.out.println("maxMemory = " + Runtime.getRuntime().maxMemory() / MB + " MB");

        switch (mode) {
            case "idle" -> idle();
            case "heap-oom" -> heapOom();
            case "threads" -> {
                int count = args.length >= 2 ? Integer.parseInt(args[1]) : 300;
                manyThreads(count);
            }
            default -> {
                System.out.println("Usage:");
                System.out.println("  java JvmLab idle");
                System.out.println("  java JvmLab heap-oom");
                System.out.println("  java JvmLab threads 300");
            }
        }
    }

    private static void idle() throws Exception {
        while (true) {
            report("idle");
            Thread.sleep(3000);
        }
    }

    private static void heapOom() throws Exception {
        int allocatedMb = 0;

        while (true) {
            KEEP.add(new BigObject(1));
            allocatedMb += 1;

            if (allocatedMb % 5 == 0) {
                report("allocated about " + allocatedMb + " MB in BigObject");
            }

            Thread.sleep(1000);
        }
    }

    private static void manyThreads(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            Thread t = new Thread(() -> LockSupport.park(), "lab-worker-" + i);
            t.start();
            KEEP.add(t);

            if (i % 50 == 0) {
                report("created " + i + " threads");
            }

            Thread.sleep(20);
        }

        while (true) {
            report("threads alive");
            Thread.sleep(3000);
        }
    }

    private static void report(String label) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();

        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        System.out.printf(
                "[%s] %s | heap used=%dMB committed=%dMB max=%dMB | nonHeap used=%dMB | threads=%d peak=%d%n",
                LocalTime.now(),
                label,
                heap.getUsed() / MB,
                heap.getCommitted() / MB,
                heap.getMax() / MB,
                nonHeap.getUsed() / MB,
                threads.getThreadCount(),
                threads.getPeakThreadCount()
        );
    }
}