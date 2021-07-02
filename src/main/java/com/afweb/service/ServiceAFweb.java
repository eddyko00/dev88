/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.processcustacc.AccountTranImp;
import com.afweb.processsystem.BackupRestoreImp;
import com.afweb.stockinternet.StockInternetImpDao;
import com.afweb.processnn.*;
import com.afweb.nnsignal.*;

import com.afweb.processemail.EmailProcess;
import com.afweb.processcustacc.PUBSUBprocess;
import com.afweb.processbilling.BillingProcess;

import com.afweb.processsystem.AccountMaintProcess;

import com.afweb.model.*;
import com.afweb.chart.ChartService;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.processcustacc.CustAccService;
import com.afweb.processnn.NNetService;
import com.afweb.processstock.StockService;
import com.afweb.processstockinfo.*;
import com.afweb.processsystem.SystemService;

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

    private DataSource dataSource;
    private String REMOTE_URL = "";

    public static String serverLockName = "server";
    public static boolean NN_AllowTraingStockFlag = false;
    private static boolean initProcessTimer = false;
    private static int delayProcessTimer = 0;
    private static long timerThreadDateValue = 0;

    public static String PROXYURL = "";
    public static String FileLocalPath = "";

    public static String UA_Str = "";
    public static String PA_Str = "";
    public static String UU_Str = "";

    public static String URL_INFO = "";

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

    public int AFtimerThread() {

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
            AFtimerHandler("");
//            }
        } catch (Exception ex) {
            logger.info("> timerThread Exception" + ex.getMessage());
        }

        return getServerObj().getTimerCnt();
    }

    //Repeat every 10 seconds
    public int AFtimerHandler(String timerThreadMsg) {
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
                SysInitDataSource();
                SysInitStaticData();   // init TR data

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
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                displayStr += "\r\n" + (">>>>> LOCAL_MYSQL = 0");
                displayStr += "\r\n" + (">>>>> DIRECT__MYSQL = 1");
                displayStr += "\r\n" + (">>>>> REMOTE_PHP_MYSQL = 2");
                displayStr += "\r\n" + (">>>>> REMOTE_PHP_1_MYSQL = 3");
                displayStr += "\r\n" + (">>>>> System SQL_DATABASE:" + CKey.SQL_DATABASE);

                getServerObj().setLocalDBservice(true);
                if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                    getServerObj().setLocalDBservice(true);

                } else if (CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) {
                    getServerObj().setLocalDBservice(true);

                } else if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) {
                    // need this for SystemSQLRequest
                    getServerObj().setLocalDBservice(false);

                } else if (CKey.SQL_DATABASE == CKey.REMOTE_PHP_1_MYSQL) {
                    getServerObj().setLocalDBservice(false);
                }

                displayStr += "\r\n" + (">>>>> System REMOTE DB URL:" + REMOTE_URL);
                displayStr += "\r\n" + (">>>>> System SERVER_DB_URL:" + CKey.SERVER_DB_URL);
                displayStr += "\r\n" + (">>>>> System SERVER_TIMMER_URL:" + CKey.SERVER_TIMMER_URL);
                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

                displayStr += "\r\n" + (">>>>> System backupFlag:" + CKey.backupFlag);
                displayStr += "\r\n" + (">>>>> System backupInfoFlag:" + CKey.backupInfoFlag);
                displayStr += "\r\n" + (">>>>> System backupNNFlag:" + CKey.backupNNFlag);
                displayStr += "\r\n" + (">>>>> System restoreFlag:" + CKey.restoreFlag);
                displayStr += "\r\n" + (">>>>> System restoreInfoFlag:" + CKey.restoreInfoFlag);
                displayStr += "\r\n" + (">>>>> System restoreNNFlag:" + CKey.restoreNNFlag);
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

                displayStr += "\r\n" + (">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                logger.info(displayStr);

                boolean retFlag = AFbackupRestoreSystem();
                if (retFlag == true) {
                    // backup or restore is done
                    logger.info(">>>>>>> backupRestoreSystem done.........");
                    getServerObj().setSysMaintenance(true);
                    return getServerObj().getTimerCnt();
                }

                if (CKey.UI_ONLY == false) {
                    String sysPortfolio = "";
                    // make sure not request during DB initialize

                    getServerObj().setSysMaintenance(true);
                    logger.info(">>>>>>> InitDBData started.........");
                    // 0 - new db, 1 - db already exist, -1 db error
                    int ret = SysInitDBData();  // init DB Adding customer account
//                        sysPortfolio = CKey.FUND_PORTFOLIO;
                    if (ret != -1) {

                        SysInitSystemData();   // Add Stock 
                        SysInitSystemFund(sysPortfolio);
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

                    SysSetLockName(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, serverObj.getSrvProjName() + " " + serverObj.getServip());

                    //try 2 times
                    ProcessAdminAddRemoveStock(this);
                    ProcessAdminAddRemoveStock(this);

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

    private boolean AFbackupRestoreSystem() {
        boolean retFlag = false;
        if (CKey.NN_DEBUG == true) {

            if (CKey.backupFlag == true) {
                retFlag = backupSystem();
                retFlag = true;
            }
            if (CKey.backupInfoFlag == true) {
                retFlag = backupInfo();
                retFlag = true;
            }
            if (CKey.backupNNFlag == true) {
                retFlag = backupNN();
                retFlag = true;
            }

            if (CKey.restoreFlag == true) {
                retFlag = restoreSystem();
                retFlag = true;
            }
            if (CKey.restoreInfoFlag == true) {
                retFlag = restoreInfo();
                retFlag = true;
            }
            if (CKey.restoreNNFlag == true) {
                retFlag = restoreNN();
                retFlag = true;
            }
        }
        return retFlag;
    }

    private boolean backupSystem() {
        serverObj.setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> backupSystem form DB URL:" + REMOTE_URL);

        boolean retSatus = false;

        BackupRestoreImp backupRestore = new BackupRestoreImp();
        retSatus = backupRestore.downloadDBData(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }
        logger.info("backupSystem " + retSatus);
        return retSatus;
    }

    private boolean backupInfo() {
        serverObj.setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> backupInfo form DB URL:" + REMOTE_URL);

        boolean retSatus = false;

        BackupRestoreInfo backupRestore = new BackupRestoreInfo();
        retSatus = backupRestore.downloadDBDataInfo(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }
        logger.info("backupInfo " + retSatus);
        return retSatus;
    }

    private boolean backupNN() {
        serverObj.setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> backupNN form DB URL:" + REMOTE_URL);

        boolean retSatus = false;

        BackupRestoreNN backupRestore = new BackupRestoreNN();
        retSatus = backupRestore.downloadDBDataNN(this);
        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }
        logger.info("backupNN " + retSatus);
        return retSatus;
    }

    private boolean restoreSystem() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> restoreSystem form DB URL:" + REMOTE_URL);

        Scanner scan = new Scanner(System.in);
        System.out.print("Hit any key to continue to restore restoreSystem?");
        String YN = scan.next();
        boolean retSatus = false;

        retSatus = SysCleanStockDB();
        if (retSatus == true) {
            BackupRestoreImp backupRestore = new BackupRestoreImp();
            retSatus = backupRestore.restoreDBData(this);
            if (retSatus == true) {
                serverObj.setSysMaintenance(true);
                serverObj.setTimerInit(false);
                serverObj.setTimerQueueCnt(0);
                serverObj.setTimerCnt(0);
            }
            getServerObj().setSysMaintenance(true);
            logger.info(">>>>> restoreSystem done");
        }
        return retSatus;
    }

    private boolean restoreInfo() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> restoreInfo form DB URL:" + REMOTE_URL);

        Scanner scan = new Scanner(System.in);
        System.out.print("Hit any key to continue to restore restoreInfo?");
        String YN = scan.next();
        boolean retSatus = false;

        retSatus = InfCleanStockInfoDB();
        if (retSatus == true) {
            BackupRestoreInfo backupRestore = new BackupRestoreInfo();
            retSatus = backupRestore.restoreDBDataInfo(this);
            if (retSatus == true) {
                serverObj.setSysMaintenance(true);
                serverObj.setTimerInit(false);
                serverObj.setTimerQueueCnt(0);
                serverObj.setTimerCnt(0);
            }
            getServerObj().setSysMaintenance(true);
            logger.info(">>>>> restoreInfo done");
        }
        return retSatus;
    }

    private boolean restoreNN() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        logger.info(">>>>> restoreNN form DB URL:" + REMOTE_URL);

        Scanner scan = new Scanner(System.in);
        System.out.print("Hit any key to continue to restore restoreNN?");
        String YN = scan.next();
        boolean retSatus = false;

        retSatus = NnCleanNNdataDB();
        if (retSatus == true) {
            BackupRestoreNN backupRestore = new BackupRestoreNN();
            retSatus = backupRestore.restoreDBDataNN(this);
            if (retSatus == true) {
                serverObj.setSysMaintenance(true);
                serverObj.setTimerInit(false);
                serverObj.setTimerQueueCnt(0);
                serverObj.setTimerCnt(0);
            }
            getServerObj().setSysMaintenance(true);
            logger.info(">>>>> restoreNN done");
        }
        return retSatus;
    }
    //////////////////////////////////////////////////////////
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
                    NNetService nnSrv = new NNetService();
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
                ProcessAllAccountTradingSignal(this);
            } else if (cmd.equals("updatestock")) {
                InfUpdateAllStockInfo();
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
            long lockReturn = SysSetLockName(LockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, "ProcessTimerCnt " + getServerObj().getProcessTimerCnt());

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
                    InfUpdateAllStockInfo();
                    accountTranP.ProcessAdminSignalTrading(this);
                    ProcessAllAccountTradingSignal(this);
                    InfUpdateAllStockInfo();

                }
            }

            /////// main execution
            AFwebExec();
            ///////
        } catch (Exception ex) {
            logger.info("> Exception lastfun - " + lastfun);
            logger.info("> processTimer Exception " + ex.getMessage());
        }
        SysRemoveNameLock(LockName, ConstantKey.SRV_LOCKTYPE);
    }

    void AFwebExec() {

        ////////////
        if (((getServerObj().getProcessTimerCnt() % 29) == 0) || (getServerObj().getProcessTimerCnt() == 1)) {
            long result = SysRenewLock(serverLockName, ConstantKey.SRV_LOCKTYPE);
            if (result == 0) {
                Calendar dateNow1 = TimeConvertion.getCurrentCalendar();
                long lockDateValue1 = dateNow1.getTimeInMillis();
                SysSetLockName(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue1, serverObj.getSrvProjName() + " " + serverObj.getServip());
            }
        }

        //2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
        if ((getServerObj().getProcessTimerCnt() % 11) == 0) {
            // add or remove stock in Mutual fund account based on all stocks in the system
            System.gc();
            SystemFundPocessAddRemoveServ();
//            ProcessAddRemoveFundAccount(this);

            AccountMaintProcess maintProcess = new AccountMaintProcess();
            maintProcess.ProcessSystemMaintance(this);

        } else if ((getServerObj().getProcessTimerCnt() % 7) == 0) {
            InfUpdateAllStockInfo();

            ProcessNeuralNetDataTrain(this);
//            
            ProcessUserBilling(this);
            ProcessPUBSUBAccountComm(this);

        } else if ((getServerObj().getProcessTimerCnt() % 5) == 0) {
//            TRprocessImp.UpdateAllStockTrend(this, true);
            InfUpdateAllStockInfo();

            ProcessAdminSignalTrading(this);
            ProcessAdminAddRemoveStock(this);

        } else if ((getServerObj().getProcessTimerCnt() % 3) == 0) {
            InfUpdateAllStockInfo();
            ProcessAllAccountTradingSignal(this);
            ProcessAdminAddRemoveStock(this);

//            
        } else if ((getServerObj().getProcessTimerCnt() % 2) == 0) {
            if (CKey.PROXY == false) {
                ProcessEmailAccount(this);
            }
        } else {

        }
    }
