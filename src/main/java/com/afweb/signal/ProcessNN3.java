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
        return NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockRecArray, DataOffset);
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
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockArray, offset);

                if (nn != null) {

                    String inputObjSt = nn.getComment();
                    NNInputOutObj inputObj = new ObjectMapper().readValue(inputObjSt, NNInputOutObj.class);
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
