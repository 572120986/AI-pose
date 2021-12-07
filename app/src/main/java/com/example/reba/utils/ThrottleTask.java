package com.example.reba.utils;

import java.util.Timer;
import java.util.TimerTask;

public class ThrottleTask {
    private Timer timer;
    private Long delay;
    private Runnable runnable;
    private boolean needWait=false;

    public ThrottleTask(Runnable runnable, Long delay) {
        this.runnable = runnable;
        this.delay = delay;
        this.timer = new Timer();
    }

    public static ThrottleTask build(Runnable runnable, Long delay){
        return new ThrottleTask(runnable, delay);
    }

    public void run(){
        if(!needWait){
            needWait=true;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    needWait=false;
                    runnable.run();
                }
            }, delay);
        }
    }
}