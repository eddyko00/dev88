/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.processemail.EmailProcess;
import com.afweb.nnsignal.TradingSignalProcess;
import com.afweb.nnsignal.TradingAPISignalProcess;
import com.afweb.accprocess.PUBSUBprocess;
import com.afweb.accprocess.FundMgrProcess;
import com.afweb.processbilling.BillingProcess;
import com.afweb.processaccounting.AccountingProcess;
import com.afweb.accprocess.AccountTranProcess;
import com.afweb.accprocess.AccountMaintProcess;
import com.afweb.nnprocess.*;
import com.afweb.model.*;
import com.afweb.account.*;
import com.afweb.chart.ChartService;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.signal.*;
import com.afweb.stock.*;
import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    private StockImp stockImp = new StockImp();
    private AccountImp accountImp = new AccountImp();
    private AccountTranProcess accountProcessImp = new AccountTranProcess();
    private ServiceAFwebREST serviceAFwebREST = new ServiceAFwebREST();
    private AccountingProcess accounting = new AccountingProcess();

    public static String PROXYURL = "";
    public static String URL_LOCALDB = "";
    public static String FileLocalPath = "";

    public static String UA_Str = "";
    public static String PA_Str = "";
    public static String UU_Str = "";

    private static ArrayList TRList = new ArrayList();

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
                ArrayList accountList = getAccountList(CKey.ADMIN_USERNAME, null);
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
                    String servIP = StockInternet.getServerIP();
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
        StockProcess stockProcess = new StockProcess();

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
                        SystemFundResetGlobal();
                    }
// use /cust/{username}/sys/processfundmgr                    }
// Fund Manger only do once a month   
                    boolean fundFlag = false;
                    if (fundFlag == true) {

//                        FundMgrProcess fundP = new FundMgrProcess();
//                        fundP.updateMutualFundAll();
//                        updateCustStatusSubStatus(CKey.FUND_MANAGER_USERNAME, ConstantKey.DISABLE + "", 0 + "");
//                        removeCustomer(CKey.FUND_MANAGER_USERNAME);
//                        CustomerObj newCustomer = new CustomerObj();
//                        newCustomer.setUsername(CKey.FUND_MANAGER_USERNAME);
//                        newCustomer.setPassword("passw0rd");
//                        newCustomer.setType(CustomerObj.INT_FUND_USER);
//                        getAccountImp().addCustomer(newCustomer);
                        SystemFundPocessAddRemove();
                    }

