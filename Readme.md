# Lab-01: Multithreading in Java

Name: <your name>
JDK: 25 LTS, Maven 3.10.0, Windows 11

How to run (from the project root):

```bat
mvn -q compile
java -cp target\classes edu.itc.se.lab01.taskN.<MainClass>
```

---

## Task 1: Creating and running threads

Four techniques were used:

| Thread | Technique |
|---|---|
| apollo | class `Countdown extends Thread` |
| gemini | `new Thread(new CountdownTask(), "gemini")` (Runnable) |
| mercury | lambda that calls `new CountdownTask().run()` |
| artemis | virtual thread: `Thread.ofVirtual().name("artemis").unstarted(...)` |

**start() vs run().** `start()` makes the JVM create a new thread, and that thread
calls `run()` concurrently with `main`. Calling `run()` directly is an ordinary
method call on the main thread, so nothing runs in parallel. With `--sequential`
each countdown finishes before the next one starts, and "all launched" is printed
after all the countdowns.

Observations from `--sequential`:
- gemini and mercury print `main` as the thread name, which proves that `run()`
  executed on the main thread.
- apollo prints its own name only because `Countdown` uses `getName()` on the
  Thread object itself.
- artemis prints nothing, because `run()` on an unstarted virtual thread does nothing.
- All final states are `NEW`, because the threads were never started.

In concurrent mode, all four final states are `TERMINATED` and only artemis has
`virtual=true`.

---

## Task 2: Race condition

`UnsafeCounter` uses `count++`, which is three steps (read, add, write). Two
threads can read the same value and both write value+1, so one update is lost.

Wrong totals from `UnsafeCounter` (8 threads x 1,000,000, expected 8,000,000):

1. ___
2. ___
3. ___

Timings (ms, typical run):

| Counter | Correct? | Time (ms) |
|---|---|---|
| UnsafeCounter | no | ___ |
| SynchronizedCounter | yes | ___ |
| LockCounter | yes | ___ |
| AtomicCounter | yes | ___ |
| LongAdderCounter | yes | ___ |

Comments on the timings: ___ (for example, which one was the slowest and why).

**Why is synchronizing only `increment()` still a bug?** `get()` reads `count`
without taking the lock. Without the same lock there is no happens-before
relationship between the writer's update and the reader's read, so the reader may
see a stale value. A plain `long` read is also not guaranteed to be atomic on
every JVM. Both methods must use the same lock (or the field must be `volatile`/atomic).

**Why is LongAdder faster under contention?** It spreads updates over several
internal cells, so threads rarely compete for the same memory location. `sum()`
adds all cells together when the value is read. `AtomicLong` makes all threads
retry a CAS on one variable, and `synchronized`/`ReentrantLock` make them wait for one lock.

---

## Task 3: Producer-consumer

`BoundedBuffer` uses `synchronized`, `wait()` in a `while` loop and `notifyAll()`.
`Producer` and `Consumer` depend only on the `OrderQueue` interface, so the same
classes run against both `BoundedBuffer` (`Main`) and `ArrayBlockingQueue`
(`Main2`). 2 producers x 50 orders and 3 consumers are used, with capacity 5, and
3 poison pills in total end the consumers.

Result: Total processed = ___ (expected 100), `Main` ___ ms, `Main2` ___ ms.

**Why `while` and `notifyAll()` are required**
- `while` instead of `if`: a thread that wakes up must re-check the condition.
  Another thread may have taken the slot first, and spurious wakeups are allowed
  by the Java spec. With `if`, a consumer could remove from an empty buffer
  (`NoSuchElementException`) or a producer could add to a full buffer.
- `notifyAll()` instead of `notify()`: producers and consumers wait on the same
  monitor. `notify()` wakes only one arbitrary thread, which may be the wrong
  kind (a producer waking another producer). Then everyone can end up waiting
  and the program hangs (deadlock/lost wakeup). `notifyAll()` wakes all of them,
  and each re-checks its own condition.

