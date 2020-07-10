/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

import com.afweb.nnprocess.TradingNNprocess;
import com.afweb.account.*;
import com.afweb.model.*;

import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.service.ServiceAFweb;

import com.afweb.nn.*;

import com.afweb.nnBP.NNBPservice;

import com.afweb.stock.*;

import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Date;
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
public class TrandingSignalProcess {

    protected static Logger logger = Logger.getLogger("TrandingSignalProcess");

    private ServiceAFweb serviceAFWeb = null;
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
        this.serviceAFWeb = serviceAFWeb;
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        UpdateStockSignalNameArray(accountAdminObj);
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

        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {

            long currentTime = System.currentTimeMillis();
            long lockDate2Min = TimeConvertion.addMinutes(currentTime, 2);
            logger.info("ProcessAdminSignalTrading for 2 minutes stocksize=" + stockSignalNameArray.size());

            for (int i = 0; i < 10; i++) {
                currentTime = System.currentTimeMillis();
                if (lockDate2Min < currentTime) {
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
                            return;
                        }
                    }
                    if (stock == null) {
                        continue;
                    }

                    String LockStock = "ADM_" + symbol;
                    LockStock = LockStock.toUpperCase();

                    long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                    long lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");
                    if (CKey.NN_DEBUG == true) {
                        lockReturnStock = 1;
                    }
                    if (lockReturnStock > 0) {
                        int ret = updateStockProcess(serviceAFWeb, symbol);
                        if (ret > 0) {

                            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountAdminObj.getId(), stock.getId(), ConstantKey.TR_MACD);
                            if (trObj != null) {
                                long lastUpdate = trObj.getUpdatedatel();
                                long lastUpdate5Min = TimeConvertion.addMinutes(lastUpdate, 5);

                                long curDateValue = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                                if (lastUpdate5Min < curDateValue) {
//                                    logger.info("> ProcessAdminSignalTrading " + symbol + " stocksize=" + stockSignalNameArray.size());
                                    updateAdminTradingsignal(serviceAFWeb, accountAdminObj, symbol);
                                    upateAdminTransaction(serviceAFWeb, accountAdminObj, symbol);
                                    upateAdminPerformance(serviceAFWeb, accountAdminObj, symbol);
                                }
                            }
                        }
                        serviceAFWeb.removeNameLock(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
                    }
                } catch (Exception ex) {
                    logger.info("> ProcessAdminSignalTrading Exception" + ex.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
        }
    }
//////////////////////////////////////////////////
// get one yr performance

    public void upateAdminPerformance(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        // update Trading signal
        //testing
//        symbol = "DIA";
        //testing

        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
        ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);

