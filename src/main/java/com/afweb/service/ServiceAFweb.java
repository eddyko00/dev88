/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.stockinfo.StockInfoImp;
import com.afweb.stockinternet.StockInternetImpDao;
import com.afweb.processnn.*;
import com.afweb.nnsignal.*;

import com.afweb.processemail.EmailProcess;
import com.afweb.processcustacc.PUBSUBprocess;
import com.afweb.processbilling.BillingProcess;
import com.afweb.processaccounting.AccountingProcess;

import com.afweb.processmain.AccountMaintProcess;

import com.afweb.model.*;
import com.afweb.account.*;
import com.afweb.chart.ChartService;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.processcustacc.CustAccService;
import com.afweb.processnn.NNService;
import com.afweb.processstock.StockService;
import com.afweb.processstockinfo.*;

import com.afweb.stock.*;
import com.afweb.stockinternet.StockUtils;
import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.DateFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;

import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

/**
 *
 * @author eddy
 */
@Service
public class ServiceAFweb {

    public static Logger logger = Logger.getLogger("AFwebService");

    private static ServerObj serverObj = new ServerObj();

    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;

    public static String serverLockName = "server";
    public static boolean NN_AllowTraingStockFlag = false;
    private static boolean initProcessTimer = false;
    private static int delayProcessTimer = 0;
    private static long timerThreadDateValue = 0;

    private ServiceAFwebREST serviceAFwebREST = new ServiceAFwebREST();
    private StockImp stockImp = new StockImp();
    private StockInfoImp stockInfoImp = new StockInfoImp();
    private AccountImp accountImp = new AccountImp();
    private AccountTranProcess accountProcessImp = new AccountTranProcess();
    private AccountingProcess accounting = new AccountingProcess();

    public static String PROXYURL = "";
    public static String URL_LOCALDB = "";
    public static String FileLocalPath = "";

    public static String UA_Str = "";
    public static String PA_Str = "";
    public static String UU_Str = "";

    public static ArrayList TRList = new ArrayList();

    private static AccountObj cacheAccountAdminObj = null;
    private static long cacheAccountAdminObjDL = 0;

    public static String FileLocalDebugPath = "T:/Netbean/debug/";
    public static String FileLocalNNPath = "T:/Netbean/debug/training";

    public static String ignoreStock[] = {"T.T"};
//    public static String allStock[] = {"NEM", "SE", "MSFT", "T.TO"};
//    public static String primaryStock[] = {"HOU.TO", "IWM", "AMZN", "SPY", "DIA", "QQQ", "HOD.TO", "FAS", "FAZ", "XIU.TO", "AAPL", "RY.TO", "GLD"};

    public static String allStock[] = {"NEM", "SE", "MSFT", "T.TO", "AMZN"};
    public static String primaryStock[] = {"HOU.TO", "IWM", "GLD", "SPY", "DIA", "QQQ", "HOD.TO", "FAS", "FAZ", "XIU.TO", "AAPL", "RY.TO"};

    public static String etfStock[] = {"SPY", "DIA", "QQQ", "XIU.TO", "GLD", "FAS", "HOU.TO", "IWM", "IYR"};

    /**
     * @return the cacheAccountAdminObj
     */
    public static AccountObj getCacheAccountAdminObj() {
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        if (cacheAccountAdminObjDL == 0) {
            cacheAccountAdminObjDL = dateNow.getTimeInMillis();
        }
        long dateNow5Min = TimeConvertion.addMinutes(cacheAccountAdminObjDL, 5);

        if (dateNow5Min < cacheAccountAdminObjDL) {
            cacheAccountAdminObjDL = dateNow5Min;
            cacheAccountAdminObj = null;
        }
        return cacheAccountAdminObj;
    }

    /**
     * @param aCacheAccountAdminObj the cacheAccountAdminObj to set
     */
    public static void setCacheAccountAdminObj(AccountObj aCacheAccountAdminObj) {
        cacheAccountAdminObj = aCacheAccountAdminObj;
    }

    public AccountObj getAdminObjFromCache() {
        try {
            AccountObj accountAdminObj = ServiceAFweb.getCacheAccountAdminObj();
            if (accountAdminObj == null) {
                ArrayList accountList = getAccountListServ(CKey.ADMIN_USERNAME, null);
                // do not clear the lock so that it not run by other tast immediately
                if (accountList == null) {
                    return null;
                }
                for (int i = 0; i < accountList.size(); i++) {
                    AccountObj accountTmp = (AccountObj) accountList.get(i);
                    if (accountTmp.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
                        accountAdminObj = accountTmp;
                        break;
                    }
                }
                ServiceAFweb.setCacheAccountAdminObj(accountAdminObj);
            }
            return accountAdminObj;
        } catch (Exception ex) {
            logger.info("> getAdminObjFromCache Exception" + ex.getMessage());
        }
        return null;
    }

    /**
     * @return the TRList
     */
    public static ArrayList getTRList() {
        return TRList;
    }

    /**
     * @param aTRList the TRList to set
     */
    public static void setTRList(ArrayList aTRList) {
        TRList = aTRList;
    }

    /**
     * @return the serverObj
     */
    public static ServerObj getServerObj() {
        if (serverObj.getCntRESTrequest() < 0) {
            serverObj.setCntRESTrequest(0);
        }
        if (serverObj.getCntRESTexception() < 0) {
            serverObj.setCntRESTexception(0);
        }
        if (serverObj.getCntInterRequest() < 0) {
            serverObj.setCntInterRequest(0);
        }
        if (serverObj.getCntInterException() < 0) {
            serverObj.setCntInterException(0);
        }
        if (serverObj.getCntControRequest() < 0) {
            serverObj.setCntControRequest(0);
        }
        if (serverObj.getCntControlResp() < 0) {
            serverObj.setCntControlResp(0);
        }
        return serverObj;
    }

    /**
     * @param aServerObj the serverObj to set
     */
    public static void setServerObj(ServerObj aServerObj) {
        serverObj = aServerObj;
    }

    public ArrayList getServerList() {
        ServerObj serverObj = ServiceAFweb.getServerObj();
        ArrayList serverObjList = new ArrayList();
        serverObjList.add(serverObj);
        return serverObjList;
    }

    public void initDataSource() {
        logger.info(">initDataSource ");
        //testing
        WebAppConfig webConfig = new WebAppConfig();
        dataSource = webConfig.dataSource();
        //testing        
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;

        String enSt = CKey.PROXYURL_TMP;
        enSt = StringTag.replaceAll("abc", "", enSt);
        PROXYURL = enSt;
        if (FileLocalPath.length() == 0) {
            if (getEnv.checkLocalPC() == true) {
                FileLocalPath = CKey.FileLocalPathTemp;
            } else {
                FileLocalPath = CKey.FileServerPathTemp;
            }
        }
        String paStr = CKey.PA;
        paStr = StringTag.replaceAll("abc", "", paStr);
        PA_Str = paStr;
        paStr = CKey.UA;
        paStr = StringTag.replaceAll("abc", "", paStr);
        UA_Str = paStr;
        paStr = CKey.UU;
        paStr = StringTag.replaceAll("abc", "", paStr);
        UU_Str = paStr;

    }

    public int timerThread() {

        if (timerThreadDateValue > 0) {
            long currentTime = System.currentTimeMillis();
            long timerThreadDate5Min = TimeConvertion.addMinutes(timerThreadDateValue, 10); // add 8 minutes
            if (timerThreadDate5Min > currentTime) {
                return getServerObj().getTimerCnt();
            }
        }

        try {
//            while (true) {
            Thread.sleep(10 * 100);
            timerThreadDateValue = System.currentTimeMillis();
            timerHandler("");
//            }
        } catch (Exception ex) {
            logger.info("> timerThread Exception" + ex.getMessage());
        }

        return getServerObj().getTimerCnt();
    }

    //Repeat every 10 seconds
    public int timerHandler(String timerThreadMsg) {
        // too much log
//        logger.info("> timerHandler " + timerThreadMsg);

        serverObj.setTimerCnt(serverObj.getTimerCnt() + 1);
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        if (initProcessTimer == false) {
            delayProcessTimer++;
            if (delayProcessTimer > 2) {
                initProcessTimer = true;
            }
            return getServerObj().getTimerCnt();
        }

        if (getServerObj().getTimerCnt() < 0) {
            serverObj.setTimerCnt(0);
        }

        //only allow 1 thread 
        if (getServerObj().getTimerQueueCnt() > 0) {

            long currentTime = System.currentTimeMillis();
            int waitMinute = 8;
            if (getServerObj().isSysMaintenance() == true) {
                waitMinute = 3;
            }
            long lockDate5Min = TimeConvertion.addMinutes(getServerObj().getLastServUpdateTimer(), waitMinute); // add 8 minutes
            if (lockDate5Min < currentTime) {
                serverObj.setTimerQueueCnt(0);
            }
            return getServerObj().getTimerCnt();
        }

        serverObj.setLastServUpdateTimer(lockDateValue);
        serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() + 1);
        try {
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            Date d = new Date();
            // timezone symbol (z) included in the format pattern 
            DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            // format date in target timezone
            format.setTimeZone(tz);
            serverObj.setLastServUpdateESTdate(format.format(d));

            serverObj.setTimerMsg("timerHandlerServ=" + getServerObj().getServerName() + "-" + "timerCnt=" + getServerObj().getTimerCnt() + "-timerQueueCnt=" + getServerObj().getTimerQueueCnt());
//            logger.info(getServerObj().getTimerMsg());
            if (timerThreadMsg != null) {
                serverObj.setTimerThreadMsg(timerThreadMsg);
            }

            if (getServerObj().isSysMaintenance() == true) {
                return getServerObj().getTimerCnt();
            }

            if (getServerObj().isTimerInit() == false) {
                /////////////
                initDataSource();
                InitStaticData();   // init TR data

                // work around. must initialize for remote MYSQL
                ServiceRemoteDB.setServiceAFWeb(this);
                getStockImp().setDataSource(jdbcTemplate, dataSource);
                getStockInfoImp().setDataSource(jdbcTemplate, dataSource);
                getAccountImp().setDataSource(jdbcTemplate, dataSource);

                // work around. must initialize for remote MYSQL
                serverObj.setTimerInit(true);
                getServerObj().setProcessTimerCnt(0);

                String SrvName = "iisweb";
                String stlockDateValue = "" + lockDateValue;
                stlockDateValue = stlockDateValue.substring(10);

                serverObj.setServerName(SrvName + lockDateValue);
                serverObj.setVerString(ConstantKey.VERSION); // + " " + getServerObj().getLastServUpdateESTdate());
                serverObj.setSrvProjName(SrvName + stlockDateValue);

                serverLockName = ServiceAFweb.getServerObj().getServerName();

                String displayStr = "";
                getServerObj().setLocalDBservice(true);
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                displayStr += "\r\n" + (">>>>> System LOCAL_MYSQL = 4, REMOTE_PHP_MYSQL = 2, DIRECT_MYSQL = 0");
                displayStr += "\r\n" + (">>>>> System SQL_DATABASE:" + CKey.SQL_DATABASE);
                String dbStr = "";
                if (CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) {
                    String dsURL = CKey.dataSourceURL;
                    dbStr += "\r\n" + (">>>>> System Local DB URL:" + dsURL);
                }
                if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
                    if (CKey.OTHER_PHP1_MYSQL == true) {
                        dbStr += "\r\n" + (">>>>> System OTHER PHP1 DB URL:" + CKey.URL_PATH_OP_DB_PHP1);
                    } else {
                        dbStr += "\r\n" + (">>>>> System PHP MYSQL DB URL:" + CKey.REMOTEDB_MY_SQLURL);
                    }
                } else if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                    if (dataSource != null) {
                        DriverManagerDataSource dataSourceObj = (DriverManagerDataSource) dataSource;
                        dbStr += "\r\n" + (">>>>> System LOCAL_MYSQL DB URL:" + dataSourceObj.getUrl());
                    }
                }
                displayStr += "\r\n" + dbStr;
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

                displayStr += "\r\n" + (">>>>> System OTHER_PHP1_MYSQL:" + CKey.OTHER_PHP1_MYSQL);
                displayStr += "\r\n" + (">>>>> System SERVER_TIMMER_URL:" + CKey.SERVER_TIMMER_URL);
                displayStr += "\r\n" + (">>>>> System backupFlag:" + CKey.backupFlag);
                displayStr += "\r\n" + (">>>>> System restoreFlag:" + CKey.restoreFlag);
                displayStr += "\r\n" + (">>>>> System restoreNNonlyFlag:" + CKey.restoreNNonlyFlag);
                displayStr += "\r\n" + (">>>>> System proxyflag PROXY:" + CKey.PROXY);
                displayStr += "\r\n" + (">>>>> System nndebugflag NN_DEBUG:" + CKey.NN_DEBUG);
                displayStr += "\r\n" + (">>>>> System nndebugflag UI_ONLY:" + CKey.UI_ONLY);
                displayStr += "\r\n" + (">>>>> System delayrestoryflag DELAY_RESTORE:" + CKey.DELAY_RESTORE);
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

                displayStr += "\r\n" + (">>>>> System processEmailFlag:" + processEmailFlag);

                displayStr += "\r\n" + (">>>>> System processNeuralNetFlag:" + processNeuralNetFlag);

                displayStr += "\r\n" + (">>>>> System nn1testflag:" + nn1testflag);
                displayStr += "\r\n" + (">>>>> System nn2testflag:" + nn2testflag);
                displayStr += "\r\n" + (">>>>> System nn3testflag:" + nn3testflag);
                displayStr += "\r\n" + (">>>>> System nn30testflag:" + nn30testflag);

