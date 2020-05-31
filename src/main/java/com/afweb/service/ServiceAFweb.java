/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

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
import java.sql.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static ArrayList TRList = new ArrayList();

    private static AccountObj cacheAccountAdminObj = null;
    private static long cacheAccountAdminObjDL = 0;

    public static String FileLocalDebugPath = "T:/Netbean/debug/";
    public static String FileLocalNNPath = "T:/Netbean/debug/training";

    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};

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

                getServerObj().setLocalDBservice(CKey.LocalPCflag);

                if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
                    if (CKey.OPENSHIFT_DB1 == true) {
                        logger.info(">>>>> System Openshift DB1 URL:" + CKey.URL_PATH_OP_DB_PHP1);
                    } else {
                        logger.info(">>>>> System MYSQL DB2 URL:" + CKey.REMOTEDB_MY_SQLURL);
                    }
                }
                if (CKey.SQL_DATABASE == CKey.MYSQL) {
                    String dsURL = CKey.dataSourceURL;
                    logger.info(">>>>> System Local DB URL:" + dsURL);
                }
                boolean backupFlag = false;
                if (backupFlag == true) {
                    backupSystem();
                    serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                    return getServerObj().getTimerCnt();

                }
                boolean restoreFlag = false;
                if (restoreFlag == true) {
                    restoreSystem();
                    serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
                    return getServerObj().getTimerCnt();

                }

                if (CKey.UI_ONLY == false) {
                    String sysPortfolio = "";
                    // make sure not request during DB initialize
                    if (getServerObj().isLocalDBservice() == true) {
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

                    }
                    serverObj.setTimerInit(true);
                    setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue, serverObj.getSrvProjName());

                    //try 2 times
                    getAccountProcessImp().ProcessAdminAccount(this);
                    getAccountProcessImp().ProcessAdminAccount(this);

                }
                // final initialization
            } else {

                processTimer();
            }

        } catch (Exception ex) {
            logger.info("> timerHandler Exception" + ex.getMessage());
        }
        serverObj.setTimerQueueCnt(serverObj.getTimerQueueCnt() - 1);
        return getServerObj().getTimerCnt();
    }

    private void backupSystem() {
        if (CKey.LocalPCflag == true) {
            getServerObj().setSysMaintenance(true);
            serverObj.setTimerInit(true);
            if (CKey.NN_DEBUG == true) {
                // LocalPCflag = true; 
                // SQL_DATABASE = REMOTE_MYSQL;
                if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
                    if ((CKey.OPENSHIFT_DB1 == true)) {
                        logger.info(">>>>> SystemDownloadDBData form Openshift");
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
    }

    private void restoreSystem() {
        getServerObj().setSysMaintenance(true);
        serverObj.setTimerInit(true);
        if (CKey.NN_DEBUG == true) {
            if (CKey.LocalPCflag == true) {
                if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
                    if ((CKey.OPENSHIFT_DB1 == true)) {
                        logger.info(">>>>> SystemRestoreDBData to Openshift");
                    } else {
                        logger.info(">>>>> SystemRestoreDBData to Heroku");
                    }
                } else if (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL) {
                    logger.info(">>>>> SystemRestoreDBData form to My SQL");
                }
                String retSt = SystemCleanDBData();
                if (retSt.equals("true")) {
                    SystemRestoreDBData();
                    getServerObj().setSysMaintenance(true);
                    logger.info(">>>>> SystemRestoreDBData done");
                }

            }
        }
    }
    //////////
    private long lastProcessTimer = 0;
    public boolean debugFlag = false;

    public static int initTrainNeuralNetNumber = 0;
//    public static ArrayList writeArrayNeuralNet = new ArrayList();

    public boolean systemNNFlag = false;

    private void processTimer() {

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (debugFlag == false) {
                    debugFlag = true;
//                    getTRprocessImp().updateStockProcess(this, "HOU.TO");
//                        
// Window -> Debugging -> Breakpoints Select all, the delete
//
///////////////////////////////////////////////////////////////////////////////////
//                    
                    boolean fundMgrFlag = false;
                    if (fundMgrFlag == true) {
                        SystemFundMgr();
                    }

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
                        getAccountProcessImp().ProcessFundAccount(this);

                    }

///////////////////////////////////////////////////////////////////////////////////
                    processNeuralNet();
///////////////////////////////////////////////////////////////////////////////////
                    logger.info(">>>>>>>> DEBUG end >>>>>>>>>");
                }
            }
        }
        if (CKey.UI_ONLY == true) {
            return;
        }
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        String LockName = null;
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

                NNProcessImp.ProcessTrainNeuralNet(this);

                if (CKey.NN_DEBUG == true) {

                    TRprocessImp.ProcessAdminSignalTrading(this);
                    getAccountProcessImp().ProcessAllAccountTradingSignal(this);
                    TRprocessImp.UpdateAllStock(this);
                    getAccountProcessImp().ProcessSystemMaintance(this);
                }
            }
            if (((getServerObj().getProcessTimerCnt() % 27) == 0) || (getServerObj().getProcessTimerCnt() == 1)) {
                long result = setRenewLock(serverLockName, ConstantKey.SRV_LOCKTYPE);
                if (result == 0) {
                    Calendar dateNow1 = TimeConvertion.getCurrentCalendar();
                    long lockDateValue1 = dateNow1.getTimeInMillis();
                    setLockNameProcess(serverLockName, ConstantKey.SRV_LOCKTYPE, lockDateValue1, serverObj.getSrvProjName());
                }
            }
            if ((getServerObj().getProcessTimerCnt() % 500) == 0) {
                //30 sec per tick ~ 24h 60 s*60 *24 / 30
                systemNNFlag = true;
            }

            if ((getServerObj().getProcessTimerCnt() % (280 * 2)) == 0) {
                // 30 sec per tick ~ 5 hour   60 s*60 * 4/ 30 
                if (systemNNFlag == true) {
                    logger.info("> processTimer SystemClearNNinput every 4 hr");
                    LockName = "LOCK_NN_INPUT";
                    //H2_LOCKTYPE //100 minutes
                    long lockNNinput = setLockNameProcess(LockName, ConstantKey.H2_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getServerName() + " ProcessTimerCnt " + getServerObj().getProcessTimerCnt());
                    if (lockNNinput > 0) {
                        getServerObj().setAutoNNCnt(getServerObj().getAutoNNCnt() + 1);
                        SystemClearNNinput();
                        // no need to clear it
                    }
                }
            }
            if ((getServerObj().getProcessTimerCnt() % (200 * 2)) == 0) {
                // 30 sec per tick ~  for 3 hour   60 s*60 *3 /30 
                if (systemNNFlag == true) {
                    logger.info("> processTimer retrainStockNNprocessNameArray every 3 hr");

                    LockName = "LOCK_NN_RETRAIN";
                    //H2_LOCKTYPE //100 minutes
                    long lockNNinput = setLockNameProcess(LockName, ConstantKey.H2_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getServerName() + " ProcessTimerCnt " + getServerObj().getProcessTimerCnt());
                    if (lockNNinput > 0) {
                        getServerObj().setAutoNNCnt(getServerObj().getAutoNNCnt() + 1);
                        NNProcessImp.retrainStockNNprocessNameArray(this);
                    }
                }
            }
            if ((getServerObj().getProcessTimerCnt() % 21) == 0) {
                // add or remove stock in Mutual fund account based on all stocks in the system
                getAccountProcessImp().ProcessFundAccount(this);

            } else if ((getServerObj().getProcessTimerCnt() % 13) == 0) {
                // not stable -  slave cannot call Master in Openshift ???? 
                TRprocessImp.UpdateAllStock(this);

            } else if ((getServerObj().getProcessTimerCnt() % 7) == 0) {
                getAccountProcessImp().ProcessSystemMaintance(this);
                System.gc();
            } else if ((getServerObj().getProcessTimerCnt() % 5) == 0) {
                NNProcessImp.ProcessReTrainNeuralNet(this);
            } else if ((getServerObj().getProcessTimerCnt() % 3) == 0) {
                //10 Sec * 5 ~ 1 minutes
                if (CKey.SERVERDB_URL.equals(CKey.URL_PATH_OP)) {
                    TRprocessImp.UpdateAllStock(this);
                }
            } else if ((getServerObj().getProcessTimerCnt() % 2) == 0) {
                TRprocessImp.ProcessAdminSignalTrading(this);
                getAccountProcessImp().ProcessAllAccountTradingSignal(this);

                ///Error R14 (Memory quota exceeded) in heroku
                ///Error R14 (Memory quota exceeded) in heroku
                NNProcessImp.ProcessTrainNeuralNet(this);

            } else {
                NNProcessImp.ProcessInputNeuralNet(this);
            }

        } catch (Exception ex) {
            logger.info("> processTimer Exception" + ex.getMessage());
        }
        removeNameLock(LockName, ConstantKey.SRV_LOCKTYPE);
    }

    public static String debugSymbol = "HOU.TO";

    public static boolean forceNNReadFileflag = false;

    private void processNeuralNet() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        boolean flagNeuralnet = false;
        if (flagNeuralnet == true) {
//            for (int i = 0; i < 100; i++) {
//                NNProcessImp.ProcessInputNeuralNet(this);
//            }

//            NeuralNetInputTesting(ConstantKey.INT_TR_NN1);
//            NeuralNetInputTesting(ConstantKey.INT_TR_NN2);
            // start training
            NeuralNetProcessTesting(ConstantKey.INT_TR_NN1);

//            ArrayList<NNInputDataObj> inputlistSym = null;
//            String symbol = "HOU.TO";
//            inputlistSym = TRprocessImp.getTrainingInputDataFromFile(this, symbol);
//
//            ArrayList<NNInputDataObj> inputlist = new ArrayList();
//            inputlist = TRprocessImp.getTrainingInputDataFromFile(this);
//
//            for (int i=0; i<inputlist.size(); i++ ) {
//
//            }
        }
