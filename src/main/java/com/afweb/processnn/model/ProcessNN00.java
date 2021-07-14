/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn.model;

import com.afweb.model.nn.*;

import com.afweb.processnn.*;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;

import com.afweb.processsignal.TradingSignalProcess;

import com.afweb.service.ServiceAFweb;
import com.afweb.signal.NNObj;

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
public class ProcessNN00 {

    protected static Logger logger = Logger.getLogger("ProcessNN3");

    public static NNObj NNpredictNN30(ServiceAFweb serviceAFWeb,  AccountObj accountObj, AFstockObj stock,
            ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        NNObj nn = new NNObj();

        String symbol = stock.getSymbol();
        AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(DataOffset);

        String nnName = ConstantKey.TR_NN30;

        nnName = nnName + "_" + symbol;
        String BPname = CKey.NN_version + "_" + nnName;

        ArrayList<NNInputDataObj> inputList = null;

        //StockArray assume recent date to old data  
        //StockArray assume recent date to old data              
        //trainingNN1dataMACD will return oldest first to new date
        //trainingNN1dataMACD will return oldest first to new date
        ProcessNN00 nn00 = new ProcessNN00();
        inputList = nn00.trainingNN00_dataMACD1(serviceAFWeb, symbol, StockRecArray, DataOffset, CKey.SHORT_MONTH_SIZE);
//        inputList = nn00.trainingNN00_dataMACD0(serviceAFWeb, symbol, StockRecArray, DataOffset, CKey.SHORT_MONTH_SIZE);

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
        nnTraining.setTrname(ConstantKey.TR_NN30);
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
                FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + symbol + "_n3_Predect.csv", writeArray);
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


/////////////////////////////////////////////////////////////////
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN00_dataMACD0(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        return this.ProcessTrainingNN00_dataMACD(serviceAFWeb, sym, StockArray, offset, monthSize, ConstantKey.INT_TR_MACD0);
//        MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD0_3, ConstantKey.INT_MACD0_6, ConstantKey.INT_MACD0_2);
//        Fast        
    }

    public ArrayList<NNInputDataObj> trainingNN00_dataMACD1(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        return this.ProcessTrainingNN00_dataMACD(serviceAFWeb, sym, StockArray, offset, monthSize, ConstantKey.INT_TR_MACD1);
//        MACDObj macd = TechnicalCal.MACD(StockArray, offset, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
//        Normal
    }

    private ArrayList<NNInputDataObj> ProcessTrainingNN00_dataMACD(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize, int type) {
        NN30ProcessByTrend NNProcessTrend = new NN30ProcessByTrend();
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> trainingNN ");

        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjMACD = serviceAFWeb.AccGetAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_NN1);

        TradingRuleObj trObjMACD1 = new TradingRuleObj();
        // TR_NN1
        trObjMACD1.setTrname(ConstantKey.TR_MACD);
        trObjMACD1.setType(type);

        trObjMACD1.setAccount(trObjMACD.getAccount());
        trObjMACD1.setStockid(trObjMACD.getStockid());

        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD1, StockArray, offset, monthSize);

//        if (getEnv.checkLocalPC() == true) {
//            if (CKey.NN_DEBUG == true) {
//                if (monthSize > 5) {
//                    ArrayList<String> writeArray = new ArrayList();
//                    ArrayList<String> displayArray = new ArrayList();
//                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListMACD, writeArray, displayArray);
//                    boolean flagHis = false;
//                    if (flagHis == true) {
//                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + ConstantKey.TR_NN3 + "_tran.csv", writeArray);
//                    }
//                    serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListMACD, symbol, ConstantKey.TR_NN3, null);
//                }
//            }
//        }
        TradingRuleObj trObjMV = serviceAFWeb.AccGetAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.AccGetAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = NNProcessTrend.getAccountStockTRListHistoryTrendNN30(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, true);

        return inputDatalist;
    }


}
