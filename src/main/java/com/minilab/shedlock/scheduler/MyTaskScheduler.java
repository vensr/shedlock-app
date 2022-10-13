package com.minilab.shedlock.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class MyTaskScheduler {
 
    @Scheduled(cron = "*/10 * * * * *")
    @SchedulerLock(name = "MyTaskScheduler", lockAtLeastFor = "PT30S", lockAtMostFor = "PT1M")
    public void run() {
        System.out.println("My Scheduled Task");
    }

}