//        
        boolean flagNeuralnetCreateJava = false;
        if (flagNeuralnetCreateJava == true) {
            String symbol = "HOU.TO";
            NeuralNetCreatJava();

            int nnTRN = ConstantKey.INT_TR_NN1;
            String nnName = ConstantKey.TR_NN1;

            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            getStockImp().updateNeuralNetStatus1(BPnameSym, ConstantKey.INITIAL, 0);
//            NNProcessImp.inputReTrainStockNeuralNetData(this, nnTRN, symbol);
            NNProcessImp.inputStockNeuralNetData(this, nnTRN, symbol);
//            NNProcessImp.stockTrainNeuralNet(this, nnTRN, symbol);
        }
//        
        boolean flaginpTrainData = false;
        if (flaginpTrainData == true) {
            TRprocessImp.initTrainingNeuralNetFile(this, ConstantKey.INT_TR_NN1);
//          getTRprocessImp().initTrainingNeuralNetFile(this, ConstantKey.INT_TR_NN2);
        }

        boolean flaginputStock = false;
        if (flaginputStock == true) {
            getAccountProcessImp().updateAllStockFile(this);
        }

        boolean retrainupdateinputflag = false;
        if (retrainupdateinputflag == true) {
            updateDBneuralnetDataProcess();

        }

        boolean flagRetrainData = false;
        if (flagRetrainData == true) {
            NNProcessImp.retrainStockNNprocessNameArray(this);
            NNProcessImp.ProcessReTrainNeuralNet(this);

//            NNProcessImp.inputReTrainStockNeuralNetData(this, ConstantKey.INT_TR_NN2, "HOU.TO");
        }

        boolean retrainflag = false;
        if (retrainflag == true) {
            ArrayList<NNInputOutObj> inputlist = new ArrayList();
            String symbol = "HOU.TO";

            TradingNNprocess trainNN = new TradingNNprocess();
            ArrayList<NNInputDataObj> inputlistSym = trainNN.getReTrainingNNdataStock(this, symbol, ConstantKey.INT_TR_NN1, 0);
        }

        boolean dbhero2opflag = false;
        if (dbhero2opflag == true) {
            CKey.OPENSHIFT_DB1 = false;
            ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_HERO_DBDB_PHP + CKey.WEBPOST_HERO_PHP);
            backupSystem();
