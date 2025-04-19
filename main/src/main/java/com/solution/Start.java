package com.solution;

import com.solution.monitoring.MonitoringMain;
import com.solution.transferer.TransfererMain;

public class Start {
    public static void main(String[] args) {
        TransfererMain.start();
        MonitoringMain.start();

        WorkerMain.start();

        while (true) {}
    }
}