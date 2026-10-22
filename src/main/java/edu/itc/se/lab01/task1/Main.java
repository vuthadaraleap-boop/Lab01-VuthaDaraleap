package edu.itc.se.lab01.task1;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && args[0].equals("--sequential");

        Thread apollo = new Countdown("apollo");
        Thread gemini = new Thread(new CountdownTask(), "gemini");
        Thread mercury = new Thread(() -> new CountdownTask().run(), "mercury");
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        Thread[] all = { apollo, gemini, mercury, artemis };

        for (Thread t : all) {
            if (sequential)
                t.run();
            else
                t.start();
        }
        System.out.println("Mission control: all launched");

        if (!sequential) {
            for (Thread t : all)
                t.join();
        }
        System.out.println("Mission control: all landed");

        for (Thread t : all) {
            System.out.printf("%-8s virtual=%-5b state=%s%n",
                    t.getName(), t.isVirtual(), t.getState());
        }
    }
}