//
            CKey.OPENSHIFT_DB1 = true;
            ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_OP_DB_PHP1 + CKey.WEBPOST_OP_PHP);
            restoreSystem();
            // restore original
            CKey.OPENSHIFT_DB1 = false;
        }

        boolean flagTran_TR_ACC = false;
        if (flagTran_TR_ACC == true) {
            SystemClearNNtranAllAcc();
        }
        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////   

        boolean stocksplitflag = false;
        if (stocksplitflag == true) {
            /////////need manually enter the communication id
            /////////need manually enter the communication id

            int commid = 215; // 216; // 215;
            CommObj commObj = getAccountImp().getCommObjByID(commid);
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
                    String nnFileName = FileLocalPath + sym + ".csv";
                    if (FileUtil.FileTest(nnFileName) == false) {
                        logger.info("updateStockFile not found " + nnFileName);
                        return;
                    }

                    getStockImp().deleteStockInfoByStockId(stock);
                    // update file
                    retBoolean = getAccountProcessImp().updateStockFile(this, sym);

                    if (retBoolean == true) {
                        processStockSplit(commData.getSymbol(), commData.getSplit());
                    }
                }
            }

        }

        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////
        boolean initflag = false;
        if (initflag == true) {

//
            String symbol = "HOU.TO";
            AFstockObj stock = this.getRealTimeStockImp(symbol);
            TRprocessImp.updateRealTimeStockTest(this, stock);
            for (int k = 0; k < 20; k++) {
                TRprocessImp.UpdateAllStock(this);
            }
            //EDDY-KO00-GMAIL-COM, EK4166294399-GMAIL-COM, EDDY-KO100-GMAIL-COM, Eddy
//            forceRemoveCustTest("EDDY-KO00-GMAIL-COM", "pass");
//            int ret = InitDBData();  // init DB Adding customer account
//            getAccountProcessImp().ProcessCustomerDisableMaintanceTest(this);
//

//
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
        boolean comtestflag = false;
        if (comtestflag == true) {
            AccountObj account = getAccountImp().getAccountByType("GUEST", "guest", AccountObj.INT_TRADING_ACCOUNT);

            ArrayList<BillingObj> billingObjList = getAccountImp().getBillingObjByAccountID(account.getId());
            String bill = "eddy testing billing";
            getAccountImp().addAccountBilling(account, 10, 20, bill);
            billingObjList = getAccountImp().getBillingObjByAccountID(account.getId());
            if (billingObjList != null) {
                BillingObj billObj = billingObjList.get(0);
                getAccountImp().updateAccountBillingData(billObj.getId(), 1, 1, billObj.getData());
            }

            ArrayList<CommObj> comObjList = getAccountImp().getComObjByAccountID(account.getId());
            String msg = "eddy testing communication";
            getAccountImp().addAccountMessage(account, msg);
            comObjList = getAccountImp().getComObjByAccountID(account.getId());
            if (comObjList != null) {
                ;
            }
        }

        boolean commadmflag = false;
        if (commadmflag == true) {
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
            this.getAccountProcessImp().AddCommMessage(this, accountAdminObj, msg);

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
            getAccountProcessImp().AddCommObjMessage(this, accountAdminObj, commDataObj);

        }

        boolean flagRetrainTest = false;
        if (flagRetrainTest == true) {
            String symbol = "HOU.TO";
            int nnTRN = ConstantKey.INT_TR_NN2;
            String nnName = ConstantKey.TR_NN2;
            String BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().deleteNeuralNetData(BPname);
////     
//
//            this.getStockImp().updateNeuralNetStatus1(BPname, ConstantKey.INITIAL, 0);
//            NNProcessImp.inputStockNeuralNetData(this, nnTRN, symbol);
//
//            for (int k = 0; k < 10; k++) {
//                int ret = NNProcessImp.stockTrainNeuralNet(this, nnTRN, symbol);
//                if (ret == 1) {
//                    break;
//                }
//            }
////

            NNProcessImp.inputReTrainStockNeuralNetData(this, ConstantKey.INT_TR_NN2, "HOU.TO");

            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            ArrayList<NNInputOutObj> inputlist = new ArrayList();
            ArrayList<AFneuralNetData> objDataList = null;
            objDataList = getStockImp().getNeuralNetDataObj(BPnameSym);
            if (objDataList != null) {

                for (int i = 0; i < objDataList.size(); i++) {
                    String dataSt = objDataList.get(i).getData();
                    NNInputOutObj input;
                    try {
                        input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                        inputlist.add(input);
                    } catch (IOException ex) {
                    }
                }

                if (getEnv.checkLocalPC() == true) {
                    ArrayList writeArray = new ArrayList();
                    String stTitle = "";
                    for (int i = 0; i < inputlist.size(); i++) {
                        NNInputOutObj obj = (NNInputOutObj) inputlist.get(i);

                        String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                                + "\",\"" + obj.getOutput1()
                                + "\",\"" + obj.getInput1() + "\",\"" + obj.getInput2() + "\",\"" + obj.getInput3()
                                + "\",\"" + obj.getInput4() + "\",\"" + obj.getInput5() + "\",\"" + obj.getInput6()
                                + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                                + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                                + "\"";

                        if (i == 0) {
                            stTitle = "\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                                    + "\",\"" + "output"
                                    + "\",\"" + "macd TSig"
                                    + "\",\"" + "LTerm"
                                    + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                                    + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                                    + "\"";

                        }
                        writeArray.add(st);
                    }
                    writeArray.add(stTitle);
                    Collections.reverse(writeArray);

                    String pathSt = "t:/Netbean/debug";
                    String filename = pathSt + "/" + symbol + "_nn_update.csv";
                    FileUtil.FileWriteTextArray(filename, writeArray);
                }
            }
        }

//                        
        boolean flagChart = false;
        if (flagChart == true) {
            //https://iiswebsrv.herokuapp.com/cust/admin1/acc/1/st/hou_to/tr/TR_nn1/tran/history/chartdisplay
            String sym = "hou_to";
//          getNNProcessImp().trainingNNdataAll(this, ConstantKey.INT_TR_NN1, 0);
            ArrayList<NNInputDataObj> inputobj = NNProcessImp.trainingNNupdateMACD(this, sym);

        }

//        boolean flagFund = false;
//        if (flagFund == true) {
//            InitSystemFund(CKey.FUND_PORTFOLIO);
//            for (int i = 0; i < 10; i++) {
//                getAccountProcessImp().ProcessFundAccount(this);
//                getAccountProcessImp().ProcessAdminAccount(this);
//                TRprocessImp.UpdateAllStock(this);
//            }
//        }
        boolean flagNeuralData = false;
        if (flagNeuralData == true) {
            SystemClearNNData();
        }

        boolean flagNeural = false;
        if (flagNeural == true) {
            SystemClearNNinput();
            NNProcessImp.ProcessInputNeuralNet(this);
            for (int k = 0; k < 20; k++) {
                NNProcessImp.ProcessTrainNeuralNet(this);
            }

        }

        boolean flagSig = false;
        if (flagSig == true) {

            String symbol = "HOU.TO";
//            String symbol = "DIA";
            String nnName = ConstantKey.TR_NN3;

// force manual signal
//            AccountObj accountAdminObj = null;
//            AccountObj accountObj = null;
//
//            ArrayList accountList = getAccountList(CKey.ADMIN_USERNAME, null);
//            // do not clear the lock so that it not run by other tast immediately
//            for (int i = 0; i < accountList.size(); i++) {
//                AccountObj accountTmp = (AccountObj) accountList.get(i);
//                if (accountTmp.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
//                    accountAdminObj = accountTmp;
//                } else if (accountTmp.getType() == AccountObj.INT_TRADING_ACCOUNT) {
//                    accountObj = accountTmp;
//                }
//
//            }
//            String accountid = accountObj.getId()+"";
//            String stockidsymbol = "HOU.TO";
//
//            int ret = addAccountStockTran(CKey.ADMIN_USERNAME,
//                    null, accountid, stockidsymbol, "TR_ACC", 2);
//            
//          // will clear the transaction history  
            AFstockObj stock = this.getRealTimeStockImp(symbol);
            AccountObj accountAdminObj = this.getAdminObjFromCache();
            getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);
//          update HOU current history of transaction
            TRprocessImp.testUpdateAdminTradingsignal(this, symbol);
//            getAccountProcessImp().ProcessAllAccountTradingSignal(this);

            // update HOU history of transaction
//            AccountObj accountAObj = getAdminObjFromCache();
//            TRprocessImp.upateAdminTransaction(this, accountAObj, symbol);
        }

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
        TRprocessImp.updateRealTimeStockTest(this, stock);
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
                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    stock.setSubstatus(ConstantKey.OPEN);
                    String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockNameSQL);
                    SystemUpdateSQLList(sqlList);
                    logger.info("updateRealTimeStock " + accountObj.getAccountname() + " " + symbol + " Stock Split cleared");

                }
                //update performance
            }

            logger.info("> processStockSplit no update " + accountObj.getAccountname());
        }

        return 1;
    }

    // training neural net input data
    // create neural net input data
    //     
    private void NeuralNetInputTesting(int TR_Name) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        boolean createTrain = true;
        if (createTrain == true) {
            int sizeYr = 3;
            String nnName = ConstantKey.TR_NN1;
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
            }

            for (int j = 0; j < sizeYr; j++) { //4; j++) {
                int size = 20 * CKey.MONTH_SIZE * j;
//                writeArrayNeuralNet.clear();
                initTrainNeuralNetNumber = j + 1;

                NNProcessImp.trainingNNdataAll(this, TR_Name, size);

//                if (getEnv.checkLocalPC() == true) {
//
//                    String filename = FileLocalDebugPath + nnName + initTrainNeuralNetNumber + ".csv";
//                    FileUtil.FileWriteTextArray(filename, writeArrayNeuralNet);
//
////                    filename = FileLocalNNPath + "/" + nnName + initTrainNeuralNetNumber + ".csv";
////                    FileUtil.FileWriteTextArray(filename, writeArrayNeuralNet);
//                    logger.info(">>> NeuralNetTesting write " + j + " " + filename);
//                }
            }
        }
        // create neural net input data
    }

    public int updateDBneuralnetDataProcess() {
        String tableName = "neuralnetdata";
        try {
            ArrayList<String> writeArray = new ArrayList();
            String fName = FileLocalPath + tableName + ".txt";
            if (FileUtil.FileTest(fName) == false) {
                return 0;
            }
            FileUtil.FileReadTextArray(fName, writeArray);
            ArrayList<String> writeSQLArray = new ArrayList();
            logger.info("> updateDBneuralnetDataProcess " + tableName + " " + writeArray.size());
            int totalAdd = 0;
            int totalDup = 0;
            for (int i = 0; i < writeArray.size(); i++) {
                String output = writeArray.get(i);
                AFneuralNetData objData = new ObjectMapper().readValue(output, AFneuralNetData.class);
                boolean flagtype = true;
                if (flagtype == true) {
                    if (objData.getType() != 0) {
                        totalDup++;
                        continue;
                    }
                }
                ArrayList<AFneuralNetData> objList = this.getStockImp().getNeuralNetDataObj(objData.getName(), objData.getType(), objData.getUpdatedatel());
                if (objList == null) {
                    continue;
                }
                if (objList.size() != 0) {
                    totalDup++;
                    continue;
                }
                this.getStockImp().insertNeuralNetDataObject(objData);
                totalAdd++;
            }
            logger.info("> updateDBneuralnetDataProcess  totalAdd=" + totalAdd + " totalDup=" + totalDup);

            return 1;

        } catch (IOException ex) {
            logger.info("> restoreDBneuralnetDataProcess - exception " + ex);
        }
        return 0;
    }

    public ArrayList<NNInputDataObj> NeuralNetGetNN1InputfromStaticCode(String symbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nnData.NN1_INPUTLIST1);
            inputBuf.append(nnData.NN1_INPUTLIST2);
            inputBuf.append(nnData.NN1_INPUTLIST3);
            inputBuf.append(nnData.NN1_INPUTLIST4);
            inputBuf.append(nnData.NN1_INPUTLIST5);
            inputBuf.append(nnData.NN1_INPUTLIST6);
            inputBuf.append(nnData.NN1_INPUTLIST7);
            inputBuf.append(nnData.NN1_INPUTLIST8);
            inputBuf.append(nnData.NN1_INPUTLIST9);
            inputBuf.append(nnData.NN1_INPUTLIST10);
