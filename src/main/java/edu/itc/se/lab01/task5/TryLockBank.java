package edu.itc.se.lab01.task5;

import edu.itc.se.lab01.task5.Bank.Account;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Fix B: tryLock with timeout, release and retry with random back-off. */
public class TryLockBank {
    static final AtomicLong retries = new AtomicLong();

    static void transfer(Account from, Account to, long amount) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                try {
                    if (to.lock.tryLock(10, TimeUnit.MILLISECONDS)) {
                        try {
                            if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
                            return;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            retries.incrementAndGet();
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));   // random back-off
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account[] accounts = Bank.newAccounts();
        Thread[] workers = new Thread[8];
        long t0 = System.nanoTime();
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                try {
                    for (int i = 0; i < 100_000; i++) {
                        Account[] p = Bank.randomPair(accounts, rnd);
                        transfer(p[0], p[1], rnd.nextInt(1, 50));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        System.out.printf("transfers=%d retries=%d total=%d (expected 4000) in %d ms%n",
                8 * 100_000, retries.get(), Bank.total(accounts), (System.nanoTime() - t0) / 1_000_000);
    }
}
