package com.solution;

public class Main {
    public static void main(String[] args) {
        TransfererMain.start();

        ConsumerMain.start();

        MonitoringMain.start();

        while (true) {}
    }
}