//            inputBuf.append(nnData.NN1_INPUTLIST11);
            String inputListSt = decompress(inputBuf.toString());
            HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
            stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);
            if (symbol != "") {
                inputlist = stockInputMap.get(symbol);
                if (inputlist == null) {
                    return null;
                }
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
                inputlist = new ArrayList<NNInputDataObj>(listItem);
                return inputlist;
            }

            for (String sym : stockInputMap.keySet()) {
                ArrayList<NNInputDataObj> inputL = stockInputMap.get(sym);
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputL);
                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
                inputL = new ArrayList<NNInputDataObj>(listItem);
                inputlist.addAll(inputL);
            }

            return inputlist;
        } catch (Exception ex) {
            logger.info("> NeuralNetGetNN1InputfromStaticCode - exception " + ex);
        }
        return null;
    }

    private boolean NeuralNetCreatJava() {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getTrainingInputDataFromFile(this, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = compress(inputListRawSt);

            String fileN = ServiceAFweb.FileLocalDebugPath + "NNBP_V1_TR_NN1_nnWeight0.txt";
            if (FileUtil.FileTest(fileN) == false) {
                return false;
            }
            StringBuffer msg1 = FileUtil.FileReadText(fileN);
            String weightSt = msg1.toString();
            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class nnData {\n"
                    + "\n"
                    + "    public static String NN1_WEIGHT_0 = \"\"\n");
            int sizeline = 1000;
            int len = weightSt.length();
            int beg = 0;
            int end = sizeline;
            while (true) {
                String st = weightSt.substring(beg, end);
                msgWrite.append("+ \"" + st + "\"\n");
                if (end >= len) {
                    break;
                }
                beg = end;
                if (end + sizeline <= len) {
                    end += sizeline;
                } else {
                    end = len;
                }
            }
            msgWrite.append(""
                    + "            + \"\";\n");

            len = inputListSt.length();
            beg = 0;
            end = sizeline;
            int index = 1;
            int line = 0;
            while (true) {
                if (line == 0) {
                    msgWrite.append(""
                            + "    public static String NN1_INPUTLIST" + index + " = \"\"\n"
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

            msgWrite.append(""
                    + "}\n"
                    ///
                    + ""
            );
            fileN = ServiceAFweb.FileLocalDebugPath + "nnData.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private void NeuralNetProcessTesting(int TR_Name) {
        ///////////////////////////////////////////////////////////////////////////////////
        // read new NN data
        forceNNReadFileflag = true; // should be true to get it from file instead from db
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        boolean initTrainNeuralNet = true;
        if (initTrainNeuralNet == true) {

            double errorNN = CKey.NN1_ERROR_THRESHOLD;
            String nnName = ConstantKey.TR_NN1;
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
                errorNN = CKey.NN2_ERROR_THRESHOLD;
            }
            String BPname = CKey.NN_version + "_" + nnName;
            // Not need to do neural net for NN2. Same NN weight for NN1 and NN2
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                return;
            }

            boolean flagInit = true;
            if (flagInit == true) {
//                getStockImp().setCreateNeuralNetObj1(BPname, "");
//                TRprocessImp.initTrainingNeuralNetData(this, nnName);

                AFneuralNet afNeuralNet = getNeuralNetObjWeight1(BPname, 0);
                if (afNeuralNet == null) {
                    afNeuralNet = new AFneuralNet();
                    afNeuralNet.setName(BPname);
                    afNeuralNet.setStatus(ConstantKey.OPEN);
                    afNeuralNet.setType(0);
                    Calendar dateDefault = TimeConvertion.getDefaultCalendar();
                    afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
                    afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
                    String weightSt = (CKey.NN1_WEIGHT_0);
                    afNeuralNet.setWeight(weightSt);
                    setNeuralNetObjWeight1(afNeuralNet);
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                if (TR_Name == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(this, nnName, errorNN);
                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
                    retflag = TRprocessImp.TRtrainingNN2NeuralNetData(this, nnName, errorNN);
                }
                if (retflag == 1) {
                    break;
                }
                logger.info(">>> initTrainNeuralNet " + i);
            }
        }

    }

    public static void AFSleep() {
        // delay seems causing openshif not working        
        if (true) {
            return;
        }
        try {
            Thread.sleep(10);
        } catch (Exception ex) {
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
//        if (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) {
//            ret = false;
//        }
        return ret;
    }

    //////////////////////////////////////
    // need ConstantKey.DISABLE status beofore remove customer
    public int removeCustomer(String customername) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().removeCustomer(customername);
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

//       SUCC = 1;  EXISTED = 2; FAIL =0;
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().addCustomerPassword(EmailUserName, Password, FirstName, LastName);
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
            int result = getAccountImp().addCustomer(newCustomer);
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
            this.getAccountProcessImp().AddCommMessage(this, accountAdminObj, msg);
//            
            webStatus.setResultID(result);
            return loginObj;
        }
        webStatus.setResultID(0);
        return loginObj;
    }

    public CustomerObj getCustomerIgnoreMaintenance(String EmailUserName, String Password) {
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getCustomerPassword(EmailUserName, Password);
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        return getAccountImp().getCustomerPassword(UserName, Password);
    }

    public CustomerObj getCustomerPassword(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getCustomerPassword(EmailUserName, Password);
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        return getAccountImp().getCustomerPassword(UserName, Password);
    }

    public LoginObj getCustomerEmailLogin(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getCustomerEmailLogin(EmailUserName, Password);
        } else {
            NameObj nameObj = new NameObj(EmailUserName);
            String UserName = nameObj.getNormalizeName();
            custObj = getAccountImp().getCustomerPassword(UserName, Password);
        }
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

    public LoginObj getCustomerLogin(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getCustomerLogin(EmailUserName, Password);
        } else {
            NameObj nameObj = new NameObj(EmailUserName);
            String UserName = nameObj.getNormalizeName();
            custObj = getAccountImp().getCustomerPassword(UserName, Password);
        }
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
        if (checkCallRemoteMysql() == true) {

            result = getServiceAFwebREST().getAllDisableStockNameList(length);
        } else {
            result = this.getStockImp().getAllDisableStockNameList(length);
        }
        return result;
    }

    //only on type=" + CustomerObj.INT_CLIENT_BASIC_USER;
    public ArrayList getExpiredCustomerList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            result = getServiceAFwebREST().getExpiredCustomerList(length);
        } else {
            result = getAccountImp().getExpiredCustomerList(length);
        }
        return result;
    }

    public ArrayList getCustomerList(int length) {
        ArrayList result = null;
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            result = getServiceAFwebREST().getCustomerList(length);
        } else {
            result = getAccountImp().getCustomerList(length);
        }

        return result;
    }

    public ArrayList getAccountList(String EmailUserName, String Password) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountList(EmailUserName, Password);
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
                NameList = new ObjectMapper().readValue(output, ArrayList.class
                );
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

    public int updateAccountStatusByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt,
            String substatusSt, String investmentSt, String balanceSt, String servicefeeSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().updateAccountStatusByAccountID(EmailUserName, Password, AccountIDSt, substatusSt, investmentSt, balanceSt, servicefeeSt);
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);

            int substatus = Integer.parseInt(substatusSt);
            float investment = Float.parseFloat(investmentSt);
            float balance = Float.parseFloat(balanceSt);
            float servicefee = Float.parseFloat(servicefeeSt);
            return getAccountImp().updateAccountStatusByCustomerAccountID(UserName, Password, accountid, substatus, investment, balance, servicefee);

        } catch (Exception e) {
        }
        return 0;
    }

    public AccountObj getAccountByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountByCustomerAccountID(EmailUserName, Password, AccountIDSt);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getBillingByCustomerAccountID(EmailUserName, Password, AccountIDSt, length);
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

    public ArrayList<CommObj> getCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getCommByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().getCommByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return null;

    }

    public int addCommByCustomerAccountID(String EmailUserName, String Password, String AccountIDSt, String data) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().addCommByCustomerAccountID(EmailUserName, Password, AccountIDSt, data);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().removeAccountCommByID(EmailUserName, Password, AccountIDSt, IDSt);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().removeCommByCustomerAccountID(EmailUserName, Password, AccountIDSt);
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return getAccountImp().removeCommByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return 0;
    }

    public ArrayList getStock_AccountStockList_StockByAccountID(String EmailUserName, String Password, String AccountIDSt, int lenght) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getStock_AccountStockList_StockByAccountID(EmailUserName, Password, AccountIDSt, lenght);
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
                        TradingRuleObj trObj = getAccountImp().getAccountStockIDByTRname(accountObj.getId(), stock.getId(), ConstantKey.TR_ACC);
                        if (trObj != null) {
                            stock.setTRsignal(trObj.getTrsignal());
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getStock_AccountStockList_ByStockID(EmailUserName, Password, AccountIDSt, stockidsymbol);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setAccountStockTran(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, signal);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockClrTranByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trName);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockPerfList(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, length);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setAccountStockTRoption(EmailUserName, Password, AccountIDSt, stockidsymbol, trName, TROptType);
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
            } else {
                AccountObj accountAdminObj = getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            }
            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistory(this, tranOrderList, stock, length);
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
            } else {
                AccountObj accountAdminObj = getAdminObjFromCache();
                if (accountAdminObj == null) {
                    return null;
                }
                tranOrderList = getAccountImp().getAccountStockTransList(accountAdminObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            }
            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            return TRprocessImp.ProcessTranPerfHistoryReinvest(this, tranOrderList, stock, length);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockByTRname(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
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

    public ArrayList getAccountStockTRListHistoryNN(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, NNTrainObj nnTraining) {
        ArrayList<StockTRHistoryObj> thObjListMACD = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, ConstantKey.TR_MACD);
        ArrayList<StockTRHistoryObj> thObjListMV = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListRSI = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, ConstantKey.TR_RSI);
        return this.getAccountStockTRListHistoryMACDNN(thObjListMACD, thObjListMV, thObjListRSI, stockidsymbol, nnTraining, ConstantKey.TR_MACD, false);
    }

    public ArrayList<NNInputDataObj> getAccountStockTRListHistoryDataMACDNN(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI, String stockidsymbol, NNTrainObj nnTraining, String TRoutput, boolean lastDateOutput) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        if ((thObjListMACD == null) || (thObjListMV == null)) {
            return null;
        }
        if (thObjListMACD.size() != thObjListMV.size()) {
            return null;
        }
        if (thObjListRSI.size() != thObjListRSI.size()) {
            return null;
        }
        NNTrainObj nnTr = new NNTrainObj();
        if (nnTraining != null) {
            nnTr = nnTraining;
        }
        ArrayList<NNInputOutObj> trInputList = new ArrayList();
        nnTr.setNnInputList(trInputList);

        StockTRHistoryObj prevThObj = null;

        boolean processLastDate = false;

        ArrayList<NNInputDataObj> inputDatalist = new ArrayList<NNInputDataObj>();
        NNInputDataObj objDataPrev = null;

        for (int i = 0; i < thObjListMACD.size(); i++) {

            if (i + 1 == thObjListMACD.size()) {
                if (lastDateOutput == true) {
                    processLastDate = true;
                }
            }
            NNInputOutObj inputList = new NNInputOutObj();

            StockTRHistoryObj thObjMACD = thObjListMACD.get(i);
            if (i == 0) {
                prevThObj = thObjMACD;
            }

            int signal = thObjMACD.getTrsignal();
            boolean contProcess = false;
            if (signal != prevThObj.getTrsignal()) {
                contProcess = true;
            }
            if (processLastDate == true) {
                contProcess = true;
            }

            if (contProcess == true) {
                inputList = NNProcessImp.getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);

                double parm1 = -1;
                if (signal == ConstantKey.S_BUY) {
                    parm1 = 0.9;
                } else if (signal == ConstantKey.S_SELL) {
                    parm1 = 0.1;
                }
                inputList.setInput1(parm1);
                inputList.setTrsignal(signal);
                ArrayList<Double> closeArray = NNProcessImp.getNNnormalizeInputClose(i, thObjListMACD);
                inputList.setInput6(closeArray.get(0));
                inputList.setInput7(closeArray.get(1));
                inputList.setInput8(closeArray.get(2));
                inputList.setInput9(closeArray.get(3));
                inputList.setInput10(closeArray.get(4));

                int retDecision = NNProcessImp.checkNNsignalDecision(thObjMACD, prevThObj);

                double output = 0;
                if (retDecision == 1) {
                    output = 0.9;
                } else {
                    output = 0.1;
                }

                NNInputDataObj objDataCur = new NNInputDataObj();
                objDataCur.setUpdatedatel(thObjMACD.getUpdateDatel());
                objDataCur.setObj(inputList);

                if (objDataPrev != null) {
                    objDataPrev.getObj().setOutput1(output);
                    trInputList.add(objDataPrev.getObj());
                    inputDatalist.add(objDataPrev);

//                    if (getEnv.checkLocalPC() == true) {
//                        if (CKey.NN_DEBUG == true) {
//
//                            NNInputOutObj objP = objDataPrev.getObj();
//                            String st = "\"" + objP.getDateSt() + "\",\"" + objP.getClose() + "\",\"" + objP.getTrsignal()
//                                    + "\",\"" + objP.getOutput1()
//                                    + "\",\"" + objP.getInput1() + "\",\"" + objP.getInput2() + "\",\"" + objP.getInput3()
//                                    + "\",\"" + objP.getInput4() + "\",\"" + objP.getInput5() + "\",\"" + objP.getInput6()
//                                    + "\",\"" + objP.getInput7() + "\",\"" + objP.getInput8()
//                                    + "\",\"" + objP.getInput9() + "\",\"" + objP.getInput10()
//                                    + "\"";
//                            logger.info(i + "," + st);
//                        }
//                    }
                }
                prevThObj = thObjMACD;
                objDataPrev = objDataCur;

            }
        }// end of loop
        if (objDataPrev != null) {
            if (lastDateOutput == true) {
                // eddy just for testing
//                trInputList.clear(); // clear so that only the last one
            }
            trInputList.add(objDataPrev.getObj());
            objDataPrev.getObj().setOutput1(0);
            trInputList.add(objDataPrev.getObj());
            inputDatalist.add(objDataPrev);

        }

        return inputDatalist;
    }

    public ArrayList getAccountStockTRListHistoryMACDNN(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI, String stockidsymbol, NNTrainObj nnTraining, String TRoutput, boolean lastDateOutput) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        ArrayList<String> writeAllArray = new ArrayList();
        if ((thObjListMACD == null) || (thObjListMV == null)) {
            return writeAllArray;
        }
        if (thObjListMACD.size() != thObjListMV.size()) {
            return writeAllArray;
        }
        if (thObjListRSI.size() != thObjListRSI.size()) {
            return writeAllArray;
        }
        NNTrainObj nnTr = new NNTrainObj();
        if (nnTraining != null) {
            nnTr = nnTraining;
        }
        ArrayList<NNInputOutObj> trInputList = new ArrayList();
        nnTr.setNnInputList(trInputList);

        StockTRHistoryObj prevThObj = null;
        NNInputOutObj prevInputList = null;

        boolean processLastDate = false;
        for (int i = 0; i < thObjListMV.size(); i++) {

            if (i + 1 == thObjListMV.size()) {
                if (lastDateOutput == true) {
                    processLastDate = true;
                }
            }
            NNInputOutObj inputList = new NNInputOutObj();

            StockTRHistoryObj thObjMACD = thObjListMACD.get(i);
            if (i == 0) {
                prevThObj = thObjMACD;
            }

            int signal = thObjMACD.getTrsignal();
            boolean contProcess = false;
            if (signal != prevThObj.getTrsignal()) {
                contProcess = true;
            }
            if (processLastDate == true) {
                contProcess = true;
            }

            if (contProcess == true) {

                inputList = NNProcessImp.getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);

                double parm1 = -1;
                if (signal == ConstantKey.S_BUY) {
                    parm1 = 0.9;
                } else if (signal == ConstantKey.S_SELL) {
                    parm1 = 0.1;
                }
                inputList.setInput1(parm1);
                inputList.setTrsignal(signal);
                ArrayList<Double> closeArray = NNProcessImp.getNNnormalizeInputClose(i, thObjListMACD);
                inputList.setInput6(closeArray.get(0));
                inputList.setInput7(closeArray.get(1));
                inputList.setInput8(closeArray.get(2));
                inputList.setInput9(closeArray.get(3));
                inputList.setInput10(closeArray.get(4));

                int retDecision = NNProcessImp.checkNNsignalDecision(thObjMACD, prevThObj);

                double output = 0;
                if (retDecision == 1) {
                    output = 0.9;
                } else {
                    output = 0.1;
                }

                if (prevInputList != null) {
                    prevInputList.setOutput1(output);
                    trInputList.add(prevInputList);
                }
                prevThObj = thObjMACD;
                prevInputList = inputList;
            }
        }// end of loop
        if (prevInputList != null) {
            if (lastDateOutput == true) {
                // eddy just for testing
//                trInputList.clear(); // clear so that only the last one
            }
            trInputList.add(prevInputList);
        }
        ArrayList writeArray = new ArrayList();
        String stTitle = "";
        int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use        
        for (int i = 0; i < trInputList.size(); i++) {
            NNInputOutObj obj = trInputList.get(i);
            String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                    + "\",\"" + obj.getOutput1()
                    + "\",\"" + obj.getInput1()
                    + "\",\"" + obj.getInput2()
                    + "\",\"" + obj.getInput3()
                    + "\",\"" + obj.getInput4()
                    + "\",\"" + obj.getInput5()
                    + "\",\"" + obj.getInput6()
                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                    //                    + "\",\"" + obj.getInput11() + "\",\"" + obj.getInput12()
                    + "\"";

            if (i == 0) {
                st += ",\"last\"";
            }

            if (i + 1 >= trInputList.size()) {
                st += ",\"first\"";
            }

            if (i == 0) {
                stTitle = "\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                        + "\",\"" + "output"
                        + "\",\"" + "macd TSig"
                        + "\",\"" + "LTerm"
                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                        + "\",\"" + stockidsymbol + "\"";

            }
            writeArray.add(st);
            String stDispaly = st.replaceAll("\"", "");
            writeAllArray.add(stDispaly);

        }
        writeArray.add(stTitle);
        writeAllArray.add(stTitle.replaceAll("\"", ""));

        Collections.reverse(writeArray);
        Collections.reverse(writeAllArray);

        if (getEnv.checkLocalPC() == true) {
            String filename = FileLocalDebugPath + stockidsymbol + "_nn1_" + initTrainNeuralNetNumber + ".csv";
            FileUtil.FileWriteTextArray(filename, writeArray);
//            writeArrayNeuralNet.addAll(writeAllArray);
        }
        return writeAllArray;
    }

    public ArrayList getAccountStockTRListHistoryMVNN(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI, String stockidsymbol, NNTrainObj nnTraining, String TRoutput, boolean lastDateOutput) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        ArrayList<String> writeAllArray = new ArrayList();
        if ((thObjListMACD == null) || (thObjListMV == null)) {
            return writeAllArray;
        }
        if (thObjListMACD.size() != thObjListMV.size()) {
            return writeAllArray;
        }

        NNTrainObj nnTr = new NNTrainObj();
        if (nnTraining != null) {
            nnTr = nnTraining;
        }
        ArrayList<NNInputOutObj> trInputList = new ArrayList();
        nnTr.setNnInputList(trInputList);

        StockTRHistoryObj prevThObj = null;
        NNInputOutObj prevInputList = null;

