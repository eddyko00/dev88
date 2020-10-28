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
import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class ProcessNN1 {

    protected static Logger logger = Logger.getLogger("ProcessNN1");

    public static NNObj NNpredictNN1(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock,
            ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {

        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        NNObj nn = new NNObj();

        String symbol = stock.getSymbol();
        AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(DataOffset);

        String BPname = CKey.NN_version + "_" + ConstantKey.TR_NN1 + "_" + symbol;

        ArrayList<NNInputDataObj> inputList = null;

        //StockArray assume recent date to old data  
        //StockArray assume recent date to old data              
        //trainingNN1dataMACD will return oldest first to new date
        //trainingNN1dataMACD will return oldest first to new date
        ProcessNN1 nn1 = new ProcessNN1();
        inputList = nn1.trainingNN1dataMACD1(serviceAFWeb, symbol, StockRecArray, DataOffset, CKey.SHORT_MONTH_SIZE);

        if (inputList.size() == 0) {
            // it is okay for input relearning
//            logger.info(">NNpredict  error inpulist");
            return nn;
        }

        //trainingNN1dataMACD will return oldest to new date so need reverse
        //trainingNN1dataMACD will return oldest to new date so need reverse
        Collections.reverse(inputList);
        ArrayList<NNInputOutObj> inputTraininglist = new ArrayList();
        NNInputOutObj inputObj = inputList.get(0).getObj();
        inputTraininglist.add(inputObj);

        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputTraininglist, ConstantKey.TR_NN1);

        nnTraining.setNameNN(BPname);
        nnTraining.setSymbol(symbol);
        nnTraining.setTrname(ConstantKey.TR_NN1);        
        int retNN = TRprocessImp.OutputNNBP(serviceAFWeb, nnTraining);
        if (retNN == 0) {
            return nn;
        }

        if (getEnv.checkLocalPC() == true) {
            boolean flag = false;
            if (flag == true) {
                double[][] inputpattern = null;
                double[][] targetpattern = null;
                double[][] response = null;

                inputpattern = nnTraining.getInputpattern();
                targetpattern = nnTraining.getOutputpattern();
                response = nnTraining.getResponse();
                double[] input;
                double[] output;
                double[] rsp;
                ArrayList writeArray = new ArrayList();
                for (int j = 0; j < inputpattern.length; j++) {
                    input = inputpattern[j];
                    output = targetpattern[j];
                    rsp = response[j];

                    double NNprediction = rsp[0];
                    int temp = 0;
                    NNprediction = NNprediction * 1000;
                    temp = (int) NNprediction;
                    NNprediction = temp;
                    NNprediction = NNprediction / 1000;

                    String stDate = new java.sql.Date(stocktmp.getEntrydatel()).toString();
                    String st = "\"" + stDate + "\",\"" + stocktmp.getFclose() + "\"";
                    st += "\",\"" + NNprediction
                            + "\",\"" + output[0]
                            + "\",\"" + input[0] + "\",\"" + input[1] + "\",\"" + input[2]
                            + "\",\"" + input[3] + "\",\"" + input[4]
                            + "\",\"" + input[5] + "\",\"" + input[6] + "\",\"" + input[7]
                            + "\",\"" + input[8] + "\",\"" + input[9]
                            + "\"";

                    writeArray.add(st);
                    logger.info(">NNpredict " + st);
                }
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + symbol + "_Predect.csv", writeArray);
            }
        }
        double[][] rsp = nnTraining.getResponse();
        nn.setOutput1((float) rsp[0][0]);
        nn.setOutput2((float) rsp[0][1]);
        inputObj.setOutput1((float) rsp[0][0]);
        inputObj.setOutput2((float) rsp[0][1]);

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
            nn.setComment(nameST);
        } catch (JsonProcessingException ex) {
        }

        return nn;
    }

    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN1dataMACD1(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> trainingNN ");

        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_NN1);

        TradingRuleObj trObjMACD1 = new TradingRuleObj();
        trObjMACD1.setTrname(ConstantKey.TR_MACD1);
        trObjMACD1.setType(ConstantKey.INT_TR_MACD1);

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
                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + ConstantKey.TR_NN1 + "_tran.csv", writeArray);
                    }
                    serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListMACD, symbol, ConstantKey.TR_NN1, null);
                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = NNProcessImp.getAccountStockTRListHistoryMACDNN1(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, true);

        return inputDatalist;
    }

    int ProcessTRHistoryOffsetNN1(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize,
            int prevSignal, int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<StockTRHistoryObj> writeArray) {

        int nnSignal = prevSignal;
        int macdSignal = nnSignal;
        float prediction = -1;
        MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
        macdSignal = macdNN.trsignal;

        AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
        Date stockDate = new Date(stockinfoT.getEntrydatel());
        // just for testing
//        logger.info("> ProcessTRHistoryOffsetNN1 " + stock.getSymbol() + " " + stockDate.toString());

        if (nnSignal == ConstantKey.S_NEUTRAL) {
            nnSignal = macdSignal;
        }
        if (macdSignal == nnSignal) {
            trObj.setTrsignal(nnSignal);

        } else {
            NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN1, accountObj, stock, tradingRuleList, StockArray, offset);
            if (nn != null) {
                float output1 = nn.getOutput1();
                float output2 = nn.getOutput2();
                if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
                    nn.setTrsignal(nnSignal);
                    float predictionV = nn.getPrediction();
                    if (predictionV > CKey.PREDICT_THRESHOLD) { //0.6) {
                        nnSignal = macdSignal;
                    }
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
//                                    logger.info("> ProcessTRH NN1 " + stock.getSymbol() + " Override 1 signal " + stockDate.toString() + " dela price > 20% Delta=" + delta);
                                    nnSignal = macdSignal;
                                } else {
                                    delta = specialOverrideRule2(nn, lastTHLong, curSGLong);
                                    if (delta > 0) {
//                                        logger.info("> ProcessTRH NN1 " + stock.getSymbol() + " Override 2 signal  " + stockDate.toString() + " date from last signal > 40 date");
                                        nnSignal = macdSignal;
                                    }
                                }
                                break; // for loop
                            }
                        }
                    }

                }
                trHistory.setParmSt1(nn.getComment());
