/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.nnprocess.*;
import com.afweb.model.*;
import com.afweb.account.*;
import com.afweb.chart.ChartService;
import com.afweb.mail.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.signal.*;
import com.afweb.stock.*;
import com.afweb.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;

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
import java.util.Collections;
import java.util.Date;
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

    private StockImp stockImp = new StockImp();
    private AccountImp accountImp = new AccountImp();
    private AccountProcess accountProcessImp = new AccountProcess();
    private ServiceAFwebREST serviceAFwebREST = new ServiceAFwebREST();

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

    public static String allStock[] = {"NEM", "SE", "FB", "GLD", "MSFT", "T.TO"};
    public static String primaryStock[] = {"HOU.TO", "IWM", "AMZN", "SPY", "DIA", "QQQ", "HOD.TO", "FAS", "FAZ", "XIU.TO", "AAPL", "RY.TO"};

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
            FileLocalPath = CKey.FileLocalPathTemp;
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
                displayStr += "\r\n" + (">>>>> System mydebugtestflag:" + ServiceAFweb.mydebugtestflag);
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
                    setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, serverObj.getSrvProjName());

                    //try 2 times
                    getAccountProcessImp().ProcessAdminAccount(this);
                    getAccountProcessImp().ProcessAdminAccount(this);

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

//    public static ArrayList writeArrayNeuralNet = new ArrayList();
//    public boolean systemNNFlag = false;
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
                        SystemFundMgr();
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
                        SystemPocessFundMgr();
                    }

///////////////////////////////////////////////////////////////////////////////////
                    AFprocessDebug();
                    processNeuralNetTrain();

///////////////////////////////////////////////////////////////////////////////////
                    logger.info(">>>>>>>> DEBUG end >>>>>>>>>");
                }
            }
        }
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        String LockName = null;
        //////           
        if (cmd.length() > 0) {
            if (cmd.equals("adminsignal")) {
//                    TRprocessImp.ProcessAdminSignalTrading(this);
                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
            } else if (cmd.equals("updatestock")) {
                TRprocessImp.UpdateAllStock(this);
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
                    TRprocessImp.UpdateAllStock(this);
                    TRprocessImp.ProcessAdminSignalTrading(this);
                    getAccountProcessImp().ProcessAllAccountTradingSignal(this);
                    TRprocessImp.UpdateAllStock(this);
                    getAccountProcessImp().ProcessSystemMaintance(this);
                }
            }

            /////// main execution
            AFwebExec();
            ///////
        } catch (Exception ex) {
            logger.info("> processTimer Exception" + ex.getMessage());
        }
        removeNameLock(LockName, ConstantKey.SRV_LOCKTYPE);
    }

    void AFwebExec() {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        ////////////
        if (((getServerObj().getProcessTimerCnt() % 29) == 0) || (getServerObj().getProcessTimerCnt() == 1)) {
            long result = setRenewLock(serverLockName, ConstantKey.SRV_LOCKTYPE);
            if (result == 0) {
                Calendar dateNow1 = TimeConvertion.getCurrentCalendar();
                long lockDateValue1 = dateNow1.getTimeInMillis();
                setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue1, serverObj.getSrvProjName());
            }
        }

        //2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
        if ((getServerObj().getProcessTimerCnt() % 11) == 0) {
            // add or remove stock in Mutual fund account based on all stocks in the system
            System.gc();
            getAccountProcessImp().ProcessFundAccount(this);
            getAccountProcessImp().ProcessSystemMaintance(this);

        } else if ((getServerObj().getProcessTimerCnt() % 7) == 0) {
            TRprocessImp.UpdateAllStock(this);
            AFprocessNeuralNet();
//            getAccountProcessImp().ProcessAdminAccount(this);

        } else if ((getServerObj().getProcessTimerCnt() % 5) == 0) {
            TRprocessImp.UpdateAllStock(this);
            TRprocessImp.ProcessAdminSignalTrading(this);
            getAccountProcessImp().ProcessAdminAccount(this);
            //
            if (CKey.PROXY == false) {
                if (ServiceAFweb.processEmailFlag == true) {
                    EmailProcess eProcess = new EmailProcess();
                    eProcess.ProcessEmailAccount(this);
                }
            }

        } else if ((getServerObj().getProcessTimerCnt() % 3) == 0) {
            TRprocessImp.UpdateAllStock(this);
            getAccountProcessImp().ProcessAllAccountTradingSignal(this);
            getAccountProcessImp().ProcessAdminAccount(this);

        } else if ((getServerObj().getProcessTimerCnt() % 2) == 0) {

        } else {

        }
    }

    public static String debugSymbol = "HOU.TO";

    public static boolean forceNNReadFileflag = false;
    public static boolean flagNNLearningSignal = false;
    public static boolean flagNN3LearningTrend = false;
    public static boolean flagNNReLearning = false;
    public static boolean processNNSignalAdmin = false;
    public static boolean processRestinputflag = false;
    public static boolean processRestAllStockflag = false;

    public static boolean processEmailFlag = false;
    public static boolean processNeuralNetFlag = false;
    public static boolean nn1testflag = false;
    public static boolean nn2testflag = false;
    public static boolean nn3testflag = false;
    public static int cntNN = 0;

    public void AFprocessNeuralNet() {
        if (processNeuralNetFlag == true) {
            cntNN++;
            TradingNNprocess NNProcessImp = new TradingNNprocess();
            NN1ProcessByTrend nn1trend = new NN1ProcessByTrend();
            NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();
            NN2ProcessByTrend nn2trend = new NN2ProcessByTrend();

            nn1testflag = true;
            nn2testflag = true;

            if (cntNN == 1) {
                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
                return;
            } else if (cntNN == 2) {
                nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(this);
                return;
            } else if (cntNN == 3) {
                nn1trend.ProcessTrainNeuralNetNN1ByTrend(this);
                return;
            } else if (cntNN == 4) {
                nn2trend.ProcessTrainNeuralNetNN2tByTrend(this);
                return;
            } else if (cntNN == 5) {
                NNProcessImp.ProcessReLearnInputNeuralNet(this);
                return;
            } else if (cntNN == 6) {
                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
                return;
            } else if (cntNN == 7) {
                nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(this);
                return;
            }

            cntNN = 0;
        }
    }

    public void processNeuralNetTrain() {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN1ProcessByTrend nn1trend = new NN1ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();
        NN2ProcessByTrend nn2trend = new NN2ProcessByTrend();

        TrandingSignalProcess.forceToGenerateNewNN = false;

        if (processNeuralNetFlag == true) {
            while (true) {
                AFprocessNeuralNet();
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
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN1);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 1 cycle " + k);
                    nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(this);
                    logger.info("> ProcessTrainNeuralNet NN 1 end... cycle " + k);

                } else if (nn2testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN2);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 2 cycle " + k);

                    nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(this);
                    logger.info("> ProcessTrainNeuralNet NN 2 end... cycle " + k);

                }
            }
