/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.util.CKey;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.nnBP.NNBPservice;
import com.afweb.service.*;

import com.afweb.signal.*;
import com.afweb.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class NNProcessBySignal {

    public static Logger logger = Logger.getLogger("NNProcess");

    public void processNeuralNet(ServiceAFweb serviceAFWeb) {

        TrandingSignalProcess.forceToGenerateNewNN = false;       
        
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        

        boolean flagNNLearning = false;
        if (flagNNLearning == true) {
            int k = 0;
            NNProcessByTrend nntrend = new NNProcessByTrend();

            while (true) {
                k++;
                long currentTime = System.currentTimeMillis();
                long lockDate1Hour = TimeConvertion.addHours(currentTime, 2);
                logger.info("> ProcessTrainNeuralNet NN 1 cycle " + k);
                NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN1);
                NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN2);
                ProcessTrainNeuralNetBySign(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet NN 3 cycle " + k);
                NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN3);
                nntrend.ProcessTrainNeuralNetByTrend(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet end... cycle " + k);
                while (true) {
                    currentTime = System.currentTimeMillis();
                    if (lockDate1Hour < currentTime) {
                        break;
                    }
                    // check every 10 minutes

                    try {
                        Thread.sleep(60 * 10 * 1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        boolean flagReLeanInput = false;
        if (flagReLeanInput == true) {
            logger.info("> ProcessReLeanInput NN 1 ");
            ProcessReLeanInput(serviceAFWeb);
            logger.info("> ProcessReLeanInput end... ");
            logger.info("> SystemPocessFundMgr start... ");
            serviceAFWeb.SystemPocessFundMgr();
            logger.info("> SystemFundMgr start... ");
            serviceAFWeb.SystemFundMgr();
            logger.info("> SystemPocessFundMgr SystemFundMgr end... ");
        }

        boolean flagNeuralnetInput = false;
        if (flagNeuralnetInput == true) {
            logger.info("> NeuralnetInput TR NN1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            logger.info("> NeuralnetInput TR NN2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);
            // need to debug to generate the java first time
            TrandingSignalProcess.forceToGenerateNewNN = true;
        }
        boolean flagNeuralnetTrain = false;
        if (flagNeuralnetTrain == true) {
            TrandingSignalProcess.forceToErrorNewNN = true;
            // start training
            // TrainingNNBP inputpattern 1748
            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);

        }
        boolean flagNeuralnetCreateJava = false;
        if (flagNeuralnetCreateJava == true) {
            // make sure you chenage the String version = "2.0903" each time
            //must match to the nnData version
            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);

        }

        boolean flagNeural = false;
        if (flagNeural == true) {
            // delete all data
//            serviceAFWeb.getStockImp().deleteNeuralNetDataTable();
            // delete all data
            serviceAFWeb.SystemClearNNinput();
            for (int k = 0; k < 10; k++) {
                ProcessTrainNeuralNetBySign(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet NN1 cycle " + k);
            }

        }

        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////   
        boolean flagTestNNSignal = false;
        if (flagTestNNSignal == true) {
            String symbol = "HOU.TO";

            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            int size1year = 5 * 52;
            ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year * 4);
            AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
            String trName = ConstantKey.TR_NN2;
            ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accObj.getId(), symbol);
            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accObj.getId(), stock.getId(), trName);
            ProcessNN2 nn2 = new ProcessNN2();
            int offset = 0;
            ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
            nn2.updateAdminTradingsignalnn2(serviceAFWeb, accObj, symbol, trObj, StockArray, offset, UpdateTRList, stock, tradingRuleList);

            logger.info("> flagTestNNSignal ");
        }
//
//
//        NNProcessImp.inputReTrainStockNeuralNetData(serviceAFWeb, ConstantKey.INT_TR_NN3, "IWM");

        boolean flagTestHistorySignal = false;
        if (flagTestHistorySignal == true) {
            String symbol = "HOU.TO";
            symbol = "SPY";
            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            int size1year = 20 * 12 * 4 + (50 * 3);
            ArrayList StockArray = serviceAFWeb.getStockHistorical(stock.getSymbol(), size1year);
            AccountObj accObj = serviceAFWeb.getAdminObjFromCache();
            String trName = ConstantKey.TR_NN2;
            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accObj.getId(), stock.getId(), trName);

            serviceAFWeb.SystemAccountStockClrTranByAccountID(accObj, stock.getId(), trObj.getTrname());
            // get 2 year
            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//            ArrayList<StockTRHistoryObj> trHistoryList = TRprocessImp.ProcessTRHistory(serviceAFWeb, trObj, 2);

            int offset = 0;
            ProcessNN2 nn2 = new ProcessNN2();
//            ArrayList<NNInputDataObj> inputList = nn2.trainingNN2dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);

            ArrayList<StockTRHistoryObj> trHistoryList = null;
            String StFileName = serviceAFWeb.FileLocalPath + "trHistory.txt";
            boolean flagWrite = true;
            if (flagWrite == true) {
                trHistoryList = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObj, StockArray, offset, CKey.MONTH_SIZE);

                try {
                    String st = new ObjectMapper().writeValueAsString(trHistoryList);
                    StringBuffer stBuf = new StringBuffer(st);
                    FileUtil.FileWriteText(StFileName, stBuf);
                } catch (JsonProcessingException ex) {
                }
            } else {
                try {
                    StringBuffer stBuf = FileUtil.FileReadText(StFileName);
                    String st = stBuf.toString();
                    StockTRHistoryObj[] arrayItem = new ObjectMapper().readValue(st, StockTRHistoryObj[].class);
                    List<StockTRHistoryObj> listItem = Arrays.<StockTRHistoryObj>asList(arrayItem);
                    trHistoryList = new ArrayList<StockTRHistoryObj>(listItem);
                } catch (IOException ex) {
                }
            }
            int lastSignal = 0;
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long date2yrBack = TimeConvertion.addMonths(dateNow.getTimeInMillis(), -24); //2 yr before

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
                    int ret = TRprocessImp.AddTransactionOrder(serviceAFWeb, accObj, stock, trObj.getTrname(), signal, dateOffet, true);

                }
            }

            logger.info("> flagTestHistorySignal ");
        }

        boolean flagTestNeuralnetTrain = false;
        if (flagTestNeuralnetTrain == true) {
            String symbol = "HOU.TO";

            int nnTRN = ConstantKey.INT_TR_NN1;
            String nnName = ConstantKey.TR_NN1;

            inputStockNeuralNetBySignal(serviceAFWeb, nnTRN, symbol);
            stockTrainNeuralNet(serviceAFWeb, nnTRN, symbol);
        }