//                if (CKey.NN_DEBUG == true) {
//                    logger.info("ProcessTRHistoryOffsetNN1 " + stdate + " macdTR=" + macdSignal + " " + nn.getComment());
//                }
            }
        }
        if (nnSignal != prevSignal) {
            // signal change double check wiht NN trend
            int trendSignal = this.specialOverrideRule3(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
            //override the previous NN1 prediction

            if (nnSignal != trendSignal) {
//                logger.info("> ProcessTRH NN1 " + stock.getSymbol() + " Override 3 signal " + stockDate.toString() + " TrendSignal " + trendSignal);
            }
            nnSignal = trendSignal;
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

    public NNObj updateAdminTradingsignalnn1(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
            TradingRuleObj trObj, ArrayList StockArray, int offset,  AFstockObj stock, ArrayList tradingRuleList) {
        NNObj nnRet = new NNObj();
        try {
            if (trObj.getSubstatus() == ConstantKey.OPEN) {
//                            MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD2_24, ConstantKey.INT_MACD2_48, ConstantKey.INT_MACD2_18);
                MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
//                            MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD_12, ConstantKey.INT_MACD_26, ConstantKey.INT_MACD_9);
                int macdSignal = macdNN.trsignal;
                AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
                Date stockDate = new Date(stockinfoT.getEntrydatel());
                int prevSignal = trObj.getTrsignal();
                int nnSignal = trObj.getTrsignal();
                if (nnSignal == ConstantKey.S_NEUTRAL) {
                    nnSignal = macdSignal;
                }
                if (macdSignal == nnSignal) {
                    nnRet.setTrsignal(macdSignal);
                    return nnRet;
                }
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN1, accountObj, stock, tradingRuleList, StockArray, offset);

                if (nn != null) {
                    float output1 = nn.getOutput1();
                    float output2 = nn.getOutput2();
                    if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
                        nn.setTrsignal(nnSignal);
                        float predictionV = nn.getPrediction();
                        if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                            nnSignal = macdSignal;
                        }
                    } else {
                        // get the last transaction price
                        AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
                        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTranListByAccountID(CKey.ADMIN_USERNAME, null,
                                accObj.getId() + "", symbol, ConstantKey.TR_NN1, 0);
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
//                                    logger.info("> updateAdminTR nn1 " + symbol + " Override 1 signal " + stockDate.toString() + " dela price > 20% Delta=" + delta);
                                    nnSignal = macdSignal;
                                } else {

                                    delta = specialOverrideRule2(nn, lastTHLong, curSGLong);
                                    if (delta > 0) {
//                                        logger.info("> updateAdminTR nn1 " + symbol + " Override 2 signal " + stockDate.toString() + " date from last signal > 40 date");
                                        nnSignal = macdSignal;
                                    }
                                }

                            }
                        }
                    }
                }
                if (nnSignal != prevSignal) {
                    // signal change double check wiht NN trend
                    int trendSignal = this.specialOverrideRule3(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
                    //override the previous NN1 prediction

                    if (nnSignal != trendSignal) {
                        if (CKey.NN_DEBUG == true) {
//                            logger.info("> updateAdminTR nn1 " + stock.getSymbol() + " Override 3 signal " + stockDate.toString() + " TrendSignal " + trendSignal);
                        }
                    }
                    nnSignal = trendSignal;
                }
                nnRet.setTrsignal(nnSignal);
                return nnRet;
            }
        } catch (Exception ex) {
            logger.info("> updateAdminTradingsignalnn1 Exception" + ex.getMessage());
        }
        return null;
    }

    public float specialOverrideRule1(float thClose, float StClose) {
        float delPer = 100 * (StClose - thClose) / thClose;
        delPer = Math.abs(delPer);
        if (delPer > 20) {  // > 15% override the NN sigal and take the MACD signal
            return delPer;
        }
        return 0;
    }

    public float specialOverrideRule2(NNObj nn, long lastTHLong, long curSGLong) {
        // ignore rule 2
        if (true) {
            return 0;
        }
        float output1 = nn.getOutput1();
        float output2 = nn.getOutput2();
        float threshold = (float) 0.4;
        //both 0.1 and 0.1
        if ((threshold > output1) && (threshold > output2)) {
            long next15date = TimeConvertion.addDays(lastTHLong, 40);
            if (next15date < curSGLong) {
                return 1;
            }
        }
        return 0;
    }

    public int specialOverrideRule3(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol, TradingRuleObj trObj, ArrayList StockArray, int offset, AFstockObj stock, ArrayList tradingRuleList, int nnSignal) {
        NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN100, accountObj, stock, tradingRuleList, StockArray, offset);
        if (nn != null) {

            float output1 = nn.getOutput1();
            float output2 = nn.getOutput2();
            if ((CKey.PREDICT_THRESHOLD > output1) && (CKey.PREDICT_THRESHOLD > output2)) {
                // loop to find the previous trend.

                for (int i = 0; i < 5; i++) {
                    //StockArray recent to old date
                    NNObj nn1 = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN100, accountObj, stock, tradingRuleList, StockArray, offset + 1 + i);
                    if (nn1 != null) {
                        output1 = nn1.getOutput1();
                        output2 = nn1.getOutput2();
                        if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
                            nn = nn1;
                            break;

                        }

                    }
                }

            }
            if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {

                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                float closeOutput0 = stockinfo.getFclose();
                float closeOutput = closeOutput0;
                // getNNnormalizeStOutputClose logic
//                          double closef = (closeOutput - closeOutput0) / closeOutput0;
//                          closef = closef * 15; // factore of 15 to make it more valid for NN
                // process NN3
                if (CKey.PREDICT_THRESHOLD < output1) {
                    float closef = (float) 0.9;
                    closef = closef / 15;
                    closeOutput = (closef * closeOutput0) + closeOutput0;
                } else if (CKey.PREDICT_THRESHOLD < output2) {
                    float closef = (float) -0.9;
                    closef = closef / 15;
                    closeOutput = (closef * closeOutput0) + closeOutput0;
                }

                // need to match getNNnormalizeStOutputClose futureDay
                int futureDay = 5;
                float step = (float) ((closeOutput - closeOutput0) / futureDay);
                ArrayList<AFstockInfo> StockPredArray = new ArrayList();
                for (int i = offset; i < StockArray.size(); i++) {
                    AFstockInfo AFstockI = (AFstockInfo) StockArray.get(i);
                    StockPredArray.add(AFstockI);
                }
                ///predit 5 day
                AFstockInfo AFstock0 = StockPredArray.get(0);
                for (int j = 0; j < futureDay; j++) {
                    AFstockInfo AFstockI = new AFstockInfo();
                    float stepP = (j + 1) * step;
                    AFstockI.setFopen(AFstock0.getFopen() + stepP);
                    AFstockI.setFclose(AFstock0.getFclose() + stepP);
                    AFstockI.setVolume(AFstock0.getVolume());
                    long dat = AFstock0.getEntrydatel();
                    long nDat = TimeConvertion.addDays(dat, j + 1);
                    AFstockI.setEntrydatel(nDat);
                    AFstockI.setEntrydatedisplay(new java.sql.Date(nDat));
                    StockPredArray.add(0, AFstockI);
                }
                // MACD1
                MACDObj macdNN = TechnicalCal.MACD(StockPredArray, 0, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                int macdSignal = macdNN.trsignal;

                return macdSignal;
            }
        }
        return nnSignal;
    }

}
