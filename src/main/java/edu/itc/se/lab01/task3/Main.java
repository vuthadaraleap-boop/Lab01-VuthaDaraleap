package edu.itc.se.lab01.task3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static void run(OrderQueue q) throws InterruptedException {
        AtomicInteger processed = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        // 2 producers x 50 orders; 3 poison pills in total (2 + 1) for 3 consumers
        threads.add(new Thread(new Producer(q, 1, 50, 2), "producer-1"));
        threads.add(new Thread(new Producer(q, 51, 50, 1), "producer-2"));
        for (int i = 1; i <= 3; i++)
            threads.add(new Thread(new Consumer(q, processed), "consumer-" + i));

        long t0 = System.nanoTime();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.printf("Total processed: %d (expected 100) in %d ms%n", processed.get(), ms);
        assert processed.get() == 100;
    }

    public static void main(String[] args) throws InterruptedException {
        run(new BoundedBuffer(5));
    }
}