**Experiment (`if` + `notify()`, run 20 times):** ___ (describe what you saw:
hang, exception, failed assertion, how many of the 20 runs failed).

**BlockingQueue:** `ArrayBlockingQueue` does the locking, waiting and signalling
internally, so `BoundedBuffer`'s 15 lines of guarded-block code disappear.

---

## Task 4: Executors, Futures and virtual threads

**CPU-bound (primes below 4,000,000, 16 chunks):**

| Version | Primes | Time (ms) |
|---|---|---|
| single-threaded | ___ | ___ |
| fixed pool (___ threads) | ___ | ___ |

The counts are equal. The pool is faster because the chunks run on several cores
at the same time (speed-up about ___x).

**I/O-bound (500 fake downloads of 200 ms):**

| Executor | ok | failed | timedOut | Time (ms) |
|---|---|---|---|---|
| fixed pool (20 platform threads) | ___ | ___ | ___ | ___ |
| virtual thread per task | ___ | ___ | ___ | ___ |

**Explanation.** With 20 platform threads only 20 downloads run at once, so
500 tasks of 200 ms need about 500/20 x 0.2 s = 5 s. A sleeping or blocking
virtual thread releases its carrier (OS) thread, so all 500 tasks overlap and
the work finishes in about 0.2 s. The total is dominated by the 1 s timeout of
the slow task. Virtual threads are made for blocking I/O work. For CPU-bound
work they give no benefit, because the number of cores is the limit.

**Failures and timeouts.** Every 50th download throws `IOException("boom")`.
`future.get()` wraps it in an `ExecutionException`, which is caught and counted,
so the program never crashes. One task sleeps 5 s. `get(1, SECONDS)` throws
`TimeoutException`, and then `future.cancel(true)` interrupts the task.

**Shutdown.** All executors are created in try-with-resources (`close()` waits
for the tasks), and the JVM exits on its own without `System.exit`.

**Bonus:** `CompletableFuture.supplyAsync(..., executor)` with `handle()` turns a
failure into the string "fallback"; `allOf(...).join()` waits for all.
Fallbacks counted: ___

---

## Task 5: Deadlock and graceful shutdown

**The deadlock.** `Bank.transfer(from, to)` locks `from` and then `to`. If
thread A transfers acc-1 -> acc-2 and thread B transfers acc-2 -> acc-1, A holds
acc-1 and waits for acc-2 while B holds acc-2 and waits for acc-1. Neither can
continue. The jstack report is in `deadlock.txt`, and the watchdog printed:

```
[watchdog] DEADLOCK detected between: ___
```

The watchdog is a daemon thread that calls
`ThreadMXBean.findDeadlockedThreads()` every second.

**Coffman conditions.** A deadlock needs all four: mutual exclusion, hold and
wait, no preemption, and circular wait.

**Fix A, OrderedBank.** Always lock the account with the lower id first. This
removes circular wait, because every thread acquires locks in the same global order.
Result: transfers=___ total=___ (expected 4000) in ___ ms.

**Fix B, TryLockBank.** Each account has a `ReentrantLock`. `tryLock(10 ms)` is
used for both accounts. On failure the thread releases what it holds and retries
after a random back-off. This removes hold and wait (a thread never waits
forever while holding a lock). The `finally` blocks always unlock.
Result: transfers=___ retries=___ total=___ (expected 4000) in ___ ms.

**Graceful shutdown.** Workers loop on `!Thread.currentThread().isInterrupted()`.
When a blocking call throws `InterruptedException`, the worker restores the flag
with `Thread.currentThread().interrupt()`, so the loop exits. After 2 s, `main`
interrupts all workers and calls `join(1000)` on each.
Result: all workers stopped=___, total=___ (expected 4000).
Transfers per worker: ___

---

## Challenge

Not attempted. (Or: describe what you did.)

## Files

- `deadlock.txt`: jstack output from Task 5
- screenshot of the watchdog output: `___`