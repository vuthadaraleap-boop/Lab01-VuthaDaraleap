package edu.itc.se.lab01.task4;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    static void runDownloads(ExecutorService ex, String label) throws InterruptedException {
        long t0 = System.nanoTime();
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 1; i <= 500; i++)
            futures.add(ex.submit(new FakeDownload("http://site/file" + i, i)));

        int ok = 0, failed = 0, timedOut = 0;
        for (Future<String> f : futures) {
            try {
                f.get(1, TimeUnit.SECONDS);
                ok++;
            } catch (ExecutionException e) {
                failed++;
            } catch (TimeoutException e) {
                f.cancel(true);
                timedOut++;
            }
        }
        System.out.printf("%-40s: ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, (System.nanoTime() - t0) / 1_000_000);
    }

    static void bonus() {
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> list = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                int n = i;
                list.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return new FakeDownload("http://site/file" + n, n).call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, ex).handle((r, e) -> e == null ? r : "fallback"));
            }
            CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
            long fallbacks = list.stream().filter(f -> f.join().equals("fallback")).count();
            System.out.println("bonus: fallbacks=" + fallbacks);
        }
    }

    public static void main(String[] args) throws Exception {
        final long LIMIT = 4_000_000;
        final int CHUNKS = 16;

        long t0 = System.nanoTime();
        long single = new PrimeCounter(0, LIMIT).call();
        System.out.printf("single-threaded: %d primes in %d ms%n",
                single, (System.nanoTime() - t0) / 1_000_000);

        int cores = Runtime.getRuntime().availableProcessors();
        t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(cores)) {
            List<Future<Long>> futures = new ArrayList<>();
            long size = LIMIT / CHUNKS;
            for (int i = 0; i < CHUNKS; i++)
                futures.add(pool.submit(new PrimeCounter(i * size, (i + 1) * size)));
            long total = 0;
            for (Future<Long> f : futures) total += f.get();
            System.out.printf("pool (%d threads): %d primes in %d ms (match=%b)%n",
                    cores, total, (System.nanoTime() - t0) / 1_000_000, total == single);
        }

        try (ExecutorService ex = Executors.newFixedThreadPool(20)) {
            runDownloads(ex, "downloads on fixed(20) platform threads");
        }
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            runDownloads(ex, "downloads on virtual threads");
        }
        bonus();
    }
}