///////////////////////////////////////////////////////////////////////////////////
                    AFprocessDebug();
                    processNeuralNetTrain();

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
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        StockProcess stockProcess = new StockProcess();
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
            AFprocessNeuralNet();
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
            //
            TradingAPISignalProcess TRAPI = new TradingAPISignalProcess();
            TRAPI.ProcessAPISignalTrading(this);

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
    public static int cntNN = 0;

    public void AFprocessNeuralNet() {
        ServiceAFweb.lastfun = "AFprocessNeuralNet";

        if (processNeuralNetFlag == true) {
            cntNN++;
            TradingNNprocess NNProcessImp = new TradingNNprocess();
            NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
            NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

            nn1testflag = true;
            nn2testflag = true;

            if (cntNN == 1) {
                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
                return;
            } else if (cntNN == 2) {
                nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(this);
                return;
            } else if (cntNN == 3) {
                nn30trend.ProcessTrainNeuralNetNN30ByTrend(this);
                return;
            } else if (cntNN == 4) {
                NNProcessImp.ProcessReLearnInputNeuralNet(this);

//                String LockStock = "NN_LEARN"; // + "_" + trNN;
//                LockStock = LockStock.toUpperCase();
//
//                long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
//                long lockReturnStock = 1;
//
//                lockReturnStock = setLockNameProcess(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "processNewLearnNeuralNet");
//
//                if (lockReturnStock > 0) {
//                    try {
//                        NNProcessImp.ProcessReLearnInputNeuralNet(this);
//                    } catch (Exception ex) {
//
//                    }
//                }
//                removeNameLock(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
                cntNN = 0;
                return;

            }

            cntNN = 0;
        }
    }

    public boolean processNewLearnNeuralNet() {
        ServiceAFweb.lastfun = "processNewLearnNeuralNet";

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN30ProcessByTrend nn1trend = new NN30ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

        AccountObj accountAdminObj = getAdminObjFromCache();
        ArrayList stockNameArray = SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            logger.info("Start processNewLearnNeuralNet.....Stock Size " + stockNameArray.size());
            for (int i = 0; i < stockNameArray.size(); i++) {

                String symbol = (String) stockNameArray.get(i);
                AFstockObj stock = getStockRealTime(symbol);
                if (stock == null) {
                    continue;
                }
                if (stock.getAfstockInfo() == null) {
                    continue;
                }

                boolean chk1 = nn1ProcBySig.checkNN1Ready(this, symbol, true);
                boolean chk2 = nn2ProcBySig.checkNN2Ready(this, symbol, true);
                if ((chk1 == true) && (chk2 == true)) {
                    continue;
                }

                String LockStock = "NN_New_" + symbol; // + "_" + trNN;
                LockStock = LockStock.toUpperCase();

                long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                long lockReturnStock = 1;

                lockReturnStock = setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "processNewLearnNeuralNet");

                if (lockReturnStock == 0) {
                    continue;
                }
                if (lockReturnStock > 0) {
                    try {

                        if (chk1 == false) {
                            nn1trend.TrainNN30NeuralNetByTrend(this, symbol, ConstantKey.INT_TR_NN30, null);
                        }

                        if (chk1 == false) {
                            // process train symbol
                            nn1trend.TrainNN30NeuralNetByTrend(this, symbol, ConstantKey.INT_TR_NN30, null);

                            for (int j = 0; j < 4; j++) {
                                nn1ProcBySig.TrainNN1NeuralNetBySign(this, symbol, ConstantKey.INT_TR_NN1, null);
                                NNProcessImp.ReLearnInputNeuralNet(this, symbol, ConstantKey.INT_TR_NN1);
                            }
//                            logger.info("End processNewLearnNeuralNet.....NN1 " + symbol);
//                            return true;
                        } else if (chk2 == false) {
                            // process train symbol
                            for (int j = 0; j < 4; j++) {
                                nn2ProcBySig.TrainNN2NeuralNetBySign(this, symbol, ConstantKey.INT_TR_NN2, null);

                                NNProcessImp.ReLearnInputNeuralNet(this, symbol, ConstantKey.INT_TR_NN2);
                            }

//                            logger.info("End processNewLearnNeuralNet.....NN2 " + symbol);
//                            return true;
                        }
                    } catch (Exception ex) {

                    }
                    removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
                    return true;
                }

            }
        }
        logger.info("End processNewLearnNeuralNet.....");
        return false;
    }

    public void processNeuralNetTrain() {
        ServiceAFweb.lastfun = "processNeuralNetTrain";
        StockProcess stockProcess = new StockProcess();

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

        NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();

        TradingSignalProcess.forceToGenerateNewNN = false;
        if (initLocalRemoteNN == true) {
            while (true) {
                processInitLocalRemoteNN();

                logger.info("> Waiting 60 minutes ........");
                try {
                    Thread.sleep(30 * 1000 * 60);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (processNeuralNetFlag == true) {
            int num = 0;
            while (true) {
                if (num == 0) {
                    boolean ret = processNewLearnNeuralNet();
                    if (ret == false) {
                        num++;
                    }
                } else {
                    AFprocessNeuralNet();
                    num++;
                    if (num > 2) {
                        num = 0;
                    }
                    if (cntNN == 0) {
                        num = 0;
                    }
                }

                logger.info("> Waiting 30 sec cntNN " + cntNN + "........");
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        int k = 0;
        while (true) {
            k++;
            boolean exitflag = true;

////////////////////////////////////////////////////////////////////////////
            if (flagNNLearningSignal == true) {
                if (nn1testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 1)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN1);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 1 cycle " + k);
                    nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
                    logger.info("> ProcessTrainNeuralNet NN 1 end... cycle " + k);

                }
                if (nn2testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN2);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 2 cycle " + k);

                    nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(this);
                    logger.info("> ProcessTrainNeuralNet NN 2 end... cycle " + k);

                }
                if (nn3testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN3);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 3 cycle " + k);

                    nn3ProcBySig.ProcessTrainNN3NeuralNetBySign(this);
                    logger.info("> ProcessTrainNeuralNet NN 3 end... cycle " + k);

                }
                if (nn30testflag == true) {
                    exitflag = false;

                    if (((k % 5) == 0) || (k == 1)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN30);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 30 cycle " + k);
                    nn30trend.ProcessTrainNeuralNetNN30ByTrend(this);
                    logger.info("> ProcessTrainNeuralNet NN 30 end... cycle " + k);

                }
            }
////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////          
            if (flagNNReLearning == true) {
                exitflag = false;

                logger.info("> ProcessReLeanInput NN 1 cycle " + k);
                NNProcessImp.ProcessReLearnInputNeuralNet(this);
                logger.info("> ProcessReLeanInput end... cycle " + k);

            }

////////////////////////////////////////////////////////////////////////////            
            if (processRestinputflag == true) {
                if ((nn1testflag == true) && (nn2testflag == true)) {
                    // clear NN1 and NN2
                    logger.info("> processNNInputNeuralNet Clear NN DB..");
                    String nnName = ConstantKey.TR_NN1;
                    removeNeuralNetDataAllNNSymbolByTR(nnName);

                    nnName = ConstantKey.TR_NN30;
                    removeNeuralNetDataAllNNSymbolByTR(nnName);

                    nnName = ConstantKey.TR_NN2;
                    removeNeuralNetDataAllNNSymbolByTR(nnName);

                    nnName = ConstantKey.TR_NN3;
                    removeNeuralNetDataAllNNSymbolByTR(nnName);
                }

                if (nn1testflag == true) {
                    logger.info("> processNN1InputNeuralNet Rest input..");
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn1ProcBySig.processNN1InputNeuralNet(this);

                    nn30trend.processNN30InputNeuralNetTrend(this);

                }
                if (nn2testflag == true) {
                    logger.info("> processNN2InputNeuralNet Rest input..");
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn2ProcBySig.processNN2InputNeuralNet(this);

//                    nn2trend.processNN40InputNeuralNetTrend(this);
//                    nn2trend.processAllNN40StockInputNeuralNetTrend(this);
                    ///////////////////////////////
                }
                if (nn3testflag == true) {
                    logger.info("> processNNInputNeuralNet Clear NN DB..");
                    String nnName = ConstantKey.TR_NN3;
                    removeNeuralNetDataAllNNSymbolByTR(nnName);
                    logger.info("> processNN3InputNeuralNet Rest input..");

                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn3ProcBySig.processNN3InputNeuralNet(this);

//                    nn2trend.processNN40InputNeuralNetTrend(this);
//                    nn2trend.processAllNN40StockInputNeuralNetTrend(this);
                    ///////////////////////////////
                }

                logger.info("> processNN1InputNeuralNet Edn..");
                return;
            }
////////////////////////////////////////////////////////////////////////////
            if (processRestAllStockflag == true) {
                exitflag = true;
                ///////////////////////////////   
                String symbolL[] = ServiceAFweb.primaryStock;
                TradingNNprocess.CreateAllStockHistoryJava(this, symbolL, "nnAllStock", "NN_ST");

                /////////////////////
                ArrayList<String> APIStockNameList = new ArrayList();
                ArrayList<AccountObj> accountAPIObjL = this.getAccountList(CKey.API_USERNAME, null);
                if (accountAPIObjL != null) {
                    if (accountAPIObjL.size() > 0) {
                        AccountObj accountAPIObj = accountAPIObjL.get(0);
                        APIStockNameList = SystemAccountStockNameList(accountAPIObj.getId());
                    }
                }
                String symbolPriL[] = ServiceAFweb.primaryStock;
                if (APIStockNameList.size() > 0) {
                    for (int i = 0; i < symbolPriL.length; i++) {
                        String sym = symbolPriL[i];
                        if (APIStockNameList.contains(sym)) {
                            APIStockNameList.remove(sym);
                        }
                    }
                    APIStockNameList.remove("T.T");
                }
                String symbolLallSt[] = ServiceAFweb.allStock;
                if (APIStockNameList.size() > 0) {
                    for (int i = 0; i < symbolLallSt.length; i++) {
                        String sym = symbolLallSt[i];
                        if (APIStockNameList.contains(sym)) {
                            continue;
                        }
                        APIStockNameList.add(sym);
                    }
                }
                /*ArrayList to Array Conversion */
                String SymbolAllOther[] = APIStockNameList.toArray(new String[APIStockNameList.size()]);
                TradingNNprocess.CreateAllStockHistoryJava(this, SymbolAllOther, "nnAllStock_1", "NN_ST1");

                return;
            }
////////////////////////////////////////////////////////////////////////////

            if (processNNSignalAdmin == true) {
                exitflag = false;
                logger.info("> processNNSignalAdmin  cycle " + k);
                AccountTranProcess accountTranP = new AccountTranProcess();
                accountTranP.ProcessAdminSignalTrading(this);

                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
                updateStockAllSrv();
                logger.info("> processNNSignalAdmin end... cycle " + k);
            }
////////////////////////////////////////////////////////////////////////////
            if (exitflag == true) {
                break;
            }
            logger.info("> Waiting 30 sec........");
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

    public int removeNeuralNetDataAllNNSymbolByTR(String TRname) {
        logger.info("> removeNeuralNetDataAllNNSymbolByTR Reset input.." + TRname);

        String BPname = CKey.NN_version + "_" + TRname;
        getStockImp().deleteNeuralNetDataByBPname(BPname);

        return removeNeuralNetDataSymbolListByTR(TRname);
    }

    public int removeNeuralNetDataSymbolListByTR(String TRname) {
        logger.info("> removeNeuralNetDataSymbolListByTR Reset input.." + TRname);

        String BPname = CKey.NN_version + "_" + TRname;

        AccountObj accountObj = getAdminObjFromCache();
        ArrayList stockNameArray = SystemAccountStockNameList(accountObj.getId());

        if (stockNameArray != null) {
            logger.info("> removeNeuralNetDataSymbolListByTR Stock " + stockNameArray.size());

            for (int i = 0; i < stockNameArray.size(); i++) {
                String symbol = (String) stockNameArray.get(i);

                String BPnameSym = CKey.NN_version + "_" + TRname + "_" + symbol;
                getStockImp().deleteNeuralNetDataByBPname(BPnameSym);
            }
        }

        return 1;
    }

    boolean initLRnn = false;

    public void processInitLocalRemoteNN() {
        logger.info("> processInitLocalRemoteNN ");
        StockProcess stockProcess = new StockProcess();

        try {
            if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
                return;
            }
            if (initLRnn == false) {
                initLRnn = true;
                ArrayList<String> StockNameRemoteList = new ArrayList();

                AccountObj accountObj = this.getAdminObjFromCache();
                ArrayList<String> stockNameArray1 = serviceAFwebREST.getRESTAccountStockNameList(CKey.ADMIN_USERNAME,
                        accountObj.getId() + "", CKey.URL_PATH_HERO);
                logger.info("> remote dB stock:" + stockNameArray1.size());
                StockNameRemoteList.addAll(stockNameArray1);
                ArrayList<String> stockNameArray2 = serviceAFwebREST.getRESTAccountStockNameList(CKey.ADMIN_USERNAME,
                        accountObj.getId() + "", CKey.URL_PATH_OP);
                logger.info("> remote dB1 stock:" + stockNameArray2.size());
                StockNameRemoteList.addAll(stockNameArray2);

                ArrayList<AccountObj> accountAPIObjL = this.getAccountList(CKey.API_USERNAME, null);
                if (accountAPIObjL == null) {
                    return;
                }
                if (accountAPIObjL.size() == 0) {
                    return;
                }
////////////////////////////////
                // Add or remove stock
                AccountObj accountAPIObj = accountAPIObjL.get(0);
                ArrayList<String> APIStockNameList = SystemAccountStockNameList(accountAPIObj.getId());
                if (APIStockNameList == null) {
                    return;
                }
                logger.info("> API stock:" + APIStockNameList.size() + " remote dB stock:" + StockNameRemoteList.size());
                ArrayList addedList = new ArrayList();

                ArrayList removeList = new ArrayList();
                boolean result = AccountTranProcess.compareStockList(StockNameRemoteList, APIStockNameList, addedList, removeList);
                if (result == true) {
                    for (int i = 0; i < addedList.size(); i++) {
                        String symbol = (String) addedList.get(i);
                        if (symbol.equals("T_T")) {
                            continue;
                        }
                        int resultAdd = addAccountStockByCustAcc(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Add API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                    for (int i = 0; i < removeList.size(); i++) {
                        String symbol = (String) removeList.get(i);
                        int resultRemove = removeAccountStockByUserNameAccId(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Remove API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                }
////////////////////////////////////////////////                
                ////update all stock                
                getAccountProcessImp().ProcessAdminAddRemoveStock(this);
                getAccountProcessImp().ProcessAdminAddRemoveStock(this);

                AccountObj accountAdminObj = getAdminObjFromCache();
                ArrayList stockNameArray = SystemAccountStockNameList(accountAdminObj.getId());

                String printName = "";
                for (int i = 0; i < stockNameArray.size(); i++) {
                    printName += stockNameArray.get(i) + ",";
                }
                logger.info("processInitLocalRemoteNN " + printName);
////////////////////////////////////////////////                
                ////update remote Neural Net
                String URL = CKey.URL_PATH_HERO;
                String nnName = ConstantKey.TR_NN1;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN2;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN30;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
////////////////
////////////////
                URL = CKey.URL_PATH_OP;
                nnName = ConstantKey.TR_NN1;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN2;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN30;
                this.updateRESTNNWeight0(stockNameArray, nnName, URL);
////////////////////////////////////////////////  

                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                logger.info("> update  stock:" + stockNameArray.size());
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String symbol = (String) stockNameArray.get(i);
                    if (symbol.equals("T_T")) {
                        continue;
                    }
                    int re = stockProcess.updateAllStockProcess(this, symbol, false);
                    if ((i % 5) == 0) {
                        logger.info("> updated: " + i);
                    }
                }
                getAccountProcessImp().ProcessAdminAddRemoveStock(this);

            }

        } catch (Exception ex) {
            logger.info("> processInitLocalRemoteNN Exception " + ex.getMessage());
        }
    }

    private int updateRESTNNWeight0(ArrayList<String> APIStockNameList, String nnName, String URL) {
        if (APIStockNameList == null) {
            return 0;
        }

        logger.info("> updateRESTNNWeight0 " + nnName + " " + APIStockNameList.size() + " " + URL);

        String BPnameSym = CKey.NN_version + "_" + nnName;
        AFneuralNet nnObj1 = this.getNeuralNetObjWeight0(BPnameSym, 0);
        if (nnObj1 != null) {
            serviceAFwebREST.setNeuralNetObjWeight0(nnObj1, URL);
        }

        for (int i = 0; i < APIStockNameList.size(); i++) {
            String symbol = (String) APIStockNameList.get(i);
            if (symbol.equals("T_T")) {
                continue;
            }

            ///*****Make sure the DB name is HOU.TO.
            ///*****Make sure the DB name is RY.TO.
            ///*****Make sure the DB name is .
            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            try {
                nnObj1 = this.getNeuralNetObjWeight0(BPnameSym, 0);
                if (nnObj1 != null) {

                    int ret = serviceAFwebREST.setNeuralNetObjWeight0(nnObj1, URL);
                    ServiceAFweb.AFSleep1Sec(3);
                    if (ret != 1) {
                        logger.info("> updateRESTNNWeight0 " + BPnameSym + " ret=" + ret);
                    }
                } else {

                    logger.info("> updateRESTNNWeight0 not found " + BPnameSym);
                }
            } catch (Exception ex) {
                logger.info("> updateRESTNNWeight0 Exception " + ex.getMessage());
            }

        }
        return 1;
    }

    public void fileNNInputOutObjList(ArrayList<NNInputDataObj> inputList, String symbol, int stockId, String filename) {
        if (getEnv.checkLocalPC() == false) {
            return;
        }
        if (inputList != null) {
            //merge inputlistSym
            ArrayList writeArray = new ArrayList();
            String stTitle = "";
            int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use        
            for (int i = 0; i < inputList.size(); i++) {
                NNInputDataObj objData = inputList.get(i);
                NNInputOutObj obj = objData.getObj();

                String st = "\"" + stockId + "\",\"" + objData.getUpdatedatel() + "\",\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                        + "\",\"" + obj.getOutput1()
                        + "\",\"" + obj.getOutput2()
                        + "\",\"" + obj.getInput1()
                        + "\",\"" + obj.getInput2()
                        + "\",\"" + obj.getInput3()
                        + "\",\"" + obj.getInput4()
                        + "\",\"" + obj.getInput5()
                        + "\",\"" + obj.getInput6()
                        + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                        + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                        // + "\",\"" + obj.getInput11() + "\",\"" + obj.getInput12()
                        + "\"";

                if (i == 0) {
                    st += ",\"last\"";
                }

                if (i + 1 >= inputList.size()) {
                    st += ",\"first\"";
                }

                if (i == 0) {
                    stTitle = "\"" + "stockId" + "\",\"" + "Updatedatel" + "\",\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                            + "\",\"" + "output1"
                            + "\",\"" + "output2"
                            + "\",\"" + "macd TSig"
                            + "\",\"" + "LTerm"
                            + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                            + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                            + "\",\"" + symbol + "\"";

                }
                String stDispaly = st.replaceAll("\"", "");
                writeArray.add(stDispaly);
            }

            writeArray.add(stTitle.replaceAll("\"", ""));

            FileUtil.FileWriteTextArray(filename, writeArray);
        }
    }
///////////////////////////////
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

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();

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
        /// update stock split process
        ///////////////////////////////////////////////////////////////////////////////////
        boolean stocksplitflag = false;
        if (stocksplitflag == true) {
            /////////need manually enter the communication id
            /////////need manually enter the communication id

            int commid = 1; // 216; // 215;
            CommObj commObj = getAccountImp().getCommObjByID(commid);
            logger.info("stocksplitflag process commid " + commid);
            if (commObj != null) {
                CommData commData = getAccountImp().getCommDataObj(commObj);
                if (commData != null) {
                    String sym = commData.getSymbol();
                    boolean retBoolean = true;
                    AFstockObj stock = getStockImp().getRealTimeStock(sym, null);
//                    if (stock.getSubstatus() == ConstantKey.OPEN) {
//                        stock.setSubstatus(ConstantKey.STOCK_SPLIT);
//                        String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
//                        ArrayList sqlList = new ArrayList();
//                        sqlList.add(sockNameSQL);
//                        SystemUpdateSQLList(sqlList);
//                    }

                    if (stock.getSubstatus() != ConstantKey.STOCK_SPLIT) {
                        return;
                    }
                    // select 5 year and apply and download
                    // https://ca.finance.yahoo.com/quote/AAPL/history
                    // select 5 year and apply and download
                    //"T:/Netbean/db/";
                    String nnFileName = FileLocalPath + sym + ".csv";
                    if (FileUtil.FileTest(nnFileName) == false) {
                        logger.info("updateStockFile not found " + nnFileName);
                        return;
                    }

                    getStockImp().deleteStockInfoByStockId(stock);
                    // update file
                    retBoolean = StockInternet.updateStockFile(this, sym);

                    if (retBoolean == true) {
                        processStockSplit(commData.getSymbol(), commData.getSplit());
                    }
                }
            }

        }

        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////
    }

    public void debugtest() {
//        String symbol = "IWM";
//        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
//        int size1yearAll = 20 * 12 * 5 + (50 * 3);
//        ArrayList<AFstockInfo> StockArray = getStockHistorical(symbol, size1yearAll);

    }

//    public void updateErrorStockYahooParseError(String symbol) {
////        String symbol = "HOU.TO";
//        AFstockObj stock = this.getRealTimeStockImp(symbol);
//
//        stock.setStatus(ConstantKey.OPEN);
//        //send SQL update
//        String sockUpdateSQL = StockDB.SQLupdateStockStatus(stock);
//        ArrayList sqlList = new ArrayList();
//        sqlList.add(sockUpdateSQL);
//        SystemUpdateSQLList(sqlList);
//        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        TRprocessImp.updateRealTimeStock(this, stock);
//    }
//    public void forceRemoveCustTest(String login, String pass) {
//        CustomerObj custObj = getAccountImp().getCustomerPasswordForce(login, pass);
//        if (custObj == null) {
//            return;
//        }
//        if (custObj.getStatus() == ConstantKey.DISABLE) {
//            custObj.setStatus(ConstantKey.DISABLE);;
//        }
//        float bal = custObj.getBalance();
//        float payment = custObj.getPayment();
//        float outstand = bal - payment;
//        if (outstand >= 0) {  //No out standing payment 
//            custObj.setStatus(ConstantKey.DISABLE);
//        }
//        custObj.setStatus(ConstantKey.DISABLE);
//        updateCustStatusSubStatus(custObj.getUsername(), custObj.getStatus() + "", custObj.getSubstatus() + "");
//        removeCustomer(custObj.getUsername());
//    }
    public int processStockSplit(String symbol, float split) {
        logger.info(">processStockSplit");
        ArrayList accountIdList = SystemAllOpenAccountIDList();
        if (accountIdList == null) {
            return 0;
        }
        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);

        if (stock.getSubstatus() != ConstantKey.STOCK_SPLIT) {
            return 0;
        }

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        ArrayList<AFstockInfo> StockInfoArray = this.getStockHistorical(stock.getSymbol(), size1yearAll);
        if (StockInfoArray == null) {
            return 0;
        }
        if (StockInfoArray.size() < 100) {
            return 0;
        }
        for (int i = 0; i < accountIdList.size(); i++) {
            String accountIdSt = (String) accountIdList.get(i);
            int accountId = Integer.parseInt(accountIdSt);
            AccountObj accountObj = SystemAccountObjByAccountID(accountId);
            if (accountObj == null) {
                continue;
            }

            if (accountObj.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
                continue;
            }
            ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
            if (stockNameList == null) {
                continue;
            }

            boolean foundS = false;
            for (int j = 0; j < stockNameList.size(); j++) {
                String stockN = (String) stockNameList.get(j);
                if (stockN.equals(symbol)) {
                    foundS = true;
                    break;
                }
            }
            if (foundS == false) {
                continue;
            }

            ArrayList<TransationOrderObj> thList = getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), "TR_ACC", 0);
            if (thList == null) {
                continue;
            }
            ArrayList transSQL = new ArrayList();
            for (int k = 0; k < thList.size(); k++) {
                TransationOrderObj thObj = thList.get(k);
                float avgprice = thObj.getAvgprice();
                float share = thObj.getShare();

                long stockdatel = TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
                AFstockInfo stockInfoMatch = null;
                for (int j = 0; j < StockInfoArray.size(); j++) {
                    AFstockInfo stockInfo = StockInfoArray.get(j);
                    long stockInfodatel = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                    if (stockdatel == stockInfodatel) {
                        stockInfoMatch = stockInfo;
                        break;
                    }

                }
                if (stockInfoMatch == null) {
                    continue;
                }
                float oldClose = avgprice;
                float newClose = stockInfoMatch.getFclose();
                float tempSplit = 0;
                if (oldClose > newClose) {
                    tempSplit = oldClose / newClose;
                } else if (newClose > oldClose) {
                    tempSplit = newClose / oldClose;
                }
                if (tempSplit < CKey.SPLIT_VAL) {
                    // This transaction already done the spliting
                    if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                        stock.setSubstatus(ConstantKey.OPEN);
                        String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
                        ArrayList sqlList = new ArrayList();
                        sqlList.add(sockNameSQL);
                        SystemUpdateSQLList(sqlList);
                        logger.info("updateRealTimeStock " + accountObj.getAccountname() + " " + symbol + " Stock Split cleared");
                    }

                    continue;
                }

                if (split > 0) {
                    avgprice = avgprice / split;
                    share = share * split;
                }
                if (split < 0) {
                    split = -split;
                    avgprice = avgprice * split;
                    share = share / split;
                }
                thObj.setAvgprice(avgprice);
                thObj.setShare(share);
                String trSql = AccountDB.updateSplitTransactionSQL(thObj);
                transSQL.add(trSql);

            }
            logger.info("> processStockSplit " + accountObj.getAccountname() + " total update:" + transSQL.size());

            int ret = 0;
            if (transSQL.size() > 0) {
                ret = getAccountImp().updateTransactionOrder(transSQL);
            }
            if (ret == 1) {
//                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
//                    stock.setSubstatus(ConstantKey.OPEN);
//                    String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
//                    ArrayList sqlList = new ArrayList();
//                    sqlList.add(sockNameSQL);
//                    SystemUpdateSQLList(sqlList);
//                    logger.info("updateRealTimeStock " + accountObj.getAccountname() + " " + symbol + " Stock Split cleared");
//                }
//
                //udpate performance logic
                //udpate performance logic
            }

        }
        logger.info("> processStockSplit no update " + symbol);
        //clear stocksplit
        stock = getStockImp().getRealTimeStock(symbol, null);
        if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
            stock.setSubstatus(ConstantKey.OPEN);
            String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
            ArrayList sqlList = new ArrayList();
            sqlList.add(sockNameSQL);
            SystemUpdateSQLList(sqlList);
            logger.info("updateRealTimeStock " + symbol + " Stock Split cleared");
        }

        return 1;
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

    public static boolean checkCallRemoteMysql() {
        boolean ret = true;
        if (ServiceAFweb.getServerObj().isLocalDBservice() == true) {
            ret = false;
        }
        return ret;
    }

    ///////////////////////
    public int changeFundCustomer(String customername) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        CustomerObj custObj = getAccountImp().getCustomerBySystem(customername, null);

        if (custObj == null) {
            return 0;
        }

        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        custObj.setType(CustomerObj.INT_FUND_USER);
        custObj.setSubstatus(ConstantKey.INT_PP_PEMIUM);
        custObj.setPayment(0);

        int result = getAccountImp().systemUpdateCustAllStatus(custObj);
        if (result == 1) {
            String accountName = "acc-" + custObj.getId() + "-" + AccountObj.MUTUAL_FUND_ACCOUNT;
            result = getAccountImp().addAccountTypeSubStatus(custObj, accountName, AccountObj.INT_MUTUAL_FUND_ACCOUNT, ConstantKey.OPEN);
        }
        /// clear the last build to regenerate new bill
        AccountObj account = getAccountImp().getAccountByType(custObj.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = getAccountImp().getBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), 2);
        if (billingObjList != null) {
            if (billingObjList.size() > 0) {
                BillingObj billObj = billingObjList.get(0);
                this.getAccountImp().removeBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), billObj.getId());
            }
        }
        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust change to Fund Manager Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
        return result;

    }

    public int changeAPICustomer(String EmailUserName) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = getAccountImp().getCustomerBySystem(UserName, null);

        if (custObj == null) {
            return 0;
        }

        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        custObj.setType(CustomerObj.INT_API_USER);
        custObj.setSubstatus(ConstantKey.INT_PP_API);
        custObj.setPayment(0);

        int result = getAccountImp().systemUpdateCustAllStatus(custObj);
        /// clear the last build to regenerate new bill
        AccountObj account = getAccountImp().getAccountByType(custObj.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = getAccountImp().getBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), 2);
        if (billingObjList != null) {
            if (billingObjList.size() > 0) {
                BillingObj billObj = billingObjList.get(0);
                this.getAccountImp().removeBillingByCustomerAccountID(custObj.getUsername(), null, account.getId(), billObj.getId());
            }
        }

        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust change to API User Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
        return result;
    }

    //////////////////////////////////////
    // need ConstantKey.DISABLE status beofore remove customer
    public int removeCustomer(String EmailUserName) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = getAccountImp().getCustomerBySystem(UserName, null);

        if (custObj == null) {
            return 0;
        }
        if (custObj.getStatus() == ConstantKey.OPEN) {
            return 0;
        }
        ArrayList accountList = getAccountImp().getAccountListByCustomerObj(custObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
                if (stockNameList != null) {
                    for (int j = 0; j < stockNameList.size(); j++) {
                        String symbol = (String) stockNameList.get(j);
                        AFstockObj stock = getStockRealTime(symbol);
                        if (stock != null) {
                            getAccountImp().removeAccountStock(accountObj, stock.getId());
                        }
                    }
                }
                // remove billing
                getAccountImp().removeAccountBilling(accountObj);
                getAccountImp().removeAccountById(accountObj);
            }
        }

        return getAccountImp().removeCustomer(custObj);
    }

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj addCustomerPassword(String EmailUserName, String Password, String FirstName, String LastName, String planSt) {
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        loginObj.setWebMsg(webStatus);
        if (getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        boolean validEmail = NameObj.isEmailValid(EmailUserName);
        if (validEmail == true) {
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(UserName);
            newCustomer.setPassword(Password);
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            newCustomer.setEmail(EmailUserName);
            newCustomer.setFirstname(FirstName);
            newCustomer.setLastname(LastName);
            int plan = 0;
            plan = Integer.parseInt(planSt);
            if (plan == ConstantKey.INT_PP_BASIC) {
                ;
            } else if (plan == ConstantKey.INT_PP_STANDARD) {
                ;
            } else if (plan == ConstantKey.INT_PP_PEMIUM) {
                ;
            }

            // result 1 = success, 2 = existed,  0 = fail
            int result = getAccountImp().addCustomer(newCustomer, plan);
            if (result == 1) {
                CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
                if (custObj != null) {
                    // set pending for new customer
                    if (custObj.getStatus() == ConstantKey.OPEN) {
                        custObj.setStatus(ConstantKey.PENDING);
                        getAccountImp().updateCustStatusSubStatus(custObj, custObj.getStatus(), custObj.getSubstatus());
                    }
                }
            }
//
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = getAdminObjFromCache();
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long dateNowLong = dateNow.getTimeInMillis();
            java.sql.Date d = new java.sql.Date(dateNowLong);
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);
            String msg = ESTdate + " " + newCustomer.getUsername() + " Cust signup Result:" + result;
            CommMsgImp commMsg = new CommMsgImp();

            commMsg.AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//            
            webStatus.setResultID(result);
            return loginObj;
        }
        webStatus.setResultID(0);
        return loginObj;
    }

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj updateCustomerPassword(String EmailUserName, String AccountID, String Email, String Password, String FirstName, String LastName, String Plan) {

        CustomerObj custObj = null;
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        if (getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        custObj = getAccountImp().getCustomerPassword(UserName, null);
        if (custObj == null) {
            return loginObj;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return loginObj;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = new CustPort();
        if ((portfolio != null) && (portfolio.length() > 0)) {
            try {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            } catch (Exception ex) {
            }
        }

        if ((Email != null) && (Email.length() > 0)) {
            boolean validEmail = NameObj.isEmailValid(Email);
            if (validEmail == true) {
                custObj.setEmail(Email);
            } else {
                // error code 2 for invalid email address
                webStatus.setResultID(2);
                return loginObj;
            }
        }
        if ((Password != null) && (Password.length() > 0)) {
            // ignore "***"
            if (!Password.equals(CKey.MASK_PASS)) {
                custObj.setPassword(Password);
            }
            // error code 3 for invalid password
        }
        if ((FirstName != null) && (FirstName.length() > 0)) {
            custObj.setFirstname(FirstName);
        }
        if ((LastName != null) && (LastName.length() > 0)) {
            custObj.setLastname(LastName);
        }
        if ((Plan != null) && (Plan.length() > 0)) {
            try {

                int planid = Integer.parseInt(Plan);
                // update pending plan
                // -1 no change, 0, 10, 20
                if (planid == -1) {
                    // no change
                } else {
                    if (custObj.getType() == CustomerObj.INT_API_USER) {
                        ;
                    } else {
                        if ((planid == ConstantKey.INT_PP_BASIC) || (planid == ConstantKey.INT_PP_STANDARD)
                                || (planid == ConstantKey.INT_PP_PEMIUM) || (planid == ConstantKey.INT_PP_DELUXEX2)) {
                            custPortfilio.setnPlan(planid);
                        } else {
                            // error
                            return loginObj;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        int result = 0;
        try {
            int accountid = Integer.parseInt(AccountID);
            result = getAccountImp().updateCustomer(custObj, accountid);

            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            result = getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
        } catch (Exception ex) {
            logger.info("> updateCustomerPassword exception " + ex.getMessage());
        }

        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust update Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//                            
        webStatus.setResultID(result);
//        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }

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

    public LoginObj getCustomerEmailLogin(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(-1);

        custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj != null) {
            webStatus.setResultID(custObj.getStatus());
            if (custObj.getStatus() != ConstantKey.OPEN) {
                custObj = null;
            }
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;
    }

//    public LoginObj getCustomerLogin(String EmailUserName, String Password) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        CustomerObj custObj = null;
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        custObj = getAccountImp().getCustomerPassword(UserName, Password);
//
//        LoginObj loginObj = new LoginObj();
//        loginObj.setCustObj(custObj);
//        WebStatus webStatus = new WebStatus();
//        webStatus.setResultID(1);
//        if (custObj == null) {
//            webStatus.setResultID(0);
//        }
//        loginObj.setWebMsg(webStatus);
//        return loginObj;
//
//    }
    public LoginObj getCustomerAccLogin(String EmailUserName, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, null, AccountIDSt);
        if (accountObj != null) {
            custObj = getAccountImp().getCustomerPassword(UserName, null);
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(1);
        if (custObj == null) {
            webStatus.setResultID(0);
        }
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }

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

    //only on type=" + CustomerObj.INT_CLIENT_BASIC_USER;
    public ArrayList getExpiredCustomerList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        result = getAccountImp().getExpiredCustomerList(length);
        return result;
    }

    public ArrayList getCustomerNList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        result = getAccountImp().getCustomerNList(length);
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

    public ArrayList getCustomerList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = getAccountImp().getCustomerObjList(length);

        return result;
    }

    public ArrayList getFundAccounBestFundList(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return getAccountImp().getAccounBestFundList(UserName, Password);

    }

    public ArrayList getAccountList(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return getAccountImp().getAccountList(UserName, Password);

    }

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
        return getStockImp().getStockHistoricalRange(symbol, start, end);
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
        return getStockImp().getRealTimeStockByStockID(stockId, null);
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
        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
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

    public int SystemUpdateSQLList(ArrayList<String> SQLlist) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            RequestObj sqlObj = new RequestObj();
            sqlObj.setCmd(ServiceAFweb.UpdateSQLList + "");
            String st;
            try {
                st = new ObjectMapper().writeValueAsString(SQLlist);
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
                logger.info("> SystemUpdateSQLList exception " + ex.getMessage());
            }
            return 0;
        }
        return getStockImp().updateSQLArrayList(SQLlist);
    }

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

    public int SystemFundClearfundbalance(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            AccountObj accObj = getAccountImp().getAccountByCustomerAccountID(UserName, Password, accountid);
            if (accObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                int substatus = accObj.getSubstatus();
                float investment = 0;
                float balance = 0;
                float servicefee = 0;
                getAccountImp().updateAccountStatusByAccountID(accObj.getId(), substatus, investment, balance, servicefee);

                return 1;
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public int getFundAccountAddAccundFund(String EmailUserName, String Password, String AccountIDSt, String FundIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
            if (custObj == null) {
                return 0;
            }
            if (custObj.getStatus() != ConstantKey.OPEN) {
                return 0;
            }

            String portfolio = custObj.getPortfolio();
            CustPort custPortfilio = null;
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                } else {
                    custPortfilio = new CustPort();
                    String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                    getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
                }
            } catch (Exception ex) {
            }
            if (custPortfilio == null) {
                return 0;
            }
            ArrayList<String> featL = custPortfilio.getFeatL();
            if (featL == null) {
                return 0;
            }
            String fundFeat = "fund" + FundIDSt;
            for (int i = 0; i < featL.size(); i++) {
                String feat = featL.get(i);
                if (fundFeat.equals(feat)) {
                    return 0;
                }
            }

            int accFundId = Integer.parseInt(FundIDSt);
            AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);
            if (accFundObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                featL.add(fundFeat);
                String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);

                // update billing
                BillingProcess BP = new BillingProcess();
                BP.updateFundFeat(this, custObj, accFundObj);
                return 1;
            }

        } catch (Exception e) {
        }
        return 0;
    }

    public int getFundAccountRemoveAcocuntFund(String EmailUserName, String Password, String AccountIDSt, String FundIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return 0;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return 0;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return 0;
        }
        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return 0;
        }

        String delFundFeat = "delfund" + FundIDSt;

        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (delFundFeat.equals(feat)) {
//                alreadyDel = true;
                return 0;
            }
        }
        try {
            featL.add(delFundFeat);
            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
            return 1;
        } catch (Exception ex) {
        }
        return 0;

    }

    public ArrayList<AccountObj> getFundAccountByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList<AccountObj> accountObjList = new ArrayList();

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
            if (custObj == null) {
                return null;
            }
            if (custObj.getStatus() != ConstantKey.OPEN) {
                return null;
            }

            String portfolio = custObj.getPortfolio();
            CustPort custPortfilio = null;
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                }
            } catch (Exception ex) {
            }
            if (custPortfilio == null) {
                return null;
            }
            ArrayList<String> featL = custPortfilio.getFeatL();
            if (featL == null) {
                return null;
            }

            for (int i = 0; i < featL.size(); i++) {
                String feat = featL.get(i);
                try {
                    feat = feat.replace("fund", "");
                    int accFundId = Integer.parseInt(feat);
                    AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);
                    if (accFundObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                        accFundObj.setSubstatus(ConstantKey.OPEN);
                        String delFundFeat = "delfund" + accFundId;
                        for (int j = 0; j < featL.size(); j++) {
                            if (delFundFeat.equals(featL.get(j))) {
                                accFundObj.setSubstatus(ConstantKey.PENDING);
                            }
                        }
                        accountObjList.add(accFundObj);
                    }
                } catch (Exception e) {
                }
            }
            return accountObjList;
        } catch (Exception e) {
        }
        return null;

    }

    public AccountObj getAccountByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().getAccountByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return null;

    }