//        Collections.reverse(thObjListMACD);
//        Collections.reverse(thObjListMV);
//        Collections.reverse(thObjListRSI);
        boolean processLastDate = false;
        for (int i = 0; i < thObjListMV.size(); i++) {

            if (i + 1 == thObjListMV.size()) {
                if (lastDateOutput == true) {
                    processLastDate = true;
                }
            }
            NNInputOutObj inputList = new NNInputOutObj();

            StockTRHistoryObj thObjMV = thObjListMV.get(i);

            if (i == 0) {
                prevThObj = thObjMV;
            }

            int signal = thObjMV.getTrsignal();
            boolean contProcess = false;
            if (signal != prevThObj.getTrsignal()) {
                contProcess = true;
            }
            if (processLastDate == true) {
                contProcess = true;
            }

            if (contProcess == true) {

                inputList = NNProcessImp.getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);

                double parm1 = -1;
                if (signal == ConstantKey.S_BUY) {
                    parm1 = 0.9;
                } else if (signal == ConstantKey.S_SELL) {
                    parm1 = 0.1;
                }
                inputList.setInput1(parm1);
                inputList.setTrsignal(signal);

                ArrayList<Double> closeArray = NNProcessImp.getNNnormalizeInputClose(i, thObjListMACD);
                inputList.setInput6(closeArray.get(0));
                inputList.setInput7(closeArray.get(1));
                inputList.setInput8(closeArray.get(2));
                inputList.setInput9(closeArray.get(3));
                inputList.setInput10(closeArray.get(4));

                int retDecision = NNProcessImp.checkNNsignalDecision(thObjMV, prevThObj);

                double output = 0;
                if (retDecision == 1) {
                    output = 0.9;
                } else {
                    output = 0.1;
                }

                if (prevInputList != null) {
                    prevInputList.setOutput1(output);
                    trInputList.add(prevInputList);
                }
                prevThObj = thObjMV;
                prevInputList = inputList;
            }
        }// end of loop
        if (prevInputList != null) {
            if (lastDateOutput == true) {
                // eddy just for testing
//                trInputList.clear(); // clear so that only the last one
            }
            trInputList.add(prevInputList);
        }
        ArrayList writeArray = new ArrayList();
        String stTitle = "";
        int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use        
        for (int i = 0; i < trInputList.size(); i++) {
            NNInputOutObj obj = trInputList.get(i);
            String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                    + "\",\"" + obj.getOutput1()
                    + "\",\"" + obj.getInput1()
                    + "\",\"" + obj.getInput2()
                    + "\",\"" + obj.getInput3()
                    + "\",\"" + obj.getInput4()
                    + "\",\"" + obj.getInput5()
                    + "\",\"" + obj.getInput6()
                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                    //                    + "\",\"" + obj.getInput11() + "\",\"" + obj.getInput12()
                    + "\"";

            if (i == 0) {
                // seems still good for MV graph. So, use this data
//                st += ",\"last\"";
            }

            if (i + 1 >= trInputList.size()) {
                st += ",\"first\"";
            }

            if (i == 0) {
                stTitle = "\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                        + "\",\"" + "output"
                        + "\",\"" + "MV TSig"
                        + "\",\"" + "LTerm"
                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                        + "\",\"" + stockidsymbol + "\"";

            }
            writeArray.add(st);
            String stDispaly = st.replaceAll("\"", "");
            writeAllArray.add(stDispaly);

        }
        writeArray.add(stTitle);
        writeAllArray.add(stTitle.replaceAll("\"", ""));

        Collections.reverse(writeArray);
        Collections.reverse(writeAllArray);

        if (getEnv.checkLocalPC() == true) {
            String filename = FileLocalDebugPath + stockidsymbol + "_nn2_" + initTrainNeuralNetNumber + ".csv";
            FileUtil.FileWriteTextArray(filename, writeArray);
//            writeArrayNeuralNet.addAll(writeAllArray);
        }
        return writeAllArray;
    }

    public String getAccountStockTRListHistoryChart(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {
        ArrayList<StockTRHistoryObj> thObjList = this.getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
        return getAccountStockTRListHistoryChartProcess(thObjList, stockidsymbol, trname, pathSt);

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
            System.out.println("> getAccountStockTRListHistoryChartProcess exception" + ex.getMessage());
        }
        return "Save failed";

    }

    public byte[] getAccountStockTRLIstCurrentChartDisplay(String EmailUserName, String Password, String AccountIDSt, String stockidsymbol, String trname, String pathSt) {

        ArrayList<TransationOrderObj> thList = this.getAccountStockTranListByAccountID(EmailUserName, Password, AccountIDSt, stockidsymbol, trname, 0);

        if (thList == null) {
            // still allow to display dummy graph
//            return null;
            thList = new ArrayList();
        }
        String symbol = stockidsymbol;
        AFstockObj stock = this.getRealTimeStockImp(symbol);

        int size1year = 20 * 10;
        ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), size1year);
        if (StockArray == null) {
            return null;
        }

        Collections.reverse(StockArray);
        Collections.reverse(thList);

        List<Date> xDate = new ArrayList<Date>();
        List<Double> yD = new ArrayList<Double>();

        List<Date> buyDate = new ArrayList<Date>();
        List<Double> buyD = new ArrayList<Double>();
        List<Date> sellDate = new ArrayList<Date>();
        List<Double> sellD = new ArrayList<Double>();

        List<Date> noDate = new ArrayList<Date>();
        List<Double> noD = new ArrayList<Double>();

        for (int j = 0; j < StockArray.size(); j++) {
            AFstockInfo stockinfo = StockArray.get(j);

            Date da = new Date(stockinfo.getEntrydatel());
            xDate.add(da);
            long stockdatel = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
            float close = stockinfo.getFclose();
            double norClose = close;
//            if (j == 0) {
//                norClose = 0;
//            }
            yD.add(norClose);

            if (j == 0) {
                noDate.add(da);
                noD.add(norClose);
            }

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
        if (buyDate.size() == 0) {
            buyDate = noDate;
            buyD = noD;
        }
        if (sellDate.size() == 0) {
            sellDate = noDate;
            sellD = noD;
        }
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
                if (getEnv.checkLocalPC() == true) {
                    if (trname.equals(ConstantKey.TR_NN1) || trname.equals(ConstantKey.TR_NN2)) {

                        int TR_Name = ConstantKey.INT_TR_NN1;
                        if (trname.equals(ConstantKey.TR_NN2)) {
                            TR_Name = ConstantKey.INT_TR_NN2;
                        }

                        int size1yearAll = 20 * 12 * 5 + (50 * 3);
                        ArrayList<AFstockInfo> StockArray = this.getStockHistorical(stock.getSymbol(), size1yearAll);
                        if (StockArray == null) {
                            return null;
                        }

                        AccountObj accountObj = this.getAdminObjFromCache();
                        ArrayList UpdateTRList = this.SystemAccountStockListByAccountID(accountObj.getId(), stock.getSymbol());

                        int size1year = 20 * 10;
                        for (int j = 0; j < size1year; j++) {
                            int stockOffset = size1year - j + 1;
                            AFstockInfo stockinfo = StockArray.get(stockOffset);
                            // testing check predicion evey date in stock
                            // testing check predicion evey date in stock                    
                            boolean flag_1 = false;
                            if (flag_1 == true) {
                                NNObj nn = NNCal.NNpredict(this, TR_Name, accountObj, stock, UpdateTRList, StockArray, stockOffset);
                                if (nn != null) {
                                    String nameST = nn.getComment();
                                    NNInputOutObj obj = new NNInputOutObj();

                                    try {
                                        obj = new ObjectMapper().readValue(nameST, NNInputOutObj.class
                                        );
                                    } catch (IOException ex) {
                                    }
                                    obj.setClose(stockinfo.getFclose());
                                    obj.setOutput1(nn.getPrediction());

                                    Calendar setDate = TimeConvertion.getCurrentCalendar(stockinfo.getEntrydatel());
                                    String stdate = new Timestamp(setDate.getTime().getTime()).toString();
                                    stdate = stdate.substring(0, 10);
                                    obj.setDateSt(stdate);

                                    inputObjlist.add(obj);
                                    String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                                            + "\",\"" + obj.getOutput1()
                                            + "\",\"" + obj.getInput1() + "\",\"" + obj.getInput2() + "\",\"" + obj.getInput3()
                                            + "\",\"" + obj.getInput4() + "\",\"" + obj.getInput5() + "\",\"" + obj.getInput6()
                                            + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                                            + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                                            + "\"";
                                    logger.info(st);
                                }
                            }

                            long stockdatel = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
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
                                //StockArray assume recent date to old data  
                                //StockArray assume recent date to old data  
                                //trainingNN1dataMACD will return oldest first to new date
                                //trainingNN1dataMACD will return oldest first to new date   
                                ArrayList<NNInputDataObj> inputDataObj = null;
                                if (TR_Name == ConstantKey.INT_TR_NN1) {

                                    //StockArray assume recent date to old data  
                                    //StockArray assume recent date to old data              
                                    //trainingNN1dataMACD will return oldest first to new date
                                    //trainingNN1dataMACD will return oldest first to new date            
                                    ProcessNN1 nn1 = new ProcessNN1();
                                    inputDataObj = nn1.trainingNN1dataMACD1(this, symbol, StockArray, stockOffset, CKey.SHORT_MONTH_SIZE);
                                } else if (TR_Name == ConstantKey.INT_TR_NN2) {

                                    ProcessNN2 nn2 = new ProcessNN2();
                                    inputDataObj = nn2.trainingNN2dataMACD(this, symbol, StockArray, stockOffset, CKey.SHORT_MONTH_SIZE);
                                }

                                // this assume from the oldest to new date no need reverse
                                NNInputOutObj obj = (NNInputOutObj) inputDataObj.get(0).getObj();

                                int retDecision = TradingNNprocess.checkNNsignalDecision(obj, objPrev);

                                double output = 0;
                                if (retDecision == 1) {
                                    output = 0.9;
                                } else {
                                    output = 0.1;
                                }
                                if (objPrev != null) {
                                    objPrev.setOutput1(output);
                                }
                                if (objPrev != null) {
                                    inputObjlist.add(objPrev);

                                    String st = "\"" + objPrev.getDateSt() + "\",\"" + objPrev.getClose() + "\",\"" + objPrev.getTrsignal()
                                            + "\",\"" + objPrev.getOutput1()
                                            + "\",\"" + objPrev.getInput1() + "\",\"" + objPrev.getInput2() + "\",\"" + objPrev.getInput3()
                                            + "\",\"" + objPrev.getInput4() + "\",\"" + objPrev.getInput5() + "\",\"" + objPrev.getInput6()
                                            + "\",\"" + objPrev.getInput7() + "\",\"" + objPrev.getInput8()
                                            + "\",\"" + objPrev.getInput9() + "\",\"" + objPrev.getInput10()
                                            + "\"";
                                    logger.info(st);
                                }

                                objPrev = obj;
                                break;
                            }
                            boolean exitflag = false;
                            if (exitflag == true) {
                                break;
                            }

                        }

                        ArrayList writeArray = new ArrayList();
                        String stTitle = "";
                        for (int i = 0; i < inputObjlist.size(); i++) {
                            NNInputOutObj obj = (NNInputOutObj) inputObjlist.get(i);
                            String st = "\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                                    + "\",\"" + obj.getOutput1()
                                    + "\",\"" + obj.getInput1() + "\",\"" + obj.getInput2() + "\",\"" + obj.getInput3()
                                    + "\",\"" + obj.getInput4() + "\",\"" + obj.getInput5() + "\",\"" + obj.getInput6()
                                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                                    + "\"";

                            if (i == 0) {
                                stTitle = "\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                                        + "\",\"" + "output"
                                        + "\",\"" + "macd TSig"
                                        + "\",\"" + "LTerm"
                                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                                        + "\"";

                            }
                            writeArray.add(st);

                        }
                        writeArray.add(stTitle);
                        Collections.reverse(writeArray);
                        String filename = FileLocalDebugPath + stockidsymbol + "_nn_display.csv";
                        FileUtil.FileWriteTextArray(filename, writeArray);
                    }
                }  // local PC
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
            System.out.println("> getAccountStockTRLIstCurrentChartFile exception" + ex.getMessage());
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getAccountStockTRListHistory(EmailUserName, Password, AccountIDSt, stockidsymbol, trname);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().updateAccountStockSignal(stockTRObj);
        }

        return getAccountImp().updateAccountStockSignal(stockTRObj.getTrlist());

    }

    public int addAccountStock(String EmailUserName, String Password, String AccountIDSt, String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().addAccountStock(EmailUserName, Password, AccountIDSt, symbol);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().removeAccountStock(EmailUserName, Password, AccountIDSt, symbol);
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

    public int removeAccountStockSymbol(AccountObj accountObj, String symbol) {

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stockObj = getStockImp().getRealTimeStock(NormalizeSymbol, null);
        if (stockObj != null) {

            int signal = ConstantKey.S_NEUTRAL;
            String trName = ConstantKey.TR_ACC;
            TradingRuleObj tradingRuleObj = SystemAccountStockIDByTRname(accountObj.getId(), stockObj.getId(), trName);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().addStock(symbol);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().DeleteStockInfo(symbol);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().disableStock(symbol);
        }
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        return getStockImp().disableStock(NormalizeSymbol);
    }

    public AFstockObj getRealTimeStockImp(String symbol) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getRealTimeStockImp(symbol);
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

    public ArrayList<AFstockInfoDisplay> getStockHistoricalDisplay(String symbol, int length) {

        ArrayList<AFstockInfoDisplay> dspList = new ArrayList();
        ArrayList<AFstockInfo> stockInfList = getStockHistorical(symbol, length);
        for (int i = 0; i < stockInfList.size(); i++) {
            AFstockInfo stockInf = stockInfList.get(i);
            AFstockInfoDisplay dsp = new AFstockInfoDisplay();
            dsp.setStockInfoObj(stockInf);

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            Date d = new Date(stockInf.getEntrydatel());
            DateFormat format = new SimpleDateFormat("M/dd/yyyy");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);
            dsp.setUpdateDateD(ESTdate);
            dspList.add(dsp);
        }

        return dspList;

    }

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
        long endDay = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
        long start = endDay;
        long end = 0;

        while (mergedList.size() < length) {
            long endDay100 = TimeConvertion.addDays(start, -100);
            end = TimeConvertion.endOfDayInMillis(endDay100);
            ArrayList<AFstockInfo> stockInfoArray = getStockHistoricalRange(NormalizeSymbol, start, end);
            if (stockInfoArray == null) {
                break;
            }
            if (stockInfoArray.size() == 0) {
                break;
            }
            mergedList.addAll(stockInfoArray);
            start = TimeConvertion.addMiniSeconds(end, -10);
        }
        if (mergedList.size() == 0) {
            return (ArrayList) mergedList;
        }
        if (length < 22) {
            ArrayList<AFstockInfo> sockInfoArray = new ArrayList<AFstockInfo>(mergedList);
            ArrayList<AFstockInfo> retArray = new ArrayList();
            for (int i = 0; i < length; i++) {
                AFstockInfo sInfo = sockInfoArray.get(i);
                retArray.add(sInfo);
            }
            return retArray;
        }
        boolean primarySt = false;