//////////Account Process

    public void ProcessAdminSignalTrading(ServiceAFweb serviceAFWeb) {
        AccountTranProcess accountTranP = new AccountTranProcess();
        accountTranP.ProcessAdminSignalTrading(this);
    }

    public void ProcessAllAccountTradingSignal(ServiceAFweb serviceAFWeb) {
        AccountTranProcess accountProcessImp = new AccountTranProcess();
        accountProcessImp.ProcessAllAccountTradingSignal(this);
    }

    public void ProcessAdminAddRemoveStock(ServiceAFweb serviceAFWeb) {
        AccountTranProcess accountProcessImp = new AccountTranProcess();
        accountProcessImp.ProcessAdminAddRemoveStock(this);
    }

    public void ProcessUserBilling(ServiceAFweb serviceAFWeb) {
        BillingProcess billProc = new BillingProcess();
        billProc.processUserBillingAll(this);
    }

    public void ProcessPUBSUBAccountComm(ServiceAFweb serviceAFWeb) {
        PUBSUBprocess pubsub = new PUBSUBprocess();
        pubsub.ProcessPUBSUBAccount(this);
    }

    public void ProcessNeuralNetDataTrain(ServiceAFweb serviceAFWeb) {
        NNetService nnSrv = new NNetService();
        nnSrv.AFprocessNeuralNet(this);
    }

    public void ProcessEmailAccount(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.processEmailFlag == true) {
            EmailProcess eProcess = new EmailProcess();
            eProcess.ProcessEmailAccount(this);
        }
    }

