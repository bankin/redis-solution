package com.solution;

public class Start {
    public static void main(String[] args) {
        TransfererMain.start();

        ConsumerMain.start();

        MonitoringMain.start();

        while (true) {}
    }
}