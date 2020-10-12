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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class NNProcessByTrend {

    public static Logger logger = Logger.getLogger("NNProcessStock");

    public void processNeuralNetStPred(ServiceAFweb serviceAFWeb) {

        TrandingSignalProcess.forceToGenerateNewNN = false;

        boolean flagNeuralnetInput = false;
        if (flagNeuralnetInput == true) {
            logger.info("> NeuralnetInput TR NN1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            logger.info("> NeuralnetInput TR NN2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);
            // need to debug to generate the java first time before NN training
            TrandingSignalProcess.forceToGenerateNewNN = true;

        }

// only need this first time        
//        TrandingSignalProcess.forceToGenerateNewNN = true;
// only need this first time      
        boolean flagNeuralnetTrain = false;
        if (flagNeuralnetTrain == true) {
            TrandingSignalProcess.forceToErrorNewNN = true;
            // start training
            NeuralNetProcessTesting(serviceAFWeb);
        }

        boolean flagNeuralnetCreateJava = false;
        if (flagNeuralnetCreateJava == true) {
            NeuralNetNN3CreatJava(serviceAFWeb, ConstantKey.TR_NN3);

        }

        boolean flagNeural = false;
        if (flagNeural == true) {
            TradingNNprocess NNProcessImp = new TradingNNprocess();
            int retSatus = NNProcessImp.ClearStockNNinputNameArray(serviceAFWeb, ConstantKey.TR_NN3);
            for (int k = 0; k < 10; k++) {
                ProcessTrainNeuralNetByTrend(serviceAFWeb);
                logger.info("> ProcessTrainNeuralNet NN3 cycle " + k);
            }
        }

        //////////////////////////////////////////////////////////
        boolean flagTestNNHistory = false;
        if (flagTestNNHistory == true) {
            AccountObj accountObj = serviceAFWeb.getAdminObjFromCache();
            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            String symbol = "HOU.TO";
            symbol = "AAPL";
            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            String nnName = ConstantKey.TR_NN3;
            serviceAFWeb.SystemAccountStockClrTranByAccountID(accountObj, stock.getId(), nnName);

            TRprocessImp.testUpdateAdminTradingsignal(serviceAFWeb, symbol);
            TRprocessImp.testUpdateAdminTradingsignal(serviceAFWeb, symbol);

            // get 2 year
            /// thObjList old first - recent last
//            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountObj.getId(), stock.getId(), nnName);
//            ArrayList<StockTRHistoryObj> trHistoryList = TRprocessImp.ProcessTRHistory(serviceAFWeb, trObj, 2);
            logger.info("trHistoryList ");
        }

        boolean flagTestNNSignal = false;
        if (flagTestNNSignal == true) {
            AccountObj accountObj = serviceAFWeb.getAdminObjFromCache();
            String symbol = "HOU.TO";

            AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
            int size1year = 20 * 12 * 2 + (50 * 3);
            ArrayList StockArray = serviceAFWeb.getStockHistorical(symbol, size1year);
            // update Trading signal
            ArrayList<TradingRuleObj> UpdateTRList = new ArrayList();
            ArrayList tradingRuleList = serviceAFWeb.SystemAccountStockListByAccountID(accountObj.getId(), symbol);
            int offset = 0;

            for (int j = 0; j < tradingRuleList.size(); j++) {

                TradingRuleObj trObj = (TradingRuleObj) tradingRuleList.get(j);

                switch (trObj.getType()) {
                    case ConstantKey.INT_TR_NN3:
                        ProcessNN3 nn3 = new ProcessNN3();
                        nn3.updateAdminTradingsignalnn3(serviceAFWeb, accountObj, symbol, trObj, StockArray, offset, UpdateTRList, stock, tradingRuleList);
                        break;

                    default:
                        break;
                }

            }
        }

        ////////
    }

    private void NeuralNetProcessTesting(ServiceAFweb serviceAFWeb) {
        ///////////////////////////////////////////////////////////////////////////////////
        // read new NN data
        serviceAFWeb.forceNNReadFileflag = true; // should be true to get it from file instead from db
        ///// just for testing
//        serviceAFWeb.forceNNReadFileflag = false; // should be true to get it from file instead from db

        ///// just for testing        
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        boolean initTrainNeuralNet = true;
        if (initTrainNeuralNet == true) {

            double errorNN = CKey.NN3_ERROR_THRESHOLD;
            String nnName = ConstantKey.TR_NN3;

            String BPname = CKey.NN_version + "_" + nnName;

            boolean flagInit = true;
            if (flagInit == true) {
                AFneuralNet afNeuralNet = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);

                if (afNeuralNet == null) {
                    afNeuralNet = new AFneuralNet();
                    afNeuralNet.setName(BPname);
                    afNeuralNet.setStatus(ConstantKey.OPEN);
                    afNeuralNet.setType(0);
                    Calendar dateDefault = TimeConvertion.getDefaultCalendar();
                    afNeuralNet.setUpdatedatedisplay(new java.sql.Date(dateDefault.getTimeInMillis()));
                    afNeuralNet.setUpdatedatel(dateDefault.getTimeInMillis());
                    String weightSt = (CKey.NN3_WEIGHT_0);
                    afNeuralNet.setWeight(weightSt);
                    String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;
                    serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);

//                    serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using NN3_WEIGHT_0");
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
//
                            String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;
                            serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
//                            serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);
                        }
                    }
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using DB");
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, nnName, nnName, errorNN);

                if (retflag == 1) {
                    break;
                }
                logger.info(">>> NeuralNetProcessTesting " + i);
            }
        }

    }