                displayStr += "\r\n" + (">>>>> System initLocalRemoteNN:" + initLocalRemoteNN);

                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                displayStr += "\r\n" + (">>>>> System mydebugtestflag:" + ServiceAFweb.mydebugtestflag);
                displayStr += "\r\n" + (">>>>> System mydebugnewtest:" + ServiceAFweb.forceMarketOpen);

                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                displayStr += "\r\n" + dbStr;
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                logger.info(displayStr);

//                boolean CKey.backupFlag = false;
                if (CKey.backupFlag == true) {
                    backupSystem();
                    serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                    return getServerObj().getTimerCnt();

                }
//                boolean restoreFlag = false;
                if (CKey.restoreFlag == true) {
                    restoreSystem();
                    serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                    return getServerObj().getTimerCnt();

                }
//                boolean restoreNNonlyFlag = false;
                if (CKey.restoreNNonlyFlag == true) {
                    restoreNNonlySystem();
                    serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                    return getServerObj().getTimerCnt();

                }
                if (CKey.UI_ONLY == false) {
                    String sysPortfolio = "";
                    // make sure not request during DB initialize

                    getServerObj().setSysMaintenance(true);
                    logger.info(">>>>>>> InitDBData started.........");
                    // 0 - new db, 1 - db already exist, -1 db error
                    int ret = InitDBData();  // init DB Adding customer account
//                        sysPortfolio = CKey.FUND_PORTFOLIO;
                    if (ret != -1) {

                        InitSystemData();   // Add Stock 
                        InitSystemFund(sysPortfolio);
                        initProcessTimer = false;
                        delayProcessTimer = 0;

                        getServerObj().setSysMaintenance(false);
                        serverObj.setTimerInit(true);
                        logger.info(">>>>>>> InitDBData Competed.....");
                    } else {
                        serverObj.setTimerInit(false);
                        serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                        logger.info(">>>>>>> InitDBData Failed.....");
                        return getServerObj().getTimerCnt();
                    }

                    serverObj.setTimerInit(true);
                    String servIP = StockInternetImpDao.getServerIP();
                    serverObj.setServip(servIP);

                    setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, serverObj.getSrvProjName() + " " + serverObj.getServip());

                    //try 2 times
                    getAccountProcessImp().ProcessAdminAddRemoveStock(this);
                    getAccountProcessImp().ProcessAdminAddRemoveStock(this);

                }
                // final initialization
            } else {
                if (timerThreadMsg != null) {
                    if (timerThreadMsg.indexOf("adminsignal") != -1) {
                        processTimer("adminsignal");
                    } else if (timerThreadMsg.indexOf("updatestock") != -1) {
                        processTimer("updatestock");
                    } else if (timerThreadMsg.indexOf("starttimer") != -1) {
                        processTimer("starttimer");
                    } else if (timerThreadMsg.indexOf("debugtest") != -1) {
                        processTimer("debugtest");
                    }
                }
                processTimer("");
            }

        } catch (Exception ex) {
            logger.info("> Exception lastfun - " + lastfun);
            logger.info("> timerHandler Exception" + ex.getMessage());
        }
        serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
        return getServerObj().getTimerCnt();
    }

    private void backupSystem() {

        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        if (CKey.NN_DEBUG == true) {
            // LocalPCflag = true; 
            // SQL_DATABASE = REMOTE_MYSQL;
            if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
                if ((CKey.OTHER_PHP1_MYSQL == true)) {
                    logger.info(">>>>> SystemDownloadDBData form Other DB");
                } else {
                    logger.info(">>>>> SystemDownloadDBData form Heroku");
                }
            } else if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                logger.info(">>>>> SystemDownloadDBData form local My SQL");
            }

            SystemDownloadDBData();
            getServerObj().setSysMaintenance(true);
            logger.info(">>>>> SystemDownloadDBData done");
        }

    }

    private void restoreNNonlySystem() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        if (CKey.NN_DEBUG == true) {
            if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
                if ((CKey.OTHER_PHP1_MYSQL == true)) {
                    logger.info(">>>>> SystemRestoreDBData to Other DB");
                } else {
                    logger.info(">>>>> SystemRestoreDBData to Heroku");
                }
            } else if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                logger.info(">>>>> SystemRestoreDBData form to My SQL");
            }

            Scanner scan = new Scanner(System.in);
            System.out.print("Hit any key to continue to restore?");
            String YN = scan.next();

            String retSt = SystemCleanNNonlyDBData();
            if (retSt.equals("true")) {
                SystemRestoreNNonlyDBData();
                getServerObj().setSysMaintenance(true);
                logger.info(">>>>> SystemRestoreDBData done");
            }

        }
    }

    private void restoreSystem() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        if (CKey.NN_DEBUG == true) {
            if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
                if ((CKey.OTHER_PHP1_MYSQL == true)) {
                    logger.info(">>>>> SystemRestoreDBData to Other DB");
                } else {
                    logger.info(">>>>> SystemRestoreDBData to Heroku");
                }
            } else if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                logger.info(">>>>> SystemRestoreDBData form to My SQL");
            }
            Scanner scan = new Scanner(System.in);
            System.out.print("Hit any key to continue to restore?");
            String YN = scan.next();

            String retSt = SystemCleanDBData();
            if (retSt.equals("true")) {
                SystemRestoreDBData();
                getServerObj().setSysMaintenance(true);
                logger.info(">>>>> SystemRestoreDBData done");
            }
        }
    }
    //////////
    private long lastProcessTimer = 0;
    public boolean debugFlag = false;

    public static int initTrainNeuralNetNumber = 0;

    public static String lastfun = "";

    private void processTimer(String cmd) {

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (debugFlag == false) {
                    debugFlag = true;
//                    getTRprocessImp().updateStockProcess(this, "HOU.TO");
//                        
// Window -> Debugging -> Breakpoints Select all, the delete
//
///////////////////////////////////////////////////////////////////////////////////
// use /cust/{username}/sys/fundmgr
// Fund Manger only do once a month                    

                    boolean fundMgrFlag = false;
                    if (fundMgrFlag == true) {
                        SystemFundResetGlobalServ();
                    }
// use /cust/{username}/sys/processfundmgr                    }
// Fund Manger only do once a month   
                    boolean fundFlag = false;
                    if (fundFlag == true) {
                        SystemFundPocessAddRemoveServ();
                    }

///////////////////////////////////////////////////////////////////////////////////
                    AFprocessDebug();
                    NNService nnSrv = new NNService();
                    nnSrv.processNeuralNetTrain(this);

///////////////////////////////////////////////////////////////////////////////////
                    logger.info(">>>>>>>> DEBUG end >>>>>>>>>");
                }
            }
        }
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        AccountTranProcess accountTranP = new AccountTranProcess();
        String LockName = null;
        //////           
        if (cmd.length() > 0) {
            if (cmd.equals("adminsignal")) {
//                    TRprocessImp.ProcessAdminSignalTrading(this);
                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
            } else if (cmd.equals("updatestock")) {
                updateStockAllSrv();
            } else if (cmd.equals("debugtest")) {
                debugtest();
            }
        }
        //////        
        if (CKey.UI_ONLY == true) {

            return;
        }

        try {
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long lockDateValue = dateNow.getTimeInMillis();

            LockName = "LOCK_" + ServiceAFweb.getServerObj().getServerName();
            long lockReturn = setLockNameProcess(LockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, "ProcessTimerCnt " + getServerObj().getProcessTimerCnt());

            if (CKey.NN_DEBUG == true) {
                lockReturn = 1;
            }
            if (lockReturn == 0) {
                return;
            }
            if (getServerObj().getProcessTimerCnt() < 0) {
                getServerObj().setProcessTimerCnt(0);
            }
            getServerObj().setProcessTimerCnt(getServerObj().getProcessTimerCnt() + 1);

//            logger.info("> processTimer " + getServerObj().getProcessTimerCnt());
            if (getEnv.checkLocalPC() == true) {
                if (CKey.NN_DEBUG == true) {
                    updateStockAllSrv();
                    accountTranP.ProcessAdminSignalTrading(this);
                    getAccountProcessImp().ProcessAllAccountTradingSignal(this);
                    updateStockAllSrv();

                }
            }

            /////// main execution
            AFwebExec();
            ///////
        } catch (Exception ex) {
            logger.info("> Exception lastfun - " + lastfun);
            logger.info("> processTimer Exception " + ex.getMessage());
        }
        removeNameLock(LockName, ConstantKey.SRV_LOCKTYPE);
    }

    void AFwebExec() {

        ////////////
        if (((getServerObj().getProcessTimerCnt() % 29) == 0) || (getServerObj().getProcessTimerCnt() == 1)) {
            long result = setRenewLock(serverLockName, ConstantKey.SRV_LOCKTYPE);
            if (result == 0) {
                Calendar dateNow1 = TimeConvertion.getCurrentCalendar();
                long lockDateValue1 = dateNow1.getTimeInMillis();
                setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue1, serverObj.getSrvProjName() + " " + serverObj.getServip());
            }
        }

        //2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
        if ((getServerObj().getProcessTimerCnt() % 11) == 0) {
            // add or remove stock in Mutual fund account based on all stocks in the system
            System.gc();
            getAccountProcessImp().ProcessAddRemoveFundAccount(this);

            AccountMaintProcess maintProcess = new AccountMaintProcess();
            maintProcess.ProcessSystemMaintance(this);

        } else if ((getServerObj().getProcessTimerCnt() % 7) == 0) {
            updateStockAllSrv();
            NNService nnSrv = new NNService();
            nnSrv.AFprocessNeuralNet(this);
//            
            BillingProcess billProc = new BillingProcess();
            billProc.processUserBillingAll(this);

            PUBSUBprocess pubsub = new PUBSUBprocess();
            pubsub.ProcessPUBSUBAccount(this);

        } else if ((getServerObj().getProcessTimerCnt() % 5) == 0) {
//            TRprocessImp.UpdateAllStockTrend(this, true);
            updateStockAllSrv();

            AccountTranProcess accountTranP = new AccountTranProcess();
            accountTranP.ProcessAdminSignalTrading(this);

            getAccountProcessImp().ProcessAdminAddRemoveStock(this);

        } else if ((getServerObj().getProcessTimerCnt() % 3) == 0) {
            updateStockAllSrv();
            getAccountProcessImp().ProcessAllAccountTradingSignal(this);
            getAccountProcessImp().ProcessAdminAddRemoveStock(this);

//            
        } else if ((getServerObj().getProcessTimerCnt() % 2) == 0) {
            if (CKey.PROXY == false) {
                if (ServiceAFweb.processEmailFlag == true) {
                    EmailProcess eProcess = new EmailProcess();
                    eProcess.ProcessEmailAccount(this);
                }
            }
        } else {

        }
    }

    public static String debugSymbol = "HOU.TO";

    public static boolean javamainflag = false;
    public static boolean forceNNReadFileflag = false;
    public static boolean flagNNLearningSignal = false;

    public static boolean flagNNReLearning = false;
    public static boolean processNNSignalAdmin = false;
    public static boolean processRestinputflag = false;
    public static boolean processRestAllStockflag = false;

    public static boolean initLocalRemoteNN = false;

    public static boolean processEmailFlag = false;
    public static boolean processNeuralNetFlag = false;
    public static boolean nn1testflag = false;
    public static boolean nn2testflag = false;
    public static boolean nn3testflag = false;
    public static boolean nn30testflag = false;