/////////////////////////////////////////////
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

    public static boolean SysCheckSymbolDebugTest(String symbol) {

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
//            NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
//            NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
//            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();
//            NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();

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

    private void AFRandomDelayMilSec(int sec) {

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

    public int SysInitDBData() {
        logger.info(">InitDBData ");
        // 0 - new db, 1 - db already exist, -1 db error
        int retStatus = SysInitDBSystem();

        if (retStatus >= 0) {
            //// init StockInfo
            InfInitStockInfoDB();

            NnInitDBNNet();

            logger.info(">InitDB Customer account ");
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(CKey.ADMIN_USERNAME);
            newCustomer.setPassword("passw0rd");
            newCustomer.setFirstname("ADM");
            newCustomer.setType(CustomerObj.INT_ADMIN_USER);
            //// result 1 = success, 2 = existed,  0 = fail
            custAccSrv.addCustomer(newCustomer, -1);

            newCustomer.setUsername(CKey.API_USERNAME);
            newCustomer.setPassword("eddy");
            newCustomer.setFirstname("APIUser");
            newCustomer.setType(CustomerObj.INT_API_USER);
            custAccSrv.addCustomer(newCustomer, -1);

            if (retStatus == 0) {

                newCustomer.setUsername(CKey.G_USERNAME);
                newCustomer.setPassword("guest");
                newCustomer.setFirstname("G");
                newCustomer.setType(CustomerObj.INT_GUEST_USER);
                custAccSrv.addCustomer(newCustomer, -1);

                newCustomer.setUsername(CKey.FUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setFirstname("FundMgr");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                custAccSrv.addCustomer(newCustomer, -1);
//                
                newCustomer.setUsername(CKey.INDEXFUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setFirstname("IndexMgr");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                custAccSrv.addCustomer(newCustomer, -1);

                AccountObj account = custAccSrv.getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);
                if (account != null) {
                    int result = 0;
                    getServerObj().setSysMaintenance(false);
                    for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                        String stockN = ServiceAFweb.primaryStock[i];
                        AFstockObj stock = StoGetStockObjBySym(stockN);
                        logger.info(">InitDBData add stock " + stock.getSymbol());
                        result = custAccSrv.addAccountStockId(account, stock.getId(), TRList);
                    }

                    AFstockObj stock = StoGetStockObjBySym("T.TO");
                    result = custAccSrv.addAccountStockId(account, stock.getId(), TRList);
                    getServerObj().setSysMaintenance(true);
                }
            }

            newCustomer.setUsername(CKey.E_USERNAME);
            newCustomer.setPassword("pass");
            newCustomer.setFirstname("E");
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            custAccSrv.addCustomer(newCustomer, -1);
        }
        return retStatus;
    }

    public void SysInitDataSource() {
        logger.info(">initDataSource ");
        //testing
        WebAppConfig webConfig = new WebAppConfig();
        this.dataSource = webConfig.dataSourceSystem();

        REMOTE_URL = webConfig.dataSourceURLSystem((DriverManagerDataSource) dataSource);

        SysDataSourceSystem(dataSource, REMOTE_URL);

        AccDataSource(dataSource, REMOTE_URL);

        InfSetDataSource(dataSource, REMOTE_URL);

        NnDataSourceNNnet(dataSource, REMOTE_URL);
////////////////////////////////////////
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

        PA_Str = StringTag.replaceAll("abc", "", CKey.PA);
        UA_Str = StringTag.replaceAll("abc", "", CKey.UA);
        UU_Str = StringTag.replaceAll("abc", "", CKey.UU);

        URL_INFO = StringTag.replaceAll("abc", "", CKey.SERVER_TIMMER_URL);
    }

    public void SysInitStaticData() {
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

    public void SysInitSystemData() {
        logger.info(">InitDB InitSystemData for Stock and account ");

    }

    public void SysInitSystemFund(String portfolio) {
        if (portfolio.length() == 0) {
            return;
        }
        CustomerObj custObj = custAccSrv.getCustomerBySystem(CKey.FUND_MANAGER_USERNAME, null);
        ArrayList accountList = custAccSrv.getAccountListByCustomerObj(custObj);
        if (accountList != null) {
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountObj = (AccountObj) accountList.get(i);
                if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                    custAccSrv.updateAccountPortfolio(accountObj.getAccountname(), portfolio);
                    break;
                }
            }
        }

    }

//////////////////////
    public static final int AllName = 10; //"1";
    public static final int AllLock = 11; //"2";

    public static final int RemoteGetMySQL = 12; //"9";
    public static final int RemoteUpdateMySQL = 13; //"10";    
    public static final int RemoteUpdateMySQLList = 14; //"11";   

//    public static final int AllOpenAccountIDList = 15; //"104";
//    public static final int AccountObjByAccountID = 16; //"105";
//    public static final int AccountStockNameList = 17; //"106";
//    public static final int UserNamebyAccountID = 18; //"107";
//    public static final int UpdateTransactionOrder = 19; //"108";
//    public static final int AccountStockClrTranByAccountID = 20; //"111";    
//    public static final int AllAccountStockNameListExceptionAdmin = 21; //"112"; 
    ////////////////////////
    // Customer Account
    public static final int AllUserName = 110; //"1";
    public static final int AllCustomer = 111; //"6";
    public static final int AllAccount = 112; //"7";
    public static final int AllAccountStock = 113; //"8";
//    public static final int AccountStockListByAccountID = 114; //"110";    
    public static final int updateAccountStockSignal = 115;// "102";    
    public static final int AddTransactionOrder = 116; //"113"; 

//    public static final int AccountStockTransList = 117; //"115";     
//    public static final int AccountStockPerfList = 118; //"116";     
//    public static final int AccountStockIDByTRname = 119; //"117";   
//    public static final int AccountStockListByAccountIDStockID = 120; //"118"; 
    public static final int AllPerformance = 121; //"13";  
    public static final int AllBilling = 122; //"17";    
    public static final int AllComm = 123; //"16";
    public static final int AllTransationorder = 124; //"12";   
    //////////////////////////////////
    public static final int AllId = 210; //"1";   
    public static final int AllSQLquery = 211; //"14";  
//    public static final int UpdateSQLList = 101; //"101";

    public static final int AllStock = 212; //"3";    
    public static final int AllSymbol = 213; //"1";    
//    public static final int RealTimeStockByStockID = 214; //"119"; 

    public static final int AllIdInfo = 250; //"1";
    public static final int AllStockInfo = 251; //"4";    
////    public static final int StockHistoricalRange = 252; //"114";     
    public static final int updateStockInfoTransaction = 253; //"103";
//    public static final int UpdateSQLListInfo = 101; //"101";

    public static final int AllNeuralNet = 310; //"5";
    public static final int AllNeuralNetData = 311; //"15";
//    public static final int NeuralNetDataObj = 312; //"120";     
//    public static final int NeuralNetDataObjStockid = 313; //"120";

    //////////////////////////////////
    public RequestObj SysSQLRequest(RequestObj sqlObj) {

//        boolean RemoteCallflag = checkCallRemoteMysql();
//        if (RemoteCallflag == true) {
//            return getServiceAFwebREST().getSQLRequest(sqlObj, CKey.SERVER_TIMMER_URL);
//        }
        try {
            RequestObj reqObj = null;

            reqObj = SysSQLRequestSystem(sqlObj);
            if (reqObj == null) {
                reqObj = AccSQLRequestCustAcc(sqlObj);
            }
            if (reqObj == null) {
                reqObj = StoSQLRequestStock(sqlObj);
            }
            if (reqObj == null) {
                reqObj = InfSQLRequestStockInfo(sqlObj);
            }
            if (reqObj == null) {
                reqObj = NnSQLRequestNN(sqlObj);
            }
            ///////////////////////////////////////////////////////
            if (reqObj != null) {
                return (reqObj);
            }

        } catch (Exception ex) {
            logger.info("> SystemSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

/////////////////////////////////////////////
    public String SysClearLock() {
        int retSatus = 0;
        retSatus = SysLockDeleteAll();
        return "" + retSatus;
    }

    ////////////////////////////////////////////
    public int SysSetLockName(String name, int type, long lockdatel, String comment) {
        int resultLock = SysSetLockNameSystem(name, type, lockdatel, comment);
        // DB will enusre the name in the lock is unique and s
        AFRandomDelayMilSec(200);
        AFLockObject lock = SysGetLockName(name, type);
        if (lock != null) {
            if (lock.getLockdatel() == lockdatel) {
                return 1;
            }
        }
        return 0;
    }

///////////////////////////
    public static boolean SysCheckCallRemoteMysql() {
        boolean ret = true;
        if (ServiceAFweb.getServerObj().isLocalDBservice() == true) {
            ret = false;
        }
        return ret;
    }

    public String SysClearNNData() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        AccountObj accountAdminObj = this.getAdminObjFromCache();
        int retStatus = NNProcessImp.ClearStockNNData(this, accountAdminObj);
        return "" + retStatus;
    }

    public String SysClearNNtranAllAcc() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(this, ConstantKey.TR_ACC, "");
        return "" + retSatus;
    }

    public String SysClearNNtran(String sym) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = NNProcessImp.SysClearNNtranBySymbol(this, sym);
        return "" + retSatus;
    }

    ////////////////////////
    public String SysUpdateFromRemoteMySQLList(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        String st = SQL;
        String[] sqlList = st.split("~");
        for (int i = 0; i < sqlList.length; i++) {
            String sqlCmd = sqlList[i];
            int ret = SysUpdateRemoteMYSQL(sqlCmd);
        }
        return ("" + sqlList.length);
    }

    public String SysUpdateFromRemoteMySQL(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        return SysUpdateRemoteMYSQL(SQL) + "";
    }

    public String SysRemoteGetMySQL(String SQL) {
        if (getServerObj().isSysMaintenance() == true) {
            return "";
        }

        return SysGetRemoteMYSQL(SQL);
    }
/////////////////////////////////
    ///// Restore DB need the following
    ////  SystemStop
    ////  SystemCleanDBData
    ////  SystemUploadDBData
    ///// Restore DB need the following    

    public String SysRestoreDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);

        BackupRestoreInfo backupRestoreInfo = new BackupRestoreInfo();
        backupRestoreInfo.restoreDBDataInfo(this);

        BackupRestoreNN backupRestoreNN = new BackupRestoreNN();
        backupRestoreNN.restoreDBDataNN(this);

        BackupRestoreImp backupRestore = new BackupRestoreImp();
        retSatus = backupRestore.restoreDBData(this);

        if (retSatus == true) {
            serverObj.setSysMaintenance(true);
            serverObj.setTimerInit(false);
            serverObj.setTimerQueueCnt(0);
            serverObj.setTimerCnt(0);
        }

        return "SystemUploadDBData " + retSatus;
    }

    public String SysStop() {
        boolean retSatus = true;
        serverObj.setSysMaintenance(true);

        return "sysMaintenance " + retSatus;
    }