////////////////////////////////////////////////////////////////////////////

            if (flagNN3LearningTrend == true) {
                exitflag = false;
                if (nn1testflag == true) {
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN30);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 30 cycle " + k);
                    nn1trend.ProcessTrainNeuralNetNN1ByTrend(this);
                    logger.info("> ProcessTrainNeuralNet NN 30 end... cycle " + k);
                } else if (nn2testflag == true) {
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(this, ConstantKey.TR_NN40);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 40 cycle " + k);
                    nn2trend.ProcessTrainNeuralNetNN2tByTrend(this);
                    logger.info("> ProcessTrainNeuralNet NN 40 end... cycle " + k);
                }
            }

////////////////////////////////////////////////////////////////////////////          
            if (flagNNReLearning == true) {
                exitflag = false;
                logger.info("> ProcessReLeanInput NN 1 cycle " + k);
                NNProcessImp.ProcessReLearnInputNeuralNet(this);
                logger.info("> ProcessReLeanInput end... cycle " + k);

            }
////////////////////////////////////////////////////////////////////////////

            if (processNNSignalAdmin == true) {
                exitflag = false;
                logger.info("> processNNSignalAdmin  cycle " + k);
                TRprocessImp.ProcessAdminSignalTrading(this);
                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
                TRprocessImp.UpdateAllStock(this);
                logger.info("> processNNSignalAdmin end... cycle " + k);
            }

////////////////////////////////////////////////////////////////////////////            
            if (processRestinputflag == true) {
                if (nn1testflag == true) {
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn1ProcBySig.processInputNeuralNet(this);
                    nn1ProcBySig.processAllStockInputNeuralNet(this);

                    nn1trend.processNN30InputNeuralNetTrend(this);
                    nn1trend.processAllNN30StockInputNeuralNetTrend(this);
                    return;
                } else if (nn2testflag == true) {
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn2ProcBySig.processNN2InputNeuralNet(this);
                    nn2ProcBySig.processAllNN2StockInputNeuralNet(this);

                    nn2trend.processNN40InputNeuralNetTrend(this);
                    nn2trend.processAllNN40StockInputNeuralNetTrend(this);
                    ///////////////////////////////

                    return;
                }
            }
////////////////////////////////////////////////////////////////////////////
            if (processRestAllStockflag == true) {
                exitflag = true;
                ///////////////////////////////   
                String symbolL[] = ServiceAFweb.primaryStock;
                nn1ProcBySig.AllStockHistoryCreatJava(this, symbolL, "nnAllStock", "NN_ALLSTOCK");

                String symbolLallSt[] = ServiceAFweb.allStock;
                nn1ProcBySig.AllStockHistoryCreatJava(this, symbolLallSt, "nnAllStock1", "NN_1ALLSTOCK");

                return;
            }
////////////////////////////////////////////////////////////////////////////
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
///////////////////////////////

    public static boolean mydebugtestflag = false;

    private void AFprocessDebug() {

        if (mydebugtestflag == true) {
            //set up run parm 
            // javamain localmysqlflag proxyflag mydebugtestflag
            // javamain localmysqlflag  mydebugtestflag

            // javamain localmysqlflag nn2testflag flagNNLearningSignal nndebugflag
            logger.info("Start mydebugtestflag.....");
            NN1ProcessByTrend nnStProcByTrend = new NN1ProcessByTrend();
            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();
            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            //select * FROM sampledb.neuralnetdata where name like '%NN2%';

            String symbol = "DIA";
            int trNN = ConstantKey.INT_TR_NN2;
            int TR_NN = trNN;
            String nnName = ConstantKey.TR_NN2;
            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;

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
//            }
//
//            try {
//                GmailSender sender = new GmailSender();
//                sender.setSender(UA_Str, PA_Str);
//                sender.addRecipient(UU_Str);
//                sender.setSubject("The subject1");
//                sender.setBody("The body1");
////            sender.addAttachment("TestFile.txt");
//                sender.send();
//            } catch (Exception ex) {
//                logger.info("> Exception ...." + ex.getMessage());
//            }
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
//              int ret = this.getAccountProcessImp().saveDBneuralnetProcess(this, "neuralnet");
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
//
//            }
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
//            symbol = "BCE.TO";
//            AccountObj accountAdminObj = getAdminObjFromCache();
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//            int retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2, symbol);
//
//            TRprocessImp.updateAdminTradingsignal(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminTransaction(this, accountAdminObj, symbol);
//            TRprocessImp.upateAdminPerformance(this, accountAdminObj, symbol);            
//            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
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
//            while (true) {
//                TRprocessImp.ProcessAdminSignalTrading(this);
//                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
//                logger.info("> Waiting 30 sec........");
//                try {
//                    Thread.sleep(30 * 1000);
//                } catch (InterruptedException ex) {
//                    Thread.currentThread().interrupt();
//                }
//            }
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

        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        ///// only acc reset
        boolean flagTran_TR_ACC = false;
        if (flagTran_TR_ACC == true) {
            SystemClearNNtranAllAcc();
        }

        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work        
        boolean flaginputStock = false;
        if (flaginputStock == true) {
            StockInternet.updateAllStockFile(this);
        }
        // need this only if yahoo get history stock does not work
        // need this only if yahoo get history stock does not work 

        boolean saveStockFileFlag = false;
        if (saveStockFileFlag == true) {
            ArrayList stockNameArray = getAllOpenStockNameArray();
            logger.info("updateRealTimeStock " + stockNameArray.size());
            for (int i = 0; i < stockNameArray.size(); i++) {
                String sym = (String) stockNameArray.get(i);
                ArrayList<String> writeArray = new ArrayList();
                int size1year = 5 * 52;
                ArrayList StockArray = getStockHistorical(sym, size1year * 4);
                if (StockArray == null) {
                    continue;
                }
                if (StockArray.size() == 0) {
                    continue;
                }
                String StFileName = FileLocalPath + sym + ".txt";
                logger.info("saveStockFile Size " + StockArray.size() + " " + StFileName);
                for (int j = 0; j < StockArray.size(); j++) {
                    try {
                        AFstockInfo obj = (AFstockInfo) StockArray.get(j);
                        String st = new ObjectMapper().writeValueAsString(obj);
                        writeArray.add(st);
                    } catch (JsonProcessingException ex) {
                    }
                }
                FileUtil.FileWriteTextArray(StFileName, writeArray);
            }
        }

        boolean dbhero2opflag = false;
        if (dbhero2opflag == true) {
            boolean prevOPSHIFT = CKey.OTHER_PHP1_MYSQL;

            CKey.OTHER_PHP1_MYSQL = false;
            ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_HERO_DBDB_PHP + CKey.WEBPOST_HERO_PHP);
            backupSystem();

            CKey.OTHER_PHP1_MYSQL = true;
            ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_OP_DB_PHP1 + CKey.WEBPOST_OP_PHP);
            restoreSystem();

            // restore original
            CKey.OTHER_PHP1_MYSQL = prevOPSHIFT;
        }

        /////other macd, mv, nn1, nn2
        boolean flagTran_TR_OTHER = false;
        if (flagTran_TR_OTHER == true) {
            SystemClearNNtran(ConstantKey.SIZE_TR);
        }
        boolean flagClearNN0Table = false;
        if (flagClearNN0Table == true) {
            this.getStockImp().deleteNeuralNet0Table();
        }

        boolean flagClearNN1Table = false;
        if (flagClearNN1Table == true) {
            this.getStockImp().deleteNeuralNet1Table();
        }

        boolean flagClearNNdataTable = false;
        if (flagClearNNdataTable == true) {
            this.getStockImp().deleteNeuralNetDataTable();
        }

        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////  
