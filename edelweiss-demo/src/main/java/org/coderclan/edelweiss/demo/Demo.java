package org.coderclan.edelweiss.demo;

import org.coderclan.edelweiss.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class Demo implements ApplicationListener<ApplicationStartedEvent> {
    @Autowired
    private IdGenerator idGenerator;


    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        while (true) {
            try {
                Thread.sleep(3*1000L);
                System.out.println(this.idGenerator.generateId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