//
    public String SysStart() {
        boolean retSatus = true;
        serverObj.setSysMaintenance(false);
        serverObj.setTimerInit(false);
        serverObj.setTimerQueueCnt(0);
        serverObj.setTimerCnt(0);
        return "sysMaintenance " + retSatus;
    }

    // drop table
    public String SysDropDBData() {
        boolean retSatus = false;
        // make sure the system is stopped first
        retSatus = InfDropStockInfoDB();
        retSatus = NnDropNNdataDB();
        retSatus = SysDropStockDB();
        return "" + retSatus;
    }

    // drop table
    // create table
    public String SysCleanDBData() {
        boolean retSatus = false;

        serverObj.setSysMaintenance(true);
        retSatus = InfCleanStockInfoDB();
        retSatus = NnCleanNNdataDB();
        retSatus = SysCleanStockDB();
        return "" + retSatus;
    }

    ////////////////////////
    ////////////////////////////
    public ArrayList SysGetRemoveStockNameList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = StoGetAllRemoveStockNameList(length);

        return result;
    }

    public ArrayList SysGetDisableStockNameList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = StoGetAllDisableStockNameList(length);

        return result;
    }

//////////////////////////////helper function
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

    ///////////////////////////////
    public static boolean SysFilePut(String fileName, ArrayList msgWrite) {
        String fileN = ServiceAFweb.FileLocalPath + fileName;
        boolean ret = FileUtil.FileWriteTextArray(fileN, msgWrite);
        return ret;
    }

    public static boolean SysFileRead(String fileName, ArrayList msgWrite) {
        String fileN = ServiceAFweb.FileLocalPath + fileName;
        boolean ret = FileUtil.FileReadTextArray(fileN, msgWrite);
        return ret;
    }

    public WebStatus SysServerPing() {
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

    //////////////////////////////////////////////////
    // SystemService
    SystemService systemSrv = new SystemService();
    // SystemService
    //////////////////////////////////////////////////  

    public RequestObj SysSQLRequestSystem(RequestObj sqlObj) {
        RequestObj reqObj = systemSrv.SQLRequestSystem(this, sqlObj);
        return reqObj;
    }

    public void SysDataSourceSystem(DataSource dataSource, String URL) {
        systemSrv.setDataSource(dataSource, URL);
    }

    public int SysInitDBSystem() {
        return systemSrv.initStockDB();
    }

    public boolean SysCleanStockDB() {
        return systemSrv.cleanStockDB();
    }

    public boolean SysDropStockDB() {
        return systemSrv.restStockDB();
    }

    public ArrayList<String> SysGetAllIdSQLServ(String sql) {
        return systemSrv.getAllIdSQL(sql);
    }

    public int SysUpdateRemoteMYSQL(String sql) {
        return systemSrv.updateRemoteMYSQL(sql);
    }

    public String SysGetRemoteMYSQL(String sql) {
        return systemSrv.getRemoteMYSQL(sql);
    }

///////////////////////////////////////
    public int SysLockDeleteAll() {
        return systemSrv.deleteAllLock();
    }

    public String SysLockGetAllDBSQL(String sql) {
        return systemSrv.getAllLockDBSQL(sql);
    }

    public ArrayList SysGetAllLock() {
        ArrayList result = null;
        result = systemSrv.getAllLock();
        return result;
    }

    public int SysRenewLock(String name, int type) {
        return systemSrv.setRenewLock(name, type);
    }

    public AFLockObject SysGetLockName(String name, int type) {
        return systemSrv.getLockName(name, type);
    }

    public int SysSetLockNameSystem(String name, int type, long lockdatel, String comment) {
        return systemSrv.setLockName(name, type, lockdatel, comment);
    }

    public int SysRemoveNameLock(String name, int type) {
        return systemSrv.removeLock(name, type);

    }

    //////////////////
    public StringBuffer SysGetInternetScreenPage(String url) {
        return systemSrv.getInternetScreenPage(url);
    }

    public AFstockObj getRealTimeStockInternet(String NormalizeSymbol) {
        return systemSrv.getRealTimeStockInternet(NormalizeSymbol);

    }
    //////////////////////////////////////////////////
    //////////////////////////////////////////
    // StockService
    StockService stockSrv = new StockService();
    public static boolean stockFlag = true;

    //////////////////////////////////////////
    public RequestObj StoSQLRequestStock(RequestObj sqlObj) {
        RequestObj reqObj = stockSrv.SQLRequestStock(this, sqlObj);
        return reqObj;
    }

    public int StoUpdateSQLArrayList(ArrayList SQLTran) {
        if (stockFlag == true) {
            return stockSrv.updateSQLArrayList(this, SQLTran);
        }
        return 0;
    }
//////////////////////////////////////////

    public ArrayList<String> StoGetAllRemoveStockNameList(int length) {
        return stockSrv.getAllRemoveStockNameList(length);
    }

    public ArrayList<String> StoGetAllDisableStockNameList(int length) {
        return stockSrv.getAllDisableStockNameList(length);
    }

    public ArrayList<String> StoGetAllOpenStockNameServ() {
        if (stockFlag == true) {
            return stockSrv.getAllOpenStockNameArray(this);
        }
        return null;
    }
//    public String getAllStockDBSQL(String sql) {
//        if (stockFlag == true) {
//            return stockSrv.getAllStockDBSQL(sql);
//        }
//        return "";
//    }

    public AFstockObj StoGetStockObjBySym(String symbol) {
        if (stockFlag == true) {
            return stockSrv.getStockByName(this, symbol);
        }
        return null;
    }

    public AFstockObj StoGetStockObjByStockID(int stockID) {
        if (stockFlag == true) {
            return stockSrv.getStockBySockID(this, stockID);
        }
        return null;
    }
//    public AFstockObj SystemRealTimeStockByStockID(int stockId) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.RealTimeStockByStockID + "");
//            sqlObj.setReq("" + stockId);
//            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return null;
//            }
//            AFstockObj stockObj = null;
//
//            try {
//                stockObj = new ObjectMapper().readValue(output, AFstockObj.class
//                );
//            } catch (Exception ex) {
//                logger.info("> SystemRealTimeStockByStockID exception " + ex.getMessage());
//            }
//            return stockObj;
//        }
//        return getStockByStockIDServ(stockId);
//    }    

    public boolean StoCheckStockValidServ(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        if (stockFlag == true) {
            return stockSrv.checkStock(serviceAFWeb, NormalizeSymbol);
        }
        return false;
    }

    public int StoAddStockServ(String symbol) {
        if (stockFlag == true) {
            return stockSrv.addStock(this, symbol);
        }
        return 0;

    }

    public int StoDisableStockServ(String symbol) {
        if (stockFlag == true) {
            return stockSrv.disableStock(this, symbol);
        }
        return 0;
    }

    public int StoDeleteStockServ(AFstockObj stock) {
        if (stockFlag == true) {
            return stockSrv.deleteStock(this, stock);
        }
        return 0;
    }

    public int StoUpdateStockStatusDBServ(AFstockObj stock) {
        if (stockFlag == true) {
            return stockSrv.updateStockStatusDB(stock);
        }
        return 0;
    }

    //////////////////////////////////////////
    // StockService
    StockInfoService stockInfoSrv = new StockInfoService();
    public static boolean stockInfoFlag = true;
    //////////////////////////////////////////

    public RequestObj InfSQLRequestStockInfo(RequestObj sqlObj) {
        RequestObj reqObj = stockInfoSrv.SQLRequestStockInfo(this, sqlObj);
        return reqObj;
    }

    public ArrayList<AFstockInfo> InfGetStockInfo(AFstockObj stock, int length, Calendar dateNow) {
        if (stockInfoFlag == true) {
            if (stock == null) {
                return null;
            }
            if (stock.getSubstatus() == ConstantKey.INITIAL) {
                return null;
            }
            return stockInfoSrv.getStockInfo(stock.getSymbol(), length, dateNow);
        }
        return null;
    }

    // Heuoku cannot get the date of the first stockinfo????
    public ArrayList<AFstockInfo> InfGetStockInfo_workaround(AFstockObj stock, int length, Calendar dateNow) {
        if (stockInfoFlag == true) {
            if (stock == null) {
                return null;
            }
            if (stock.getSubstatus() == ConstantKey.INITIAL) {
                return null;
            }
            return stockInfoSrv.getStockInfo_workaround(stock.getSymbol(), length, dateNow);
        }
        return null;
    }

    /////recent day first and the old data last////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> InfGetStockHistorical(String symbol, int length) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.getStockHistorical(this, symbol, length);
        }
        return null;
    }

