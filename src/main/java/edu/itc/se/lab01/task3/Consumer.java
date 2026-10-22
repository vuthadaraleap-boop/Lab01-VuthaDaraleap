package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final OrderQueue q;
    private final AtomicInteger processed;

    public Consumer(OrderQueue q, AtomicInteger processed) {
        this.q = q;
        this.processed = processed;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        try {
            while (true) {
                Order o = q.take();
                if (o.id() == -1) {
                    System.out.println(name + " received poison pill, stopping");
                    return;
                }
                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 31));
                System.out.println(name + " processed " + o);
                processed.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