        for (int j = 0; j < tradingRuleList.size(); j++) {
            TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
            if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                continue;
            }
            // process performance
            String trName = trObj.getTrname();
            ArrayList<TransationOrderObj> tranOrderList = serviceAFWeb.SystemAccountStockTransList(accountObj.getId(), stock.getId(), trName.toUpperCase(), 0);
            if ((tranOrderList == null) || (tranOrderList.size() == 0)) {
                continue;
            }
            // get one yr performance
            ArrayList<PerformanceObj> performanceList = ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, 1);
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

    public ArrayList<PerformanceObj> ProcessTranPerfHistoryReinvest(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length) {

        int lengthYr = 2;
        int size1year = 20 * 12 * lengthYr + (20 * 3);
        int offset = 0;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        return ProcessTranPerfHistoryOffset(serviceAFWeb, tranOrderList, StockArray, offset, length, true); // reinvest = true
    }

    public ArrayList<PerformanceObj> ProcessTranPerfHistory(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length) {
        int lengthYr = 2;
        int size1year = 20 * 12 * lengthYr + (20 * 3);
        int offset = 0;
        ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        return ProcessTranPerfHistoryOffset(serviceAFWeb, tranOrderList, StockArray, offset, length, false); // reinvest = false
    }

    public ArrayList<PerformanceObj> ProcessTranPerfHistoryOffset(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, ArrayList<AFstockInfo> StockArray, int offset, int length, boolean reinvest) {

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
                if (lastStockInfoEOD >= trObj.getEntrydatel()) {
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

                // both are end of day so it must equal
                if (tranObjEOD == stockInfoEOD) {
                    if (tranObjIndex >= 0) {
                        tranObjIndex--;
                    }
                } else {
                    stockInfoIndex--;
                    continue;
                }

                perfObj.setUpdatedatel(tranObj.getEntrydatel());
                perfObj.setUpdateDateD(tranObj.getUpdateDateD());
                perfObj.setUpdatedatedisplay(tranObj.getEntrydatedisplay());

                perfObj.setName(tranObj.getTrname());
                perfObj.setIndexDate(stockInfoIndex);

                if (writeArray.size() == 0) {
                    perfObj.setStartdate(tranObj.getEntrydatedisplay());
                }
                if (reinvest == true) {
                    perfObj = processTranPerfReinvest(perfObj, lastPerfObj, stockinfo, tranObj);

                } else {
                    perfObj = processTranPerf(perfObj, lastPerfObj, stockinfo, tranObj);
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

    private PerformanceObj processTranPerf(PerformanceObj pObj, PerformanceObj lastPerfObj, AFstockInfo stockinfo, TransationOrderObj tranObj) {
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
                }

                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(tranObj.getShare());
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + invest - pObj.getInvestment();
                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if (currentSignal == ConstantKey.S_EXIT_LONG) {

                float invest = tranObj.getShare() * tranObj.getAvgprice();
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

                pObj.setRating(2);
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
                if (pObj.getBalance() >= invest) {
                    pObj.setBalance(pObj.getBalance() - invest);
                } else {
                    pObj.setInvestment(pObj.getInvestment() + invest);
                }
                perfData.setClose(tranObj.getAvgprice());
                perfData.setShare(tranObj.getShare());
                pObj.setNumtrade(pObj.getNumtrade() + 1);

                float fProfit = pObj.getBalance() + invest - pObj.getInvestment();

                pObj.setGrossprofit(fProfit);
                float nProfit = fProfit - (pObj.getNumtrade() * CKey.TRADING_COMMISSION);
                pObj.setNetprofit(nProfit);

            } else if (currentSignal == ConstantKey.S_EXIT_SHORT) {

                float deltaPrice = tranObj.getAvgprice() - perfData.getClose(); //final price - original price
                deltaPrice = -deltaPrice;  // negative for exit short
                float netPrice = pObj.getPerformData().getClose() + deltaPrice;

                float invest = tranObj.getShare() * netPrice;

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
                pObj.setRating(2);
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

                pObj.setRating(2);
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
                pObj.setRating(2);
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
    public void upateAdminTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        try {
            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            if (stock != null) {
                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    return;
                }
            }
            ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);
            Calendar dateNow = TimeConvertion.getCurrentCalendar();

            TradingRuleObj NN_TR2Obj = null;
            for (int j = 0; j < tradingRuleList.size(); j++) {
                TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);
                if (trObj.getType() == ConstantKey.INT_TR_ACC) {
                    continue;
                }
                int subStatus = trObj.getSubstatus();

                if (trObj.getType() == ConstantKey.INT_TR_NN1) {
                    AFneuralNet nnObj0 = testNeuralNet0Symbol(serviceAFWeb, trObj.getType(), symbol);
                    if (nnObj0 == null) {
                        continue;
                    }

                    if (nnObj0.getStatus() != ConstantKey.OPEN) {
                        continue;
                    }
                } else if (trObj.getType() == ConstantKey.INT_TR_NN2) {

                    AFneuralNet nnObj0 = testNeuralNet0Symbol(serviceAFWeb, trObj.getType(), symbol);
                    if (nnObj0 == null) {
                        continue;
                    }

                    if (nnObj0.getStatus() != ConstantKey.OPEN) {
                        continue;
                    }
                    NN_TR2Obj = trObj;
                } else if (trObj.getType() == ConstantKey.INT_TR_NN3) {
                    if (NN_TR2Obj == null) {
                        continue;
                    }
                    if (NN_TR2Obj.getStatus() != ConstantKey.OPEN) {
                        continue;
                    }
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
                    serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, stock.getId(), trObj.getTrname());

                    // get 2 year
//                    ArrayList<StockTRHistoryObj> trHistoryList = serviceAFWeb.SystemProcessTRHistory(trObj, 2);
                    ArrayList<StockTRHistoryObj> trHistoryList = ProcessTRHistory(serviceAFWeb, trObj, 2);

                    if ((trHistoryList == null) || (trHistoryList.size() == 0)) {
                        continue;
                    }

                    if (lockExit90M < TimeConvertion.getCurrentCalendar().getTimeInMillis()) {
                        //exit
                        logger.info("> upateAdminTransaction Process too long " + stock.getSymbol());
                        continue;
                    }

                    if (CKey.NN_DEBUG == true) {
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

                    StockTRHistoryObj trHistory = trHistoryList.get(trHistoryList.size() - 1);
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
            logger.info("> upateAdminTransaction Exception" + ex.getMessage());
        }
    }

    public ArrayList<StockTRHistoryObj> ProcessTRHistory(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, int lengthYr) {

        int size1year = 20 * 12 * lengthYr + (50 * 3);
        String symbol = trObj.getSymbol();
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
        int offset = 0;

        ArrayList<StockTRHistoryObj> thObjList = ProcessTRHistoryOffset(serviceAFWeb, trObj, StockArray, offset, CKey.MONTH_SIZE);
        return thObjList;
    }

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

//        long stockOffsetL = StockArray.get(offsetS).getEntrydatel();
//        stockOffsetL = TimeConvertion.endOfDayInMillis(stockOffsetL);
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
            trHistory.setAfstockInfo(stockinfo);
            trHistory.setParmSt1("");

            // adding > parameter 4 need to udpate getAccountStockTRListHistoryDisplay
            switch (trObj.getType()) {
                case ConstantKey.INT_TR_MV:
                    // check if signal to buy or sell
                    EMAObj ema2050 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_MV_20, ConstantKey.INT_MV_50);
                    trObj.setTrsignal(ema2050.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) ema2050.ema);
                    trHistory.setParm2((float) ema2050.lastema);
                    float LTerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.LONG_TERM_TREND);
                    float STerm = (float) TechnicalCal.TrendUpDown(StockArray, offset, StockImp.SHORT_TERM_TREND);
                    trHistory.setParm3((float) LTerm);
                    trHistory.setParm4((float) STerm);
                    break;
                case ConstantKey.INT_TR_MACD: {
                    MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                    trObj.setTrsignal(macd.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd.macd);
                    trHistory.setParm2((float) macd.signal);
                    trHistory.setParm3((float) macd.diff);
                    break;
                }
                case ConstantKey.INT_TR_MACD1: {
                    MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                    trObj.setTrsignal(macd.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd.macd);
                    trHistory.setParm2((float) macd.signal);
                    trHistory.setParm3((float) macd.diff);
                    break;
                }
                case ConstantKey.INT_TR_MACD2: {
                    MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD2_24, ConstantKey.INT_MACD2_48, ConstantKey.INT_MACD2_18);
                    trObj.setTrsignal(macd.trsignal);
                    trHistory.setTrsignal(trObj.getTrsignal());
                    trHistory.setParm1((float) macd.macd);
                    trHistory.setParm2((float) macd.signal);
                    trHistory.setParm3((float) macd.diff);
                    break;
                }
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
                        int nn1Signal = nn1.ProcessTRHistoryOffsetNN1(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList);
                        prevSignal = nn1Signal;
                    }
                    break;
                case ConstantKey.INT_TR_NN2:
                    boolean nn2Flag = true;
                    if (nn2Flag == true) {
                        ProcessNN2 nn2 = new ProcessNN2();
                        int nn2Signal = nn2.ProcessTRHistoryOffsetNN2(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList, writeArray);
                        prevSignal = nn2Signal;
                    }
                    break;
                case ConstantKey.INT_TR_NN3:
                    boolean nn3Flag = true;
                    if (nn3Flag == true) {
                        ProcessNN3 nn3 = new ProcessNN3();
                        int nn3Signal = nn3.ProcessTRHistoryOffsetNN3(serviceAFWeb, trObj, StockArray, offsetInput, monthSize, prevSignal, offset, stdate, trHistory, accountObj, stock, tradingRuleList);
                        prevSignal = nn3Signal;
                    }
                    break;
                default:
                    break;
            }
            writeArray.add(trHistory);
        }
        return writeArray;
    }

    public void testUpdateAdminTradingsignal(ServiceAFweb serviceAFWeb, String symbol) {
        this.serviceAFWeb = serviceAFWeb;
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        updateAdminTradingsignal(serviceAFWeb, accountAdminObj, symbol);
        upateAdminTransaction(serviceAFWeb, accountAdminObj, symbol);
//        upateAdminPerformance(serviceAFWeb, accountAdminObj, symbol);
    }

    public void updateAdminTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);

        if (stock != null) {
            if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                return;
            }
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long endofDate = TimeConvertion.endOfDayInMillis(dateNow.getTimeInMillis());
        dateNow = TimeConvertion.getCurrentCalendar(endofDate);

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
                    trObj.setTrsignal(ema2050.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                case ConstantKey.INT_TR_MACD:
                    MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                    trObj.setTrsignal(macd.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                case ConstantKey.INT_TR_MACD1:
                    MACDObj macd1 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                    trObj.setTrsignal(macd1.trsignal);
                    UpdateTRList.add(trObj);
                    break;
                case ConstantKey.INT_TR_MACD2:
                    MACDObj macd2 = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD2_24, ConstantKey.INT_MACD2_48, ConstantKey.INT_MACD2_18);
                    trObj.setTrsignal(macd2.trsignal);
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
                        nn1.updateAdminTradingsignalnn1(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, UpdateTRList, stock, tradingRuleList);
                    }
                    break;
                case ConstantKey.INT_TR_NN2:
                    boolean nn2Flag = true;
                    if (nn2Flag == true) {
                        ProcessNN2 nn2 = new ProcessNN2();
                        nn2.updateAdminTradingsignalnn2(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, UpdateTRList, stock, tradingRuleList);
                    }
                    break;
                case ConstantKey.INT_TR_NN3:
                    boolean nn3Flag = true;
                    if (nn3Flag == true) {
                        ProcessNN3 nn3 = new ProcessNN3();
                        nn3.updateAdminTradingsignalnn3(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, UpdateTRList, stock, tradingRuleList);
                    }
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

    private ArrayList UpdateStockSignalNameArray(AccountObj accountObj) {
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

    public int calculateTrend(AFstockObj stock, long dateNowL) {
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
        return 1;
    }
///////////////

    public void ResetStockUpdateNameArray(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;
        stockUpdateNameArray.clear();
        updateStockUpdateNameArray();
    }

    private ArrayList updateStockUpdateNameArray() {
        if (stockUpdateNameArray != null && stockUpdateNameArray.size() > 0) {
            return stockUpdateNameArray;
        }
        ArrayList stockNameArray = serviceAFWeb.getAllOpenStockNameArray();
        if (stockNameArray != null) {
            stockUpdateNameArray = stockNameArray;
        }

        return stockUpdateNameArray;
    }

    public int UpdateAllStock(ServiceAFweb serviceAFWeb) {
        this.serviceAFWeb = serviceAFWeb;

        //SimpleDateFormat etDf = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mma 'ET'");
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        int hr = 0;
        try {
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.util.Date d = new java.util.Date(lockDateValue);
            DateFormat format = new SimpleDateFormat("M/dd/yyyy HH z");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);  //4/03/2020 04:47 PM EDT
            String[] arrOfStr = ESTdate.split(" ");
            hr = Integer.parseInt(arrOfStr[1]);

//            SimpleDateFormat etDf = new SimpleDateFormat("HH 'ET'");
//            String str = etDf.format(lockDateValue);
//            String[] arrOfStr = str.split(" ");
//            hr = Integer.parseInt(arrOfStr[0]);
        } catch (Exception ex) {
        }

        if ((hr > 18) && (hr < 8)) {  //if (hr > 17) {
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
        updateStockUpdateNameArray();
        int result = 0;
        try {
            if ((stockUpdateNameArray == null) || (stockUpdateNameArray.size() == 0)) {
                return 0;
            }
            logger.info("UpdateAllStock for 1 minutes market time=" + hr + " stocksize=" + stockUpdateNameArray.size());

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
                result = updateAllStockProcess(NormalizeSymbol);

            }
        } catch (Exception ex) {
        }
        return result;
    }

    public int updateStockProcess(ServiceAFweb serviceAFWebObj, String NormalizeSymbol) {
        this.serviceAFWeb = serviceAFWebObj;
        return updateAllStockProcess(NormalizeSymbol);
    }

    private int updateAllStockProcess(String NormalizeSymbol) {

//        logger.warning("> updateAllStock " + NormalizeSymbol);
        AFstockObj stock = null;
        // eddy testing
        // yahoo crumb fail not working
        boolean flagEnd = false;
        if (flagEnd == true) {
            return 0;
        }
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
            if (CKey.NN_DEBUG == true) {
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
                    int resultUpdate = updateRealTimeStock(stock);

                    stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
                    if (stock == null) {
                        logger.info("> updateAllStock getRealTimeStockImp stock Null " + NormalizeSymbol);
                        return 0;
                    }
                    if (resultUpdate > 0) {

                        //work around to fix AAPL or APPL not update but valid
                        long lastupdate1 = stock.getUpdatedatel();
                        long lastUpdate5Day = TimeConvertion.addDays(lastupdate1, 5); // 5 days

                        if (lastUpdate5Day > currentdate) {
                            //////// Update Long and short term trend 
                            int resultCalcuate = calculateTrend(stock, 0);
                            if (resultCalcuate == 1) {
                                // send SQL update
                                String sockUpdateSQL = StockDB.SQLupdateStockSignal(stock);
                                ArrayList sqlList = new ArrayList();
                                sqlList.add(sockUpdateSQL);
                                serviceAFWeb.SystemUpdateSQLList(sqlList);

                            }
                            return serviceAFWeb.removeNameLock(NormalizeSymbol, ConstantKey.STOCK_LOCKTYPE);
                        }

                    }
                    logger.info("> updateAllStock resultUpdate fail " + NormalizeSymbol);
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
                    logger.info("> updateAllStock lockReturn fail " + lockDateValue + " " + NormalizeSymbol);
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

    public int updateRealTimeStockTest(ServiceAFweb serviceAFWeb, AFstockObj stock) {
        this.serviceAFWeb = serviceAFWeb;
        return updateRealTimeStock(stock);
    }

    public int updateRealTimeStock(AFstockObj stock) {
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
                stock.setStockInfo(stockRTinternet.getStockInfo());
                stock.setDirection(timerCnt);
                if (!stock.getStockname().equals(stockRTinternet.getStockname())) {
                    stock.setStockname(stockRTinternet.getStockname());
                    String sockNameSQL = StockDB.SQLupdateStockName(stock);
                    ArrayList sqlList = new ArrayList();
                    sqlList.add(sockNameSQL);
                    serviceAFWeb.SystemUpdateSQLList(sqlList);
                }
                int internetHistoryLen = 0;
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
                // eddy testing
                // yahoo crumb fail not working
                boolean flagEnd = false;
                if (flagEnd == true) {
                    return 1;
                }
                // temporary fix the yahoo finance cannot get history
                // temporary fix the yahoo finance cannot get history
                // temporary fix the yahoo finance cannot get history
//                if (internetHistoryLen == 0) {
//                    return 0;
//                }

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
                    AFstockInfo stockInfoObj = StockArray.get(0);
                    boolean splitFlag = false;

                    int size1yearAll = 20;
                    float splitF = 0;
                    String msg = "";
                    CommData commDataObj = new CommData();
                    ArrayList<AFstockInfo> StockArrayTemp = serviceAFWeb.getStockHistorical(NormalizeSymbol, size1yearAll);
                    if (StockArrayTemp != null && StockArrayTemp.size() > 0) {
                        AFstockInfo stockInfo = StockArrayTemp.get(0);
                        long historydate = stockInfo.getEntrydatel();
                        historydate = TimeConvertion.endOfDayInMillis(historydate);
                        float newClose = stockInfo.getFclose();
                        for (int j = 0; j < StockArray.size(); j++) {
                            AFstockInfo stockInfoObjTmp = StockArray.get(j);
                            long currentdate = TimeConvertion.endOfDayInMillis(stockInfoObjTmp.getEntrydatel());
                            if (historydate == currentdate) {

                                float oldClose = stockInfoObjTmp.getFclose();
                                float deltaPTmp = 0;
                                if (newClose > oldClose) {
                                    deltaPTmp = newClose / oldClose;
                                    splitF = deltaPTmp;
                                } else {
                                    deltaPTmp = oldClose / newClose;
                                    splitF = -deltaPTmp;
                                }
                                if (deltaPTmp > CKey.SPLIT_VAL) {
//                                
                                    splitFlag = true;
                                    msg = "updateRealTimeStock " + NormalizeSymbol + " Split=" + splitF + " "
                                            + stockInfoObj.getEntrydatedisplay() + " newClose " + newClose + " oldClose " + oldClose;

                                    commDataObj.setType(0);
                                    commDataObj.setSymbol(NormalizeSymbol);
                                    commDataObj.setEntrydatedisplay(stockInfoObj.getEntrydatedisplay());
                                    commDataObj.setEntrydatel(stockInfoObj.getEntrydatel());
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
                            return 0;
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
                        serviceAFWeb.getAccountProcessImp().AddCommObjMessage(serviceAFWeb, accountAdminObj, commDataObj);
                        return 0;
                    }

                }

                if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                    stock.setSubstatus(ConstantKey.OPEN);
//                    String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
//                    ArrayList sqlList = new ArrayList();
//                    sqlList.add(sockNameSQL);
//                    serviceAFWeb.SystemUpdateSQLList(sqlList);
//                    logger.info("updateRealTimeStock " + NormalizeSymbol + " Stock Split cleared");
//                    return 0;
                }

                boolean primarySt = false;
//                for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
//                    String stockN = ServiceAFweb.primaryStock[i];
//                    if (stockN.equals(NormalizeSymbol.toUpperCase())) {
//                        primarySt = true;
//                        break;
//                    }
//                }
                if (primarySt == false) {
                    // assume yahoo finance is working.
                    // save only the last 10 to save memory 10M only in Clever Cloud 
                    ArrayList<AFstockInfo> StockArrayTmp = new ArrayList();
                    for (int j = 0; j < 25; j++) {
                        StockArrayTmp.add(StockArray.get(j));
                    }
                    StockArray = StockArrayTmp;

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

    public AFneuralNet testNeuralNet0Symbol(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        String nnName = ConstantKey.TR_NN1;
        if (TR_Name == ConstantKey.INT_TR_NN2) {
            nnName = ConstantKey.TR_NN2;
        }
        String BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
        AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
        if (nnObj0 == null) {
            return null;
        }
        return nnObj0;
    }

    public boolean testNeuralNet0Release(ServiceAFweb serviceAFWeb) {
        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release
        String name = CKey.NN_version + "_" + ConstantKey.TR_NN1;
        AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(name, 0);
        if (nnObj0 == null) {
            return false;
        }
        return true;
    }

    public int TRtrainingNN1NeuralNetData(ServiceAFweb serviceAFWeb, String nnNameSym, double nnError) {
        String BPnameSym = CKey.NN_version + "_" + nnNameSym;

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release        
        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        if (nnObj1 == null) {
            return 0;
        }

        if (nnObj1.getStatus() != ConstantKey.OPEN) {
            boolean flag = true;
            if (flag == true) {
                if (CKey.NN_DEBUG == true) {
                    ;
                } else {
                    return 1;
                }
            }

        }
        int exitNN = 8;
        if (getEnv.checkLocalPC() == true) {
            exitNN = 12;
        }
        if (nnObj1.getType() > exitNN) {
            // exit if over 4 times training
            // force to end training
            nnError = 999;
        }
        logger.info("> TRtrainingNeuralNet " + BPnameSym + " Statue=" + nnObj1.getStatus() + " Type=" + nnObj1.getType());

        String BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN1;
        if (nnNameSym.equals(ConstantKey.TR_NN4)) {
            BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN4;
        }
        return TRtrainingNNNeuralNetProcess(serviceAFWeb, BPnameTR, nnNameSym, nnError);
    }

    public int TRtrainingNN2NeuralNetData(ServiceAFweb serviceAFWeb, String nnNameSym, double nnError) {
        String BPname = CKey.NN_version + "_" + nnNameSym;

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release        
        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);
        if (nnObj1 == null) {
            return 0;
        }

        if (nnObj1.getStatus() != ConstantKey.OPEN) {
            boolean flag = true;
            if (flag == true) {
                return 1;
            }
        }
        int exitNN = 8;
        if (getEnv.checkLocalPC() == true) {
            exitNN = 12;
        }
        if (nnObj1.getType() > exitNN) {
            // exit if over 4 times training
            // force to end training
            nnError = 999;
        }
        logger.info("> TRtrainingNeuralNet " + BPname + " Statue=" + nnObj1.getStatus() + " Type=" + nnObj1.getType());

        String BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN2;
        return TRtrainingNNNeuralNetProcess(serviceAFWeb, BPnameTR, nnNameSym, nnError);
    }

    public ArrayList<NNInputDataObj> getTrainingInputDataFromFileProcess(ServiceAFweb serviceAFWeb, String symbol) {
        ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
        symbol = symbol.replace(".", "_");
        String nn12 = "_nn1_";
        for (int i = 1; i < 20; i++) {
            String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + i + ".csv";
            logger.info("> initTrainingNeuralNet1 " + nnFileName);
            boolean ret = readTrainingNeuralNet1(serviceAFWeb, inputDatalist, ConstantKey.TR_NN1, nnFileName);
            if (i == 0) {
                continue;
            }
            if (ret == false) {
                break;
            }
        }
        nn12 = "_nn2_";
        for (int i = 1; i < 20; i++) {
            String nnFileName = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + i + ".csv";
            logger.info("> initTrainingNeuralNet1 " + nnFileName);
            boolean ret = readTrainingNeuralNet1(serviceAFWeb, inputDatalist, ConstantKey.TR_NN1, nnFileName);
            if (i == 0) {
                continue;
            }
            if (ret == false) {
                break;
            }
        }

        return inputDatalist;
    }

    public ArrayList<NNInputOutObj> getTrainingInputFromFile(ServiceAFweb serviceAFWeb) {
        ArrayList<NNInputOutObj> inputlist = new ArrayList();
        HashMap<String, ArrayList> stockInputMap = null;
        ArrayList<NNInputDataObj> inputDatalist = this.getTrainingInputDataFromFile(serviceAFWeb, stockInputMap);

        //convert inptdatalist to inputlist
        for (int i = 0; i < inputDatalist.size(); i++) {
            NNInputDataObj inputDObj = inputDatalist.get(i);
            NNInputOutObj inputObj = new NNInputOutObj();
            inputObj.setDateSt(inputDObj.getObj().getDateSt());
            inputObj.setClose(inputDObj.getObj().getClose());
            inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
            inputObj.setInput1(inputDObj.getObj().getInput1());
            inputObj.setInput2(inputDObj.getObj().getInput2());
            inputObj.setInput3(inputDObj.getObj().getInput3());
            inputObj.setInput4(inputDObj.getObj().getInput4());
            inputObj.setInput5(inputDObj.getObj().getInput5());
            inputObj.setInput6(inputDObj.getObj().getInput6());
            inputObj.setInput7(inputDObj.getObj().getInput7());
            inputObj.setInput8(inputDObj.getObj().getInput8());
            inputObj.setInput9(inputDObj.getObj().getInput9());
            inputObj.setInput10(inputDObj.getObj().getInput10());
            inputObj.setInput11(inputDObj.getObj().getInput11());
            inputObj.setInput12(inputDObj.getObj().getInput12());
            inputObj.setInput13(inputDObj.getObj().getInput13());
            inputObj.setOutput1(inputDObj.getObj().getOutput1());
            if (inputDObj.getObj().getOutput1() < 0) {
                // ignore negative -1
                continue;
            }
            inputlist.add(inputObj);
        }

        return inputlist;
    }

    public ArrayList<NNInputDataObj> getStaticJavaInputDataFromFile(ServiceAFweb serviceAFWeb, HashMap<String, ArrayList> stockInputMap) {
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        ArrayList<NNInputDataObj> inputlistRet = new ArrayList();

        String symbol = "";
//        public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};
        String symbolL[] = ServiceAFweb.primaryStock;

        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];
            inputlist = getTrainingInputDataFromFileProcess(serviceAFWeb, symbol);
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

    public ArrayList<NNInputDataObj> getTrainingInputDataFromFile(ServiceAFweb serviceAFWeb, HashMap<String, ArrayList> stockInputMap) {
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        ArrayList<NNInputDataObj> inputlistRet = new ArrayList();
        String symbol = "";
//        String symbolL[] = ServiceAFweb.neuralNetTrainStock;
        String symbolL[] = ServiceAFweb.primaryStock;
        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];
            inputlist = getTrainingInputDataFromFileProcess(serviceAFWeb, symbol);
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

//    private int TRtrainingNNNeuralNetProcessFile(ServiceAFweb serviceAFWeb, String BPnameTR, String nnNameSym, double nnError) {
//        String BPnameSym = CKey.NN_version + "_" + nnNameSym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList();
//        boolean forceNN1flag = true;
//        if (forceNN1flag == true) {
//            BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN1;
//        }
//
//        boolean forceNNReadFileflag = true;
//        if (forceNNReadFileflag == true) {
//            inputlist = getTrainingInputFromFile(serviceAFWeb);
//        }
//
//        if (inputlist.size() == 0) {
//            return 0;
//        }
//        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputlist);
//
//        String NNnameSt = nnTraining.getNameNN() + "_" + nnNameSym;
//        nnTraining.setNameNN(NNnameSt);
//        nnTraining.setSymbol(nnNameSym);
//
//        /// start training or continue training           
//        /// start training or continue training
//        return TrainingNNBP(serviceAFWeb, nnNameSym, nnTraining, nnError);
//    }
    private int TRtrainingNNNeuralNetProcess(ServiceAFweb serviceAFWeb, String BPnameTR, String nnNameSym, double nnError) {
        String BPnameSym = CKey.NN_version + "_" + nnNameSym;
        ArrayList<NNInputOutObj> inputlist = new ArrayList();
        boolean forceNN1flag = true;
        if (forceNN1flag == true) {
            BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN1;
            if (nnNameSym.equals(ConstantKey.TR_NN4)) {
                BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN4;
            }
        }

        if (ServiceAFweb.forceNNReadFileflag == true) {
            inputlist = getTrainingInputFromFile(serviceAFWeb);
        } else {
            ArrayList<NNInputDataObj> inputDatalist = TradingNNprocess.NeuralNetGetNN1InputfromStaticCode("");
            for (int i = 0; i < inputDatalist.size(); i++) {
                NNInputDataObj inputDObj = inputDatalist.get(i);
                NNInputOutObj inputObj = new NNInputOutObj();
                inputObj.setDateSt(inputDObj.getObj().getDateSt());
                inputObj.setClose(inputDObj.getObj().getClose());
                inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
                inputObj.setInput1(inputDObj.getObj().getInput1());
                inputObj.setInput2(inputDObj.getObj().getInput2());
                inputObj.setInput3(inputDObj.getObj().getInput3());
                inputObj.setInput4(inputDObj.getObj().getInput4());
                inputObj.setInput5(inputDObj.getObj().getInput5());
                inputObj.setInput6(inputDObj.getObj().getInput6());
                inputObj.setInput7(inputDObj.getObj().getInput7());
                inputObj.setInput8(inputDObj.getObj().getInput8());
                inputObj.setInput9(inputDObj.getObj().getInput9());
                inputObj.setInput10(inputDObj.getObj().getInput10());
                inputObj.setInput11(inputDObj.getObj().getInput11());
                inputObj.setInput12(inputDObj.getObj().getInput12());
                inputObj.setInput13(inputDObj.getObj().getInput13());
                inputObj.setOutput1(inputDObj.getObj().getOutput1());

                inputlist.add(inputObj);
            }
            ArrayList<AFneuralNetData> objDataList = new ArrayList();

//            ArrayList<AFneuralNetData> objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameTR);
//            if ((objDataList == null) || (objDataList.size() < 10)) {
//                return 0;
//            }
//            ArrayList<AFneuralNetData> objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameTR);
//            if ((objDataList == null) || (objDataList.size() < 10)) {
//                return 0;
//
//            } else {
////            logger.info("> TRtrainingNNNeuralNetProcess " + BPnameTR + " " + objDataList.size());
//
//                // get it from DB
//                for (int i = 0; i < objDataList.size(); i++) {
//                    String dataSt = objDataList.get(i).getData();
//                    NNInputOutObj input;
//                    try {
//                        input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
//                        inputlist.add(input);
//                    } catch (IOException ex) {
//                    }
//                }
//            }
            if (BPnameTR.equals(BPnameSym)) {
                ;
            } else {
                objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym);
                if (objDataList != null) {
                    logger.info("> TRtrainingNNNeuralNetProcess " + BPnameSym + " " + inputlist.size() + " " + objDataList.size());
                    for (int i = 0; i < objDataList.size(); i++) {
                        String dataSt = objDataList.get(i).getData();
                        NNInputOutObj input;
                        try {
                            input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                            inputlist.add(input);
                        } catch (IOException ex) {
                        }
                    }
                }
            }

        }

        if (inputlist.size() == 0) {
            return 0;
        }
        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputlist);

        String NNnameSt = nnTraining.getNameNN() + "_" + nnNameSym;
        nnTraining.setNameNN(NNnameSt);
        nnTraining.setSymbol(nnNameSym);

        /// start training or continue training           
        /// start training or continue training
        return TrainingNNBP(serviceAFWeb, nnNameSym, nnTraining, nnError);
    }

    public void initTrainingNeuralNetData(ServiceAFweb serviceAFWeb, String nnName) {
//        logger.info("> initTrainingNeuralNet1 ");
        if (getEnv.checkLocalPC() == true) {
            ArrayList<NNInputOutObj> inputlist = new ArrayList();
            String BPname = CKey.NN_version + "_" + nnName;
            ArrayList<AFneuralNetData> objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPname);

            if ((objDataList == null) || (objDataList.size() < 10)) {
                return;
            } else {
                // get it from DB
                logger.info("> initTrainingNeuralNet1 objDataList " + objDataList.size());
                for (int i = 0; i < objDataList.size(); i++) {
                    String dataSt = objDataList.get(i).getData();
                    NNInputOutObj input;
                    try {
                        input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                        inputlist.add(input);
                    } catch (IOException ex) {
                    }
                }
            }
            if (inputlist.size() == 0) {
                return;
            }
            NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputlist);

            String NNnameSt = nnTraining.getNameNN() + "_" + nnName;
            nnTraining.setNameNN(NNnameSt);
            nnTraining.setSymbol(nnName);
            /// start training or continue training           
            /// start training or continue training
            TrainingNNBP(serviceAFWeb, nnName, nnTraining, 0);
        }
    }