//    public ArrayList<AFstockInfo> getStockHistoricalRangeServ(String symbol, long start, long end) {
//        if (stockInfoFlag == true) {
//            return stockInfoSrv.getStockHistoricalRange(this, symbol, start, end);
//        }
//        return null;
//    }
//    public ArrayList<AFstockInfo> SystemStockHistoricalRange(String symbol, long start, long end) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            try {
//                RequestObj sqlObj = new RequestObj();
//                sqlObj.setCmd(ServiceAFweb.StockHistoricalRange + "");
//                sqlObj.setReq(symbol);
//                sqlObj.setReq1(start + "");
//                sqlObj.setReq2(end + "");
//
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//
//                ArrayList<AFstockInfo> trArray = null;
//                AFstockInfo[] arrayItem = new ObjectMapper().readValue(output, AFstockInfo[].class
//                );
//
//                List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
//                trArray = new ArrayList<AFstockInfo>(listItem);
//                return trArray;
//
//            } catch (Exception ex) {
//                logger.info("> SystemStockHistoricalRange exception " + ex.getMessage());
//            }
//            return null;
//        }
//
//        return getStockHistoricalRangeServ(symbol, start, end);
//    }    
    public int InfUpdateStockInfoTransaction(StockInfoTranObj stockInfoTran) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.updateStockInfoTransaction(this, stockInfoTran);
        }
        return 0;
    }

    public int InfRemoveStockInfo(String symbol) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.removeStockInfo(this, symbol);
        }
        return 0;
    }

    public int InfUpdateAllStockInfo() {
        if (stockInfoFlag == true) {
            return stockInfoSrv.updateAllStockInfo(this);
        }
        return 0;
    }

    public int InfUpdateSQLStockInfoArrayList(ArrayList SQLTran) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.updateSQLStockInfoArrayList(this, SQLTran);
        }
        return 0;
    }

    public ArrayList<AFstockInfo> InfGetAllStockInfoDBSQLArray(String sql) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.getAllStockInfoDBSQLArray(this, sql);
        }
        return null;
    }

    public String InfGetAllStockInfoDBSQL(String sql) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.getAllStockInfoDBSQL(this, sql);
        }
        return "";
    }

    public ArrayList<String> InfGetAllIdStockInfoSQL(String sql) {
        if (stockInfoFlag == true) {
            return stockInfoSrv.getAllIdStockInfoSQL(sql);
        }
        return null;
    }

    public boolean InfDropStockInfoDB() {
        if (stockInfoFlag == true) {
            return stockInfoSrv.restStockInfoDB(this);
        }
        return false;
    }

    public boolean InfCleanStockInfoDB() {
        if (stockInfoFlag == true) {
            return stockInfoSrv.cleanStockInfoDB(this);
        }
        return false;
    }

    public int InfInitStockInfoDB() {
        if (stockInfoFlag == true) {
            return stockInfoSrv.initStockInfoDB(this);
        }
        return 0;
    }

    public void InfSetDataSource(DataSource dataSource, String URL) {
        if (stockInfoFlag == true) {
            stockInfoSrv.setStockInfoDataSource(dataSource, URL);
        }

    }
    //////////////////////////////////////////////////
    // NNService
    NNetService nnSrv = new NNetService();
    public static boolean nnFlag = true;

    //////////////////////////////////////////////////    
    public RequestObj NnSQLRequestNN(RequestObj sqlObj) {
        RequestObj reqObj = nnSrv.SQLRequestNN(this, sqlObj);
        return reqObj;
    }

    public ArrayList<AFneuralNetData> NnNeuralNetDataObjSystem(String BPnameTR) {
        return NnGetNeuralNetDataObj(BPnameTR, 0);
    }

    public ArrayList<AFneuralNetData> NnGetNeuralNetDataObj(String name, int length) {
        return nnSrv.getNeuralNetDataObj(name, length);
    }
//    public ArrayList<AFneuralNetData> SystemNeuralNetDataObj(String BPnameTR) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.NeuralNetDataObj + "");
//            String st;
//            try {
//                sqlObj.setReq(BPnameTR + "");
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//                ArrayList<AFneuralNetData> trArray = null;
//
//                AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class
//                );
//                List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
//                trArray = new ArrayList<AFneuralNetData>(listItem);
//                return trArray;
//            } catch (Exception ex) {
//                logger.info("> SystemNeuralNetDataObj exception " + ex.getMessage());
//            }
//            return null;
//        }
//        return getNeuralNetDataObj(BPnameTR, 0);
//    }

    public ArrayList<AFneuralNetData> NnGetNeuralNetDataObjByStockId(String name, String refname, int stockId, long updatedatel) {
        return nnSrv.getNeuralNetDataObjByStockId(name, refname, stockId, updatedatel);
    }

    public int NnDeleteNeuralNetDataByBPname(String name) {
        return nnSrv.deleteNeuralNetDataByBPname(name);
    }

    public int NnDeleteNeuralNet1(String name) {
        return nnSrv.deleteNeuralNet1(name);
    }

    public int NnUpdateNeuralNetStatus1(String name, int status, int type) {
        return nnSrv.updateNeuralNetStatus1(name, status, type);
    }

    public int NnUpdateNeuralNetDataObject(String name, String sym, int stockId, NNInputDataObj objData) {
        return nnSrv.updateNeuralNetDataObject(name, sym, stockId, objData);
    }

    public int NnUpdateNeuralNetRef0(String name, ReferNameData refnameData) {
        return nnSrv.updateNeuralNetRef0(name, refnameData);
    }

    public int NnCreateNeuralNetObj1(String name, String weight) {
        return nnSrv.setCreateNeuralNetObj1(name, weight);
    }

    public int NnUpdateNeuralNetRef1(String name, ReferNameData refnameData) {
        return nnSrv.updateNeuralNetRef1(name, refnameData);
    }

    public int NnDeleteNeuralNetDataObjById(int id) {
        return nnSrv.deleteNeuralNetDataObjById(id);
    }

    public void NnfileNNInputOutObjListServ(ArrayList<NNInputDataObj> inputList, String symbol, int stockId, String filename) {
        if (nnFlag == true) {
            nnSrv.fileNNInputOutObjList(this, inputList, symbol, stockId, filename);
        }
    }

    public void NnDataSourceNNnet(DataSource dataSource, String URL) {
        if (nnFlag == true) {
            nnSrv.setNNDataDataSource(dataSource, URL);
        }
    }

    public int NnInitDBNNet() {
        if (nnFlag == true) {
            return nnSrv.initNNetDataDB(this);
        }
        return 0;
    }

    public int NnUpdateSQLArrayListServ(ArrayList SQLTran) {
        if (nnFlag == true) {
            return nnSrv.updateSQLNNArrayList(this, SQLTran);
        }
        return 0;
    }

    public boolean NnDropNNdataDB() {
        if (nnFlag == true) {
            return nnSrv.restNNdataDB(this);
        }
        return false;
    }

    public boolean NnCleanNNdataDB() {
        if (nnFlag == true) {
            return nnSrv.cleanNNdataDB(this);
        }
        return false;
    }

    ///////////////////////////////////////
    //////////////////////////////////////////////////
    // CustAccService
    CustAccService custAccSrv = new CustAccService();
    public static boolean custAccFlag = true;

    //////////////////////////////////////////////////
    public RequestObj AccSQLRequestCustAcc(RequestObj sqlObj) {
        RequestObj reqObj = custAccSrv.SQLRequestCustAcc(this, sqlObj);
        return reqObj;
    }

    public void AccDataSource(DataSource dataSource, String URL) {
        custAccSrv.setAccountDataSource(dataSource, URL);
    }

    public CustomerObj getCustomerPasswordNull(String UserName) {
        return custAccSrv.getCustomerPasswordNull(UserName);
    }

    public int setCustStatusPaymentBalance(String UserName,
            int status, float payment, float balance) {
        return custAccSrv.setCustStatusPaymentBalance(UserName, status, payment, balance);
    }

    public int updateTransactionOrderSystem(ArrayList transSQL) {
        return custAccSrv.updateTransactionOrder(transSQL);
    }
