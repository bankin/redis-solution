package com.solution;

import com.solution.monitoring.MonitoringMain;
import com.solution.transferer.TransfererMain;

/**
 * Entry point for the whole system. The idea is for each part to work independently.
 */
public class Start {
    public static void main(String[] args) {
        TransfererMain.start();
        MonitoringMain.start();

        WorkerMain.start();

        while (true) {}
    }
}