//        boolean inputTest = false;
//        if (inputTest == true) {
//            ArrayList writeArray = new ArrayList();
//            ArrayList<NNInputOutObj> inputlist = new ArrayList();
//            String nnName = ConstantKey.TR_NN1;
//            TrandingSignalProcess TSignal = new TrandingSignalProcess();
//
//            inputlist = TSignal.getTrainingInputFromFile(serviceAFWeb, nnName);
//            for (int i = 0; i < inputlist.size(); i++) {
//                NNInputOutObj inputObj = inputlist.get(i);
//                String st = "";
//                st += inputObj.getDateSt();
//                st += "," + inputObj.getClose();
//                st += "," + inputObj.getTrsignal();
//                st += "," + inputObj.getInput1();
//                st += "," + inputObj.getInput2();
//                st += "," + inputObj.getInput3();
//                st += "," + inputObj.getInput4();
//                st += "," + inputObj.getInput5();
//                st += "," + inputObj.getInput6();
//                st += "," + inputObj.getInput7();
//                st += "," + inputObj.getInput8();
//                st += "," + inputObj.getInput9();
//                st += "," + inputObj.getInput10();
//                st += "," + inputObj.getOutput1();
//                st += "," + inputObj.getOutput2();
//                st += "," + inputObj.getOutput3();
//                st += "," + inputObj.getOutput4();
//                writeArray.add(st);
//            }
//            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + "test1.csv", writeArray);
//
/////////////
//            writeArray = new ArrayList();
//            inputlist = new ArrayList();
//            ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
//            if (nnName.equals(ConstantKey.TR_NN4)) {
//                inputDatalist = NNProcessStock.NeuralNetGetNN4InputfromStaticCode("");
//            } else {
//                inputDatalist = TradingNNprocess.NeuralNetGetNN1InputfromStaticCode("");
//            }
//            for (int i = 0; i < inputDatalist.size(); i++) {
//                NNInputDataObj inputDObj = inputDatalist.get(i);
//                NNInputOutObj inputObj = new NNInputOutObj();
//                inputObj.setDateSt(inputDObj.getObj().getDateSt());
//                inputObj.setClose(inputDObj.getObj().getClose());
//                inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
//                inputObj.setInput1(inputDObj.getObj().getInput1());
//                inputObj.setInput2(inputDObj.getObj().getInput2());
//                inputObj.setInput3(inputDObj.getObj().getInput3());
//                inputObj.setInput4(inputDObj.getObj().getInput4());
//                inputObj.setInput5(inputDObj.getObj().getInput5());
//                inputObj.setInput6(inputDObj.getObj().getInput6());
//                inputObj.setInput7(inputDObj.getObj().getInput7());
//                inputObj.setInput8(inputDObj.getObj().getInput8());
//                inputObj.setInput9(inputDObj.getObj().getInput9());
//                inputObj.setInput10(inputDObj.getObj().getInput10());
//                inputObj.setInput11(inputDObj.getObj().getInput11());
//                inputObj.setInput12(inputDObj.getObj().getInput12());
//                inputObj.setInput13(inputDObj.getObj().getInput13());
//                //////
//                inputObj.setOutput1(inputDObj.getObj().getOutput1());
//                inputObj.setOutput2(inputDObj.getObj().getOutput2());
//                inputObj.setOutput3(inputDObj.getObj().getOutput3());
//                inputObj.setOutput4(inputDObj.getObj().getOutput4());
//                if (inputObj.getOutput1() < 0) {
//                    continue;
//                }
//                if (inputObj.getOutput2() < 0) {
//                    continue;
//                }
//                inputlist.add(inputObj);
//            }
//
//            for (int i = 0; i < inputlist.size(); i++) {
//                NNInputOutObj inputObj = inputlist.get(i);
//                String st = "";
//                st += inputObj.getDateSt();
//                st += "," + inputObj.getClose();
//                st += "," + inputObj.getTrsignal();
//                st += "," + inputObj.getInput1();
//                st += "," + inputObj.getInput2();
//                st += "," + inputObj.getInput3();
//                st += "," + inputObj.getInput4();
//                st += "," + inputObj.getInput5();
//                st += "," + inputObj.getInput6();
//                st += "," + inputObj.getInput7();
//                st += "," + inputObj.getInput8();
//                st += "," + inputObj.getInput9();
//                st += "," + inputObj.getInput10();
//                st += "," + inputObj.getOutput1();
//                st += "," + inputObj.getOutput2();
//                st += "," + inputObj.getOutput3();
//                st += "," + inputObj.getOutput4();
//                writeArray.add(st);
//            }
//            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalDebugPath + "test2.csv", writeArray);
//
//        }
    }

    /////////////////////////////////////////////////////////
    public void ProcessReLeanInput(ServiceAFweb serviceAFWeb) {
        boolean flagReLeanInput = true;
        if (flagReLeanInput == true) {
            TradingNNprocess trainNN = new TradingNNprocess();
            int custId = 0;;
            String Name = TradingNNprocess.cfg_stockNNretrainNameArray;
            ArrayList stockNameArray = null;
//
//            serviceAFWeb.getAccountImp().removeCommByCustID(custId);
//
            ArrayList<CommObj> commObjArry = serviceAFWeb.getAccountImp().getComObjByCustName(custId, Name);
            if (commObjArry != null) {
                if (commObjArry.size() > 0) {
                    CommObj commObj = commObjArry.get(0);
                    String stockNameArraySt = commObj.getData();
                    try {
                        stockNameArraySt = StringTag.replaceAll("^", "\"", stockNameArraySt);
                        stockNameArray = new ObjectMapper().readValue(stockNameArraySt, ArrayList.class);
                    } catch (Exception ex) {
                    }
                }
            } else {
                ArrayList stArray = new ArrayList();
                String msg = "";
                try {
                    msg = new ObjectMapper().writeValueAsString(stArray);
                } catch (JsonProcessingException ex) {
                }
                msg = StringTag.replaceAll("\"", "^", msg);

                serviceAFWeb.getAccountImp().addCommByCustName(custId, Name, msg);
            }
            if ((stockNameArray == null) || (stockNameArray.size() == 0)) {

                stockNameArray = trainNN.reLearnInputStockNNprocessNameArray(serviceAFWeb);
            }
            trainNN.setStockNNretrainNameArray(stockNameArray);

//            //////////
//            int cfgId = 0;;
//            String cfgName = TradingNNprocess.cfg_stockNNretrainNameArray;
//            commObjArry = serviceAFWeb.getAccountImp().getComObjByCustName(cfgId, cfgName);
//            if (commObjArry != null) {
//                CommObj commObj = commObjArry.get(0);
//                String dataSt = "";
//                try {
//                    dataSt = new ObjectMapper().writeValueAsString(stockNameArray);
//                } catch (JsonProcessingException ex) {
//                }
//                dataSt = StringTag.replaceAll("\"", "^", dataSt);
//                commObj.setData(dataSt);
//                serviceAFWeb.getAccountImp().updateCommByCustNameById(commObj);
//            }
//            ////////////
            TradingNNprocess NNProcessImp = new TradingNNprocess();
            NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);

        }

    }

    // training neural net input data
    // create neural net input data
    //     
    private void NeuralNetInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        int sizeYr = 2;
        for (int j = 0; j < sizeYr; j++) { //4; j++) {
            int size = 20 * CKey.MONTH_SIZE * j;
//                writeArrayNeuralNet.clear();
            serviceAFWeb.initTrainNeuralNetNumber = j + 1;
            logger.info("> initTrainNeuralNetNumber " + serviceAFWeb.initTrainNeuralNetNumber);
            String symbol = "";
            String symbolL[] = ServiceAFweb.primaryStock;
            for (int i = 0; i < symbolL.length; i++) {
                symbol = symbolL[i];
                ArrayList<NNInputDataObj> InputList = getTrainingNNdataProcess(serviceAFWeb, symbol, TR_Name, size);
            }
        }
    }

    public ArrayList<NNInputDataObj> getTrainingNNdataProcess(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
        logger.info("> getTrainingNNdataProcess " + symbol);

        boolean trainStock = false;
        for (int i = 0; i < ServiceAFweb.neuralNetTrainStock.length; i++) {
            String stockN = ServiceAFweb.neuralNetTrainStock[i];
            if (stockN.equals(symbol)) {
                trainStock = true;
                break;
            }
        }
        if (trainStock == false) {
            if (ServiceAFweb.initTrainNeuralNetNumber > 1) {
                return null;
            }
        }
        symbol = symbol.replace(".", "_");

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        if (offset == 0) {
            size1yearAll = size1yearAll / 2;
        }

        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        String nnName = ConstantKey.TR_NN1;
        if (tr == ConstantKey.INT_TR_NN1) {
            nnName = ConstantKey.TR_NN1;
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN1 nn1 = new ProcessNN1();
            inputList = nn1.trainingNN1dataMACD1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            nnName = ConstantKey.TR_NN2;
            ProcessNN2 nn2 = new ProcessNN2();
            inputList = nn2.trainingNN2dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        }

        String BPname = CKey.NN_version + "_" + nnName;
        boolean forceNN2flag = true;
        if (forceNN2flag) {
            BPname = CKey.NN_version + "_" + ConstantKey.TR_NN1;
        }
        // ignor first and last
        int len = inputList.size();
        if (len <= 2) {
            return null;
        }
        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();
        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
        if (stock == null) {
            return inputList;
        }
        int stockId = stock.getId();

        ArrayList writeArray = new ArrayList();
        String stTitle = "";
        int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use        
        for (int i = 0; i < inputList.size(); i++) {
            NNInputDataObj objData = inputList.get(i);
            NNInputOutObj obj = objData.getObj();

            String st = "\"" + stockId + "\",\"" + objData.getUpdatedatel() + "\",\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
                    + "\",\"" + obj.getOutput1()
                    + "\",\"" + obj.getOutput2()
                    + "\",\"" + obj.getInput1()
                    + "\",\"" + obj.getInput2()
                    + "\",\"" + obj.getInput3()
                    + "\",\"" + obj.getInput4()
                    + "\",\"" + obj.getInput5()
                    + "\",\"" + obj.getInput6()
                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
                    // + "\",\"" + obj.getInput11() + "\",\"" + obj.getInput12()
                    + "\"";

            if (i == 0) {
                st += ",\"last\"";
            }

            if (i + 1 >= inputList.size()) {
                st += ",\"first\"";
            }

            if (i == 0) {
                stTitle = "\"" + "stockId" + "\",\"" + "Updatedatel" + "\",\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
                        + "\",\"" + "output1"
                        + "\",\"" + "output2"
                        + "\",\"" + "macd TSig"
                        + "\",\"" + "LTerm"
                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                        + "\",\"" + symbol + "\"";

            }
            String stDispaly = st.replaceAll("\"", "");
            writeArray.add(stDispaly);
        }
        writeArray.add(stTitle.replaceAll("\"", ""));

        Collections.reverse(writeArray);
        Collections.reverse(inputList);

        if (getEnv.checkLocalPC() == true) {
            String nn12 = "_nn1_";
            if (tr == ConstantKey.INT_TR_NN2) {
                nn12 = "_nn2_";
            }
            String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + ServiceAFweb.initTrainNeuralNetNumber + ".csv";

            FileUtil.FileWriteTextArray(filename, writeArray);
//            ServiceAFweb.writeArrayNeuralNet.addAll(writeArray);

        }
        inputList.remove(len - 1);
        inputList.remove(0);

        //////// do not save in DB, only files
        //////// do not save in DB, only files
        //////// do not save in DB, only files
        boolean inputSaveFlag = false;
        if (inputSaveFlag == true) {
            int totalAdd = 0;
            int totalDup = 0;
            for (int i = 0; i < inputList.size(); i++) {
                NNInputDataObj objData = inputList.get(i);
                ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPname, stockId, objData.getUpdatedatel());
                if ((objList == null) || (objList.size() == 0)) {
                    serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPname, stockId, objData);
                    totalAdd++;
                    continue;
                }
                totalDup++;
                boolean flag = false;
                if (flag == true) {
                    if (CKey.NN_DEBUG == true) {
                        logger.info("> getTrainingNNdataProcess duplicate " + BPname + " " + symbol + " " + objData.getObj().getDateSt());
                    }
                }
            }
            logger.info("> getTrainingNNdataProcess " + BPname + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
        }
        return inputList;
    }

    private void NeuralNetProcessTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        ///////////////////////////////////////////////////////////////////////////////////
        // read new NN data
        serviceAFWeb.forceNNReadFileflag = true; // should be true to get it from file instead from db
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        boolean initTrainNeuralNet = true;
        if (initTrainNeuralNet == true) {

            double errorNN = CKey.NN1_ERROR_THRESHOLD;
            String nnName = ConstantKey.TR_NN1;
//            if (TR_Name == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//                errorNN = CKey.NN2_ERROR_THRESHOLD;
//            }
            String BPname = CKey.NN_version + "_" + nnName;
            // Not need to do neural net for NN2. Same NN weight for NN1 and NN2
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                return;
            }

            boolean flagInit = true;
            if (flagInit == true) {
                AFneuralNet afNeuralNet = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);
                ////just for teting