//    public int SystemuUpdateTransactionOrder(ArrayList<String> transSQL) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.UpdateTransactionOrder + "");
//            String st;
//            try {
//                st = new ObjectMapper().writeValueAsString(transSQL);
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
//                logger.info("> SystemuUpdateTransactionOrder exception " + ex.getMessage());
//            }
//            return 0;
//        }
//        return custAccSrv.updateTransactionOrder(transSQL);
//    }

    public int SysClearAccountStockTranByAccountIDSystem(AccountObj accountObj, int stockID, String trName) {
        return custAccSrv.clearAccountStockTranByAccountID(accountObj, stockID, trName);
    }
//    public int SystemAccountStockClrTranByAccountID(AccountObj accountObj, int stockId, String trName) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//
//        if (checkCallRemoteMysql() == true) {
//            try {
//                RequestObj sqlObj = new RequestObj();
//                sqlObj.setCmd(ServiceAFweb.AccountStockClrTranByAccountID + "");
//
//                String st = new ObjectMapper().writeValueAsString(accountObj);
//                sqlObj.setReq(st);
//                sqlObj.setReq1(stockId + "");
//                sqlObj.setReq2(trName);
//
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return 0;
//
//                }
//
//                int result = new ObjectMapper().readValue(output, Integer.class
//                );
//                return result;
//
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockClrTranByAccountID exception " + ex.getMessage());
//            }
//            return 0;
//        }
//
//        return custAccSrv.clearAccountStockTranByAccountID(accountObj, stockId, trName.toUpperCase());
//
//    }

    public boolean AccCheckTRListByStockID(String StockID) {
        return custAccSrv.checkTRListByStockID(StockID);
    }

    public ArrayList<TradingRuleObj> getAccountStockTRListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
        return custAccSrv.getAccountStockTRListByAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt, stockidsymbol);
    }

    ////////////////////////////////
    // Security - Do not allow outside ServiceAFweb to access this function
    ////////////////////////////////
    public AccountObj AccGetAccountByAccountID(int accountID) {
        return custAccSrv.getAccountByAccountID(accountID);
    }

    public AccountObj AccGetAccountByType(String UserName, String Password, int accType) {
        return custAccSrv.getAccountByType(UserName, Password, accType);
    }

    public ArrayList AccGetAccountStockNameListServ(int accountId) {
        return custAccSrv.getAccountStockNameList(accountId);
    }
//    public ArrayList<String> SystemAccountStockNameList(int accountId) {
//        ArrayList<String> NameList = new ArrayList();
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AccountStockNameList + "");
//            sqlObj.setReq("" + accountId);
//            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return NameList;
//            }
//            try {
//                NameList = new ObjectMapper().readValue(output, ArrayList.class
//                );
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockNameList exception " + ex.getMessage());
//            }
//            return NameList;
//        }
//        return custAccSrv.getAccountStockNameList(accountId);
//    }    

    public int AccUpdateAccounStockPref(TradingRuleObj trObj, float perf) {
        return custAccSrv.updateAccounStockPref(trObj, perf);
    }

    public int AccRemoveAccountStock(AccountObj accountObj, int StockID) {
        return custAccSrv.removeAccountStock(accountObj, StockID);
    }

    public CustomerObj AccGetCustomerByAccount(AccountObj accountObj) {
        return custAccSrv.getCustomerByAccount(accountObj);
    }

    public ArrayList<CustomerObj> AccGetCustomerByType(int type) {
        return custAccSrv.getCustomerByType(type);
    }

    public AccountObj AccGetAccountObjByAccountIDServ(int accountID) {
        return custAccSrv.getAccountObjByAccountID(accountID);
    }

//    public AccountObj SystemAccountObjByAccountID(int accountId) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AccountObjByAccountID + "");
//            sqlObj.setReq("" + accountId);
//            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return null;
//            }
//            AccountObj accountObj = null;
//
//            try {
//                accountObj = new ObjectMapper().readValue(output, AccountObj.class
//                );
//            } catch (Exception ex) {
//                logger.info("> SystemAccountObjByAccountID exception " + ex.getMessage());
//            }
//            return accountObj;
//        }
//        return custAccSrv.getAccountObjByAccountID(accountId);
//    }
    public int AccUpdateAccountPortfolio(String accountName, String portfolio) {
        return custAccSrv.updateAccountPortfolio(accountName, portfolio);
    }

    public int AccUpdateAccountStatusByAccountID(int accountID,
            int substatus, float investment, float balance, float servicefee) {
        return custAccSrv.updateAccountStatusByAccountID(accountID, substatus, investment, balance, servicefee);
    }

    public ArrayList<AccountObj> getAccountListByCustomerId(int custId) {
        return custAccSrv.getAccountListByCustomerId(custId);
    }

    public TradingRuleObj getAccountStockIDByTRStockIDSystem(int accountID, int stockID, String trName) {
        // not sure why it does not work in Open shift but work local
        return custAccSrv.getAccountStockIDByTRStockID(accountID, stockID, trName);
    }
