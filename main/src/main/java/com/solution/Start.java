package com.solution;

public class Start {
    public static void main(String[] args) {
        TransfererMain.start();
        MonitoringMain.start();

        ConsumerMain.start();

        while (true) {}
    }
}