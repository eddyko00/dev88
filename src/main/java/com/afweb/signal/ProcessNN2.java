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
public class ProcessNN2 {

    protected static Logger logger = Logger.getLogger("ProcessNN2");

    public static NNObj NNpredictNN2(ServiceAFweb serviceAFWeb, AccountObj accountObj, AFstockObj stock,
            ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        NNObj nn = new NNObj();

        String symbol = stock.getSymbol();
        AFstockInfo stocktmp = (AFstockInfo) StockRecArray.get(DataOffset);

        String BPname = CKey.NN_version + "_" + ConstantKey.TR_NN2 + "_" + symbol;

        ArrayList<NNInputDataObj> inputList = null;

        //StockArray assume recent date to old data  
        //StockArray assume recent date to old data              
        //trainingNN1dataMACD will return oldest first to new date
        //trainingNN1dataMACD will return oldest first to new date
        ProcessNN2 NN2 = new ProcessNN2();
        inputList = NN2.trainingNN2dataEMA1(serviceAFWeb, symbol, StockRecArray, DataOffset, CKey.SHORT_MONTH_SIZE);
        // alway use normal

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

        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputTraininglist, ConstantKey.TR_NN2);

        nnTraining.setNameNN(BPname);
        nnTraining.setSymbol(symbol);
        nnTraining.setTrname(ConstantKey.TR_NN2);
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

//    A moving average can be any length: 15, 28, 89, etc. Adjusting the moving average 
//    so it provides more accurate signals on historical data may help create better future signals.
//    Another strategy is to apply two moving averages to a chart: one longer and one shorter. 
//    When the shorter-term MA crosses above the longer-term MA, it's a buy signal, as it indicates 
//    that the trend is shifting up. This is known as a "golden cross."
//    Meanwhile, when the shorter-term MA crosses below the longer-term MA, it's a sell signal, 
//    as it indicates that the trend is shifting down. This is known as a "dead/death cross."
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN2dataEMA1(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> trainingNN2 ");

        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;

        String TRname = ConstantKey.TR_NN2;
        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjNN2 = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, TRname);

        TradingRuleObj trObj2 = new TradingRuleObj();
        trObj2.setTrname(ConstantKey.TR_MV);
        trObj2.setType(ConstantKey.INT_TR_EMA1);
//        trObj2.setType(ConstantKey.INT_TR_EMA00);
        // normal

        trObj2.setAccount(trObjNN2.getAccount());
        trObj2.setStockid(trObjNN2.getStockid());

        ArrayList<StockTRHistoryObj> thObjListEMA = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObj2, StockArray, offset, monthSize);

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (monthSize > 5) {
                    ArrayList<String> writeArray = new ArrayList();
                    ArrayList<String> displayArray = new ArrayList();
                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListEMA, writeArray, displayArray);
                    boolean flagHis = false;
                    if (flagHis == true) {
                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + TRname + "_2" + "_tran.csv", writeArray);
                        serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListEMA, symbol, TRname + "_2", null);
                    }
                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MACD);
        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = getAccountStockTRListHistoryEMANN2(thObjListEMA, thObjListMV, thObjListMACD, symbol, nnTrSym, true);

        return inputDatalist;
    }

    public ArrayList<NNInputDataObj> trainingNN2dataEMA2(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> trainingNN2 ");

        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();
        String TRname = ConstantKey.TR_NN2;

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjNN2 = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, TRname);

        TradingRuleObj trObj2 = new TradingRuleObj();
        trObj2.setTrname(ConstantKey.TR_MV);
        trObj2.setType(ConstantKey.INT_TR_EMA2);
