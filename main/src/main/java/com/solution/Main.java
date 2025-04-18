package com.solution;

public class Main {
    public static void main(String[] args) throws InterruptedException {
//        TransfererMain.start();

        for (int i = 0; i < 10; i++) {
////            Thread.ofVirtual().name("" + i).start(() -> new ConsumerMain().start()).start();
//            new Thread(() -> new ConsumerMain().start()).start();
            ConsumerMain.start();
        }


        MonitoringMain.start();

        while (true) {}
    }
}