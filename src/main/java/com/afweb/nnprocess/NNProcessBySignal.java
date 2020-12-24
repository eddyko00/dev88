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
import com.afweb.stock.StockInternet;
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
    public static boolean flagNNLearningSignal = false;
    public static boolean flagNN3LearningTrend = false;
    public static boolean flagNNReLearning = false;
    public static boolean processNNSignalAdmin = false;
    public static boolean processRestinputflag = false;
    public static boolean processRestAllStockflag = false;

    public static int cntNN = 0;

    public void mainProcessNeuralNet(ServiceAFweb serviceAFWeb) {
        cntNN++;
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NNProcessByTrend nntrend = new NNProcessByTrend();
        NNProcessBySignal nnProcBySig = new NNProcessBySignal();
        if (cntNN == 1) {
            if (flagNNLearningSignal == true) {
                nnProcBySig.ProcessTrainNeuralNetBySign(serviceAFWeb);
            }
            return;
        } else if (cntNN == 2) {
            if (flagNN3LearningTrend == true) {
                nntrend.ProcessTrainNeuralNetByTrend(serviceAFWeb);
            }
            return;
        } else if (cntNN == 3) {
            if (flagNNReLearning == true) {
                NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);
            }
            return;
        }
        cntNN = 0;
    }

    public void processNeuralNetTrain(ServiceAFweb serviceAFWeb) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NNProcessByTrend nntrend = new NNProcessByTrend();
        TrandingSignalProcess.forceToGenerateNewNN = false;
        int k = 0;

        while (true) {
            k++;
            boolean exitflag = true;
            if (flagNNLearningSignal == true) {
                exitflag = false;
                if (((k % 5) == 0) || (k == 0)) {
                    NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN1);
                }
                logger.info("> ProcessTrainNeuralNet NN 1 cycle " + k);
                ProcessTrainNeuralNetBySign(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet NN 1 end... cycle " + k);
            }

////////////////////////////////////////////////////////////////////////////
            if (flagNN3LearningTrend == true) {
                exitflag = false;
                if (((k % 5) == 0) || (k == 0)) {
                    NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN3);
                }
                logger.info("> ProcessTrainNeuralNet NN 3 cycle " + k);
                nntrend.ProcessTrainNeuralNetByTrend(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet NN 3 end... cycle " + k);
            }

////////////////////////////////////////////////////////////////////////////
            if (flagNNReLearning == true) {
                exitflag = false;
                logger.info("> ProcessReLeanInput NN 1 cycle " + k);
                NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);
                logger.info("> ProcessReLeanInput end... cycle " + k);

            }
////////////////////////////////////////////////////////////////////////////
            if (processNNSignalAdmin == true) {
                exitflag = false;
                logger.info("> processNNSignalAdmin  cycle " + k);
                TRprocessImp.ProcessAdminSignalTrading(serviceAFWeb);
                serviceAFWeb.getAccountProcessImp().ProcessAllAccountTradingSignal(serviceAFWeb);
                TRprocessImp.UpdateAllStock(serviceAFWeb);
                logger.info("> processNNSignalAdmin end... cycle " + k);
            }
