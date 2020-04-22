package com.example.herokudemo;

import com.afweb.util.CKey;
import static com.afweb.util.CKey.URL_PATH_OP;
import com.afweb.util.getEnv;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class HerokuDemoApplication {

    private static AFwebService afWebService = new AFwebService();
    private static RESTtimer restTimer = new RESTtimer();

    public static void main(String[] args) {
        SpringApplication.run(HerokuDemoApplication.class, args);
    }
    public static int timerSchCnt = 0;
    public static boolean init = false;

    // just for testing to use 1 minute delay
    @Scheduled(fixedDelay = 10000) //60000) //2000)
    public void scheduleTaskWithFixedDelay() {

        timerSchCnt++;
        if (timerSchCnt < 0) {
            timerSchCnt = 100;
        }

        try {
            if (CKey.UI_TIMER == true) {
                restTimer.RestTimerHandler();
                return;
            }
            if (getEnv.checkLocalPC() == true) {
                restTimer.RestTimerHandler();
                return;
            }
            if (CKey.SERVERDB_URL.equals(CKey.URL_PATH_OP)) {
                restTimer.RestTimerHandler();
            } else {
                restTimer.RestTimerHandler();
            }

            TimeUnit.SECONDS.sleep(1);

        } catch (Exception ex) {

        }
    }

}