//        trObj2.setType(ConstantKey.INT_TR_EMA0);
        // slow

        trObj2.setAccount(trObjNN2.getAccount());
        trObj2.setStockid(trObjNN2.getStockid());

        ArrayList<StockTRHistoryObj> thObjListEMA = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObj2, StockArray, offset, monthSize);

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (monthSize > 5) {
                    ArrayList<String> writeArray = new ArrayList();
                    ArrayList<String> displayArray = new ArrayList();
                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListEMA, writeArray, displayArray);
                    boolean flagHis = false;
                    if (flagHis == true) {
                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + TRname + "_1" + "_tran.csv", writeArray);
                        serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListEMA, symbol, TRname + "_1", null);
                    }
                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MACD);
        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = getAccountStockTRListHistoryEMANN2(thObjListEMA, thObjListMV, thObjListMACD, symbol, nnTrSym, true);

        return inputDatalist;
    }

    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> RetrainingNN2dataReTrain(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjNN2 = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_NN2);

        TradingRuleObj trObj2 = new TradingRuleObj();
        trObj2.setTrname(ConstantKey.TR_NN2);
        trObj2.setType(ConstantKey.INT_TR_NN2);

        trObj2.setAccount(trObjNN2.getAccount());
        trObj2.setStockid(trObjNN2.getStockid());

        ArrayList<StockTRHistoryObj> thObjListEMA = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObj2, StockArray, offset, monthSize);

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockTRByTRname(username, null, accountid, symbol, ConstantKey.TR_MACD);
        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = getAccountStockTRListHistoryEMANN2(thObjListEMA, thObjListMV, thObjListMACD, symbol, nnTrSym, true);

        return inputDatalist;
    }

    int ProcessTRHistoryOffsetNN2(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize, int prevSignal,
            int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<StockTRHistoryObj> writeArray, AccData accData) {
        int confident = 0;
        boolean stopLoss = false;
        boolean stopReset = false;
        boolean profitTake = false;

        int nnSignal = prevSignal;
        int emaSignal = nnSignal;
        float prediction = -1;
///////////////////////////////   
        EMAObj ema510 = this.getTechnicalCal(StockArray, offset);
//        EMAObj ema510 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_5, ConstantKey.INT_EMA_10);

        emaSignal = ema510.trsignal;
///////////////////////////////////////////////////
        AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
        Date stockDate = new Date(stockinfoT.getEntrydatel());
        // just for testing
//        logger.info("> ProcessTRHistoryOffsetNN1 " + stock.getSymbol() + " " + stockDate.toString());

        if (nnSignal == ConstantKey.S_NEUTRAL) {
            nnSignal = emaSignal;
        }
        if (emaSignal == nnSignal) {
            trObj.setTrsignal(nnSignal);

        } else {
            confident += 30;

            accData.setNn(0);
            NNObj nn = new NNObj();
            boolean nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset, stock);
            trHistory.setParmSt1(nn.getComment());

            if (nnFlag == true) {
                nn.setTrsignal(nnSignal);
                nnSignal = emaSignal;
                confident += 30;
                if (nnSignal != prevSignal) {
                    accData.setNn(1);
                    nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset + 1, stock);
                    if (nnFlag == true) {
                        accData.setNn(accData.getNn() + 1);
                        nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset + 2, stock);
                        if (nnFlag == true) {
                            accData.setNn(accData.getNn() + 1);
                        }
                    }
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
//                                int rule4Signal = specialOverrideRule4(adxObj, prevSignal, thClose, StClose);
//                                if (rule4Signal != prevSignal) {
//                                    nnSignal = rule4Signal;
//                                    break;
//                                }
                            float delta = Rule1_StopLoss(prevSignal, thClose, StClose);

                            if (delta > 0) {
//                              logger.info("> ProcessTRH NN2 " + stock.getSymbol() + " Override 1 signal " + stockDate.toString() + " dela price > 20% Delta=" + delta);
                                stopLoss = true;
                                nnSignal = emaSignal;
                                confident += 15;
                            } else {
//                                    long lastTHLong = lastTH.getUpdateDatel();
//                                    long curSGLong = stockinfo.getEntrydatel();
//                                    delta = Rule2_LongTran(nn, lastTHLong, curSGLong);
//                                    if (delta > 0) {
////                                        logger.info("> ProcessTRH NN1 " + stock.getSymbol() + " Override 2 signal  " + stockDate.toString() + " date from last signal > 40 date");
//                                        nnSignal = emaSignal;
//                                        confident += 15;
//                                    }
                            }
                            break;
                        }
                    }  // for loop
                }

            }
        }

        if (accData.getNn() >= 3) {
            ;
        } else if (nnSignal != prevSignal) {

            // signal change double check wiht NN trend
            int trendSignal = this.Rule3_CheckTrend(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
            //override the previous NN1 prediction
            if (nnSignal == trendSignal) {
                confident += 30;
            } else {
//                logger.info("> ProcessTRH NN1 " + stock.getSymbol() + " Override 3 signal " + stockDate.toString() + " TrendSignal " + trendSignal);
            }
            nnSignal = trendSignal;
        }

        if (nnSignal != prevSignal) {
            int retSignal = Rule4_DayChange(nnSignal, prevSignal, StockArray, offset);
            if (nnSignal == retSignal) {
                confident += 10;
            }
            nnSignal = retSignal;
        }

        if ((prevSignal == ConstantKey.S_BUY) || (prevSignal == ConstantKey.S_SELL)) {
            String confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_SELL_ST;
            if (prevSignal == ConstantKey.S_SELL) {
                confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_BUY_ST;
            }
            if (stopReset == true) {
                confidentSt = confidentSt + " (Stop NTR)";
            } else if (stopLoss == true) {
                confidentSt = confidentSt + " (Stop Loss)";
            } else if (profitTake == true) {
                confidentSt = confidentSt + " (Take Profit)";
            }

            accData.setConf(confidentSt);
        }

        trObj.setTrsignal(nnSignal);
        trHistory.setTrsignal(nnSignal);
//        trHistory.setParm1((float) adxObj.adx); // getNNnormalizeInput must be set to macd vaule for NN input
//        trHistory.setParm2((float) adxObj.trsignal);
        trHistory.setParm1((float) ema510.ema);
        trHistory.setParm2((float) ema510.lastema);

        trHistory.setParm3(emaSignal);
        trHistory.setParm4(prediction);
        trHistory.setParm5(confident);

        prevSignal = nnSignal;
        return nnSignal;

    }

