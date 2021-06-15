/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.stock;

import com.afweb.account.AccountTranProcess;
import com.afweb.account.CommMsgImp;
import com.afweb.model.ConstantKey;
import com.afweb.model.StockInfoTranObj;
import com.afweb.model.account.AccountObj;
import com.afweb.model.account.CommData;
import com.afweb.model.account.TradingRuleObj;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.model.stock.AFstockObj;
import com.afweb.model.stock.StockData;
import com.afweb.service.ServiceAFweb;
import com.afweb.signal.ADXObj;
import com.afweb.signal.NNCal;
import com.afweb.signal.NNObj;
import com.afweb.signal.TechnicalCal;
import com.afweb.signal.TradingSignalProcess;
import com.afweb.util.CKey;
import com.afweb.util.DateUtil;
import com.afweb.util.TimeConvertion;
import com.afweb.util.getEnv;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class StockProcess {

    protected static Logger logger = Logger.getLogger("StockProcess");

//    private ServiceAFweb serviceAFWeb = null;
    private static int timerCnt = 0;
    private static ArrayList stockUpdateNameArray = new ArrayList();
    private static ArrayList stockSignalNameArray = new ArrayList();

    public void InitSystemData() {
        timerCnt = 0;
        stockUpdateNameArray = new ArrayList();
        stockSignalNameArray = new ArrayList();
    }

    public void ProcessAdminSignalTrading(ServiceAFweb serviceAFWeb) {
//        logger.info("> ProcessAdminSignalTrading ");
//        this.serviceAFWeb = serviceAFWeb;
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        UpdateStockSignalNameArray(serviceAFWeb, accountAdminObj);
        if (stockSignalNameArray == null) {
            return;
        }
        if (stockSignalNameArray.size() == 0) {
            return;
        }

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "ADM_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");

        long lockReturn = 1;
        lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");

        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }
        logger.info("ProcessAdminSignalTrading " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {

            long currentTime = System.currentTimeMillis();
            long lockDate5Min = TimeConvertion.addMinutes(currentTime, 5);
            logger.info("ProcessAdminSignalTrading for 3 minutes stocksize=" + stockSignalNameArray.size());

            for (int i = 0; i < 10; i++) {
                currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
                    break;
                }
                if (stockSignalNameArray.size() == 0) {
                    break;
                }

                try {
                    String symbol = (String) stockSignalNameArray.get(0);
                    stockSignalNameArray.remove(0);

                    if (ServiceAFweb.mydebugtestNN3flag == true) {
                        if (ServiceAFweb.checkSymbolDebugTest(symbol) == false) {
                            continue;
                        }
                    }
                    AFstockObj stock = serviceAFWeb.getStockRealTime(symbol);
                    if (stock != null) {
                        if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                            logger.info("> ProcessAdminSignalTrading return stock split " + symbol);
                            return;
                        }
                    }
                    if (stock == null) {
                        logger.info("> ProcessAdminSignalTrading return stock null ");
                        continue;
                    }

                    String LockStock = "ADM_" + symbol;
                    LockStock = LockStock.toUpperCase();

                    long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                    long lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");
                    if (testing == true) {
                        lockReturnStock = 1;
                    }
//                    logger.info("ProcessAdminSignalTrading " + LockStock + " LockStock " + lockReturnStock);
                    if (lockReturnStock > 0) {

                        boolean ret = serviceAFWeb.checkStock(serviceAFWeb, symbol);
                        if (ret == true) {

                            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountAdminObj.getId(), stock.getId(), ConstantKey.TR_NN1);
                            if (trObj != null) {
                                long lastUpdate = trObj.getUpdatedatel();
                                long lastUpdate5Min = TimeConvertion.addMinutes(lastUpdate, 5);

                                long curDateValue = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                                if (testing == true) {
                                    lastUpdate5Min = 0;
                                }
                                if (lastUpdate5Min < curDateValue) {
                                    // process only if within 5 minutes on the last update
                                    // so that it will not do it so often
                                    TradingSignalProcess TSprocess = new TradingSignalProcess();
                                    TSprocess.updateAdminTradingsignal(serviceAFWeb, accountAdminObj, symbol);
                                    TSprocess.upateAdminTransaction(serviceAFWeb, accountAdminObj, symbol);
                                    TSprocess.upateAdminPerformance(serviceAFWeb, accountAdminObj, symbol);
                                    TSprocess.upateAdminTRPerf(serviceAFWeb, accountAdminObj, symbol);
                                }
                            }
                        }

                        serviceAFWeb.removeNameLock(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
//                        logger.info("ProcessAdminSignalTrading " + LockStock + " unLock LockStock ");
                    }
                } catch (Exception ex) {
                    logger.info("> ProcessAdminSignalTrading Exception" + ex.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
            logger.info("ProcessAdminSignalTrading " + LockName + " unlock LockName");
        }
    }

    private ArrayList UpdateStockSignalNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        if (stockSignalNameArray != null && stockSignalNameArray.size() > 0) {
            return stockSignalNameArray;
        }

        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            stockSignalNameArray = stockNameArray;
        }
