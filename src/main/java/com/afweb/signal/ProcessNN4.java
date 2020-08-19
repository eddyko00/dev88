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
public class ProcessNN4 {

    protected static Logger logger = Logger.getLogger("ProcessNN4");

    public static NNObj NNpredictNN4(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock,
            ArrayList<TradingRuleObj> tradingRuleList, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        NNObj nn = new NNObj();

        String stockidsymbol = stock.getSymbol();
//        AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(DataOffset);

        String nnName = ConstantKey.TR_NN4;
        nnName = nnName + "_" + stockidsymbol;
        String BPname = CKey.NN_version + "_" + nnName;

        ArrayList<NNInputDataObj> inputList = null;
        ProcessNN4 nn4 = new ProcessNN4();
//        inputList = nn4.trainingNN4StdataMACD1(serviceAFWeb, stockidsymbol, StockRecArray, DataOffset, 1);
        inputList = nn4.trainingNN4StdataMACD1(serviceAFWeb, stockidsymbol, StockRecArray, DataOffset, CKey.MONTH_SIZE / 2);

        if (inputList.size() == 0) {
            logger.info(">NNpredict  error inpulist");
            return nn;
        }

        //trainingNN1dataMACD will return oldest to new date so need reverse
        //trainingNN1dataMACD will return oldest to new date so need reverse
        Collections.reverse(inputList);
        ArrayList<NNInputOutObj> inputTraininglist = new ArrayList();
        NNInputOutObj inputObj = inputList.get(0).getObj();

        ///////testing
//        inputObj = inputList.get(4).getObj();
//        AFstockInfo AFstockInfo0 = StockRecArray.get(15);  // 2020 7 20
//        AFstockInfo AFstockInfo5 = StockRecArray.get(15 - 5);
//        DataOffset = 15;
        ///////
        float closeOutput0 = inputObj.getClose();

        ArrayList<AFstockInfo> StockPredArray = new ArrayList();
        for (int i = DataOffset; i < StockRecArray.size(); i++) {
            AFstockInfo AFstockI = StockRecArray.get(i);
            StockPredArray.add(AFstockI);
        }

        ///////
        inputTraininglist.add(inputObj);

        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputTraininglist, ConstantKey.TR_NN4);

        nnTraining.setNameNN(BPname);
        nnTraining.setSymbol(stockidsymbol);
        int retNN = TRprocessImp.OutputNNBP(serviceAFWeb, nnTraining);
        if (retNN == 0) {
            return nn;
        }
        double output = 0.1;

        double[][] rsp = nnTraining.getResponse();
        if (rsp[0][0] > 0.5) {
            output = 0.9;
        } else if (rsp[0][1] > 0.5) {
            output = 0.5;
        } else if (rsp[0][2] > 0.5) {
            output = -0.5;
        } else if (rsp[0][3] > 0.5) {
            output = -0.9;
        }

//        double closef = (closeOutput - closeOutput0) / closeOutput0;
//        closef = closef * 100;
//        closef = closef * 10;   // factore of 10 to make it more valid for NN
//        closef = closef / 100;
        double closeP = output;
        closeP = closeP / 10;
        double closeOutput = closeP * closeOutput0 + closeOutput0;

