package com.example.herokudemo;

import com.afweb.service.*;
import com.afweb.util.CKey;
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
    public static boolean webapp = true;

    public static void main(String[] args) {

        if (args.length > 0) {
            String cmd = args[0];
            if (cmd.indexOf("javamain") != -1) {
                webapp = false;
                Javamain.javamain(args);
            }
            if (cmd.indexOf("proxyflag") != -1) {
                CKey.PROXY = true;

            } else if (cmd.indexOf("localmysqlflag") != -1) {
                CKey.SQL_DATABASE = CKey.LOCAL_MYSQL;

            } else if (cmd.indexOf("otherphp1mysqlflag") != -1) {
                CKey.OTHER_PHP1_MYSQL = true;
                CKey.SERVER_TIMMER_URL = CKey.URL_PATH_OP;
                ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_OP_DB_PHP1 + CKey.WEBPOST_OP_PHP);
            }

            SpringApplication.run(HerokuDemoApplication.class, args);
        }
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
//        if (webapp == true) {
//            Javamain.javamain(null);
//        }
        try {
            if (getEnv.checkLocalPC() == true) {
                restTimer.RestTimerHandler();
                return;
            }

            restTimer.RestTimerHandler();

            TimeUnit.SECONDS.sleep(1);

        } catch (Exception ex) {

        }
    }

}