//
//    int ProcessTRHistoryOffsetNN2(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, ArrayList<AFstockInfo> StockArray, int offsetInput, int monthSize,
//            int prevSignal, int offset, String stdate, StockTRHistoryObj trHistory, AccountObj accountObj, AFstockObj stock, ArrayList<TradingRuleObj> tradingRuleList, ArrayList<StockTRHistoryObj> writeArray) {
//        int confident = 0;
//
//        boolean stopLoss = false;
//        boolean profitTake = false;
//        boolean stopReset = false;
//        int nnSignal = prevSignal;
//        int emaSignal = nnSignal;
//        float prediction = -1;
/////////////////////////////////   
//        EMAObj ema510 = this.getTechnicalCal(StockArray, offset);
////        EMAObj ema510 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_5, ConstantKey.INT_EMA_10);
//
//        emaSignal = ema510.trsignal;
/////////////////////////////////////////////////////
//        AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
//        Date stockDate = new Date(stockinfoT.getEntrydatel());
//        // just for testing
////        logger.info("> ProcessTRHistoryOffsetNN1 " + stock.getSymbol() + " " + stockDate.toString());
//
//        if (nnSignal == ConstantKey.S_NEUTRAL) {
//            nnSignal = emaSignal;
//        }
//        if (emaSignal == nnSignal) {
//            trObj.setTrsignal(nnSignal);
//
//        } else {
//            confident += 30;
//            NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, StockArray, offset);
//            if (nn != null) {
//                float output1 = nn.getOutput1();
//                float output2 = nn.getOutput2();
//                if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
//                    nn.setTrsignal(nnSignal);
//                    float predictionV = nn.getPrediction();
//                    if (predictionV > CKey.PREDICT_THRESHOLD) { //0.6) {
//                        nnSignal = emaSignal;
//                        confident += 30;
//                    }
//                } else {
//                    //
//                    if (writeArray.size() > 0) {
//                        for (int j = 0; j < writeArray.size(); j++) {
//                            StockTRHistoryObj lastTH = writeArray.get(writeArray.size() - 1 - j);
//                            if (lastTH.getTrsignal() != nnSignal) {
//                                float thClose = lastTH.getClose();
//                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                                float StClose = stockinfo.getFclose();
////                                int rule4Signal = specialOverrideRule4(adxObj, prevSignal, thClose, StClose);
////                                if (rule4Signal != prevSignal) {
////                                    nnSignal = rule4Signal;
////                                    break;
////                                }
//                                float delta = Rule1_StopLoss(prevSignal, thClose, StClose);
//
//                                if (delta > 0) {
////                              logger.info("> ProcessTRH NN2 " + stock.getSymbol() + " Override 1 signal " + stockDate.toString() + " dela price > 20% Delta=" + delta);
//                                    stopLoss = true;
//                                    nnSignal = emaSignal;
//                                    confident += 15;
//                                }
//                                break;
//                            }
//                        }  // for loop
//                    }
//
//                }
//                trHistory.setParmSt1(nn.getComment());
////                if (CKey.NN_DEBUG == true) {
////                    logger.info("ProcessTRHistoryOffsetNN1 " + stdate + " macdTR=" + macdSignal + " " + nn.getComment());
////                }
//            }
//        }
//        if (nnSignal != prevSignal) {
//            // signal change double check wiht NN trend
//            int trendSignal = this.Rule3_CheckTrend(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
//            //override the previous NN1 prediction
//            if (nnSignal == trendSignal) {
//                confident += 30;
//            }
//            nnSignal = trendSignal;
//        }
//
//        // get the last transaction price
//        if (writeArray.size() > 0) {
//            for (int j = 0; j < writeArray.size(); j++) {
//                StockTRHistoryObj lastTH = writeArray.get(writeArray.size() - 1 - j);
//                if (lastTH.getTrsignal() != nnSignal) {
//                    float thClose = lastTH.getClose();
//                    AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                    float StClose = stockinfo.getFclose();
//
//                    int nnSignalNew = this.Rule6_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 4);
//                    if (nnSignal == nnSignalNew) {
//                        nnSignalNew = this.Rule6_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 8);
//                    }
//
//                    if (nnSignal != nnSignalNew) {
//                        nnSignal = nnSignalNew;
//                        confident += 32;
//                        profitTake = true;
//                    }
//                }
//            }
//        }
//
//        if (nnSignal != prevSignal) {
//            int retSignal = Rule4_DayChange(nnSignal, prevSignal, StockArray, offset);
//            if (nnSignal == retSignal) {
//                confident += 10;
//            }
//            nnSignal = retSignal;
//        }
//
//        if ((prevSignal == ConstantKey.S_BUY) || (prevSignal == ConstantKey.S_SELL)) {
//            String confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_SELL_ST;
//            if (prevSignal == ConstantKey.S_SELL) {
//                confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_BUY_ST;
//            }
//            if (stopReset == true) {
//                confidentSt = confidentSt + " (Stop NTR)";
//            } else if (stopLoss == true) {
//                confidentSt = confidentSt + " (Stop Loss)";
//            } else if (profitTake == true) {
//                confidentSt = confidentSt + " (Take Profit)";
//            }
////            trHistory.setParmSt1(trHistory.getParmSt1() + " " + confidentSt);
//            trHistory.setParmSt1(confidentSt);
//        }
//
//        trObj.setTrsignal(nnSignal);
//        trHistory.setTrsignal(nnSignal);
////        trHistory.setParm1((float) adxObj.adx); // getNNnormalizeInput must be set to macd vaule for NN input
////        trHistory.setParm2((float) adxObj.trsignal);
//        trHistory.setParm1((float) ema510.ema);
//        trHistory.setParm2((float) ema510.lastema);
//
//        trHistory.setParm3(emaSignal);
//        trHistory.setParm4(prediction);
//        trHistory.setParm5(confident);
//
//        prevSignal = nnSignal;
//        return nnSignal;
//
//    }
    public EMAObj getTechnicalCal(ArrayList StockArray, int offset) {
        EMAObj ema510 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_3, ConstantKey.INT_EMA_6);
        return ema510;
    }

    public NNObj updateAdminTradingsignalNN2(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
            TradingRuleObj trObj, ArrayList StockArray, int offset, AFstockObj stock, ArrayList tradingRuleList, AccData accData) {
        NNObj nnRet = new NNObj();
        int confident = 0;
        boolean stopLoss = false;
        boolean stopReset = false;
        boolean profitTake = false;

        try {
            if (trObj.getSubstatus() == ConstantKey.OPEN) {
/////////////////////////////////////////////              
                EMAObj ema510 = this.getTechnicalCal(StockArray, offset);
                // EMAObj ema510 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_5, ConstantKey.INT_EMA_10);

                int emaSignal = ema510.trsignal;
/////////////////////////////////////////////                

                AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
                Date stockDate = new Date(stockinfoT.getEntrydatel());
                int prevSignal = trObj.getTrsignal();
                int nnSignal = trObj.getTrsignal();
                if (nnSignal == ConstantKey.S_NEUTRAL) {
                    nnSignal = emaSignal;
                }
                if (emaSignal == nnSignal) {
                    nnRet.setTrsignal(emaSignal);
                    return nnRet;
                }

                // get the last transaction price for later
                AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
                ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
                        accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);

                confident += 30;

                accData.setNn(0);
                NNObj nn = new NNObj();
                boolean nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset, stock);

                nnRet.setComment(nn.getComment());
                if (nnFlag == true) {
                    nn.setTrsignal(nnSignal);
                    nnSignal = emaSignal;
                    confident += 30;
                    if (nnSignal != prevSignal) {
                        accData.setNn(1);
                        nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset + 1, stock);
                        if (nnFlag == true) {
                            accData.setNn(accData.getNn() + 1);
                            nnFlag = this.Rule0_CheckNN(serviceAFWeb, nn, accountObj, StockArray, offset + 2, stock);
                            if (nnFlag == true) {
                                accData.setNn(accData.getNn() + 1);
                            }
                        }
                    }
                } else {

                    // get the last transaction price
                    if (thList != null) {
                        // somthing wrong. no transaction for 1 year
                        if (thList.size() == 1) {
                            // try to get the MACD transaction. for sure this will have some signal
                            thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
                                    accObj.getId() + "", symbol, ConstantKey.TR_NN1, 0);
                        }
                        if (thList.size() > 0) {
                            TransationOrderObj lastTH = thList.get(0);
                            float thClose = lastTH.getAvgprice();
                            AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                            float StClose = stockinfo.getFclose();
//                                int rule4Signal = specialOverrideRule4(adxObj, prevSignal, thClose, StClose);
//                                if (rule4Signal != prevSignal) {
//                                    nnSignal = rule4Signal;
//                                    confident += 15;
//                                } else {
                            float delta = Rule1_StopLoss(prevSignal, thClose, StClose);

                            if (delta > 0) {
                                logger.info("> updateAdminTR NN2 " + symbol + " Override 1 signal " + stockDate.toString() + " Stop loss > "+NN2StopLoss+"% Delta=" + delta);
                                stopLoss = true;
                                nnSignal = emaSignal;
                                confident += 15;
                            } else {
                                int newSignal = Rule7_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 4);
                                if (nnSignal == newSignal) {
                                    newSignal = Rule7_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 8);
                                }
//                                    int newSignal = this.Rule6_CheckProfitTake(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, prevSignal);
                                if (prevSignal != newSignal) {
                                    confident += 32;
                                    profitTake = true;
                                    nnSignal = newSignal;
                                }

                            }
