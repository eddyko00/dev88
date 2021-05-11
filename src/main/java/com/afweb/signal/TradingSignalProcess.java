/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;
//https://tradingstrategyguides.com/bollinger-bands-bounce-trading-strategy/
//https://www.programcreek.com/java-api-examples/?api=org.ta4j.core.indicators.EMAIndicator
//https://jar-download.com/artifacts/eu.verdelhan/ta4j-examples/0.4/source-code/ta4jexamples/strategies/MovingMomentumStrategy.java

//https://stackoverflow.com/questions/8587047/support-resistance-algorithm-technical-analysis/8590007
import com.afweb.account.*;
import com.afweb.model.*;

import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.service.ServiceAFweb;

import com.afweb.nn.*;
import com.afweb.nnBP.NNBPservice;
import com.afweb.nnprocess.*;

import com.afweb.stock.*;
import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimeZone;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class TradingSignalProcess {

    protected static Logger logger = Logger.getLogger("TrandingSignalProcess");

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

                    AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
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

                        boolean ret = this.checkStock(serviceAFWeb, symbol);
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
                                    updateAdminTradingsignal(serviceAFWeb, accountAdminObj, symbol);
                                    upateAdminTransaction(serviceAFWeb, accountAdminObj, symbol);
                                    upateAdminPerformance(serviceAFWeb, accountAdminObj, symbol);
                                    upateAdminTRPerf(serviceAFWeb, accountAdminObj, symbol);
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
//////////////////////////////////////////////////
// get one yr performance

    public void upateAdminPerformance(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        // update Trading signal
        //testing
//        symbol = "DIA";
        //testing
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
        ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);

        for (int j = 0; j < tradingRuleList.size(); j++) {
            TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                continue;
            }
            // process performance
            String trName = trObj.getTrname();

            // most recent tran first - old tran at the end 
            ArrayList<TransationOrderObj> tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            if ((tranOrderList == null) || (tranOrderList.size() == 0)) {
                continue;
            }
            ///////add the dummy last transaction
            ///////add the dummy last transaction
            ArrayList<TransationOrderObj> currTranOrderList = new ArrayList();
            currTranOrderList.add(tranOrderList.get(0));

//            currTranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName, 1);
            int tranSignal = ConstantKey.S_NEUTRAL;

            if (trObj.getTrsignal() != ConstantKey.S_NEUTRAL) {
                AFstockInfo lastStockinfo = stock.getAfstockInfo();
                Calendar tranDate = null;
                if (lastStockinfo != null) {
                    tranDate = Calendar.getInstance();
                    tranDate.setTimeInMillis(lastStockinfo.getEntrydatel());
                }
                ArrayList transObjList = AddTransactionOrderProcess(currTranOrderList, trObj, accountObj, stock, trName, tranSignal, tranDate, true);

                if ((transObjList != null) && (transObjList.size() > 0)) {
                    TransationOrderObj trOrder = null;
                    for (int i = 0; i < transObjList.size(); i += 2) {
                        trOrder = (TransationOrderObj) transObjList.get(i);
                        TradingRuleObj trObjNew = (TradingRuleObj) transObjList.get(i + 1);
                    }
                    if (trOrder != null) {
                        tranOrderList.add(0, trOrder);
                    }
                }
            }
            // get one yr performance
            ArrayList<PerformanceObj> performanceList = ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, 1, false); // buyOnly false
            if (performanceList != null) {
                if (performanceList.size() == 1) {
                    PerformanceObj pObj = performanceList.get(0);
                    pObj.setAccountid(accountObj.getId());
                    pObj.setStockid(stock.getId());
                    pObj.setTradingruleid(trObj.getId());
                    ArrayList<PerformanceObj> currentPerfList = serviceAFWeb.SystemAccountStockPerfList(accountObj.getId(), stock.getId(), trObj.getTrname(), 1);
                    String SQLPerf = "";
                    if ((currentPerfList != null) && (currentPerfList.size() > 0)) {
                        PerformanceObj currentpObj = currentPerfList.get(0);
                        pObj.setId(currentpObj.getId());
                        SQLPerf = AccountDB.SQLUpdateAccountStockPerformance(pObj);
                    } else {
                        SQLPerf = AccountDB.SQLaddAccountStockPerformance(pObj);
                    }
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(SQLPerf);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                }
            }

        }  // loop
    }

    public void upateAdminTRPerf(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        // update Trading signal
        //testing
//        symbol = "DIA";
        //testing
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
        ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);

        for (int j = 0; j < tradingRuleList.size(); j++) {
            TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
//            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
//                continue;
//            }
            // process performance
            String trName = trObj.getTrname();
            ArrayList<TransationOrderObj> tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            if ((tranOrderList == null) || (tranOrderList.size() == 0)) {
                continue;
            }
            float perf = serviceAFWeb.getAccountStockRealTimeBalance(trObj);
            trObj.setPerf(perf);

            serviceAFWeb.getAccountImp().updateAccounStockPref(trObj, perf);

        }  // loop
    }

    private PerformanceObj duplicatePerformanceObj(PerformanceObj perfObj) {
        PerformanceObj dupObj = null;
        String st;
        try {
            st = new ObjectMapper().writeValueAsString(perfObj);
            dupObj = new ObjectMapper().readValue(st, PerformanceObj.class);
            dupObj.setUpdatedatedisplay(new java.sql.Date(dupObj.getUpdatedatel()));
            dupObj.setStartdate(new java.sql.Date(dupObj.getUpdatedatel()));
            return dupObj;
        } catch (Exception ex) {
            logger.info("> duplicatePerformanceObj Exception" + ex.getMessage());
        }

        return null;
    }

    public ArrayList<PerformanceObj> ProcessTranPerfHistoryReinvest(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length, boolean buyOnly) {

        int lengthYr = 2;
        int size1year = 20 * 12 * lengthYr + (20 * 3);
        int offset = 0;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        return ProcessTranPerfHistoryOffset(serviceAFWeb, tranOrderList, StockArray, offset, length, true, buyOnly); // reinvest = true
    }

    public ArrayList<PerformanceObj> ProcessTranPerfHistory(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length, boolean buyOnly) {
        int lengthYr = 2;
        int size1year = 20 * 12 * lengthYr + (20 * 3);
        int offset = 0;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        return ProcessTranPerfHistoryOffset(serviceAFWeb, tranOrderList, StockArray, offset, length, false, buyOnly); // reinvest = false
    }

    public ArrayList<PerformanceObj> ProcessTranPerfHistoryOffset(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, ArrayList<AFstockInfo> StockArray, int offset, int length, boolean reinvest, boolean buyOnly) {

        if ((tranOrderList == null) || (tranOrderList.size() == 0)) {
            return null;
        }
        try {
            if (StockArray == null) {
                return null;
            }
            if (StockArray.size() < 10) {
                return null;
            }
            AFstockInfo lastStockinfo = StockArray.get(offset);
            long lastStockInfoEOD = TimeConvertion.endOfDayInMillis(lastStockinfo.getEntrydatel());

            ArrayList<TransationOrderObj> newTranOrderList = new ArrayList();
            for (int i = 0; i < tranOrderList.size(); i++) {
                TransationOrderObj trObj = tranOrderList.get(i);
                long tranObjEOD = TimeConvertion.endOfDayInMillis(trObj.getEntrydatel());

                if (lastStockInfoEOD >= tranObjEOD) {
                    newTranOrderList.add(trObj);
                }
            }
            if (newTranOrderList.size() == 0) {
                return null;
            }
            ArrayList<AFstockInfo> newStockArray = new ArrayList();
            for (int i = 0; i < StockArray.size(); i++) {
                AFstockInfo info = (AFstockInfo) StockArray.get(i);
                if (lastStockInfoEOD >= info.getEntrydatel()) {
                    newStockArray.add(info);
                }
            }
            tranOrderList = newTranOrderList;
            StockArray = newStockArray;
            ///////////////
            int tranObjIndex = tranOrderList.size() - 1;
            int stockInfoIndex = StockArray.size() - 1;

            ArrayList<PerformanceObj> writeArray = new ArrayList();

            PerformanceObj perfObj = new PerformanceObj();
            perfObj.getPerformData().setMinholdtime(1000);

            PerformanceObj lastPerfObj = new PerformanceObj();

            TransationOrderObj lastTranObj = tranOrderList.get(tranObjIndex);

            while (true) {
                if (stockInfoIndex < 0) {
                    // last signal generate the last???
                    break;
                }

                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(stockInfoIndex);
                if (tranObjIndex == -1) {

                    stockInfoIndex--;
                    continue;
                }

                TransationOrderObj tranObj = tranOrderList.get(tranObjIndex);
                long tranObjEOD = TimeConvertion.endOfDayInMillis(tranObj.getEntrydatel());

                long stockInfoEOD = TimeConvertion.endOfDayInMillis(stockinfo.getEntrydatel());
//                // both are end of day so it must equal                
                if (tranObjEOD == stockInfoEOD) {
                    if (tranObjIndex >= 0) {
                        tranObjIndex--;
                    }
                } else if (tranObjEOD < stockInfoEOD) {
                    stockinfo = (AFstockInfo) StockArray.get(stockInfoIndex + 1);

                    if (tranObjIndex >= 0) {
                        tranObjIndex--;
                    }

                } else {
                    stockInfoIndex--;
                    continue;
                }
                if (tranObjIndex < 0) {
                    // tranObjIndex == -1 process only last transaction exit
                    if (tranObj.getTrsignal() != 5) {
                        continue;
                    }
                }
                perfObj.setUpdatedatel(stockinfo.getEntrydatel());
                perfObj.setUpdateDateD(tranObj.getUpdateDateD());
                perfObj.setUpdatedatedisplay(stockinfo.getEntrydatedisplay());

                perfObj.setName(tranObj.getTrname());
                perfObj.setIndexDate(stockInfoIndex);

                if (writeArray.size() == 0) {
                    perfObj.setStartdate(tranObj.getEntrydatedisplay());
                }
                if (reinvest == true) {
                    perfObj = processTranPerfReinvest(perfObj, lastPerfObj, stockinfo, tranObj);

                } else {
                    perfObj = processTranPerf(perfObj, lastPerfObj, stockinfo, tranObj, buyOnly);
                }
                perfObj.getPerformData().setFromdate(lastTranObj.getEntrydatedisplay());

                lastPerfObj = duplicatePerformanceObj(perfObj);

                if (lastPerfObj != null) {
                    writeArray.add(lastPerfObj);
                }
            }

//          Collections.reverse(writeArray);
            ArrayList<PerformanceObj> retList = new ArrayList();
            if (length == 0) {
                length = writeArray.size();
            }
            int size = writeArray.size() - 1;
            for (int i = 0; i < writeArray.size(); i++) {
                PerformanceObj pObj = writeArray.get(size - i);
                retList.add(pObj);
                length--;
                if (length <= 0) {
                    break;
                }
            }
            return retList;
        } catch (Exception ex) {
            logger.info("> ProcessTranPerfHistory Exception" + ex.getMessage());

        }
        return null;
    }

    private PerformanceObj processTranPerf(PerformanceObj pObj, PerformanceObj lastPerfObj, AFstockInfo stockinfo, TransationOrderObj tranObj, boolean buyOnly) {
        if (pObj == null) {
            pObj = new PerformanceObj();
        }
        if (tranObj == null) {
            return pObj;
        }

        pObj.setUpdatedatel(stockinfo.getEntrydatel());
        Calendar setDate = TimeConvertion.getCurrentCalendar(stockinfo.getEntrydatel());
        String stdate = new Timestamp(setDate.getTime().getTime()).toString();
        stdate = stdate.substring(0, 10);
        pObj.setUpdateDateD(stdate);

        int currentSignal = tranObj.getTrsignal();

        if (pObj.getPerformData().getTrsignal() != currentSignal) {
            PerformData perfData = pObj.getPerformData();
            int perfSignal = perfData.getTrsignal();
            if (perfSignal == ConstantKey.S_NEUTRAL) {
                if ((currentSignal == ConstantKey.S_EXIT_LONG) || (currentSignal == ConstantKey.S_EXIT_SHORT)) {
                    if (pObj.getNumtrade() == 0) {
                        return pObj;
                    }
                }
            }

            perfData.setTrsignal(currentSignal);
            if (currentSignal == ConstantKey.S_LONG_BUY) {
                float invest = tranObj.getShare() * tranObj.getAvgprice();
                if (pObj.getBalance() >= invest) {
                    pObj.setBalance(pObj.getBalance() - invest);
                } else {
                    pObj.setInvestment(pObj.getInvestment() + invest);
                    pObj.setBalance(pObj.getBalance() - invest);
                }

                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(tranObj.getShare());
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + pObj.getInvestment();
                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if (currentSignal == ConstantKey.S_EXIT_LONG) {

                float invest = tranObj.getShare() * tranObj.getAvgprice();
                pObj.setBalance(pObj.getBalance() + invest);
                pObj.setInvestment(0);
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(0);
                pObj.setNumtrade(pObj.getNumtrade() + 1);
                // calculate numWin performance
                float fProfit = pObj.getBalance() + pObj.getInvestment();
                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

                if (fProfit > lastPerfObj.getGrossprofit()) {
                    perfData.setNumwin(perfData.getNumwin() + 1);
                    float maxWin = fProfit - lastPerfObj.getGrossprofit();
                    perfData.setTotalwin(maxWin + perfData.getTotalwin());

                    if (maxWin > perfData.getMaxwin()) {
                        perfData.setMaxwin(maxWin);
                    }
                    perfData.setAvgwin(perfData.getTotalwin() / perfData.getNumwin());
                    if (perfData.getAvgwin() == 0) {
                        perfData.setRatioavgwinloss(0);
                    } else if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }

                } else if (fProfit < lastPerfObj.getGrossprofit()) {
                    perfData.setNumloss(perfData.getNumloss() + 1);

                    float maxLoss = lastPerfObj.getGrossprofit() - fProfit;
                    perfData.setTotalloss(maxLoss + perfData.getTotalloss());

                    if (maxLoss > perfData.getMaxloss()) {
                        perfData.setMaxloss(maxLoss);
                    }
                    perfData.setAvgloss(perfData.getTotalloss() / perfData.getNumloss());
                    if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                }

                pObj.setRating(1);
                if (perfData.getRatioavgwinloss() != 0) {
                    pObj.setRating(perfData.getRatioavgwinloss());
                }
                int holdtime = lastPerfObj.getIndexDate() - pObj.getIndexDate();
                perfData.setHoldtime(holdtime);
                if (holdtime > perfData.getMaxholdtime()) {
                    perfData.setMaxholdtime(holdtime);
                }
                if (holdtime < perfData.getMinholdtime()) {
                    perfData.setMinholdtime(holdtime);
                }

            } else if ((currentSignal == ConstantKey.S_SHORT_SELL) && (buyOnly == false)) {
                float invest = tranObj.getShare() * tranObj.getAvgprice();
                if (pObj.getBalance() >= invest) {
                    pObj.setBalance(pObj.getBalance() - invest);
                } else {
                    pObj.setInvestment(pObj.getInvestment() + invest);
                    pObj.setBalance(pObj.getBalance() - invest);
                }
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(tranObj.getShare());
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + pObj.getInvestment();

                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if ((currentSignal == ConstantKey.S_EXIT_SHORT) && (buyOnly == false)) {

                float deltaPrice = tranObj.getAvgprice() - perfData.getClose(); //final price - original price
                deltaPrice = -deltaPrice;  // negative for exit short
                float netPrice = pObj.getPerformData().getClose() + deltaPrice;

                float invest = tranObj.getShare() * netPrice;

                pObj.setBalance(pObj.getBalance() + invest);
                pObj.setInvestment(0);
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(0);
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                // calculate numWin performance
                float fProfit = pObj.getBalance() + pObj.getInvestment();

                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

                if (fProfit > lastPerfObj.getGrossprofit()) {
                    perfData.setNumwin(perfData.getNumwin() + 1);
                    float maxWin = fProfit - lastPerfObj.getGrossprofit();
                    perfData.setTotalwin(maxWin + perfData.getTotalwin());

                    if (maxWin > perfData.getMaxwin()) {
                        perfData.setMaxwin(maxWin);
                    }
                    perfData.setAvgwin(perfData.getTotalwin() / perfData.getNumwin());
                    if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                } else if (fProfit < lastPerfObj.getGrossprofit()) {
                    perfData.setNumloss(perfData.getNumloss() + 1);

                    float maxLoss = lastPerfObj.getGrossprofit() - fProfit;
                    perfData.setTotalloss(maxLoss + perfData.getTotalloss());

                    if (maxLoss > perfData.getMaxloss()) {
                        perfData.setMaxloss(maxLoss);
                    }
                    perfData.setAvgloss(perfData.getTotalloss() / perfData.getNumloss());
                    if (perfData.getAvgwin() == 0) {
                        perfData.setRatioavgwinloss(0);
                    } else if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                }
                pObj.setRating(1);
                if (perfData.getRatioavgwinloss() != 0) {
                    pObj.setRating(perfData.getRatioavgwinloss());
                }
                int holdtime = lastPerfObj.getIndexDate() - pObj.getIndexDate();
                perfData.setHoldtime(holdtime);
                if (holdtime > perfData.getMaxholdtime()) {
                    perfData.setMaxholdtime(holdtime);
                }
                if (holdtime < perfData.getMinholdtime()) {
                    perfData.setMinholdtime(holdtime);
                }
            }

            return pObj;
        }
        return pObj;
    }

    private PerformanceObj processTranPerfReinvest(PerformanceObj pObj, PerformanceObj lastPerfObj, AFstockInfo stockinfo, TransationOrderObj tranObj) {
        if (pObj == null) {
            pObj = new PerformanceObj();
        }
        if (tranObj == null) {
            return pObj;
        }

        pObj.setUpdatedatel(stockinfo.getEntrydatel());
        Calendar setDate = TimeConvertion.getCurrentCalendar(stockinfo.getEntrydatel());
        String stdate = new Timestamp(setDate.getTime().getTime()).toString();
        stdate = stdate.substring(0, 10);
        pObj.setUpdateDateD(stdate);

        int currentSignal = tranObj.getTrsignal();

        if (pObj.getPerformData().getTrsignal() != currentSignal) {
            PerformData perfData = pObj.getPerformData();
            int perfSignal = perfData.getTrsignal();
            if (perfSignal == ConstantKey.S_NEUTRAL) {
                if ((currentSignal == ConstantKey.S_EXIT_LONG) || (currentSignal == ConstantKey.S_EXIT_SHORT)) {
                    if (pObj.getNumtrade() == 0) {
                        return pObj;
                    }
                }
            }

            perfData.setTrsignal(currentSignal);
            if (currentSignal == ConstantKey.S_LONG_BUY) {

                float invest = tranObj.getShare() * tranObj.getAvgprice();

                int shareInt = (int) tranObj.getShare();

                if (pObj.getBalance() >= invest) {
                    float ba = pObj.getBalance();
                    float sh = ba / tranObj.getAvgprice();
                    shareInt = (int) sh; // get the share from all balance
                    invest = shareInt * tranObj.getAvgprice();
                    pObj.setBalance(pObj.getBalance() - invest);
                } else {
                    pObj.setInvestment(pObj.getInvestment() + invest);
                }

                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(shareInt);
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + invest - pObj.getInvestment();
                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if (currentSignal == ConstantKey.S_EXIT_LONG) {

                float invest = pObj.getPerformData().getShare() * tranObj.getAvgprice();

                pObj.setBalance(pObj.getBalance() + invest);
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(0);
                pObj.setNumtrade(pObj.getNumtrade() + 1);
                // calculate numWin performance
                float fProfit = pObj.getBalance() - pObj.getInvestment();
                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

                if (fProfit > lastPerfObj.getGrossprofit()) {
                    perfData.setNumwin(perfData.getNumwin() + 1);
                    float maxWin = fProfit - lastPerfObj.getGrossprofit();
                    perfData.setTotalwin(maxWin + perfData.getTotalwin());

                    if (maxWin > perfData.getMaxwin()) {
                        perfData.setMaxwin(maxWin);
                    }
                    perfData.setAvgwin(perfData.getTotalwin() / perfData.getNumwin());
                    if (perfData.getAvgwin() == 0) {
                        perfData.setRatioavgwinloss(0);
                    } else if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }

                } else if (fProfit < lastPerfObj.getGrossprofit()) {
                    perfData.setNumloss(perfData.getNumloss() + 1);

                    float maxLoss = lastPerfObj.getGrossprofit() - fProfit;
                    perfData.setTotalloss(maxLoss + perfData.getTotalloss());

                    if (maxLoss > perfData.getMaxloss()) {
                        perfData.setMaxloss(maxLoss);
                    }
                    perfData.setAvgloss(perfData.getTotalloss() / perfData.getNumloss());
                    if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                }

                pObj.setRating(1);
                if (perfData.getRatioavgwinloss() != 0) {
                    pObj.setRating(perfData.getRatioavgwinloss());
                }
                int holdtime = lastPerfObj.getIndexDate() - pObj.getIndexDate();
                perfData.setHoldtime(holdtime);
                if (holdtime > perfData.getMaxholdtime()) {
                    perfData.setMaxholdtime(holdtime);
                }
                if (holdtime < perfData.getMinholdtime()) {
                    perfData.setMinholdtime(holdtime);
                }

            } else if (currentSignal == ConstantKey.S_SHORT_SELL) {

                float invest = tranObj.getShare() * tranObj.getAvgprice();

                int shareInt = (int) tranObj.getShare();

                if (pObj.getBalance() >= invest) {
                    float ba = pObj.getBalance();
                    float sh = ba / tranObj.getAvgprice();
                    shareInt = (int) sh; // get the share from all balance
                    invest = shareInt * tranObj.getAvgprice();
                    pObj.setBalance(pObj.getBalance() - invest);
                } else {
                    pObj.setInvestment(pObj.getInvestment() + invest);
                }

                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(shareInt);
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + invest - pObj.getInvestment();

                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if (currentSignal == ConstantKey.S_EXIT_SHORT) {

                float deltaPrice = tranObj.getAvgprice() - perfData.getClose(); //final price - original price
                deltaPrice = -deltaPrice;  // negative for exit short
                float netPrice = pObj.getPerformData().getClose() + deltaPrice;

                float invest = pObj.getPerformData().getShare() * netPrice;

                pObj.setBalance(pObj.getBalance() + invest);
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(0);
                pObj.setNumtrade(pObj.getNumtrade() + 1);
                // calculate numWin performance
                float fProfit = pObj.getBalance() - pObj.getInvestment();

                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

                if (fProfit > lastPerfObj.getGrossprofit()) {
                    perfData.setNumwin(perfData.getNumwin() + 1);
                    float maxWin = fProfit - lastPerfObj.getGrossprofit();
                    perfData.setTotalwin(maxWin + perfData.getTotalwin());

                    if (maxWin > perfData.getMaxwin()) {
                        perfData.setMaxwin(maxWin);
                    }
                    perfData.setAvgwin(perfData.getTotalwin() / perfData.getNumwin());
                    if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                } else if (fProfit < lastPerfObj.getGrossprofit()) {
                    perfData.setNumloss(perfData.getNumloss() + 1);

                    float maxLoss = lastPerfObj.getGrossprofit() - fProfit;
                    perfData.setTotalloss(maxLoss + perfData.getTotalloss());

                    if (maxLoss > perfData.getMaxloss()) {
                        perfData.setMaxloss(maxLoss);
                    }
                    perfData.setAvgloss(perfData.getTotalloss() / perfData.getNumloss());
                    if (perfData.getAvgwin() == 0) {
                        perfData.setRatioavgwinloss(0);
                    } else if (perfData.getAvgloss() > 0) {
                        float ratio = perfData.getAvgwin() / perfData.getAvgloss();
                        perfData.setRatioavgwinloss(ratio);
                    }
                }
                pObj.setRating(1);
                if (perfData.getRatioavgwinloss() != 0) {
                    pObj.setRating(perfData.getRatioavgwinloss());
                }
                int holdtime = lastPerfObj.getIndexDate() - pObj.getIndexDate();
                perfData.setHoldtime(holdtime);
                if (holdtime > perfData.getMaxholdtime()) {
                    perfData.setMaxholdtime(holdtime);
                }
                if (holdtime < perfData.getMinholdtime()) {
                    perfData.setMinholdtime(holdtime);
                }
            }

            return pObj;
        }
        return pObj;
    }

    ///////////////////////////////////////////////////////
    // only admin update need to check the CheckRefData = true
    // relearn NN does not need to check the CheckRefData = false
    ///asc thObjList old first - recent last
    public ArrayList<StockTRHistoryObj> resetVitualTransaction(ServiceAFweb serviceAFWeb, AFstockObj stock, String trName) {
        try {
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            serviceAFWeb.SystemAccountStockClrTranByAccountID(accountAdminObj, stock.getId(), trName);
            TradingRuleObj trObj = serviceAFWeb.getAccountImp().getAccountStockIDByTRname(accountAdminObj.getId(), stock.getId(), trName);
            // get 2 year
            /// thObjList old first - recent last
            ArrayList<StockTRHistoryObj> trHistoryList = ProcessTRHistory(serviceAFWeb, trObj, 2, CKey.SHORT_MONTH_SIZE);

//        int size1year = 20 * 12 * 1 + (50 * 3);
//        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
//        int offset = 0;
//        ///asc thObjList old first - recent last
//        ArrayList<StockTRHistoryObj> trHistoryList = ProcessTRHistoryOffset(serviceAFWeb, trObj, StockArray, offset, CKey.SHORT_MONTH_SIZE);
            serviceAFWeb.SystemAccountStockClrTranByAccountID(accountAdminObj, stock.getId(), trName);
            return trHistoryList;
        } catch (Exception ex) {
            logger.info("> resetVitualTransaction Exception" + ex.getMessage());
        }
        return null;
    }

    public void upateAdminTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        try {
            if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
                return;
            }
            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            if (stock != null) {
                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    return;
                }
            }
//            logger.info("> upateAdminTransaction " + stock.getSymbol());
            ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);
            Calendar dateNow = TimeConvertion.getCurrentCalendar();

            for (int j = 0; j < tradingRuleList.size(); j++) {
                TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
                if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                    continue;
                }
                int subStatus = trObj.getSubstatus();

                if (trObj.getType() == ConstantKey.INT_TR_NN1) {
                    NN1ProcessBySignal NN1Proc = new NN1ProcessBySignal();
                    if (NN1Proc.checkNN1Ready(serviceAFWeb, symbol, true) == false) {
                        continue;
                    }
                } else if (trObj.getType() == ConstantKey.INT_TR_NN91) { // shadow of INT_TR_NN1
                    String name = ConstantKey.TR_NN91; // just for the search name
                    continue;
                } else if (trObj.getType() == ConstantKey.INT_TR_NN92) { // shadow of INT_TR_NN1
                    String name = ConstantKey.TR_NN92; // just for the search name
                    continue;
                } else if (trObj.getType() == ConstantKey.INT_TR_NN93) { // shadow of INT_TR_NN3
                    String name = ConstantKey.TR_NN93; // just for the search name
                    continue;
                } else if (trObj.getType() == ConstantKey.INT_TR_NN2) {
                    NN2ProcessBySignal NN2Proc = new NN2ProcessBySignal();
                    if (NN2Proc.checkNN2Ready(serviceAFWeb, symbol, true) == false) {
                        continue;
                    }
                } else if (trObj.getType() == ConstantKey.INT_TR_NN3) {
//                    if (stock.getSymbol().equals("GLD")) {
//
//                    } else if (stock.getSymbol().equals("HOU.TO")) {
//
//                    } else {
//                        continue;
//                    }

                    NN3ProcessBySignal NN3Proc = new NN3ProcessBySignal();
                    if (NN3Proc.checkNN3Ready(serviceAFWeb, symbol, true) == false) {
                        continue;
                    }
                    if (ServiceAFweb.nn3testflag == false) {
                        continue;
                    }

                }

                /////// no need to handle other indicator. Just make NN1 and NN2 working
                if (trObj.getType() == ConstantKey.INT_TR_RSI) {
                    continue;
                } else if (trObj.getType() == ConstantKey.INT_TR_MACD) {
                    if (ServiceAFweb.mydebugtestNN3flag == true) {
                        if (stock.getSymbol().equals("GLD")) {
                            ;
                        } else if (stock.getSymbol().equals("HOU.TO")) {
                            ;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else if (trObj.getType() == ConstantKey.INT_TR_MV) {
                    continue;
                }
//                
                if (subStatus == ConstantKey.OPEN) {
                    int ret = AddTransactionOrder(serviceAFWeb, accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null, true);
//                    int ret = serviceAFWeb.SystemAddTransactionOrder(accountObj, stock, trObj.getTrname(), trObj.getTrsignal(), null);
                    continue;
                }

                long lockExit90M = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                lockExit90M = TimeConvertion.addMinutes(lockExit90M, StockDB.MaxMinuteAdminSignalTrading - 20);  //90 minutes

                if (subStatus == ConstantKey.INITIAL) {
//                    logger.info("> upateAdminTransaction INITIAL " + stock.getSymbol() + " " + trObj.getTrname());
                    serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, stock.getId(), trObj.getTrname());

                    // get 2 year
                    /// thObjList old first - recent last
                    ArrayList<StockTRHistoryObj> trHistoryList = ProcessTRHistory(serviceAFWeb, trObj, 2, CKey.MONTH_SIZE);

                    if ((trHistoryList == null) || (trHistoryList.size() == 0)) {
                        continue;
                    }

                    if (lockExit90M < TimeConvertion.getCurrentCalendar().getTimeInMillis()) {
                        //exit
                        logger.info("> upateAdminTransaction Process too long " + stock.getSymbol());
                        continue;
                    }

                    if (CKey.NN_DEBUG == true) {
                        if (ServiceAFweb.processRestinputflag == true) {
                            boolean flag = true;
                            if (flag == true) {
                                ArrayList<String> writeArray = new ArrayList();
                                ArrayList<String> displayArray = new ArrayList();
                                int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(trHistoryList, writeArray, displayArray);
                                if (ret == 1) {
                                    FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_predict_tran.csv", writeArray);
                                }
                            }
                        }
                    }
                    ArrayList<TransationOrderObj> currTranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trObj.getTrname(), 1);
                    if (currTranOrderList != null) {
                        if (currTranOrderList.size() > 0) {
                            //should be empty it should be cleared at the begining by 
                            //SystemAccountStockClrTranByAccountID
                            logger.info("> upateAdminTransaction Process not empty " + stock.getSymbol());
                            continue;
                        }
                    }

                    int lastSignal = 0;
                    long date2yrBack = TimeConvertion.addMonths(dateNow.getTimeInMillis(), -24); //2 yr before

//                    StockTRHistoryObj trHistory = trHistoryList.get(trHistoryList.size() - 1);
                    StockTRHistoryObj trHistory = trHistoryList.get(0);
                    lastSignal = trHistory.getTrsignal();

                    logger.info("> upateAdminTransaction " + stock.getSymbol() + ", TR=" + trObj.getTrname() + ", Size=" + trHistoryList.size());

                    for (int k = 0; k < trHistoryList.size(); k++) {
                        trHistory = trHistoryList.get(k);
                        int signal = trHistory.getTrsignal();
                        if (lastSignal == signal) {
                            continue;
                        }

                        lastSignal = signal;
                        //check time only when signal change
                        if (trHistory.getUpdateDatel() > date2yrBack) {
                            // add signal
                            long endofDay = TimeConvertion.addHours(trHistory.getUpdateDatel(), -5);
                            //// not sure why tran date is one day more (may be daylight saving?????????                            
                            //// not sure why tran date is one day more (may be daylight saving?????????    
                            //// not sure why tran date is one day more (may be daylight saving?????????                               
                            Calendar dateOffet = TimeConvertion.getCurrentCalendar(endofDay);

                            //Override the stockinfo for the price
                            AFstockInfo afstockInfo = trHistory.getAfstockInfo();
                            stock.setAfstockInfo(afstockInfo);
                            int ret = AddTransactionOrder(serviceAFWeb, accountObj, stock, trObj.getTrname(), signal, dateOffet, true);
//                               
//                            int ret = serviceAFWeb.SystemAddTransactionOrder(accountObj, stock, trObj.getTrname(), signal, dateOffet);
                        }
                    }
                    // udpate tr SubStatus to open
                    // need to get the latest TR object after the SystemAddTransactionOrder
                    trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), trObj.getTrname());
                    // need to get the latest TR object after the SystemAddTransactionOrder
                    trObj.setSubstatus(ConstantKey.OPEN);
                    String updateSQL = AccountDB.SQLUpdateAccountStockStatus(trObj);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(updateSQL);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                }
            }  // loop
        } catch (Exception ex) {
            logger.info("> upateAdminTransaction Exception " + ex.getMessage());
        }
    }

    /// thObjList old first - recent last
    public ArrayList<StockTRHistoryObj> ProcessTRHistory(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, int lengthYr, int month) {

        int size1year = 20 * 12 * lengthYr + (50 * 3);
        String symbol = trObj.getSymbol();
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        int offset = 0;
        ///asc thObjList old first - recent last
        ArrayList<StockTRHistoryObj> thObjList = ProcessTRHistoryOffset(serviceAFWeb, trObj, StockArray, offset, month);
        return thObjList;
    }

    /// thObjList old first - recent last
    public ArrayList<StockTRHistoryObj> ProcessTRHistoryOffset(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize) {

        if (trObj == null) {
            return null;
        }
        if (trObj.getType() == ConstantKey.INT_TR_ACC) {
            // need to get the buy sell signal history
            return null;
        }

        if (StockArray == null) {
            return null;
        }
        if (StockArray.size() < 10) {
            return null;
        }

        ArrayList<StockTRHistoryObj> writeArray = new ArrayList();
        String dupLastSt = "";

        if (offsetInput >= StockArray.size()) {
            return null;
        }

        AccountObj accountObj = serviceAFWeb.SystemAccountObjByAccountID(trObj.getAccountid());
        AFstockObj stock = serviceAFWeb.SystemRealTimeStockByStockID(trObj.getStockid());

        // this only use by ConstantKey.INT_TR_BST can delete it 
        ArrayList<TradingRuleObj> tradingRuleList = null;

        int prevSignal = ConstantKey.S_NEUTRAL;
        if (monthSize <= 0) {
            monthSize = 1;
        }

        int sizeTR = 20 * monthSize; //20 * 14;

//////////////
//////////////
        int i = 0;
        int offset = sizeTR + offsetInput - i;
        while (offset > offsetInput) {
            i++;

            offset = sizeTR + offsetInput - i;
            AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);

            String dupSt = "" + stockinfo.getEntrydatedisplay() + stockinfo.getFclose();
            if (dupLastSt.equals(dupSt)) {
                continue;
            }
            dupLastSt = dupSt;

            Calendar setDate = TimeConvertion.getCurrentCalendar(stockinfo.getEntrydatel());
            String stdate = new Timestamp(setDate.getTime().getTime()).toString();
            stdate = stdate.substring(0, 10);

            StockTRHistoryObj trHistory = new StockTRHistoryObj();
            trHistory.setTrname(trObj.getTrname());
            trHistory.setSymbol(trObj.getSymbol());
            trHistory.setType(trObj.getType());
            trHistory.setUpdateDateD(stdate);
            trHistory.setUpdateDatel(stockinfo.getEntrydatel());
            trHistory.setClose(stockinfo.getFclose());
            trHistory.setVolume(stockinfo.getVolume());
            trHistory.setAfstockInfo(stockinfo);
            trHistory.setParmSt1("");

            // adding > parameter 4 need to udpate getAccountStockTRListHistoryDisplay
            switch (trObj.getType()) {
                case ConstantKey.INT_TR_MV:
                    // check if signal to buy or sell
                    EMAObj ema2050 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_MV_20, ConstantKey.INT_MV_50);

//                    ProcessNN2 pnn2 = new ProcessNN2();
//                    EMAObj ema2050 = pnn2.getTechnicalCal(StockArray, offset);                    
                    trObj.setTrsignal(ema2050.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) ema2050.ema);
                    trHistory.setParm2((float) ema2050.lastema);
                    float LTerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
                    float STerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
                    trHistory.setParm3((float) LTerm);
                    trHistory.setParm4((float) STerm);
                    break;

                case ConstantKey.INT_TR_SMA0:
                    SMAObj sma12 = TechnicalCal.SMASignal(StockArray, offset, ConstantKey.INT_EMA_1, ConstantKey.INT_EMA_3);
                    trObj.setTrsignal(sma12.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) sma12.ema);
                    trHistory.setParm2((float) sma12.lastema);
                    break;
                case ConstantKey.INT_TR_SMA1:
                    SMAObj sma48 = TechnicalCal.SMASignal(StockArray, offset, ConstantKey.INT_EMA_4, ConstantKey.INT_EMA_8);
                    trObj.setTrsignal(sma48.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) sma48.ema);
                    trHistory.setParm2((float) sma48.lastema);
                    break;