/////////////////////////////////
    public static boolean mydebugtestflag = false;
    public static boolean mydebugtestNN3flag = false;

    public static boolean mydebugSim = false; //false;  
    public static long SimDateL = 0;

    public static boolean forceMarketOpen = false; //forceMarketOpen;

    public static boolean checkSymbolDebugTest(String symbol) {

        if (ServiceAFweb.mydebugtestNN3flag == true) {
            if (symbol.equals("GLD")) {

            } else if (symbol.equals("HOU.TO")) {

            } else if (symbol.equals("XIU.TO")) {

            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    private void AFprocessDebug() {
        //Feb 10, 2021 db size = 5,543 InnoDB utf8_general_ci 4.7 MiB	
        if (mydebugtestflag == true) {
            //set up run parm 
            // javamain localmysqlflag proxyflag mydebugtestflag
            // javamain localmysqlflag  mydebugtestflag

            // javamain localmysqlflag nn2testflag flagNNLearningSignal nndebugflag
            logger.info("Start mydebugtestflag.....");
            NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
            NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();
            NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();

            TradingSignalProcess TRprocessImp = new TradingSignalProcess();
            //select * FROM sampledb.neuralnetdata where name like '%NN2%';

            String symbol = "HOU.TO";
            int trNN = ConstantKey.INT_TR_NN1;
            int TR_NN = trNN;
            String nnName = ConstantKey.TR_NN1;
            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;

            symbol = "TSLA";

//            int size1yearAll = 20 * 12 * 5 + (50 * 3);
//            AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
//            ArrayList<AFstockInfo> StockInfoArray = this.getStockHistorical(stock.getSymbol(), size1yearAll);
//            if (StockInfoArray == null) {
//                ;
//            }
//            String symbolL[] = ServiceAFweb.allStock;
//            TradingNNprocess.CreateAllStockHistoryFile(this, symbolL, "nnAllStock");
//            ArrayList<AFstockInfo> stockInfoList = TradingNNprocess.getAllStockHistoryFile(this, "MSFT", "nnAllStock");
//            if (stockInfoList != null) {
//                logger.info("stockInfoList " + stockInfoList.size());
//            }
//            symbol = "T.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            getStockImp().deleteNeuralNetDataByBPname(BPnameSym);
//            trNN = ConstantKey.INT_TR_NN3;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            AFneuralNet nnObj1 = nn3ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            symbol = "GLD";
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, nnName, symbol);
////
//            nn3testflag = true;
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//////// clear TR_NN3
//            nnName = ConstantKey.TR_NN3;
//            BPnameSym = CKey.NN_version + "_" + nnName;
//            removeNeuralNetDataAllSymbolByTR(BPnameSym);
//////// clear TR_NN3
///////////////////////
//            nn3testflag = true;
//            nn3ProcBySig.NeuralNetNN3CreateJava(this, ConstantKey.TR_NN3);
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            symbol = "GLD";
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            //
//
//            symbol = "GLD";
//            trNN = ConstantKey.INT_TR_MACD;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_MACD;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
////            // http://localhost:8080/cust/admin1/acc/1/st/hou_to/tr/TR_nn2/tran/history/chart
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            nn3testflag = true;
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
            // javamain localmysqlflag nn3testflag  mydebugtestflag
//            TRprocessImp.ProcessAdminSignalTrading(this);
//
//            BillingProcess BP = new BillingProcess();
//            BP.processUserBillingAll(this);
            /////////// delete NN2
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            ArrayList stockNameArray = new ArrayList();
//            stockNameArray.add("BB.TO");
//            stockNameArray.add("SU");
//            stockNameArray.add("ENB.TO");            
//            stockNameArray.add("TSLA");              
//            if (stockNameArray != null) {
//                for (int i = 0; i < stockNameArray.size(); i++) {
//                    symbol = (String) stockNameArray.get(i);
//
//                    trNN = ConstantKey.INT_TR_NN2;
//                    TR_NN = trNN;
//                    nnName = ConstantKey.TR_NN2;
//                    BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                    getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                }
//            }
//            
//            nn1ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            symbol = "HOD.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            symbol = "AAPL";
//             AccountObj account = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
//            this.getAccountProcessImp().updateTradingAccountBalance(this, account, symbol); 
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            for (int i = 0; i < stockNameArray.size(); i++) {
//                symbol = (String) stockNameArray.get(i);
//                trNN = ConstantKey.INT_TR_NN40;
//                TR_NN = trNN;
//                nnName = ConstantKey.TR_NN40;
//                BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                getStockImp().deleteNeuralNet0Rel(BPnameSym);
//            }
//            for (int i = 0; i < 30; i++) {
//                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
//            }
//            nn1ProcBySig.TrainNN1NeuralNetBySign(this, symbol, TR_NN, null);
//            this.getAccountProcessImp().ProcessStockInfodeleteMaintance(this);
//            symbol = "FAS";
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            symbol = "FAZ";
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            BillingProcess billProc = new BillingProcess();
//            for (int i = 0; i < 10; i++) {
//                billProc.processUserBillingAll(this);
//            }       
//
//////////////////////////////// trading Simulation ////////////              
//////////////////////////////// trading Simulation ////////////  
//            symbol = "AAPL";
//
//            mydebugSim = true;
//            Calendar dateNow = TimeConvertion.getCurrentCalendar();
//            SimDateL = dateNow.getTimeInMillis();
//            SimDateL = TimeConvertion.endOfDayInMillis(SimDateL);
////            SimDateL = TimeConvertion.addDays(SimDateL, -10);
//
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            AccountObj accountAdminObj = getAdminObjFromCache();
//
//            boolean flag1 = true;
//            if (flag1 == true) {
//                int retSatus = NNProcessImp.ClearStockNNTranHistory(this, nnName, symbol);
//                TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            } else {
//
//                for (int i = 0; i < 15; i++) {
//                    SimDateL = TimeConvertion.addDays(SimDateL, 1);
//
//                    TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTRPerf(this, accountAdminObj, symbol);
//                }
//            }
//////////////////////////////// trading Simulation ////////////  
//////////////////////////////// trading Simulation ////////////  
///////////////////////////////////////////////////// Update new TR NN91 in Admin
//            ///////Adding  new TR in Admin Sotcks
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
//            if (accountObj.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
//                for (int i = 0; i < stockNameList.size(); i++) {
//                    symbol = (String) stockNameList.get(i);
//                    AFstockObj stock = getRealTimeStockImp(symbol);
//                    TradingRuleObj tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN91);
//                    tr.setType(ConstantKey.INT_TR_NN91);
//                    tr.setComment("");
//                    int retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//                    tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN92);
//                    tr.setType(ConstantKey.INT_TR_NN92);
//                    tr.setComment("");
//                    retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//                    tr = new TradingRuleObj();
//                    tr.setTrname(ConstantKey.TR_NN93);
//                    tr.setType(ConstantKey.INT_TR_NN93);
//                    tr.setComment("");
//                    retAdd = this.getAccountImp().accountdb.addAccountStock(accountObj.getId(), stock.getId(), tr);
//
//                }
//            }
//            ///////Adding  new TR in Admin Sotcks
///////////////////////////////////////////////////// Update stock
//            TRprocessImp.UpdateAllStock(this);
//            AFstockObj stock = getRealTimeStockImp(symbol);
//            TRprocessImp.updateRealTimeStock(this, stock);
/////////////////////////////////////////////////////            
//            if (nn3testflag == true) {
            // javamain localmysqlflag nn3testflag mydebugtestflag
            // http://localhost:8080/cust/admin1/acc/1/st/gld/tr/TR_NN3/tran/history/chart
            // http://localhost:8080/cust/admin1/acc/1/st/gld/tr/TR_NN3/perf
            // https://iiswebsrv.herokuapp.com/cust/admin1/acc/1/st/gld/tr/TR_NN2/tran/history/chart
//                symbol = "HOU.TO"; // "GLD";
//                trNN = ConstantKey.INT_TR_NN3;
//                TR_NN = trNN;
//                nnName = ConstantKey.TR_NN3;
//                BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                TradingNNprocess NNProcessImp = new TradingNNprocess();
//                NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();
//                boolean init = true;
//                
//                init = false;
//                if (init == true) {
//                    for (int j = 0; j < 5; j++) {
//                        nn3ProcBySig.TrainNN3NeuralNetBySign(this, symbol, ConstantKey.INT_TR_NN3, null);
//                        NNProcessImp.ReLearnInputNeuralNet(this, symbol, ConstantKey.INT_TR_NN3);
//
//                    }
//                    
//                } else {
//                    int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, symbol);
//
//                    TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//                    TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//                }
//            }
////////////////////////////////////////////////////////
//            systemRemoveAllEmail();
//            BillingProcess billProc = new BillingProcess();
//            for (int i = 0; i < 10; i++) {
//                billProc.processUserBillingAll(this);
//            }
//            ArrayList custNameList = getCustomerObjByNameList(CKey.G_USERNAME);
//            CustomerObj customer = (CustomerObj) custNameList.get(0);
//            billProc.updateUserBilling(this, customer);
//            getAccountImp().removeCommByName(CKey.ADMIN_USERNAME, null, ConstantKey.COM_EMAIL);
//            
//
//            EmailProcess eProcess = new EmailProcess();
//            ServiceAFweb.processEmailFlag = true;
//            String tzid = "America/New_York"; //EDT
//            TimeZone tz = TimeZone.getTimeZone(tzid);
//            java.sql.Date d = new java.sql.Date(System.currentTimeMillis());
//            DateFormat format = new SimpleDateFormat(" hh:mm a");
//            format.setTimeZone(tz);
//            String ESTdate = format.format(d);
//            String sig = "exit";
//            String msg = ESTdate + " " + symbol + " Sig:" + sig;
//            AccountObj accountObj = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
//            getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.COM_EMAIL, msg);
//            for (int i = 0; i < 100; i++) {
//                eProcess.ProcessEmailAccount(this);
//                try {
//                    Thread.sleep(30 * 1000);
//                } catch (InterruptedException ex) {
//                    Thread.currentThread().interrupt();
//                }
//            }
////////////////////////////////////////////////////////////////////
//             AFstockObj stock = getRealTimeStockImp(symbol);
//             int resultUpdate = TRprocessImp.updateRealTimeStock(this, stock);
//            getAccountProcessImp().downloadDBData(this);
//            NN1ProcessByTrend nn1trend = new NN1ProcessByTrend();
//            TrandingSignalProcess.forceToGenerateNewNN = false;
//            NN1ProcessBySignal.processRestinputflag = true;
//            NN2ProcessByTrend nn2trend = new NN2ProcessByTrend();
//            nn2trend.processNN40InputNeuralNetTrend(this);
//            nn2trend.processAllNN40StockInputNeuralNetTrend(this);
//            nn1trend.processNN30InputNeuralNetTrend(this);
//            nn1trend.processAllNN30StockInputNeuralNetTrend(this);
//            int ret = this.getAccountProcessImp().saveDBneuralnetProcess(this, "neuralnet");
//            AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//            delete NN2            
//            AccountObj accountObj = getAdminObjFromCache();
//            ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());
//            if (stockNameArray != null) {
//                for (int i = 0; i < stockNameArray.size(); i++) {
//                    symbol = (String) stockNameArray.get(i);
//
//                    trNN = ConstantKey.INT_TR_NN2;
//                    TR_NN = trNN;
//                    nnName = ConstantKey.TR_NN2;
//                    BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                    getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//                }
//            }
//
//////////////////////////////////////////////////////////////
//            symbol = "H.TO";
//            trNN = ConstantKey.INT_TR_NN2;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN2;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);
//            trNN = ConstantKey.INT_TR_NN1;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN1;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);            
//            trNN = ConstantKey.INT_TR_NN30;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN30;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);   
//            trNN = ConstantKey.INT_TR_NN40;
//            TR_NN = trNN;
//            nnName = ConstantKey.TR_NN40;
//            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetDataObj(BPnameSym, 0);    
//            
//            int accountId = 3;
//            AccountObj accountObj = SystemAccountObjByAccountID(accountId);
//            getAccountProcessImp().updateTradingTransaction(this, accountObj, symbol);
//            for (int j = 0; j < 3; j++) {
//                AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainSignalNeuralNet(this, BPnameSym, TR_NN, symbol);
//                getStockImp().deleteNeuralNet1(BPnameSym);
//                
//                NN2ProcessBySignal nn2Process = new NN2ProcessBySignal();
//                nn2Process.inputReTrainNN2StockNeuralNetData(this, trNN, symbol);
//            }
//
///////////////////////////////////////////////////////////////////////
//            symbol = "HOU.TO";
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
////            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//
//            symbol = "T.TO";
//            AccountObj account = getAccountImp().getAccountByType("GUEST", null, AccountObj.INT_TRADING_ACCOUNT);
//            AFstockObj stock = getRealTimeStockImp(symbol);
//            TradingRuleObj trObj = getAccountStockByTRname("GUEST", null, account.getId() + "", symbol, ConstantKey.TR_ACC);
//            ArrayList<PerformanceObj> currentPerfList = this.SystemAccountStockPerfList(account.getId(), stock.getId(), trObj.getTrname(), 1);
//
//////////////////////////////////////////////////////////////
//            String symbol = "HOU.TO";
//            int trNN = ConstantKey.INT_TR_NN2;
//            String nnName = ConstantKey.TR_NN2;
//            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            // http://localhost:8080/cust/admin1/acc/1/st/hou_to/tr/TR_nn2/tran/history/chart
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
//
/////////////////////////////////////////////////////////////
//            getStockImp().deleteNeuralNet1(BPnameSym);
//            AFneuralNet nnObj1 = nn2ProcBySig.ProcessTrainNeuralNet1(this, BPnameSym, trNN, symbol);
//            int ret = nn2ProcBySig.inputReTrainNN2StockNeuralNetData(this, trNN, symbol);
//
//            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            AFstockObj stock = getRealTimeStockImp(symbol);
//
//            getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);
            logger.info("End mydebugtestflag.....");
        }

        ///// only acc reset
//        boolean flagTran_TR_ACC = false;
//        if (flagTran_TR_ACC == true) {
//            SystemClearNNtranAllAcc();
//        }
        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work        
//        boolean flaginputStock = false;
//        if (flaginputStock == true) {
//            StockInternet.updateAllStockFile(this);
//        }
        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work 
//        boolean saveStockFileFlag = false;
//        if (saveStockFileFlag == true) {
//            ArrayList stockNameArray = getAllOpenStockNameArray();
//            logger.info("updateRealTimeStock " + stockNameArray.size());
//            for (int i = 0; i < stockNameArray.size(); i++) {
//                String sym = (String) stockNameArray.get(i);
//                ArrayList<String> writeArray = new ArrayList();
//                int size1year = 5 * 52;
//                ArrayList StockArray = getStockHistorical(sym, size1year * 4);
//                if (StockArray == null) {
//                    continue;
//                }
//                if (StockArray.size() == 0) {
//                    continue;
//                }
//                String StFileName = FileLocalPath + sym + ".txt";
//                logger.info("saveStockFile Size " + StockArray.size() + " " + StFileName);
//                for (int j = 0; j < StockArray.size(); j++) {
//                    try {
//                        AFstockInfo obj = (AFstockInfo) StockArray.get(j);
//                        String st = new ObjectMapper().writeValueAsString(obj);
//                        writeArray.add(st);
//                    } catch (JsonProcessingException ex) {
//                    }
//                }
//                FileUtil.FileWriteTextArray(StFileName, writeArray);
//            }
//        }
//        boolean flagClearNN0Table = false;
//        if (flagClearNN0Table == true) {
//            this.getStockImp().deleteNeuralNet0Table();
//        }
//
//        boolean flagClearNN1Table = false;
//        if (flagClearNN1Table == true) {
//            this.getStockImp().deleteNeuralNet1Table();
//        }
//
//        boolean flagClearNNdataTable = false;
//        if (flagClearNNdataTable == true) {
//            this.getStockImp().deleteNeuralNetDataTable();
//        }
        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////
    }

    public void debugtest() {
//        String symbol = "IWM";
//        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
//        int size1yearAll = 20 * 12 * 5 + (50 * 3);
//        ArrayList<AFstockInfo> StockArray = getStockHistorical(symbol, size1yearAll);

    }

//////////////////////////////////////////////////
    public static void AFSleep1Sec(int sec) {
        // delay seems causing openshif not working        
//        if (true) {
//            return;
//        }
        try {
            Thread.sleep(sec * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static void AFSleep() {
        try {
            // delay seems causing openshif not working
//        if (true) {
//            return;
//        }
            Thread.sleep(10);
        } catch (InterruptedException ex) {

        }
    }

    private void RandomDelayMilSec(int sec) {

        // delay seems causing openshif not working
        if (true) {
            return;
        }
        try {
            int max = sec + 100;
            int min = sec;
            Random randomNum = new Random();
            int sleepRandom = min + randomNum.nextInt(max);

            if (sleepRandom < 0) {
                sleepRandom = sec;
            }

            Thread.sleep(sleepRandom);
        } catch (InterruptedException ex) {
            logger.info("> RandomDelayMilSec exception " + ex.getMessage());
        }
    }

    ///////////////////////////////////
    public static boolean checkCallRemoteSQL_Mysql() {
        boolean ret = false;
        if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
            ret = true;
        }
        return ret;
    }

    public static boolean checkCallRemoteMysql() {
        boolean ret = true;
        if (ServiceAFweb.getServerObj().isLocalDBservice() == true) {
            ret = false;
        }
        return ret;
    }

    ///////////////////////////////////////
    public CustomerObj getCustomerIgnoreMaintenance(String EmailUserName, String Password) {

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        return getAccountImp().getCustomerPassword(UserName, Password);
    }

    public CustomerObj getCustomerPassword(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj != null) {
            if (custObj.getStatus() != ConstantKey.OPEN) {
                custObj.setUsername("");
                custObj.setPassword("");
            }
        }
        return custObj;
    }
//

    ////////////////////////////
    public ArrayList getRemoveStockNameList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = this.getStockImp().getAllRemoveStockNameList(length);

        return result;
    }

    public ArrayList getDisableStockNameList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = this.getStockImp().getAllDisableStockNameList(length);

        return result;
    }

    public CustomerObj getCustomerObjByName(String name) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        ArrayList<CustomerObj> custList = getAccountImp().getCustomerObjByNameList(name);
        if (custList != null) {
            if (custList.size() > 0) {
                return custList.get(0);
            }
        }
        return null;
    }

    public CustomerObj getCustomerByAccoutObj(AccountObj accObj) {
        CustomerObj result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = getAccountImp().getCustomerByAccoutObj(accObj);
        return result;
    }

    //////////////////////////////////////////////////
    // NNService
    //////////////////////////////////////////////////    
    public void fileNNInputOutObjListServ(ArrayList<NNInputDataObj> inputList, String symbol, int stockId, String filename) {
        if (true) {
            NNService nnSrv = new NNService();
            nnSrv.fileNNInputOutObjList(this, inputList, symbol, stockId, filename);
        }
    }

    public String getAccountStockTRListHistoryChartServ(ArrayList<StockTRHistoryObj> thObjListMain, String stockidsymbol, String trname, String pathSt) {
        if (true) {
            ChartService chartSrv = new ChartService();
            return chartSrv.getAccountStockTRListHistoryChartToFile(this, thObjListMain, stockidsymbol, trname, pathSt);
        }
        return "";
    }

    //////////////////////////////////////////////////
    // CustAccService
    CustAccService custAccSrv = new CustAccService();

    //////////////////////////////////////////////////
    public ArrayList getAccountListServ(String EmailUserName, String Password) {
        if (true) {
            return custAccSrv.getAccountList(this, EmailUserName, Password);
        }
        return null;
    }

    public AccountObj getAccountByCustomerAccountIDServ(String EmailUserName, String Password, String AccountIDSt) {
        if (true) {
            return custAccSrv.getAccountByCustomerAccountID(this, EmailUserName, Password, AccountIDSt);
        }
        return null;
    }

    public float getAccountStockBalanceServ(TradingRuleObj trObj) {
        if (true) {
            return custAccSrv.getAccountStockRealTimeBalance(this, trObj);
        }
        return -9999;
    }

    public int addAccountStockByAccountServ(AccountObj accountObj, String symbol) {
        if (true) {
            return custAccSrv.addAccountStockByAccount(this, accountObj, symbol);
        }
        return 0;
    }

    public int addAccountStockByCustAccServ(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (true) {
            return custAccSrv.addAccountStockByCustAcc(this, EmailUserName, Password, AccountIDSt, symbol);
        }
        return 0;
    }

    public int removeAccountStockByUserNameAccIdServ(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (true) {
            return custAccSrv.removeAccountStockByUserNameAccId(this, EmailUserName, Password, AccountIDSt, symbol);
        }
        return 0;
    }
//    //ConstantKey.NOTEXISTED

    public int removeAccountStockSymbolServ(AccountObj accountObj, String symbol) {
        if (true) {
            return custAccSrv.removeAccountStockSymbol(this, accountObj, symbol);
        }
        return 0;
    }

    public TradingRuleObj getAccountStockTRByTRnameServ(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        if (true) {
            return custAccSrv.getAccountStockTRByTRname(this, EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        }
        return null;
    }

    public ArrayList<TransationOrderObj> getAccountStockTRTranListByAccountIDServ(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (true) {
            return custAccSrv.getAccountStockTRTranListByAccountID(this, EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
        }
        return null;
    }

    public int getAccountStockTRListHistoryDisplayProcessServ(ArrayList<StockTRHistoryObj> trObjList, ArrayList<String> writeArray, ArrayList<String> displayArray) {
        if (true) {
            return custAccSrv.getAccountStockTRListHistoryDisplayProcess(trObjList, writeArray, displayArray);
        }
        return 0;
    }

//    //only on type=" + CustomerObj.INT_CLIENT_BASIC_USER;
    public ArrayList getExpiredCustomerListServ(int length) {
        if (true) {
            return custAccSrv.getExpiredCustomerList(this, length);
        }
        return null;
    }
//    // need ConstantKey.DISABLE status beofore remove customer

    public int removeCustomer(String EmailUserName) {
        if (true) {
            return custAccSrv.removeCustomer(this, EmailUserName);
        }
        return 0;
    }

    public boolean SystemFundResetGlobalServ() {
        if (true) {
            return custAccSrv.SystemFundResetGlobal(this);
        }
        return false;
    }

    public boolean SystemFundSelectBestServ() {
        if (true) {
            return custAccSrv.SystemFundSelectBest(this);
        }
        return false;
    }

    public boolean SystemFundPocessAddRemoveServ() {
        if (true) {
            return custAccSrv.SystemFundPocessAddRemove(this);
        }
        return false;
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    public ArrayList SystemUserNamebyAccountID(int accountID) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {

            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UserNamebyAccountID + "");
            sqlObj.setReq("" + accountID);
            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return null;
            }
            ArrayList<String> NameList = new ArrayList();

            try {
                NameList = new ObjectMapper().readValue(output, ArrayList.class
                );
            } catch (Exception ex) {
                logger.info("> SystemUserNamebyAccountID exception " + ex.getMessage());
            }
            return NameList;
        }
        return getAccountImp().getUserNamebyAccountID(accountID);
    }

    public ArrayList<AFstockInfo> SystemStockHistoricalRange(String symbol, long start, long end) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            try {
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.StockHistoricalRange + "");
                sqlObj.setReq(symbol);
                sqlObj.setReq1(start + "");
                sqlObj.setReq2(end + "");

                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }

                ArrayList<AFstockInfo> trArray = null;
                AFstockInfo[] arrayItem = new ObjectMapper().readValue(output, AFstockInfo[].class
                );

                List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
                trArray = new ArrayList<AFstockInfo>(listItem);
                return trArray;

            } catch (Exception ex) {
                logger.info("> SystemStockHistoricalRange exception " + ex.getMessage());
            }
            return null;
        }

        return getStockHistoricalRangeServ(this, symbol, start, end);
    }

    public ArrayList<String> SystemAccountStockNameList(int accountId) {
        ArrayList<String> NameList = new ArrayList();
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AccountStockNameList + "");
            sqlObj.setReq("" + accountId);
            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return NameList;
            }
            try {
                NameList = new ObjectMapper().readValue(output, ArrayList.class
                );
            } catch (Exception ex) {
                logger.info("> SystemAccountStockNameList exception " + ex.getMessage());
            }
            return NameList;
        }
        return getAccountImp().getAccountStockNameList(accountId);
    }

    public ArrayList SystemAllOpenAccountIDList() {
        ArrayList<String> NameList = new ArrayList();
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllOpenAccountIDList + "");

            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return NameList;

            }

            try {
                NameList = new ObjectMapper().readValue(output, ArrayList.class);
            } catch (Exception ex) {
                logger.info("> SystemAllOpenAccountIDList exception " + ex.getMessage());
            }
            return NameList;
        }
        return getAccountImp().getAllOpenAccountID();
    }

    public ArrayList SystemAllAccountStockNameListExceptionAdmin(int accountId) {
        ArrayList<String> NameList = new ArrayList();
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllAccountStockNameListExceptionAdmin + "");
            sqlObj.setReq(accountId + "");
            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);

            String output = sqlObjresp.getResp();
            if (output == null) {
                return NameList;

            }

            try {
                NameList = new ObjectMapper().readValue(output, ArrayList.class
                );
            } catch (Exception ex) {
                logger.info("> AllAccountStockNameListExceptionAdmin exception " + ex.getMessage());
            }
            return NameList;
        }
        return getAccountImp().getAllAccountStockNameListExceptionAdmin(accountId);

    }

    public AFstockObj SystemRealTimeStockByStockID(int stockId) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.RealTimeStockByStockID + "");
            sqlObj.setReq("" + stockId);
            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return null;
            }
            AFstockObj stockObj = null;

            try {
                stockObj = new ObjectMapper().readValue(output, AFstockObj.class
                );
            } catch (Exception ex) {
                logger.info("> SystemRealTimeStockByStockID exception " + ex.getMessage());
            }
            return stockObj;
        }
        return getStockByStockIDServ(stockId);
    }

    public AccountObj SystemAccountObjByAccountID(int accountId) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AccountObjByAccountID + "");
            sqlObj.setReq("" + accountId);
            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
            String output = sqlObjresp.getResp();
            if (output == null) {
                return null;
            }
            AccountObj accountObj = null;

            try {
                accountObj = new ObjectMapper().readValue(output, AccountObj.class
                );
            } catch (Exception ex) {
                logger.info("> SystemAccountObjByAccountID exception " + ex.getMessage());
            }
            return accountObj;
        }
        return getAccountImp().getAccountObjByAccountID(accountId);
    }

    public TradingRuleObj SystemAccountStockIDByTRname(int accountID, int stockID, String trName) {

        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        if (checkCallRemoteMysql() == true) {
            try {
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AccountStockIDByTRname + "");

                sqlObj.setReq(accountID + "");
                sqlObj.setReq1(stockID + "");
                sqlObj.setReq2(trName);

                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;

                }

                TradingRuleObj ret = new ObjectMapper().readValue(output, TradingRuleObj.class
                );
                return ret;

            } catch (Exception ex) {
                logger.info("> SystemAccountStockIDByTRname exception " + ex.getMessage());
            }
        }
        return getAccountImp().getAccountStockIDByTRStockID(accountID, stockID, trName);
    }

    public int SystemAccountStockClrTranByAccountID(AccountObj accountObj, int stockId, String trName) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        if (checkCallRemoteMysql() == true) {
            try {
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AccountStockClrTranByAccountID + "");

                String st = new ObjectMapper().writeValueAsString(accountObj);
                sqlObj.setReq(st);
                sqlObj.setReq1(stockId + "");
                sqlObj.setReq2(trName);

                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;

                }

                int result = new ObjectMapper().readValue(output, Integer.class
                );
                return result;

            } catch (Exception ex) {
                logger.info("> SystemAccountStockClrTranByAccountID exception " + ex.getMessage());
            }
            return 0;
        }

        return getAccountImp().clearAccountStockTranByAccountID(accountObj, stockId, trName.toUpperCase());

    }

    public ArrayList<TradingRuleObj> SystemAccountStockListByAccountID(int accountId, String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        if (checkCallRemoteMysql() == true) {
            try {
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AccountStockListByAccountID + "");

                sqlObj.setReq(accountId + "");
                sqlObj.setReq1(symbol);

                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<TradingRuleObj> trArray = null;

                TradingRuleObj[] arrayItem = new ObjectMapper().readValue(output, TradingRuleObj[].class
                );
                List<TradingRuleObj> listItem = Arrays.<TradingRuleObj>asList(arrayItem);
                trArray = new ArrayList<TradingRuleObj>(listItem);
                return trArray;

            } catch (Exception ex) {
                logger.info("> SystemAccountStockListByAccountID exception " + ex.getMessage());
            }
            return null;
        }
        AFstockObj stock = getStockBySymServ(symbol);
        int stockID = stock.getId();
        return getAccountImp().getAccountStockTRListByAccountID(accountId, stockID);
    }

    public ArrayList<TradingRuleObj> SystemAccountStockListByAccountIDStockID(int accountId, int stockId) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        if (checkCallRemoteMysql() == true) {
            try {
                RequestObj sqlObj = new RequestObj();
                sqlObj.setCmd(ServiceAFweb.AccountStockListByAccountIDStockID + "");

                sqlObj.setReq(accountId + "");
                sqlObj.setReq1(stockId + "");

                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<TradingRuleObj> trArray = null;

                TradingRuleObj[] arrayItem = new ObjectMapper().readValue(output, TradingRuleObj[].class
                );
                List<TradingRuleObj> listItem = Arrays.<TradingRuleObj>asList(arrayItem);
                trArray = new ArrayList<TradingRuleObj>(listItem);
                return trArray;

            } catch (Exception ex) {
                logger.info("> SystemAccountStockListByAccountIDStockID exception " + ex.getMessage());
            }
            return null;
        }

        return getAccountImp().getAccountStockTRListByAccountID(accountId, stockId);
    }