//        boolean flagSig = false;
//        if (flagSig == true) {
//
//            String symbol = "HOU.TO";
//            symbol = "AAPL";
//            String nnName = ConstantKey.TR_NN1;
//            int TR_NN = ConstantKey.INT_TR_NN1;
//
////            TrandingSignalProcess tsprocess = new TrandingSignalProcess();
////            int result = tsprocess.updateAllStockProcess(this, symbol);
////            TRprocessImp.testUpdateAdminTradingsignal(this, symbol);
////            AccountObj accountAdminObj = this.getAdminObjFromCache();
////            AFstockObj stock = this.getRealTimeStockImp(symbol);
////            getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);
//            NNProcessByTrend nnStProcByTrend = new NNProcessByTrend();
//            NN1ProcessBySignal nnProcBySig = new NN1ProcessBySignal();
//            TradingNNprocess NNProcessImp = new TradingNNprocess();
//
//            for (int i = 0; i < 3; i++) {
//                nnName = ConstantKey.TR_NN1;
//                TR_NN = ConstantKey.INT_TR_NN1;
//                String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                AFneuralNet nnObj1 = nnProcBySig.ProcessTrainNeuralNet1(this, BPnameSym, TR_NN, symbol);
//
//                nnName = ConstantKey.TR_NN3;
//                BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//                nnObj1 = nnStProcByTrend.ProcessTrainNeuralNetByTrend1(this, BPnameSym, TR_NN, symbol);
//
//                NNProcessImp.inputReTrainStockNeuralNetData(this, TR_NN, symbol);
//            }
//
//            TRprocessImp.testUpdateAdminTradingsignal(this, symbol);
//            getAccountProcessImp().ProcessAllAccountTradingSignal(this);
//
//        }
        boolean flagtesting = false;
        if (flagtesting == true) {
            TRprocessImp.updateStockProcess(this, "HOU.TO");
            ArrayList StockArraytmp = getStockHistorical("HOU.TO", 5 * 52 * 4);

        }

        boolean initflag = false;
        if (initflag == true) {
            for (int m = 0; m < 20; m++) {
                getAccountProcessImp().ProcessAllAccountTradingSignal(this);
            }
        }

        boolean updatetrflag = false;
        if (updatetrflag == true) {

            AccountObj account = getAccountImp().getAccountByType(CKey.ADMIN_USERNAME, null, AccountObj.INT_ADMIN_ACCOUNT);
            ArrayList stockNameList = getAccountImp().getAccountStockNameList(account.getId());
            if (stockNameList == null) {
                return;
            }
            int result = 0;
//            for (int j = 0; j < stockNameList.size(); j++) {
//                String stockN = (String) stockNameList.get(j);
//                AFstockObj stock = getStockImp().getRealTimeStock(stockN, null);
//                result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
//            }
            account = getAccountImp().getAccountByType("GUEST", null, AccountObj.INT_TRADING_ACCOUNT);
//            account.setSubstatus(ConstantKey.INT_PP_DELUXE);
//            account.setBalance(ConstantKey.INT_PP_DELUXE_PRICE);
//            getAccountImp().updateAccountStatus(account.getAccountname(), account);
            stockNameList = getAccountImp().getAccountStockNameList(account.getId());
            if (stockNameList == null) {
                return;
            }

            for (int j = 0; j < stockNameList.size(); j++) {
                String stockN = (String) stockNameList.get(j);
                AFstockObj stock = getStockImp().getRealTimeStock(stockN, null);
                result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            }
        }
