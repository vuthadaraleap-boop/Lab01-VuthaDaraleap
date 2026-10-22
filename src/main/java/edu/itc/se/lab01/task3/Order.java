package edu.itc.se.lab01.task3;

public record Order(int id, String item, int qty) {
    public static final Order POISON = new Order(-1, "", 0);
}
