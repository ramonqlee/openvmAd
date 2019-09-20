package com.idreems.openvmAd.utils;

import android.util.Log;

import com.idreems.openvmAd.constant.Consts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramonqlee_macpro on 2018/5/30.
 * 一个自定义的timer，支持如下特性
 * 兼容timer的接口
 * 通过一定定时器+线程池的方式，来实现了线程的合理利用
 */

public class ReusableTimer {
    private static final String TAG = "ReusableTimer";
    private static Timer sTimer;
    private static Map<ReusableTimer, MyTimerTask> sTimerTaskMap = new HashMap<>();
    private static Byte sSyncObj = Byte.valueOf((byte) 0);

    private void loop() {
        if (null != sTimer) {
            return;
        }
        sTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                List<ReusableTimer> toRemoveList = new ArrayList<>();
                try {
                    Map<ReusableTimer, MyTimerTask> tempTimerTaskMap = new HashMap<>(sTimerTaskMap);
                    for (Map.Entry<ReusableTimer, MyTimerTask> entry : tempTimerTaskMap.entrySet()) {
                        if (null == entry || null == entry.getKey() || null == entry.getValue() || null == entry.getValue().task) {
                            continue;
                        }

                        final MyTimerTask task = entry.getValue();
                        //删除取消状态的任务
                        if (task.cancelled) {
                            toRemoveList.add(entry.getKey());
                            continue;
                        }

                        final long currentTime = System.currentTimeMillis();
                        if (currentTime >= task.nextExecutionTime) {
                            //删除到期的一次性的任务
                            if (0 == task.period) {
                                toRemoveList.add(entry.getKey());
                            }

                            try {
                                task.task.run();
                            } catch (Exception e) {
                                toRemoveList.add(entry.getKey());
                            }

                            // 对于非一次性任务,更新下次执行的时间
                            if (0 != task.period) {
                                task.nextExecutionTime = currentTime + task.period;
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                if (null == toRemoveList || toRemoveList.isEmpty()) {
                    return;
                }

                synchronized (sSyncObj) {
                    try {
                        //remove task
                        for (ReusableTimer key : toRemoveList) {
                            sTimerTaskMap.remove(key);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        sTimer.schedule(task, Consts.ONE_SECOND_IN_MS, Consts.ONE_SECOND_IN_MS);
    }

    public void schedule(TimerTask task, long delayInMs) {
        schedule(task, delayInMs, 0);
    }

    public void schedule(TimerTask task, long delayInMs, long periodInMs) {
        if (Consts.logEnabled()) {
            Log.d(Consts.LOG_TEST_TAG, this + " schedule period task = " + task + " delay = " + delayInMs +
                    " period =" + periodInMs);
        }
        synchronized (sSyncObj) {
            try {
                sTimerTaskMap.put(this, new MyTimerTask(System.currentTimeMillis() + delayInMs, periodInMs, task));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        loop();
    }

    public void cancel() {
        try {
            MyTimerTask task = sTimerTaskMap.get(this);
            if (null == task) {
                return;
            }
            task.cancelled = true;
            if (Consts.logEnabled()) {
                Log.d(Consts.LOG_TEST_TAG, TAG + " cancel task = " + task.task);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class MyTimerTask {
        public Runnable task;
        /**
         * Next execution time for this task in the format returned by
         * System.currentTimeMillis, assuming this task is scheduled for execution.
         * For repeating tasks, this field is updated prior to each task execution.
         */
        public long nextExecutionTime;
        /**
         * Period in milliseconds for repeating tasks.  A positive value indicates
         * fixed-rate execution.  A negative value indicates fixed-delay execution.
         * A value of 0 indicates a non-repeating task.
         */
        public long period = 0;
        public boolean cancelled;

        public MyTimerTask(long nextExecutionTime, long period, Runnable r) {
            this.nextExecutionTime = nextExecutionTime;
            this.period = period;
            this.task = r;
        }
    }
}