//    public ArrayList<BillingObj> getBillingByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            int accountid = Integer.parseInt(AccountIDSt);
//            ArrayList<BillingObj> billingObjList = getAccountImp().getBillingByCustomerAccountID(UserName, Password, accountid, length);
//            return billingObjList;
//        } catch (Exception e) {
//        }
//        return null;
//
//    }

//    public int removeBillingByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, String BillIDSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            int accountid = Integer.parseInt(AccountIDSt);
//            int billid = Integer.parseInt(BillIDSt);
//            int ret = getAccountImp().removeBillingByCustomerAccountID(UserName, Password, accountid, billid);
//            return ret;
//        } catch (Exception e) {
//        }
//        return 0;
//    }

//    public AccEntryObj getAccountingEntryByCustomerById(String EmailUserName, String Password, String idSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = getCustomerPassword(UserName, Password);
//            if (customer != null) {
//                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {
//                    int id = Integer.parseInt(idSt);
//
//                    AccEntryObj accEntry = getAccounting().getAccountingEntryById(this, id);
//                    return accEntry;
//                }
//            }
//        } catch (Exception e) {
//        }
//        return null;
//    }

//    public int removeAccountingEntryById(String EmailUserName, String Password, String idSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = getCustomerPassword(UserName, Password);
//            if (customer != null) {
//                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {
//                    int id = Integer.parseInt(idSt);
//                    return getAccounting().removeAccountingEntryById(this, id);
//
//                }
//            }
//        } catch (Exception e) {
//        }
//        return 0;
//    }