//        for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
//            String stockN = ServiceAFweb.primaryStock[i];
//            if (stockN.equals(NormalizeSymbol.toUpperCase())) {
//                primarySt = true;
//                break;
//            }
//        }
        if (primarySt == false) {
            // assume yahoo finance is working.
            // save only the last 10 to save memory 10M only in Clever Cloud 
            // always the earliest day first
            StockInternet internet = new StockInternet();
            ArrayList<AFstockInfo> StockArray = internet.GetStockHistoricalInternet(NormalizeSymbol, length);
            if (StockArray == null) {
                ///////seems internet error
                logger.info("getStockHistorical internet error " + NormalizeSymbol);
                return null;
            }
            if (StockArray.size() == 0) {
                return StockArray;
            }
            AFstockInfo mergeInfo = mergedList.get(0);
            long mergeInfoEOD = TimeConvertion.endOfDayInMillis(mergeInfo.getEntrydatel());
            AFstockInfo StockInfo = StockArray.get(0);
            long StockInfoEOD = TimeConvertion.endOfDayInMillis(StockInfo.getEntrydatel());
            if (mergeInfoEOD == StockInfoEOD) {
                StockArray.remove(0);
                StockArray.add(0, mergedList.get(0));
            } else {
                StockArray.add(mergedList.get(0));
            }
            mergedList = StockArray;

        }

        return (ArrayList) mergedList;
    }

