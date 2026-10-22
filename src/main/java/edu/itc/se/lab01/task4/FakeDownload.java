package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.concurrent.Callable;

public class FakeDownload implements Callable<String> {
    private final String url;
    private final int index;

    public FakeDownload(String url, int index) { this.url = url; this.index = index; }

    @Override
    public String call() throws Exception {
        Thread.sleep(index == 7 ? 5000 : 200);               // task 7 is the slow one
        if (index % 50 == 0) throw new IOException("boom");  // 50, 100, ... 500 fail
        return url + " -> " + url.length();
    }
}