//
//    public void initTrainingNeuralNetFile(ServiceAFweb serviceAFWeb, int TR_Name) {
//        if (getEnv.checkLocalPC() == true) {
//            //get sample
//            String nnName = ConstantKey.TR_NN1;
//            if (TR_Name == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//            }
//
//            boolean flagClearInput = true;
//            if (flagClearInput == true) {
//                // delete TR nn1 transaction
//                String BPname = CKey.NN_version + "_" + nnName;
//                serviceAFWeb.getStockImp().deleteNeuralNetData(BPname);
//            }
//
//            ArrayList<NNInputDataObj> inputlist = new ArrayList();
//
//            for (int i = 1; i < 20; i++) {
//                String nnFileName = ServiceAFweb.FileLocalNNPath + "/" + nnName + i + ".csv";
//                logger.info("> initTrainingNeuralNet1 " + nnFileName);
//                boolean ret = readTrainingNeuralNet1(serviceAFWeb, inputlist, nnName, nnFileName);
//                if (i == 0) {
//                    continue;
//                }
//                if (ret == false) {
//                    break;
//                }
//            }
//        }
//    }

//    public void testXORneuralnet(ServiceAFweb serviceAFWeb) {
//        String TestName = "xor";
//        Calendar dateNow = TimeConvertion.getCurrentCalendar();
//        long lockDateValue = dateNow.getTimeInMillis();
//        int lockReturn = serviceAFWeb.setLockNameProcess(TestName, ConstantKey.NN_TR_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessNeuralNet");
//        if (lockReturn == 0) {
//            System.out.println("locked");
//            return;
//        }
//        testxor(TestName);
//        serviceAFWeb.removeNameLock(TestName, ConstantKey.NN_TR_LOCKTYPE);
//
//    }
    public boolean readTrainingNeuralNet1(ServiceAFweb serviceAFWeb, ArrayList<NNInputDataObj> inputlist, String nnName, String nnFileName) {

        ArrayList inputArray = new ArrayList();
        if (FileUtil.FileTest(nnFileName) == false) {
            return false;
        }
        int addTotal = 0;
        String BPname = CKey.NN_version + "_" + nnName;
        boolean ret = FileUtil.FileReadTextArray(nnFileName, inputArray);
        if (ret == true) {
            for (int i = 0; i < inputArray.size(); i++) {
                String st = (String) inputArray.get(i);
                String[] stList = st.split(",");
                if (stList.length != (CKey.NN_INPUT_SIZE + 2 + 1 + 3)) { //12) {
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

                objData.setObj(obj);
                inputlist.add(objData);
//              
                //////////do not update DB
                //////////do not update DB                
                boolean inputSaveFlag = false;
                if (inputSaveFlag == true) {
                    ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPname, stockId, objData.getUpdatedatel());
                    if ((objList == null) || (objList.size() == 0)) {
                        serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPname, stockId, objData);
                        addTotal++;
                        continue;
                    }

                    boolean flag = true;
                    if (flag == true) {
                        if (CKey.NN_DEBUG == true) {
//                        logger.info("> readTrainingNeuralNet1 duplicate " + BPname + " " + stockId + " " + objData.getObj().getDateSt());
                        }
                    }
                }
            }
            logger.info("> readTrainingNeuralNet1 done " + nnFileName + " " + BPname + " Size" + inputlist.size() + "  " + addTotal);

            return true;
        }
        return false;
    }

    public int OutputNNBP(ServiceAFweb serviceAFWeb, NNTrainObj nnTraining) {
        double[][] inputpattern = null;
        double[][] targetpattern = null;
        double[][] response = null;
        if (nnTraining == null) {
            return 0;
        }

        String name = nnTraining.getNameNN();
        if (name == null) {
            return 0;
        }

        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight0(name, 0);
        if (nnObj1 == null) {
            return 0;
        }
        if (nnObj1 != null) {
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
        }
        return 0;
    }

    public int TrainingNNBP(ServiceAFweb serviceAFWeb, String nnName, NNTrainObj nnTraining, double nnError) {
        int inputListSize = CKey.NN_INPUT_SIZE; //12;
        int outputSize = 1;
        int middleSize = CKey.NN1_MIDDLE_SIZE;

//        if (nnName.equals(ConstantKey.TR_NN2) == true) {
//            middleSize = CKey.NN2_MIDDLE_SIZE;
//        }
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
        String sym = nnTraining.getSymbol();
//        if (sym.equals(ConstantKey.TR_NN1)) {
//            middleSize = middleSize + 4;
//        }

        NNBPservice nn = null;
        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release

        AFneuralNet afNeuralNet = serviceAFWeb.getNeuralNetObjWeight1(name, 0);

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
        int repeatSize = 900000;
        double errorReturn = 1;
        if (nnError == 0) {
            ;
        } else {
            if (CKey.NN_DEBUG == true) {
                logger.info("> TrainingNNBP inputpattern " + inputpattern.length);
            }
            errorReturn = nn.learn(nNetName, inputpattern, targetpattern, response, repeatSize, nnError);
        }

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

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                StringBuffer msg1 = new StringBuffer(weightSt);
                FileUtil.FileWriteText(ServiceAFweb.FileLocalDebugPath + nnName + "_nnWeight1.txt", msg1);
            }
        }
        int retFlag = 0;
        if (errorReturn < nnError) {

            retFlag = 1;
            //////////// training completed and release the NN
            serviceAFWeb.releaseNeuralNetObj(name);

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
                    for (int j = 0; j < inputpattern.length; j++) {
                        input = inputpattern[j];
                        output = targetpattern[j];
                        rsp = response[j];
                        if (j == 0) {

                            String stTitle = "\"" + "output"
                                    + "\",\"" + "macd TSig"
                                    + "\",\"" + "LTerm"
                                    + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                                    + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                                    + "\",\"" + "predict" + "\"";
                            writeArray.add(stTitle);
                        }
                        String st = "\"" + output[0]
                                + "\",\"" + input[0] + "\",\"" + input[1] + "\",\"" + input[2]
                                + "\",\"" + input[3] + "\",\"" + input[4] + "\",\"" + input[5]
                                + "\",\"" + input[6] + "\",\"" + input[7]
                                + "\",\"" + input[8] + "\",\"" + input[9]
                                + "\",\"" + rsp[0] + "\"";

                        float delta = (float) (output[0] - rsp[0]);
                        delta = Math.abs(delta);
                        float deltaCmp = (float) CKey.PREDICT_THRESHOLD;
                         if (nnName.equals(ConstantKey.TR_NN4) == true) {
                             deltaCmp = (float) 0.09;
                         }
                        if (delta > deltaCmp) {
                            st += ",\"" + delta + "\"";
                        }
                        writeArray.add(st);
                    }
                    if (getEnv.checkLocalPC() == true) {
                        if (CKey.NN_DEBUG == true) {
                            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + nnName + "_nnPredit.csv", writeArray);

                            StringBuffer msg = new StringBuffer(weightSt0);
                            FileUtil.FileWriteText(ServiceAFweb.FileLocalDebugPath + nnName + "_nnWeight0.txt", msg);
                        }
                    }
                }
            }
        } // training completed and release the NN

        if (nnError == 0) {
            return retFlag;
        }

        if (CKey.NN_DEBUG == true) {
            if (getEnv.checkLocalPC() == true) {
                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(name, 0);
                if (nnObj1 != null) {
                    String weightSt11 = nnObj1.getWeight();
                    if (weightSt1.length() > 0) {
                        NNBPservice nn1 = new NNBPservice();
                        nn1.createNet(weightSt11);
                        errorReturn = nn1.predictTest(inputpattern, targetpattern, response, nnError);

                        double[] input;
                        double[] output;
                        double[] rsp;
                        ArrayList writeArray = new ArrayList();
                        for (int j = 0; j < inputpattern.length; j++) {
                            input = inputpattern[j];
                            output = targetpattern[j];
                            rsp = response[j];
                            if (j == 0) {

                                String stTitle = "\"" + "output"
                                        + "\",\"" + "macd TSig"
                                        + "\",\"" + "LTerm"
                                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                                        + "\",\"" + "predict" + "\"";
                                writeArray.add(stTitle);
                            }
                            String st = "\"" + output[0]
                                    + "\",\"" + input[0] + "\",\"" + input[1] + "\",\"" + input[2]
                                    + "\",\"" + input[3] + "\",\"" + input[4] + "\",\"" + input[5]
                                    + "\",\"" + input[6] + "\",\"" + input[7]
                                    + "\",\"" + input[8] + "\",\"" + input[9]
                                    + "\",\"" + rsp[0] + "\"";
                            float delta = (float) (output[0] - rsp[0]);
                            delta = Math.abs(delta);
                            if (delta > 0.5) {
                                st += ",\"" + delta + "\"";
                            }
                            writeArray.add(st);
                        }
                        FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + nnName + "_nnPredit.csv", writeArray);
                    }
                }
            }
        } // training completed and release the NN
        return retFlag;

    }