//    public ArrayList getStockHistorical_old(String symbol, int length) {
//        if (getServerObj().isSysMaintenance() == true) {
//            return null;
//        }
//        if (checkCallRemoveMysql() == true) {
//            return getServiceAFwebREST().getStockHistorical(symbol, length);
//        }
//        SymbolNameObj symObj = new SymbolNameObj(symbol);
//        String NormalizeSymbol = symObj.getYahooSymbol();
//        ArrayList stockInfoArray = getStockImp().getStockHistorical(NormalizeSymbol, length, null);
//
//        return stockInfoArray;
//    }
    public ArrayList getAllOpenStockNameArray() {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getStockNameArray();
        }
        ArrayList stockNameList = getStockImp().getOpenStockNameArray();
        return stockNameList;
    }

    public ArrayList getStockArray(int length) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getStockArray(length);
        }
        ArrayList stockList = getStockImp().getStockArray(length);
        return stockList;
    }

    ////////////////////////
    public ArrayList getAllLock() {

        ArrayList result = null;

        if (checkCallRemoteMysql() == true) {
            result = getServiceAFwebREST().getAllLock();
        } else {
            result = getStockImp().getAllLock();
        }

        return result;
    }

    public int setRenewLock(String symbol_acc, int type) {

        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setRenewLock(symbol_acc, type);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getLockName(symbol_acc, type);
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

        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setLockName(symbol_acc, type, lockdatel, comment);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().removeNameLock(symbol_acc, type);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().releaseNeuralNetObj(name);
        }