//                case ConstantKey.INT_TR_EMA0:
//                    EMAObj ema12 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_1, ConstantKey.INT_EMA_2);
//                    trObj.setTrsignal(ema12.trsignal);
//                    trHistory.setTrsignal(trObj.getTrsignal());
//                    trHistory.setParm1((float) ema12.ema);
//                    trHistory.setParm2((float) ema12.lastema);
//                    break;                                        
                case ConstantKey.INT_TR_EMA1:   // normal
                    EMAObj ema36 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_3, ConstantKey.INT_EMA_6);
                    trObj.setTrsignal(ema36.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) ema36.ema);
                    trHistory.setParm2((float) ema36.lastema);
                    break;
                case ConstantKey.INT_TR_EMA2:
                    EMAObj ema816 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_8, ConstantKey.INT_EMA_16);
                    trObj.setTrsignal(ema816.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) ema816.ema);
                    trHistory.setParm2((float) ema816.lastema);
                    break;
////////////////////////////////////////////////
                case ConstantKey.INT_TR_MACD0: {
                    MACDObj macd36 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD0_3, ConstantKey.INT_MACD0_6, ConstantKey.INT_MACD0_2);
                    trObj.setTrsignal(macd36.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd36.macd);
                    trHistory.setParm2((float) macd36.signal);
                    trHistory.setParm3((float) macd36.diff);
                    break;
                }

                case ConstantKey.INT_TR_MACD1: {    //normal
                    MACDObj macd612 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                    trObj.setTrsignal(macd612.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd612.macd);
                    trHistory.setParm2((float) macd612.signal);
                    trHistory.setParm3((float) macd612.diff);
                    break;
                }
                case ConstantKey.INT_TR_MACD2: {
                    MACDObj macd1226 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                    trObj.setTrsignal(macd1226.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd1226.macd);
                    trHistory.setParm2((float) macd1226.signal);
                    trHistory.setParm3((float) macd1226.diff);
                    break;
                }
                case ConstantKey.INT_TR_MACD: {

                    MACDObj macd12 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                    if (ServiceAFweb.mydebugtestflag == true) {
                        ProcessNN1 pnn1 = new ProcessNN1();
                        macd12 = pnn1.getTechnicalCal(StockArray, offset);
                    }
                    trObj.setTrsignal(macd12.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd12.macd);
                    trHistory.setParm2((float) macd12.signal);
                    trHistory.setParm3((float) macd12.diff);
                    break;
                }