////////////////////////////////////////////////////////////////////
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
        logger.info("> getTrainingNNdataProcess tr_" + tr + " " + symbol);

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
        ///// just for testing
//        symbol = "DIA";
        symbol = symbol.replace(".", "_");

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        if (offset == 0) {
            size1yearAll = size1yearAll / 2;
        }

        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;
        ProcessNN00 nn00 = new ProcessNN00();
        String nnName = ConstantKey.TR_NN1;
        if (tr == ConstantKey.INT_TR_NN1) {
            nnName = ConstantKey.TR_NN1;
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            

            inputList = nn00.trainingNN00_1dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            nnName = ConstantKey.TR_NN2;

            inputList = nn00.trainingNN00_0dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        }

        String BPname = CKey.NN_version + "_" + nnName;
        boolean forceNN3flag = true;
        if (forceNN3flag) {
            BPname = CKey.NN_version + "_" + ConstantKey.TR_NN3;
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
            String nn12 = "_nn31_";
            if (tr == ConstantKey.INT_TR_NN2) {
                nn12 = "_nn32_";
            }
            String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + ServiceAFweb.initTrainNeuralNetNumber + ".csv";

            FileUtil.FileWriteTextArray(filename, writeArray);
//            ServiceAFweb.writeArrayNeuralNet.addAll(writeArray);

        }
        inputList.remove(len - 1);
        inputList.remove(0);

        //////// do not save in DB, only files
        return inputList;
    }

    public ArrayList<NNInputDataObj> getTrainingNNStdataProcess(ServiceAFweb serviceAFWeb, String symbol, int offset) {
        logger.info("> getTrainingNNdataProcess " + symbol);
        symbol = symbol.replace(".", "_");

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        if (offset == 0) {
            size1yearAll = size1yearAll / 2;
        }

        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        String nnName = ConstantKey.TR_NN1;
        ProcessNN00 nn00 = new ProcessNN00();
        inputList = nn00.trainingNN00_1dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);

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
                    + "\",\"" + obj.getOutput3()
                    + "\",\"" + obj.getOutput4()
                    //                    + "\",\"" + obj.getOutput5()
                    //                    + "\",\"" + obj.getOutput6()
                    //                    + "\",\"" + obj.getOutput7()
                    //                    + "\",\"" + obj.getOutput8()
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
                        + "\",\"" + "output3"
                        + "\",\"" + "output4"
                        //                        + "\",\"" + "output5"
                        //                        + "\",\"" + "output6"
                        //                        + "\",\"" + "output7"
                        //                        + "\",\"" + "output8"
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
            String nn14 = "_nn4_";
            String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn14 + ServiceAFweb.initTrainNeuralNetNumber + ".csv";

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

    private boolean NeuralNetNN3CreatJava(ServiceAFweb serviceAFWeb, String nnName) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, nnName, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            String fileN = ServiceAFweb.FileLocalDebugPath + nnName + "_nnWeight0.txt";
            if (FileUtil.FileTest(fileN) == false) {
                return false;
            }
            StringBuffer msg1 = FileUtil.FileReadText(fileN);
            String weightSt = msg1.toString();
            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class nn3Data {\n"
                    + "\n"
                    + "    public static String " + nnName + "_WEIGHT_0 = \"\"\n");
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
                            + "    public static String " + nnName + "_INPUTLIST" + index + " = \"\"\n"
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
            fileN = ServiceAFweb.FileLocalDebugPath + "nn3Data.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    //////////////////////////////////////////////////
    private static ArrayList stockNNprocessNameArray = new ArrayList();
    private static ArrayList stockNNinputNameArray = new ArrayList();
    private static ArrayList stockNNretrainprocessNameArray = new ArrayList();

    private ArrayList UpdateStockNN3processNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
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
                String symTR = sym + "#" + ConstantKey.INT_TR_NN3;
                stockTRNameArray.add(symTR);
            }

            stockNNprocessNameArray = stockTRNameArray;
        }
        return stockNNprocessNameArray;
    }

    public void ProcessTrainNeuralNetByTrend(ServiceAFweb serviceAFWeb) {
//        logger.info("> ProcessTrainNeuralNet ");
        if (getEnv.checkLocalPC() != true) {
            if (CKey.SERVERDB_URL.equals(CKey.URL_PATH_HERO) == true) {
                ///Error R14 (Memory quota exceeded) in heroku
                ///Error R14 (Memory quota exceeded) in heroku
                if (ServiceAFweb.NN_AllowTraingStockFlag == false) {
                    return;
                }
            }
        }
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        UpdateStockNN3processNameArray(serviceAFWeb, accountAdminObj);
        if (stockNNprocessNameArray == null) {
            return;
        }
        if (stockNNprocessNameArray.size() == 0) {
            return;
        }

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NN3_" + ServiceAFweb.getServerObj().getServerName();
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
                        int TR_NN = Integer.parseInt(symbolArray[1]);

                        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);

                        if (stock == null) {
                            stockNNprocessNameArray.remove(0);
                            continue;
                        }
                        if (stock.getAfstockInfo() == null) {
                            stockNNprocessNameArray.remove(0);
                            continue;
                        }

                        String LockStock = "NN3_TR_" + symbol; // + "_" + trNN;
                        LockStock = LockStock.toUpperCase();

                        long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                        long lockReturnStock = 1;