//    public AccReportObj getAccountingReportByCustomerByName(String EmailUserName, String Password, String name, int year, String namerptSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = getCustomerPassword(UserName, Password);
//            if (customer != null) {
//                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {
//
//                    if (name != null) {
//                        if (name.length() > 0) {
//                            AccReportObj accReport = getAccounting().getAccountReportYearByName(this, name, year);
//                            return accReport;
//                        }
//                    }
//                    String namerpt = "income";
//                    if (namerptSt != null) {
//                        if (namerptSt.length() > 0) {
//                            namerpt = namerptSt;
//                        }
//                    }
//                    AccReportObj accReport = null;
//                    if (namerpt.equals("balance")) {
//                        accReport = getAccounting().getAccountBalanceReportYear(this, year, namerptSt);
//                    } else if (namerpt.equals("deprecation")) {
//                        accReport = getAccounting().getAccountDeprecationReportYear(this, year, namerptSt);
//                    } else {
//                        accReport = getAccounting().getAccountReportYear(this, year, namerptSt);
//                    }
//                    return accReport;
//                }
//            }
//
//        } catch (Exception e) {
//        }
//        return null;
//
//    }

//    public ArrayList<CommObj> getCommEmaiByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            int accountid = Integer.parseInt(AccountIDSt);
//            return getAccountImp().getCommEmailByCustomerAccountID(UserName, Password, accountid, length);
//        } catch (Exception e) {
//        }
//        return null;
//
//    }

    public ArrayList<CommObj> getCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().getCommSignalSplitByCustomerAccountID(UserName, Password, accountid, length);
        } catch (Exception e) {
        }
        return null;

    }

    public int addCommByCustAccountID(String EmailUserName, String Password, String AccountIDSt, String data) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().addCommByCustomerAccountID(UserName, Password, accountid, data);
        } catch (Exception e) {
        }
        return 0;
    }

    public int removeAllCommBy1Month() {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long last1monthbefore = TimeConvertion.addMonths(dateNow.getTimeInMillis(), -1); // last 1 month before

        getAccountImp().removeCommByTimebefore(last1monthbefore, ConstantKey.INT_TYPE_COM_SIGNAL);
        return 1;

    }

    public int removeCommByID(String EmailUserName, String Password, String AccountIDSt, String IDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            int id = Integer.parseInt(IDSt);
            return getAccountImp().removeAccountCommByID(UserName, Password, accountid, id);
        } catch (Exception e) {
        }
        return 0;
    }