////////////////////////////////////////////////////////////////////
                case ConstantKey.INT_TR_RSI:
                    RSIObj rsi = TechnicalCal.RSI(StockArray, offset, ConstantKey.INT_RSI_14);
                    trObj.setTrsignal(rsi.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) rsi.rsi);
                    trHistory.setParm2((float) rsi.lastRsi);
                    break;
                case ConstantKey.INT_TR_NN1:
                    boolean nn1Flag = true;
                    if (nn1Flag == true) {
                        ProcessNN1 nn1 = new ProcessNN1();
                        int nn1Signal = nn1.ProcessTRHistoryOffsetNN1(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                        prevSignal = nn1Signal;
                        if (ServiceAFweb.mydebugtestflag == true) {

                            float STerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
                            float LTerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);

                            logger.info(">ProcessTRHistoryOffset NN1 " + offset + " " + stdate + " S:" + nn1Signal + " C:" + trHistory.getParm5()
                                    + " " + trHistory.getParmSt1());

                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN91: // shadow of INT_TR_NN1
                    ProcessNN91 nn91 = new ProcessNN91();
                    int nn91Signal = nn91.ProcessTRHistoryOffsetNN91(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                    prevSignal = nn91Signal;
                    break;
                case ConstantKey.INT_TR_NN2:

                    boolean nn2Flag = true;
                    if (nn2Flag == true) {
                        ProcessNN2 nn2 = new ProcessNN2();
                        int nn2Signal = nn2.ProcessTRHistoryOffsetNN2(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                        prevSignal = nn2Signal;
                        if (ServiceAFweb.mydebugtestflag == true) {
//                            if (offset < 99) {
//                                prevSignal = nn2Signal;
//                            }
                            float STerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
                            float LTerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
                            logger.info(">ProcessTRHistoryOffset NN1 " + offset + " " + stdate + " S:" + nn2Signal + " C:" + trHistory.getParm5()
                                    + " " + trHistory.getParmSt1());
                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN92: // shadow of INT_TR_NN2
                    ProcessNN92 nn92 = new ProcessNN92();
                    int nn92Signal = nn92.ProcessTRHistoryOffsetNN92(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                    prevSignal = nn92Signal;
                    break;
                case ConstantKey.INT_TR_NN3:
                    boolean nn3Flag = true;
                    if (nn3Flag == true) {
                        if (ServiceAFweb.nn3testflag == true) {
                            if (stock.getSymbol().equals("GLD")) {

                            } else if (stock.getSymbol().equals("HOU.TO")) {

                            } else {
                                break;
                            }
                            ProcessNN3 nn3 = new ProcessNN3();
                            int nn3Signal = nn3.ProcessTRHistoryOffsetNN3(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                            prevSignal = nn3Signal;
                            if (ServiceAFweb.mydebugtestflag == true) {
//                                if (offset < 99) {
//                                    prevSignal = nn3Signal;
//                                }
                                float STerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
                                float LTerm1 = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
                                logger.info(">ProcessTRHistoryOffset NN3 " + offset + " " + stdate + " S:" + nn3Signal + " C:" + trHistory.getParm5()
                                        + " L:" + LTerm1 + " S:" + STerm1);
                            }
                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN93: // shadow of INT_TR_NN3
                    ProcessNN93 nn93 = new ProcessNN93();
                    int nn93Signal = nn93.ProcessTRHistoryOffsetNN93(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                    prevSignal = nn93Signal;
                    break;
                default:
                    break;
            }
            writeArray.add(trHistory);
        }
        return writeArray;
    }

    public void testUpdateAdminTradingsignal(ServiceAFweb serviceAFWeb, String symbol) {
//        this.serviceAFWeb = serviceAFWeb;
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        updateAdminTradingsignal(serviceAFWeb, accountAdminObj, symbol);
        upateAdminTransaction(serviceAFWeb, accountAdminObj, symbol);
        upateAdminPerformance(serviceAFWeb, accountAdminObj, symbol);
    }

    public void updateAdminTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return;
        }
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);

        if (stock != null) {
            if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                return;
            }
        }

        int size1year = 5 * 52;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year * 4);
        if (StockArray == null) {
            return;
        }
        if (StockArray.size() < 10) {
            return;
        }
        // update Trading signal
        ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
        ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);
        int offset = 0;

        TradingRuleObj trTradingACCObj = null;
        Calendar dateNowUpdate = TimeConvertion.getCurrentCalendar();

        for (int j = 0; j < tradingRuleList.size(); j++) {

            TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);

            switch (trObj.getType()) {
                case ConstantKey.INT_TR_MV:
                    // check if signal to buy or sell
                    EMAObj ema2050 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_MV_20, ConstantKey.INT_MV_50);

//                    ProcessNN2 pnn2 = new ProcessNN2();
//                    EMAObj ema2050 = pnn2.getTechnicalCal(StockArray, offset);    
                    trObj.setTrsignal(ema2050.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                case ConstantKey.INT_TR_MACD:

                    MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                    if (ServiceAFweb.mydebugtestflag == true) {
                        ProcessNN1 pnn1 = new ProcessNN1();
                        macd = pnn1.getTechnicalCal(StockArray, offset);
                    }
                    trObj.setTrsignal(macd.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                case ConstantKey.INT_TR_RSI:
                    RSIObj rsi = TechnicalCal.RSI(StockArray, offset, ConstantKey.INT_RSI_14);
                    trObj.setTrsignal(rsi.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                // neural net processing                    
                case ConstantKey.INT_TR_NN1:
                    boolean nn1Flag = true;
                    if (nn1Flag == true) {
                        ProcessNN1 nn1 = new ProcessNN1();
                        NNObj nn = nn1.updateAdminTradingsignalNN1(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, stock, tradingRuleList);
                        if (nn != null) {
                            trObj.setTrsignal(nn.getTrsignal());
                            if (nn.getConfident() != null) {
                                if (nn.getConfident().length() > 0) {
                                    AccData accData = serviceAFWeb.getAccData(trObj);
                                    accData.setConf(nn.getConfident());
//                                    serviceAFWeb.getAccountImp().updateAccountRef(accountObj, accData);
                                    String nameSt = "";
                                    try {
                                        nameSt = new ObjectMapper().writeValueAsString(accData);
                                        nameSt = nameSt.replaceAll("\"", "#");
                                    } catch (JsonProcessingException ex) {
                                    }
                                    trObj.setComment(nameSt);
//                                    trObj.setComment(nn.getConfident());
                                }
                            }
                            UpdateTRList.add(trObj);
                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN91:  // shadow of INT_TR_NN1
                    break;
                case ConstantKey.INT_TR_NN2:

                    boolean nn2Flag = true;
                    if (nn2Flag == true) {
                        ProcessNN2 nn2 = new ProcessNN2();
                        NNObj nn = nn2.updateAdminTradingsignalNN2(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, stock, tradingRuleList);
                        if (nn != null) {
                            trObj.setTrsignal(nn.getTrsignal());
                            if (nn.getConfident() != null) {
                                if (nn.getConfident().length() > 0) {
                                    AccData accData = serviceAFWeb.getAccData(trObj);
                                    accData.setConf(nn.getConfident());
//                                    serviceAFWeb.getAccountImp().updateAccountRef(accountObj, accData);
                                    String nameSt = "";
                                    try {
                                        nameSt = new ObjectMapper().writeValueAsString(accData);
                                        nameSt = nameSt.replaceAll("\"", "#");
                                    } catch (JsonProcessingException ex) {
                                    }
                                    trObj.setComment(nameSt);
//                                    trObj.setComment(nn.getConfident());
                                }
                            }
                            UpdateTRList.add(trObj);
                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN92:  // shadow of INT_TR_NN2
                    break;
                case ConstantKey.INT_TR_NN3:
                    boolean nn3Flag = true;
                    if (nn3Flag == true) {
                        if (ServiceAFweb.nn3testflag == true) {
                            if (stock.getSymbol().equals("GLD")) {

                            } else if (stock.getSymbol().equals("HOU.TO")) {

                            } else {
                                break;
                            }
                            ProcessNN3 nn3 = new ProcessNN3();
                            NNObj nn = nn3.updateAdminTradingsignalNN3(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, stock, tradingRuleList);
                            if (nn != null) {
                                trObj.setTrsignal(nn.getTrsignal());
                                if (nn.getConfident() != null) {
                                    if (nn.getConfident().length() > 0) {
                                        AccData accData = serviceAFWeb.getAccData(trObj);
                                        accData.setConf(nn.getConfident());
//                                    serviceAFWeb.getAccountImp().updateAccountRef(accountObj, accData);
                                        String nameSt = "";
                                        try {
                                            nameSt = new ObjectMapper().writeValueAsString(accData);
                                            nameSt = nameSt.replaceAll("\"", "#");
                                        } catch (JsonProcessingException ex) {
                                        }
                                        trObj.setComment(nameSt);
//                                    trObj.setComment(nn.getConfident());
                                    }
                                }
                                UpdateTRList.add(trObj);
                            }
                        }
                    }
                    break;
                case ConstantKey.INT_TR_NN93:  // shadow of INT_TR_NN3
                    break;
                case ConstantKey.INT_TR_ACC:
                    trTradingACCObj = trObj;
                    break;
                default:
                    break;
            }

            trObj.setUpdatedatedisplay(new java.sql.Date(dateNowUpdate.getTimeInMillis()));
            trObj.setUpdatedatel(dateNowUpdate.getTimeInMillis());
        }
        if (trTradingACCObj != null) {
            int trLinkId = trTradingACCObj.getLinktradingruleid();
//            if (trLinkId == 0) {
//                trLinkId = ConstantKey.INT_TR_MACD;
//            }

            if (trLinkId != 0) {
                for (int j = 0; j < tradingRuleList.size(); j++) {
                    TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
                    if (trLinkId == trObj.getType()) {
                        trTradingACCObj.setTrsignal(trObj.getTrsignal());
                        UpdateTRList.add(trTradingACCObj);
                        break;
                    }
                }
            }
        }
        TRObj stockTRObj = new TRObj();

        stockTRObj.setTrlist(UpdateTRList);

        serviceAFWeb.updateAccountStockSignal(stockTRObj);
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
        int offset = AccountProcess.getOffetDate(StockArray, dateNowL);

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
        return UpdateAllStockTrend(serviceAFWeb, true);
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

    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
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

    public int updateAllStockProcess(ServiceAFweb serviceAFWeb, String NormalizeSymbol, boolean updateTrend) {
        ServiceAFweb.lastfun = "updateAllStockProcess";

//        logger.warning("> updateAllStock " + NormalizeSymbol);
        AFstockObj stock = null;
        // eddy testing
        // yahoo crumb fail not working

        try {
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long currentdate = dateNow.getTimeInMillis();

            stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
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

                    stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
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
            serviceAFWeb.getAccountProcessImp().AddCommObjMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SPLIT, ConstantKey.INT_TYPE_COM_SPLIT, commDataObj);

            return true;
        }
        return false;
    }

    ///////////////////////
    // Need to change for multiple Neural Net weight
    public void uploadNeuralNetWeight(ServiceAFweb serviceAFWeb, int nnNum) {
        String name = CKey.NN_version + "_" + ConstantKey.TR_NN1;
        AFneuralNet nnObj = null;

        StringBuffer msg = FileUtil.FileReadText(ServiceAFweb.FileLocalDebugPath + "nnWeight" + nnNum + ".txt");
        if (msg.length() == 0) {
            return;
        }
        String weightSt = msg.toString();

    }

    public void downloadNeuralNetWeight(ServiceAFweb serviceAFWeb, int nnNum) {
        String name = CKey.NN_version + "_" + ConstantKey.TR_NN1;
        AFneuralNet nnObj = null;
        if (nnNum == 0) {
            nnObj = serviceAFWeb.getNeuralNetObjWeight0(name, 0);
        } else if (nnNum == 1) {
            nnObj = serviceAFWeb.getNeuralNetObjWeight1(name, 0);
        }
        if (nnObj != null) {
            String weightSt = nnObj.getWeight();
            if (weightSt.length() > 0) {
                if (getEnv.checkLocalPC() == true) {
                    StringBuffer msg1 = new StringBuffer(weightSt);
                    FileUtil.FileWriteText(ServiceAFweb.FileLocalDebugPath + "nnWeight" + nnNum + ".txt", msg1);
                }
            }
        }
    }

    public AFneuralNet testNeuralNet0Symbol(ServiceAFweb serviceAFWeb, String nnName, String symbol) {
        String BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
        AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
        if (nnObj0 == null) {
            return null;
        }
        return nnObj0;
    }

    public static String NN40_FILE_1 = "_nn400_";
    public static String NN40_FILE_2 = "_nn401_";
    public static String NN30_FILE_1 = "_nn300_";
    public static String NN30_FILE_2 = "_nn301_";

    public static String NN1_FILE_1 = "_nn1_";
    public static String NN1_FILE_2 = "_nn2_";
    public static String NN2_FILE_1 = "_nn21_";
    public static String NN2_FILE_2 = "_nn22_";
    public static String NN3_FILE_1 = "_nn31_";
    public static String NN3_FILE_2 = "_nn32_";
    public static String NN35_FILE_1 = "_nn351_";
    public static String NN35_FILE_2 = "_nn352_";

    public ArrayList<NNInputDataObj> getTrainingInputDataFromFileProcess(ServiceAFweb serviceAFWeb, String nnName, String symbol) {
        ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
//        symbol = symbol.replace(".", "_");

        if (nnName.equals(ConstantKey.TR_NN30)) {

            String nnIndex = NN30_FILE_1; //"_nn300_";

            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }
            nnIndex = NN30_FILE_2; //"_nn301_";
            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }

            return inputDatalist;
        }

        if (nnName.equals(ConstantKey.TR_NN2)) {

            String nnIndex = NN2_FILE_1; //"_nn21_";

            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }
            nnIndex = NN2_FILE_2; //"_nn22_";
            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }

            return inputDatalist;
        }

        if (nnName.equals(ConstantKey.TR_NN3)) {

            String nnIndex = NN3_FILE_1; //"_nn31_";

            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }
            nnIndex = NN3_FILE_2; //"_nn32_";
            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }

            return inputDatalist;
        }
        /////////////////////////////
        // if (nnName.equals(ConstantKey.TR_NN1 ))        
        /////////////////////////////
        if (nnName.equals(ConstantKey.TR_NN1)) {

            String nnIndex = NN1_FILE_1; // "_nn1_";

            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }
            nnIndex = NN1_FILE_2; //"_nn2_";
            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nnIndex + i + ".csv";
//            logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = readTrainingNeuralNet(serviceAFWeb, inputDatalist, nnFileName, nnName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }

            return inputDatalist;
        }
        return null;
    }

    public ArrayList<NNInputDataObj> getStaticJavaInputDataFromFile(ServiceAFweb serviceAFWeb, String nnName, HashMap<String, ArrayList> stockInputMap) {
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        ArrayList<NNInputDataObj> inputlistRet = new ArrayList();

        String symbol = "";
        String symbolL[] = ServiceAFweb.primaryStock;

        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];
            inputlist = getTrainingInputDataFromFileProcess(serviceAFWeb, nnName, symbol);
            if (inputlist.size() == 0) {
                continue;
            }
            inputlistRet.addAll(inputlist);
            if (stockInputMap != null) {
                stockInputMap.put(symbol, inputlist);
            }
        }

        String symbolAll = "";
        String symbolLAll[] = ServiceAFweb.allStock;

        for (int i = 0; i < symbolLAll.length; i++) {
            symbolAll = symbolLAll[i];
            inputlist = getTrainingInputDataFromFileProcess(serviceAFWeb, nnName, symbolAll);
            if (inputlist.size() == 0) {
                continue;
            }
            inputlistRet.addAll(inputlist);
            if (stockInputMap != null) {
                stockInputMap.put(symbolAll, inputlist);
            }
        }
        return inputlistRet;

    }

    public ArrayList<NNInputDataObj> getStaticJavaAllStockInputDataFromFile(ServiceAFweb serviceAFWeb, String nnName, HashMap<String, ArrayList> stockInputMap) {
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        ArrayList<NNInputDataObj> inputlistRet = new ArrayList();

        String symbol = "";
        String symbolL[] = ServiceAFweb.allStock;

        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];
            inputlist = getTrainingInputDataFromFileProcess(serviceAFWeb, nnName, symbol);
            if (inputlist.size() == 0) {
                return null;
            }
            inputlistRet.addAll(inputlist);
            if (stockInputMap != null) {
                stockInputMap.put(symbol, inputlist);
            }
        }

        return inputlistRet;

    }

    public boolean readTrainingNeuralNet(ServiceAFweb serviceAFWeb, ArrayList<NNInputDataObj> inputlist, String nnFileName, String nnName) {

        ArrayList inputArray = new ArrayList();
        if (FileUtil.FileTest(nnFileName) == false) {
            return false;
        }

        boolean ret = FileUtil.FileReadTextArray(nnFileName, inputArray);
        if (ret == true) {
            for (int i = 0; i < inputArray.size(); i++) {
                String st = (String) inputArray.get(i);
                String[] stList = st.split(",");
                int outputN = CKey.NN_OUTPUT_SIZE;  //2
                if (stList.length != (CKey.NN_INPUT_SIZE + 2 + outputN + 3)) { //12) {
                    continue;
                }
                NNInputDataObj objData = new NNInputDataObj();
                int j = 0;
                int stockId = Integer.parseInt(stList[j++]);
                objData.setUpdatedatel(Long.parseLong(stList[j++]));
                NNInputOutObj obj = new NNInputOutObj();

                obj.setDateSt(stList[j++]);
                obj.setClose(Float.parseFloat(stList[j++]));
                obj.setTrsignal(Integer.parseInt(stList[j++]));

                obj.setOutput1(Double.parseDouble(stList[j++]));
                obj.setOutput2(Double.parseDouble(stList[j++]));

                obj.setInput1(Double.parseDouble(stList[j++]));
                obj.setInput2(Double.parseDouble(stList[j++]));
                obj.setInput3(Double.parseDouble(stList[j++]));
                obj.setInput4(Double.parseDouble(stList[j++]));
                obj.setInput5(Double.parseDouble(stList[j++]));
                obj.setInput6(Double.parseDouble(stList[j++]));
                obj.setInput7(Double.parseDouble(stList[j++]));
                obj.setInput8(Double.parseDouble(stList[j++]));
                obj.setInput9(Double.parseDouble(stList[j++]));
                obj.setInput10(Double.parseDouble(stList[j++]));
//                obj.setInput11(Double.parseDouble(stList[14]));
//                obj.setInput12(Double.parseDouble(stList[15]));

                if (obj.getOutput1() < 0) {
                    continue;
                }
                if (obj.getOutput2() < 0) {
                    continue;
                }
                objData.setObj(obj);
                inputlist.add(objData);

            }
//            logger.info("> readTrainingNeuralNet1 done " + nnFileName + " Size" + inputlist.size());

            return true;
        }
        return false;
    }

    public static AFneuralNet nn1ObjCache = null;
    public static AFneuralNet nn2ObjCache = null;
    public static AFneuralNet nn3ObjCache = null;
    public static AFneuralNet nn30ObjCache = null;
    public static AFneuralNet nn35ObjCache = null;
    public static long lastUpdateTime1 = 0;
    public static long lastUpdateTime2 = 0;
    public static long lastUpdateTime3 = 0;
    public static long lastUpdateTime30 = 0;
    public static long lastUpdateTime35 = 0;

    private AFneuralNet getCacheNNBP(ServiceAFweb serviceAFWeb, NNTrainObj nnTraining) {
        String BPname = nnTraining.getNameNN();
        AFneuralNet nnObj1 = null;
        if (nnTraining.getTrname().equals(ConstantKey.TR_NN1)) {
            if (nn1ObjCache != null) {
                if (nn1ObjCache.getName().equals(BPname)) {

                    long date5Min = TimeConvertion.addMinutes(lastUpdateTime1, 10);
                    long currentTime = System.currentTimeMillis();
                    if (date5Min > currentTime) {
                        nnObj1 = nn1ObjCache;
                    }
                }
            }

            if (nnObj1 == null) {
                nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
                if (nnObj1 == null) {
                    return null;
                }
                nn1ObjCache = nnObj1;
                lastUpdateTime1 = System.currentTimeMillis();
            }
        } else if (nnTraining.getTrname().equals(ConstantKey.TR_NN2)) {
            if (nn2ObjCache != null) {
                if (nn2ObjCache.getName().equals(BPname)) {

                    long date5Min = TimeConvertion.addMinutes(lastUpdateTime2, 10);
                    long currentTime = System.currentTimeMillis();
                    if (date5Min > currentTime) {
                        nnObj1 = nn2ObjCache;
                    }
                }
            }

            if (nnObj1 == null) {
                nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
                if (nnObj1 == null) {
                    return null;
                }
                nn2ObjCache = nnObj1;
                lastUpdateTime2 = System.currentTimeMillis();
            }
        } else if (nnTraining.getTrname().equals(ConstantKey.TR_NN3)) {
            if (nn3ObjCache != null) {
                if (nn3ObjCache.getName().equals(BPname)) {

                    long date5Min = TimeConvertion.addMinutes(lastUpdateTime3, 10);
                    long currentTime = System.currentTimeMillis();
                    if (date5Min > currentTime) {
                        nnObj1 = nn3ObjCache;
                    }
                }
            }

            if (nnObj1 == null) {
                nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
                if (nnObj1 == null) {
                    return null;
                }
                nn3ObjCache = nnObj1;
                lastUpdateTime3 = System.currentTimeMillis();
            }
        } else if (nnTraining.getTrname().equals(ConstantKey.TR_NN30)) {
            if (nn30ObjCache != null) {
                if (nn30ObjCache.getName().equals(BPname)) {

                    long date5Min = TimeConvertion.addMinutes(lastUpdateTime30, 10);
                    long currentTime = System.currentTimeMillis();
                    if (date5Min > currentTime) {
                        nnObj1 = nn30ObjCache;
                    }
                }
            }

            if (nnObj1 == null) {
                nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
                if (nnObj1 == null) {
                    return null;
                }
                nn30ObjCache = nnObj1;
                lastUpdateTime30 = System.currentTimeMillis();
            }

        } else {
            logger.info("> OutputNNBP exception - need to define new cache " + nnTraining.getTrname());
            nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
            if (nnObj1 == null) {
                return null;
            }
        }
        return nnObj1;
    }

    public int OutputNNBP(ServiceAFweb serviceAFWeb, NNTrainObj nnTraining) {
        double[][] inputpattern = null;
        double[][] targetpattern = null;
        double[][] response = null;
        if (nnTraining == null) {
            return 0;
        }

        String BPname = nnTraining.getNameNN();
        if (BPname == null) {
            return 0;
        }
        AFneuralNet nnObj1 = null;
        nnObj1 = this.getCacheNNBP(serviceAFWeb, nnTraining);
        if (nnObj1 == null) {
            return 0;
        }
        String weightSt1 = nnObj1.getWeight();
        if (weightSt1.length() > 0) {
            NNBPservice nn1 = new NNBPservice();
            nn1.createNet(weightSt1);
            inputpattern = nnTraining.getInputpattern();
            targetpattern = nnTraining.getOutputpattern();
            response = nnTraining.getResponse();
            double nnError = 0.0001;
            double errorReturn = nn1.predictTest(inputpattern, targetpattern, response, nnError);

            return 1;
        }
        return 0;
    }

    public static boolean forceToInitleaningNewNN = false;
    public static boolean forceToGenerateNewNN = false;
    public static boolean forceToErrorNewNN = false;

    public int TrainingNNBP(ServiceAFweb serviceAFWeb, String nnNameSym, String nnNAme, NNTrainObj nnTraining, double nnError) {
        ServiceAFweb.lastfun = "TrainingNNBP";

        int inputListSize = CKey.NN_INPUT_SIZE; //12;
        int outputSize = CKey.NN_OUTPUT_SIZE; //2;
        int middleSize = CKey.NN1_MIDDLE_SIZE;

        //
        double[][] inputpattern = null;
        double[][] targetpattern = null;
        double[][] response = null;

        if (nnTraining == null) {
            return -1;
        }

        String name = nnTraining.getNameNN();
        if (name == null) {
            return -1;
        }

        NNBPservice nn = null;
        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release

        AFneuralNet afNeuralNet = serviceAFWeb.getNeuralNetObjWeight1(name, 0);

        if (forceToErrorNewNN == true) {
            nnError = nnError - 0.002;
        }
        if (forceToGenerateNewNN == true) {
            // force to save new NN
            afNeuralNet = null;
            nnError = 2;
        }
        if (afNeuralNet != null) {
            String weightSt = afNeuralNet.getWeight();

            if (weightSt.length() > 0) {
                nn = new NNBPservice();
                nn.createNet(weightSt);
                inputpattern = nn.getInputpattern();
                targetpattern = nn.getOutputpattern();

            } else {
                //Weight too large when save. So, alway empty
//                if (ServiceAFweb.forceNNReadFileflag == true) {
//                    nn = new NNBPservice();
//                    weightSt = CKey.NN1_WEIGHT_0;
//                    nn.createNet(weightSt);
//                    inputpattern = nn.getInputpattern();
//                    targetpattern = nn.getOutputpattern();
//
//                }
            }
        } else {
            //Get again
            afNeuralNet = new AFneuralNet();
            afNeuralNet.setName(name);
            afNeuralNet.setStatus(ConstantKey.OPEN);
            afNeuralNet.setType(0);
            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
            afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
            afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
            String weightSt = "";
            afNeuralNet.setWeight(weightSt);
            serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
        }
        if (nn == null) {
            nn = new NNBPservice();
            nn.create(inputListSize, middleSize, outputSize);
            inputpattern = nnTraining.getInputpattern();
            targetpattern = nnTraining.getOutputpattern();

            nn.setInputpattern(inputpattern);
            nn.setOutputpattern(targetpattern);

        }

        if (nn.getInputpattern() == null) {
            inputpattern = nnTraining.getInputpattern();
            targetpattern = nnTraining.getOutputpattern();

            nn.setInputpattern(inputpattern);
            nn.setOutputpattern(targetpattern);
        }

        int targetLength = targetpattern.length;
        response = new double[targetLength][outputSize];
        for (int i = 0; i < targetLength; i++) {
            response[i] = new double[outputSize];
        }

        String nNetName = afNeuralNet.getName();
        int repeatSize = 60000;

        if (TradingSignalProcess.forceToInitleaningNewNN == true) {
            repeatSize = 100000;
        }

        double errorReturn = 1;

//        /// exit when tried repeatSize without reaching the error threshold
//        if (afNeuralNet.getType() > 1) {
//            nnError = 1;
//        }
        logger.info("> TrainingNNBP inputpattern " + inputpattern.length);
        errorReturn = nn.learn(nNetName, inputpattern, targetpattern, response, repeatSize, nnError);
        double nnErrorOrg = nnError;
        double minError = nn.minError;

        String weightSt1 = nn.getNetObjSt();
        //Weight too large when save. So, alway empty
        //Weight too large when save. So, alway empty
        //Weight too large when save. So, alway empty        
        NNBPservice nnTemp = new NNBPservice();
        nnTemp.createNet(weightSt1);
        nnTemp.setInputpattern(null);
        nnTemp.setOutputpattern(null);
        String weightSt = nnTemp.getNetObjSt();
        afNeuralNet.setWeight(weightSt);
        //Weight too large when save. So, alway empty  

        afNeuralNet.setType(afNeuralNet.getType() + 1);
        serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
        serviceAFWeb.getStockImp().updateNeuralNetStatus1(name, ConstantKey.OPEN, afNeuralNet.getType());

//        if (getEnv.checkLocalPC() == true) {
//            StringBuffer msg1 = new StringBuffer(weightSt);
//            FileUtil.FileWriteText(ServiceAFweb.FileLocalDebugPath + nnNameSym + "_nnWeight1.txt", msg1);
//        }
        int retFlag = 0;

        if (errorReturn > nnError) {
            /// force to release if type =2;
            if (afNeuralNet.getType() > 1) {
                nnError = 1;
            }
        }
        if (errorReturn < nnError) {

            retFlag = 1;
            //////////// training completed and release the NN
            serviceAFWeb.releaseNeuralNetObj(name);

            if (nnError == 1) {
                ReferNameData refData = serviceAFWeb.getReferNameData(afNeuralNet);
                refData.setmError(minError);

                serviceAFWeb.getStockImp().updateNeuralNetRef0(name, refData);
                logger.info("> TrainingNNBP override new minError " + name + " " + minError);

            } else {

                ReferNameData refData = serviceAFWeb.getReferNameData(afNeuralNet);
                if (refData.getmError() != 0) {
                    double refError = refData.getmError();
                    double refminError = minError + 0.002; //+ 0.001;
                    if (refminError < refError) {
                        if (nnErrorOrg < minError) {
                            refData.setmError(minError);
                            serviceAFWeb.getStockImp().updateNeuralNetRef0(name, refData);
                            logger.info("> TrainingNNBP override new minError " + name + " " + refminError);
                        }
                    }
                }

//                String refName = afNeuralNet.getRefname();
//                if (refName != null) {
//                    try {
//                        if (refName.length() > 0) {
//                            double refError = Double.parseDouble(refName);
//                            double refminError = minError + 0.002; //+ 0.001;
//                            if (refminError < refError) {
//                                ReferNameData refData = afNeuralNet.getReferNameData();
//                                refData.setMinError(refminError);
//                                serviceAFWeb.getStockImp().updateNeuralNetRef0(name, refData);
//                                logger.info("> TrainingNNBP override new minError " + name + " " + refminError);
//                            }
//                        }
//                    } catch (Exception ex) {
//                        logger.info("> TrainingNNBP Exception " + ex.getMessage());
//                    }
//
//                }
            }
            if (getEnv.checkLocalPC() == true) {

                AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(name, 0);
                if (nnObj0 != null) {
                    String weightSt0 = nnObj0.getWeight();
                    if (weightSt0.length() > 0) {
                        NNBPservice nn0 = new NNBPservice();
                        nn0.createNet(weightSt0);
                        errorReturn = nn0.predictTest(inputpattern, targetpattern, response, nnError);

                        double[] input;
                        double[] output;
                        double[] rsp;
                        ArrayList writeArray = new ArrayList();
                        int num0 = 0;
                        int num1 = 0;
                        int num0Err = 0;
                        int num1Err = 0;
                        for (int j = 0; j < inputpattern.length; j++) {
                            input = inputpattern[j];
                            output = targetpattern[j];
                            rsp = response[j];
                            if (j == 0) {

                                String stTitle = "" + "output0"
                                        + "," + "output1"
                                        + "," + "macd TSig"
                                        + "," + "LTerm"
                                        + "," + "ema2050" + "," + "macd" + "," + "rsi"
                                        + "," + "close-0" + "," + "close-1" + "," + "close-2" + "," + "close-3" + "," + "close-4"
                                        + "," + "predict0" + "," + "predict1" + "";
                                writeArray.add(stTitle);
                            }

                            String st = "";

                            st = "" + output[0]
                                    + "," + output[1]
                                    + "," + input[0] + "," + input[1] + "," + input[2]
                                    + "," + input[3] + "," + input[4] + "," + input[5]
                                    + "," + input[6] + "," + input[7]
                                    + "," + input[8] + "," + input[9]
                                    + "," + rsp[0] + "," + rsp[1]
                                    + "";
                            if (output[0] > CKey.PREDICT_THRESHOLD) {
                                num0++;
                            }
                            if (output[1] > CKey.PREDICT_THRESHOLD) {
                                num1++;
                            }
                            float delta = (float) (output[0] - rsp[0]);
                            delta = Math.abs(delta);
                            float deltaCmp = (float) CKey.PREDICT_THRESHOLD;

                            if (delta > deltaCmp) {
                                st += "," + delta + "";
                                num0Err++;
                            }
                            float delta1 = (float) (output[1] - rsp[1]);
                            delta1 = Math.abs(delta1);

                            if (delta1 > deltaCmp) {
                                st += "," + delta1 + "";
                                num1Err++;
                            }
                            writeArray.add(st);
                        }
                        logger.info("> predictTest release " + num0 + " num0Err=" + num0Err + ", " + num1 + " num1Err=" + num1Err);

                        if (ServiceAFweb.processRestinputflag == true) {
                            boolean flag = true;
                            if (flag == true) {
                                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + nnNameSym + "_nnPredit.csv", writeArray);

                                StringBuffer msg = new StringBuffer(weightSt0);
                                FileUtil.FileWriteText(ServiceAFweb.FileLocalDebugPath + nnNameSym + "_nnWeight0.txt", msg);
                            }
                        }
                    }
                }
            }
            return retFlag;
        } // training completed and release the NN

        if (nnError == 0) {
            return retFlag;
        }

        if (getEnv.checkLocalPC() == true) {

            AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(name, 0);
            if (nnObj1 != null) {
                String weightSt11 = nnObj1.getWeight();
                if (weightSt1.length() > 0) {
                    NNBPservice nn1 = new NNBPservice();
                    nn1.createNet(weightSt11);
                    errorReturn = nn1.predictTest(inputpattern, targetpattern, response, nnError);

                    double[] output;
                    double[] rsp;
                    int num0 = 0;
                    int num1 = 0;
                    int num0Err = 0;
                    int num1Err = 0;
                    for (int j = 0; j < inputpattern.length; j++) {

                        output = targetpattern[j];
                        rsp = response[j];

                        if (output[0] > CKey.PREDICT_THRESHOLD) {
                            num0++;
                        }
                        if (output[1] > CKey.PREDICT_THRESHOLD) {
                            num1++;
                        }
                        float delta = (float) (output[0] - rsp[0]);
                        delta = Math.abs(delta);
                        float deltaCmp = (float) CKey.PREDICT_THRESHOLD;

                        if (delta > deltaCmp) {
                            num0Err++;
                        }
                        float delta1 = (float) (output[1] - rsp[1]);
                        delta1 = Math.abs(delta1);

                        if (delta1 > deltaCmp) {
                            num1Err++;
                        }
                    }
                    logger.info("> predictTest nnObj1 " + num0 + " num0Err=" + num0Err + ", " + num1 + " num1Err=" + num1Err);
                }
            }
        } // training completed and release the NN
        return retFlag;

    }

    //////////////////////////////////////// transaction order
    // user add buy or sell transaction manually
    public int AddTransactionOrderWithComm(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, String trName, int tranSignal) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        int ret = TRprocessImp.AddTransactionOrder(serviceAFWeb, accountObj, stock, trName, tranSignal, null, false);
        if (ret == 1) {
            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), trName);

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.sql.Date d = new java.sql.Date(trObj.getUpdatedatel());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);

            String sig = "exit";
            if (trObj.getTrsignal() == ConstantKey.S_BUY) {
                sig = ConstantKey.S_BUY_ST;
            } else if (trObj.getTrsignal() == ConstantKey.S_SELL) {
                sig = ConstantKey.S_SELL_ST;
            }

            CustomerObj cust = serviceAFWeb.getCustomerByAccoutObj(accountObj);
            if (cust.getType() == CustomerObj.INT_API_USER) {
                DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                formatD.setTimeZone(tz);
                String ESTdateD = formatD.format(d);

                serviceAFWeb.getAccountProcessImp().AddCommAPISignalMessage(serviceAFWeb, accountObj, trObj, ESTdateD, stock.getSymbol(), sig);

            } else {
                String accTxt = "acc-" + accountObj.getId();
                String msg = ESTdate + " " + accTxt + " " + stock.getSymbol() + " Sig:" + sig;
                serviceAFWeb.getAccountProcessImp().AddCommSignalMessage(serviceAFWeb, accountObj, trObj, msg);
            }
        }
        return ret;

    }

    public int AddTransactionOrder(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, String trName, int tranSignal, Calendar tranDate, boolean fromSystem) {
        try {
            ArrayList<TransationOrderObj> currTranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName, 1);
            TradingRuleObj tradingRuleObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), trName);

            ArrayList transObjList = AddTransactionOrderProcess(currTranOrderList, tradingRuleObj, accountObj, stock, trName, tranSignal, tranDate, fromSystem);

            if ((transObjList != null) && (transObjList.size() > 0)) {
                ArrayList<String> transSQL = new ArrayList();
                for (int i = 0; i < transObjList.size(); i += 2) {
                    TransationOrderObj trOrder = (TransationOrderObj) transObjList.get(i);
                    TradingRuleObj trObj = (TradingRuleObj) transObjList.get(i + 1);

                    String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);

                    transSQL.add(trOrderSql);
                    String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
                    transSQL.add(trSql);
                }
                return serviceAFWeb.SystemuUpdateTransactionOrder(transSQL);
            }
        } catch (Exception e) {
            logger.info("> AddTransactionOrder exception " + e.getMessage());
        }
        return 0;
    }

    public ArrayList AddTransactionOrderProcess(ArrayList<TransationOrderObj> currTranOrderList, TradingRuleObj trObj, AccountObj accountObj, AFstockObj stock, String trName, int currentSignal, Calendar tranDate, boolean fromSystem) {
        ServiceAFweb.lastfun = "AddTransactionOrderProcess";

        ArrayList transSQL = new ArrayList();
        Calendar daOffset0 = tranDate;
        Calendar daOffset = tranDate;
        try {
            if (stock.getAfstockInfo() == null) {
                return null;
            }
            if (tranDate == null) {
                daOffset0 = TimeConvertion.getCurrentCalendar();
                daOffset = TimeConvertion.getCurrentCalendar();
            }
            boolean buyOnly = false;
            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                buyOnly = true;
            }
            //get the current transaction order to see the last transaction
            int currentTranOrderSiganl = ConstantKey.S_NEUTRAL;
            if (currTranOrderList != null) {
                if (currTranOrderList.size() > 0) {
                    TransationOrderObj tranOrder = currTranOrderList.get(0);
                    currentTranOrderSiganl = tranOrder.getTrsignal();
                    if ((currentTranOrderSiganl == ConstantKey.S_EXIT_LONG) || (currentTranOrderSiganl == ConstantKey.S_EXIT_SHORT)) {
                        currentTranOrderSiganl = ConstantKey.S_NEUTRAL;
                    }
                }
            }
            int tranSiganl_0 = ConstantKey.S_NEUTRAL;
            int tranSiganl_1 = ConstantKey.S_NEUTRAL;

            switch (currentSignal) {
                case ConstantKey.S_LONG_BUY:
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        return null;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        tranSiganl_0 = ConstantKey.S_EXIT_SHORT;
                        tranSiganl_1 = ConstantKey.S_LONG_BUY;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_NEUTRAL) {
                        tranSiganl_0 = ConstantKey.S_LONG_BUY;
                    }
                    break;
                case ConstantKey.S_SHORT_SELL:
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        return null;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        tranSiganl_0 = ConstantKey.S_EXIT_LONG;
                        tranSiganl_1 = ConstantKey.S_SHORT_SELL;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_NEUTRAL) {
                        tranSiganl_0 = ConstantKey.S_SHORT_SELL;
                    }
                    break;
                case ConstantKey.S_NEUTRAL:
                    if (currentTranOrderSiganl == ConstantKey.S_LONG_BUY) {
                        tranSiganl_0 = ConstantKey.S_EXIT_LONG;
                        tranSiganl_1 = ConstantKey.S_NEUTRAL;
                    }
                    if (currentTranOrderSiganl == ConstantKey.S_SHORT_SELL) {
                        tranSiganl_0 = ConstantKey.S_EXIT_SHORT;
                        tranSiganl_1 = ConstantKey.S_NEUTRAL;
                    }
                    break;
            }

            if (tranSiganl_1 != ConstantKey.S_NEUTRAL) {
                long newDaL = TimeConvertion.addMiniSeconds(daOffset0.getTimeInMillis(), -10);
                daOffset0 = TimeConvertion.getCurrentCalendar(newDaL);
            }

            int retTrans = 1;
            if (tranSiganl_0 != ConstantKey.S_NEUTRAL) {
                //process buysell

                int signal = tranSiganl_0;
                Calendar dateOffset = daOffset0;
                switch (signal) {
                    case ConstantKey.S_LONG_BUY:
                        retTrans = TransactionOrderLONG_BUY(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_LONG:
                        retTrans = TransactionOrderEXIT_LONG(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_SHORT_SELL:
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    default:
                        break;
                }
            }

            // exit if error
            if (retTrans == 0) {
                return null;
            }
            retTrans = 1;
            if (tranSiganl_1 != ConstantKey.S_NEUTRAL) {
                //process buysell
                int signal = tranSiganl_1;
                Calendar dateOffset = daOffset;
                switch (signal) {
                    case ConstantKey.S_LONG_BUY:
                        retTrans = TransactionOrderLONG_BUY(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_LONG:
                        retTrans = TransactionOrderEXIT_LONG(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_SHORT_SELL:
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL, buyOnly);
                        break;
                    default:
                        break;
                }
            }
            // exit if error
            if (retTrans == 0) {
                return null;
            }
            if (fromSystem == false) {
                if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                    if (trObj.getLinktradingruleid() != 0) {
                        logger.info("> transactionOrder not allow when linking other TS");
                        return null;
                    }
                }
            }
            if (transSQL.size() > 0) {
//                int ret = updateTransactionOrder(transSQL);
                return transSQL;
            }

        } catch (Exception e) {
            logger.info("> transactionOrder exception " + e.getMessage());
        }
        return null;
    }

    static TradingRuleObj duplicateTRObj(TradingRuleObj trObj) {
        TradingRuleObj dupObj = null;
        String st;
        try {
            st = new ObjectMapper().writeValueAsString(trObj);
            dupObj
                    = new ObjectMapper().readValue(st, TradingRuleObj.class
                    );
            dupObj.setUpdatedatedisplay(new java.sql.Date(dupObj.getUpdatedatel()));
            return dupObj;
        } catch (Exception ex) {
            logger.info("> duplicateTRObj Exception" + ex.getMessage());
        }
        return null;
    }

    private int TransactionOrderEXIT_SHORT(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL, boolean buyOnly) {
        float curPrice = stock.getAfstockInfo().getFclose();

        float originalPrice = trObj.getShortamount() / trObj.getShortshare();
        float deltaPrice = curPrice - originalPrice; //final price - original price
        deltaPrice = -deltaPrice; // negative for exit short
        float netPrice = originalPrice + deltaPrice;

        float amount = trObj.getShortshare() * netPrice;

        if (buyOnly == true) {
            // TR ACC can only support BUY transaction
        } else {
            trObj.setBalance(trObj.getBalance() + amount);
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(trObj.getShortshare());
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //EXIT_SHORT
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setShortshare(0);
        trObj.setShortamount(0);
        //update trObj

        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);

//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       
        return 1;
    }

    private int TransactionOrderSHORT_SELL(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL, boolean buyOnly) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float shareTmp = CKey.TRADING_AMOUNT / curPrice;  //$6000
        shareTmp += 0.5;
        if (shareTmp == 0) {
            shareTmp = 1;
        }
        int shareInt = (int) shareTmp;
        float amount = curPrice * shareInt;

        if (buyOnly == true) {
            // TR ACC can only support BUY transaction
            shareInt = 0;
            amount = 0;
        } else {

            if (trObj.getBalance() < amount) {
                trObj.setInvestment(trObj.getInvestment() + amount);

            } else {
                trObj.setBalance(trObj.getBalance() - amount);
            }
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(shareInt);
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //SHORT_SELL
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setShortshare(shareInt);
        trObj.setShortamount(amount);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    private int TransactionOrderEXIT_LONG(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float amount = curPrice * trObj.getLongshare();

        trObj.setBalance(trObj.getBalance() + amount);

        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(trObj.getLongshare());
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //EXIT_LONG
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setLongshare(0);
        trObj.setLongamount(0);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    private int TransactionOrderLONG_BUY(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float shareTmp = CKey.TRADING_AMOUNT / curPrice;  //$6000
        shareTmp += 0.5;
        if (shareTmp == 0) {
            shareTmp = 1;
        }
        int shareInt = (int) shareTmp;
        float amount = curPrice * shareInt;

        if (trObj.getBalance() < amount) {
            trObj.setInvestment(trObj.getInvestment() + amount);

        } else {
            trObj.setBalance(trObj.getBalance() - amount);
        }
        // add trading order
        TransationOrderObj trOrder = new TransationOrderObj();
        trOrder.setAccountid(trObj.getAccountid());
        trOrder.setAvgprice(curPrice);
        trOrder.setEntrydatedisplay(new java.sql.Date(dateOffset.getTimeInMillis()));
        trOrder.setEntrydatel(dateOffset.getTimeInMillis());
        trOrder.setShare(shareInt);
        trOrder.setStatus(ConstantKey.OPEN);
        trOrder.setStockid(stock.getId());
        trOrder.setSymbol(stock.getSymbol());
        trOrder.setTradingruleid(trObj.getId());
        trOrder.setTrname(trObj.getTrname());
        trOrder.setTrsignal(siganl);  //LONG_BUY
        trOrder.setType(ConstantKey.INITIAL);
        trOrder.setComment("");
        transSQL.add(trOrder);

//        String trOrderSql = AccountDB.SQLaddAccountStockTransaction(trOrder);
//        transSQL.add(trOrderSql);
        // add trading order                                                
        trObj.setTrsignal(trOrder.getTrsignal());
        trObj.setUpdatedatedisplay(trOrder.getEntrydatedisplay());
        trObj.setUpdatedatel(trOrder.getEntrydatel());
        trObj.setLongshare(shareInt);
        trObj.setLongamount(amount);
        //update trObj
        TradingRuleObj trandingRuleObj = duplicateTRObj(trObj);
        transSQL.add(trandingRuleObj);
//        String trSql = AccountDB.SQLUpdateAccountStockSignal(trObj);
//        transSQL.add(trSql);
        //update trObj       

        return 1;
    }

    ////////////////////////////////////////
}