//    public int SystemUpdateSQLList(ArrayList<String> SQLlist) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.UpdateSQLList + "");
//            String st;
//            try {
//                st = new ObjectMapper().writeValueAsString(SQLlist);
//                sqlObj.setReq(st);
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return 0;
//
//                }
//                int result = new ObjectMapper().readValue(output, Integer.class
//                );
//                return result;
//            } catch (Exception ex) {
//                logger.info("> SystemUpdateSQLList exception " + ex.getMessage());
//            }
//            return 0;
//        }
//        return updateSQLArrayListServ(this, SQLlist);
//    }
    public ArrayList<AFneuralNetData> SystemNeuralNetDataObj(String BPnameTR) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.NeuralNetDataObj + "");
            String st;
            try {
                sqlObj.setReq(BPnameTR + "");
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<AFneuralNetData> trArray = null;

                AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class
                );
                List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
                trArray = new ArrayList<AFneuralNetData>(listItem);
                return trArray;
            } catch (Exception ex) {
                logger.info("> SystemNeuralNetDataObj exception " + ex.getMessage());
            }
            return null;
        }
        return getStockImp().getNeuralNetDataObj(BPnameTR, 0);
    }

    public ArrayList<AFneuralNetData> SystemNeuralNetDataObjStockid(String BPname, int stockId, long updatedatel) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.NeuralNetDataObjStockid + "");
            String st;
            try {
                sqlObj.setReq(BPname + "");
                sqlObj.setReq1(stockId + "");
                sqlObj.setReq2(updatedatel + "");
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<AFneuralNetData> trArray = null;

                AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class
                );
                List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
                trArray = new ArrayList<AFneuralNetData>(listItem);
                return trArray;
            } catch (Exception ex) {
                logger.info("> SystemNeuralNetDataObjStockid exception " + ex.getMessage());
            }
            return null;
        }
        return getStockImp().getNeuralNetDataObj(BPname, stockId, updatedatel);
    }

    //  entrydatel desc recent transaction first
    public ArrayList<TransationOrderObj> SystemAccountStockTransList(int accountID, int stockID, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AccountStockTransList + "");
            String st;
            try {
                sqlObj.setReq(accountID + "");
                sqlObj.setReq1(stockID + "");
                sqlObj.setReq2(trName);
                sqlObj.setReq3(length + "");
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<TransationOrderObj> trArray = null;

                TransationOrderObj[] arrayItem = new ObjectMapper().readValue(output, TransationOrderObj[].class
                );
                List<TransationOrderObj> listItem = Arrays.<TransationOrderObj>asList(arrayItem);
                trArray = new ArrayList<TransationOrderObj>(listItem);
                return trArray;
            } catch (Exception ex) {
                logger.info("> SystemAccountStockTransList exception " + ex.getMessage());
            }
            return null;
        }
        return getAccountImp().getAccountStockTransList(accountID, stockID, trName, length);
    }

    public ArrayList<PerformanceObj> SystemAccountStockPerfList(int accountID, int stockID, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AccountStockPerfList + "");
            String st;
            try {
                sqlObj.setReq(accountID + "");
                sqlObj.setReq1(stockID + "");
                sqlObj.setReq2(trName);
                sqlObj.setReq3(length + "");
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return null;
                }
                if (output.equals(ConstantKey.nullSt)) {
                    return null;
                }
                ArrayList<PerformanceObj> trArray = null;

                PerformanceObj[] arrayItem = new ObjectMapper().readValue(output, PerformanceObj[].class
                );
                List<PerformanceObj> listItem = Arrays.<PerformanceObj>asList(arrayItem);
                trArray = new ArrayList<PerformanceObj>(listItem);
                return trArray;
            } catch (Exception ex) {
                logger.info("> SystemAccountStockPerfList exception " + ex.getMessage());
            }
            return null;
        }
        return getAccountImp().getAccountStockPerfList(accountID, stockID, trName, length);
    }

    public String SystemSQLquery(String SQL) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return "";