//        return getStockImp().releaseNeuralNetObj(name);
        return getStockImp().releaseNeuralNetBPObj(name);
    }

    public AFneuralNet getNeuralNetObjWeight0(String name, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getNeuralNetObjWeight0(name, type);
        }

        return getStockImp().getNeuralNetObjWeight0(name);
    }

    public AFneuralNet getNeuralNetObjWeight1(String name, int type) {
        if (getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().getNeuralNetObjWeight1(name, type);
        }

        return getStockImp().getNeuralNetObjWeight1(name);
    }

    public int setNeuralNetObjWeight0(AFneuralNet nn) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setNeuralNetObjWeight0(nn);
        }

        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty
        return getStockImp().setCreateNeuralNetObj0(nn.getName(), nn.getWeight());
    }

    public int setNeuralNetObjWeight1(AFneuralNet nn) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().setNeuralNetObjWeight1(nn);
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
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().updateStockInfoTransaction(stockInfoTran);
        }

        return getStockImp().updateStockInfoTransaction(stockInfoTran);
    }

    //http://localhost:8080/cust/admin1/sys/cust/eddy/update?substatus=10&investment=0&balance=15
    public int updateCustAllStatus(String customername,
            String substatusSt, String investmentSt, String balanceSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().updateCustAllStatus(customername, substatusSt, investmentSt, balanceSt);
        }
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {

            int substatus = Integer.parseInt(substatusSt);
            float investment = Float.parseFloat(investmentSt);
            float balance = Float.parseFloat(balanceSt);
            return getAccountImp().updateCustAllStatus(UserName, substatus, investment, balance);

        } catch (Exception e) {
        }
        return 0;
    }

//http://localhost:8080/cust/admin1/sys/cust/eddy/status/0/substatus/0
    public int updateCustStatusSubStatus(String customername, String statusSt, String substatusSt) {
        if (getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        if (checkCallRemoteMysql() == true) {
            return getServiceAFwebREST().updateCustStatusSubStatus(customername, statusSt, substatusSt);
        }

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
        return getAccountImp().updateCustStatus(custObj);
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
        fundmgr.ProcessIISWebGlobalFundMgr(this);
        fundmgr.ProcessFundMgrAccount(this);
        return true;
    }

    public String SystemCleanDBData() {
        boolean retSatus = false;
        if (getServerObj().isLocalDBservice() == true) {
            serverObj.setSysMaintenance(true);
            retSatus = getStockImp().cleanStockDB();
        }
        return "" + retSatus;
    }

    public String SystemClearLock() {
        int retSatus = 0;
        if (getServerObj().isLocalDBservice() == true) {
            retSatus = getStockImp().deleteAllLock();
        }
        return "" + retSatus;
    }

    public String SystemRestDBData() {
        boolean retSatus = false;
        if (getServerObj().isLocalDBservice() == true) {
            // make sure the system is stopped first
            retSatus = getStockImp().restStockDB();
        }
        return "" + retSatus;
    }

    public String SystemClearNNinput() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;
        if (getServerObj().isLocalDBservice() == true) {
            retSatus = NNProcessImp.ClearStockNNinputNameArray(this, ConstantKey.TR_NN1);
            retSatus = NNProcessImp.ClearStockNNinputNameArray(this, ConstantKey.TR_NN2);

        }
        return "" + retSatus;
    }

    public String SystemRetrainNN() {
        int retSatus = 0;
        logger.info(">SystemRetrainNN retraining... ");
        TradingNNprocess trainNN = new TradingNNprocess();
        trainNN.retrainStockNNprocessNameArray(this);
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
        if (getServerObj().isLocalDBservice() == true) {
            retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(this, ConstantKey.TR_ACC, "");
        }
        return "" + retSatus;
    }

    public String SystemClearNNtran() {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;
        if (getServerObj().isLocalDBservice() == true) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MACD);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_MV);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_RSI);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN1);
            retSatus = NNProcessImp.ClearStockNNTranHistory(this, ConstantKey.TR_NN2);
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

                newCustomer.setUsername("GUEST");
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
            }

            newCustomer.setUsername("EDDY");
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
        logger.info(">InitDB InitSystemData Stock to account ");
        AccountObj account = getAccountImp().getAccountByType("GUEST", "guest", AccountObj.INT_TRADING_ACCOUNT);

        // make sure both ServiceAFweb.InitSystemData and StockImp.initStockDB are update
        if (account != null) {
            AFstockObj stock = getStockImp().getRealTimeStock("SPY", null);
            int result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);

            stock = getStockImp().getRealTimeStock("DIA", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("QQQ", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("HOU.TO", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("HOD.TO", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("T.TO", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("FAS", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("FAZ", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("XIU.TO", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("RY.TO", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
            stock = getStockImp().getRealTimeStock("AAPL", null);
            result = getAccountImp().addAccountStockId(account, stock.getId(), TRList);
        }
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        TRprocessImp.InitSystemData();
        getAccountProcessImp().InitSystemData();

    }

    public static String getSQLLengh(String sql, int length) {
        //https://www.petefreitag.com/item/59.cfm
        //SELECT TOP 10 column FROM table - Microsoft SQL Server
        //SELECT column FROM table LIMIT 10 - PostgreSQL and MySQL
        //SELECT column FROM table WHERE ROWNUM <= 10 - Oracle
        if ((CKey.SQL_DATABASE == CKey.MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
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