////////////////////////////////////////////////////////////////////////////

            if (processRestinputflag == true) {
                exitflag = false;
                /// reset weight0 and use latest stock
                /// remember to update nnData and nn3Data and version                
                processInputNeuralNet(serviceAFWeb);
                nntrend.processInputNeuralNetTrend(serviceAFWeb);
                ///////////////////////////////
                processAllStockInputNeuralNet(serviceAFWeb);
                nntrend.processAllStockInputNeuralNetTrend(serviceAFWeb);
                return;
            }

            if (processRestAllStockflag == true) {
                exitflag = false;
                ///////////////////////////////   
                String symbolL[] = ServiceAFweb.primaryStock;
                AllStockHistoryCreatJava(serviceAFWeb, symbolL, "nnAllStock", "NN_ALLSTOCK");

                String symbolLallSt[] = ServiceAFweb.allStock;
                AllStockHistoryCreatJava(serviceAFWeb, symbolLallSt, "nn1AllStock", "NN_1ALLSTOCK");

                return;
            }
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
            if (exitflag == true) {
                break;
            }
            logger.info("> Waiting 30 sec........");
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }
///////////////////////////////

    public void processStockInput(ServiceAFweb serviceAFWeb) {

    }

    public void processInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////
        boolean flagIntitNN1Input = true;
        if (flagIntitNN1Input == true) {

            TrandingSignalProcess.forceToInitleaningNewNN = true;  // must be true all for init learning             
            TrandingSignalProcess.forceToGenerateNewNN = false;
            logger.info("> processInputNeuralNet TR NN1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            logger.info("> processInputNeuralNet TR NN2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);
            // need to debug to generate the java first time
            TrandingSignalProcess.forceToGenerateNewNN = true;

            TrandingSignalProcess.forceToErrorNewNN = true;
            // start training
            // TrainingNNBP inputpattern 1748
            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);

            TrandingSignalProcess.forceToGenerateNewNN = false;
            // start training
            // TrainingNNBP inputpattern 1748
            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
            logger.info("> processInputNeuralNet TR NN1 end....... ");

        }
    }

    public void processAllStockInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////

        logger.info("> processAllStockInputNeuralNet TR NN1... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
        logger.info("> processAllStockInputNeuralNet TR NN2... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);
        NeuralNetAllStockCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
        logger.info("> processAllStockInputNeuralNet TR NN1 end....... ");

        ////////////////////////////////////////////
    }

    public void processNeuralNet(ServiceAFweb serviceAFWeb) {

        ////////////////////////////////////////////
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
//////////////////////////////////////////////////
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
            nn2.updateAdminTradingsignalnn2(serviceAFWeb, accObj, symbol, trObj, StockArray, offset, stock, tradingRuleList);

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

    }

    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////
//    public void ProcessReLeanInput(ServiceAFweb serviceAFWeb) {
//        boolean flagReLeanInput = true;
//        if (flagReLeanInput == true) {
//            String LockName = null;
//            Calendar dateNow = TimeConvertion.getCurrentCalendar();
//            long lockDateValue = dateNow.getTimeInMillis();
//
//            LockName = "ReLean_NN";
//            long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
//            boolean testing = false;
//            if (testing == true) {
//                lockReturn = 1;
//            }
//            logger.info("ProcessReLeanInput " + LockName + " LockName " + lockReturn);
//            if (lockReturn > 0) {
//                long LastServUpdateTimer = System.currentTimeMillis();
//                long lockDate10Min = TimeConvertion.addMinutes(LastServUpdateTimer, 15); // add 3 minutes
//
////            for (int i = 0; i < 10; i++) {
//                while (true) {
//                    long currentTime = System.currentTimeMillis();
//                    if (testing == true) {
//                        currentTime = 0;
//                    }
//                    if (lockDate10Min < currentTime) {
//                        logger.info("ProcessReLeanInput exit after 15 minutes");
//                        break;
//                    }
//                    ProcessReLeanInput1(serviceAFWeb);
//                }
//                serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//                logger.info("ProcessReLeanInput " + LockName + " unlock LockName");
//
//            }
//
//        }
//
//    }
//
//    private void ProcessReLeanInput1(ServiceAFweb serviceAFWeb) {
//        try {
//            boolean flagReLeanInput = true;
//            if (flagReLeanInput == true) {
//                TradingNNprocess trainNN = new TradingNNprocess();
//                int custId = 0;;
//                String Name = TradingNNprocess.cfg_stockNNretrainNameArray;
//                ArrayList stockNameArray = null;
//
//                //// just for testing, always clear reset
//                boolean resetflag = true;
//                //// just for testing, always clear reset
//                if (resetflag == false) {
//
//                    ArrayList<CommObj> commObjArry = serviceAFWeb.getAccountImp().getComObjByCustName(custId, Name);
//
//                    if (commObjArry != null) {
//                        if (commObjArry.size() > 0) {
//                            CommObj commObj = commObjArry.get(0);
//                            String stockNameArraySt = commObj.getData();
//                            try {
//                                stockNameArraySt = StringTag.replaceAll("^", "\"", stockNameArraySt);
//                                stockNameArray = new ObjectMapper().readValue(stockNameArraySt, ArrayList.class);
//                            } catch (Exception ex) {
//                            }
//                        }
//                    } else {
//                        ArrayList stArray = new ArrayList();
//                        String msg = "";
//                        try {
//                            msg = new ObjectMapper().writeValueAsString(stArray);
//                        } catch (JsonProcessingException ex) {
//                        }
//                        msg = StringTag.replaceAll("\"", "^", msg);
//
//                        serviceAFWeb.getAccountImp().addCommByCustName(custId, Name, msg);
//                    }
//                }
//
//                if ((stockNameArray == null) || (stockNameArray.size() == 0)) {
//
//                    stockNameArray = trainNN.reLearnInputStockNNprocessNameArray(serviceAFWeb);
//                }
//                trainNN.setStockNNretrainNameArray(stockNameArray);
//
//                TradingNNprocess NNProcessImp = new TradingNNprocess();
//                NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);
//            }
//        } catch (Exception ex) {
//
//        }
//    }
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

    private void NeuralNetAllStockInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        int sizeYr = 2;
        for (int j = 0; j < sizeYr; j++) { //4; j++) {
            int size = 20 * CKey.MONTH_SIZE * j;
//                writeArrayNeuralNet.clear();
            serviceAFWeb.initTrainNeuralNetNumber = j + 1;
            logger.info("> initTrainNeuralNetNumber " + serviceAFWeb.initTrainNeuralNetNumber);
            String symbol = "";
            String symbolL[] = ServiceAFweb.allStock;
            for (int i = 0; i < symbolL.length; i++) {
                symbol = symbolL[i];
                ArrayList<NNInputDataObj> InputList = getTrainingNNdataProcess(serviceAFWeb, symbol, TR_Name, size);
            }
        }
    }

    public ArrayList<NNInputDataObj> getTrainingNNdataProcess(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
        logger.info("> getTrainingNNdataProcess tr_" + tr + " " + symbol);

//        boolean trainStock = false;
//        for (int i = 0; i < ServiceAFweb.neuralNetTrainStock.length; i++) {
//            String stockN = ServiceAFweb.neuralNetTrainStock[i];
//            if (stockN.equals(symbol)) {
//                trainStock = true;
//                break;
//            }
//        }
//        if (trainStock == false) {
//            if (ServiceAFweb.initTrainNeuralNetNumber > 1) {
//                return null;
//            }
//        }
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
            inputList = nn1.trainingNN1dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
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
//                boolean flag = false;
//                if (flag == true) {
//                    if (CKey.NN_DEBUG == true) {
//                        logger.info("> getTrainingNNdataProcess duplicate " + BPname + " " + symbol + " " + objData.getObj().getDateSt());
//                    }
//                }
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

//                    String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;
//                    serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
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

//                            String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;                           
//                            serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
                            serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
                        }
                    }
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using DB");
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                if (TR_Name == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN1, nnName, "", errorNN);
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

    public boolean AllStockHistoryCreatJava(ServiceAFweb serviceAFWeb, String symbolL[], String fileName, String tagName) {
        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {

            AllStockHistoryCreatJavaProcess(serviceAFWeb, symbolL, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class " + fileName + " {\n"
                    + "\n");

            int sizeline = 1000;
            int len = inputListSt.length();
            int beg = 0;
            int end = sizeline;
            int index = 1;
            int line = 0;
            while (true) {
                if (line == 0) {
                    msgWrite.append(""
                            + "    public static String " + tagName + index + " = \"\"\n"
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
            String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public void AllStockHistoryCreatJavaProcess(ServiceAFweb serviceAFWeb, String symbolL[], HashMap<String, ArrayList> stockInputMap) {
        boolean saveStockDBFlag = true;
        if (saveStockDBFlag == true) {

            StockInternet internet = new StockInternet();
            ArrayList stockNameArray = new ArrayList();

            if (symbolL == null) {
                return;
            }

            for (int i = 0; i < symbolL.length; i++) {
                stockNameArray.add(symbolL[i]);
            }
            logger.info("updateRealTimeStock " + stockNameArray.size());

            int sizeyear = 5 * 52 * 5;
            for (int k = 0; k < stockNameArray.size(); k++) {
                String symbol = (String) stockNameArray.get(k);

                String StFileName = ServiceAFweb.FileLocalPath + symbol + ".txt";

                ArrayList<String> writeArray = new ArrayList();
                ArrayList<AFstockInfo> StockArray = null;

                try {
                    // always the earliest day first  
                    StockArray = internet.GetStockHistoricalInternet(symbol, sizeyear);
                } catch (Exception ex) {

                }
                if (StockArray == null) {
                    continue;
                }
                if (StockArray.size() < 3) {
                    continue;
                }
                // skiping first 3 days (last days is not final
                for (int j = 3; j < StockArray.size(); j++) {
                    try {
                        AFstockInfo obj = StockArray.get(j);
                        String st = new ObjectMapper().writeValueAsString(obj);
                        writeArray.add(st);
                    } catch (JsonProcessingException ex) {
                        writeArray = null;
                        break;
                    }
                }
                if (writeArray == null) {
                    continue;
                }
                FileUtil.FileWriteTextArray(StFileName, writeArray);
                ///////////////////////
                FileUtil.FileReadTextArray(StFileName, writeArray);
                if (writeArray.size() == 0) {
                    continue;
                }
                StockArray = new ArrayList();
                for (int j = 0; j < writeArray.size(); j++) {
                    String st = writeArray.get(j);
                    try {
                        AFstockInfo stockInfo = new ObjectMapper().readValue(st, AFstockInfo.class);
                        StockArray.add(stockInfo);
                    } catch (IOException ex) {
                    }
                }
                //////////
                if (StockArray == null) {
                    continue;
                }
                logger.info(">>> AllStockHistoryCreatJava " + symbol + " " + StockArray.size());
                stockInputMap.put(symbol, StockArray);

            } // loop for stockNameArray
        }
    }

    public static HashMap<String, ArrayList> stockInputMap = null;

    public static boolean AllStockHistoryGetfromStaticCodeInit() {

        if (stockInputMap != null) {
            return true;
        }

        StringBuffer inputBuf = new StringBuffer();
        try {
            inputBuf.append(nnAllStock.NN_ALLSTOCK1);
            inputBuf.append(nnAllStock.NN_ALLSTOCK2);
            inputBuf.append(nnAllStock.NN_ALLSTOCK3);
            inputBuf.append(nnAllStock.NN_ALLSTOCK4);
            inputBuf.append(nnAllStock.NN_ALLSTOCK5);
            inputBuf.append(nnAllStock.NN_ALLSTOCK6);
            inputBuf.append(nnAllStock.NN_ALLSTOCK7);
            inputBuf.append(nnAllStock.NN_ALLSTOCK8);
            inputBuf.append(nnAllStock.NN_ALLSTOCK9);

            inputBuf.append(nnAllStock.NN_ALLSTOCK10);
            inputBuf.append(nnAllStock.NN_ALLSTOCK11);
            inputBuf.append(nnAllStock.NN_ALLSTOCK12);
            inputBuf.append(nnAllStock.NN_ALLSTOCK13);
            inputBuf.append(nnAllStock.NN_ALLSTOCK14);
            inputBuf.append(nnAllStock.NN_ALLSTOCK15);
            inputBuf.append(nnAllStock.NN_ALLSTOCK16);
            inputBuf.append(nnAllStock.NN_ALLSTOCK17);
            inputBuf.append(nnAllStock.NN_ALLSTOCK18);
            inputBuf.append(nnAllStock.NN_ALLSTOCK19);

            inputBuf.append(nnAllStock.NN_ALLSTOCK20);
            inputBuf.append(nnAllStock.NN_ALLSTOCK21);
            inputBuf.append(nnAllStock.NN_ALLSTOCK22);
            inputBuf.append(nnAllStock.NN_ALLSTOCK23);
            inputBuf.append(nnAllStock.NN_ALLSTOCK24);
            inputBuf.append(nnAllStock.NN_ALLSTOCK25);
            inputBuf.append(nnAllStock.NN_ALLSTOCK26);
            inputBuf.append(nnAllStock.NN_ALLSTOCK27);
            inputBuf.append(nnAllStock.NN_ALLSTOCK28);
            inputBuf.append(nnAllStock.NN_ALLSTOCK29);

            inputBuf.append(nnAllStock.NN_ALLSTOCK30);
            inputBuf.append(nnAllStock.NN_ALLSTOCK31);
            inputBuf.append(nnAllStock.NN_ALLSTOCK32);
            inputBuf.append(nnAllStock.NN_ALLSTOCK33);
            inputBuf.append(nnAllStock.NN_ALLSTOCK34);
            inputBuf.append(nnAllStock.NN_ALLSTOCK35);
            inputBuf.append(nnAllStock.NN_ALLSTOCK36);
            inputBuf.append(nnAllStock.NN_ALLSTOCK37);
            inputBuf.append(nnAllStock.NN_ALLSTOCK38);
            inputBuf.append(nnAllStock.NN_ALLSTOCK39);

            inputBuf.append(nnAllStock.NN_ALLSTOCK40);
            inputBuf.append(nnAllStock.NN_ALLSTOCK41);
            inputBuf.append(nnAllStock.NN_ALLSTOCK42);
            inputBuf.append(nnAllStock.NN_ALLSTOCK43);
            inputBuf.append(nnAllStock.NN_ALLSTOCK44);
            inputBuf.append(nnAllStock.NN_ALLSTOCK45);
            inputBuf.append(nnAllStock.NN_ALLSTOCK46);
            inputBuf.append(nnAllStock.NN_ALLSTOCK47);
            inputBuf.append(nnAllStock.NN_ALLSTOCK48);
            inputBuf.append(nnAllStock.NN_ALLSTOCK49);

            inputBuf.append(nnAllStock.NN_ALLSTOCK50);
            inputBuf.append(nnAllStock.NN_ALLSTOCK51);
            inputBuf.append(nnAllStock.NN_ALLSTOCK52);
            inputBuf.append(nnAllStock.NN_ALLSTOCK53);
            inputBuf.append(nnAllStock.NN_ALLSTOCK54);
            inputBuf.append(nnAllStock.NN_ALLSTOCK55);
            inputBuf.append(nnAllStock.NN_ALLSTOCK56);
            inputBuf.append(nnAllStock.NN_ALLSTOCK57);
            inputBuf.append(nnAllStock.NN_ALLSTOCK58);
            inputBuf.append(nnAllStock.NN_ALLSTOCK59);

            inputBuf.append(nnAllStock.NN_ALLSTOCK60);
            inputBuf.append(nnAllStock.NN_ALLSTOCK61);
            inputBuf.append(nnAllStock.NN_ALLSTOCK62);
            inputBuf.append(nnAllStock.NN_ALLSTOCK63);
            inputBuf.append(nnAllStock.NN_ALLSTOCK64);
            inputBuf.append(nnAllStock.NN_ALLSTOCK65);
            inputBuf.append(nnAllStock.NN_ALLSTOCK66);
            inputBuf.append(nnAllStock.NN_ALLSTOCK67);
            inputBuf.append(nnAllStock.NN_ALLSTOCK68);
            inputBuf.append(nnAllStock.NN_ALLSTOCK69);

            inputBuf.append(nnAllStock.NN_ALLSTOCK70);
            inputBuf.append(nnAllStock.NN_ALLSTOCK71);
            inputBuf.append(nnAllStock.NN_ALLSTOCK72);
            inputBuf.append(nnAllStock.NN_ALLSTOCK73);
            inputBuf.append(nnAllStock.NN_ALLSTOCK74);
            inputBuf.append(nnAllStock.NN_ALLSTOCK75);
            inputBuf.append(nnAllStock.NN_ALLSTOCK76);
            inputBuf.append(nnAllStock.NN_ALLSTOCK77);
            inputBuf.append(nnAllStock.NN_ALLSTOCK78);
            inputBuf.append(nnAllStock.NN_ALLSTOCK79);

            inputBuf.append(nnAllStock.NN_ALLSTOCK80);
            inputBuf.append(nnAllStock.NN_ALLSTOCK81);
            inputBuf.append(nnAllStock.NN_ALLSTOCK82);
            inputBuf.append(nnAllStock.NN_ALLSTOCK83);
            inputBuf.append(nnAllStock.NN_ALLSTOCK84);
            inputBuf.append(nnAllStock.NN_ALLSTOCK85);
            inputBuf.append(nnAllStock.NN_ALLSTOCK86);
            inputBuf.append(nnAllStock.NN_ALLSTOCK87);
            inputBuf.append(nnAllStock.NN_ALLSTOCK88);
            inputBuf.append(nnAllStock.NN_ALLSTOCK89);

            inputBuf.append(nnAllStock.NN_ALLSTOCK90);
            inputBuf.append(nnAllStock.NN_ALLSTOCK91);
            inputBuf.append(nnAllStock.NN_ALLSTOCK92);
            inputBuf.append(nnAllStock.NN_ALLSTOCK93);
            inputBuf.append(nnAllStock.NN_ALLSTOCK94);
            inputBuf.append(nnAllStock.NN_ALLSTOCK95);
            inputBuf.append(nnAllStock.NN_ALLSTOCK96);
            inputBuf.append(nnAllStock.NN_ALLSTOCK97);
            inputBuf.append(nnAllStock.NN_ALLSTOCK98);
            inputBuf.append(nnAllStock.NN_ALLSTOCK99);

            inputBuf.append(nnAllStock.NN_ALLSTOCK100);
            inputBuf.append(nnAllStock.NN_ALLSTOCK101);
            inputBuf.append(nnAllStock.NN_ALLSTOCK102);
            inputBuf.append(nnAllStock.NN_ALLSTOCK103);
            inputBuf.append(nnAllStock.NN_ALLSTOCK104);
            inputBuf.append(nnAllStock.NN_ALLSTOCK105);
            inputBuf.append(nnAllStock.NN_ALLSTOCK106);
            inputBuf.append(nnAllStock.NN_ALLSTOCK107);
            inputBuf.append(nnAllStock.NN_ALLSTOCK108);
            inputBuf.append(nnAllStock.NN_ALLSTOCK109);

            inputBuf.append(nnAllStock.NN_ALLSTOCK110);
            inputBuf.append(nnAllStock.NN_ALLSTOCK111);
            inputBuf.append(nnAllStock.NN_ALLSTOCK112);
            inputBuf.append(nnAllStock.NN_ALLSTOCK113);
            inputBuf.append(nnAllStock.NN_ALLSTOCK114);
            inputBuf.append(nnAllStock.NN_ALLSTOCK115);
            inputBuf.append(nnAllStock.NN_ALLSTOCK116);
            inputBuf.append(nnAllStock.NN_ALLSTOCK117);
            inputBuf.append(nnAllStock.NN_ALLSTOCK118);
            inputBuf.append(nnAllStock.NN_ALLSTOCK119);

            inputBuf.append(nnAllStock.NN_ALLSTOCK120);
            inputBuf.append(nnAllStock.NN_ALLSTOCK121);
            inputBuf.append(nnAllStock.NN_ALLSTOCK122);
            inputBuf.append(nnAllStock.NN_ALLSTOCK123);
            inputBuf.append(nnAllStock.NN_ALLSTOCK124);
            inputBuf.append(nnAllStock.NN_ALLSTOCK125);
            inputBuf.append(nnAllStock.NN_ALLSTOCK126);
            inputBuf.append(nnAllStock.NN_ALLSTOCK127);
            inputBuf.append(nnAllStock.NN_ALLSTOCK128);
            inputBuf.append(nnAllStock.NN_ALLSTOCK129);

            inputBuf.append(nnAllStock.NN_ALLSTOCK130);
            inputBuf.append(nnAllStock.NN_ALLSTOCK131);
            inputBuf.append(nnAllStock.NN_ALLSTOCK132);
            inputBuf.append(nnAllStock.NN_ALLSTOCK133);
            inputBuf.append(nnAllStock.NN_ALLSTOCK134);
            inputBuf.append(nnAllStock.NN_ALLSTOCK135);
            inputBuf.append(nnAllStock.NN_ALLSTOCK136);
            inputBuf.append(nnAllStock.NN_ALLSTOCK137);
            inputBuf.append(nnAllStock.NN_ALLSTOCK138);
            inputBuf.append(nnAllStock.NN_ALLSTOCK139);

            inputBuf.append(nnAllStock.NN_ALLSTOCK140);
            inputBuf.append(nnAllStock.NN_ALLSTOCK141);
            inputBuf.append(nnAllStock.NN_ALLSTOCK142);
            inputBuf.append(nnAllStock.NN_ALLSTOCK143);
            inputBuf.append(nnAllStock.NN_ALLSTOCK144);
            inputBuf.append(nnAllStock.NN_ALLSTOCK145);
            inputBuf.append(nnAllStock.NN_ALLSTOCK146);
            inputBuf.append(nnAllStock.NN_ALLSTOCK147);
            inputBuf.append(nnAllStock.NN_ALLSTOCK148);
            inputBuf.append(nnAllStock.NN_ALLSTOCK149);

            inputBuf.append(nnAllStock.NN_ALLSTOCK150);
            inputBuf.append(nnAllStock.NN_ALLSTOCK151);
            inputBuf.append(nnAllStock.NN_ALLSTOCK152);
            inputBuf.append(nnAllStock.NN_ALLSTOCK153);
            inputBuf.append(nnAllStock.NN_ALLSTOCK154);
            inputBuf.append(nnAllStock.NN_ALLSTOCK155);
            inputBuf.append(nnAllStock.NN_ALLSTOCK156);
            inputBuf.append(nnAllStock.NN_ALLSTOCK157);
            inputBuf.append(nnAllStock.NN_ALLSTOCK158);
            inputBuf.append(nnAllStock.NN_ALLSTOCK159);

            inputBuf.append(nnAllStock.NN_ALLSTOCK160);
            inputBuf.append(nnAllStock.NN_ALLSTOCK161);
            inputBuf.append(nnAllStock.NN_ALLSTOCK162);
            inputBuf.append(nnAllStock.NN_ALLSTOCK163);
            inputBuf.append(nnAllStock.NN_ALLSTOCK164);
            inputBuf.append(nnAllStock.NN_ALLSTOCK165);
            inputBuf.append(nnAllStock.NN_ALLSTOCK166);
            inputBuf.append(nnAllStock.NN_ALLSTOCK167);
            inputBuf.append(nnAllStock.NN_ALLSTOCK168);
            inputBuf.append(nnAllStock.NN_ALLSTOCK169);

            inputBuf.append(nnAllStock.NN_ALLSTOCK170);
            inputBuf.append(nnAllStock.NN_ALLSTOCK171);
            inputBuf.append(nnAllStock.NN_ALLSTOCK172);
            inputBuf.append(nnAllStock.NN_ALLSTOCK173);
            inputBuf.append(nnAllStock.NN_ALLSTOCK174);
            inputBuf.append(nnAllStock.NN_ALLSTOCK175);
            inputBuf.append(nnAllStock.NN_ALLSTOCK176);
//            inputBuf.append(nnAllStock.NN_ALLSTOCK177);  // check nnAllStock data 
//            inputBuf.append(nnAllStock.NN_ALLSTOCK178);
//            inputBuf.append(nnAllStock.NN_ALLSTOCK179);

            String inputListSt = ServiceAFweb.decompress(inputBuf.toString());
            stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);
            return true;
        } catch (Exception ex) {
            logger.info("> AllStockHistoryGetfromStaticCode - exception " + ex);
        }
        return false;
    }

    public static ArrayList<AFstockInfo> AllStockHistoryGetfromStaticCode(String symbol) {

        ArrayList<AFstockInfo> inputlist = new ArrayList();
        AllStockHistoryGetfromStaticCodeInit();
        if (stockInputMap == null) {
            return inputlist;
        }

        if (symbol != "") {
            try {
                inputlist = stockInputMap.get(symbol);
                if (inputlist == null) {
                    return null;
                }
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
                AFstockInfo[] arrayItem = new ObjectMapper().readValue(inputListRawSt, AFstockInfo[].class);
                List<AFstockInfo> listItem = Arrays.<AFstockInfo>asList(arrayItem);
                inputlist = new ArrayList<AFstockInfo>(listItem);
                return inputlist;
            } catch (Exception ex) {
            }
        }

        return inputlist;

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

    private boolean NeuralNetAllStockCreatJava(ServiceAFweb serviceAFWeb, String nnName) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getStaticJavaAllStockInputDataFromFile(serviceAFWeb, nnName, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class nnAllData {\n"
                    + "\n");

            int sizeline = 1000;
            int len = inputListSt.length();
            int beg = 0;
            int end = sizeline;
            int index = 1;
            int line = 0;
            while (true) {
                if (line == 0) {
                    msgWrite.append(""
                            + "    public static String NN_ALLINPUTLIST" + index + " = \"\"\n"
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
            String fileN = ServiceAFweb.FileLocalDebugPath + "nnAllData.java";
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

    public ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
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

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        UpdateStockNNprocessNameArray(serviceAFWeb, accountAdminObj);
        if (stockNNprocessNameArray == null) {
            return;
        }
        if (stockNNprocessNameArray.size() == 0) {
            return;
        }

        String printName = "";
        for (int i = 0; i < stockNNprocessNameArray.size(); i++) {
            printName += stockNNprocessNameArray.get(i) + ",";
        }
        logger.info("ProcessTrainNeuralNetBySign " + printName);

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NN1_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }

//        logger.info("ProcessTrainNeuralNet " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long LastServUpdateTimer = System.currentTimeMillis();
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, 15); // add 3 minutes

//            for (int i = 0; i < 10; i++) {
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
//                    logger.info("ProcessTrainNeuralNet exit after 15 minutes");
                    break;
                }

                if (stockNNprocessNameArray.size() == 0) {
                    break;
                }

                String symbolTR = (String) stockNNprocessNameArray.get(0);
//                    stockNNprocessNameArray.remove(0);

                String[] symbolArray = symbolTR.split("#");
                if (symbolArray.length >= 0) {

                    String symbol = symbolArray[0];
                    //////////////////////
                    // just for testing
//                    symbol = "BABA";

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

                    String LockStock = "NN1_TR_" + symbol; // + "_" + trNN;
                    LockStock = LockStock.toUpperCase();

                    long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                    long lockReturnStock = 1;

                    lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
                    if (testing == true) {
                        lockReturnStock = 1;
                    }
//                    logger.info("ProcessTrainNeuralNet " + LockStock + " LockStock " + lockReturnStock);
                    if (lockReturnStock == 0) {
                        stockNNprocessNameArray.remove(0);
                        continue;
                    }

                    if (lockReturnStock > 0) {
                        try {
                            String nnName = ConstantKey.TR_NN1;
                            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;

                            AFneuralNet nnObj1 = ProcessTrainNeuralNet1(serviceAFWeb, BPnameSym, TR_NN, symbol);

                            if (nnObj1 != null) {
                                if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
                                    stockNNprocessNameArray.remove(0);
                                    serviceAFWeb.getStockImp().deleteNeuralNet1(BPnameSym);

//                                    if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
//                                        /// need to create the table to reduce the memeory in DB
//                                        serviceAFWeb.getStockImp().deleteNeuralNet1Table();
//                                    } else {
//                                        serviceAFWeb.getStockImp().deleteNeuralNet1(BPnameSym);
//                                    }
                                }
                            }

                        } catch (Exception ex) {
                            logger.info("> ProcessTrainNeuralNetBySign Exception" + ex.getMessage());
                        }
                        serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
//                        logger.info("ProcessTrainNeuralNet " + LockStock + " unLock LockStock ");
                    }
                }
            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
        logger.info("> ProcessTrainNeuralNetBySign ... done");
    }

    public AFneuralNet ProcessTrainNeuralNet1(ServiceAFweb serviceAFWeb, String BPnameSym, int TR_NN, String symbol) {
        this.Process1TrainNeuralNet(serviceAFWeb, TR_NN, BPnameSym, symbol);
        // first one is initial and the second one is to execute
        this.Process1TrainNeuralNet(serviceAFWeb, TR_NN, BPnameSym, symbol);

        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        return nnObj1;
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

//                logger.info("> inputStockNeuralNetData " + BPnameSym + " Status=" + status);
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
                boolean flag = true;
                if (flag == true) {
                    inputlistSym1 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1, 0);
                    inputlistSym2 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, 0);
                }
                inputlistSym.addAll(inputlistSym1);
                inputlistSym.addAll(inputlistSym2);

                ArrayList<NNInputDataObj> inputL = new ArrayList();
                boolean trainInFile = true;
                if (trainInFile == true) {
                    inputL = NeuralNetGetNN1InputfromStaticCode(symbol, null);
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
                boolean trainAllInFile = true;
                if (trainAllInFile == true) {
                    inputL = NeuralNetAllStockGetNN1InputfromStaticCode(symbol, null);
                    if (inputL != null) {
                        if (inputL.size() > 0) {
                            logger.info("> inputStockNeuralNetAllStockData " + BPnameSym + " " + symbol + " " + inputL.size());
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

//                        if (CKey.NN_DEBUG == true) {
////                            logger.info("> inputStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
//                        }

                    }
                }

                String refName = "";
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
                            logger.info("> inputStockNeuralNetData create existing Symbol ");
                            //just for testing                           
                            nnTemp.createNet(stWeight0);
                            refName = nnObj0.getRefname();
                        } else {
                            logger.info("> inputStockNeuralNetData create Static Base ");
                        }
                    }
                } else {
                    logger.info("> inputStockNeuralNetData create Static Base ");
                }
                logger.info("> inputStockNeuralNetData v" + version + " " + middlelayer + " " + nnName + " " + BPnameSym + "  toAdd=" + totalAdd + " toDup=" + totalDup);

                String weightSt = nnTemp.getNetObjSt();
                int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObj1(BPnameSym, weightSt);

                if (refName.length() > 0) {
                    // just for testing
//                    refName = "" + CKey.NN1_ERROR_THRESHOLD;
                    logger.info("> inputStockNeuralNet  " + BPnameSym + " refError " + refName);
                    serviceAFWeb.getStockImp().updateNeuralNetRef1(BPnameSym, refName);
                }

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
                String refName = nnObj1.getRefname();
                if (refName.length() > 0) {
                    try {
                        double refError = Double.parseDouble(refName);
                        errorNN = refError + 0.0001;
                        logger.info("> stockTrainNeuralNet override new error " + BPname + " " + errorNN);
                    } catch (Exception ex) {

                    }
                }
                int retflag = 0;
                if (TR_NN == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN1, nnNameSym, symbol, errorNN);
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

    public static ArrayList<NNInputDataObj> NeuralNetGetNN1InputfromStaticCode(String symbol, String subSymbol) {
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
            inputBuf.append(nnData.NN_INPUTLIST10);
//            inputBuf.append(nnData.NN_INPUTLIST11); //need to check nnData file

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
                if (subSymbol != null) {
                    if (sym.equals("AAPL")) {
                        continue;
                    }
                    if (sym.equals("RY.TO")) {
                        continue;
                    }
                    if (subSymbol.equals(sym)) {
                        continue;
                    }
                }
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

    public static ArrayList<NNInputDataObj> NeuralNetAllStockGetNN1InputfromStaticCode(String symbol, String subSymbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nnAllData.NN_ALLINPUTLIST1);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST2);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST3);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST4);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST5);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST6);
            inputBuf.append(nnAllData.NN_ALLINPUTLIST7);
//            inputBuf.append(nnAllData.NN_ALLINPUTLIST8); //need to check nnData file

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
                if (subSymbol != null) {
                    if (subSymbol.equals(sym)) {
                        continue;
                    }
                }
                ArrayList<NNInputDataObj> inputL = stockInputMap.get(sym);
                String inputListRawSt = new ObjectMapper().writeValueAsString(inputL);
                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
                inputL = new ArrayList<NNInputDataObj>(listItem);
                inputlist.addAll(inputL);
            }

            return inputlist;
        } catch (Exception ex) {
            logger.info("> NeuralNetAllStockGetNN1InputfromStaticCode - exception " + ex);
        }
        return null;
    }

}