//    public TradingRuleObj SystemAccountStockIDByTRname(int accountID, int stockID, String trName) {
//
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        if (checkCallRemoteMysql() == true) {
//            try {
//                RequestObj sqlObj = new RequestObj();
//                sqlObj.setCmd(ServiceAFweb.AccountStockIDByTRname + "");
//
//                sqlObj.setReq(accountID + "");
//                sqlObj.setReq1(stockID + "");
//                sqlObj.setReq2(trName);
//
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//
//                }
//
//                TradingRuleObj ret = new ObjectMapper().readValue(output, TradingRuleObj.class
//                );
//                return ret;
//
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockIDByTRname exception " + ex.getMessage());
//            }
//        }
//        return custAccSrv.getAccountStockIDByTRStockID(accountID, stockID, trName);
//    }

    public ArrayList<TradingRuleObj> getAccountStockTRListByAccIdSymbolSystem(int accountId, String symbol) {
        AFstockObj stock = StoGetStockObjBySym(symbol);
        return getAccountStockTRListByAccIdStockIdSystem(accountId, stock.getId());
    }

    public ArrayList<TradingRuleObj> getAccountStockTRListByAccIdStockIdSystem(int accountId, int stockId) {
        return custAccSrv.getAccountStockTRListByAccIdStockId(accountId, stockId);
    }

//    public ArrayList<TradingRuleObj> SystemAccountStockListByAccountID(int accountId, String symbol) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        if (checkCallRemoteMysql() == true) {
//            try {
//                RequestObj sqlObj = new RequestObj();
//                sqlObj.setCmd(ServiceAFweb.AccountStockListByAccountID + "");
//
//                sqlObj.setReq(accountId + "");
//                sqlObj.setReq1(symbol);
//
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//                ArrayList<TradingRuleObj> trArray = null;
//
//                TradingRuleObj[] arrayItem = new ObjectMapper().readValue(output, TradingRuleObj[].class
//                );
//                List<TradingRuleObj> listItem = Arrays.<TradingRuleObj>asList(arrayItem);
//                trArray = new ArrayList<TradingRuleObj>(listItem);
//                return trArray;
//
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockListByAccountID exception " + ex.getMessage());
//            }
//            return null;
//        }
//        AFstockObj stock = getStockBySymServ(symbol);
//        int stockID = stock.getId();
//        return custAccSrv.getAccountStockTRListByAccountID(accountId, stockID);
//    }
//    public ArrayList<TradingRuleObj> SystemAccountStockListByAccountIDStockID(int accountId, int stockId) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//
//        if (checkCallRemoteMysql() == true) {
//            try {
//                RequestObj sqlObj = new RequestObj();
//                sqlObj.setCmd(ServiceAFweb.AccountStockListByAccountIDStockID + "");
//
//                sqlObj.setReq(accountId + "");
//                sqlObj.setReq1(stockId + "");
//
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//                ArrayList<TradingRuleObj> trArray = null;
//
//                TradingRuleObj[] arrayItem = new ObjectMapper().readValue(output, TradingRuleObj[].class
//                );
//                List<TradingRuleObj> listItem = Arrays.<TradingRuleObj>asList(arrayItem);
//                trArray = new ArrayList<TradingRuleObj>(listItem);
//                return trArray;
//
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockListByAccountIDStockID exception " + ex.getMessage());
//            }
//            return null;
//        }
//
//        return custAccSrv.getAccountStockTRListByAccountID(accountId, stockId);
//    }
    //  entrydatel desc recent transaction first    
    public ArrayList<TransationOrderObj> getAccountStockTransListSystem(int accountID, int stockID, String trName, int length) {
        return custAccSrv.getAccountStockTransList(accountID, stockID, trName, length);
    }
//    //  entrydatel desc recent transaction first
//    public ArrayList<TransationOrderObj> SystemAccountStockTransList(int accountID, int stockID, String trName, int length) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AccountStockTransList + "");
//            String st;
//            try {
//                sqlObj.setReq(accountID + "");
//                sqlObj.setReq1(stockID + "");
//                sqlObj.setReq2(trName);
//                sqlObj.setReq3(length + "");
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//                ArrayList<TransationOrderObj> trArray = null;
//
//                TransationOrderObj[] arrayItem = new ObjectMapper().readValue(output, TransationOrderObj[].class
//                );
//                List<TransationOrderObj> listItem = Arrays.<TransationOrderObj>asList(arrayItem);
//                trArray = new ArrayList<TransationOrderObj>(listItem);
//                return trArray;
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockTransList exception " + ex.getMessage());
//            }
//            return null;
//        }
//        return custAccSrv.getAccountStockTransList(accountID, stockID, trName, length);
//    }

    public ArrayList getAllAccountStockNameListExceptionAdmin(int accountId) {
        return custAccSrv.getAllAccountStockNameListExceptionAdmin(accountId);

    }
//    public ArrayList SystemAllAccountStockNameListExceptionAdmin(int accountId) {
//        ArrayList<String> NameList = new ArrayList();
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllAccountStockNameListExceptionAdmin + "");
//            sqlObj.setReq(accountId + "");
//            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return NameList;
//
//            }
//
//            try {
//                NameList = new ObjectMapper().readValue(output, ArrayList.class
//                );
//            } catch (Exception ex) {
//                logger.info("> AllAccountStockNameListExceptionAdmin exception " + ex.getMessage());
//            }
//            return NameList;
//        }
//        return custAccSrv.getAllAccountStockNameListExceptionAdmin(accountId);
//
//    }

    public ArrayList getAccountListServ(String EmailUserName, String Password) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountList(this, EmailUserName, Password);
        }
        return null;
    }

    public AccountObj getAccountByCustomerAccountIDServ(String EmailUserName, String Password, String AccountIDSt) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountByCustomerAccountID(this, EmailUserName, Password, AccountIDSt);
        }
        return null;
    }

    public float getAccountStockBalanceServ(TradingRuleObj trObj) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountStockRealTimeBalance(this, trObj);
        }
        return -9999;
    }

    public int AccAddAccountStockByAccountServ(AccountObj accountObj, String symbol) {
        if (custAccFlag == true) {
            return custAccSrv.addAccountStockByAccount(this, accountObj, symbol);
        }
        return 0;
    }

    public int AccAddAccountStockByCustAccServ(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (custAccFlag == true) {
            return custAccSrv.addAccountStockByCustAcc(this, EmailUserName, Password, AccountIDSt, symbol);
        }
        return 0;
    }

    public int removeAccountStockByUserNameAccIdServ(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (custAccFlag == true) {
            return custAccSrv.removeAccountStockByUserNameAccId(this, EmailUserName, Password, AccountIDSt, symbol);
        }
        return 0;
    }
//    //ConstantKey.NOTEXISTED

    public int removeAccountStockSymbolServ(AccountObj accountObj, String symbol) {
        if (custAccFlag == true) {
            return custAccSrv.removeAccountStockSymbol(this, accountObj, symbol);
        }
        return 0;
    }

    public TradingRuleObj getAccountStockTRByTRnameServ(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountStockTRByTRname(this, EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        }
        return null;
    }

    public ArrayList<TransationOrderObj> getAccountStockTRTranListByAccountIDServ(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountStockTRTranListByAccountID(this, EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
        }
        return null;
    }

    public int getAccountStockTRListHistoryDisplayProcessServ(ArrayList<StockTRHistoryObj> trObjList, ArrayList<String> writeArray, ArrayList<String> displayArray) {
        if (custAccFlag == true) {
            return custAccSrv.getAccountStockTRListHistoryDisplayProcess(trObjList, writeArray, displayArray);
        }
        return 0;
    }

//    //only on type=" + CustomerObj.INT_CLIENT_BASIC_USER;
    public ArrayList getExpiredCustomerListServ(int length) {
        if (custAccFlag == true) {
            return custAccSrv.getExpiredCustomerList(this, length);
        }
        return null;
    }
//    // need ConstantKey.DISABLE status beofore remove customer

    public int removeCustomerServ(String EmailUserName) {
        if (custAccFlag == true) {
            return custAccSrv.removeCustomer(this, EmailUserName);
        }
        return 0;
    }

    public boolean SystemFundResetGlobalServ() {
        if (custAccFlag == true) {
            return custAccSrv.SystemFundResetGlobal(this);
        }
        return false;
    }

    public boolean SystemFundSelectBestServ() {
        if (custAccFlag == true) {
            return custAccSrv.SystemFundSelectBest(this);
        }
        return false;
    }

    public boolean SystemFundPocessAddRemoveServ() {
        if (custAccFlag == true) {
            return custAccSrv.SystemFundPocessAddRemove(this);
        }
        return false;
    }

    public String getAccountStockTRListHistoryChartServ(ArrayList<StockTRHistoryObj> thObjListMain, String stockidsymbol, String trname, String pathSt) {
        if (custAccFlag == true) {
            ChartService chartSrv = new ChartService();
            return chartSrv.getAccountStockTRListHistoryChartToFile(this, thObjListMain, stockidsymbol, trname, pathSt);
        }
        return "";
    }

    public ArrayList<PerformanceObj> getAccountStockPerfListSystem(int accountID, int stockID, String trName, int length) {
        return custAccSrv.getAccountStockPerfList(accountID, stockID, trName, length);
    }
    //    public ArrayList<PerformanceObj> SystemAccountStockPerfList(int accountID, int stockID, String trName, int length) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AccountStockPerfList + "");
