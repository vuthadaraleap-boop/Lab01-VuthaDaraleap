package edu.itc.se.lab01.task5;

import edu.itc.se.lab01.task5.Bank.Account;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLongArray;

public class GracefulShutdown {
    public static void main(String[] args) throws InterruptedException {
        Account[] accounts = Bank.newAccounts();
        Thread[] workers = new Thread[8];
        AtomicLongArray done = new AtomicLongArray(workers.length);

        for (int w = 0; w < workers.length; w++) {
            final int id = w;
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Account[] p = Bank.randomPair(accounts, rnd);
                        TryLockBank.transfer(p[0], p[1], rnd.nextInt(1, 50));
                        done.incrementAndGet(id);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();   // restore flag; loop then exits
                    }
                }
            }, "transfer-" + w);
            workers[w].start();
        }

        Thread.sleep(2000);
        System.out.println("[main] interrupting " + workers.length + " workers after 2000 ms");
        for (Thread t : workers) t.interrupt();
        for (Thread t : workers) t.join(1000);

        for (int w = 0; w < workers.length; w++)
            System.out.println(workers[w].getName() + " stopped after " + done.get(w) + " transfers");
        boolean stopped = Arrays.stream(workers).noneMatch(Thread::isAlive);
        System.out.println("all workers stopped=" + stopped
                + " total=" + Bank.total(accounts) + " (expected 4000)");
    }
}
