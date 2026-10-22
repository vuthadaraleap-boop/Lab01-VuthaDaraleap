package edu.itc.se.lab01.task5;

import edu.itc.se.lab01.task5.Bank.Account;
import java.util.concurrent.ThreadLocalRandom;

/** Fix A: always lock the account with the lower id first. */
public class OrderedBank {
    static void transfer(Account from, Account to, long amount) {
        Account first = from.id < to.id ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Bank.startWatchdog();
        Account[] accounts = Bank.newAccounts();
        Thread[] workers = new Thread[8];
        long t0 = System.nanoTime();
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < 100_000; i++) {
                    Account[] p = Bank.randomPair(accounts, rnd);
                    transfer(p[0], p[1], rnd.nextInt(1, 50));
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        System.out.printf("transfers=%d total=%d (expected 4000) in %d ms%n",
                8 * 100_000, Bank.total(accounts), (System.nanoTime() - t0) / 1_000_000);
    }
}