//    public int removeAllEmailByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            if (UserName.equals(CKey.ADMIN_USERNAME)) {
//                int accountid = Integer.parseInt(AccountIDSt);
//                return getAccountImp().removeAllCommByType(ConstantKey.INT_TYPE_COM_EMAIL);
//            }
//        } catch (Exception e) {
//        }
//        return 0;
//    }

    public int removeAllCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().removeCommByCustomerAccountIDType(UserName, Password, accountid, ConstantKey.INT_TYPE_COM_SIGNAL);
        } catch (Exception e) {
        }
        return 0;
    }

    public ArrayList<AFstockObj> getFundStockListByAccountID(String EmailUserName, String Password, String AccountIDSt, String FundIDSt, int lenght) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }

        String fundFeat = "fund" + FundIDSt;
        boolean featureExist = false;
        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (fundFeat.equals(feat)) {
                featureExist = true;
                break;
            }
        }
        if (featureExist == false) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);
        if (accFundObj == null) {
            return null;
        }
        if (accFundObj.getType() != AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
            return null;
        }

        ArrayList stockNameList = getAccountImp().getAccountStockNameList(accFundObj.getId());
        if (stockNameList != null) {
            if (lenght == 0) {
                lenght = stockNameList.size();
            } else if (lenght > stockNameList.size()) {
                lenght = stockNameList.size();
            }
            ArrayList returnStockList = new ArrayList();

            /// only TR ACC allowed
            String trname = ConstantKey.TR_ACC;

            for (int i = 0; i < lenght; i++) {
                String NormalizeSymbol = (String) stockNameList.get(i);
                AFstockObj stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
                if (stock != null) {

                    ArrayList<TradingRuleObj> trObjList = getAccountImp().getAccountStockTRListByAccountID(accFundObj.getId(), stock.getId());
                    if (trObjList != null) {
                        for (int j = 0; j < trObjList.size(); j++) {
                            TradingRuleObj trObj = trObjList.get(j);

                            if (trname.equals(ConstantKey.TR_ACC)) {
                                stock.setTRsignal(trObj.getTrsignal());

                                float total = getAccountStockRealTimeBalance(trObj);
                                stock.setPerform(total);

                                break;
                            }

                        }
                    }

                    returnStockList.add(stock);
                }
            }
            return returnStockList;
        }
        return null;
    }

    public ArrayList<AFstockObj> getStockNameListByAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
            return stockNameList;
        }
        return null;
    }

    public ArrayList<AFstockObj> getStockListByAccountIDTRname(String EmailUserName, String Password, String AccountIDSt, String trname, String filterSt, int lenght) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = null;

            ArrayList<String> filterArray = new ArrayList();
            if (filterSt != null) {
                if (filterSt.length() > 0) {
                    String[] filterList = filterSt.split(",");
                    int len = filterList.length;
                    if (len > 50) {
                        len = 50;
                    }
                    for (int i = 0; i < len; i++) {
                        String sym = filterList[i];
                        if (sym.length() > 0) {
                            filterArray.add(sym);
                        }
                    }
                }
            }

            if (filterArray.size() > 0) {
                stockNameList = filterArray;
            } else {
                stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());
            }

            if (stockNameList != null) {
                if (lenght == 0) {
                    lenght = stockNameList.size();
                } else if (lenght > stockNameList.size()) {
                    lenght = stockNameList.size();
                }

                ArrayList<AFstockObj> returnStockList = new ArrayList();
                for (int i = 0; i < lenght; i++) {
                    String NormalizeSymbol = (String) stockNameList.get(i);
                    AFstockObj stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
                    if (stock != null) {
                        stock.setTrname(trname);

                        ArrayList<TradingRuleObj> trObjList = getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stock.getId());
                        if (trObjList != null) {
                            if (trObjList.size() == 0) {
                                continue;
                            }
                            for (int j = 0; j < trObjList.size(); j++) {
                                TradingRuleObj trObj = trObjList.get(j);
                                if (trname.equals(trObj.getTrname())) {

                                    stock.setTRsignal(trObj.getTrsignal());
                                    float total = getAccountStockRealTimeBalance(trObj);
                                    stock.setPerform(total);
                                    break;
                                }

                            }
                        }

                        returnStockList.add(stock);
                    }
                }
                return returnStockList;
            }
        }
        return null;
    }

    // returen percent
    public float getAccountStockRealTimeBalance(TradingRuleObj trObj) {

        float totalPercent = 0;
        float deltaTotal = 0;
        float sharebalance = 0;
        try {
            if (trObj == null) {
                return 0;
            }

            AFstockObj stock = this.getStockRealTime(trObj.getSymbol());
//            int stockId = trObj.getStockid();            
//            AFstockObj stock = getStockImp().getRealTimeStockByStockID(stockId, null);
            if (stock == null) {
                return 0;
            }
            if (stock.getAfstockInfo() == null) {
                return 0;
            }

            float close = stock.getAfstockInfo().getFclose();
            if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                sharebalance = trObj.getLongamount();
                if (trObj.getLongshare() > 0) {
                    if (close > 0) {
                        deltaTotal = (close - (trObj.getLongamount() / trObj.getLongshare())) * trObj.getLongshare();
                    }
                }
            } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                sharebalance = trObj.getShortamount();
                if (trObj.getShortshare() > 0) {
                    if (close > 0) {
                        deltaTotal = ((trObj.getShortamount() / trObj.getShortshare()) - close) * trObj.getShortshare();
                    }
                }
            }
            totalPercent = trObj.getBalance() + sharebalance;
            totalPercent = totalPercent - trObj.getInvestment();

            if (stock.getSubstatus() == 0) {
                totalPercent = totalPercent + deltaTotal;
            }
            totalPercent = (totalPercent / CKey.TRADING_AMOUNT) * 100;
            // rounding 2 decimal round off
            totalPercent = (float) (Math.round(totalPercent * 100.0) / 100.0);
        } catch (Exception ex) {
            logger.info("> getAccountStockRealTimeBalance exception " + ex.getMessage());
        }
        return totalPercent;
    }

    public AFstockObj getStockByAccountIDStockID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);

        int stockID = 0;
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            ArrayList tradingRuleList = getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stockID);
            if (tradingRuleList != null) {
                return stock;
            }
        }
        return null;
    }

    public int addAccountStockTran(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int signal) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        int stockID = 0;
        if (accountObj != null) {
            try {
                stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return 0;
            }

            AccountTranImp accountTran = new AccountTranImp();
            int ret = accountTran.AddTransactionOrderWithComm(this, accountObj, stock, trName, signal);

            return ret;
        }
        return 0;
    }

    public int getAccountStockTRClrTranByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return 0;
            }
            return getAccountImp().clearAccountStockTranByAccountID(accountObj, stock.getId(), trName.toUpperCase());
        }
        return 0;
    }

    public ArrayList<TransationOrderObj> getAccountStockTRTranListByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }

            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                return getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), length);
            } else {
                AccountObj accountAdminObj = getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                return getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), length);
            }
        }
        return null;
    }

    public ArrayList<PerformanceObj> getFundAccountStockTRPerfList(String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);

        AFstockObj stock = null;
        if (accFundObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            ArrayList<PerformanceObj> perfList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                perfList = getAccountImp().getAccountStockPerfList(accFundObj.getId(), stock.getId(), trName, length);
            } else {
                AccountObj accountAdminObj = getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                perfList = getAccountImp().getAccountStockPerfList(accountAdminObj.getId(), stock.getId(), trName, length);
            }
            return perfList;
        }
        return null;
    }

    public ArrayList<TransationOrderObj> getFundAccountStockTRTranListByAccountID(String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);

        AFstockObj stock = null;
        if (accFundObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }

            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                return getAccountImp().getAccountStockTransList(accFundObj.getId(), stock.getId(), trName.toUpperCase(), length);
            }
        }
        return null;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfList(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            ArrayList<PerformanceObj> perfList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                perfList = getAccountImp().getAccountStockPerfList(accountObj.getId(), stock.getId(), trName, length);
            } else {
                AccountObj accountAdminObj = getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                perfList = getAccountImp().getAccountStockPerfList(accountAdminObj.getId(), stock.getId(), trName, length);
            }
            return perfList;
        }
        return null;
    }

    public int setAccountStockTRoption(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, String TROptType) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }

            if (stock == null) {
                return 0;
            }
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                TradingRuleObj tr = getAccountImp().getAccountStockIDByTRStockID(accountObj.getId(), stock.getId(), trName);
                if (tr == null) {
                    return 0;
                }
                int opt = 0;
                try {
                    if (TROptType != null) {
                        opt = Integer.parseInt(TROptType);
                    }
                } catch (NumberFormatException ex) {

                }

                if (opt < ConstantKey.SIZE_TR) {
                    tr.setLinktradingruleid(opt);

                    ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
                    UpdateTRList.add(tr);
                    return getAccountImp().updateAccountStockSignal(UpdateTRList);
                }
            }
        }
        return 0;

    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfHistory(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            ArrayList<TransationOrderObj> tranOrderList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                tranOrderList = getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistory(this, tranOrderList, stock, length, true);  // buyOnly = true
            }

            AccountObj accountAdminObj = getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TradingSignalProcess TRprocessImp = new TradingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistory(this, tranOrderList, stock, length, false);  // buyOnly = false
        }
        return null;
    }

    public ArrayList<PerformanceObj> getAccountStockTRPerfHistoryReinvest(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        if (accountObj != null) {
            try {
                int stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            ArrayList<TransationOrderObj> tranOrderList = null;
            if (trName.toUpperCase().equals(ConstantKey.TR_ACC)) {
                tranOrderList = getAccountImp().getAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistoryReinvest(this, tranOrderList, stock, length, true);  // buyOnly = true

            }
            AccountObj accountAdminObj = getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TradingSignalProcess TRprocessImp = new TradingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistoryReinvest(this, tranOrderList, stock, length, false); //buyOnly = false
        }
        return null;
    }

    public ArrayList<String> getAccountStockTRPerfHistoryDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {

        ArrayList<StockTRHistoryObj> trObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        ArrayList<String> writeTranArray = new ArrayList();
        ArrayList<String> displayArray = new ArrayList();
        int ret = getAccountStockTRListHistoryDisplayProcess(trObjList, writeTranArray, displayArray);

        ArrayList<PerformanceObj> perfObjList = getAccountStockTRPerfHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
        ArrayList<String> writePerfArray = new ArrayList();
        ArrayList<String> perfList = new ArrayList();
        ret = getAccountStockTRPerfHistoryDisplayProcess(perfObjList, writePerfArray, perfList);

        ArrayList<String> writeAllArray = new ArrayList();
        if (ret == 1) {
            if (getEnv.checkLocalPC() == true) {
                int j = 0;
                for (int i = 0; i < writeTranArray.size(); i++) {
                    if (i == 0) {
                        String st = writeTranArray.get(i);
                        st += "," + writePerfArray.get(j);
                        j++;
                        writeAllArray.add(st);
                        continue;
                    }
                    StockTRHistoryObj tran = trObjList.get(i - 1);

                    if (j >= perfObjList.size()) {
                        j = perfObjList.size() - 1;
                    }
                    PerformanceObj perf = perfObjList.get(j - 1);

                    if (tran.getUpdateDateD().equals(perf.getUpdateDateD())) {
                        String st = writeTranArray.get(i);
                        st += "," + writePerfArray.get(j);
                        writeAllArray.add(st);
                        j++;
                        if (j >= perfObjList.size()) {
                            j = perfObjList.size() - 1;
                        } else {
                            while (true) {
                                perf = perfObjList.get(j - 1);
                                if (tran.getUpdateDateD().equals(perf.getUpdateDateD())) {
                                    st = writeTranArray.get(i);
                                    st += "," + writePerfArray.get(j);
                                    j++;
                                    if (j >= perfObjList.size()) {
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                    String st = writeTranArray.get(i);
                    writeAllArray.add(st);
                }
                if (getEnv.checkLocalPC() == true) {
                    FileUtil.FileWriteTextArray(FileLocalDebugPath + stockidsymbol + "_" + trname + "_perf.csv", writeAllArray);
                }
            }

        }
        return perfList;

    }

    public int getAccountStockTRPerfHistoryDisplayProcess(ArrayList<PerformanceObj> perfObjList, ArrayList<String> writePerfArray, ArrayList<String> perfList) {

        if (perfObjList == null) {
            return 0;
        }

        for (int i = 0; i < perfObjList.size(); i++) {
            PerformanceObj trObj = perfObjList.get(i);
            String st = "";
            String stDispaly = "";
            if (writePerfArray.size() == 0) {
                st = "\"Investment" + "\",\"Balance" + "\",\"Grossprofit"
                        + "\",\"Netprofit" + "\",\"Numtrade" + "\",\"Rating"
                        + "\",\"Close" + "\",\"Trsignal" + "\",\"Numwin" + "\",\"Numloss"
                        + "\",\"Avgwin" + "\",\"Avgloss" + "\",\"Maxwin" + "\",\"Maxloss"
                        + "\",\"Maxholdtime" + "\",\"Minholdtime" + "\",\"updateDateD" + "\"";
                writePerfArray.add(st);
                stDispaly = st.replaceAll("\"", "");
                perfList.add(stDispaly);
            }
            st = "\"" + trObj.getInvestment() + "\",\"" + trObj.getBalance() + "\",\"" + trObj.getGrossprofit()
                    + "\",\"" + trObj.getNetprofit() + "\",\"" + trObj.getNumtrade() + "\",\"" + trObj.getRating()
                    + "\",\"" + trObj.getPerformData().getClose() + "\",\"" + trObj.getPerformData().getTrsignal()
                    + "\",\"" + trObj.getPerformData().getNumwin() + "\",\"" + trObj.getPerformData().getNumloss()
                    + "\",\"" + trObj.getPerformData().getAvgwin() + "\",\"" + trObj.getPerformData().getAvgloss()
                    + "\",\"" + trObj.getPerformData().getMaxwin() + "\",\"" + trObj.getPerformData().getMaxloss()
                    + "\",\"" + trObj.getPerformData().getMaxholdtime() + "\",\"" + trObj.getPerformData().getMinholdtime()
                    + "\",\"" + trObj.getUpdateDateD() + "\"";

            writePerfArray.add(st);
            stDispaly = st.replaceAll("\"", "");
            perfList.add(stDispaly);

        }

        return 1;
    }

    public ArrayList<TradingRuleObj> getAccountStockTRListByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        int stockID = 0;
        if (accountObj != null) {
            try {
                stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            return getAccountImp().getAccountStockTRListByAccountID(accountObj.getId(), stockID);
        }
        return null;
    }

    public TradingRuleObj getFundAccountStockTRByTRname(String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol, String trname) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }

        String fundFeat = "fund" + FundIDSt;
        boolean featureExist = false;
        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (fundFeat.equals(feat)) {
                featureExist = true;
                break;
            }
        }
        if (featureExist == false) {
            return null;
        }

        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);
        if (accFundObj == null) {
            return null;
        }
        if (accFundObj.getType() != AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
            return null;
        }

        AFstockObj stock = null;
        int stockID = 0;

        try {
            stockID = Integer.parseInt(stockidsymbol);
            stock = getStockImp().getRealTimeStockByStockID(stockID, null);
        } catch (NumberFormatException e) {
            SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
            String NormalizeSymbol = symObj.getYahooSymbol();
            stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        }
        if (stock == null) {
            return null;
        }
        stockID = stock.getId();
        return getAccountImp().getAccountStockIDByTRStockID(accFundObj.getId(), stockID, trname);

    }

    public TradingRuleObj getAccountStockTRByTRname(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        trname = trname.toUpperCase();
        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        AFstockObj stock = null;
        int stockID = 0;
        if (accountObj != null) {
            try {
                stockID = Integer.parseInt(stockidsymbol);
                stock = getStockImp().getRealTimeStockByStockID(stockID, null);
            } catch (NumberFormatException e) {
                SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
                String NormalizeSymbol = symObj.getYahooSymbol();
                stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            }
            if (stock == null) {
                return null;
            }
            stockID = stock.getId();
            return getAccountImp().getAccountStockIDByTRStockID(accountObj.getId(), stockID, trname);
        }
        return null;
    }

    public String getAccountStockTRListHistoryChartProcess(ArrayList<StockTRHistoryObj> thObjListMain, String stockidsymbol, String trname, String pathSt) {
        try {
            if (thObjListMain == null) {
                return "";
            }

            if ((pathSt == null) || (pathSt.length() == 0)) {
                pathSt = "t:/Netbean/debug";
            }
            if (getEnv.checkLocalPC() == true) {
                pathSt = "t:/Netbean/debug";
            }
            String filepath = pathSt + "/" + stockidsymbol + "_" + trname + "_" + initTrainNeuralNetNumber;

            List<Date> xDate = new ArrayList<Date>();
            List<Double> yD = new ArrayList<Double>();

            ArrayList<StockTRHistoryObj> thObjList = new ArrayList();
            thObjList.addAll(thObjListMain);

            ArrayList closeList = new ArrayList<Float>();
            for (int i = 0; i < thObjList.size(); i++) {
                StockTRHistoryObj thObj = thObjList.get(i);
                float close = thObj.getClose();
                closeList.add(close);
            }
            NNormalObj normal = new NNormalObj();
            normal.initHighLow(closeList);

            List<Date> buyDate = new ArrayList<Date>();
            List<Double> buyD = new ArrayList<Double>();
            List<Date> sellDate = new ArrayList<Date>();
            List<Double> sellD = new ArrayList<Double>();

            StockTRHistoryObj prevThObj = null;

            for (int i = 0; i < thObjList.size(); i++) {
                StockTRHistoryObj thObj = thObjList.get(i);
                if (i == 0) {
                    prevThObj = thObj;
                }
                Date da = new Date(thObj.getUpdateDatel());
                xDate.add(da);
                float close = thObj.getClose();
                double norClose = normal.getNormalizeValue(close);
                yD.add(norClose);

                int signal = thObj.getTrsignal();
                if (signal != prevThObj.getTrsignal()) {

                    if (signal == ConstantKey.S_BUY) {
                        buyD.add(norClose);
                        buyDate.add(da);
                    }
                    if (signal == ConstantKey.S_SELL) {
                        sellD.add(norClose);
                        sellDate.add(da);
                    }
                }
                prevThObj = thObj;
            }
            ChartService chart = new ChartService();
            chart.saveChartToFile(stockidsymbol + "_" + trname, filepath,
                    xDate, yD, buyDate, buyD, sellDate, sellD);
            return "Save in " + filepath;
        } catch (Exception ex) {
            logger.info("> getAccountStockTRListHistoryChartProcess exception" + ex.getMessage());
        }
        return "Save failed";

    }
//////////

    public byte[] getFundAccountStockTRLIstCurrentChartDisplay(String EmailUserName, String Password, String AccountIDSt, String FundIDSt, String stockidsymbol,
            String trname, String monthSt) {
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj == null) {
            return null;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return null;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return null;
        }

        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return null;
        }
        int accFundId = Integer.parseInt(FundIDSt);
        AccountObj accFundObj = getAccountImp().getAccountObjByAccountID(accFundId);
        AFstockObj stock = null;
        try {
            int stockID = Integer.parseInt(stockidsymbol);
            stock = getStockImp().getRealTimeStockByStockID(stockID, null);
        } catch (NumberFormatException e) {
            SymbolNameObj symObj = new SymbolNameObj(stockidsymbol);
            String NormalizeSymbol = symObj.getYahooSymbol();
            stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        }
        if (stock == null) {
            return null;
        }

        int month = 6;
        if (monthSt != null) {
            try {
                month = Integer.parseInt(monthSt);
                if (month > 48) {
                    month = 48;
                }
            } catch (Exception ex) {
            }
        }

        ArrayList<TransationOrderObj> thList = getAccountImp().getAccountStockTransList(accFundObj.getId(), stock.getId(), trname.toUpperCase(), 0);

        if (thList == null) {
            thList = new ArrayList();
        }

        int sizeLen = 20 * 10;

        if (month > 0) {
            sizeLen = 20 * month;
        }

        // recent date first
        ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), sizeLen);
        if (StockArray == null) {
            return null;
        }
        if (StockArray.size() < 10) {
            return null;
        }
        // recent date last
        Collections.reverse(StockArray);
        Collections.reverse(thList);

        ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

        xDate = new ArrayList<Date>();
        yD = new ArrayList<Double>();
        buyDate = new ArrayList<Date>();
        buyD = new ArrayList<Double>();
        sellDate = new ArrayList<Date>();
        sellD = new ArrayList<Double>();

        StockArrayTmp = new ArrayList();
        for (int i = 0; i < StockArray.size(); i++) {
            StockArrayTmp.add(StockArray.get(i));
        }
        int numBS = this.checkCurrentChartDisplay(StockArrayTmp, xDate, yD, buyDate, buyD, sellDate, sellD, thList);

        ChartService chart = new ChartService();
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;
    }

    public byte[] getAccountStockTRLIstCurrentChartDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol,
            String trname, String monthSt) {

        int month = 6;
        if (monthSt != null) {
            try {
                month = Integer.parseInt(monthSt);
                if (month > 48) {
                    month = 48;
                }
            } catch (Exception ex) {
            }
        }
        ArrayList<TransationOrderObj> thList = this.getAccountStockTRTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);

        if (thList == null) {
            thList = new ArrayList();
        }
        String symbol = stockidsymbol;
        AFstockObj stock = this.getStockRealTime(symbol);
        if (stock == null) {
            return null;
        }
        int sizeLen = 20 * 10;

        if (month > 0) {
            sizeLen = 20 * month;
        }

        // recent date first
        ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), sizeLen);
        if (StockArray == null) {
            return null;
        }
        if (StockArray.size() < 10) {
            return null;
        }
        // recent date last
        Collections.reverse(StockArray);
        Collections.reverse(thList);

        ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

        xDate = new ArrayList<Date>();
        yD = new ArrayList<Double>();
        buyDate = new ArrayList<Date>();
        buyD = new ArrayList<Double>();
        sellDate = new ArrayList<Date>();
        sellD = new ArrayList<Double>();

        StockArrayTmp = new ArrayList();
        for (int i = 0; i < StockArray.size(); i++) {
            StockArrayTmp.add(StockArray.get(i));
        }
        int numBS = this.checkCurrentChartDisplay(StockArrayTmp, xDate, yD, buyDate, buyD, sellDate, sellD, thList);

        ChartService chart = new ChartService();
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;

    }

    private int checkCurrentChartDisplay(ArrayList<AFstockInfo> StockArray, List<Date> xDate, List<Double> yD,
            List<Date> buyDate, List<Double> buyD, List<Date> sellDate, List<Double> sellD,
            ArrayList<TransationOrderObj> thList) {

        for (int j = 0; j < StockArray.size(); j++) {
            AFstockInfo stockinfo = StockArray.get(j);

            Date da = new Date(stockinfo.getEntrydatel());
            xDate.add(da);
            float close = stockinfo.getFclose();
            double norClose = close;
            yD.add(norClose);

        }

        AFstockInfo stockinfo = StockArray.get(0);
        long stockdatel = stockinfo.getEntrydatel();
        for (int i = 0; i < thList.size(); i++) {
            TransationOrderObj thObj = thList.get(i);

            long THdatel = thObj.getEntrydatel(); //TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
            if (stockdatel > THdatel) {
                continue;
            }
            TransationOrderObj thObjNext = thObj;
            if ((thObj.getTrsignal() == ConstantKey.S_BUY) || (thObj.getTrsignal() == ConstantKey.S_SELL)) {
                ;
            } else {
                if (i + 1 < thList.size()) {
                    thObjNext = thList.get(i + 1);
                }
                long THdatelNext = TimeConvertion.endOfDayInMillis(thObjNext.getEntrydatel());
                if (THdatel == THdatelNext) {
                    thObj = thObjNext;
                }
            }
            int signal = thObj.getTrsignal();

            float close = thObj.getAvgprice();
            double norClose = close;
            Date da = new Date(thObj.getEntrydatel());
            if (signal == ConstantKey.S_BUY) {
                buyD.add(norClose);
                buyDate.add(da);
            } else if (signal == ConstantKey.S_SELL) {
                sellD.add(norClose);
                sellDate.add(da);
            } else {
//                sellD.add(norClose);
//                sellDate.add(da);
//                buyD.add(norClose);
//                buyDate.add(da);
            }
        }

//        /// add this one to show the trend line
        Date da = new Date(stockinfo.getEntrydatel());
        xDate.add(da);
        float close = stockinfo.getFclose();
        double norClose = close;
        yD.add(norClose);

        return buyD.size() + sellD.size();
    }

    public String getAccountStockTRLIstCurrentChartFile(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        try {
            ArrayList<TransationOrderObj> thList = this.getAccountStockTRTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
            if (thList == null) {
                return null;
            }
            Collections.reverse(thList);

            trname = trname.toUpperCase();
            String symbol = stockidsymbol;
            AFstockObj stock = this.getStockRealTime(symbol);

            int size1year = 20 * 10;
            ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), size1year);
            if (StockArray == null) {
                return null;
            }
            Collections.reverse(StockArray);

            List<Date> xDate = new ArrayList<Date>();
            List<Double> yD = new ArrayList<Double>();

            ArrayList<Float> closeList = new ArrayList<Float>();
            for (int i = 0; i < StockArray.size(); i++) {
                AFstockInfo stockinfo = StockArray.get(i);
                float close = stockinfo.getFclose();
                closeList.add(close);
            }
            NNormalObj normal = new NNormalObj();
            normal.initHighLow(closeList);

            List<Date> buyDate = new ArrayList<Date>();
            List<Double> buyD = new ArrayList<Double>();
            List<Date> sellDate = new ArrayList<Date>();
            List<Double> sellD = new ArrayList<Double>();

            for (int j = 0; j < StockArray.size(); j++) {
                AFstockInfo stockinfo = StockArray.get(j);

                Date da = new Date(stockinfo.getEntrydatel());
                xDate.add(da);
                long stockdatel = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
                float close = stockinfo.getFclose();
                double norClose = normal.getNormalizeValue(close);
                yD.add(norClose);
                for (int i = 0; i < thList.size(); i++) {
                    TransationOrderObj thObj = thList.get(i);
                    long THdatel = TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
                    if (stockdatel != THdatel) {
                        continue;
                    }

                    TransationOrderObj thObjNext = thObj;
                    if ((thObj.getTrsignal() == ConstantKey.S_BUY) || (thObj.getTrsignal() == ConstantKey.S_SELL)) {
                        ;
                    } else {
                        if (i + 1 < thList.size()) {
                            thObjNext = thList.get(i + 1);
                        }
                        long THdatelNext = TimeConvertion.endOfDayInMillis(thObjNext.getEntrydatel());
                        if (THdatel == THdatelNext) {
                            thObj = thObjNext;
                        }
                    }

                    int signal = thObj.getTrsignal();
                    if (signal == ConstantKey.S_BUY) {
                        buyD.add(norClose);
                        buyDate.add(da);
                    } else if (signal == ConstantKey.S_SELL) {
                        sellD.add(norClose);
                        sellDate.add(da);
                    } else {
                        sellD.add(norClose);
                        sellDate.add(da);
                        buyD.add(norClose);
                        buyDate.add(da);
                    }
                    break;
                }
            }
            if ((pathSt == null) || (pathSt.length() == 0)) {
                pathSt = "t:/Netbean/debug";
            }
            if (getEnv.checkLocalPC() == true) {
                pathSt = "t:/Netbean/debug";
            }
            String filepath = pathSt + "/" + stockidsymbol + "_" + trname;

            ChartService chart = new ChartService();
            chart.saveChartToFile(stockidsymbol + "_" + trname, filepath,
                    xDate, yD, buyDate, buyD, sellDate, sellD);
            return "Save in " + filepath;
        } catch (Exception ex) {
            logger.info("> getAccountStockTRLIstCurrentChartFile exception" + ex.getMessage());
        }
        return "Save failed";
    }

