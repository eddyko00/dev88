/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.model.stock.AFstockObj;

import com.afweb.service.ServiceAFweb;

import java.util.ArrayList;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class NNCal {

    protected static Logger logger = Logger.getLogger("NNCal");

    public static NNObj NNpredict(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {

        NNObj nn = new NNObj();

        switch (TR_Name) {
            case ConstantKey.INT_TR_NN1:
                return ProcessNN1.NNpredictNN1(serviceAFWeb, TR_Name, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);
            case ConstantKey.INT_TR_NN2:
                return ProcessNN2.NNpredictNN2(serviceAFWeb, TR_Name, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);
            case ConstantKey.INT_TR_NN3:
                return ProcessNN3.NNpredictNN3(serviceAFWeb, TR_Name, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);

            case ConstantKey.INT_TR_NN30:
                return ProcessNN00.NNpredictNN30(serviceAFWeb, TR_Name, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);
//            case ConstantKey.INT_TR_NN40:
//                return ProcessNN00.NNpredictNN40(serviceAFWeb, TR_Name, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);

            default:
                break;
        }
        return nn;
    }

//    public static NNObj NNCaluclation(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
//        try {
//
//            NNObj nn = new NNObj();
//            return nn;
//
//        } catch (Exception ex) {
//            logger.info(">NNCaluclation  exception" + ex.getMessage());
//
//        }
//        return null;
//    }
//    public static NNObj SelectBestTR(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
//        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        try {
////        if (true) {
////            return null;
////        }
//            NNObj nn = new NNObj();
//            ArrayList<TransationOrderObj> currTranOrderList_MV = new ArrayList();
//            ArrayList<TradingRuleObj> currTranTRList_MV = new ArrayList();
//            PerformanceObj perfMV = new PerformanceObj();
//
//            ArrayList<TransationOrderObj> currTranOrderList_MACD = new ArrayList();
//            ArrayList<TradingRuleObj> currTranTRList_MACD = new ArrayList();
//            PerformanceObj perfMACD = new PerformanceObj();
//
//            ArrayList<TransationOrderObj> currTranOrderList_RSI = new ArrayList();
//            ArrayList<TradingRuleObj> currTranTRList_RSI = new ArrayList();
//            PerformanceObj perfRSI = new PerformanceObj();
//
//            PerformanceObj maxPerfObj = null;
//            TradingRuleObj maxtrObj = null;
//
//            for (int i = 0; i < tradingRuleList.size(); i++) {
//                TradingRuleObj trOriginal = tradingRuleList.get(i);
//                //must create new object because the caller will still useing this object
//                //must create new object because the caller will still useing this object
//                TradingRuleObj tr = duplicateTRObj(trOriginal);
//                //must create new object because the caller will still useing this object
//                //must create new object because the caller will still useing this object
//                //
//                //initial to do the simulation
//                tr.setTrsignal(0);
//                tr.setBalance(0);
//                tr.setInvestment(0);
//                tr.setLongshare(0);
//                tr.setLongamount(0);
//                tr.setShortshare(0);
//                tr.setShortamount(0);
//
//                if (tr.getType() == ConstantKey.INT_TR_MV) {
//                    TradingRuleObj trObj = ProcessTROffset(serviceAFWeb, currTranOrderList_MV, currTranTRList_MV, tr, accountObj, stock, StockRecArray, DataOffset);
//                    if (trObj == null) {
//                        continue;
//                    }
//                    ArrayList<PerformanceObj> perfList = TRprocessImp.ProcessTranPerfHistoryOffset(serviceAFWeb, currTranOrderList_MV, StockRecArray, DataOffset, 1, false);
//                    if ((perfList != null) && (perfList.size() > 0)) {
//                        if (maxtrObj == null) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//                        if (maxPerfObj.getGrossprofit() < perfList.get(0).getGrossprofit()) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//
//                        perfMV = perfList.get(0);
////                    logger.info("> NNSelectBestTR " + tr.getSymbol() + " " + tr.getTrname() + " R=" + perfList.get(0).getRating() + " G=" + perfList.get(0).getGrossprofit());
//                    }
//
//                } else if (tr.getType() == ConstantKey.INT_TR_MACD) {
//
//                    TradingRuleObj trObj = ProcessTROffset(serviceAFWeb, currTranOrderList_MACD, currTranTRList_MACD, tr, accountObj, stock, StockRecArray, DataOffset);
//                    if (trObj == null) {
//                        continue;
//                    }
//                    ArrayList<PerformanceObj> perfList = TRprocessImp.ProcessTranPerfHistoryOffset(serviceAFWeb, currTranOrderList_MACD, StockRecArray, DataOffset, 1, false);
//                    if ((perfList != null) && (perfList.size() > 0)) {
//                        if (maxtrObj == null) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//                        if (maxPerfObj.getGrossprofit() < perfList.get(0).getGrossprofit()) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//
//                        perfMACD = perfList.get(0);
////                    logger.info("> NNSelectBestTR " + tr.getSymbol() + " " + tr.getTrname() + " R=" + perfList.get(0).getRating() + " G=" + perfList.get(0).getGrossprofit());
//                    }
//
//                } else if (tr.getType() == ConstantKey.INT_TR_RSI) {
//
//                    TradingRuleObj trObj = ProcessTROffset(serviceAFWeb, currTranOrderList_RSI, currTranTRList_RSI, tr, accountObj, stock, StockRecArray, DataOffset);
//                    if (trObj == null) {
//                        continue;
//                    }
//                    ArrayList<PerformanceObj> perfList = TRprocessImp.ProcessTranPerfHistoryOffset(serviceAFWeb, currTranOrderList_RSI, StockRecArray, DataOffset, 1, false);
//                    if ((perfList != null) && (perfList.size() > 0)) {
//                        if (maxtrObj == null) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//                        if (maxPerfObj.getGrossprofit() < perfList.get(0).getGrossprofit()) {
//                            maxPerfObj = perfList.get(0);
//                            maxtrObj = trObj;
//                        }
//
//                        perfRSI = perfList.get(0);
////                    logger.info("> NNSelectBestTR " + tr.getSymbol() + " " + tr.getTrname() + " R=" + perfList.get(0).getRating() + " G=" + perfList.get(0).getGrossprofit());
//                    }
//                }
//
//            }
//            nn.setTrsignal(maxtrObj.getTrsignal());
//            nn.setTrNameLink(maxtrObj.getTrname());
//            nn.setRating(maxPerfObj.getRating());
//            nn.setGrossprofit(maxPerfObj.getGrossprofit());
////        logger.info("> NNSelectBestTR Best " + nn.getTrNameLink() + " s=" + nn.getTrsignal() + " r=" + nn.getRating() + " g=" + nn.getGrossprofit());
//
//            return nn;
//
//        } catch (Exception ex) {
//            logger.info(">NNSelectBestTR  exception" + ex.getMessage());
//
//        }
//        return null;
//    }
//    public static TradingRuleObj ProcessTROffset(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> currTranOrderList, ArrayList<TradingRuleObj> currTranTRList, TradingRuleObj trObj, AccountObj accountObj, AFstockObj stock, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
//        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        try {
//            ArrayList<StockTRHistoryObj> trHistoryList = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObj, StockRecArray, DataOffset, CKey.MONTH_SIZE);
//            if ((trHistoryList == null) || (trHistoryList.size() == 0)) {
//                return null;
//            }
//            int lastSignal = 0;
//            long stockOffsetL = StockRecArray.get(DataOffset).getEntrydatel();
//
//            long date2yrBack = TimeConvertion.addMonths(stockOffsetL, -24); //2 yr before
//
//            StockTRHistoryObj trHistory = trHistoryList.get(trHistoryList.size() - 1);
//            lastSignal = trHistory.getTrsignal();
//
//            TradingRuleObj trObjNew = trObj;
//
//            for (int k = 0; k < trHistoryList.size(); k++) {
//                trHistory = trHistoryList.get(trHistoryList.size() - 1 - k);
//                int signal = trHistory.getTrsignal();
//                if (lastSignal == signal) {
//                    continue;
//                }
//
//                lastSignal = signal;
//                //check time only when signal change
//                if (trHistory.getUpdateDatel() > date2yrBack) {
//
//                    //Override the stockinfo for the price
//                    AFstockInfo afstockInfo = trHistory.getAfstockInfo();
//                    stock.setAfstockInfo(afstockInfo);
//                    Calendar tranDateOffet = TimeConvertion.getCurrentCalendar(trHistory.getUpdateDatel());
//
//                    ArrayList transObjList = TRprocessImp.AddTransactionOrderProcess(currTranOrderList, trObjNew, accountObj, stock, trObjNew.getTrname(), signal, tranDateOffet, true);
//                    if ((transObjList != null) && (transObjList.size() > 0)) {
//
//                        for (int i = 0; i < transObjList.size(); i += 2) {
//                            TransationOrderObj trOrder = (TransationOrderObj) transObjList.get(i);
//                            currTranOrderList.add(0, trOrder); // add first
//                            trObjNew = (TradingRuleObj) transObjList.get(i + 1);
//                            currTranTRList.add(0, trObjNew);
//                        }
//                    }
//                }
//            }
//            return trObjNew;
//        } catch (Exception ex) {
//            logger.info(">ProcessTROffset  exception" + ex.getMessage());
//
//        }
//        return null;
//    }
    ///////
}