//            String st;
//            try {
//                sqlObj.setReq(accountID + "");
//                sqlObj.setReq1(stockID + "");
//                sqlObj.setReq2(trName);
//                sqlObj.setReq3(length + "");
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return null;
//                }
//                if (output.equals(ConstantKey.nullSt)) {
//                    return null;
//                }
//                ArrayList<PerformanceObj> trArray = null;
//
//                PerformanceObj[] arrayItem = new ObjectMapper().readValue(output, PerformanceObj[].class
//                );
//                List<PerformanceObj> listItem = Arrays.<PerformanceObj>asList(arrayItem);
//                trArray = new ArrayList<PerformanceObj>(listItem);
//                return trArray;
//            } catch (Exception ex) {
//                logger.info("> SystemAccountStockPerfList exception " + ex.getMessage());
//            }
//            return null;
//        }
//        return custAccSrv.getAccountStockPerfList(accountID, stockID, trName, length);
//    }

    public ArrayList getAllOpenAccountIDSystem() {
        return custAccSrv.getAllOpenAccountID();
    }
//    public ArrayList SystemAllOpenAccountIDList() {
//        ArrayList<String> NameList = new ArrayList();
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllOpenAccountIDList + "");
//
//            RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            if (output == null) {
//                return NameList;
//
//            }
//
//            try {
//                NameList = new ObjectMapper().readValue(output, ArrayList.class);
//            } catch (Exception ex) {
//                logger.info("> SystemAllOpenAccountIDList exception " + ex.getMessage());
//            }
//            return NameList;
//        }
//        return custAccSrv.getAllOpenAccountID();
//    }    
//    

    public CommObj getCommObjByID(int commID) {
        return custAccSrv.getCommObjByID(commID);
    }

    public CommData getCommDataObj(CommObj commObj) {
        return custAccSrv.getCommDataObj(commObj);
    }

    ///////////////////////////////////////
    public CustomerObj getCustomerIgnoreMaintenance(String EmailUserName, String Password) {

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        return getCustomerPassword(UserName, Password);
    }

    public CustomerObj getCustomerPassword(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        CustomerObj custObj = custAccSrv.getCustomerPassword(UserName, Password);
        if (custObj != null) {
            if (custObj.getStatus() != ConstantKey.OPEN) {
                custObj.setUsername("");
                custObj.setPassword("");
            }
        }
        return custObj;
    }
//

    public CustomerObj getCustomerObjByName(String name) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        ArrayList<CustomerObj> custList = custAccSrv.getCustomerObjByNameList(name);
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
        result = custAccSrv.getCustomerByAccoutObj(accObj);
        return result;
    }

    //////////////////////////////////////////////////
//    public String SystemSQLquery(String SQL) {
////        if (getServerObj().isSysMaintenance() == true) {
////            return "";
////        }
//        if (checkCallRemoteMysql() == true) {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllSQLquery + "");
//
//            try {
//                sqlObj.setReq(SQL);
//                RequestObj sqlObjresp = SystemSQLRequest(sqlObj);
//                String output = sqlObjresp.getResp();
//                if (output == null) {
//                    return "";
//                }
//
//                return output;
//            } catch (Exception ex) {
//                logger.info("> SystemSQLquery exception " + ex.getMessage());
//            }
//            return "";
//        }
//        return getAccountImp().getAllSQLquery(SQL);
//    }
    public int AccAddTransactionOrder(AccountObj accountObj, AFstockObj stock, String trName, int tranSignal, Calendar tranDate) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        AccountTranImp accountTran = new AccountTranImp();
        return accountTran.AddTransactionOrder(this, accountObj, stock, trName, tranSignal, tranDate, true);
    }

//////////
    public int updateAccountStockSignalList(ArrayList<TradingRuleObj> TRList) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return custAccSrv.updateAccountStockSignal(TRList);

    }

    public int updateAccountStockSignal(TRObj stockTRObj) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return custAccSrv.updateAccountStockSignal(stockTRObj.getTrlist());

    }

    public int systemRemoveAllEmail() {
        custAccSrv.removeCommByType(CKey.ADMIN_USERNAME, null, ConstantKey.INT_TYPE_COM_EMAIL);
        return 1;
    }
////////////////////////////////////////////////////
    // Util
    public static HashMap<String, ArrayList> stockInputMap = null;
    public static HashMap<String, ArrayList> stockInputMap_1 = null;

    public static boolean SysCreateStaticStockHistoryServ(ServiceAFweb serviceAFWeb, String symbolL[], String fileName, String tagName) {
        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            SysCreateStaticStockHistory(serviceAFWeb, symbolL, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.SysCompress(inputListRawSt);

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

    private static void SysCreateStaticStockHistory(ServiceAFweb serviceAFWeb, String symbolL[], HashMap<String, ArrayList> stockInputMap) {
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

    public static ArrayList<AFstockInfo> SysGetAllStaticStockHistoryServ(String symbol) {
        if (stockInputMap == null) {
            stockInputMap = nnAllStock_src.AllStockHistoryStaticCodeInit(stockInputMap);
        }
        ArrayList<AFstockInfo> stockInfoList = SysGetAllStockHistoryStaticCode(symbol, stockInputMap);
        if (stockInfoList != null) {
            return stockInfoList;
        }
        return SysGetAllStockHistory(symbol);
    }

    private static ArrayList<AFstockInfo> SysGetAllStockHistory(String symbol) {
        if (stockInputMap_1 == null) {
            stockInputMap_1 = nnAllStock_1_src.AllStockHistoryStaticCodeInit(stockInputMap_1);
        }
        return SysGetAllStockHistoryStaticCode(symbol, stockInputMap_1);

    }

    private static ArrayList<AFstockInfo> SysGetAllStockHistoryStaticCode(String symbol,
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
    public static boolean SysUpdateStockFileServ(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        ArrayList inputArray = new ArrayList();
        String nnFileName = ServiceAFweb.FileLocalPath + NormalizeSymbol + ".csv";
        if (FileUtil.FileTest(nnFileName) == false) {
            logger.info("updateStockFile not found " + nnFileName);
            return false;
        }

        serviceAFWeb.InfRemoveStockInfo(NormalizeSymbol);

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
                    int retTran = serviceAFWeb.InfUpdateStockInfoTransaction(stockInfoTran);
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
            serviceAFWeb.InfUpdateStockInfoTransaction(stockInfoTran);
        }
        return true;
    }
//////////////////////////////////////////    

//    public String SystemRestoreNNonlyDBData() {
//        boolean retSatus = false;
//
//        serverObj.setSysMaintenance(true);
//        BackupRestoreNN backupRestore = new BackupRestoreNN();
//        retSatus = backupRestore.restoreNNonlyDBData(this);
//        if (retSatus == true) {
//            serverObj.setSysMaintenance(true);
//            serverObj.setTimerInit(false);
//            serverObj.setTimerQueueCnt(0);
//            serverObj.setTimerCnt(0);
//        }
//
//        return "SystemUploadDBData " + retSatus;
//    }
    public static String getSQLLengh(String sql, int length) {
        //https://www.petefreitag.com/item/59.cfm
        //SELECT TOP 10 column FROM table - Microsoft SQL Server
        //SELECT column FROM table LIMIT 10 - PostgreSQL and MySQL
        //SELECT column FROM table WHERE ROWNUM <= 10 - Oracle
        if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL)
                || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL)
                || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_1_MYSQL)
                || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
            if (length != 0) {
                if (length == 1) {
                    sql += " limit 1 ";
                } else {
                    sql += " limit " + length + " ";
                }
            }
        }

//        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
//            if (length != 0) {
//                if (length == 1) {
//                    sql = sql.replace("select ", "select top 1 ");
//                } else {
//                    sql = sql.replace("select ", "select top " + length + " ");
//                }
//            }
//        }
        return sql;
    }

////////////////////////////////
//    @Autowired
//    public void setDataSource(DataSource dataSource) {
//        //testing
//        WebAppConfig webConfig = new WebAppConfig();
//        dataSource = webConfig.dataSource();
//        //testing        
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//        this.dataSource = dataSource;
//    }
    /**
     * @return the accountImp
     */
//    public AccountImp getAccountImp() {
//        return accountImp;
//    }
//
//    /**
//     * @param accountImp the accountImp to set
//     */
//    public void setAccountImp(AccountImp accountImp) {
//        this.accountImp = accountImp;
//    }
    public static String SysCompress(String str) {
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

    public static String SysDecompress(String str) {
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

//////////////////////////////
}