//                        lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
//
//                        if (testing == true) {
//                            lockReturnStock = 1;
//                        }
//                        logger.info("ProcessTrainNeuralNet " + LockStock + " LockStock " + lockReturnStock);
                        if (lockReturnStock > 0) {
                            String nnName = ConstantKey.TR_NN3;
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
            inputStockNeuralNetData(serviceAFWeb, TR_NN, symbol);
            return;
        }
        if (nnObj1.getStatus() == ConstantKey.INITIAL) {
            inputStockNeuralNetData(serviceAFWeb, TR_NN, symbol);
        }
        if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
            stockNNprocessNameArray.remove(0);
            return;
        }
        stockTrainNeuralNet(serviceAFWeb, TR_NN, symbol);
    }

    public int stockTrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String symbol) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> processStockNeuralNet " + TR_Name + " " + symbol);

        boolean nnsymTrain = true;
        if (nnsymTrain == true) {
            String nnName = ConstantKey.TR_NN3;
            double errorNN = CKey.NN3_ERROR_THRESHOLD;

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
                if (TR_NN == ConstantKey.INT_TR_NN3) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN3, nnNameSym, errorNN);
                }
//                logger.info("> processStockNeuralNet ... Done");
                return retflag;
            } catch (Exception e) {
                logger.info("> stockTrainNeuralNet exception " + BPname + " - " + e.getMessage());
            }
        }
        return -1;
    }

    public int inputStockNeuralNetData(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        boolean nnsym = true;
        if (nnsym == true) {
            int totalAdd = 0;
            int totalDup = 0;
            String nnName = ConstantKey.TR_NN3;

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
                if (TR_Name == ConstantKey.INT_TR_NN3) {
                    if (CKey.NN3_WEIGHT_0.length() == 0) {
                        return 0;
                    }
                    nnTemp.createNet(CKey.NN3_WEIGHT_0);
                    String weightSt = nnTemp.getNetObjSt();
                    String[] strNetArray = CKey.NN3_WEIGHT_0.split(";");
                    version = strNetArray[0];
                    middlelayer = strNetArray[4];
                }
                ArrayList<NNInputOutObj> inputlist = new ArrayList();

                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
                inputlistSym = getTrainingNN3dataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN3, 0);

                ArrayList<NNInputDataObj> inputL = new ArrayList();

                inputL = NeuralNetGetNN3InputfromStaticCode(symbol);
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

                if (inputlistSym != null) {
                    //merge inputlistSym

                    for (int i = 0; i < inputlistSym.size(); i++) {
                        NNInputOutObj inputObj = inputlistSym.get(i).getObj();
                        if (inputObj.getOutput1() < 0) {
                            continue;
                        }
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
                        //just for testing
//                        versionSym="";
                        if (middlelayer.equals(middlelayerSym) && version.equals(versionSym)) {
                            logger.info("> inputStockNeuralNetData create existing Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
                            ///just for testing
                            nnTemp.createNet(stWeight0);
                        } else {
                            logger.info("> inputStockNeuralNetData create Static Base NN1_WEIGHT " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
                        }
                    }
                }
                String weightSt = nnTemp.getNetObjSt();
//                
                String refname = CKey.NN_version + "_" + ConstantKey.TR_NN3;
                int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPnameSym, refname, weightSt);

//                int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObj1(BPnameSym, weightSt);
//                logger.info("> inputStockNeuralNet " + BPnameSym + " inputlist=" + inputlist.size() + " ...Done");
                return ret;

            } catch (Exception e) {
                logger.info("> inputStockNeuralNet exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;

    }

    public static ArrayList<NNInputDataObj> NeuralNetGetNN3InputfromStaticCode(String symbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST1);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST2);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST3);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST4);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST5);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST6);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST7);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST8);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST9);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST10);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST11);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST12);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST13);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST14);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST15);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST16);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST17);
            inputBuf.append(nn3Data.TR_NN3_INPUTLIST18);