//    public void TrainingNN(ServiceAFweb serviceAFWeb, NNTrainObj nnTraining) {
//        double[][] inputpattern = null;
//        double[] targetpattern = null;
//        double[] response = null;
//        NeuralNetwork nn = null;
//
//        if (nnTraining == null) {
//            return;
//        }
//
//        String name = nnTraining.getNameNN();
//        if (name == null) {
//            return;
//        }
//
//        nn = new NeuralNetwork();
//        nn.create(7, 8, 1);
//
//        inputpattern = nnTraining.getInputpattern();
//        targetpattern = nnTraining.getTargetpattern();
//
//        nn.setInput(inputpattern);
//        nn.setTarget(targetpattern);
//
//        response = new double[targetpattern.length];
//
//        double nnError = 0.00001;
//        for (int i = 0; i < 100; i++) {
//            double errorReturn = nn.learn(inputpattern, targetpattern, response, 1000000, nnError);
//        }
//    }
//
//    public void testxor(String name) {
//
//        double[][] inputpattern = null;
//        double[] targetpattern = null;
//        double[] response = null;
//
//        NeuralNetwork nn = null;
//
//        AFneuralNet afNeuralNet = serviceAFWeb.getNeuralNetObjWeight1(name, 0);
//        if (afNeuralNet != null) {
//            String weightSt = afNeuralNet.getWeight();
//            if (weightSt.length() > 0) {
//                nn = new NeuralNetwork();
//                nn.createNeuralNetbyWeight(weightSt);
//
//                inputpattern = nn.getInput();
//                targetpattern = nn.getTarget();
//            } else {
//                Calendar dateNow = TimeConvertion.getCurrentCalendar();
//                long currentdate = dateNow.getTimeInMillis();
//                long lastupdate1 = afNeuralNet.getUpdatedatel();
//                long lastUpdateNextDay = TimeConvertion.addDays(lastupdate1, 2); // 1 days
//                if (lastUpdateNextDay > currentdate) {
//                    return;
//                }
//            }
//        } else {
//            //Get again
//
//            afNeuralNet = new AFneuralNet();
//            afNeuralNet.setName(name);
//            afNeuralNet.setStatus(ConstantKey.OPEN);
//            afNeuralNet.setType(0);
//            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
//            afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
//            afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
//            afNeuralNet.setWeight("");
//            serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
//        }
//
//        if (nn == null) {
//            nn = new NeuralNetwork();
//            nn.create(2, 2, 1);
//
//            inputpattern = new double[4][2];
//
//            inputpattern[0][0] = 0.1;
//            inputpattern[0][1] = 0.1;
//
//            inputpattern[1][0] = 0.1;
//            inputpattern[1][1] = 0.9;
//
//            inputpattern[2][0] = 0.9;
//            inputpattern[2][1] = 0.1;
//
//            inputpattern[3][0] = 0.9;
//            inputpattern[3][1] = 0.9;
//
//            targetpattern = new double[4];
//
//            targetpattern[0] = 0.1;
//            targetpattern[1] = 0.9;
//            targetpattern[2] = 0.9;
//            targetpattern[3] = 0.1;
//
//            nn.setInput(inputpattern);
//            nn.setTarget(targetpattern);
//        }
//        response = new double[targetpattern.length];
//
//        double nnError = 0.00001;
//        double errorReturn = nn.learn(inputpattern, targetpattern, response, 1000000, nnError);
//
//        String weightSt = nn.getNeuralNetObjSt();
//        afNeuralNet.setWeight(weightSt);
//        afNeuralNet.setType(afNeuralNet.getType() + 1);
//        serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
//
//        if (errorReturn < nnError) {
//            serviceAFWeb.releaseNeuralNetObj(name);
//        }
//        double[] input = new double[2];
//        input[0] = 0.1;
//        input[1] = 0.1;
//
//        nn.predict(input, response);
//        logger.info("> ProcessNeuralNet predict 0.1 0.1 output=" + response[0]);
//
//        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight0("xor", 0);
//        if (nnObj1 != null) {
//            String weightSt1 = nnObj1.getWeight();
//            if (weightSt1.length() > 0) {
//                NeuralNetwork nn1 = new NeuralNetwork();
//                nn1.createNeuralNetbyWeight(weightSt1);
//                input[0] = 0.1;
//                input[1] = 0.9;
//
//                nn1.predict(input, response);
//                logger.info("> ProcessNeuralNet predict 0.1 0.9 output=" + response[0]);
//
//            }
//        }
//
//    }
    //////////////////////////////////////// transaction order
    public int AddTransactionOrderWithComm(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, String trName, int tranSignal, Calendar tranDate, boolean fromSystem) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
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
            String msg = ESTdate + " " + stock.getSymbol() + " Sig:" + sig;
            serviceAFWeb.getAccountProcessImp().AddCommMessage(serviceAFWeb, accountObj, trObj, msg);
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
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL);
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
                        retTrans = TransactionOrderSHORT_SELL(trObj, stock, signal, dateOffset, transSQL);
                        break;
                    case ConstantKey.S_EXIT_SHORT:
                        retTrans = TransactionOrderEXIT_SHORT(trObj, stock, signal, dateOffset, transSQL);
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

    private int TransactionOrderEXIT_SHORT(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();

        float originalPrice = trObj.getShortamount() / trObj.getShortshare();
        float deltaPrice = curPrice - originalPrice; //final price - original price
        deltaPrice = -deltaPrice; // negative for exit short
        float netPrice = originalPrice + deltaPrice;

        float amount = trObj.getShortshare() * netPrice;

        trObj.setBalance(trObj.getBalance() + amount);

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

    private int TransactionOrderSHORT_SELL(TradingRuleObj trObj, AFstockObj stock, int siganl, Calendar dateOffset, ArrayList transSQL) {
        float curPrice = stock.getAfstockInfo().getFclose();
        float shareTmp = CKey.TRADING_AMOUNT / curPrice;  //$6000
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