//        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.AllSQLquery + "");

            try {
                sqlObj.setReq(SQL);
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return "";
                }

                return output;
            } catch (Exception ex) {
                logger.info("> SystemSQLquery exception " + ex.getMessage());
            }
            return "";
        }
        return getAccountImp().getAllSQLquery(SQL);
    }

    public int SystemAddTransactionOrder(AccountObj accountObj, AFstockObj stock, String trName, int tranSignal, Calendar tranDate) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        AccountTranImp accountTran = new AccountTranImp();
        return accountTran.AddTransactionOrder(this, accountObj, stock, trName, tranSignal, tranDate, true);
    }

    public int SystemuUpdateTransactionOrder(ArrayList<String> transSQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UpdateTransactionOrder + "");
            String st;
            try {
                st = new ObjectMapper().writeValueAsString(transSQL);
                sqlObj.setReq(st);
                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
                String output = sqlObjresp.getResp();
                if (output == null) {
                    return 0;

                }
                int result = new ObjectMapper().readValue(output, Integer.class
                );
                return result;
            } catch (Exception ex) {
                logger.info("> SystemuUpdateTransactionOrder exception " + ex.getMessage());
            }
            return 0;
        }
        return getAccountImp().updateTransactionOrder(transSQL);
    }

//////////
    public int updateAccountStockSignal(TRObj stockTRObj) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return getAccountImp().updateAccountStockSignal(stockTRObj.getTrlist());

    }

    public int systemRemoveAllEmail() {
        getAccountImp().removeCommByType(CKey.ADMIN_USERNAME, null, ConstantKey.INT_TYPE_COM_EMAIL);
        return 1;
    }

/////////////////////////////////////////        
/////////////////////////////////////////        
//
//    public static HashMap<String, ArrayList> stockInputMapFile = null;
//
//    public static boolean CreateAllStockHistoryFile(ServiceAFweb serviceAFWeb, String symbolL[], String fileName) {
//        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
//
//        try {
//            ProcessAllStockHistoryCreatJava(serviceAFWeb, symbolL, stockInputMap);
//
//            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
//            String inputListSt = ServiceAFweb.compress(inputListRawSt);
//
//            ArrayList msgWrite = new ArrayList();
//
//            int sizeline = 1000;
//            int len = inputListSt.length();
//            int beg = 0;
//            int end = sizeline;
//            while (true) {
//                String st = inputListSt.substring(beg, end);
//
//                msgWrite.add(st);
//
//                if (end >= len) {
//                    break;
//                }
//                beg = end;
//                if (end + sizeline <= len) {
//                    end += sizeline;
//                } else {
//                    end = len;
//                }
//            }
//
//            ////// end
//            String fileN = ServiceAFweb.FileLocalPath + fileName + ".txt";
//            FileUtil.FileWriteTextArray(fileN, msgWrite);
//
//            return true;
//        } catch (Exception ex) {
//        }
//        return false;
//    }
//
//    public static ArrayList<AFstockInfo> getAllStockHistoryFile(ServiceAFweb serviceAFWeb, String symbol, String fileName) {
//        if (stockInputMapFile == null) {
//            try {
//
//                String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".txt";
//                if (getEnv.checkLocalPC() == true) {
//                    fileN = ServiceAFweb.FileLocalPath + fileName + ".txt";
//                }
//                ArrayList msgRead = new ArrayList();
//                boolean ret = FileUtil.FileReadTextArray(fileN, msgRead);
//                if (ret == true) {
//                    StringBuffer msgWrite = new StringBuffer();
//                    for (int i = 0; i < msgRead.size(); i++) {
//                        msgWrite.append(msgRead.get(i));
//                    }
//                    String inputListSt = ServiceAFweb.decompress(msgWrite.toString());
//                    stockInputMapFile = new ObjectMapper().readValue(inputListSt, HashMap.class);
//                }
//            } catch (Exception ex) {
//
//            }
//
//        }
//        ArrayList<AFstockInfo> stockInfoList = ProcessAllStockHistoryfromStaticCode(symbol, stockInputMapFile);
//        return stockInfoList;
//
//    }
    public static HashMap<String, ArrayList> stockInputMap = null;
    public static HashMap<String, ArrayList> stockInputMap_1 = null;

    public static boolean CreateStaticStockHistoryServ(ServiceAFweb serviceAFWeb, String symbolL[], String fileName, String tagName) {
        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            ProcessAllStockHistoryCreatJava(serviceAFWeb, symbolL, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.processstockinfo;\n"
                    + "\n"
                    + "/*  This file is generated by system. Do not modify. */\n"
                    + "public class " + fileName + " {\n"
                    + "\n");

            int sizeline = 1000;
            int len = inputListSt.length();
            int beg = 0;
            int end = sizeline;
            int index = 1;
            int line = 0;
            while (true) {
                if (line == 0) {
                    msgWrite.append(""
                            + "    public static String " + tagName + index + " = \"\"\n"
                            + "            + \"\"\n");
                }
                line++;
                String st = inputListSt.substring(beg, end);

                msgWrite.append("+ \"" + st + "\"\n");

                if (end >= len) {
                    msgWrite.append(""
                            + "            + \"\";\n");

                    break;
                }
                if (line == 20) {
                    msgWrite.append(""
                            + "            + \"\";\n");
                    line = 0;
                    index++;
                }
                beg = end;
                if (end + sizeline <= len) {
                    end += sizeline;
                } else {
                    end = len;
                }
            }

            ////// end
            msgWrite.append(""
                    + "}\n"
                    ///
                    + ""
            );
            String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".java";
            FileUtil.FileWriteText(fileN, msgWrite);

            StringBuffer msgWrite_src = new StringBuffer();
            msgWrite_src.append("" ///
                    + "package com.afweb.processstockinfo;\n"
                    + "import com.afweb.service.ServiceAFweb;\n"
                    + "import com.fasterxml.jackson.databind.ObjectMapper;\n"
                    + "import java.util.ArrayList;\n"
                    + "import java.util.HashMap;\n"
                    + "/*  This file is generated by system. Do not modify. */\n"
                    + "public class " + fileName + "_src {\n"
                    + "\n");
            /*
             */
            String javaSt = "";
            javaSt = ""
                    + "public static HashMap<String, ArrayList> AllStockHistoryStaticCodeInit(HashMap<String, ArrayList> stockInputMap) {\n"
                    + "StringBuffer inputBuf = new StringBuffer();\n"
                    + "try {\n"
                    + "";

            msgWrite_src.append(javaSt + "\n");
            for (int i = 1; i < index + 1; i++) {
                javaSt = ""
                        + "inputBuf.append(" + fileName + "." + tagName + i + ");\n"
                        + "";
                msgWrite_src.append(javaSt);
            }
            javaSt = ""
                    + "String inputListSt = ServiceAFweb.decompress(inputBuf.toString());\n"
                    + "stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);\n"
                    + "return stockInputMap;\n"
                    + "} catch (Exception ex) {\n"
                    + "}\n"
                    + "return stockInputMap;\n"
                    + "}\n"
                    + "";
            msgWrite_src.append(javaSt + "\n");

            ////// end
            msgWrite_src.append(""
                    + "}\n"
                    ///
                    + ""
            );
            String fileN_src = ServiceAFweb.FileLocalDebugPath + fileName + "_src.java";
            FileUtil.FileWriteText(fileN_src, msgWrite_src);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private static void ProcessAllStockHistoryCreatJava(ServiceAFweb serviceAFWeb, String symbolL[], HashMap<String, ArrayList> stockInputMap) {
        boolean saveStockDBFlag = true;
        if (saveStockDBFlag == true) {

            StockInternetImpDao internet = new StockInternetImpDao();
            ArrayList stockNameArray = new ArrayList();

            if (symbolL == null) {
                return;
            }

            for (int i = 0; i < symbolL.length; i++) {
                stockNameArray.add(symbolL[i]);
            }
            logger.info("AllStockHistoryCreatJavaProcess " + stockNameArray.size());

            int sizeyear = 5 * 52 * 5;
            for (int k = 0; k < stockNameArray.size(); k++) {
                String symbol = (String) stockNameArray.get(k);

                String StFileName = ServiceAFweb.FileLocalDebugPath + symbol + ".txt";

                ArrayList<String> writeArray = new ArrayList();
                ArrayList<AFstockInfo> StockArray = null;

                try {
                    // always the earliest day first  
                    StockArray = internet.GetStockHistoricalInternet(symbol, sizeyear);
                } catch (Exception ex) {

                }
                if (StockArray == null) {
                    continue;
                }
                if (StockArray.size() < 3) {
                    continue;
                }
                // skiping first 3 days (last days is not final
                for (int j = 5; j < StockArray.size(); j++) {
                    try {
                        AFstockInfo obj = StockArray.get(j);
                        String st = new ObjectMapper().writeValueAsString(obj);
                        writeArray.add(st);
                    } catch (Exception ex) {
                        writeArray = null;
                        break;
                    }
                }
                if (writeArray == null) {
                    continue;
                }
                FileUtil.FileWriteTextArray(StFileName, writeArray);
                ///////////////////////
                FileUtil.FileReadTextArray(StFileName, writeArray);
                if (writeArray.size() == 0) {
                    continue;
                }
                StockArray = new ArrayList();
                for (int j = 0; j < writeArray.size(); j++) {
                    String st = writeArray.get(j);
                    try {
                        AFstockInfo stockInfo = new ObjectMapper().readValue(st, AFstockInfo.class);
                        StockArray.add(stockInfo);
                    } catch (Exception ex) {
                    }
                }
                //////////
                if (StockArray == null) {
                    continue;
                }
                logger.info(">>> AllStockHistoryCreatJavaProcess " + symbol + " " + StockArray.size());
                stockInputMap.put(symbol, StockArray);

            } // loop for stockNameArray
        }
    }

    public static ArrayList<AFstockInfo> getAllStaticStockHistoryServ(String symbol) {
        if (stockInputMap == null) {
            stockInputMap = nnAllStock_src.AllStockHistoryStaticCodeInit(stockInputMap);
        }
        ArrayList<AFstockInfo> stockInfoList = ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap);
        if (stockInfoList != null) {
            return stockInfoList;
        }
        return getAllStockHistory(symbol);
    }

    private static ArrayList<AFstockInfo> getAllStockHistory(String symbol) {
        if (stockInputMap_1 == null) {
            stockInputMap_1 = nnAllStock_1_src.AllStockHistoryStaticCodeInit(stockInputMap_1);
        }
        return ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap_1);

    }

    private static ArrayList<AFstockInfo> ProcessAllStockHistoryfromStaticCode(String symbol,
            HashMap<String, ArrayList> stockInMap) {

        ArrayList<AFstockInfo> inputlist = new ArrayList();

        String symbolL[] = ServiceAFweb.ignoreStock;
        for (int i = 0; i < symbolL.length; i++) {
            String ignoreSym = symbolL[i];
            if (ignoreSym.equals(symbol)) {
                return inputlist;
            }
        }

        if (stockInMap == null) {
            return inputlist;
        }

        if (symbol != "") {
            try {
                inputlist = stockInMap.get(symbol);
                if (inputlist == null) {
                    return null;
                }
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
                AFstockInfo[] arrayItem = new ObjectMapper().readValue(inputListRawSt, AFstockInfo[].class);
                List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
                inputlist = new ArrayList<AFstockInfo>(listItem);
                return inputlist;
            } catch (Exception ex) {
            }
        }
        return inputlist;
    }