//    public byte[] getAccountStockTRListHistoryChartDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
//        ArrayList<StockTRHistoryObj> thObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
//
//        if (thObjList == null) {
//            return null;
//        }
//
//        List<Date> xDate = new ArrayList<Date>();
//        List<Double> yD = new ArrayList<Double>();
//
//        ArrayList closeList = new ArrayList<Float>();
//        for (int i = 0; i < thObjList.size(); i++) {
//            StockTRHistoryObj thObj = thObjList.get(i);
//            float close = thObj.getClose();
//            closeList.add(close);
//        }
//        NNormalObj normal = new NNormalObj();
//        normal.initHighLow(closeList);
//
//        List<Date> buyDate = new ArrayList<Date>();
//        List<Double> buyD = new ArrayList<Double>();
//        List<Date> sellDate = new ArrayList<Date>();
//        List<Double> sellD = new ArrayList<Double>();
//
//        StockTRHistoryObj prevThObj = null;
//        Collections.reverse(thObjList);
//
//        for (int i = 0; i < thObjList.size(); i++) {
//            StockTRHistoryObj thObj = thObjList.get(i);
//            if (i == 0) {
//                prevThObj = thObj;
//            }
//            Date da = new Date(thObj.getUpdateDatel());
//            xDate.add(da);
//            float close = thObj.getClose();
//            double norClose = normal.getNormalizeValue(close);
//            yD.add(norClose);
//
//            int signal = thObj.getTrsignal();
//            if (signal != prevThObj.getTrsignal()) {
//
//                if (signal == ConstantKey.S_BUY) {
//                    buyD.add(norClose);
//                    buyDate.add(da);
//                }
//                if (signal == ConstantKey.S_SELL) {
//                    sellD.add(norClose);
//                    sellDate.add(da);
//                }
//            }
//            prevThObj = thObj;
//        }
//        ChartService chart = new ChartService();
//        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
//                xDate, yD, buyDate, buyD, sellDate, sellD);
//
//        return ioStream;
//
//    }
    public ArrayList<String> getAccountStockTRListHistoryDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        ArrayList<StockTRHistoryObj> thObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);

        ArrayList<String> writeArray = new ArrayList();
        ArrayList<String> displayArray = new ArrayList();
        int ret = getAccountStockTRListHistoryDisplayProcess(thObjList, writeArray, displayArray);
        if (ret == 1) {
            if (getEnv.checkLocalPC() == true) {
                FileUtil.FileWriteTextArray(FileLocalDebugPath + stockidsymbol + "_" + trname + "_tran.csv", writeArray);
            }
        }
        return displayArray;
    }

    public int getAccountStockTRListHistoryDisplayProcess(ArrayList<StockTRHistoryObj> trObjList, ArrayList<String> writeArray, ArrayList<String> displayArray) {

        if (trObjList == null) {
            return 0;
        }
        for (int i = 0; i < trObjList.size(); i++) {
            StockTRHistoryObj trObj = trObjList.get(i);
            String st = "";
            String stDispaly = "";
            if (writeArray.size() == 0) {
                st = "\"symbol" + "\",\"trname" + "\",\"type";
                /////
                if (trObj.getType() == ConstantKey.INT_TR_MV) {
                    st += "\",\"ema2050" + "\",\"last ema2050" + "\",\"LTerm" + "\",\"STerm" + "\",\"-";

                } else if (trObj.getType() == ConstantKey.INT_TR_MACD) {
                    st += "\",\"macd 12 26" + "\",\"signal 9" + "\",\"diff" + "\",\"-" + "\",\"-";

                } else if (trObj.getType() == ConstantKey.INT_TR_RSI) {
                    st += "\",\"rsi 14" + "\",\"last rsi 14" + "\",\"-" + "\",\"-" + "\",\"-";

                } else {
                    st += "\",\"parm1" + "\",\"parm2" + "\",\"parm3" + "\",\"parm4" + "\",\"name";
                }
                ////
                st += "\",\"close" + "\",\"trsignal" + "\",\"updateDateD" + "\"";
                writeArray.add(st);
                stDispaly = st.replaceAll("\"", "");
                displayArray.add(stDispaly);
            }
            st = "\"" + trObj.getSymbol() + "\",\"" + trObj.getTrname() + "\",\"" + trObj.getType();
            st += "\",\"" + trObj.getParm1() + "\",\"" + trObj.getParm2() + "\",\"" + trObj.getParm3() + "\",\"" + trObj.getParm4() + "\",\"" + trObj.getParmSt1();
            st += "\",\"" + trObj.getClose() + "\",\"" + trObj.getTrsignal() + "\",\"" + trObj.getUpdateDateD() + "\"";

            writeArray.add(st);
            stDispaly = st.replaceAll("\"", "");
            displayArray.add(stDispaly);

        }

        return 1;
    }

    public ArrayList<StockTRHistoryObj> getAccountStockTRListHistory(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList<TradingRuleObj> trObjList = ServiceAFweb.this.getAccountStockTRListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol);
        trname = trname.toUpperCase();
        if (trObjList != null) {
            for (int i = 0; i < trObjList.size(); i++) {
                TradingRuleObj trObj = trObjList.get(i);
                if (trname.equals(trObj.getTrname())) {
                    ArrayList<StockTRHistoryObj> thObjList = TRprocessImp.ProcessTRHistory(this, trObj, 2, CKey.MONTH_SIZE);
                    return thObjList;
                }
            }
        }
        return null;
    }

    public int updateAccountStockSignal(TRObj stockTRObj) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        return getAccountImp().updateAccountStockSignal(stockTRObj.getTrlist());

    }

    public int addAccountStockByCustAcc(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj == null) {
            int result = addStock(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
            //  get the stock object after added into the stockDB
            stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            if (stockObj == null) {
                return 0;
            }
        }
        if (stockObj.getStatus() != ConstantKey.OPEN) {
            // set to open
            int result = addStock(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
        }
        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {
            return getAccountImp().addAccountStockId(accountObj, stockObj.getId(), TRList);
        }
        return 0;
    }

    public int addAccountStockByAccount(AccountObj accountObj, String symbol) {
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj == null) {
            int result = addStock(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
            //  get the stock object after added into the stockDB
            stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
            if (stockObj == null) {
                return 0;
            }
        }
        if (stockObj.getStatus() != ConstantKey.OPEN) {
            // set to open
            int result = addStock(NormalizeSymbol);
            if (result == 0) {
                return 0;
            }
        }
        return getAccountImp().addAccountStockId(accountObj, stockObj.getId(), TRList);
    }

    public int systemRemoveAllEmail() {
        getAccountImp().removeCommByType(CKey.ADMIN_USERNAME, null, ConstantKey.INT_TYPE_COM_EMAIL);
        return 1;
    }

    public int removeAccountStockByUserNameAccId(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj != null) {
            AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
            if (accountObj != null) {
                return removeAccountStockSymbol(accountObj, stockObj.getSymbol());
            }
        }
        return 0;
    }

    //ConstantKey.NOTEXISTED
    public int removeAccountStockSymbol(AccountObj accountObj, String symbol) {

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj != null) {

            int signal = ConstantKey.S_NEUTRAL;
            String trName = ConstantKey.TR_ACC;
            TradingRuleObj tradingRuleObj = SystemAccountStockIDByTRname(accountObj.getId(), stockObj.getId(), trName);
            if (tradingRuleObj == null) {
                return ConstantKey.NOTEXISTED;
            }
            int curSignal = tradingRuleObj.getTrsignal();

            boolean updateTran = true;
            if (curSignal == ConstantKey.S_BUY) {
                ;
            } else if (curSignal == ConstantKey.S_SELL) {
                ;
            } else {
                updateTran = false;
            }
            if (updateTran == true) {
                TradingSignalProcess TRprocessImp = new TradingSignalProcess();
                tradingRuleObj.setLinktradingruleid(ConstantKey.INT_TR_ACC);

                ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
                UpdateTRList.add(tradingRuleObj);
                getAccountImp().updateAccountStockSignal(UpdateTRList);

                AccountTranImp accountTran = new AccountTranImp();
                accountTran.AddTransactionOrderWithComm(this, accountObj, stockObj, trName, signal);
            }

            return getAccountImp().removeAccountStock(accountObj, stockObj.getId());
        }

        return 0;
    }

    public int addStock(String symbol) {
        StockProcess stockProcess = new StockProcess();
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        int result = getStockImp().addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
            stockProcess.ResetStockUpdateNameArray(this);
        }
        return result;
    }

    public int cleanAllStockInfo() {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        logger.info("> cleanAllStockInfo");
        AccountObj accountObj = getAdminObjFromCache();
        ArrayList<String> stockNameArray = SystemAccountStockNameList(accountObj.getId());
        for (int i = 0; i < stockNameArray.size(); i++) {
            String symbol = stockNameArray.get(i);
            if (symbol.equals("T.T")) {
                continue;
            }
            AFstockObj stockObj = getStockRealTime(symbol);
            if (stockObj == null) {
                continue;
            }
            if (CKey.CACHE_STOCKH == true) {

                long endStaticDay = 0;
                ArrayList<AFstockInfo> stockInfoArrayStatic = TradingNNprocess.getAllStockHistory(symbol);
                if (stockInfoArrayStatic == null) {
                    stockInfoArrayStatic = new ArrayList();
                }
                if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                    AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                    endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                    endStaticDay = TimeConvertion.addDays(endStaticDay, -3);
                    getStockImp().deleteStockInfoByDate(stockObj, endStaticDay);
                }

            }
        }
        return 1;
    }

    public int removeAllStockInfo() {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        AccountObj accountObj = getAdminObjFromCache();
        ArrayList<String> stockNameArray = SystemAccountStockNameList(accountObj.getId());
        for (int i = 0; i < stockNameArray.size(); i++) {
            String symbol = stockNameArray.get(i);
            removeStockInfo(symbol);
        }

        return 1;
    }

    public boolean checkTRListByStockID(String StockID) {
        if (getServerObj().isSysMaintenance() == true) {
            return true;
        }
        return getAccountImp().checkTRListByStockID(StockID);
    }

    public int removeStockInfo(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockRealTime(NormalizeSymbol);
        if (stockObj != null) {
            return getStockImp().deleteStockInfoByStockId(stockObj);
        }
        return 0;
    }

    public int deleteStock(AFstockObj stock) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return getStockImp().deleteStock(stock);
    }

    public int disableStock(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return getStockImp().disableStock(NormalizeSymbol);
    }

    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        AFstockObj stock = serviceAFWeb.getStockRealTime(NormalizeSymbol);
        if (stock == null) {
            return false;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return false;
        }
        if (stock.getAfstockInfo() == null) {
            return false;
        }
        return true;
    }

    public AFstockObj getStockRealTime(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        AFstockObj stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);

        if (mydebugSim == true) {
            Calendar cDate = null;
            cDate = Calendar.getInstance();
            cDate.setTimeInMillis(ServiceAFweb.SimDateL);
            ArrayList<AFstockInfo> stockInfolist = getStockHistorical(NormalizeSymbol, 80);
            if (stockInfolist != null) {
                if (stockInfolist.size() > 0) {
                    AFstockInfo stockinfo = stockInfolist.get(0);

                    stock.setAfstockInfo(stockinfo);
                    stock.setUpdatedatel(SimDateL);
                    stock.setUpdatedatedisplay(new java.sql.Date(SimDateL));
                    stock.setPrevClose(stockinfo.getFopen());

                    String tzid = "America/New_York"; //EDT
                    TimeZone tz = TimeZone.getTimeZone(tzid);
                    Date d = new Date(stock.getUpdatedatel());
                    DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                    format.setTimeZone(tz);
                    String ESTdate = format.format(d);
                    stock.setUpdateDateD(ESTdate);

                    return stock;
                }
            }
        }

        return stock;
    }

    public ArrayList<AFstockInfo> getStockHistoricalRange(String symbol, long start, long end) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return SystemStockHistoricalRange(symbol, start, end);
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        ArrayList<AFstockInfo> stockInfoArray = getStockImp().getStockHistoricalRange(NormalizeSymbol, start, end);

        return stockInfoArray;
    }

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> getStockHistorical(String symbol, int length) {
        ServiceAFweb.lastfun = "getStockHistorical";

        if (length == 0) {
            return null;
        }
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
//        if (checkCallRemoveMysql() == true) {
//            return getServiceAFwebREST().getStockHistorical(symbol, length);
//        }
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        List<AFstockInfo> mergedList = new ArrayList();

        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        //////some bug in Heroku to get the current day actually missing the first date
        ///// may be the server time - 2hr when try to do end of day not working in this case.
        ///// so, need work around to move to next begining of day
        long endDay = TimeConvertion.workaround_nextday_endOfDayInMillis(dateNow.getTimeInMillis());
        long start = endDay;
        float len = (float) (1.5 * length);  // add sat sun in to the length
        length = (int) (len);
        long end = TimeConvertion.addDays(start, -length);

        if (CKey.CACHE_STOCKH == true) {
            start = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
            end = TimeConvertion.addDays(start, -length);

            long endStaticDay = 0;
            ArrayList<AFstockInfo> stockInfoArrayStatic = TradingNNprocess.getAllStockHistory(NormalizeSymbol);
            if (stockInfoArrayStatic == null) {
                stockInfoArrayStatic = new ArrayList();
            }
            if (stockInfoArrayStatic.size() > 0) {
//                logger.info("> getStockHistorical" + NormalizeSymbol + " " + stockInfoArrayStatic.size());
                AFstockInfo stockInfo = stockInfoArrayStatic.get(0);
                endStaticDay = TimeConvertion.endOfDayInMillis(stockInfo.getEntrydatel());
                end = TimeConvertion.addDays(endStaticDay, 1);

            }

            long startLoop = start;
            long endLoop = 0;
            while (true) {
                long endDay100 = TimeConvertion.addDays(startLoop, -100);
                endLoop = TimeConvertion.endOfDayInMillis(endDay100);
                if (endLoop <= end) {
                    endLoop = end;
                }
                ArrayList<AFstockInfo> stockInfoArray = getStockHistoricalRange(NormalizeSymbol, startLoop, endLoop);
                if (stockInfoArray == null) {
                    break;
                }
                if (stockInfoArray.size() == 0) {
                    break;
                }
                mergedList.addAll(stockInfoArray);
                startLoop = TimeConvertion.addMiniSeconds(endLoop, -10);
                if (endLoop == end) {
                    break;
                }
            }
            mergedList.addAll(stockInfoArrayStatic);

        } else {
            long startLoop = start;
            long endLoop = 0;
            while (true) {
                long endDay100 = TimeConvertion.addDays(startLoop, -100);
                endLoop = TimeConvertion.endOfDayInMillis(endDay100);
                if (endLoop <= end) {
                    endLoop = end;
                }
                ArrayList<AFstockInfo> stockInfoArray = getStockHistoricalRange(NormalizeSymbol, startLoop, endLoop);
                if (stockInfoArray == null) {
                    break;
                }
                if (stockInfoArray.size() == 0) {
                    break;
                }
                mergedList.addAll(stockInfoArray);
                startLoop = TimeConvertion.addMiniSeconds(endLoop, -10);
                if (endLoop == end) {
                    break;
                }
            }
        }
        if (mergedList.size() == 0) {
            return (ArrayList) mergedList;
        }
//        if (length < 50) {
//            ArrayList<AFstockInfo> sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
//            ArrayList<AFstockInfo> retArray = new ArrayList();
//            for (int i = 0; i < sockInfoArray.size(); i++) {
//                AFstockInfo sInfo = sockInfoArray.get(i);
//                retArray.add(sInfo);
//                if (i > length) {
//                    break;
//                }
//            }
//            return retArray;
//        }

        ////////////////error in HEROKU and Local not sure why?????? //////////////
        ////////////////error in HEROKU and Local not sure why?????? //////////////
        ////////////////error in HEROKU and Local not sure why?????? //////////////
        // TZ problem make sure it is set to TZ Canada/Eastern
        if (mergedList.size() > 1) {

//           AFstockInfo first = mergedList.get(0);
//           AFstockInfo first1 = mergedList.get(1);
//           logger.info(symbol + "getStockHistorical first " + first.getEntrydatel() + " first-1 " + first1.getEntrydatel());
            AFstockInfo last = mergedList.get(mergedList.size() - 1);
            AFstockInfo last1 = mergedList.get(mergedList.size() - 2);

            if (last.getEntrydatel() > last1.getEntrydatel()) {
//                logger.info(symbol + " getStockHistorical last " + last.getEntrydatel() + " last-1 " + last1.getEntrydatel());
                //drop the last become only the last one become the current day (not happen in local) 
                mergedList.remove(last);

            }
        }
//        return (ArrayList) mergedList;
        ArrayList<AFstockInfo> sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
        ArrayList<AFstockInfo> retArray = new ArrayList();
        for (int i = 0; i < sockInfoArray.size(); i++) {
            AFstockInfo sInfo = sockInfoArray.get(i);
            retArray.add(sInfo);
            if (i > length) {
                break;
            }
        }

        if (mydebugSim == true) {
            sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
            retArray = new ArrayList();
            for (int i = 0; i < sockInfoArray.size(); i++) {
                AFstockInfo sInfo = sockInfoArray.get(i);
                if (sInfo.getEntrydatel() > SimDateL) {
                    continue;
                }
                retArray.add(sInfo);
                if (i > length) {
                    break;
                }
            }
        }

        return retArray;

    }

    public ArrayList getAllOpenStockNameArray() {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockNameList = getStockImp().getOpenStockNameArray();
        return stockNameList;
    }

    public ArrayList getStockArray(int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList stockList = getStockImp().getStockArray(length);
        return stockList;
    }

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

    public int releaseNeuralNetObj(String name) {
        logger.info("> releaseNeuralNetObj " + name);
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
//        return getStockImp().releaseNeuralNetObj(name);
        return getStockImp().releaseNeuralNetBPObj(name);
    }

    public AFneuralNet getNeuralNetObjWeight0(String name, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return getStockImp().getNeuralNetObjWeight0(name);
    }

    public AFneuralNet getNeuralNetObjWeight1(String name, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return getStockImp().getNeuralNetObjWeight1(name);
    }

    public int setNeuralNetObjWeight0(AFneuralNet nn) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty

        int ret = getStockImp().setCreateNeuralNetObjRef0(nn.getName(), nn.getWeight(), nn.getRefname());
        return ret;
    }

    public int setNeuralNetObjWeight1(AFneuralNet nn) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty
        return getStockImp().setCreateNeuralNetObj1(nn.getName(), nn.getWeight());
    }
    // require oldest date to earliest
    // require oldest date to earliest

    public int updateStockAllSrv() {

        StockProcess stockProcess = new StockProcess();
        return stockProcess.updateAllStock(this);
    }

    public int updateStockInfoTransaction(StockInfoTranObj stockInfoTran) {
        ServiceAFweb.lastfun = "updateStockInfoTransaction";

        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return getStockImp().updateStockInfoTransaction(stockInfoTran);
    }