        float step = (float) ((closeOutput - closeOutput0) / 5);
        ///predit 5 day
        AFstockInfo AFstock0 = StockPredArray.get(0);
        for (int j = 0; j < 5; j++) {
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
        MACDObj macdNN = TechnicalCal.MACD(StockPredArray, 0, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
        int macdSignal = macdNN.trsignal;
        nn.setPrediction((float) closeOutput);

        nn.setTrsignal(macdSignal);
        String nameST;
        try {
            inputObj.setOutput1(output);
            nameST = new ObjectMapper().writeValueAsString(inputObj);
            nn.setComment(nameST);
        } catch (JsonProcessingException ex) {
        }
        return nn;

    }
/////////////////////////////////////////
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   

    public ArrayList<NNInputDataObj> trainingNN4StdataMACD1(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
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

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
        ArrayList<NNInputDataObj> inputList1 = NNProcessImp.getAccountStockTRListHistoryStDataMACDNN4(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, ConstantKey.TR_MACD, true);

        ArrayList<NNInputDataObj> inputList2 = NNProcessImp.getAccountStockTRListHistoryStDataMACDNN4Add2(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, ConstantKey.TR_MACD, true);
        ArrayList<NNInputDataObj> inputList3 = NNProcessImp.getAccountStockTRListHistoryStDataMACDNN4Sub2(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, ConstantKey.TR_MACD, true);

        inputDatalist.addAll(inputList1);
        checkduplicate(inputDatalist, inputList2);
        checkduplicate(inputDatalist, inputList3);

        return inputDatalist;
    }

    private void checkduplicate(ArrayList<NNInputDataObj> inputDatalist, ArrayList<NNInputDataObj> Addinputlist) {
        if (inputDatalist == null) {
            return;
        }
        if (Addinputlist == null) {
            return;
        }
        ArrayList<NNInputDataObj> NewinputDatalist = new ArrayList();

        for (int i = 0; i < Addinputlist.size(); i++) {
            NNInputDataObj input = Addinputlist.get(i);
            boolean found = false;
            for (int j = 0; j < inputDatalist.size(); j++) {
                NNInputDataObj inputAdd = inputDatalist.get(j);
                if (input.getObj().getDateSt().equals(inputAdd.getObj().getDateSt())) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                NewinputDatalist.add(input);
            }
        }
        inputDatalist.addAll(NewinputDatalist);
    }

    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
//    public ArrayList<NNInputDataObj> trainingNN4StdataMACD2(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
//        TradingNNprocess NNProcessImp = new TradingNNprocess();
//        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
////        logger.info("> trainingNN ");
//
//        String username = CKey.ADMIN_USERNAME;
//        String accountid = "1";
//        String symbol = sym;
////        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();
//
//        NNTrainObj nnTrSym = new NNTrainObj();
//        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_NN2);
//
//        TradingRuleObj trObjMACD1 = new TradingRuleObj();
//        trObjMACD1.setTrname(ConstantKey.TR_MACD);
//        trObjMACD1.setType(ConstantKey.INT_TR_MACD);
//
//        trObjMACD1.setAccount(trObjMACD.getAccount());
//        trObjMACD1.setStockid(trObjMACD.getStockid());
//
//        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD1, StockArray, offset, monthSize);
//
//        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
//        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);
//
//        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
//        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);
//
//        ArrayList<NNInputDataObj> inputDatalist = NNProcessImp.getAccountStockTRListHistoryStDataMACDNN(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, ConstantKey.TR_MACD, true);
//
//        return inputDatalist;
//    }
    int ProcessTRHistoryOffsetNN4(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize,
            int prevSignal, int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<StockTRHistoryObj> writeArray) {

        int nnSignal = prevSignal;
        int macdSignal = nnSignal;
        float prediction = -1;
        MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
        macdSignal = macdNN.trsignal;
        if (CKey.NN_DEBUG == true) {
            logger.info("ProcessTRHistoryOffsetNN2 " + stdate + " macdTR=" + macdSignal);
        }
        if (CKey.NN_DEBUG == true) {
            boolean flag = false;
            if (flag == true) {
                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN4, accountObj, stock, tradingRuleList, StockArray, offset);
                trHistory.setParmSt1(nn.getComment());
            }
        }
        if (nnSignal == ConstantKey.S_NEUTRAL) {
            nnSignal = macdSignal;
        }
        if (macdSignal == nnSignal) {
            trObj.setTrsignal(nnSignal);

        } else {

            NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN4, accountObj, stock, tradingRuleList, StockArray, offset);
            if (nn != null) {
                nnSignal = nn.getTrsignal();
//                nn.setTrsignal(nnSignal);
//                float predictionV = nn.getPrediction();
//                if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
//                    nnSignal = macdSignal;
//                } else {
//                    //
//                    if (writeArray.size() > 0) {
//                        for (int j = 0; j < writeArray.size(); j++) {
//                            StockTRHistoryObj lastTH = writeArray.get(writeArray.size() - 1 - j);
//                            if (lastTH.getTrsignal() != nnSignal) {
//                                float thClose = lastTH.getClose();
//                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                                float StClose = stockinfo.getFclose();
//                                float delta = specialRule1(thClose, StClose);
//                                if (delta > 0) {
//                                    nnSignal = macdSignal;
//                                }
//                            }
//                        }
//                    }
//
//                }
                trHistory.setParmSt1(nn.getComment());
                if (CKey.NN_DEBUG == true) {
                    logger.info("ProcessTRHistoryOffsetNN2 " + stdate + " macdTR=" + macdSignal + " " + nn.getComment());
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

    public void updateAdminTradingsignalnn4(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
            TradingRuleObj trObj, ArrayList StockArray, int offset, ArrayList<TradingRuleObj> UpdateTRList, AFstockObj stock, ArrayList tradingRuleList) {
        try {
            if (trObj.getSubstatus() == ConstantKey.OPEN) {
                MACDObj macdNN = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
                int macdSignal = macdNN.trsignal;
                int nnSignal = trObj.getTrsignal();

                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN4, accountObj, stock, tradingRuleList, StockArray, offset);
                if (nn != null) {
                    nnSignal = nn.getTrsignal();
//                    nn.setTrsignal(nnSignal);
//                    float predictionV = nn.getPrediction();
//                    if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
//                        nnSignal = macdSignal;
//                    } else {
//                        // get the last transaction price
//                        AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
//                        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTranListByAccountID(CKey.ADMIN_USERNAME, null,
//                                accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);
//                        if (thList != null) {
//                            if (thList.size() > 0) {
//                                TransationOrderObj lastTH = thList.get(0);
//                                float thClose = lastTH.getAvgprice();
//                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                                float StClose = stockinfo.getFclose();
//                                float delta = specialRule1(thClose, StClose);
//                                if (delta > 0) {
//                                    logger.info("> updateAdminTradingsignalnn2 " + symbol + " Override NN signal dela price > 15% Delta=" + delta);
//                                    nnSignal = macdSignal;
//                                }
//
//                            }
//                        }
//                    }
                }
                trObj.setTrsignal(nnSignal);
                UpdateTRList.add(trObj);
            }
        } catch (Exception ex) {
            logger.info("> updateAdminTradingsignalnn2 Exception" + ex.getMessage());
        }
    }

//    public float specialRule1(float thClose, float StClose) {
//        float delPer = 100 * (StClose - thClose) / thClose;
//        delPer = Math.abs(delPer);
//        if (delPer > 15) {  // > 15% override the NN sigal and take the MACD signal
//            return delPer;
//        }
//        return 0;
//    }
}