//                                }
                        } //thList.size() > 0
                    }
                }

                if (nnSignal == prevSignal) {
                    // get the last transaction price

                    if (thList != null) {
                        TransationOrderObj lastTH = thList.get(0);
                        float thClose = lastTH.getAvgprice();
                        AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                        float StClose = stockinfo.getFclose();
                        int rule5_Signal = this.Rule5_ResetTR(serviceAFWeb, accountObj, StockArray, offset, stock, prevSignal, thClose, StClose);
                        if (rule5_Signal != prevSignal) {
                            logger.info("> updateAdminTR NN2 " + symbol + " Override 5 signal " + stockDate.toString());
                            nnSignal = rule5_Signal;
                            confident += 15;
                            stopReset = true;
                        }
                    }
                }

                if (accData.getNn() > 3) {
                    ;
                } else if (nnSignal != prevSignal) {
                    // signal change double check wiht NN trend
                    int trendSignal = this.Rule3_CheckTrend(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
                    //override the previous NN1 prediction
                    if (nnSignal == trendSignal) {
                        confident += 30;
                    } else {
                    }
                    nnSignal = trendSignal;
                }

                if (nnSignal != prevSignal) {
                    int retSignal = Rule4_DayChange(nnSignal, prevSignal, StockArray, offset);
                    if (nnSignal == retSignal) {
                        confident += 10;
                    }
                    nnSignal = retSignal;
                }

                if ((prevSignal == ConstantKey.S_BUY) || (prevSignal == ConstantKey.S_SELL)) {
                    String confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_SELL_ST;
                    if (prevSignal == ConstantKey.S_SELL) {
                        confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_BUY_ST;
                    }
                    if (stopReset == true) {
                        confidentSt = confidentSt + " (Stop NTR)";
                    }
                    if (stopLoss == true) {
                        confidentSt = confidentSt + " (Stop Loss)";
                    } else if (profitTake == true) {
                        confidentSt = confidentSt + " (Take Profit)";
                    }
                    nnRet.setConfident(confidentSt);
                }

                nnRet.setTrsignal(nnSignal);
                return nnRet;
            }
        } catch (Exception ex) {
            logger.info("> updateAdminTradingsignalNN2 Exception" + ex.getMessage());
        }
        return null;
    }

