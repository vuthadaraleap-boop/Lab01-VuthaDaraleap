package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/** Deliberately broken version: deadlocks. Also holds shared helpers. */
public class Bank {
    static final class Account {
        final int id;
        private long balance;
        final ReentrantLock lock = new ReentrantLock();   // used by TryLockBank

        Account(int id, long balance) { this.id = id; this.balance = balance; }
        long balance() { return balance; }
        void withdraw(long a) { balance -= a; }
        void deposit(long a) { balance += a; }
    }

    static Account[] newAccounts() {
        Account[] accounts = new Account[4];
        for (int i = 0; i < accounts.length; i++) accounts[i] = new Account(i, 1_000);
        return accounts;
    }

    static long total(Account[] accounts) {
        long t = 0;
        for (Account a : accounts) t += a.balance();
        return t;
    }

    /** Picks two different random accounts. */
    static Account[] randomPair(Account[] accounts, ThreadLocalRandom rnd) {
        Account a = accounts[rnd.nextInt(accounts.length)];
        Account b = accounts[(a.id + 1 + rnd.nextInt(accounts.length - 1)) % accounts.length];
        return new Account[] { a, b };
    }

    static void startWatchdog() {
        Thread wd = new Thread(() -> {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                long[] ids = mx.findDeadlockedThreads();
                if (ids != null) {
                    String names = String.join(", ", Arrays.stream(mx.getThreadInfo(ids))
                            .map(ThreadInfo::getThreadName).toList());
                    System.out.println("[watchdog] DEADLOCK detected between: " + names);
                }
            }
        }, "watchdog");
        wd.setDaemon(true);
        wd.start();
    }

    // BUG: lock order depends on the argument order -> circular wait
    static void transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() >= amount) { from.withdraw(amount); to.deposit(amount); }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        startWatchdog();
        Account[] accounts = newAccounts();
        Thread[] workers = new Thread[8];
        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();
                for (int i = 0; i < 100_000; i++) {
                    Account[] p = randomPair(accounts, rnd);
                    transfer(p[0], p[1], rnd.nextInt(1, 50));
                }
            }, "transfer-" + w);
            workers[w].start();
        }
        for (Thread t : workers) t.join();
        System.out.println("total = " + total(accounts) + " (expected 4000)");
    }
}
