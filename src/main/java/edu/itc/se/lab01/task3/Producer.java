package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;

public class Producer implements Runnable {
    private final OrderQueue q;
    private final int startId, count, pills;

    public Producer(OrderQueue q, int startId, int count, int pills) {
        this.q = q;
        this.startId = startId;
        this.count = count;
        this.pills = pills;
    }

    @Override
    public void run() {
        var rnd = ThreadLocalRandom.current();
        try {
            for (int i = 0; i < count; i++) {
                int id = startId + i;
                q.put(new Order(id, "item-" + id, rnd.nextInt(1, 6)));
                Thread.sleep(rnd.nextInt(0, 21));
            }
            for (int i = 0; i < pills; i++) q.put(Order.POISON);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