//    
//    public NNObj updateAdminTradingsignalNN2(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol,
//            TradingRuleObj trObj, ArrayList StockArray, int offset, AFstockObj stock, ArrayList tradingRuleList) {
//        NNObj nnRet = new NNObj();
//        boolean stopLoss = false;
//        boolean stopReset = false;
//        boolean profitTake = false;
//        int confident = 0;
//        try {
//            if (trObj.getSubstatus() == ConstantKey.OPEN) {
///////////////////////////////////////////////              
//                EMAObj ema510 = this.getTechnicalCal(StockArray, offset);
//                // EMAObj ema510 = TechnicalCal.EMASignal(StockArray, offset, ConstantKey.INT_EMA_5, ConstantKey.INT_EMA_10);
//
//                int emaSignal = ema510.trsignal;
///////////////////////////////////////////////                
//
//                AFstockInfo stockinfoT = (AFstockInfo) StockArray.get(offset);
//                Date stockDate = new Date(stockinfoT.getEntrydatel());
//                int prevSignal = trObj.getTrsignal();
//                int nnSignal = trObj.getTrsignal();
//                if (nnSignal == ConstantKey.S_NEUTRAL) {
//                    nnSignal = emaSignal;
//                }
//                if (emaSignal == nnSignal) {
//                    nnRet.setTrsignal(emaSignal);
//                    return nnRet;
//                }
//                confident += 30;
//                NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, StockArray, offset);
//
//                if (nn != null) {
//                    float output1 = nn.getOutput1();
//                    float output2 = nn.getOutput2();
//                    if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
//                        nn.setTrsignal(nnSignal);
//                        float predictionV = nn.getPrediction();
//                        if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
//                            nnSignal = emaSignal;
//                            confident += 30;
//                        }
//                    } else {
//                        // get the last transaction price
//                        AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
//                        ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
//                                accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);
//
//                        if (thList != null) {
//                            // somthing wrong. no transaction for 1 year
//                            if (thList.size() == 1) {
//                                // try to get the MACD transaction. for sure this will have some signal
//                                thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
//                                        accObj.getId() + "", symbol, ConstantKey.TR_NN1, 0);
//                            }
//                            if (thList.size() > 0) {
//                                TransationOrderObj lastTH = thList.get(0);
//                                float thClose = lastTH.getAvgprice();
//                                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                                float StClose = stockinfo.getFclose();
////                                int rule4Signal = specialOverrideRule4(adxObj, prevSignal, thClose, StClose);
////                                if (rule4Signal != prevSignal) {
////                                    nnSignal = rule4Signal;
////                                    confident += 15;
////                                } else {
//                                float delta = Rule1_StopLoss(prevSignal, thClose, StClose);
//
//                                if (delta > 0) {
//                                    logger.info("> updateAdminTR NN2 " + symbol + " Override 1 signal " + stockDate.toString() + " Stop loss > 20% Delta=" + delta);
//                                    stopLoss = true;
//                                    nnSignal = emaSignal;
//                                    confident += 15;
//                                }
////                                }
//                            } //thList.size() > 0
//                        }
//                    }
//                }
//
//                if (nnSignal == prevSignal) {
//                    // get the last transaction price
//                    AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
//                    ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
//                            accObj.getId() + "", symbol, ConstantKey.TR_NN1, 0);
//                    if (thList != null) {
//                        TransationOrderObj lastTH = thList.get(0);
//                        float thClose = lastTH.getAvgprice();
//                        AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                        float StClose = stockinfo.getFclose();
//                        int rule5_Signal = this.Rule5_ResetTR(serviceAFWeb, accountObj, StockArray, offset, stock, prevSignal, thClose, StClose);
//                        if (rule5_Signal != prevSignal) {
//                            logger.info("> updateAdminTR NN1 " + symbol + " Override 5 signal " + stockDate.toString());
//                            nnSignal = rule5_Signal;
//                            confident += 15;
//                            stopReset = false;
//
//                        }
//                    }
//                }
//
//                if (nnSignal != prevSignal) {
//                    // signal change double check wiht NN trend
//                    int trendSignal = this.Rule3_CheckTrend(serviceAFWeb, accountObj, stock.getSymbol(), trObj, StockArray, offset, stock, tradingRuleList, nnSignal);
//                    //override the previous NN1 prediction
//                    if (nnSignal == trendSignal) {
//                        confident += 30;
//                    }
//                    nnSignal = trendSignal;
//                }
//
//                // get the last transaction price
//                AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
//                ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
//                        accObj.getId() + "", symbol, ConstantKey.TR_NN2, 0);
//                if (thList != null) {
//                    if (thList.size() > 0) {
//                        TransationOrderObj lastTH = thList.get(0);
//                        float thClose = lastTH.getAvgprice();
//                        AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
//                        float StClose = stockinfo.getFclose();
//
//                        int nnSignalNew = this.Rule6_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 4);
//                        if (nnSignal == nnSignalNew) {
//                            nnSignalNew = this.Rule6_CheckProfitTake(nnSignal, StockArray, offset, thClose, StClose, 8);
//                        }
//
//                        if (nnSignal != nnSignalNew) {
//                            nnSignal = nnSignalNew;
//                            confident += 32;
//                            profitTake = true;
//                        }
//                    }
//                }
//
//                if (nnSignal != prevSignal) {
//                    int retSignal = Rule4_DayChange(nnSignal, prevSignal, StockArray, offset);
////                    if (ServiceAFweb.mydebugtestflag == true) {
////                        if (stock.getSymbol().equals("HOU.TO")) {
////                            logger.info("> updateAdminTradingsignalNN1 " + ", offset=" + offset + ", retSignal=" + retSignal + ", nnSignal=" + nnSignal);
////                        }
////                    }
//                    if (nnSignal == retSignal) {
//                        confident += 10;
//                    }
//                    nnSignal = retSignal;
//                }
//
//                if ((prevSignal == ConstantKey.S_BUY) || (prevSignal == ConstantKey.S_SELL)) {
//                    String confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_SELL_ST;
//                    if (prevSignal == ConstantKey.S_SELL) {
//                        confidentSt = stockDate.toString() + " " + confident + "% confident on " + ConstantKey.S_BUY_ST;
//                    }
//                    if (stopReset == true) {
//                        confidentSt = confidentSt + " (Stop NTR)";
//                    } else if (stopLoss == true) {
//                        confidentSt = confidentSt + " (Stop Loss)";
//                    } else if (profitTake == true) {
//                        confidentSt = confidentSt + " (Take Profit)";
//                    }
//
//                    nnRet.setConfident(confidentSt);
//                }
//                nnRet.setTrsignal(nnSignal);
//                return nnRet;
//            }
//        } catch (Exception ex) {
//            logger.info("> updateAdminTradingsignalNN2 Exception" + ex.getMessage());
//        }
//        return null;
//    }
// 
    public boolean Rule0_CheckNN(ServiceAFweb serviceAFWeb, NNObj nn, AccountObj accountObj,
            ArrayList StockArray, int offset, AFstockObj stock) {
        NNObj nnTmp = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN2, accountObj, stock, StockArray, offset);

        if (nnTmp != null) {
            nn.setComment(nnTmp.getComment());
            nn.setOutput1(nnTmp.getOutput1());
            nn.setOutput2(nnTmp.getOutput2());
            nn.setPrediction(nnTmp.getPrediction());

            float output1 = nnTmp.getOutput1();
            float output2 = nnTmp.getOutput2();
            if ((CKey.PREDICT_THRESHOLD < output1) || (CKey.PREDICT_THRESHOLD < output2)) {
                float predictionV = nnTmp.getPrediction();
                if (predictionV > CKey.PREDICT_THRESHOLD) { //0.8) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float NN2StopLoss = 12; //7; //5; //16; 
    // check stop loss

    public float Rule1_StopLoss(int currSignal, float thClose, float StClose) {
//        if (true) {
//            return 0;
//        }
        float delPer = 100 * (StClose - thClose) / thClose;

        if (currSignal == ConstantKey.S_BUY) {
            if (delPer < -NN2StopLoss) {
                delPer = Math.abs(delPer);
                return delPer;
            }
        } else if (currSignal == ConstantKey.S_SELL) {
            if (delPer > NN2StopLoss) {
                delPer = Math.abs(delPer);
                return delPer;
            }
        }
        return 0;
    }

    public int Rule5_ResetTR(ServiceAFweb serviceAFWeb, AccountObj accountObj, ArrayList StockArray, int offset, AFstockObj stock,
            int currSignal, float thClose, float StClose) {

//        if (ServiceAFweb.mydebugnewtest == false) {
//            return currSignal;
//        }
        boolean checkResetTR = false;
//        checkResetTR = true; ////// just for testing

        float delPer = 100 * (StClose - thClose) / thClose;

        float delErr = (float) 1.6; //2; // greater 1.5%

        if (currSignal == ConstantKey.S_BUY) {
            if (delPer < -delErr) {
                delPer = Math.abs(delPer);
                checkResetTR = true;
            }
        } else if (currSignal == ConstantKey.S_SELL) {
            if (delPer > delErr) {
                delPer = Math.abs(delPer);
                checkResetTR = true;
            }
        }
        if (checkResetTR == true) {
            TradingSignalProcess TS = new TradingSignalProcess();
            String trName = ConstantKey.TR_NN92;
            ///asc thObjList old first - recent last
            ArrayList<StockTRHistoryObj> THhistory = TS.resetVitualTransaction(serviceAFWeb, stock, trName);
            if (THhistory != null) {
                if (THhistory.size() > 0) {
                    ///desc recent last - thObjList old first
                    Collections.reverse(THhistory);
                    StockTRHistoryObj thObj = THhistory.get(0);
                    int resetSignal = thObj.getTrsignal();
                    if (currSignal != resetSignal) {
                        return resetSignal;
                    }
                }
            }
        }
        return currSignal;
    }

    public float Rule2_LongTran(NNObj nn, long lastTHLong, long curSGLong) {
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

    // return signal
    // check current trend change
    public int Rule3_CheckTrend(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol, TradingRuleObj trObj, ArrayList StockArray, int offset, AFstockObj stock, ArrayList tradingRuleList, int nnSignal) {
//        if (true) {
//            return nnSignal;
//        }
//        return nnSignal;  //////disable must return the original signal
////////        
//        NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN40, accountObj, stock, tradingRuleList, StockArray, offset);
        NNObj nn = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN30, accountObj, stock, StockArray, offset);
        if (nn != null) {

            float output1 = nn.getOutput1();
            float output2 = nn.getOutput2();
            if ((CKey.PREDICT_THRESHOLD > output1) && (CKey.PREDICT_THRESHOLD > output2)) {
                // loop to find the previous trend.

                for (int i = 0; i < 5; i++) {
                    //StockArray recent to old date
//                    NNObj nn1 = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN40, accountObj, stock, tradingRuleList, StockArray, offset + 1 + i);
                    NNObj nn1 = NNCal.NNpredict(serviceAFWeb, ConstantKey.INT_TR_NN30, accountObj, stock, StockArray, offset + 1 + i);
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

                EMAObj ema510 = TechnicalCal.EMASignal(StockPredArray, 0, ConstantKey.INT_EMA_5, ConstantKey.INT_EMA_10);
                int macdSignal = ema510.trsignal;

//                // MACD1
//                MACDObj macdNN = TechnicalCal.MACD(StockPredArray, 0, ConstantKey.INT_MACD1_6, ConstantKey.INT_MACD1_12, ConstantKey.INT_MACD1_4);
//                int macdSignal = macdNN.trsignal;
                return macdSignal;
            }
        }
        return nnSignal;
    }

    // check current day change
    public int Rule4_DayChange(int newSignal, int preSignal, ArrayList StockArray, int offset) {

        AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
        float StClose = stockinfo.getFclose();
        AFstockInfo stockinfoPrev = (AFstockInfo) StockArray.get(offset + 1);
        float StClosePrev = stockinfoPrev.getFclose();

        float delPer = 100 * (StClose - StClosePrev) / StClosePrev;

        if (newSignal == ConstantKey.S_BUY) {
            if (delPer < -1.1) {
                return preSignal;
            }
        } else if (newSignal == ConstantKey.S_SELL) {
            if (delPer > 1.1) {
                return preSignal;
            }
        }
        return newSignal;
    }

    public int Rule6_CheckProfitTake(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol, TradingRuleObj trObj, ArrayList StockArray, int offset, AFstockObj stock, ArrayList tradingRuleList, int nnSignal) {
        try {
            // get the last transaction price
            AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
            ArrayList<TransationOrderObj> thList = serviceAFWeb.getAccountStockTRTranListByAccountID(CKey.ADMIN_USERNAME, null,
                    accObj.getId() + "", symbol, ConstantKey.TR_NN1, 0);
            if (thList != null) {
                TransationOrderObj lastTH = thList.get(0);
                float thClose = lastTH.getAvgprice();
                AFstockInfo stockinfo = (AFstockInfo) StockArray.get(offset);
                float StClose = stockinfo.getFclose();

                float delPer = 100 * (StClose - thClose) / thClose;

                float DEL_ERR = (float) 7; //10;
                int currSignal = nnSignal;

                // need to check if 5 days has drop of 5%
                AFstockInfo stockinfo5 = (AFstockInfo) StockArray.get(offset + 5);
                float StClose5 = stockinfo5.getFclose();
                float delERR5 = (float) 3.5;

                if (currSignal == ConstantKey.S_BUY) {
                    if (delPer > DEL_ERR) {
                        float delPer5 = 100 * (StClose - StClose5) / StClose5;
                        if (delPer5 < -delERR5) {
                            // force to take profit
                            return ConstantKey.S_SELL;
                        }
                    }
                } else if (currSignal == ConstantKey.S_SELL) {
                    if (delPer < -DEL_ERR) {
                        float delPer5 = 100 * (StClose - StClose5) / StClose5;
                        if (delPer5 > delERR5) {
                            // force to take profit
                            return ConstantKey.S_BUY;
                        }
                    }
                }

            }
        } catch (Exception ex) {
        }
        return nnSignal;
    }

    private int Rule7_CheckProfitTake(int nnSignal, ArrayList StockArray, int offset, float thClose, float StClose, int day) {
        float DEL_ERR = (float) 12; //7; //10;
        int currSignal = nnSignal;

        float delPer = 100 * (StClose - thClose) / thClose;
        // need to check if 5 days has drop of 5%
        AFstockInfo stockinfo5 = (AFstockInfo) StockArray.get(offset + day);
        float StClose5 = stockinfo5.getFclose();
        float delERR5 = (float) 6; //2;

        if (currSignal == ConstantKey.S_BUY) {
            if (delPer > DEL_ERR) {
                float delPer5 = 100 * (StClose - StClose5) / StClose5;
                if (delPer5 < -delERR5) {
                    // force to take profit
                    return ConstantKey.S_SELL;
                }
            }
        } else if (currSignal == ConstantKey.S_SELL) {
            if (delPer < -DEL_ERR) {
                float delPer5 = 100 * (StClose - StClose5) / StClose5;
                if (delPer5 > delERR5) {
                    // force to take profit
                    return ConstantKey.S_BUY;
                }
            }
        }
        return currSignal;
    }

//    private int Rule6_CheckProfitTake(int nnSignal, ArrayList StockArray, int offset, float thClose, float StClose, int day) {
//        float DEL_ERR = (float) 10; //10;
//        int currSignal = nnSignal;
//
//        float delPer = 100 * (StClose - thClose) / thClose;
//        // need to check if 5 days has drop of 5%
//        AFstockInfo stockinfo5 = (AFstockInfo) StockArray.get(offset + day);
//        float StClose5 = stockinfo5.getFclose();
//        float delERR5 = (float) 3.5;
//
//        if (currSignal == ConstantKey.S_BUY) {
//            if (delPer > DEL_ERR) {
//                float delPer5 = 100 * (StClose - StClose5) / StClose5;
//                if (delPer5 < -delERR5) {
//                    // force to take profit
//                    return ConstantKey.S_SELL;
//                }
//            }
//        } else if (currSignal == ConstantKey.S_SELL) {
//            if (delPer < -DEL_ERR) {
//                float delPer5 = 100 * (StClose - StClose5) / StClose5;
//                if (delPer5 > delERR5) {
//                    // force to take profit
//                    return ConstantKey.S_BUY;
//                }
//            }
//        }
//        return currSignal;
//    }
    ////////////////////////////////////////////////////////
    public ArrayList<NNInputDataObj> getAccountStockTRListHistoryEMANN2(ArrayList<StockTRHistoryObj> thObjListEMA, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListMACD,
            String stockidsymbol, NNTrainObj nnTraining, boolean lastDateOutput) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        if ((thObjListEMA == null) || (thObjListMV == null)) {
            return null;
        }
        if (thObjListEMA.size() != thObjListMV.size()) {
            return null;
        }
        if (thObjListEMA.size() != thObjListEMA.size()) {
            return null;
        }
        NNTrainObj nnTr = new NNTrainObj();
        if (nnTraining != null) {
            nnTr = nnTraining;
        }
        ArrayList<NNInputOutObj> trInputList = new ArrayList();
        nnTr.setNnInputList(trInputList);

        StockTRHistoryObj prevThObj = null;

        boolean processLastDate = false;

        ArrayList<NNInputDataObj> inputDatalist = new ArrayList<NNInputDataObj>();
        NNInputDataObj objDataPrev = null;

        for (int i = 0; i < thObjListEMA.size(); i++) {

            if (i + 1 == thObjListEMA.size()) {
                if (lastDateOutput == true) {
                    processLastDate = true;
                }
            }
            NNInputOutObj inputList = new NNInputOutObj();

            StockTRHistoryObj thObjEMA = thObjListEMA.get(i);
            if (i == 0) {
                prevThObj = thObjEMA;
            }

            int signal = thObjEMA.getTrsignal();
            boolean contProcess = false;
            if (signal != prevThObj.getTrsignal()) {
                contProcess = true;
            }
            if (processLastDate == true) {
                contProcess = true;
            }

            if (contProcess == true) {
                // setup input parameter in inputList
                inputList = this.setupInputNN2(i, signal, thObjListMACD, thObjListMV, thObjListEMA);
                if (inputList == null) {
                    continue;
                }
                int retDecision = checkNNsignalDecision(thObjEMA, prevThObj);

                double output = retDecision;

                NNInputDataObj objDataCur = new NNInputDataObj();
                objDataCur.setUpdatedatel(thObjEMA.getUpdateDatel());
                objDataCur.setObj(inputList);

                if (objDataPrev != null) {
                    if (output == 1) {
                        objDataPrev.getObj().setOutput1(0.9);
                        objDataPrev.getObj().setOutput2(0.1);
                    } else if (output == 0) {
                        objDataPrev.getObj().setOutput1(0.1);
                        objDataPrev.getObj().setOutput2(0.9);
                    } else {
                        objDataPrev.getObj().setOutput1(0.1);
                        objDataPrev.getObj().setOutput2(0.1);
                    }
                    trInputList.add(objDataPrev.getObj());
                    inputDatalist.add(objDataPrev);

//                    if (getEnv.checkLocalPC() == true) {
//                        if (CKey.NN_DEBUG == true) {
//
//                            NNInputOutObj objP = objDataPrev.getObj();
//                            String st = "\"" + objP.getDateSt() + "\",\"" + objP.getClose() + "\",\"" + objP.getTrsignal()
//                                    + "\",\"" + objP.getOutput1()
//                                    + "\",\"" + objP.getInput1() + "\",\"" + objP.getInput2() + "\",\"" + objP.getInput3()
//                                    + "\",\"" + objP.getInput4() + "\",\"" + objP.getInput5() + "\",\"" + objP.getInput6()
//                                    + "\",\"" + objP.getInput7() + "\",\"" + objP.getInput8()
//                                    + "\",\"" + objP.getInput9() + "\",\"" + objP.getInput10()
//                                    + "\"";
//                            logger.info(i + "," + st);
//                        }
//                    }
                }
                prevThObj = thObjEMA;
                objDataPrev = objDataCur;

            }
        }// end of loop
        if (objDataPrev != null) {
            if (lastDateOutput == true) {
                // eddy just for testing
//                trInputList.clear(); // clear so that only the last one
            }
            trInputList.add(objDataPrev.getObj());
            objDataPrev.getObj().setOutput1(0);
            trInputList.add(objDataPrev.getObj());
            inputDatalist.add(objDataPrev);

        }
        /// adding extra in betreen signal in case buy and sell is large > 10 day. 
        //  so, just add day 5 as extra signal
        ArrayList<NNInputDataObj> inputRetDatalist = new ArrayList<NNInputDataObj>();
//        boolean flag = true;
//        if (flag == false) {
//            return inputDatalist;
//        }

        if (inputDatalist != null) {
            if (inputDatalist.size() > 1) {
                for (int i = 0; i < inputDatalist.size(); i++) {
                    NNInputDataObj inputDaObj0 = inputDatalist.get(i);

                    inputRetDatalist.add(inputDaObj0);
                    if ((i + 1) >= inputDatalist.size()) {
                        continue;
                    }
                    NNInputDataObj inputDaObj1 = inputDatalist.get(i + 1);
                    int index0 = inputDaObj0.getObj().getIndex();
                    int index1 = inputDaObj1.getObj().getIndex();
                    int step = (index1 - index0) / 10;
                    if (step > 1) {
                        for (int j = 1; j < step; j++) {
                            int index = index0 + 10 * j;
                            double output1 = inputDaObj0.getObj().getOutput1();
                            double output2 = inputDaObj0.getObj().getOutput2();
                            if ((output1 == 0.1) && (output2 == 0.1)) {
                                continue;
                            }
                            NNInputDataObj inputDaObj = new NNInputDataObj();
                            NNInputOutObj inputList = new NNInputOutObj();

                            int signal = inputDaObj0.getObj().getTrsignal();

                            for (int k = index; k < index1; k++) {
                                StockTRHistoryObj thObjEMA = thObjListEMA.get(index);
                                int signalIndex = thObjEMA.getTrsignal();
                                if (signalIndex == signal) {
                                    index = k;
                                    break;
                                }
                            }

                            inputList = this.setupInputNN2(index, signal, thObjListMACD, thObjListMV, thObjListEMA);
                            if (inputList == null) {
                                continue;
                            }
                            inputList.setOutput1(output1);
                            inputList.setOutput2(output2);

                            StockTRHistoryObj thObjEMAIndex = thObjListEMA.get(index);
                            inputDaObj.setUpdatedatel(thObjEMAIndex.getUpdateDatel());
                            inputDaObj.setObj(inputList);
                            inputRetDatalist.add(inputDaObj);
//                                logger.info("> getAccountStockTR MACD NN1 add " + inputDaObj.getObj().getDateSt());

                        }
                    }

                }
            }
        }

        return inputRetDatalist;

    }

    public NNInputOutObj setupInputNN2(int i, int signal, ArrayList<StockTRHistoryObj> thObjListMACD,
            ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        NNInputOutObj inputList = new NNInputOutObj();
        inputList = NNProcessImp.getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);
        if (inputList == null) {
            return inputList;
        }
        double parm1 = -1;
        if (signal == ConstantKey.S_BUY) {
            parm1 = 0.9;
        } else if (signal == ConstantKey.S_SELL) {
            parm1 = 0.1;
        }

        inputList.setInput1(parm1);
        inputList.setTrsignal(signal);
        inputList.setIndex(i);
        ArrayList<Double> closeArray = NNProcessImp.getNNnormalizeInputClose(i, thObjListMACD);
        inputList.setInput6(closeArray.get(0));
        inputList.setInput7(closeArray.get(1));
        inputList.setInput8(closeArray.get(2));
        inputList.setInput9(closeArray.get(3));
        inputList.setInput10(closeArray.get(4));

        ArrayList<Double> volumeArray = NNProcessImp.getNNnormalizeInputVolume(i, thObjListMACD);
        // override close normalize
//          inputList.setInput9(volumeArray.get(0));
        inputList.setInput10(volumeArray.get(1));

        return inputList;
    }

    public static int checkNNsignalDecision(StockTRHistoryObj thObj, StockTRHistoryObj prevThObj) {
        if (prevThObj == null) {
            prevThObj = thObj;
        }
        int retDecision = -1;
        int pervSignal = prevThObj.getTrsignal();

        float pricePrev = prevThObj.getClose();
        float price = thObj.getClose();
        float percent = (price - pricePrev) / pricePrev;
        percent = percent * 100 * 15;
        float percentAbs = Math.abs(percent);
        if (percentAbs < 25) { //30) { //20){
            return -1;
        }

        if (pervSignal == ConstantKey.S_BUY) {
            retDecision = 0;
            if (thObj.getClose() > prevThObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }
        if (pervSignal == ConstantKey.S_SELL) {
            retDecision = 0;
            if (prevThObj.getClose() > thObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }

        return -1;
    }

}