//        

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
        boolean comtestflag = false;
        if (comtestflag == true) {
            AccountObj account = getAccountImp().getAccountByType(CKey.G_USERNAME, "guest", AccountObj.INT_TRADING_ACCOUNT);

            ArrayList<BillingObj> billingObjList = getAccountImp().getBillingObjByAccountID(account.getId());
            String bill = "eddy testing billing";
            getAccountImp().addAccountBilling(account, 10, 20, bill);
            billingObjList = getAccountImp().getBillingObjByAccountID(account.getId());
            if (billingObjList != null) {
                BillingObj billObj = billingObjList.get(0);
                getAccountImp().updateAccountBillingData(billObj.getId(), 1, 1, billObj.getData());
            }

            ArrayList<CommObj> comObjList = getAccountImp().getComObjByAccountID(account.getId(), 0);
            String msg = "eddy testing communication";
            getAccountImp().addAccountMessage(account, ConstantKey.COM_SIGNAL, msg);
            comObjList = getAccountImp().getComObjByAccountID(account.getId(), 0);
            if (comObjList != null) {
                ;
            }
        }

        boolean commadmflag = false;
        if (commadmflag == true) {

            //clear all communication
//            this.getAccountImp().removeCommByCommID(65);
//            this.getAccountImp().removeCommByCommID(215);
//            this.getAccountImp().removeCommByCommID(216);
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = getAdminObjFromCache();
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long dateNowLong = dateNow.getTimeInMillis();
            java.sql.Date d = new java.sql.Date(dateNowLong);
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);
            String msg = ESTdate + " testing Cust signup Result: 0";
            this.getAccountProcessImp().AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);

            // send admin messsage
            String commMsg = ESTdate + " Test" + " stock split= 10";
            CommData commDataObj = new CommData();
            commDataObj.setType(0);
            commDataObj.setSymbol("test");
            commDataObj.setEntrydatedisplay(new java.sql.Date(dateNowLong));
            commDataObj.setEntrydatel(dateNowLong);
            commDataObj.setSplit(10);
            commDataObj.setOldclose(1);
            commDataObj.setNewclose(2);
            commDataObj.setMsg(commMsg);
            getAccountProcessImp().AddCommObjMessage(this, accountAdminObj, ConstantKey.COM_SPLIT, ConstantKey.INT_COM_SPLIT, commDataObj);

        }

        boolean flagNeuralData = false;
        if (flagNeuralData == true) {
            SystemClearNNData();
        }

    }

    public void debugtest() {
        String symbol = "IWM";
        AFstockObj stock = getStockImp().getRealTimeStock(symbol, null);
        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        ArrayList<AFstockInfo> StockArray = getStockHistorical(symbol, size1yearAll);

//        Calendar dateNow = TimeConvertion.getCurrentCalendar();
//
//        long workaround = TimeConvertion.workaround_nextday_endOfDayInMillis(dateNow.getTimeInMillis());
//        long endDay = workaround;
//        long start = endDay;
//        long end = 0;
//        logger.info("debugtest>>> dateNow " + dateNow.getTimeInMillis() + ", start " + start);
//
//        String sql = "select * from stockinfo where stockid = " + stock.getId();
//        sql += " and entrydatel >= " + end + " and entrydatel <= " + start + " order by entrydatel desc";
//        logger.info("debugtest>>> sql " + sql);
    }

    public void updateErrorStockYahooParseError(String symbol) {
//        String symbol = "HOU.TO";
        AFstockObj stock = this.getRealTimeStockImp(symbol);

        stock.setStatus(ConstantKey.OPEN);
        //send SQL update
        String sockUpdateSQL = StockDB.SQLupdateStockStatus(stock);
        ArrayList sqlList = new ArrayList();
        sqlList.add(sockUpdateSQL);
        SystemUpdateSQLList(sqlList);
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        TRprocessImp.updateRealTimeStock(this, stock);
    }

    public void forceRemoveCustTest(String login, String pass) {
        CustomerObj custObj = getAccountImp().getCustomerPasswordForce(login, pass);
        if (custObj == null) {
            return;
        }
        if (custObj.getStatus() == ConstantKey.DISABLE) {
            custObj.setStatus(ConstantKey.DISABLE);;
        }
        float bal = custObj.getBalance();
        float payment = custObj.getPayment();
        float outstand = bal - payment;
        if (outstand >= 0) {  //No out standing payment 
            custObj.setStatus(ConstantKey.DISABLE);
        }
        custObj.setStatus(ConstantKey.DISABLE);
        updateCustStatusSubStatus(custObj.getUsername(), custObj.getStatus() + "", custObj.getSubstatus() + "");
        removeCustomer(custObj.getUsername());
    }

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

    //////////////////////////////////////
    // need ConstantKey.DISABLE status beofore remove customer
    public int removeCustomer(String customername) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        CustomerObj custObj = getAccountImp().getCustomerStatus(customername, null);

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
                        AFstockObj stock = getRealTimeStockImp(symbol);
                        if (stock != null) {
                            getAccountImp().removeAccountStock(accountObj, stock.getId());
                        }
                    }
                }
                getAccountImp().removeAccount(accountObj);
            }
        }
        return getAccountImp().removeCustomer(custObj);
    }

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj addCustomerPassword(String EmailUserName, String Password, String FirstName, String LastName) {
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
            // result 1 = success, 2 = existed,  0 = fail
            int result = getAccountImp().addCustomer(newCustomer);
            if (result == 1) {
                CustomerObj custObj = getAccountImp().getCustomerPassword(UserName, Password);
                if (custObj != null) {
                    // set pending for new customer
                    if (custObj.getStatus() == ConstantKey.OPEN) {
                        custObj.setStatus(ConstantKey.PENDING);
                        getAccountImp().updateCustStatusSubStatus(custObj);
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
            this.getAccountProcessImp().AddCommMessage(this, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//            
            webStatus.setResultID(result);
            return loginObj;
        }
        webStatus.setResultID(0);
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
            custObj.setPassword("***");
        }
        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;
    }

    public LoginObj getCustomerLogin(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        custObj = getAccountImp().getCustomerPassword(UserName, Password);

        LoginObj loginObj = new LoginObj();
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
    public ArrayList getExpiredStockNameList(int length) {
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

    public ArrayList getCustomerNameList(String name) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = getAccountImp().getCustomerNameList(name);
        return result;
    }

    public ArrayList getCustomerList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        result = getAccountImp().getCustomerList(length);

        return result;
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

    public ArrayList SystemAccountStockNameList(int accountId) {
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
        return getAccountImp().getAccountStockIDByTRname(accountID, stockID, trName);
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
        return getAccountImp().getAccountStockListByAccountID(accountId, stockID);
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

        return getAccountImp().getAccountStockListByAccountID(accountId, stockId);
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
        return getStockImp().getNeuralNetDataObj(BPnameTR);
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
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        return TRprocessImp.AddTransactionOrder(this, accountObj, stock, trName, tranSignal, tranDate, true);
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

//    public int updateAccountStatusByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt,
//            String substatusSt, String investmentSt, String balanceSt, String servicefeeSt) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//        if (checkCallRemoteMysql() == true) {
//            return getServiceAFwebREST().updateAccountStatusByAccountID(EmailUserName, Password, AccountIDSt, substatusSt, investmentSt, balanceSt, servicefeeSt);
//        }
//        NameObj nameObj = new NameObj(EmailUserName);
//        String UserName = nameObj.getNormalizeName();
//        try {
//            int accountid = Integer.parseInt(AccountIDSt);
//
//            int substatus = Integer.parseInt(substatusSt);
//            float investment = Float.parseFloat(investmentSt);
//            float balance = Float.parseFloat(balanceSt);
//            float servicefee = Float.parseFloat(servicefeeSt);
//            return getAccountImp().updateAccountStatusByCustomerAccountID(UserName, Password, accountid, substatus, investment, balance, servicefee);
//
//        } catch (Exception e) {
//        }
//        return 0;
//    }
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

    public ArrayList<BillingObj> getBillingByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            ArrayList<BillingObj> billingObjList = getAccountImp().getBillingByCustomerAccountID(UserName, Password, accountid);
            return billingObjList;
        } catch (Exception e) {
        }
        return null;

    }

    public ArrayList<CommObj> getEmailCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().getEmailCommByCustomerAccountID(UserName, Password, accountid, length);
        } catch (Exception e) {
        }
        return null;

    }

    public ArrayList<CommObj> getCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().getCommByCustomerAccountID(UserName, Password, accountid, length);
        } catch (Exception e) {
        }
        return null;

    }

    public int addCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, String data) {
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

    public int removeCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().removeCommSignalByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return 0;
    }

    public ArrayList getStock_AccountStockList_StockByAccountID(String EmailUserName, String Password, String AccountIDSt, int lenght) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        AccountObj accountObj = getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = getAccountImp().getAccountStockNameList(accountObj.getId());

            if (stockNameList != null) {
                if (lenght == 0) {
                    lenght = stockNameList.size();
                } else if (lenght > stockNameList.size()) {
                    lenght = stockNameList.size();
                }
                ArrayList returnStockList = new ArrayList();
                for (int i = 0; i < lenght; i++) {
                    String NormalizeSymbol = (String) stockNameList.get(i);
                    AFstockObj stock = getStockImp().getRealTimeStock(NormalizeSymbol, null);
                    if (stock != null) {

                        ArrayList<TradingRuleObj> trObjList = getAccountImp().getAccountStockListByAccountID(accountObj.getId(), stock.getId());
                        if (trObjList != null) {
                            for (int j = 0; j < trObjList.size(); j++) {
                                TradingRuleObj trObj = trObjList.get(j);
                                if (trObj.getTrname().equals(ConstantKey.TR_ACC)) {
                                    stock.setTRsignal(trObj.getTrsignal());
                                } else if (trObj.getTrname().equals(ConstantKey.TR_NN1)) {
                                    float perfProfit = 0;
                                    AccountObj accountAdminObj = getAdminObjFromCache();
                                    ArrayList<PerformanceObj> perfList = getAccountImp().getAccountStockPerfList(accountAdminObj.getId(), stock.getId(), trObj.getTrname(), 1);
                                    if (perfList != null) {
                                        if (perfList.size() > 0) {
                                            PerformanceObj perf = perfList.get(0);
                                            perfProfit = perf.getGrossprofit();
                                        }
                                    }
                                    float per = 100 * (perfProfit) / CKey.TRADING_AMOUNT;
                                    stock.setPerform(per);
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

    public AFstockObj getStock_AccountStockList_ByStockID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
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
            ArrayList tradingRuleList = getAccountImp().getAccountStockListByAccountID(accountObj.getId(), stockID);
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

            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            int ret = TRprocessImp.AddTransactionOrderWithComm(this, accountObj, stock, trName, signal, null, false);

            return ret;
        }
        return 0;
    }

    public int getAccountStockClrTranByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName) {
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

    public ArrayList<TransationOrderObj> getAccountStockTranListByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
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

    public ArrayList<PerformanceObj> getAccountStockPerfList(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
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
                TradingRuleObj tr = getAccountImp().getAccountStockIDByTRname(accountObj.getId(), stock.getId(), trName);
                if (tr == null) {
                    return 0;
                }
                int opt = 0;
                try {
                    if (TROptType != null) {
                        opt = Integer.parseInt(TROptType);
                    }
                } catch (NumberFormatException ex) {
                    opt = ConstantKey.getTRtypeByName(TROptType);
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

    public ArrayList<PerformanceObj> getAccountStockPerfHistory(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
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
                TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistory(this, tranOrderList, stock, length, true);  // buyOnly = true
            }

            AccountObj accountAdminObj = getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistory(this, tranOrderList, stock, length, false);  // buyOnly = false
        }
        return null;
    }

    public ArrayList<PerformanceObj> getAccountStockPerfHistoryReinvest(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trName, int length) {
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
                TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
                return TRprocessImp.ProcessTranPerfHistoryReinvest(this, tranOrderList, stock, length, true);  // buyOnly = true

            }
            AccountObj accountAdminObj = getAdminObjFromCache();
            if (accountAdminObj == null) {
                return null;
            }
            tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);

            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistoryReinvest(this, tranOrderList, stock, length, false); //buyOnly = false
        }
        return null;
    }

    public ArrayList<String> getAccountStockPerfHistoryDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {

        ArrayList<StockTRHistoryObj> trObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        ArrayList<String> writeTranArray = new ArrayList();
        ArrayList<String> displayArray = new ArrayList();
        int ret = getAccountStockTRListHistoryDisplayProcess(trObjList, writeTranArray, displayArray);

        ArrayList<PerformanceObj> perfObjList = getAccountStockPerfHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
        ArrayList<String> writePerfArray = new ArrayList();
        ArrayList<String> perfList = new ArrayList();
        ret = getAccountStockPerfHistoryDisplayProcess(perfObjList, writePerfArray, perfList);

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

    public int getAccountStockPerfHistoryDisplayProcess(ArrayList<PerformanceObj> perfObjList, ArrayList<String> writePerfArray, ArrayList<String> perfList) {

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

    public ArrayList getAccountStockListByAccountID(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol) {
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
            return getAccountImp().getAccountStockListByAccountID(accountObj.getId(), stockID);
        }
        return null;
    }

    public TradingRuleObj getAccountStockByTRname(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
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
            return getAccountImp().getAccountStockIDByTRname(accountObj.getId(), stockID, trname);
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

    public byte[] getAccountStockTRLIstCurrentChartDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {

        ArrayList<TransationOrderObj> thList = this.getAccountStockTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);

//        System.out.println("> getAccountStockTRLIstCurrentChartDisplay size " + thList.size());
//        System.out.println(EmailUserName + " " + Password + " " + AccountIDSt + " " + stockidsymbol + " " + trname);
        if (thList == null) {
            // still allow to display dummy graph
//            return null;
            thList = new ArrayList();
        }
        String symbol = stockidsymbol;
        AFstockObj stock = this.getRealTimeStockImp(symbol);
        if (stock == null) {
            return null;
        }
        int sizeLen = 20 * 10;
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

        float closeFirst = StockArray.get(StockArray.size() - 1).getFclose();
        float closeLast = StockArray.get(0).getFclose();
        float perC = 100 * (closeFirst - closeLast) / closeLast;
        perC = Math.abs(perC);
        float thold = 45; // 35;
        if (perC > thold) { //35) {
            int j = 0;
            for (j = 0; j < StockArray.size(); j++) {
                closeLast = StockArray.get(j).getFclose();
                perC = 100 * (closeFirst - closeLast) / closeLast;
                perC = Math.abs(perC);
                if (perC < thold) { // 35) {
                    break;
                }
            }
//            if (j > (sizeLen / 2)) {
//                j = sizeLen / 2;
//            }
            int index = j;
            ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();
            for (int i = index; i < StockArray.size(); i++) {
                StockArrayTmp.add(StockArray.get(i));
            }
            StockArray = StockArrayTmp;
        }

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

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

        /// add this one to show the trend line
        Date da = new Date(stockinfo.getEntrydatel());
        xDate.add(da);
        float close = stockinfo.getFclose();
        double norClose = close;
        yD.add(norClose);

//            if (trname.equals(ConstantKey.TR_NN3)) {
//                //StockArray assume recent date to old data 
//                int sizeNewLen = 20 * 24;
//                StockArray = this.getStockHistorical(stock.getSymbol(), sizeNewLen);
//
//                List<Date> compDate = new ArrayList<Date>();
//                List<Double> compD = new ArrayList<Double>();
//                AccountObj accountObj = this.getAdminObjFromCache();
//                ArrayList UpdateTRList = this.SystemAccountStockListByAccountID(accountObj.getId(), stock.getSymbol());
//
////                int sizeTR = 20 * CKey.MONTH_SIZE / 2; //20 * 14/2;
////                StockArray = this.getStockHistorical(stock.getSymbol(), sizeTR);
////                int start = StockArray.size() - sizeLen;
//                for (int j = 0; j < sizeLen; j++) {
//                    AFstockInfo stockinfoC = StockArray.get(j);
//
//                    if (j < 20) {
//                        //StockArray assume recent date to old data     
//                        NNObj nn = NNCal.NNpredict(this, ConstantKey.INT_TR_NN3, accountObj, stock, UpdateTRList, StockArray, j);
//                        norClose = nn.getPrediction();
//                        Date daC = new Date(stockinfoC.getEntrydatel());
//                        compDate.add(daC);
////                        close = stockinfoC.getFclose();
////                        norClose = close * 1.05;
//                        compD.add(norClose);
//                    }
//
//                }
//
//                ChartService chart = new ChartService();
//                byte[] ioStream = chart.streamCompareChartToByte(stockidsymbol + "_" + trname,
//                        xDate, yD, buyDate, buyD, sellDate, sellD, compDate, compD);
//
//                return ioStream;
//
//            }
        ChartService chart = new ChartService();
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;

    }

    public String getAccountStockTRLIstCurrentChartFile(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        try {
            ArrayList<TransationOrderObj> thList = this.getAccountStockTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);
            if (thList == null) {
                return null;
            }
            Collections.reverse(thList);

            trname = trname.toUpperCase();
            String symbol = stockidsymbol;
            AFstockObj stock = this.getRealTimeStockImp(symbol);
            ArrayList<NNInputOutObj> inputObjlist = new ArrayList();
            NNInputOutObj objPrev = null;
            boolean flag = false; //false;
            if (flag == true) {
//                if (getEnv.checkLocalPC() == true) {
//                    if (trname.equals(ConstantKey.TR_NN1) || trname.equals(ConstantKey.TR_NN2)) {
//
//                        int TR_Name = ConstantKey.INT_TR_NN1;
//                        if (trname.equals(ConstantKey.TR_NN2)) {
//                            TR_Name = ConstantKey.INT_TR_NN2;
//                        }
//
//                        int size1yearAll = 20 * 12 * 5 + (50 * 3);
//                        ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), size1yearAll);
//                        if (StockArray == null) {
//                            return null;
//                        }
//
//                        AccountObj accountObj = this.getAdminObjFromCache();
//                        ArrayList UpdateTRList = this.SystemAccountStockListByAccountID(accountObj.getId(), stock.getSymbol());
//
//                        int size1year = 20 * 10;
//                        for (int j = 0; j < size1year; j++) {
//                            int stockOffset = size1year - j + 1;
//                            AFstockInfo stockinfo = StockArray.get(stockOffset);
//                            // testing check predicion evey date in stock
//                            // testing check predicion evey date in stock                    
//                            boolean flag_1 = false;
//                            if (flag_1 == true) {
//                                NNObj nn = NNCal.NNpredict(this, TR_Name, accountObj, stock, UpdateTRList, StockArray, stockOffset);
//                                if (nn != null) {
//                                    String nameST = nn.getComment();
//                                    NNInputOutObj obj = new NNInputOutObj();
//
//                                    try {
//                                        obj = new ObjectMapper().readValue(nameST, NNInputOutObj.class
//                                        );
//                                    } catch (IOException ex) {
//                                    }
//                                    obj.setClose(stockinfo.getFclose());
//                                    obj.setOutput1(nn.getPrediction());
//
//                                    Calendar setDate = TimeConvertion.getCurrentCalendar(stockinfo.getEntrydatel());
//                                    String stdate = new Timestamp(setDate.getTime().getTime()).toString();
//                                    stdate = stdate.substring(0, 10);
//                                    obj.setDateSt(stdate);
//
//                                    inputObjlist.add(obj);
//                                    String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
//                                            + "\",\"" + obj.getOutput1()
//                                            + "\",\"" + obj.getInput1() + "\",\"" + obj.getInput2() + "\",\"" + obj.getInput3()
//                                            + "\",\"" + obj.getInput4() + "\",\"" + obj.getInput5() + "\",\"" + obj.getInput6()
//                                            + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
//                                            + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
//                                            + "\"";
//                                    logger.info(st);
//                                }
//                            }
//
//                            long stockdatel = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
//                            for (int i = 0; i < thList.size(); i++) {
//                                TransationOrderObj thObj = thList.get(i);
//                                long THdatel = TimeConvertion.endOfDayInMillis(thObj.getEntrydatel());
//                                if (stockdatel != THdatel) {
//                                    continue;
//                                }
//
//                                TransationOrderObj thObjNext = thObj;
//                                if ((thObj.getTrsignal() == ConstantKey.S_BUY) || (thObj.getTrsignal() == ConstantKey.S_SELL)) {
//                                    ;
//                                } else {
//                                    if (i + 1 < thList.size()) {
//                                        thObjNext = thList.get(i + 1);
//                                    }
//                                    long THdatelNext = TimeConvertion.endOfDayInMillis(thObjNext.getEntrydatel());
//                                    if (THdatel == THdatelNext) {
//                                        thObj = thObjNext;
//                                    }
//                                }
//                                //StockArray assume recent date to old data  
//                                //StockArray assume recent date to old data  
//                                //trainingNN1dataMACD will return oldest first to new date
//                                //trainingNN1dataMACD will return oldest first to new date   
//                                ArrayList<NNInputDataObj> inputDataObj = null;
//                                if (TR_Name == ConstantKey.INT_TR_NN1) {
//
//                                    //StockArray assume recent date to old data  
//                                    //StockArray assume recent date to old data              
//                                    //trainingNN1dataMACD will return oldest first to new date
//                                    //trainingNN1dataMACD will return oldest first to new date            
//                                    ProcessNN1 nn1 = new ProcessNN1();
//                                    inputDataObj = nn1.trainingNN1dataMACD(this, symbol, StockArray, stockOffset, CKey.SHORT_MONTH_SIZE);
//                                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
//
//                                    ProcessNN1 nn1 = new ProcessNN1();
//                                    inputDataObj = nn1.trainingNN1dataMACD2(this, symbol, StockArray, stockOffset, CKey.SHORT_MONTH_SIZE);
//                                }
//
//                                // this assume from the oldest to new date no need reverse
//                                NNInputOutObj obj = (NNInputOutObj) inputDataObj.get(0).getObj();
//
//                                int retDecision = TradingNNprocess.checkNNsignalDecision(obj, objPrev);
//
//                                double output = 0;
//                                if (retDecision == 1) {
//                                    output = 0.9;
//                                } else {
//                                    output = 0.1;
//                                }
//                                if (objPrev != null) {
//                                    objPrev.setOutput1(output);
//                                }
//                                if (objPrev != null) {
//                                    inputObjlist.add(objPrev);
//
//                                    String st = "\"" + objPrev.getDateSt() + "\",\"" + objPrev.getClose() + "\",\"" + objPrev.getTrsignal()
//                                            + "\",\"" + objPrev.getOutput1()
//                                            + "\",\"" + objPrev.getInput1() + "\",\"" + objPrev.getInput2() + "\",\"" + objPrev.getInput3()
//                                            + "\",\"" + objPrev.getInput4() + "\",\"" + objPrev.getInput5() + "\",\"" + objPrev.getInput6()
//                                            + "\",\"" + objPrev.getInput7() + "\",\"" + objPrev.getInput8()
//                                            + "\",\"" + objPrev.getInput9() + "\",\"" + objPrev.getInput10()
//                                            + "\"";
//                                    logger.info(st);
//                                }
//
//                                objPrev = obj;
//                                break;
//                            }
//                            boolean exitflag = false;
//                            if (exitflag == true) {
//                                break;
//                            }
//
//                        }
//
//                        ArrayList writeArray = new ArrayList();
//                        String stTitle = "";
//                        for (int i = 0; i < inputObjlist.size(); i++) {
//                            NNInputOutObj obj = (NNInputOutObj) inputObjlist.get(i);
//                            String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
//                                    + "\",\"" + obj.getOutput1()
//                                    + "\",\"" + obj.getInput1() + "\",\"" + obj.getInput2() + "\",\"" + obj.getInput3()
//                                    + "\",\"" + obj.getInput4() + "\",\"" + obj.getInput5() + "\",\"" + obj.getInput6()
//                                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
//                                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
//                                    + "\"";
//
//                            if (i == 0) {
//                                stTitle = "\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
//                                        + "\",\"" + "output"
//                                        + "\",\"" + "macd TSig"
//                                        + "\",\"" + "LTerm"
//                                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
//                                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
//                                        + "\"";
//
//                            }
//                            writeArray.add(st);
//
//                        }
//                        writeArray.add(stTitle);
//                        Collections.reverse(writeArray);
//                        String filename = FileLocalDebugPath + stockidsymbol + "_nn_display.csv";
//                        FileUtil.FileWriteTextArray(filename, writeArray);
//                    }
//                }  // local PC
            }

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

    public byte[] getAccountStockTRListHistoryChartDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
        ArrayList<StockTRHistoryObj> thObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);

        if (thObjList == null) {
            return null;
        }

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

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
        Collections.reverse(thObjList);

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
        byte[] ioStream = chart.streamChartToByte(stockidsymbol + "_" + trname,
                xDate, yD, buyDate, buyD, sellDate, sellD);

        return ioStream;

    }

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

//    private ArrayList<StockTRHistoryObj> getAccountStockTRListHistorySize(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
//        ArrayList<TradingRuleObj> trObjList = getAccountStockListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol);
//        trname = trname.toUpperCase();
//        if (trObjList != null) {
//            for (int i = 0; i < trObjList.size(); i++) {
//                TradingRuleObj trObj = trObjList.get(i);
//                if (trname.equals(trObj.getTrname())) {
//                    ArrayList<StockTRHistoryObj> thObjList = getTRprocessImp().ProcessTRHistory(this, trObj, 2);
//                    return thObjList;
//                }
//            }
//        }
//        return null;
//    }
    public ArrayList<StockTRHistoryObj> getAccountStockTRListHistory(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        ArrayList<TradingRuleObj> trObjList = getAccountStockListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol);
        trname = trname.toUpperCase();
        if (trObjList != null) {
            for (int i = 0; i < trObjList.size(); i++) {
                TradingRuleObj trObj = trObjList.get(i);
                if (trname.equals(trObj.getTrname())) {
                    ArrayList<StockTRHistoryObj> thObjList = TRprocessImp.ProcessTRHistory(this, trObj, 2);
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

    public int addAccountStock(String EmailUserName, String Password, String AccountIDSt, String symbol) {
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

    public int addAccountStockSymbol(AccountObj accountObj, String symbol) {
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

    public int removeAccountStock(String EmailUserName, String Password, String AccountIDSt, String symbol) {
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
                TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
                tradingRuleObj.setLinktradingruleid(ConstantKey.INT_TR_ACC);

                ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
                UpdateTRList.add(tradingRuleObj);
                getAccountImp().updateAccountStockSignal(UpdateTRList);

                TRprocessImp.AddTransactionOrderWithComm(this, accountObj, stockObj, trName, signal, null, false);
            }

            return getAccountImp().removeAccountStock(accountObj, stockObj.getId());
        }

        return 0;
    }

    public int addStock(String symbol) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        int result = getStockImp().addStock(NormalizeSymbol);
        if (result == ConstantKey.NEW) {
            TRprocessImp.ResetStockUpdateNameArray(this);
        }
        return result;
    }

//    public int AddFailCntStock(String symbol) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return 0;
//        }
//        if (checkCallRemoveMysql() == true) {
//            return getServiceAFwebREST().AddFailCntStock(symbol);
//        }
//        SymbolNameObj symObj = new SymbolNameObj(symbol);
//        String NormalizeSymbol = symObj.getYahooSymbol();
//        int result = getStockImp().AddFailCntStock(NormalizeSymbol);
//        return result;
//    }
    public int deleteStockInfo(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getRealTimeStockImp(NormalizeSymbol);
        if (stockObj != null) {
            return getStockImp().deleteStockInfoByStockId(stockObj);
        }
        return 0;
    }

    public int disableStock(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return getStockImp().disableStock(NormalizeSymbol);
    }

    public AFstockObj getRealTimeStockImp(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        return getStockImp().getRealTimeStock(NormalizeSymbol, null);
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
            ArrayList<AFstockInfo> stockInfoArrayStatic = NN1ProcessBySignal.AllStockHistoryGetfromStaticCode(NormalizeSymbol);
            if (stockInfoArrayStatic == null) {
                stockInfoArrayStatic = NN1ProcessBySignal.All_1_StockHistoryGetfromStaticCode(NormalizeSymbol);
                if (stockInfoArrayStatic == null) {
                    stockInfoArrayStatic = new ArrayList();
                }
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
        return getStockImp().setCreateNeuralNetObj0(nn.getName(), nn.getWeight());
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

    public int updateStockInfoTransaction(StockInfoTranObj stockInfoTran) {

        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        return getStockImp().updateStockInfoTransaction(stockInfoTran);
    }

    //http://localhost:8080/cust/admin1/sys/cust/eddy/update?substatus=10&investment=0&balance=15
    public int updateCustAllStatus(String customername,
            String statusSt, String paymenttSt, String balanceSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
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
            return getAccountImp().updateCustAllStatus(UserName, status, payment, balance);

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
        CustomerObj custObj = getAccountImp().getCustomerStatus(customername, null);
        custObj.setStatus(status);
        custObj.setSubstatus(substatus);
        return getAccountImp().updateCustStatusSubStatus(custObj);
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
            return getServiceAFwebREST().getSQLRequest(sqlObj);
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
                        ArrayList<TradingRuleObj> trList = getAccountImp().getAccountStockListByAccountID(accountId, stockID);
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
                        TradingRuleObj trObj = getAccountImp().getAccountStockIDByTRname(accountId, stockId, trName);
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

                        ArrayList<TradingRuleObj> trList = getAccountImp().getAccountStockListByAccountID(accountId, stockId);
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
                        ArrayList<AFneuralNetData> retArray = getStockImp().getNeuralNetDataObj(BPname);
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

    public AccData getAccData(TradingRuleObj trObj) {
        AccData refData = new AccData();
        String refName = trObj.getComment();
        try {
            if ((refName != null) && (refName.length() > 0)) {
                refName = refName.replaceAll("#", "\"");
                refData = new ObjectMapper().readValue(refName, AccData.class);
                return refData;
            }
        } catch (Exception ex) {
        }
        return refData;
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
        retSatus = getAccountProcessImp().downloadDBData(this);
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
        retSatus = getAccountProcessImp().restoreNNonlyDBData(this);
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
        retSatus = getAccountProcessImp().restoreDBData(this);
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

    public boolean SystemFundMgr() {
        FundMgrProcess fundmgr = new FundMgrProcess();
        logger.info(">SystemFundMgr start ");
        fundmgr.ProcessIISWebGlobalFundMgr(this);
        fundmgr.ProcessFundMgrAccount(this);
        logger.info(">SystemFundMgr end ");
        return true;
    }

    public boolean SystemPocessFundMgr() {
        logger.info(">SystemPocessFundMgr start ");
        getAccountProcessImp().ProcessFundAccount(this);
        logger.info(">SystemPocessFundMgr end ");
        return true;
    }

    public boolean SystemDeleteNN1Table() {
        logger.info(">SystemDeleteNN1Table start ");
        getStockImp().deleteNeuralNet1Table();
        logger.info(">SystemDeleteNN1Table end ");
        return true;
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
        int retSatus = getStockImp().initStockDB();

        if (retSatus >= 0) {
            logger.info(">InitDB Customer account ");
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(CKey.ADMIN_USERNAME);
            newCustomer.setPassword("passw0rd");
            newCustomer.setType(CustomerObj.INT_ADMIN_USER);
            getAccountImp().addCustomer(newCustomer);
            if (retSatus == 0) {

                newCustomer.setUsername(CKey.G_USERNAME);
                newCustomer.setPassword("guest");
                newCustomer.setType(CustomerObj.INT_GUEST_USER);
                getAccountImp().addCustomer(newCustomer);

                newCustomer.setUsername(CKey.FUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                getAccountImp().addCustomer(newCustomer);
//                
                newCustomer.setUsername(CKey.INDEXFUND_MANAGER_USERNAME);
                newCustomer.setPassword("passw0rd");
                newCustomer.setType(CustomerObj.INT_FUND_USER);
                getAccountImp().addCustomer(newCustomer);

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
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            getAccountImp().addCustomer(newCustomer);
        }
        return retSatus;

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

    }

    public void InitSystemFund(String portfolio) {
        if (portfolio.length() == 0) {
            return;
        }
        CustomerObj custObj = getAccountImp().getCustomerStatus(CKey.FUND_MANAGER_USERNAME, null);
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
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        TRprocessImp.InitSystemData();
        getAccountProcessImp().InitSystemData();

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
    public AccountProcess getAccountProcessImp() {
        return accountProcessImp;
    }

    /**
     * @param accountProcessImp the accountProcessImp to set
     */
    public void setAccountProcessImp(AccountProcess accountProcessImp) {
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

}