//                afNeuralNet = null;
                ////just for teting                
                if (afNeuralNet == null) {
                    afNeuralNet = new AFneuralNet();
                    afNeuralNet.setName(BPname);
                    afNeuralNet.setStatus(ConstantKey.OPEN);
                    afNeuralNet.setType(0);
                    Calendar dateDefault = TimeConvertion.getDefaultCalendar();
                    afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
                    afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
                    String weightSt = (CKey.NN1_WEIGHT_0);
                    afNeuralNet.setWeight(weightSt);
                    serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using NN1_WEIGHT_0");
                } else {
                    String weightSt = afNeuralNet.getWeight();
                    if ((weightSt == null) || (weightSt.length() == 0)) {
                        AFneuralNet afNeuralNet0 = serviceAFWeb.getNeuralNetObjWeight0(BPname, 0);
                        if (afNeuralNet0 != null) {
                            weightSt = afNeuralNet0.getWeight();
                            afNeuralNet.setName(BPname);
                            afNeuralNet.setStatus(ConstantKey.OPEN);
                            afNeuralNet.setType(0);
                            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
                            afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
                            afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
                            afNeuralNet.setWeight(weightSt);
                            serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
                        }
                    }
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using DB");
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                if (TR_Name == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN1, nnName, errorNN);
//                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
//                    retflag = TRprocessImp.TRtrainingNN2NeuralNetData(serviceAFWeb, nnName, errorNN);
                }
                if (retflag == 1) {
                    break;
                }
                logger.info(">>> initTrainNeuralNet " + i);
            }
        }

    }

    private boolean NeuralNetCreatJava(ServiceAFweb serviceAFWeb, String nnName) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, nnName, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