/////////////////////////////////////////    
////////////////////////////////////////////////////        
    //https://ca.finance.yahoo.com/quote/T.TO/history?period1=1200441600&period2=1583539200&interval=1d&filter=history&frequency=1d
//    public static void updateAllStockFile(ServiceAFweb serviceAFWeb) {
//
//        //    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};
//        for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
//            String stockN = ServiceAFweb.primaryStock[i];
//            updateStockFile(serviceAFWeb, stockN);
//        }
//
//    }
    public static boolean updateStockFileServ(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        ArrayList inputArray = new ArrayList();
        String nnFileName = ServiceAFweb.FileLocalPath + NormalizeSymbol + ".csv";
        if (FileUtil.FileTest(nnFileName) == false) {
            logger.info("updateStockFile not found " + nnFileName);
            return false;
        }

        serviceAFWeb.removeStockInfoServ(NormalizeSymbol);

        ArrayList<AFstockInfo> StockArray = new ArrayList();
        boolean ret = FileUtil.FileReadTextArray(nnFileName, inputArray);
        if (ret == true) {
            int LineNum = 0;
            String inLine = "";
            for (int i = 0; i < inputArray.size(); i++) {
                inLine = (String) inputArray.get(i);
                LineNum++;
                //Date,Open,High,Low,Close,Adj Close,Volume
                if (inLine.indexOf("Date,Open") != -1) {
                    continue;
                }
                //1995-04-14,null,null,null,null,null,null
                if (inLine.indexOf("null,null,null") != -1) {
                    continue;
                }
                if (inLine.indexOf("Dividend") != -1) {
                    continue;
                }
                if (inLine.indexOf("Stock Split") != -1) {
                    break;
                }
                if (inLine.indexOf("-,-,-,-,-") != -1) {
                    continue;
                }
                AFstockInfo StockD = StockUtils.parseCSVLine(inLine, NormalizeSymbol);
                if (StockD == null) {
                    logger.info("updateStockFile Exception " + NormalizeSymbol + " " + inLine);
                    break;
                }

                StockArray.add(StockD);
            }
            logger.info("updateStockFile  " + NormalizeSymbol + " " + StockArray.size());
            if (StockArray.size() == 0) {
                return false;
            }
            ArrayList<AFstockInfo> StockSendArray = new ArrayList();
            int index = 0;
//            Collections.reverse(StockArray);
            for (int i = 0; i < StockArray.size(); i++) {

                StockSendArray.add(StockArray.get(i));
                index++;
                if (index > 99) {
                    index = 0;
//                    Collections.reverse(StockSendArray);
                    StockInfoTranObj stockInfoTran = new StockInfoTranObj();
                    stockInfoTran.setNormalizeName(NormalizeSymbol);
                    stockInfoTran.setStockInfoList(StockSendArray);
                    // require oldest date to earliest
                    // require oldest date to earliest
                    int retTran = serviceAFWeb.updateStockInfoTransactionServ(serviceAFWeb, stockInfoTran);
                    if (retTran == 0) {
                        return false;
                    }
                    StockSendArray.clear();
                }

            }
//            Collections.reverse(StockSendArray);
            StockInfoTranObj stockInfoTran = new StockInfoTranObj();
            stockInfoTran.setNormalizeName(NormalizeSymbol);
            stockInfoTran.setStockInfoList(StockSendArray);
//                logger.info("updateRealTimeStock send " + StockSendArray.size());

            // require oldest date to earliest
            // require oldest date to earliest
            serviceAFWeb.updateStockInfoTransactionServ(serviceAFWeb, stockInfoTran);
        }
        return true;
    }
//////////////////////////////////////////    

//////////////////////////////////////////
    // StockService
    StockInfoService stockInfoSrv = new StockInfoService();
//////////////////////////////////////////

    public ArrayList<AFstockInfo> getStockInfoServ(AFstockObj stock, int length, Calendar dateNow) {
        if (stock == null) {
            return null;
        }
        if (stock.getSubstatus() == ConstantKey.INITIAL) {
            return null;
        }
        return stockInfoSrv.getStockInfo(stock.getSymbol(), length, dateNow);
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> getStockInfo_workaroundServ(AFstockObj stock, int length, Calendar dateNow) {
        if (stock == null) {
            return null;
        }
        if (stock.getSubstatus() == ConstantKey.INITIAL) {
            return null;
        }
        return stockInfoSrv.getStockInfo_workaround(stock.getSymbol(), length, dateNow);
    }

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> getStockHistoricalServ(String symbol, int length) {
        if (true) {
            return stockInfoSrv.getStockHistorical(this, symbol, length);
        }
        return null;
    }

    public ArrayList<AFstockInfo> getStockHistoricalRangeServ(ServiceAFweb serviceAFWeb, String symbol, long start, long end) {
        if (true) {
            return stockInfoSrv.getStockHistoricalRange(serviceAFWeb, symbol, start, end);
        }
        return null;
    }

    public int updateStockInfoTransactionServ(ServiceAFweb serviceAFWeb, StockInfoTranObj stockInfoTran) {
        if (true) {
            return stockInfoSrv.updateStockInfoTransaction(serviceAFWeb, stockInfoTran);
        }
        return 0;
    }

    public int removeStockInfoServ(String symbol) {
        if (true) {
            return stockInfoSrv.removeStockInfo(this, symbol);
        }
        return 0;
    }

    public int updateStockAllSrv() {
        if (true) {
            return stockInfoSrv.updateAllStock(this);
        }
        return 0;
    }

    public int updateSQLStockInfoArrayList(ArrayList SQLTran) {
        if (true) {
            return stockInfoSrv.updateSQLStockInfoArrayList(this, SQLTran);
        }
        return 0;
    }

    public String getAllStockInfoDBSQLServ(String sql) {
        if (true) {
            return stockInfoSrv.getAllStockInfoDBSQL(this, sql);
        }
        return "";
    }
    ////////////////////////////////////////////
//////////////////////////////////////////
    // StockService
    StockService stockSrv = new StockService();
//////////////////////////////////////////

    public int updateSQLArrayListServ(ServiceAFweb serviceAFWeb, ArrayList SQLTran) {
        if (true) {
            return stockSrv.updateSQLArrayList(serviceAFWeb, SQLTran);
        }
        return 0;
    }

    public AFstockObj getStockBySymServ(String symbol) {
        if (true) {
            return stockSrv.getStockByName(this, symbol);
        }
        return null;
    }

    public AFstockObj getStockByStockIDServ(int stockID) {
        if (true) {
            return stockSrv.getStockBySockID(this, stockID);
        }
        return null;
    }

    public boolean checkStockServ(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        if (true) {
            return stockSrv.checkStock(serviceAFWeb, NormalizeSymbol);
        }
        return false;
    }

    public int addStockServ(String symbol) {
        if (true) {
            return stockSrv.addStock(this, symbol);
        }
        return 0;

    }

    public int disableStockServ(String symbol) {
        if (true) {
            return stockSrv.disableStock(this, symbol);
        }
        return 0;
    }

    public int deleteStockServ(AFstockObj stock) {
        if (true) {
            return stockSrv.deleteStock(this, stock);
        }
        return 0;
    }

    public int updateStockStatusDBServ(AFstockObj stock) {
        if (true) {
            return stockSrv.updateStockStatusDB(stock);
        }
        return 0;
    }

    public StringBuffer getInternetScreenPageServ(String url) {
        if (true) {
            return this.getStockImp().getInternetScreenPage(url);
        }
        return null;
    }

    public AFstockObj getRealTimeStockInternetServ(String NormalizeSymbol) {
        if (true) {
            return this.getStockImp().getRealTimeStockInternet(NormalizeSymbol);
        }
        return null;
    }

    public ArrayList<String> getAllOpenStockNameServ() {
        if (true) {
            return stockSrv.getAllOpenStockNameArray(this);
        }
        return null;
    }
//////////////////////////////////////////
//////////////////////////////////////////

    ////////////////////////
    public ArrayList getAllLock() {

        ArrayList result = null;

        result = getStockImp().getAllLock();
        return result;
    }

    public int setRenewLock(String symbol_acc, int type) {

        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        String name = symbol_acc;
        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(symbol_acc);
            name = symObj.getYahooSymbol();
        }
        return getStockImp().setRenewLock(name, type, lockDateValue);
    }

    public int setLockNameProcess(String name, int type, long lockdatel, String comment) {
        int resultLock = setLockName(name, type, lockdatel, comment);
        // DB will enusre the name in the lock is unique and s
        RandomDelayMilSec(200);
        AFLockObject lock = getLockName(name, type);
        if (lock != null) {
            if (lock.getLockdatel() == lockdatel) {
                return 1;
            }
        }

        return 0;
    }

    public AFLockObject getLockName(String symbol_acc, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        String name = symbol_acc;
        name = name.toUpperCase();
        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(symbol_acc);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return getStockImp().getLockName(name, type);
    }

    public int setLockName(String symbol_acc, int type, long lockdatel, String comment) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        String name = symbol_acc;
        name = name.toUpperCase();
        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(symbol_acc);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return getStockImp().setLockName(name, type, lockdatel, comment);
    }

    public int removeNameLock(String symbol_acc, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        String name = symbol_acc;
        name = name.toUpperCase();
        if (type == ConstantKey.STOCK_LOCKTYPE) {
            SymbolNameObj symObj = new SymbolNameObj(symbol_acc);
            name = symObj.getYahooSymbol();
        }
        name = name.toUpperCase();
        return getStockImp().removeLock(name, type);

    }

//////////////////
    public int systemCustStatusPaymentBalance(String customername,
            String statusSt, String paymenttSt, String balanceSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
            }
            ArrayList accountList = getAccountListServ(UserName, null);

            if (accountList == null) {
                return 0;
            }
            AccountObj accountObj = null;
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountTmp = (AccountObj) accountList.get(i);
                if (accountTmp.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                    accountObj = accountTmp;
                    break;
                }
            }
            if (accountObj == null) {
                return 0;
            }

            int status = -9999;
            if (statusSt != null) {
                if (!statusSt.equals("")) {
                    status = Integer.parseInt(statusSt);
                }
            }
            float payment = -9999;
            if (paymenttSt != null) {
                if (!paymenttSt.equals("")) {
                    payment = Float.parseFloat(paymenttSt);
                }
            }
            float balance = -9999;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);
                }
            }
            return getAccountImp().setCustStatusPaymentBalance(UserName, status, payment, balance);

        } catch (Exception e) {
        }
        return 0;
    }

    public WebStatus serverPing() {
        WebStatus msg = new WebStatus();

        msg.setResult(true);
        msg.setResponse("Server Ready");
        ArrayList serverlist = getServerList();
        if (serverlist == null) {
            msg.setResult(false);
            msg.setResponse("WebServer down");
            return msg;
        }
        if (serverlist.size() == 1) {
            ServerObj serverObj = (ServerObj) serverlist.get(0);
            if (serverObj.isLocalDBservice() == false) {
                msg.setResult(false);
                msg.setResponse("MasterDBServer down");
                return msg;
            }
        }
        for (int i = 0; i < serverlist.size(); i++) {
            ServerObj serverObj = (ServerObj) serverlist.get(i);
            if (serverObj.isSysMaintenance() == true) {
                msg.setResult(false);
                msg.setResponse("Server in Maintenance");
                break;
            }
        }
        return msg;
    }

    public String SystemRemoteUpdateMySQLList(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        String st = SQL;
        String[] sqlList = st.split("~");
        for (int i = 0; i < sqlList.length; i++) {
            String sqlCmd = sqlList[i];
            int ret = getStockImp().updateRemoteMYSQL(sqlCmd);
        }
        return ("" + sqlList.length);
    }

    public String SystemRemoteUpdateMySQL(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        return getStockImp().updateRemoteMYSQL(SQL) + "";
    }

    public String SystemRemoteGetMySQL(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        return getStockImp().getRemoteMYSQL(SQL);
    }