//    public int AccountingYearEnd(String customername, String yearSt) {
//        ServiceAFweb.lastfun = "insertAccountEarning";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            int year = 0;
//            if (yearSt != null) {
//                if (yearSt.length() > 0) {
//                    try {
//                        year = Integer.parseInt(yearSt);
//                    } catch (Exception e) {
//                    }
//                }
//            }
//
//            return getAccounting().closingYearEnd(this, customer, year);
//
//        } catch (Exception e) {
//        }
//        return 0;
//    }

//    public int removeAccounting(String customername, String yearSt) {
//        ServiceAFweb.lastfun = "insertAccountEarning";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            int year = 0;
//            if (yearSt != null) {
//                if (yearSt.length() > 0) {
//                    try {
//                        year = Integer.parseInt(yearSt);
//                    } catch (Exception e) {
//                    }
//                }
//            }
//            if (year == -99) {
//                return getAccountImp().removeAccountingAll();
//            }
//
//            int newYear = 0;
//            if (year != 0) {
//                newYear = year * 12;
//            }
//
//            // begin 2021 01 01  (updatedatel)  end 2021 12 31
//            long BeginingYear = DateUtil.getFirstDayCurrentYear();
//            long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);
//
//            if (newYear != 0) {
//                BeginingYear = TimeConvertion.addMonths(BeginingYear, newYear);
//                EndingYear = TimeConvertion.addMonths(EndingYear, newYear);
//            }
//
//            EndingYear = TimeConvertion.addDays(EndingYear, -1);
//            return getAccountImp().removeAccounting(BeginingYear, EndingYear);
//        } catch (Exception e) {
//        }
//        return 0;
//    }