//TR_NN1_nnWeight0.txt
            String fileN = ServiceAFweb.FileLocalDebugPath + "TR_NN1_nnWeight0.txt";
            if (FileUtil.FileTest(fileN) == false) {
                return false;
            }
            StringBuffer msg1 = FileUtil.FileReadText(fileN);
            String weightSt = msg1.toString();
            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class nnData {\n"
                    + "\n"
                    + "    public static String NN1_WEIGHT_0 = \"\"\n");
            int sizeline = 1000;
            int len = weightSt.length();
            int beg = 0;
            int end = sizeline;
            while (true) {
                String st = weightSt.substring(beg, end);
                msgWrite.append("+ \"" + st + "\"\n");
                if (end >= len) {
                    break;
                }
                beg = end;
                if (end + sizeline <= len) {
                    end += sizeline;
                } else {
                    end = len;
                }
            }
            msgWrite.append(""
                    + "            + \"\";\n");

            len = inputListSt.length();
            beg = 0;
            end = sizeline;
            int index = 1;
            int line = 0;
            while (true) {
                if (line == 0) {
                    msgWrite.append(""
                            + "    public static String NN_INPUTLIST" + index + " = \"\"\n"
                            + "            + \"\"\n");
                }
                line++;
                String st = inputListSt.substring(beg, end);

                msgWrite.append("+ \"" + st + "\"\n");

                if (end >= len) {
                    msgWrite.append(""
                            + "            + \"\";\n");

                    break;
                }
                if (line == 20) {
                    msgWrite.append(""
                            + "            + \"\";\n");
                    line = 0;
                    index++;
                }
                beg = end;
                if (end + sizeline <= len) {
                    end += sizeline;
                } else {
                    end = len;
                }
            }

            msgWrite.append(""
                    + "}\n"
                    ///
                    + ""
            );
            fileN = ServiceAFweb.FileLocalDebugPath + "nnData.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

