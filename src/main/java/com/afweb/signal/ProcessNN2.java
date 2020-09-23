/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal;

import com.afweb.nnprocess.TradingNNprocess;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.service.ServiceAFweb;
import com.afweb.stock.StockDB;
import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class ProcessNN2 {

    protected static Logger logger = Logger.getLogger("ProcessNN2");

    public static NNObj NNpredictNN2(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock,
            ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        NNObj nn = new NNObj();

        String stockidsymbol = stock.getSymbol();
        AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(DataOffset);

        String nnName = ConstantKey.TR_NN2;
        nnName = nnName + "_" + stockidsymbol;
        String BPname = CKey.NN_version + "_" + nnName;

        ArrayList<NNInputDataObj> inputList = null;
        ProcessNN2 nn2 = new ProcessNN2();
        inputList = nn2.trainingNN2dataMACD(serviceAFWeb, stockidsymbol, StockRecArray, DataOffset, CKey.SHORT_MONTH_SIZE);

        if (inputList.size() == 0) {
            logger.info(">NNpredict  error inpulist");
            return nn;
        }

        //trainingNN1dataMACD will return oldest to new date so need reverse
        //trainingNN1dataMACD will return oldest to new date so need reverse
        Collections.reverse(inputList);
        ArrayList<NNInputOutObj> inputTraininglist = new ArrayList();
        NNInputOutObj inputObj = inputList.get(0).getObj();
        inputTraininglist.add(inputObj);

        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputTraininglist, ConstantKey.TR_NN2);

        nnTraining.setNameNN(BPname);
        nnTraining.setSymbol(stockidsymbol);
        int retNN = TRprocessImp.OutputNNBP(serviceAFWeb, nnTraining);
        if (retNN == 0) {
            return nn;
        }

        double[][] rsp = nnTraining.getResponse();
        nn.setOutput1((float) rsp[0][0]);
        nn.setOutput2((float) rsp[0][1]);

        double NNprediction = rsp[0][0];
        int temp = 0;
        NNprediction = NNprediction * 1000;
        temp = (int) NNprediction;
        NNprediction = temp;
        NNprediction = NNprediction / 1000;

        nn.setPrediction((float) NNprediction);
        nn.setTrsignal(inputObj.getTrsignal());
        String nameST;
        try {
            inputObj.setOutput1(NNprediction);
            nameST = new ObjectMapper().writeValueAsString(inputObj);
            nameST = StringTag.replaceAll("\"", "", nameST);
            nn.setComment(nameST);
        } catch (JsonProcessingException ex) {
        }
        return nn;

    }

    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN2dataMACD(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> trainingNN ");

        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_NN2);

        TradingRuleObj trObjMACD1 = new TradingRuleObj();
        trObjMACD1.setTrname(ConstantKey.TR_MACD);
        trObjMACD1.setType(ConstantKey.INT_TR_MACD);

        trObjMACD1.setAccount(trObjMACD.getAccount());
        trObjMACD1.setStockid(trObjMACD.getStockid());

        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD1, StockArray, offset, monthSize);

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (monthSize > 5) {
                    ArrayList<String> writeArray = new ArrayList();
                    ArrayList<String> displayArray = new ArrayList();
                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListMACD, writeArray, displayArray);
                    boolean flagHis = false;
                    if (flagHis == true) {
                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + ConstantKey.TR_NN2 + "_tran.csv", writeArray);
                    }
                    serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListMACD, symbol, ConstantKey.TR_NN2, null);
                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = NNProcessImp.getAccountStockTRListHistoryMACDNN(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, ConstantKey.TR_MACD, true);

        return inputDatalist;
    }

    int ProcessTRHistoryOffsetNN2(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize,
            int prevSignal, int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<StockTRHistoryObj> writeArray) {

        int nnSignal = prevSignal;
        int macdSignal = nnSignal;
        float prediction = -1;
//      MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
        MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
        macdSignal = macdNN.trsignal;
//        if (CKey.NN_DEBUG == true) {
//            logger.info("ProcessTRHistoryOffsetNN2 " + stdate + " macdTR=" + macdSignal);
//        }
//        if (CKey.NN_DEBUG == true) {
//            boolean flag = false;
//            if (flag == true) {
//                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockArray, offset);
//                trHistory.setParmSt1(nn.getComment());
//            }
//        }
        if (nnSignal == ConstantKey.S_NEUTRAL) {
            nnSignal = macdSignal;
        }
        if (macdSignal == nnSignal) {
            trObj.setTrsignal(nnSignal);

        } else {

            NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockArray, offset);
            if (nn != null) {
                nn.setTrsignal(nnSignal);
                float predictionV = nn.getPrediction();
                if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                    nnSignal = macdSignal;
                } else {
                    //
                    if (writeArray.size() > 0) {
                        for (int j = 0; j < writeArray.size(); j++) {
                            StockTRHistoryObj lastTH = writeArray.get(writeArray.size() - 1 - j);
                            if (lastTH.getTrsignal() != nnSignal) {
                                float thClose = lastTH.getClose();
                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                                float StClose = stockinfo.getFclose();
                                float delta = specialOverrideRule1(thClose, StClose);
                                long lastTHLong = lastTH.getUpdateDatel();
                                long curSGLong = stockinfo.getEntrydatel();
                                if (delta > 0) {
                                    logger.info("> ProcessTRHistoryOffsetNN2 " + stock.getSymbol() + " Override1 signal " + lastTHLong + " " + curSGLong + " dela price > 15% Delta=" + delta);
                                    nnSignal = macdSignal;
                                } else {

                                    delta = specialOverrideRule2(nn, lastTHLong, curSGLong);
                                    if (delta > 0) {
                                        logger.info("> ProcessTRHistoryOffsetNN2 " + stock.getSymbol() + " Override2 signal  " + lastTHLong + " " + curSGLong + " date from last signal > 15 date");
                                        nnSignal = macdSignal;
                                    }
                                }
                                break; // for loop
                            }
                        }
                    }

                }
                trHistory.setParmSt1(nn.getComment());

                if (CKey.NN_DEBUG == true) {
                    boolean flag = false;
                    if (flag == true) {
                        logger.info("ProcessTRHistoryOffsetNN2 " + stdate + " macdTR=" + macdSignal + " " + nn.getComment());
                    }
                }
            }

        }
        trObj.setTrsignal(nnSignal);
        trHistory.setTrsignal(nnSignal);
        trHistory.setParm1((float) macdNN.macd); // getNNnormalizeInput must be set to macd vaule for NN input
        trHistory.setParm2((float) macdNN.signal);

        trHistory.setParm3(macdSignal);
        trHistory.setParm4(prediction);
        prevSignal = nnSignal;

        return nnSignal;

    }

    public void updateAdminTradingsignalnn2(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
            TradingRuleObj trObj, ArrayList StockArray, int offset, ArrayList<TradingRuleObj> UpdateTRList, AFstockObj stock, ArrayList tradingRuleList) {
        try {
            if (trObj.getSubstatus() == ConstantKey.OPEN) {
//                            MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                int macdSignal = macdNN.trsignal;
                int nnSignal = trObj.getTrsignal();
                if (nnSignal == ConstantKey.S_NEUTRAL) {
                    nnSignal = macdSignal;
                }
                if (macdSignal == nnSignal) {
                    // test if suddent drop
                    boolean testRule2 = true;
                    if (testRule2 == true) {
                        AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
                        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTranListByAccountID(CKey.ADMIN_USERNAME, null,
                                accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);
                        if (thList != null) {
                            if (thList.size() > 0) {
                                TransationOrderObj lastTH = thList.get(0);
                                float thClose = lastTH.getAvgprice();
                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                                float StClose = stockinfo.getFclose();
                                float delta = specialOverrideRule1(thClose, StClose);
                                if (delta > 0) {
                                    logger.info("> updateAdminTradingsignalnn2 " + symbol + " Delta Transactoin change > 15% Delta=" + delta);
//                                    nnSignal = macdSignal;
                                    int subStatus = stock.getSubstatus();
                                    if (subStatus == 0) {
                                        stock.setSubstatus(ConstantKey.STOCK_DELTA);
                                        String sockNameSQL = StockDB.SQLupdateStockStatus(stock);
                                        ArrayList sqlList = new ArrayList();
                                        sqlList.add(sockNameSQL);
                                        serviceAFWeb.SystemUpdateSQLList(sqlList);
                                    }

                                }

                            }
                        }
                    }
                    trObj.setTrsignal(macdSignal);
                    UpdateTRList.add(trObj);
                    return;
                }
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, tradingRuleList, StockArray, offset);
                if (nn != null) {
                    nn.setTrsignal(nnSignal);
                    float predictionV = nn.getPrediction();
                    if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                        nnSignal = macdSignal;
                    } else {
                        // get the last transaction price
                        AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
                        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTranListByAccountID(CKey.ADMIN_USERNAME, null,
                                accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);
                        if (thList != null) {
                            if (thList.size() > 0) {
                                TransationOrderObj lastTH = thList.get(0);
                                float thClose = lastTH.getAvgprice();
                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                                float StClose = stockinfo.getFclose();
                                float delta = specialOverrideRule1(thClose, StClose);
                                long lastTHLong = lastTH.getEntrydatel();
                                long curSGLong = stockinfo.getEntrydatel();
                                if (delta > 0) {
                                    logger.info("> updateAdminTradingsignalnn2 " + symbol + " Override1 signal " + lastTHLong + " " + curSGLong + " dela price > 15% Delta=" + delta);
                                    nnSignal = macdSignal;
                                } else {

                                    delta = specialOverrideRule2(nn, lastTHLong, curSGLong);
                                    if (delta > 0) {
                                        logger.info("> updateAdminTradingsignalnn2 " + symbol + " Override2 signal " + lastTHLong + " " + curSGLong + " date from last signal > 15 date");
                                        nnSignal = macdSignal;
                                    }
                                }

                            }
                        }
                    }
                }
                trObj.setTrsignal(nnSignal);
                UpdateTRList.add(trObj);
            }
        } catch (Exception ex) {
            logger.info("> updateAdminTradingsignalnn2 Exception" + ex.getMessage());
        }
    }

    public float specialOverrideRule1(float thClose, float StClose) {
        float delPer = 100 * (StClose - thClose) / thClose;
        delPer = Math.abs(delPer);
        if (delPer > 15) {  // > 15% override the NN sigal and take the MACD signal
            return delPer;
        }
        return 0;
    }

    public float specialOverrideRule2(NNObj nn, long lastTHLong, long curSGLong) {
        float output1 = nn.getOutput1();
        float output2 = nn.getOutput2();
        float threshold = (float) 0.4;
        //both 0.1 and 0.1
        if ((threshold > output1) && (threshold > output2)) {
            long next15date = TimeConvertion.addDays(lastTHLong, 15);  // check if last tran > 15 days
            if (next15date < curSGLong) {
                return 1;
            }
        }
        return 0;
    }
}
