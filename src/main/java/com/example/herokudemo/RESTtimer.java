/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class RESTtimer {

    protected static Logger logger = Logger.getLogger("service");

    private static String timerMsg = null;
    private static int timerCnt = 0;
    private static int timerExceptionCnt = 0;
    private static long lastTimer = 0;
    private static long timerServ = 0;

    public static String serverURL_1 = "";
    public static String serverURL_2 = "";
    public static String serverURL_0 = "";

    public void RestTimerHandler() {
        if (getEnv.checkLocalPC() == true) {
            ;
        }
        if (CKey.SERVERDB_URL.equals(CKey.URL_PATH_OP)) {
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long lockDateValue = dateNow.getTimeInMillis();
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.util.Date d = new java.util.Date(lockDateValue);
            DateFormat format = new SimpleDateFormat("M/dd/yyyy HH z");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);  //4/03/2020 04:47 PM EDT
            String[] arrOfStr = ESTdate.split(" ");
            int hr = Integer.parseInt(arrOfStr[1]);
            if ((hr >= 10) && (hr <= 11)) {
                serverURL_0 = "";
            } else if ((hr >= 17) && (hr <= 18)) {
                serverURL_0 = "";
            } else {
                if (HerokuDemoApplication.timerSchCnt > 5) {
                    serverURL_0 = "stop";
                }
            }
        }
        RestTimerHandler0(CKey.SERVERDB_URL);
    }

    public void RestTimerHandler1(String urlStr) {

        if (serverURL_1.equals("stop")) {
            return;
        }
        if (timerServ == 0) {
            timerServ = System.currentTimeMillis();
        }
        timerCnt++;
        if (timerCnt < 0) {
            timerCnt = 0;
        }

        if (timerExceptionCnt > 2) {
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(lastTimer, 1); // add 1 minutes
            if (lockDate1Min < currentTime) {
                timerExceptionCnt = 0;
            }
            return;
        }
        lastTimer = System.currentTimeMillis();
        timerMsg = "timerThreadServ=" + timerServ + "-timerCnt=" + timerCnt + "-ExceptionCnt=" + timerExceptionCnt;
        // // too much log
//        logger.info(timerMsg);
        try {

            // Create Client
            String url = "";
            if (serverURL_1.length() == 0) {
                url = urlStr + "/timerhandler?resttimerMsg=" + timerMsg;
            } else {
                url = serverURL_1 + "/timerhandler?resttimerMsg=" + timerMsg;
            }
            RESTtimerREST restAPI = new RESTtimerREST();
            String ret = restAPI.sendRequest(RESTtimerREST.METHOD_GET, url, null, null, CKey.PROXY);
            if (CKey.UI_TIMER == true) {
                logger.info(ret);
            }
            timerExceptionCnt--;
            if (timerExceptionCnt < 0) {
                timerExceptionCnt = 0;
            }
        } catch (Exception ex) {
            logger.info("RestTimerHandler1 Failed with HTTP Error ");
        }
        timerExceptionCnt++;
    }

    private static int timerCnt2 = 0;
    private static int timerExceptionCnt2 = 0;
    private static long lastTimer2 = 0;
    private static long timerServ2 = 0;

    public void RestTimerHandler2(String urlStr) {
        if (serverURL_2.equals("stop")) {
            return;
        }
        if (timerServ2 == 0) {
            timerServ2 = System.currentTimeMillis();
        }
        timerCnt2++;
        if (timerCnt2 < 0) {
            timerCnt2 = 0;
        }

        if (timerExceptionCnt2 > 2) {
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(lastTimer2, 1); // add 1 minutes
            if (lockDate1Min < currentTime) {
                timerExceptionCnt2 = 0;
            }
            return;
        }
        lastTimer2 = System.currentTimeMillis();
        timerMsg = "timerThreadServ=" + timerServ2 + "-timerCnt=" + timerCnt2 + "-ExceptionCnt=" + timerExceptionCnt2;
        // // too much log
//        logger.info(timerMsg);
        try {

            // Create Client
            String url = "";
            if (serverURL_2.length() == 0) {
                url = urlStr + "/timerhandler?resttimerMsg=" + timerMsg;
            } else {
                url = serverURL_2 + "/timerhandler?resttimerMsg=" + timerMsg;
            }
            RESTtimerREST restAPI = new RESTtimerREST();
            String ret = restAPI.sendRequest(RESTtimerREST.METHOD_GET, url, null, null, CKey.PROXY);
            if (CKey.UI_TIMER == true) {
                logger.info(ret);
            }

            timerExceptionCnt2--;
            if (timerExceptionCnt2 < 0) {
                timerExceptionCnt2 = 0;
            }
        } catch (Exception ex) {
            logger.info("RestTimerHandler2 Failed with HTTP Error ");
        }
        timerExceptionCnt2++;
    }

    private static int timerCnt3 = 0;
    private static int timerExceptionCnt3 = 0;
    private static long lastTimer3 = 0;
    private static long timerServ3 = 0;

    public void RestTimerHandler0(String urlStr) {
        if (serverURL_0.equals("stop")) {
            return;
        }
        if (timerServ3 == 0) {
            timerServ3 = System.currentTimeMillis();
        }
        timerCnt3++;
        if (timerCnt3 < 0) {
            timerCnt3 = 0;
        }

        if (timerExceptionCnt3 > 2) {
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(lastTimer3, 1); // add 1 minutes
            if (lockDate1Min < currentTime) {
                timerExceptionCnt3 = 0;
            }
            return;
        }
        lastTimer3 = System.currentTimeMillis();
        timerMsg = "timerThreadServ=" + timerServ3 + "-timerCnt=" + timerCnt3 + "-ExceptionCnt=" + timerExceptionCnt3;
        // // too much log
//        logger.info(timerMsg);
        try {

            // Create Client
            String url = "";

            if (serverURL_0.length() == 0) {
                url = urlStr + "/timerhandler?resttimerMsg=" + timerMsg;
                if (getEnv.checkLocalPC() == true) {
                    url = AFwebService.localTimerURL + AFwebService.webPrefix + "/timerhandler?resttimerMsg=" + timerMsg;
                }
            } else {
                url = serverURL_0 + "/timerhandler?resttimerMsg=" + timerMsg;
            }
            RESTtimerREST restAPI = new RESTtimerREST();
            String ret = restAPI.sendRequest(RESTtimerREST.METHOD_GET, url, null, null, false);
            if (CKey.UI_TIMER == true) {
                logger.info(ret);
            }

            timerExceptionCnt3--;
            if (timerExceptionCnt3 < 0) {
                timerExceptionCnt3 = 0;
            }
        } catch (Exception ex) {
            if (CKey.NN_DEBUG == true) {
//                logger.info("RestTimerHandler0 Failed with HTTP Error ");
            }
        }
        timerExceptionCnt3++;
    }

}