//            inputBuf.append(nn3Data.TR_NN3_INPUTLIST19); // check nn3 data

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
            logger.info("> NeuralNetGetNN4InputfromStaticCode - exception " + ex);
        }
        return null;
    }

    public ArrayList<NNInputDataObj> getTrainingNN3dataStock(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> trainingNN ");
        int size1yearAll = 20 * 12 * 2 + (50 * 3);

//        logger.info("> trainingNN " + symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_NN3) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN00 nn00 = new ProcessNN00();
            inputList = nn00.trainingNN00_0dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE); // 14
        }

        // ignor first and last
        int len = inputList.size();
        if (len <= 2) {
            return null;
        }
        return inputList;
    }

    public ArrayList retrainStockNNprocessNameArray(ServiceAFweb serviceAFWeb) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (stockNNretrainprocessNameArray != null && stockNNretrainprocessNameArray.size() > 0) {
            return stockNNretrainprocessNameArray;
        }

        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            ArrayList stockTRNameArray = new ArrayList();
            for (int i = 0; i < stockNameArray.size(); i++) {
                String sym = (String) stockNameArray.get(i);
                String symTR = sym + "#" + ConstantKey.INT_TR_NN1;
                stockTRNameArray.add(symTR);
                boolean NN2flag = true;
                if (NN2flag == true) {
                    symTR = sym + "#" + ConstantKey.INT_TR_NN2;
                    stockTRNameArray.add(symTR);
                }
            }
            stockNNretrainprocessNameArray = stockTRNameArray;
        }
        return stockNNretrainprocessNameArray;
    }

}