//        logger.info("> UpdateStockSignalNameArray stocksize=" + stockSignalNameArray.size());

        return stockSignalNameArray;
    }

    public int calculateTrend(ServiceAFweb serviceAFWeb, AFstockObj stock, long dateNowL) {
        if (stock == null) {
            return 0;
        }
        int size1year = 5 * 52;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        if (StockArray == null) {
            return 0;
        }
        if (StockArray.size() < 10) {
            return 0;
        }
        int offset = AccountTranProcess.getOffetDate(StockArray, dateNowL);

        float STerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
        float LTerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
        ADXObj adx = TechnicalCal.AvgDir(StockArray, offset, 14);

        int iSTerm = (int) STerm;
        int iLTerm = (int) LTerm;
        stock.setLongterm(iLTerm);
        stock.setShortterm(iSTerm);
        stock.setDirection((float) adx.trsignal);

        updateStockRecommendation(serviceAFWeb, stock, StockArray);

        return 1;
    }

    ///////////////
    public static int stockPass = 0;
    public static int stockFail = 0;

    public void ResetStockUpdateNameArray(ServiceAFweb serviceAFWeb) {
//        this.serviceAFWeb = serviceAFWeb;
        stockUpdateNameArray.clear();
        updateStockUpdateNameArray(serviceAFWeb);
    }

    private ArrayList updateStockUpdateNameArray(ServiceAFweb serviceAFWeb) {
        if (stockUpdateNameArray != null && stockUpdateNameArray.size() > 0) {
            return stockUpdateNameArray;
        }
        ArrayList stockNameArray = serviceAFWeb.getAllOpenStockNameArray();
        if (stockNameArray != null) {
            stockUpdateNameArray = stockNameArray;
        }
        stockPass = 0;
        stockFail = 0;
        return stockUpdateNameArray;
    }

    public int UpdateAllStock(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.javamainflag == true) {
            return UpdateAllStockTrend(serviceAFWeb, true);
        }
        return UpdateAllStockTrend(serviceAFWeb, false);
    }

    public int UpdateAllStockTrend(ServiceAFweb serviceAFWeb, boolean updateTrend) {
        ServiceAFweb.lastfun = "UpdateAllStock";

        //SimpleDateFormat etDf = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mma 'ET'");
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        int hr = DateUtil.getHourNow();
        String marketClose = "Market Close";
        boolean mkopen = DateUtil.isMarketOpen();
//        if (hr < 21) { // < 9:00 pm) {
//            if (hr > 8) {  // > 8:00 pm
//                if (dayOfW <= 5) {
//                    mkopen = true;
//                }
//            }
//        }
        if (mkopen == true) {
            marketClose = "Market Open";
        }

        if (mkopen == false) {  //
            String LockName = "MK_CLOSE_" + ServiceAFweb.getServerObj().getServerName();

            int lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_updateAllStockProcess_" + hr);
//            if (CKey.NN_DEBUG == true) {
//                lockReturn = 1;
//            }
            if (lockReturn == 0) {
                //no unlock and just wait for 90 minutes
                return 0;
            }
            logger.info("UpdateAllStock marke close " + hr);
        }
        updateStockUpdateNameArray(serviceAFWeb);
        int result = 0;
        try {
            if ((stockUpdateNameArray == null) || (stockUpdateNameArray.size() == 0)) {
                return 0;
            }
            logger.info("UpdateAllStock for 1 minutes " + marketClose + " time=" + hr
                    + " stocksize=" + stockUpdateNameArray.size() + " P:" + stockPass + " F:" + stockFail);

            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(currentTime, 1);

            for (int i = 0; i < 10; i++) {
                currentTime = System.currentTimeMillis();
                if (CKey.NN_DEBUG != true) {
                    if (lockDate1Min < currentTime) {
                        break;
                    }
                }
                if (stockUpdateNameArray.size() == 0) {
                    break;
                }

                String NormalizeSymbol = (String) stockUpdateNameArray.get(0);
                stockUpdateNameArray.remove(0);
                result = updateAllStockProcess(serviceAFWeb, NormalizeSymbol, updateTrend);
                if (result == 1) {
                    stockPass++;
                } else {
                    stockFail++;
                }
            }
        } catch (Exception ex) {
        }
        return result;
    }