//////////
//////////////////////////////////////////////////
    private static ArrayList stockNNprocessNameArray = new ArrayList();
    private static ArrayList stockNNinputNameArray = new ArrayList();

    private ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        if (stockNNprocessNameArray != null && stockNNprocessNameArray.size() > 0) {
            return stockNNprocessNameArray;
        }

        boolean guestFlag = false;
        if (guestFlag == true) {
            AccountObj account = serviceAFWeb.getAccountImp().getAccountByType("GUEST", "guest", AccountObj.INT_TRADING_ACCOUNT);
            accountObj = account;
        }
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            ArrayList stockTRNameArray = new ArrayList();
            for (int i = 0; i < stockNameArray.size(); i++) {
                String sym = (String) stockNameArray.get(i);
                String symTR = sym + "#" + ConstantKey.INT_TR_NN1;
                stockTRNameArray.add(symTR);
//                boolean NN2flag = true;
//                if (NN2flag == true) {
//                    symTR = sym + "#" + ConstantKey.INT_TR_NN2;
//                    stockTRNameArray.add(symTR);
//                }
            }

            stockNNprocessNameArray = stockTRNameArray;
        }
        return stockNNprocessNameArray;
    }

    public void ProcessTrainNeuralNetBySign(ServiceAFweb serviceAFWeb) {
//        boolean flag =true;
//        if (flag == true) {
//            return;
//        }        
//        logger.info("> ProcessTrainNeuralNet ");
//        if (getEnv.checkLocalPC() != true) {
//            if (CKey.SERVERDB_URL.equals(CKey.URL_PATH_HERO) == true) {
//                ///Error R14 (Memory quota exceeded) in heroku
//                ///Error R14 (Memory quota exceeded) in heroku
//                if (ServiceAFweb.NN_AllowTraingStockFlag == false) {
//                    return;
//                }
//            }
//        }

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        UpdateStockNNprocessNameArray(serviceAFWeb, accountAdminObj);
        if (stockNNprocessNameArray == null) {
            return;
        }
        if (stockNNprocessNameArray.size() == 0) {
            return;
        }

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NN_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (CKey.NN_DEBUG == true) {
            testing = true;
        }
        if (testing == true) {
            lockReturn = 1;
        }

        logger.info("ProcessTrainNeuralNet " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(currentTime, 5);

//            for (int i = 0; i < 10; i++) {
            while (true) {
                if (CKey.NN_DEBUG != true) {
                    currentTime = System.currentTimeMillis();
                    if (lockDate1Min < currentTime) {
                        break;
                    }
                }

                if (stockNNprocessNameArray.size() == 0) {
                    break;
                }
                try {
                    String symbolTR = (String) stockNNprocessNameArray.get(0);
//                    stockNNprocessNameArray.remove(0);

                    String[] symbolArray = symbolTR.split("#");
                    if (symbolArray.length >= 0) {

                        String symbol = symbolArray[0];
                        int TR_NN = Integer.parseInt(symbolArray[1]);  // assume TR_NN1

                        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);

                        if (stock == null) {
                            stockNNprocessNameArray.remove(0);
                            continue;
                        }
                        if (stock.getAfstockInfo() == null) {
                            stockNNprocessNameArray.remove(0);
                            continue;
                        }

                        String LockStock = "NN_TR_" + symbol; // + "_" + trNN;
                        LockStock = LockStock.toUpperCase();

                        long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                        long lockReturnStock = 1;

//                        lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
//                        if (testing == true) {
//                            lockReturnStock = 1;
//                        }
//                        logger.info("ProcessTrainNeuralNet " + LockStock + " LockStock " + lockReturnStock);
                        if (lockReturnStock > 0) {
                            String nnName = ConstantKey.TR_NN1;
                            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
                            this.Process1TrainNeuralNet(serviceAFWeb, TR_NN, BPnameSym, symbol);

//                            serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
//                            logger.info("ProcessTrainNeuralNet " + LockStock + " unLock LockStock ");
                            AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
                            if (nnObj1 != null) {
                                if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
                                    stockNNprocessNameArray.remove(0);

                                    /// need to create the table to reduce the memeory in DB
//                                    serviceAFWeb.getStockImp().deleteNeuralNet1Table();
                                }
                            }
                        }

                    }
                } catch (Exception ex) {
                    logger.info("> ProcessTrainNeuralNet Exception" + ex.getMessage());
                }
            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
//        logger.info("> ProcessTrainNeuralNet ... done");
    }

    private void Process1TrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String BPnameSym, String symbol) {

        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        if (nnObj1 == null) {
            inputStockNeuralNetBySignal(serviceAFWeb, TR_NN, symbol);
            return;
        }
        if (nnObj1.getStatus() == ConstantKey.INITIAL) {
            inputStockNeuralNetBySignal(serviceAFWeb, TR_NN, symbol);
        }
        if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
            stockNNprocessNameArray.remove(0);
            return;
        }
        stockTrainNeuralNet(serviceAFWeb, TR_NN, symbol);
    }

    public int inputStockNeuralNetBySignal(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        boolean nnsym = true;
        if (nnsym == true) {
            int totalAdd = 0;
            int totalDup = 0;
            String nnName = ConstantKey.TR_NN1;
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
            }
            String nnNameSym = nnName + "_" + symbol;

            String BPnameSym = CKey.NN_version + "_" + nnNameSym;
            try {
                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
                String status = "NA";
                if (nnObj1 != null) {
                    if ((nnObj1.getStatus() == ConstantKey.OPEN) || (nnObj1.getStatus() == ConstantKey.COMPLETED)) {
                        return 0;
                    }
                    status = "" + nnObj1.getStatus();
                }

                logger.info("> inputStockNeuralNetData " + BPnameSym + " Status=" + status);
                NNBPservice nnTemp = new NNBPservice();

                String middlelayer = "";
                String version = "";
                if (TR_Name == ConstantKey.INT_TR_NN1) {
                    if (CKey.NN1_WEIGHT_0.length() == 0) {
                        return 0;
                    }
                    nnTemp.createNet(CKey.NN1_WEIGHT_0);
                    String weightSt = nnTemp.getNetObjSt();
                    String[] strNetArray = CKey.NN1_WEIGHT_0.split(";");
                    version = strNetArray[0];
                    middlelayer = strNetArray[4];
//                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
//                    if (CKey.NN2_WEIGHT_0.length() == 0) {
//                        return 0;
//                    }
//                    nnTemp.createNet(CKey.NN2_WEIGHT_0);
//                    String weightSt = nnTemp.getNetObjSt();
//                    String[] strNetArray = CKey.NN2_WEIGHT_0.split(";");
//                    version = strNetArray[0];
//                    middlelayer = strNetArray[4];
                }
                ArrayList<NNInputOutObj> inputlist = new ArrayList();

                TradingNNprocess trainNN = new TradingNNprocess();
//                ArrayList<NNInputDataObj> inputlistSym = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, TR_Name, 0);
                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
                ArrayList<NNInputDataObj> inputlistSym1 = new ArrayList();
                ArrayList<NNInputDataObj> inputlistSym2 = new ArrayList();

                /// just for testing
                inputlistSym1 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1, 0);
                inputlistSym2 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, 0);

                inputlistSym.addAll(inputlistSym1);
                inputlistSym.addAll(inputlistSym2);

                ArrayList<NNInputDataObj> inputL = new ArrayList();
                boolean trainInFile = true;
                if (trainInFile == true) {
                    inputL = NeuralNetGetNN1InputfromStaticCode(symbol);
                    if (inputL != null) {
                        if (inputL.size() > 0) {
                            logger.info("> inputStockNeuralNetData " + BPnameSym + " " + symbol + " " + inputL.size());
                            for (int k = 0; k < inputL.size(); k++) {
                                NNInputDataObj inputLObj = inputL.get(k);
                                for (int m = 0; m < inputlistSym.size(); m++) {
                                    NNInputDataObj inputSymObj = inputlistSym.get(m);
                                    float output1 = (float) inputSymObj.getObj().getOutput1();
                                    if ((output1 == 0) || (output1 == -1)) {
                                        inputlistSym.remove(m);
                                        break;
                                    }
                                    String inputLObD = inputLObj.getObj().getDateSt();
                                    String inputSymObD = inputSymObj.getObj().getDateSt();
                                    if (inputLObD.equals(inputSymObD)) {
                                        inputlistSym.remove(m);
//                                        logger.info("> inputStockNeuralNetData " + BPnameSym + " " + symbol + " " + inputLObj.getUpdatedatel());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (inputlistSym != null) {
                    //merge inputlistSym

                    for (int i = 0; i < inputlistSym.size(); i++) {
                        NNInputOutObj inputObj = inputlistSym.get(i).getObj();
                        inputlist.add(inputObj);
                        // save into db
                        // save into db
                        // save into db
                        // save into db
                        NNInputDataObj objData = inputlistSym.get(i);
                        ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
                        if ((objList == null) || (objList.size() == 0)) {
                            serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
                            totalAdd++;
                            continue;
                        }
                        totalDup++;
                        boolean flag = false;
                        if (flag == true) {
                            if (CKey.NN_DEBUG == true) {
                                logger.info("> inputStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
                            }
                        }
                    }
                }

                AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPnameSym, 0);
                if (nnObj0 != null) {
                    String stWeight0 = nnObj0.getWeight();

                    if (stWeight0.length() > 0) {

                        String[] strNetArraySym = stWeight0.split(";");
                        String versionSym = strNetArraySym[0];
                        String middlelayerSym = strNetArraySym[4];
                        // reset to use TR Weight 0  if middel layer is different
                        // reset to use TR Weight 0  if middel layer is different 
                        // just for testing
//                        versionSym = "";
                        // just for testing
                        if (middlelayer.equals(middlelayerSym) && version.equals(versionSym)) {
                            logger.info("> inputStockNeuralNetData create existing Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
                            //just for testing                           
                            nnTemp.createNet(stWeight0);
                        } else {
                            logger.info("> inputStockNeuralNetData create Static Base NN1_WEIGHT " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
                        }
                    }
                }

                String weightSt = nnTemp.getNetObjSt();
//                
                String refname = CKey.NN_version + "_" + ConstantKey.TR_NN1;
                int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPnameSym, refname, weightSt);
//                logger.info("> inputStockNeuralNet " + BPnameSym + " inputlist=" + inputlist.size() + " ...Done");
                return ret;

            } catch (Exception e) {
                logger.info("> inputStockNeuralNet exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;

    }

    public int stockTrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String symbol) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> processStockNeuralNet " + TR_Name + " " + symbol);

        boolean nnsymTrain = true;
        if (nnsymTrain == true) {
            String nnName = ConstantKey.TR_NN1;
            double errorNN = CKey.NN1_ERROR_THRESHOLD;
//            if (TR_NN == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//                errorNN = CKey.NN2_ERROR_THRESHOLD;
//            }
            String nnNameSym = nnName + "_" + symbol;
            String BPname = CKey.NN_version + "_" + nnNameSym;
            try {

                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);
                if (nnObj1 != null) {
                    if (nnObj1.getStatus() != ConstantKey.OPEN) {
                        return -1;
                    }
                }
                int retflag = 0;
                if (TR_NN == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN1, nnNameSym, errorNN);
//                } else if (TR_NN == ConstantKey.INT_TR_NN2) {
//                    retflag = TRprocessImp.TRtrainingNN2NeuralNetData(serviceAFWeb, nnNameSym, errorNN);
                }
//                logger.info("> processStockNeuralNet ... Done");
                return retflag;
            } catch (Exception e) {
                logger.info("> stockTrainNeuralNet exception " + BPname + " - " + e.getMessage());
            }
        }
        return -1;
    }

    public static ArrayList<NNInputDataObj> NeuralNetGetNN1InputfromStaticCode(String symbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nnData.NN_INPUTLIST1);
            inputBuf.append(nnData.NN_INPUTLIST2);
            inputBuf.append(nnData.NN_INPUTLIST3);
            inputBuf.append(nnData.NN_INPUTLIST4);
            inputBuf.append(nnData.NN_INPUTLIST5);
            inputBuf.append(nnData.NN_INPUTLIST6);
            inputBuf.append(nnData.NN_INPUTLIST7);
            inputBuf.append(nnData.NN_INPUTLIST8);
            inputBuf.append(nnData.NN_INPUTLIST9); //need to check nnData file
//            inputBuf.append(nnData.NN_INPUTLIST10);

            String inputListSt = ServiceAFweb.decompress(inputBuf.toString());
            HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
            stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);
            if (symbol != "") {
                inputlist = stockInputMap.get(symbol);
                if (inputlist == null) {
                    return null;
                }
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
                inputlist = new ArrayList<NNInputDataObj>(listItem);
                return inputlist;
            }

            for (String sym : stockInputMap.keySet()) {
                ArrayList<NNInputDataObj> inputL = stockInputMap.get(sym);
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputL);
                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
                inputL = new ArrayList<NNInputDataObj>(listItem);
                inputlist.addAll(inputL);
            }

            return inputlist;
        } catch (Exception ex) {
            logger.info("> NeuralNetGetNN1InputfromStaticCode - exception " + ex);
        }
        return null;
    }

}
