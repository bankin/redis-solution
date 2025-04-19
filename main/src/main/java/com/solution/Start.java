package com.solution;

public class Start {
    public static void main(String[] args) {
        TransfererMain.start();
        MonitoringMain.start();

        WorkerMain.start();

        while (true) {}
    }
}