//    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
//        AFstockObj stock = serviceAFWeb.getStockRealTime(NormalizeSymbol);
//        if (stock == null) {
//            return false;
//        }
//        if (stock.getStatus() != ConstantKey.OPEN) {
//            return false;
//        }
//        if (stock.getAfstockInfo() == null) {
//            return false;
//        }
//        return true;
//    }
    public int updateAllStockProcess(ServiceAFweb serviceAFWeb, String NormalizeSymbol, boolean updateTrend) {
        ServiceAFweb.lastfun = "updateAllStockProcess";

//        logger.warning("> updateAllStock " + NormalizeSymbol);
        AFstockObj stock = null;
        // eddy testing
        // yahoo crumb fail not working

        try {
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long currentdate = dateNow.getTimeInMillis();

            stock = serviceAFWeb.getStockRealTime(NormalizeSymbol);
            if (stock == null) {
                return 0;
            }
            if (stock.getStatus() != ConstantKey.OPEN) {
                return 0;
            }
            long lastUpdate = stock.getUpdatedatel();
            long lastUpdate5Min = TimeConvertion.addMinutes(lastUpdate, 5);

            long lockDateValue = TimeConvertion.getCurrentCalendar().getTimeInMillis();
            boolean flagEnd = false;
            if (flagEnd == true) {
                lastUpdate5Min = 0;
            }
            if (lastUpdate5Min < lockDateValue) {

                int lockReturn = serviceAFWeb.setLockNameProcess(NormalizeSymbol, ConstantKey.STOCK_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_updateAllStockProcess");
                if (CKey.NN_DEBUG == true) {
                    lockReturn = 1;
                }
                if (lockReturn > 0) {
//                    logger.info("updateAllStockProcess =" + NormalizeSymbol);

                    // get real time stock from internet and update shock DB
                    int resultUpdate = updateRealTimeStock(serviceAFWeb, stock);

                    stock = serviceAFWeb.getStockRealTime(NormalizeSymbol);
                    if (stock == null) {
                        logger.info("> updateAllStockProcess " + NormalizeSymbol + " data:" + stock.getData());
                        return 0;
                    }
                    if (resultUpdate > 0) {

                        //work around to fix AAPL or APPL not update but valid
                        long lastupdate1 = stock.getUpdatedatel();
                        long lastUpdate5Day = TimeConvertion.addDays(lastupdate1, 5); // 5 days

                        if (lastUpdate5Day > currentdate) {

                            if (updateTrend == true) {
                                //////// Update Long and short term trend 
                                int resultCalcuate = calculateTrend(serviceAFWeb, stock, 0);
//                                logger.info("> updateAllStock " + NormalizeSymbol + " data:" + stock.getData());
                                // udpate other trends 
                            }

                            // send SQL update
                            String sockUpdateSQL = StockDB.SQLupdateStockSignal(stock);
                            ArrayList sqlList = new ArrayList();
                            sqlList.add(sockUpdateSQL);
                            serviceAFWeb.SystemUpdateSQLList(sqlList);

                            serviceAFWeb.removeNameLock(NormalizeSymbol, ConstantKey.STOCK_LOCKTYPE);
                            return 1;
                        }

                    }
                    if (ServiceAFweb.mydebugtestflag == true) {
                        logger.info("> updateAllStock resultUpdate fail " + NormalizeSymbol);
                    }
                    // update fail do not remove lock
                    int failCnt = stock.getFailedupdate() + 1;
                    // too many failure
                    if (failCnt > CKey.FAIL_STOCK_CNT) {
                        stock.setStatus(ConstantKey.DISABLE);
                    }
                    stock.setFailedupdate(failCnt);
                    //send SQL update
                    String sockUpdateSQL = StockDB.SQLupdateStockStatus(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockUpdateSQL);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                } else {
                    if (ServiceAFweb.mydebugtestflag == true) {
                        logger.info("> updateAllStock lockReturn fail " + lockDateValue + " " + NormalizeSymbol);
                    }
                }
                return 0;
            } else {
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> updateAllStock exception " + ex.getMessage());
        }
        return 0;
    }

    public int updateStockRecommendation(ServiceAFweb serviceAFWeb, AFstockObj stock, ArrayList StockArray) {
        if (stock == null) {
            return 0;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        // reduce server processing
        if (getEnv.checkLocalPC() == false) {
            return 0;
        }
        String dataSt = stock.getData();

        StockData stockData = new StockData();

        try {
            if ((dataSt != null) && (dataSt.length() > 0)) {
                dataSt = dataSt.replaceAll("#", "\"");
                stockData = new ObjectMapper().readValue(dataSt, StockData.class);
            }

        } catch (Exception ex) {
        }
/////////////////////////////        
        try {
            String trname = ConstantKey.TR_NN1;
            if (stock != null) {
                stock.setTrname(trname);
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                ArrayList<TradingRuleObj> trObjList = serviceAFWeb.getAccountImp().getAccountStockTRListByAccountID(accountAdminObj.getId(), stock.getId());
                if (trObjList != null) {
                    if (trObjList.size() != 0) {

                        for (int j = 0; j < trObjList.size(); j++) {
                            TradingRuleObj trObj = trObjList.get(j);
                            if (trname.equals(trObj.getTrname())) {

                                stock.setTRsignal(trObj.getTrsignal());
                                float totalPercent = serviceAFWeb.getAccountStockRealTimeBalance(trObj);
                                stock.setPerform(totalPercent);
                                break;
                            }
                        }
                    }
                }

                stockData.setRec(0);
                if (stock.getPerform() > 40) {  // greater than 20 %
                    stockData.setRec(2);
                } else if (stock.getPerform() > 20) {  // greater than 20 %
                    stockData.setRec(1);
                }

                stockData.setUpDn((int) stock.getLongterm());
                stockData.setChDir((int) stock.getDirection());

                if (stock.getLongterm() > 60) {
                    stockData.setTop(1);
                } else if (stock.getLongterm() < -60) {
                    stockData.setTop(-1);
                }

                stockData.setpCl(0);
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN30, accountAdminObj, stock, StockArray, 0);
                if (nn != null) {

                    float output1 = nn.getOutput1();
                    float output2 = nn.getOutput2();
                    if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {

                        AFstockInfo stockinfo = (AFstockInfo) StockArray.get(0);
                        float closeOutput0 = stockinfo.getFclose();
                        float closeOutput = closeOutput0;
                        if (CKey.PREDICT_THRESHOLD < output1) {
                            float closef = (float) 0.9;
                            closef = closef / 15;
                            closeOutput = (closef * closeOutput0) + closeOutput0;
                        } else if (CKey.PREDICT_THRESHOLD < output2) {
                            float closef = (float) -0.9;
                            closef = closef / 15;
                            closeOutput = (closef * closeOutput0) + closeOutput0;
                        }
                        stockData.setpCl(closeOutput);
                    }
                }

            } // end if stock null
//////////////////////
            String sdataStr = new ObjectMapper().writeValueAsString(stockData);
            sdataStr = sdataStr.replaceAll("\"", "#");
            stock.setData(sdataStr);
            return 1;
        } catch (Exception ex) {
        }
        return 0;
    }

//    public int updateRealTimeStockTest(ServiceAFweb serviceAFWeb, AFstockObj stock) {
////        this.serviceAFWeb = serviceAFWeb;
//        return updateRealTimeStock(serviceAFWeb, stock);
//    }
    public int updateRealTimeStock(ServiceAFweb serviceAFWeb, AFstockObj stock) {
        ServiceAFweb.lastfun = "updateRealTimeStock";

//        logger.warning("> updateRealTimeStock ");
        if (stock == null) {
            return 0;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return 0;
        }
        String NormalizeSymbol = stock.getSymbol();
        StockInternet stockInternet = new StockInternet();
//        logger.warning("> updateRealTimeStock " + NormalizeSymbol);
        try {
            // get realtime from internet
            AFstockObj stockRTinternet = stockInternet.GetRealTimeStockInternet(NormalizeSymbol);
            if (stockRTinternet != null) {
                stock.setUpdatedatedisplay(stockRTinternet.getUpdatedatedisplay());
                stock.setUpdatedatel(stockRTinternet.getUpdatedatel());
                stock.setAfstockInfo(stockRTinternet.getAfstockInfo());
                stock.setDirection(timerCnt);
                if (!stock.getStockname().equals(stockRTinternet.getStockname())) {
                    stock.setStockname(stockRTinternet.getStockname());
                    String sockNameSQL = StockDB.SQLupdateStockName(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockNameSQL);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                }
                int internetHistoryLen = 0;

                int size1yearAll = 20;
                ArrayList<AFstockInfo> StockArrayHistory = serviceAFWeb.getStockHistorical(NormalizeSymbol, size1yearAll);
                if ((StockArrayHistory == null) || (StockArrayHistory.size() == 0)) {
                    ;
                } else {
                    AFstockInfo stockinfoDB = stock.getAfstockInfo();
                    if (stockinfoDB != null) {
                        long lastUpdate = stockinfoDB.getEntrydatel();
                        long lastUpdate5Mon = TimeConvertion.addMonths(lastUpdate, 5);
                        Calendar dateNow = TimeConvertion.getCurrentCalendar();
                        long dateValue = dateNow.getTimeInMillis();
                        if (lastUpdate5Mon > dateValue) {
                            internetHistoryLen = 20 * 6; // 6 month;
                        }
                    }
                }
                // eddy testing
                // yahoo crumb fail not working
                boolean flagEnd = false;
                if (flagEnd == true) {
                    return 1;
                }
                if (CKey.GET_STOCKHISTORY_SCREEN == true) {
                    // temporary fix the yahoo finance cannot get history
                    // temporary fix the yahoo finance cannot get history
                    // temporary fix the yahoo finance cannot get history
                    if (internetHistoryLen == 0) {
                        return 0;
                    }
                }
                // always the earliest day first
                ArrayList<AFstockInfo> StockArray = stockInternet.GetStockHistoricalInternet(NormalizeSymbol, internetHistoryLen);

                if (StockArray == null || StockArray.size() < 20 * 2) { // 2 month
                    if (StockArray != null) {
                        logger.info("updateRealTimeStock " + NormalizeSymbol + " size " + StockArray.size());
                    }
                    return 0;
                }

                if (stockRTinternet != null) {
                    // check for stock split
                    // check for stock split

//                    StockArrayHistory It has done early
//                    int size1yearAll = 20;
//                    ArrayList<AFstockInfo> StockArrayHistory = serviceAFWeb.getStockHistorical(NormalizeSymbol, size1yearAll);
                    if ((StockArrayHistory != null) && (StockArrayHistory.size() > 10)) {
                        AFstockInfo stockInfoHistory = StockArrayHistory.get(0);
                        boolean ret = this.checkStockSplit(serviceAFWeb, stock, StockArray, StockArrayHistory, stockInfoHistory, NormalizeSymbol);
                        if (ret == false) {
                            stockInfoHistory = StockArrayHistory.get(1);
                            ret = this.checkStockSplit(serviceAFWeb, stock, StockArray, StockArrayHistory, stockInfoHistory, NormalizeSymbol);
                            if (ret == false) {
                                stockInfoHistory = StockArrayHistory.get(2);
                                ret = this.checkStockSplit(serviceAFWeb, stock, StockArray, StockArrayHistory, stockInfoHistory, NormalizeSymbol);

                            }
                        }
                        if (ret == true) {
                            return 0;
                        }
                    }

                }

                // splitFlag == false     
                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    stock.setSubstatus(ConstantKey.OPEN);

                    String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockNameSQL);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                    logger.info("updateRealTimeStock " + NormalizeSymbol + " Split flag was not correct. Clear Split flag");
                    return 0;
                }

                ArrayList<AFstockInfo> StockSendArray = new ArrayList();
                int index = 0;

                ///make it last date fisrt
                Collections.reverse(StockArray);

                if (StockArray.size() > 500) {
                    logger.info("updateRealTimeStock " + NormalizeSymbol + " send " + StockArray.size());
                }
                for (int i = 0; i < StockArray.size(); i++) {

                    StockSendArray.add(StockArray.get(i));
                    index++;
                    if (index > 99) {
                        index = 0;
                        Collections.reverse(StockSendArray);
                        StockInfoTranObj stockInfoTran = new StockInfoTranObj();
                        stockInfoTran.setNormalizeName(NormalizeSymbol);
                        stockInfoTran.setStockInfoList(StockSendArray);

                        int ret = serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
                        if (ret == 0) {
                            return 0;
                        }
                        StockSendArray.clear();
                    }

                }
                Collections.reverse(StockSendArray);
                StockInfoTranObj stockInfoTran = new StockInfoTranObj();
                stockInfoTran.setNormalizeName(NormalizeSymbol);
                stockInfoTran.setStockInfoList(StockSendArray);
                if (StockSendArray.size() == 0) {
                    return 1;
                }
//                logger.info("updateRealTimeStock send " + StockSendArray.size());
                return serviceAFWeb.updateStockInfoTransaction(stockInfoTran);
            }
        } catch (Exception e) {
            logger.info("> updateRealTimeStock " + NormalizeSymbol + " exception " + e.getMessage());
        }
        return 0;
    }

    private boolean checkStockSplit(ServiceAFweb serviceAFWeb, AFstockObj stock,
            ArrayList<AFstockInfo> StockArray,
            ArrayList<AFstockInfo> StockArrayHistory, AFstockInfo stockInfoHistory,
            String NormalizeSymbol) {

        boolean splitFlag = false;
        float splitF = 0;
        String msg = "";
        CommData commDataObj = new CommData();
        AFstockInfo stockInfoObjInternet = null;

        if (StockArrayHistory != null && StockArrayHistory.size() > 0) {

            long historydate = stockInfoHistory.getEntrydatel();
            historydate = TimeConvertion.endOfDayInMillis(historydate);
            float newClose = stockInfoHistory.getFclose();
            for (int j = 0; j < StockArray.size(); j++) {
                stockInfoObjInternet = StockArray.get(j);
                long currentdate = TimeConvertion.endOfDayInMillis(stockInfoObjInternet.getEntrydatel());
                if (historydate == currentdate) {

                    float oldClose = stockInfoObjInternet.getFclose();
                    float deltaPTmp = 0;
                    if (newClose > oldClose) {
                        deltaPTmp = newClose / oldClose;
                        splitF = deltaPTmp;
                    } else {
                        deltaPTmp = oldClose / newClose;
                        splitF = -deltaPTmp;
                    }

//                    if (ServiceAFweb.mydebugtestflag == true) {
//                        deltaPTmp = 10;
//                    }
                    if (deltaPTmp > CKey.SPLIT_VAL) {
//                                
                        splitFlag = true;
                        msg = "updateRealTimeStock " + NormalizeSymbol + " Split=" + splitF + " "
                                + stockInfoHistory.getEntrydatedisplay() + " newClose " + newClose + " oldClose " + oldClose;

                        commDataObj.setType(0);
                        commDataObj.setSymbol(NormalizeSymbol);
                        commDataObj.setEntrydatedisplay(stockInfoHistory.getEntrydatedisplay());
                        commDataObj.setEntrydatel(stockInfoHistory.getEntrydatel());
                        commDataObj.setSplit(splitF);
                        commDataObj.setOldclose(oldClose);
                        commDataObj.setNewclose(newClose);
                    }
                    break;
                }
            }

        }
        if (splitFlag == true) {
            if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                //just for testing
                return true;
            }
            stock.setSubstatus(ConstantKey.STOCK_SPLIT);
            String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
            ArrayList sqlList = new ArrayList();
            sqlList.add(sockNameSQL);
            serviceAFWeb.SystemUpdateSQLList(sqlList);
            logger.info(msg);

            // send admin messsage
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long dateNowLong = dateNow.getTimeInMillis();
            java.sql.Date d = new java.sql.Date(dateNowLong);
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);

            String commMsg = ESTdate + " " + NormalizeSymbol + " stock split=" + splitF;

            commDataObj.setMsg(commMsg);
            CommMsgImp commMsgImp = new CommMsgImp();
            commMsgImp.AddCommObjMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SPLIT, ConstantKey.INT_TYPE_COM_SPLIT, commDataObj);

            return true;
        }
        return false;
    }

}