///////////////////////////
//    cannot autowire Could not autowire field:
    public static final int AllName = 200; //"1";
    public static final int AllSymbol = 201; //"1";
    public static final int AllId = 202; //"1";
    public static final int AllUserName = 203; //"1";

    public static final int AllLock = 2; //"2";
    public static final int AllStock = 3; //"3";
    public static final int AllStockInfo = 4; //"4";
    public static final int AllNeuralNet = 5; //"5";
    public static final int AllCustomer = 6; //"6";
    public static final int AllAccount = 7; //"7";
    public static final int AllAccountStock = 8; //"8";
    public static final int RemoteGetMySQL = 9; //"9";
    public static final int RemoteUpdateMySQL = 10; //"10";    
    public static final int RemoteUpdateMySQLList = 11; //"11";   
    public static final int AllTransationorder = 12; //"12";    
    public static final int AllPerformance = 13; //"13";  
    public static final int AllSQLquery = 14; //"14"; 
    public static final int AllNeuralNetData = 15; //"15";
    public static final int AllComm = 16; //"16";
    public static final int AllBilling = 17; //"17";    
    ////////
    public static final int UpdateSQLList = 101; //"101";
    public static final int updateAccountStockSignal = 102;// "102";
    public static final int updateStockInfoTransaction = 103; //"103";
    public static final int AllOpenAccountIDList = 104; //"104";
    public static final int AccountObjByAccountID = 105; //"105";
    public static final int AccountStockNameList = 106; //"106";
    public static final int UserNamebyAccountID = 107; //"107";
    public static final int UpdateTransactionOrder = 108; //"108";
//    public static final int ProcessTRHistory = 109; //"109";
    public static final int AccountStockListByAccountID = 110; //"110";
    public static final int AccountStockClrTranByAccountID = 111; //"111";    
    public static final int AllAccountStockNameListExceptionAdmin = 112; //"112";   
    public static final int AddTransactionOrder = 113; //"113"; 
    public static final int StockHistoricalRange = 114; //"114"; 
    public static final int AccountStockTransList = 115; //"115";     
    public static final int AccountStockPerfList = 116; //"116";     
    public static final int AccountStockIDByTRname = 117; //"117";   
    public static final int AccountStockListByAccountIDStockID = 118; //"118"; 
    public static final int RealTimeStockByStockID = 119; //"119"; 
    public static final int NeuralNetDataObj = 120; //"120";     
    public static final int NeuralNetDataObjStockid = 121; //"120";   

    public RequestObj SystemSQLRequest(RequestObj sqlObj) {

        boolean RemoteCallflag = ServiceAFweb.getServerObj().isLocalDBservice();
        if (RemoteCallflag == false) {
            return getServiceAFwebREST().getSQLRequest(sqlObj, CKey.SERVER_TIMMER_URL);
        }
        String st = "";
        String nameST = "";
        int ret;
        int accountId = 0;
        ArrayList<String> nameList = null;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
                case AllName:
                    nameList = getStockImp().getAllNameSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllSymbol:
                    nameList = getStockImp().getAllSymbolSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllId:
                    nameList = getAccountImp().getAllIdSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllUserName:
                    nameList = getAccountImp().getAllUserNameSQL(sqlObj.getReq());
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllLock:
                    nameST = getStockImp().getAllLockDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllStock:
                    nameST = getStockImp().getAllStockDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllStockInfo:
                    nameST = getAllStockInfoDBSQLServ(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllNeuralNet:
                    nameST = getStockImp().getAllNeuralNetDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllNeuralNetData:
                    nameST = getStockImp().getAllNeuralNetDataDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllCustomer:
                    nameST = getAccountImp().getAllCustomerDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllAccount:
                    nameST = getAccountImp().getAllAccountDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllAccountStock:
                    nameST = getAccountImp().getAllAccountStockDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case RemoteGetMySQL:  //RemoteGetMySQL = 9; //"9"; 
                    st = sqlObj.getReq();
                    nameST = getStockImp().getRemoteMYSQL(st);
                    sqlObj.setResp("" + nameST);

                    return sqlObj;

                case RemoteUpdateMySQL:  //RemoteUpdateMySQL = 10; //"10"; 
                    st = sqlObj.getReq();
                    ret = getStockImp().updateRemoteMYSQL(st);
                    sqlObj.setResp("" + ret);

                    return sqlObj;
                case RemoteUpdateMySQLList:  //RemoteUpdateMySQLList = 11; //"11"; 
                    st = sqlObj.getReq();
                    String[] sqlList = st.split("~");
                    for (int i = 0; i < sqlList.length; i++) {
                        String sqlCmd = sqlList[i];
                        ret = getStockImp().updateRemoteMYSQL(sqlCmd);
                    }
                    sqlObj.setResp("" + sqlList.length);

                    return sqlObj;

                case AllTransationorder: //AllTransationorder = 12; //"12";
                    nameST = getAccountImp().getAllTransationOrderDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllPerformance: //AllPerformance = 13; //"13";  
                    nameST = getAccountImp().getAllPerformanceDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case AllSQLquery: //AllSQLreq = 14; //"14";  
                    nameST = getAccountImp().getAllSQLquery(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case AllComm: //AllComm = 16; //"16";
                    nameST = getAccountImp().getAllCommDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case AllBilling: // AllBilling = 17; //"17";      
                    nameST = getAccountImp().getAllBillingDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
/////////////////////////
                case UpdateSQLList:  //UpdateSQLList = "101";
                    ArrayList<String> SQLArray = new ArrayList();

                    try {
                        SQLArray = new ObjectMapper().readValue(sqlObj.getReq(), ArrayList.class
                        );
                        int result = updateSQLArrayListServ(this, SQLArray);
                        sqlObj.setResp("" + result);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case updateAccountStockSignal:  //updateAccountStockSignal = "102";
                    try {
                        st = sqlObj.getReq();
                        TRObj stockTRObj = new ObjectMapper().readValue(st, TRObj.class
                        );
                        int result = getAccountImp().updateAccountStockSignal(stockTRObj.getTrlist());
                        sqlObj.setResp("" + result);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case updateStockInfoTransaction:  //updateStockInfoTransaction = "103";
                    try {
                        st = sqlObj.getReq();
                        StockInfoTranObj stockInfoTran = new ObjectMapper().readValue(st, StockInfoTranObj.class);

                        int result = updateStockInfoTransactionServ(this, stockInfoTran);
                        sqlObj.setResp("" + result);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AllOpenAccountIDList:  //AllOpenAccountIDList = "104";
                    ArrayList<String> nameId = getAccountImp().getAllOpenAccountID();
                    nameST = new ObjectMapper().writeValueAsString(nameId);
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case AccountObjByAccountID:  //AccountObjByAccountID = "105";
                    String accIdSt = sqlObj.getReq();
                    accountId = Integer.parseInt(accIdSt);
                    AccountObj accountObj = getAccountImp().getAccountObjByAccountID(accountId);
                    nameST = new ObjectMapper().writeValueAsString(accountObj);
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case AccountStockNameList:  //AccountStockNameList = "106";
                    accIdSt = sqlObj.getReq();
                    accountId = Integer.parseInt(accIdSt);
                    nameList = getAccountImp().getAccountStockNameList(accountId);
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case UserNamebyAccountID:  //UserNamebyAccountID = "107";
                    accIdSt = sqlObj.getReq();
                    accountId = Integer.parseInt(accIdSt);
                    nameList = getAccountImp().getUserNamebyAccountID(accountId);
                    nameST = new ObjectMapper().writeValueAsString(nameList);
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case UpdateTransactionOrder:  //UpdateTransactionOrder = "108";
                    try {
                        st = sqlObj.getReq();
                        ArrayList transSQL = new ObjectMapper().readValue(st, ArrayList.class
                        );
                        ret = this.getAccountImp().updateTransactionOrder(transSQL);
                        sqlObj.setResp("" + ret);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockListByAccountID:  //AccountStockListByAccountID = 110; //"110";  
                    try {
                        accIdSt = sqlObj.getReq();
                        accountId = Integer.parseInt(accIdSt);
                        String symbol = sqlObj.getReq1();
                        AFstockObj stock = getStockBySymServ(symbol);
                        int stockID = stock.getId();
                        ArrayList<TradingRuleObj> trList = getAccountImp().getAccountStockTRListByAccountID(accountId, stockID);
                        nameST = new ObjectMapper().writeValueAsString(trList);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockClrTranByAccountID:  //AccountStockClrTranByAccountID = 111; //"111";       
                    try {
                        st = sqlObj.getReq();
                        accountObj
                                = new ObjectMapper().readValue(st, AccountObj.class
                                );
                        String stockID = sqlObj.getReq1();
                        String trName = sqlObj.getReq2();

                        int stockId = Integer.parseInt(stockID);
                        ret = getAccountImp().clearAccountStockTranByAccountID(accountObj, stockId, trName.toUpperCase());
                        sqlObj.setResp("" + ret);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AllAccountStockNameListExceptionAdmin:  //AllAccountStockNameListExceptionAdmin = 112; //"112";        
                    try {
                        accIdSt = sqlObj.getReq();
                        accountId = Integer.parseInt(accIdSt);
                        nameList = getAccountImp().getAllAccountStockNameListExceptionAdmin(accountId);
                        nameST = new ObjectMapper().writeValueAsString(nameList);
                        sqlObj.setResp(nameST);
                        return sqlObj;
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AddTransactionOrder:  //AddTransactionOrder = 113; //"113";         
                    try {
                        st = sqlObj.getReq();
                        accountObj
                                = new ObjectMapper().readValue(st, AccountObj.class
                                );
                        st = sqlObj.getReq1();
                        AFstockObj stock = new ObjectMapper().readValue(st, AFstockObj.class
                        );
                        String trName = sqlObj.getReq2();
                        String tranSt = sqlObj.getReq3();
                        int tran = Integer.parseInt(tranSt);
                        Calendar tranDate = null;
                        String tranDateLSt = sqlObj.getReq4();
                        if (tranDateLSt != null) {
                            long tranDateL = Long.parseLong(tranDateLSt);
                            tranDate = TimeConvertion.getCurrentCalendar(tranDateL);
                        }
                        ret = getAccountImp().AddTransactionOrder(accountObj, stock, trName, tran, tranDate, true);
                        sqlObj.setResp("" + ret);
                        return sqlObj;
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case StockHistoricalRange: //StockHistoricalRange = 114; //"114";  
                    try {
                        String symbol = sqlObj.getReq();
                        String startSt = sqlObj.getReq1();
                        long start = Long.parseLong(startSt);
                        String endSt = sqlObj.getReq2();
                        long end = Long.parseLong(endSt);
                        ArrayList<AFstockInfo> StockArray = getStockInfoImp().getStockHistoricalRange(symbol, start, end);
                        nameST = new ObjectMapper().writeValueAsString(StockArray);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockTransList: //AccountStockTransList = 115; //"115";    
                    try {
                        String accountIDSt = sqlObj.getReq();
                        int accountID = Integer.parseInt(accountIDSt);
                        String stockIDSt = sqlObj.getReq1();
                        int stockID = Integer.parseInt(stockIDSt);
                        String trName = sqlObj.getReq2();
                        String lengthSt = sqlObj.getReq3();
                        int length = Integer.parseInt(lengthSt);

                        ArrayList<TransationOrderObj> retArray = getAccountImp().getAccountStockTransList(accountID, stockID, trName, length);

                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockPerfList: //AccountStockPerfList = 116; //"116";    
                    try {
                        String accountIDSt = sqlObj.getReq();
                        int accountID = Integer.parseInt(accountIDSt);
                        String stockIDSt = sqlObj.getReq1();
                        int stockID = Integer.parseInt(stockIDSt);
                        String trName = sqlObj.getReq2();
                        String lengthSt = sqlObj.getReq3();
                        int length = Integer.parseInt(lengthSt);

                        ArrayList<PerformanceObj> retArray = getAccountImp().getAccountStockPerfList(accountID, stockID, trName, length);

                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockIDByTRname:  //AccountStockIDByTRname = 117; //"117";          
                    try {

                        String accountID = sqlObj.getReq();
                        String stockID = sqlObj.getReq1();
                        String trName = sqlObj.getReq2();

                        accountId = Integer.parseInt(accountID);
                        int stockId = Integer.parseInt(stockID);
                        TradingRuleObj trObj = getAccountImp().getAccountStockIDByTRStockID(accountId, stockId, trName);
                        nameST = new ObjectMapper().writeValueAsString(trObj);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case AccountStockListByAccountIDStockID:  //AccountStockListByAccountIDStockID = 118; //"118";
                    try {
                        accIdSt = sqlObj.getReq();
                        accountId = Integer.parseInt(accIdSt);
                        String stockIdSt = sqlObj.getReq1();
                        int stockId = Integer.parseInt(stockIdSt);

                        ArrayList<TradingRuleObj> trList = getAccountImp().getAccountStockTRListByAccountID(accountId, stockId);
                        nameST = new ObjectMapper().writeValueAsString(trList);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case RealTimeStockByStockID:  //RealTimeStockByStockID = 119; //"119"; 
                    String stockIdSt = sqlObj.getReq();
                    int stockId = Integer.parseInt(stockIdSt);
                    AFstockObj stockObj = getStockByStockIDServ(stockId);
                    nameST = new ObjectMapper().writeValueAsString(stockObj);
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case NeuralNetDataObj: //NeuralNetDataObj = 120; //"120";      

                    try {
                        String BPname = sqlObj.getReq();
                        ArrayList<AFneuralNetData> retArray = getStockImp().getNeuralNetDataObj(BPname, 0);
                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case NeuralNetDataObjStockid: //NeuralNetDataObj = 121; //"121";        
                    try {
                        String BPname = sqlObj.getReq();

                        String stockID = sqlObj.getReq1();
                        int stockId121 = Integer.parseInt(stockID);

                        String updatedateSt = sqlObj.getReq2();
                        long updatedatel = Long.parseLong(updatedateSt);

                        ArrayList<AFneuralNetData> retArray = getStockImp().getNeuralNetDataObj(BPname, stockId121, updatedatel);
                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;

                /////
            }
        } catch (Exception ex) {
            logger.info("> SystemSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    public AccData getAccData(String accDataStr) {
        AccData refData = new AccData();
        try {
            if ((accDataStr != null) && (accDataStr.length() > 0)) {
                accDataStr = accDataStr.replaceAll("#", "\"");
                refData = new ObjectMapper().readValue(accDataStr, AccData.class);
                return refData;
            }
        } catch (Exception ex) {
        }
        return refData;
    }

    public String saveAccData(AccData accData) {

        String nameSt = "";
        try {
            nameSt = new ObjectMapper().writeValueAsString(accData);
            nameSt = nameSt.replaceAll("\"", "#");
        } catch (Exception ex) {
        }

        return nameSt;
    }

    /////helper function
    public ReferNameData getReferNameData(AFneuralNet nnObj0) {
        ReferNameData refData = new ReferNameData();
        String refName = nnObj0.getRefname();
        try {
            if ((refName != null) && (refName.length() > 0)) {
                refName = refName.replaceAll("#", "\"");
                refData = new ObjectMapper().readValue(refName, ReferNameData.class);
                return refData;
            }
        } catch (Exception ex) {
        }
        return refData;
    }

    public String SystemDownloadDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        BackkupkRestoreImp backupRestore = new BackkupkRestoreImp();
        retSatus = backupRestore.downloadDBData(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }

        return "SystemDownloadDBData " + retSatus;
    }

    public String SystemRestoreNNonlyDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        BackkupkRestoreImp backupRestore = new BackkupkRestoreImp();
        retSatus = backupRestore.restoreNNonlyDBData(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }

        return "SystemUploadDBData " + retSatus;
    }

    ///// Restore DB need the following
    ////  SystemStop
    ////  SystemCleanDBData
    ////  SystemUploadDBData
    ///// Restore DB need the following    
    public String SystemRestoreDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        BackkupkRestoreImp backupRestore = new BackkupkRestoreImp();
        retSatus = backupRestore.restoreDBData(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }

        return "SystemUploadDBData " + retSatus;
    }

    public String SystemStop() {
        boolean retSatus = true;
        serverObj.setSysMaintenance(true);

        return "sysMaintenance " + retSatus;
    }

    public static boolean SystemFilePut(String fileName, ArrayList msgWrite) {
        String fileN = ServiceAFweb.FileLocalPath + fileName;
        boolean ret = FileUtil.FileWriteTextArray(fileN, msgWrite);
        return ret;
    }

    public static boolean SystemFileRead(String fileName, ArrayList msgWrite) {
        String fileN = ServiceAFweb.FileLocalPath + fileName;
        boolean ret = FileUtil.FileReadTextArray(fileN, msgWrite);
        return ret;
    }

    public String SystemCleanNNonlyDBData() {
        boolean retSatus = false;
        serverObj.setSysMaintenance(true);
        retSatus = getStockImp().cleanNNonlyStockDB();
        return "" + retSatus;
    }

    public String SystemClearLock() {
        int retSatus = 0;
        retSatus = getStockImp().deleteAllLock();
        return "" + retSatus;
    }

    public String SystemRestDBData() {
        boolean retSatus = false;
        // make sure the system is stopped first
        retSatus = getStockInfoImp().restStockInfoDB();
        retSatus = getStockImp().restStockDB();
        return "" + retSatus;
    }

    public String SystemCleanDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        retSatus = getStockInfoImp().cleanStockInfoDB();
        retSatus = getStockImp().cleanStockDB();
        return "" + retSatus;
    }

    public String SystemClearNNData() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        AccountObj accountAdminObj = this.getAdminObjFromCache();
        int retStatus = NNProcessImp.ClearStockNNData(this, accountAdminObj);
        return "" + retStatus;
    }

    public String SystemClearNNtranAllAcc() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;

        retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(this, ConstantKey.TR_ACC, "");
        return "" + retSatus;
    }
//    

    public String SystemClearNNtran(String sym) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;

        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD, sym);
        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV, sym);
        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI, sym);
        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1, sym);
        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2, sym);
        retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3, sym);

        return "" + retSatus;
    }
//
//    public String SystemClearNNtran(int tr) {
//
//        TradingNNprocess NNProcessImp = new TradingNNprocess();
//        int retSatus = 0;
//        if (tr == ConstantKey.SIZE_TR) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD);
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV);
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI);
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1);
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2);
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3);
//        } else if (tr == ConstantKey.INT_TR_ACC) {
//            retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(this, ConstantKey.TR_ACC, "");
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_ACC);
//        } else if (tr == ConstantKey.INT_TR_MACD) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD);
//        } else if (tr == ConstantKey.INT_TR_MV) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV);
//        } else if (tr == ConstantKey.INT_TR_RSI) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI);
//        } else if (tr == ConstantKey.INT_TR_NN1) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1);
//        } else if (tr == ConstantKey.INT_TR_NN2) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2);
//        } else if (tr == ConstantKey.INT_TR_NN3) {
//            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3);
//        }
//
//        return "" + retSatus;
//    }

    public String SystemStart() {
        boolean retSatus = true;
        serverObj.setSysMaintenance(false);
        serverObj.setTimerInit(false);
        serverObj.setTimerQueueCnt(0);
        serverObj.setTimerCnt(0);
        return "sysMaintenance " + retSatus;
    }