//    public int insertAccountEarning(String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
//        ServiceAFweb.lastfun = "insertAccountEarning";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//            BillingProcess BP = new BillingProcess();
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System Retained Earning change " + currency;
//
////                    if (reasonSt != null) {
////                        if (reasonSt.length() > 0) {
////
////                        }
////                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferEarning(this, customer, payment, year, commSt);
//
//                }
//            }
//
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

//    public int insertAccountTAX(String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
//        ServiceAFweb.lastfun = "insertAccountTAX";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//            BillingProcess BP = new BillingProcess();
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System TAX change " + currency;
//
////                    if (reasonSt != null) {
////                        if (reasonSt.length() > 0) {
////
////                        }
////                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferPayTax(this, customer, payment, commSt);
//
//                }
//            }
//
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

//    public int insertAccountCash(String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
//        ServiceAFweb.lastfun = "insertAccountCash";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//            BillingProcess BP = new BillingProcess();
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System Cash change " + currency;
//
////                    if (reasonSt != null) {
////                        if (reasonSt.length() > 0) {
////
////                        }
////                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferCash(this, customer, payment, year, commSt);
//
//                }
//            }
//
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

//    public int updateAccountingExUtility(String customername, String paymentSt, String yearSt, String reasonSt, String commentSt) {
//        ServiceAFweb.lastfun = "updateAccountingExUtility";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System expense change " + currency;
//
////                    if (reasonSt != null) {
////                        if (reasonSt.length() > 0) {
////
////                        }
////                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferUtilityExpense(this, customer, payment, year, commSt);
//
//                    ret = 1;
//                }
//            }
//
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

//    public int updateAccountingExDeprecation(String customername, String paymentSt, String rateSt, String reasonSt, String commentSt) {
//        ServiceAFweb.lastfun = "updateAccountingExDeprecation";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System expense change " + currency;
//
////                    if (reasonSt != null) {
////                        if (reasonSt.length() > 0) {
////
////                        }
////                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    float rate = 100;
//                    if (rateSt != null) {
//                        if (rateSt.length() > 0) {
//                            try {
//                                rate = Float.parseFloat(rateSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferDepreciation(this, customer, payment, rate, commSt);
//                    ret = 1;
//                }
//            }
//
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

//updateAccountingEntryPaymentBalance
//    public int updateAccountingEntryPaymentBalance(String customername, String paymentSt, String balanceSt,
//            String reasonSt, String rateSt, String yearSt, String commentSt) {
//        ServiceAFweb.lastfun = "updateAccountingPaymentBalance";
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        customername = customername.toUpperCase();
//        NameObj nameObj = new NameObj(customername);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            CustomerObj customer = this.getAccountImp().getCustomerPasswordNull(UserName);
//            if (customer == null) {
//                return 0;
//            }
//            String comment = "";
//            if (commentSt != null) {
//                comment = commentSt;
//            }
//
//            float payment = 0;
//            String commSt = "";
//            int ret = 0;
//            if (paymentSt != null) {
//                if (!paymentSt.equals("")) {
//                    payment = Float.parseFloat(paymentSt);
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(payment);
//                    commSt += "System expense change " + currency;
//
//                    ////////update accounting entry
//                    String entryName = "";
//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//                            entryName = reasonSt;
//                        }
//                    }
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    float rate = 100;
//                    if (rateSt != null) {
//                        if (rateSt.length() > 0) {
//                            try {
//                                rate = Float.parseFloat(rateSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    if (entryName.equals(AccountingProcess.E_USER_WITHDRAWAL)) {
//                        ret = getAccounting().addTransferWithDrawRevenueTax(this, customer, payment, year, entryName + " " + commSt);
//                    } else {
//                        ret = getAccounting().addTransferExpenseTax(this, customer, payment, rate, year, commSt);
//                    }
//                }
//            }
//            float balance = 0;
//            if (balanceSt != null) {
//                if (!balanceSt.equals("")) {
//                    balance = Float.parseFloat(balanceSt);
//
//                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
//                    String currency = formatter.format(balance);
//
//                    commSt += "System revenue change " + currency;
//
//                    ////////update accounting entry
//                    String entryName = "";
//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//                            entryName = reasonSt;
//                        }
//                    }
//
//                    if (comment.length() > 0) {
//                        commSt = comment;
//                    }
//                    int year = 0;
//                    if (yearSt != null) {
//                        if (yearSt.length() > 0) {
//                            try {
//                                year = Integer.parseInt(yearSt);
//                            } catch (Exception e) {
//                            }
//                        }
//                    }
//                    ret = getAccounting().addTransferRevenueTax(this, customer, balance, year, commSt);
//
//                }
//            }
//            if (ret == 1) {
//                String tzid = "America/New_York"; //EDT
//                TimeZone tz = TimeZone.getTimeZone(tzid);
//                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
////                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
//                DateFormat format = new SimpleDateFormat(" hh:mm a");
//                format.setTimeZone(tz);
//                String ESTtime = format.format(d);
//
//                String msg = ESTtime + " " + commSt;
//
//                AccountObj accountAdminObj = getAdminObjFromCache();
//                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);
//
//            }
//            return ret;
//
//        } catch (Exception e) {
//
//        }
//        return 0;
//    }

    //http://localhost:8080/cust/admin1/sys/cust/eddy/update?substatus=10&investment=0&balance=15&?reason=
    public int updateAddCustStatusPaymentBalance(String customername,
            String statusSt, String paymentSt, String balanceSt, String yearSt, String reasonSt) {
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
            ArrayList accountList = getAccountList(UserName, null);

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
            String emailSt = "";
            int status = -9999;
            if (statusSt != null) {
                if (!statusSt.equals("")) {
                    status = Integer.parseInt(statusSt);
                    String st = "Disabled";
                    if (status == ConstantKey.OPEN) {
                        st = "Enabled";
                    }
                    emailSt += "\n\r " + customername + " Accout Status change to " + st;
                }
            }
            float payment = -9999;
            if (paymentSt != null) {
                if (!paymentSt.equals("")) {
                    payment = Float.parseFloat(paymentSt);
                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(payment);
                    emailSt += "\n\r " + customername + " Accout invoice bill adjust " + currency;

                }
            }
            float balance = -9999;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(balance);
                    emailSt += "\n\r " + customername + " Accout balance adjust " + currency;

                    ////////update accounting entry
                    String entryName = "";
                    if (reasonSt != null) {
                        if (reasonSt.length() > 0) {
                            entryName = reasonSt;
                        }
                    }
                    if (customer != null) {
                        boolean byPassPayment = BillingProcess.isSystemAccount(customer);
                        if (byPassPayment == false) {
                            int year = 0;
                            if (yearSt != null) {
                                if (yearSt.length() > 0) {
                                    try {
                                        year = Integer.parseInt(yearSt);
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            if (entryName.equals(AccountingProcess.E_USER_WITHDRAWAL)) {
                                // UI will set payment to negative 
                                float withdraw = -balance;
                                int ret = getAccounting().addTransferWithDrawRevenueTax(this, customer, withdraw, year, entryName + " " + emailSt);
                            } else if (entryName.equals(AccountingProcess.R_USER_PAYMENT)) {
                                int ret = getAccounting().addTransferRevenueTax(this, customer, balance, year, emailSt);
                            }
                        }
                    }

                }
            }
            int ret = getAccountImp().updateAddCustStatusPaymentBalance(UserName, status, payment, balance);
            if (ret == 1) {
                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTtime = format.format(d);

                String msg = ESTtime + " " + emailSt;

                getAccountImp().addAccountMessage(accountObj, ConstantKey.ACCT_TRAN, msg);
                AccountObj accountAdminObj = getAdminObjFromCache();
                getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

                // send email
                DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                formatD.setTimeZone(tz);
                String ESTdateD = formatD.format(d);
                String msgD = ESTdateD + " " + emailSt;
                getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.ACCT_TRAN, msgD);

            }
            return ret;

        } catch (Exception e) {
        }
        return 0;
    }

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
            ArrayList accountList = getAccountList(UserName, null);

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

//http://localhost:8080/cust/admin1/sys/cust/eddy/status/0/substatus/0
    public int updateCustStatusSubStatus(String customername, String statusSt, String substatusSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
//
//        if (checkCallRemoteMysql() == true) {
//            return getServiceAFwebREST().updateCustStatusSubStatus(customername, statusSt, substatusSt);
//        }

        int status;
        int substatus;
        try {
            status = Integer.parseInt(statusSt);
            substatus = Integer.parseInt(substatusSt);
        } catch (NumberFormatException e) {
            return 0;
        }
        CustomerObj custObj = getAccountImp().getCustomerBySystem(customername, null);
        custObj.setStatus(status);
        custObj.setSubstatus(substatus);
        return getAccountImp().updateCustStatusSubStatus(custObj, custObj.getStatus(), custObj.getSubstatus());
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
                    nameST = getStockImp().getAllStockInfoDBSQL(sqlObj.getReq());
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
                        int result = getStockImp().updateSQLArrayList(SQLArray);
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
                        StockInfoTranObj stockInfoTran = new ObjectMapper().readValue(st, StockInfoTranObj.class
                        );
                        int result = getStockImp().updateStockInfoTransaction(stockInfoTran);
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
                        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
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
                        ArrayList<AFstockInfo> StockArray = getStockImp().getStockHistoricalRange(symbol, start, end);
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
                    AFstockObj stockObj = getStockImp().getRealTimeStockByStockID(stockId, null);
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

    public boolean SystemFundResetGlobal() {
        FundMgrProcess fundmgr = new FundMgrProcess();
        logger.info(">ProcessGetGlobalFundMgr start ");
        fundmgr.ProcessGetGlobalFundMgr(this);
//        fundmgr.ProcessFundMgrAccount(this);
        return true;
    }

    public boolean SystemFundSelectBest() {
        FundMgrProcess fundmgr = new FundMgrProcess();
        logger.info(">ProcessSelectBestFundMgrAccount start ");
        fundmgr.ProcessSelectBestFundMgrAccount(this);
        return true;
    }

    public boolean SystemFundPocessAddRemove() {
        logger.info(">ProcessAddRemoveFundAccount start ");
        getAccountProcessImp().ProcessAddRemoveFundAccount(this);
        return true;
    }

    public boolean SystemDeleteNN1Table() {
        logger.info(">SystemDeleteNN1Table start ");
        getStockImp().deleteNeuralNet1Table();
        logger.info(">SystemDeleteNN1Table end ");
        return true;
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

    public String SystemCleanDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        retSatus = getStockImp().cleanStockDB();
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
        retSatus = getStockImp().restStockDB();
        return "" + retSatus;
    }

    public String SystemClearNNinput() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;

        retSatus = NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN1);
//            retSatus = NNProcessImp.ClearStockNNinputNameArray(this, ConstantKey.TR_NN2);

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

    public String SystemClearNNtran(int tr) {

        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;
        if (tr == ConstantKey.SIZE_TR) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3);
        } else if (tr == ConstantKey.INT_TR_ACC) {
            retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(this, ConstantKey.TR_ACC, "");
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_ACC);
        } else if (tr == ConstantKey.INT_TR_MACD) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD);
        } else if (tr == ConstantKey.INT_TR_MV) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV);
        } else if (tr == ConstantKey.INT_TR_RSI) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI);
        } else if (tr == ConstantKey.INT_TR_NN1) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2);
        } else if (tr == ConstantKey.INT_TR_NN3) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN3);
        }

        return "" + retSatus;
    }

    public String SystemStart() {
        boolean retSatus = true;
        serverObj.setSysMaintenance(false);
        serverObj.setTimerInit(false);
        serverObj.setTimerQueueCnt(0);
        serverObj.setTimerCnt(0);
        return "sysMaintenance " + retSatus;
    }

    public int testDBData() {
        logger.info(">testDBData ");
        int retSatus = getStockImp().testStockDB();
        return retSatus;
    }

    public int InitDBData() {
        logger.info(">InitDBData ");
        // 0 - new db, 1 - db already exist, -1 db error
        int retStatus = getStockImp().initStockDB();

        if (retStatus >= 0) {
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
                        AFstockObj stock = getStockImp().getRealTimeStock(stockN, null);
                        logger.info(">InitDBData add stock " + stock.getSymbol());
                        result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
                    }
                    AFstockObj stock = getStockImp().getRealTimeStock("T.TO", null);
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

}
