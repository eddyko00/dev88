/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.util.CKey;

/**
 *
 * @author eddyko
 */
public class Javamain {

    public static void checkParameterFlag(String cmd) {

        if (cmd.indexOf("flagNNLearningSignal") != -1) {
            ServiceAFweb.flagNNLearningSignal = true;

        } else if (cmd.indexOf("flagNNReLearning") != -1) {
            ServiceAFweb.flagNNReLearning = true;
        } else if (cmd.indexOf("processNNSignalAdmin") != -1) {
            ServiceAFweb.processNNSignalAdmin = true;
        } else if (cmd.indexOf("processRestinputflag") != -1) {
            ServiceAFweb.processRestinputflag = true;
        } else if (cmd.indexOf("processRestAllStockflag") != -1) {
            ServiceAFweb.processRestAllStockflag = true;
        } else if (cmd.indexOf("initLocalRemoteNN") != -1) {
            ServiceAFweb.initLocalRemoteNN = true;
//
        } else if (cmd.indexOf("directmysqlflag") != -1) {
            CKey.SQL_DATABASE = CKey.DIRECT__MYSQL;
        } else if (cmd.indexOf("localmysqlflag") != -1) {
            CKey.SQL_DATABASE = CKey.LOCAL_MYSQL;
        } else if (cmd.indexOf("dbinfoflag") != -1) {
            CKey.dbinfonnflag = true;
        } else if (cmd.indexOf("phpmysqlflag") != -1) {
            set_phpmysqlflag();
        } else if (cmd.indexOf("php_1_mysqlflag") != -1) {
            set_php_1_mysqlflag();
        } else if (cmd.indexOf("php_2_mysqlflag") != -1) {
            set_php_2_mysqlflag();
        } else if (cmd.indexOf("php_4_mysqlflag") != -1) {
            set_php_4_mysqlflag();
///////////////
        } else if (cmd.indexOf("backupFlag") != -1) {
            CKey.backupFlag = true;
        } else if (cmd.indexOf("backupInfoFlag") != -1) {
            CKey.backupInfoFlag = true;
        } else if (cmd.indexOf("backupNNFlag") != -1) {
            CKey.backupNNFlag = true;
        } else if (cmd.indexOf("restoreFlag") != -1) {
            CKey.restoreFlag = true;
        } else if (cmd.indexOf("restoreInfoFlag") != -1) {
            CKey.restoreInfoFlag = true;
        } else if (cmd.indexOf("restoreNNFlag") != -1) {
            CKey.restoreNNFlag = true;
////////////////
        } else if (cmd.indexOf("proxyflag") != -1) {
            CKey.PROXY = true;
        } else if (cmd.indexOf("nndebugflag") != -1) {
            CKey.NN_DEBUG = true;
            CKey.UI_ONLY = true;

        } else if (cmd.indexOf("delayrestoryflag") != -1) {
            CKey.DELAY_RESTORE = true;

        } else if (cmd.indexOf("processEmailFlag") != -1) {
            ServiceAFweb.processEmailFlag = true;

        } else if (cmd.indexOf("processNeuralNetFlag") != -1) {
            ServiceAFweb.processNeuralNetFlag = true;

        } else if (cmd.indexOf("nn1testflag") != -1) {
            ServiceAFweb.nn1testflag = true;
        } else if (cmd.indexOf("nn1testflag") != -1) {
            ServiceAFweb.nn1testflag = true;
        } else if (cmd.indexOf("nn2testflag") != -1) {
            ServiceAFweb.nn2testflag = true;
        } else if (cmd.indexOf("nn3testflag") != -1) {
            ServiceAFweb.nn3testflag = true;
        } else if (cmd.indexOf("nn30testflag") != -1) {
            ServiceAFweb.nn30testflag = true;

        } else if (cmd.indexOf("forceMarketOpen") != -1) {
            ServiceAFweb.forceMarketOpen = true;
        } else if (cmd.indexOf("mydebugtestNN3flag") != -1) {
            ServiceAFweb.mydebugtestNN3flag = true;
            ServiceAFweb.forceMarketOpen = true;

        } else if (cmd.indexOf("mydebugtestflag") != -1) {
            CKey.NN_DEBUG = true;
            CKey.UI_ONLY = true;
            ServiceAFweb.mydebugtestflag = true;

        }

    }

    public static void set_phpmysqlflag() {
        CKey.SQL_DATABASE = CKey.REMOTE_PHP_MYSQL;
        CKey.SERVER_DB_URL = CKey.URL_PATH_HERO_DBDB_PHP + CKey.WEBPOST_HERO_PHP;
        CKey.SERVER_TIMMER_URL = CKey.URL_PATH_HERO;
    }

    public static void set_php_1_mysqlflag() {
        CKey.SQL_DATABASE = CKey.REMOTE_PHP_1_MYSQL;
        CKey.SERVER_DB_URL = CKey.URL_PATH_HERO_1_DBDB_PHP + CKey.WEBPOST_HERO_1_PHP;
        CKey.SERVER_TIMMER_URL = CKey.URL_PATH_HERO_1;
    }

    public static void set_php_2_mysqlflag() {

        CKey.SQL_DATABASE = CKey.REMOTE_PHP_2_MYSQL;
        CKey.SERVER_DB_URL = CKey.URL_PATH_HERO_2_DBDB_PHP + CKey.WEBPOST_HERO_2_PHP;
        CKey.SERVER_TIMMER_URL = CKey.URL_PATH_HERO_2;
    }

    public static void set_php_4_mysqlflag() {

        CKey.SQL_DATABASE = CKey.REMOTE_PHP_4_MYSQL;
        CKey.SERVER_DB_URL = CKey.URL_PATH_HERO_4_DBDB_PHP + CKey.WEBPOST_HERO_4_PHP;
        CKey.SERVER_TIMMER_URL = CKey.URL_PATH_HERO_4;
    }

    /**
     * @param args the command line arguments
     */
    public static void javamain(String[] args) {
        // TODO code application logic here
        ServiceAFweb srv = new ServiceAFweb();

        if (args != null) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    String cmd = args[i];
                    checkParameterFlag(cmd);
                } // loop
            }
        }

        while (true) {
            srv.AFtimerHandler("");
            ServiceAFweb.AFSleep1Sec(1);
        }

    }
}
