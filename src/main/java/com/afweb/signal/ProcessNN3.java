/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class ProcessNN3 {

    protected static Logger logger = Logger.getLogger("ProcessNN3");

    public static NNObj NNpredictNN3(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock,
            ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {

        if (DataOffset != 0) {
            return NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);
        }

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        ArrayList<TransationOrderObj> thObjListnn1 = serviceAFWeb.getAccountImp().getAccountStockTransList(accountAdminObj.getId(),
                stock.getId(), "TR_NN1", 0);
        ArrayList<TransationOrderObj> thObjListnn2 = serviceAFWeb.getAccountImp().getAccountStockTransList(accountAdminObj.getId(),
                stock.getId(), "TR_NN2", 0);
        NNObj nn = new NNObj();
        nn.setPrediction((float) 0.1);
        nn.setTrsignal(0);
        if ((thObjListnn1 != null) && (thObjListnn2 != null)) {
            /// calculate which one is better
            AFstockInfo afstockInfo = stock.getAfstockInfo();
            float close = afstockInfo.getFclose();
            float perfnn1 = calculateBest(thObjListnn1, close);
            float perfnn2 = calculateBest(thObjListnn2, close);
            String TR_N = "TR_NN2";
            if (perfnn1 > perfnn2) {
                TR_N = "TR_NN1";
            }
            TradingRuleObj tr = serviceAFWeb.getAccountImp().getAccountStockIDByTRname(accountObj.getId(), stock.getId(), TR_N);
            nn.setTrsignal(tr.getTrsignal());
            nn.setPrediction((float) 0.9);
        }
        return nn;
    }

    private static float calculateBest(ArrayList<TransationOrderObj> thObjLis, float close) {
        int j = thObjLis.size() - 1;
        TransationOrderObj prevTranObj = null;
        float total = 0;
        for (int i = 0; i < thObjLis.size(); i++) {
            TransationOrderObj tranObj = thObjLis.get(j - i);
            if ((tranObj.getTrsignal() != ConstantKey.S_BUY) && (tranObj.getTrsignal() != ConstantKey.S_SELL)) {
                if (prevTranObj != null) {
                    float diff = (tranObj.getAvgprice() - prevTranObj.getAvgprice()) * tranObj.getShare();
                    if (prevTranObj.getTrsignal() == ConstantKey.S_BUY) {
                        ;
                    }
                    if (prevTranObj.getTrsignal() == ConstantKey.S_SELL) {
                        diff = -diff;
                    }
                    total += diff;
                }
            } else {
                if (i == thObjLis.size() - 1) {
                    //calculate the result on the last one
                    float diff = (close - tranObj.getAvgprice()) * tranObj.getShare();
                    if (tranObj.getTrsignal() == ConstantKey.S_BUY) {
                        ;
                    }
                    if (tranObj.getTrsignal() == ConstantKey.S_SELL) {
                        diff = -diff;
                    }
                    total += diff;

                }
            }
            prevTranObj = tranObj;
        }
        return total;
    }

    int ProcessTRHistoryOffsetNN3(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize,
            int prevSignal, int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList) {
        int nnSignal = prevSignal;

        float prediction = -1;
        try {
            NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN3, accountObj, stock, tradingRuleList, StockArray, offset);
            if (nn != null) {
                float predictionV = nn.getPrediction();
                if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                    nnSignal = nn.getTrsignal();
                }
                trHistory.setParmSt1(nn.getComment());
                if (CKey.NN_DEBUG == true) {
                    logger.info("ProcessTRHistoryOffsetNN3 " + stdate + " nn=" + nnSignal + " " + nn.getComment());
                }
            }
        } catch (Exception ex) {
            logger.info("> ProcessTRHistoryOffsetNN3 Exception" + ex.getMessage());
        }
        trObj.setTrsignal(nnSignal);
        trHistory.setTrsignal(nnSignal);
        trHistory.setParm1((float) 0);
        trHistory.setParm2((float) 0);
        trHistory.setParm3(nnSignal);
        trHistory.setParm4(prediction);
        return nnSignal;

    }

    public void updateAdminTradingsignalnn3(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
            TradingRuleObj trObj, ArrayList StockArray, int offset, ArrayList<TradingRuleObj> UpdateTRList, AFstockObj stock, ArrayList tradingRuleList) {
        try {
            if (trObj.getSubstatus() == ConstantKey.OPEN) {
                int nnSignal = trObj.getTrsignal();
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN3, accountObj, stock, tradingRuleList, StockArray, offset);

                if (nn != null) {

                    float predictionV = nn.getPrediction();
                    if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                        nnSignal = nn.getTrsignal();
                    }
                    trObj.setTrsignal(nnSignal);
                    UpdateTRList.add(trObj);
                }
            }
        } catch (Exception ex) {
            logger.info("> updateAdminTradingsignalnn3 Exception" + ex.getMessage());
        }
    }

}