//    public int testDBData() {
//        logger.info(">testDBData ");
//        int retSatus = getStockImp().testStockDB();
//        return retSatus;
//    }
    public int InitDBData() {
        logger.info(">InitDBData ");
        // 0 - new db, 1 - db already exist, -1 db error
        int retStatus = getStockImp().initStockDB();

        if (retStatus >= 0) {
            //// init StockInfo
            getStockImp().initStockInfoDB();

            logger.info(">InitDB Customer account ");
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(CKey.ADMIN_USERNAME);
            newCustomer.setPassword("passw0rd");
            newCustomer.setFirstname("ADM");
            newCustomer.setType(CustomerObj.INT_ADMIN_USER);
            //// result 1 = success, 2 = existed,  0 = fail
            getAccountImp().addCustomer(newCustomer, -1);

            newCustomer.setUsername(CKey.API_USERNAME);
            newCustomer.setPassword("eddy");
            newCustomer.setFirstname("APIUser");
            newCustomer.setType(CustomerObj.INT_API_USER);
            getAccountImp().addCustomer(newCustomer, -1);

            if (retStatus == 0) {

                newCustomer.setUsername(CKey.G_USERNAME);
                newCustomer.setPassword("guest");
                newCustomer.setFirstname("G");
                newCustomer.setType(CustomerObj.INT_GUEST_USER);
                getAccountImp().addCustomer(newCustomer, -1);

                newCustomer.setUsername(CKey.FUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setFirstname("FundMgr");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                getAccountImp().addCustomer(newCustomer, -1);
//                
                newCustomer.setUsername(CKey.INDEXFUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setFirstname("IndexMgr");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                getAccountImp().addCustomer(newCustomer, -1);

                AccountObj account = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
                if (account != null) {
                    int result = 0;
                    for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                        String stockN = ServiceAFweb.primaryStock[i];
                        AFstockObj stock = getStockBySymServ(stockN);
                        logger.info(">InitDBData add stock " + stock.getSymbol());
                        result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
                    }
                    AFstockObj stock = getStockBySymServ("T.TO");
                    result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
                }
            }

            newCustomer.setUsername(CKey.E_USERNAME);
            newCustomer.setPassword("pass");
            newCustomer.setFirstname("E");
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            getAccountImp().addCustomer(newCustomer, -1);
        }
        return retStatus;

    }

    public void InitStaticData() {
        logger.info(">InitDB InitStaticData ");
        getTRList().clear();
        TradingRuleObj tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_ACC);
        tr.setType(ConstantKey.INT_TR_ACC);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_MV);
        tr.setType(ConstantKey.INT_TR_MV);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_MACD);
        tr.setType(ConstantKey.INT_TR_MACD);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_RSI);
        tr.setType(ConstantKey.INT_TR_RSI);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_NN1);
        tr.setType(ConstantKey.INT_TR_NN1);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_NN2);
        tr.setType(ConstantKey.INT_TR_NN2);
        tr.setComment("");
        getTRList().add(tr);

        tr = new TradingRuleObj();
        tr.setTrname(ConstantKey.TR_NN3);
        tr.setType(ConstantKey.INT_TR_NN3);
        tr.setComment("");
        getTRList().add(tr);

//        tr = new TradingRuleObj();
//        tr.setTrname(ConstantKey.TR_NN91);
//        tr.setType(ConstantKey.INT_TR_NN91);
//        tr.setComment("");
//        getTRList().add(tr);
    }

    public void InitSystemFund(String portfolio) {
        if (portfolio.length() == 0) {
            return;
        }
        CustomerObj custObj = getAccountImp().getCustomerBySystem(CKey.FUND_MANAGER_USERNAME, null);
        ArrayList accountList = getAccountImp().getAccountListByCustomerObj(custObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                    getAccountImp().updateAccountPortfolio(accountObj.getAccountname(), portfolio);
                    break;
                }
            }
        }

    }

    public void InitSystemData() {
        logger.info(">InitDB InitSystemData for Stock and account ");

    }

    public static String getSQLLengh(String sql, int length) {
        //https://www.petefreitag.com/item/59.cfm
        //SELECT TOP 10 column FROM table - Microsoft SQL Server
        //SELECT column FROM table LIMIT 10 - PostgreSQL and MySQL
        //SELECT column FROM table WHERE ROWNUM <= 10 - Oracle
        if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
            if (length != 0) {
                if (length == 1) {
                    sql += " limit 1 ";
                } else {
                    sql += " limit " + length + " ";
                }
            }
        }

        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
            if (length != 0) {
                if (length == 1) {
                    sql = sql.replace("select ", "select top 1 ");
                } else {
                    sql = sql.replace("select ", "select top " + length + " ");
                }
            }
        }
        return sql;
    }

////////////////////////////////
    @Autowired
    public void setDataSource(DataSource dataSource) {
        //testing
        WebAppConfig webConfig = new WebAppConfig();
        dataSource = webConfig.dataSource();
        //testing        
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dataSource = dataSource;
    }

    /**
     * @return the stockImp
     */
    public StockImp getStockImp() {
//    private StockImp getStockImp() {
        return stockImp;
    }

    /**
     * @param stockImp the stockImp to set
     */
    public void setStockImp(StockImp stockImp) {
        this.stockImp = stockImp;
    }

    /**
     * @return the accountImp
     */
    public AccountImp getAccountImp() {
        return accountImp;
    }

    /**
     * @param accountImp the accountImp to set
     */
    public void setAccountImp(AccountImp accountImp) {
        this.accountImp = accountImp;
    }

    /**
     * @return the accountProcessImp
     */
    public AccountTranProcess getAccountProcessImp() {
        return accountProcessImp;
    }

    /**
     * @param accountProcessImp the accountProcessImp to set
     */
    public void setAccountProcessImp(AccountTranProcess accountProcessImp) {
        this.accountProcessImp = accountProcessImp;
    }

    /**
     * @return the serviceAFwebREST
     */
    public ServiceAFwebREST getServiceAFwebREST() {
        return serviceAFwebREST;
    }

    /**
     * @param serviceAFwebREST the serviceAFwebREST to set
     */
    public void setServiceAFwebREST(ServiceAFwebREST serviceAFwebREST) {
        this.serviceAFwebREST = serviceAFwebREST;
    }

    public static String compress(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        try {
            String inEncoding = "UTF-8";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes(inEncoding));
            gzip.close();
            return URLEncoder.encode(out.toString("ISO-8859-1"), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decompress(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        try {
            String outEncoding = "UTF-8";
            String decode = URLDecoder.decode(str, "UTF-8");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream(decode.getBytes("ISO-8859-1"));
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toString(outEncoding);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return the accounting
     */
    public AccountingProcess getAccounting() {
        return accounting;
    }

    /**
     * @param accounting the accounting to set
     */
    public void setAccounting(AccountingProcess accounting) {
        this.accounting = accounting;
    }

    /**
     * @return the stockInfoImp
     */
    public StockInfoImp getStockInfoImp() {
        return stockInfoImp;
    }

    /**
     * @param stockInfoImp the stockInfoImp to set
     */
    public void setStockInfoImp(StockInfoImp stockInfoImp) {
        this.stockInfoImp = stockInfoImp;
    }

}
