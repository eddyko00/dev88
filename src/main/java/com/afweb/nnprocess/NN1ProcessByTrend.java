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
public class NN1ProcessByTrend {

    public static Logger logger = Logger.getLogger("NNProcessStock");

    public void processNN30InputNeuralNetTrend(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////
        boolean flagIntitNN3Input = true;
        if (flagIntitNN3Input == true) {

            TrandingSignalProcess.forceToInitleaningNewNN = true;  // must be true all for init learning
            TrandingSignalProcess.forceToGenerateNewNN = false;
            logger.info("> processInputTrend TR MACD0... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_MACD0);
            logger.info("> processInputTrend TR MACD1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_MACD1);
//            logger.info("> processInputNeuralNetTrend TR NN1... ");
//            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
//            logger.info("> processInputNeuralNetTrend TR NN2... ");
//            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);
            // need to debug to generate the java first time
            TrandingSignalProcess.forceToGenerateNewNN = true;

            TrandingSignalProcess.forceToErrorNewNN = true;
            // start training
            // TrainingNNBP inputpattern 1748
            NeuralNetProcessTesting(serviceAFWeb);
            NeuralNetNN30CreatJava(serviceAFWeb, ConstantKey.TR_NN30);

            TrandingSignalProcess.forceToGenerateNewNN = false;
            // start training
            // TrainingNNBP inputpattern 1748
            NeuralNetProcessTesting(serviceAFWeb);
            NeuralNetNN30CreatJava(serviceAFWeb, ConstantKey.TR_NN30);
            NeuralNetProcessTesting(serviceAFWeb);
            NeuralNetNN30CreatJava(serviceAFWeb, ConstantKey.TR_NN30);
            logger.info("> processInputNeuralNetTrend TR NN1 end....... ");

        }

        ////////////////////////////////////////////
    }

    public void processAllNN30StockInputNeuralNetTrend(ServiceAFweb serviceAFWeb) {
        logger.info("> processAllStockInputNeuralNetTrend TR MACD0... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_MACD0);
        logger.info("> processAllStockInputNeuralNetTrend TR MACD1... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_MACD1);

        NeuralNetAllStockNN30CreatJava(serviceAFWeb, ConstantKey.TR_NN30);
        logger.info("> processAllStockInputNeuralNetTrend TR NN1 end....... ");

    }

    public void NeuralNetProcessTesting(ServiceAFweb serviceAFWeb) {
        ///////////////////////////////////////////////////////////////////////////////////
        // read new NN data
        serviceAFWeb.forceNNReadFileflag = true; // should be true to get it from file instead from db
        ///// just for testing
//        serviceAFWeb.forceNNReadFileflag = false; // should be true to get it from file instead from db

        ///// just for testing        
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        boolean initTrainNeuralNet = true;
        if (initTrainNeuralNet == true) {

            double errorNN = CKey.NN30_ERROR_THRESHOLD;
            String nnName = ConstantKey.TR_NN30;

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
                    String weightSt = (CKey.NN30_WEIGHT_0);
                    afNeuralNet.setWeight(weightSt);

//                    String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;
//                    serviceAFWeb.getStockImp().setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
                    serviceAFWeb.setNeuralNetObjWeight1(afNeuralNet);

                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using NN30_WEIGHT_0");
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
                retflag = TrainingNNTrendNeuralNetData(serviceAFWeb, nnName, nnName, "", errorNN);

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
                ArrayList<NNInputDataObj> InputList = getTrainingNNtrendProcess(serviceAFWeb, symbol, TR_Name, size);
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
                ArrayList<NNInputDataObj> InputList = getTrainingNNtrendProcess(serviceAFWeb, symbol, TR_Name, size);
            }
        }
    }

    public ArrayList<NNInputDataObj> getTrainingNNtrendProcess(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
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

        if (tr == ConstantKey.INT_TR_MACD1) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            

            inputList = nn00.trainingNN00_dataMACD1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
            // normal 
        } else if (tr == ConstantKey.INT_TR_MACD0) {
            inputList = nn00.trainingNN00_dataMACD0(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
            // fast
        }

        String BPname = CKey.NN_version + "_" + ConstantKey.TR_NN30;

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
            String nn12 = "_nn301_";
            if (tr == ConstantKey.INT_TR_MACD0) {
                nn12 = "_nn300_";
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

    public boolean NeuralNetNN30CreatJava(ServiceAFweb serviceAFWeb, String nnName) {
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
                    + "public class nn30Data {\n"
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
            if (end <= len) {
                ;
            } else {
                end = len;
            }
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
            fileN = ServiceAFweb.FileLocalDebugPath + "nn30Data.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public boolean NeuralNetAllStockNN30CreatJava(ServiceAFweb serviceAFWeb, String nnName) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
        try {
            TRprocessImp.getStaticJavaAllStockInputDataFromFile(serviceAFWeb, nnName, stockInputMap);
            String inputListSt = "Data in DB";
            if (CKey.NN_DATA_DB == true) {
                TradingNNData nndata = new TradingNNData();
                nndata.updateNNdataDB(serviceAFWeb, nnName, stockInputMap);

            } else {
                String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
                inputListSt = ServiceAFweb.compress(inputListRawSt);
            }
            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "\n"
                    + "public class nn30AllData {\n"
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
                            + "    public static String " + nnName + "_ALLINPUTLIST" + index + " = \"\"\n"
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
            String fileN = ServiceAFweb.FileLocalDebugPath + "nn30AllData.java";
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

    private ArrayList UpdateStockNN30processNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        if (stockNNprocessNameArray != null && stockNNprocessNameArray.size() > 0) {
            return stockNNprocessNameArray;
        }

//        boolean guestFlag = false;
//        if (guestFlag == true) {
//            AccountObj account = serviceAFWeb.getAccountImp().getAccountByType("GUEST", "guest", AccountObj.INT_TRADING_ACCOUNT);
//            accountObj = account;
//        }
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            ArrayList stockTRNameArray = new ArrayList();
            for (int i = 0; i < stockNameArray.size(); i++) {
                String sym = (String) stockNameArray.get(i);
                String symTR = sym + "#" + ConstantKey.INT_TR_NN30;
                stockTRNameArray.add(symTR);
            }

            stockNNprocessNameArray = stockTRNameArray;
        }
        return stockNNprocessNameArray;
    }

    public void ProcessTrainNeuralNetNN1ByTrend(ServiceAFweb serviceAFWeb) {

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        UpdateStockNN30processNameArray(serviceAFWeb, accountAdminObj);
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
        logger.info("ProcessTrainNeuralNetNN1ByTrend " + printName);

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NN30_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }
//        logger.info("ProcessTrainNeuralNetByTrend " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long LastServUpdateTimer = System.currentTimeMillis();
            int timeout = 15;
            if (ServiceAFweb.processNeuralNetFlag == true) {
                timeout = timeout * 4;
            }
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, timeout); // add 3 minutes

//            for (int i = 0; i < 10; i++) {
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
//                    logger.info("ProcessTrainNeuralNetByTrend exit after 15 minutes");
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
                    // just for testing
//                    symbol = "BABA";
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
                    this.TrainNN30NeuralNetByTrend(serviceAFWeb, symbol, TR_NN, stockNNprocessNameArray);

//                    String LockStock = "NN30_TR_" + symbol; // + "_" + trNN;
//                    LockStock = LockStock.toUpperCase();
//
//                    long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
//                    long lockReturnStock = 1;
//
//                    lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
//
//                    if (testing == true) {
//                        lockReturnStock = 1;
//                    }
////                    logger.info("ProcessTrainNeuralNetByTrend " + LockStock + " LockStock " + lockReturnStock);
//                    if (lockReturnStock == 0) {
//                        stockNNprocessNameArray.remove(0);
//                        continue;
//                    }
//                    if (lockReturnStock > 0) {
//                        try {
//                            String nnName = ConstantKey.TR_NN30;
//                            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//
//                            AFneuralNet nnObj1 = ProcessTrainNeuralNetByTrend1(serviceAFWeb, BPnameSym, TR_NN, symbol);
//
//                            if (nnObj1 != null) {
//                                if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
//                                    stockNNprocessNameArray.remove(0);
//                                    serviceAFWeb.getStockImp().deleteNeuralNet1(BPnameSym);
//
////                                    if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
////                                        /// need to create the table to reduce the memeory in DB
////                                        serviceAFWeb.getStockImp().deleteNeuralNet1Table();
////                                    } else {
////                                        serviceAFWeb.getStockImp().deleteNeuralNet1(BPnameSym);
////                                    }
//                                }
//                            }
//                        } catch (Exception ex) {
//                            logger.info("> ProcessTrainNeuralNetNN1ByTrend Exception" + ex.getMessage());
//                        }
//                        serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
////                        logger.info("ProcessTrainNeuralNetByTrend " + LockStock + " unLock LockStock ");
//                    }
                }
            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNetByTrend " + LockName + " unlock LockName");
        }
        logger.info("> ProcessTrainNeuralNetNN1ByTrend ... done");
    }

    public void TrainNN30NeuralNetByTrend(ServiceAFweb serviceAFWeb, String symbol, int TR_NN, ArrayList stockNNprocessNameArray) {

        String LockStock = "NN30_TR_" + symbol; // + "_" + trNN;
        LockStock = LockStock.toUpperCase();

        long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
        long lockReturnStock = 1;

        lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");

//                    logger.info("ProcessTrainNeuralNetByTrend " + LockStock + " LockStock " + lockReturnStock);
        if (lockReturnStock == 0) {
            if (stockNNprocessNameArray != null) {
                stockNNprocessNameArray.remove(0);
            }
            return;
        }
        if (lockReturnStock > 0) {
            try {
                String nnName = ConstantKey.TR_NN30;
                String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;

                AFneuralNet nnObj1 = ProcessTrainNeuralNetByTrend1(serviceAFWeb, BPnameSym, TR_NN, symbol);

                if (nnObj1 != null) {
                    if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
                        if (stockNNprocessNameArray != null) {
                            stockNNprocessNameArray.remove(0);
                        }
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
                logger.info("> PTrainNN30NeuralNetByTrend Exception" + ex.getMessage());
            }
            serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
//                        logger.info("ProcessTrainNeuralNetByTrend " + LockStock + " unLock LockStock ");
        }
    }

    public AFneuralNet ProcessTrainNeuralNetByTrend1(ServiceAFweb serviceAFWeb, String BPnameSym, int TR_NN, String symbol) {

        this.Process1TrainNeuralNetByTrend(serviceAFWeb, TR_NN, BPnameSym, symbol);
        // first one is initial and the second one is to execute
        this.Process1TrainNeuralNetByTrend(serviceAFWeb, TR_NN, BPnameSym, symbol);

        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        return nnObj1;
    }

    private void Process1TrainNeuralNetByTrend(ServiceAFweb serviceAFWeb, int TR_NN, String BPnameSym, String symbol) {

        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        if (nnObj1 == null) {
            inputStockNeuralNetData(serviceAFWeb, TR_NN, symbol);
            return;
        }
        if (nnObj1.getStatus() == ConstantKey.INITIAL) {
            inputStockNeuralNetData(serviceAFWeb, TR_NN, symbol);
        }
        if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
//            stockNNprocessNameArray.remove(0);
            return;
        }
        stockTrainNeuralNet(serviceAFWeb, TR_NN, symbol);
    }

    public int stockTrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String symbol) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> processStockNeuralNet " + TR_Name + " " + symbol);

        boolean nnsymTrain = true;
        if (nnsymTrain == true) {
            String nnName = ConstantKey.TR_NN30;
            double errorNN = CKey.NN30_ERROR_THRESHOLD;

            String nnNameSym = nnName + "_" + symbol;
            String BPname = CKey.NN_version + "_" + nnNameSym;
            try {

                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);
                if (nnObj1 != null) {
                    if (nnObj1.getStatus() != ConstantKey.OPEN) {
                        return -1;
                    }
                }
                ReferNameData refData = new ReferNameData();
                refData = serviceAFWeb.getReferNameData(nnObj1);
                if (refData.getmError() != 0) {
                    errorNN = refData.getmError() + 0.0002;
                    logger.info("> stockTrainNeuralNet override new error " + BPname + " " + errorNN);
                }
//                String refName = nnObj1.getRefname();
//                if (refName != null) {
//                    if (refName.length() > 0) {
//                        try {
//                            double refError = Double.parseDouble(refName);
//                            errorNN = refError + 0.0001;
//                            logger.info("> stockTrainNeuralNet override new error " + BPname + " " + errorNN);
//                        } catch (Exception ex) {
//
//                        }
//                    }
//                }
                int retflag = 0;
                if (TR_NN == ConstantKey.INT_TR_NN30) {
                    retflag = TrainingNNTrendNeuralNetData(serviceAFWeb, ConstantKey.TR_NN30, nnNameSym, symbol, errorNN);
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
            String nnName = ConstantKey.TR_NN30;

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
                if (TR_Name == ConstantKey.INT_TR_NN30) {
                    if (CKey.NN30_WEIGHT_0.length() == 0) {
                        return 0;
                    }
                    nnTemp.createNet(CKey.NN30_WEIGHT_0);
                    String weightSt = nnTemp.getNetObjSt();
                    String[] strNetArray = CKey.NN30_WEIGHT_0.split(";");
                    version = strNetArray[0];
                    middlelayer = strNetArray[4];
                }
                ArrayList<NNInputOutObj> inputlist = new ArrayList();

                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
                inputlistSym = getTrainingNN30dataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN30, 0);

                ArrayList<NNInputDataObj> inputL = new ArrayList();
                boolean trainInFile = true;
                if (trainInFile == true) {
                    inputL = NeuralNetGetNN3InputfromStaticCode(symbol, null);
                    if (inputL != null) {
                        if (inputL.size() > 0) {
//                            logger.info("> inputStockNeuralNetData " + BPnameSym + " " + symbol + " " + inputL.size());
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
                    inputL = NeuralNetAllStockGetNN3InputfromStaticCode(symbol, null);
                    if (inputL != null) {
                        if (inputL.size() > 0) {
//                            logger.info("> inputStockNeuralNetAllData " + BPnameSym + " " + symbol + " " + inputL.size());
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
//                        boolean flag = false;
//                        if (flag == true) {
//                            if (CKey.NN_DEBUG == true) {
////                                logger.info("> inputStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
//                            }
//                        }
                    }
                }
                ReferNameData refData = new ReferNameData();
//                String refName = "";
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
                            logger.info("> inputStockNeuralNetData create existing Symbol ");
                            ///just for testing
                            nnTemp.createNet(stWeight0);
                            refData = serviceAFWeb.getReferNameData(nnObj0);
//                            refName = nnObj0.getRefname();
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
                if (refData.getmError() != 0) {
                    logger.info("> inputStockNeuralNet  " + BPnameSym + " refMinError " + refData.getmError());
                    serviceAFWeb.getStockImp().updateNeuralNetRef1(BPnameSym, refData);
                }
//                if (refName != null) {
//                    if (refName.length() > 0) {
//
//                        logger.info("> inputStockNeuralNetData  " + BPnameSym + " refError " + refName);
//                        serviceAFWeb.getStockImp().updateNeuralNetRef1(BPnameSym, refName);
//                    }
//                }
//                logger.info("> inputStockNeuralNet " + BPnameSym + " inputlist=" + inputlist.size() + " ...Done");
                return ret;

            } catch (Exception e) {
                logger.info("> inputStockNeuralNetData exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;

    }

    public ArrayList<NNInputDataObj> getTrainingNN30dataStock(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> trainingNN ");
        int size1yearAll = 20 * 12 * 2 + (50 * 3);

//        logger.info("> trainingNN " + symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_NN30) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN00 nn00 = new ProcessNN00();
            inputList = nn00.trainingNN00_dataMACD0(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE + 10); // 14

//            inputList = nn00.trainingNN00_0dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE); // 14
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

    public int TrainingNNTrendNeuralNetData(ServiceAFweb serviceAFWeb, String nnName, String nnNameSym, String symbol, double nnError) {
        String BPnameSym = CKey.NN_version + "_" + nnNameSym;

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release        
        AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
        if (nnObj1 == null) {
            return 0;
        }

        if (nnObj1.getStatus() != ConstantKey.OPEN) {
            boolean flag = true;
            if (flag == true) {
                if (CKey.NN_DEBUG == true) {
                    ;
                } else {
                    return 1;
                }
            }

        }

        logger.info("> TRtrainingNNTrendNeuralNet " + BPnameSym + " Statue=" + nnObj1.getStatus() + " Type=" + nnObj1.getType());

        String BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN30;

        return TrainingNNNeuralNetProcess(serviceAFWeb, BPnameTR, nnName, nnNameSym, symbol, nnError);
    }

    public int TrainingNNNeuralNetProcess(ServiceAFweb serviceAFWeb, String BPnameTR, String nnName, String nnNameSym, String symbol, double nnError) {
        String BPnameSym = CKey.NN_version + "_" + nnNameSym;
        ArrayList<NNInputOutObj> inputlist = new ArrayList();

        //just for testing
//        ServiceAFweb.forceNNReadFileflag = false;
        //just for testing 
        ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
        if (ServiceAFweb.forceNNReadFileflag == true) {
//            inputlist = getTrainingInputFromFile(serviceAFWeb, nnName);

            TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
            HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
            TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, nnName, stockInputMap);
            for (String sym : stockInputMap.keySet()) {
                ArrayList<NNInputDataObj> inputL = stockInputMap.get(sym);
                inputDatalist.addAll(inputL);
            }
            for (int i = 0; i < inputDatalist.size(); i++) {
                NNInputDataObj inputDObj = inputDatalist.get(i);
                NNInputOutObj inputObj = new NNInputOutObj();
                inputObj.setDateSt(inputDObj.getObj().getDateSt());
                inputObj.setClose(inputDObj.getObj().getClose());
                inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
                inputObj.setInput1(inputDObj.getObj().getInput1());
                inputObj.setInput2(inputDObj.getObj().getInput2());
                inputObj.setInput3(inputDObj.getObj().getInput3());
                inputObj.setInput4(inputDObj.getObj().getInput4());
                inputObj.setInput5(inputDObj.getObj().getInput5());
                inputObj.setInput6(inputDObj.getObj().getInput6());
                inputObj.setInput7(inputDObj.getObj().getInput7());
                inputObj.setInput8(inputDObj.getObj().getInput8());
                inputObj.setInput9(inputDObj.getObj().getInput9());
                inputObj.setInput10(inputDObj.getObj().getInput10());
                inputObj.setInput11(inputDObj.getObj().getInput11());
                inputObj.setInput12(inputDObj.getObj().getInput12());
                inputObj.setInput13(inputDObj.getObj().getInput13());
                //////
                inputObj.setOutput1(inputDObj.getObj().getOutput1());
                inputObj.setOutput2(inputDObj.getObj().getOutput2());
                inputObj.setOutput3(inputDObj.getObj().getOutput3());
                inputObj.setOutput4(inputDObj.getObj().getOutput4());
                if (inputObj.getOutput1() < 0) {
                    continue;
                }
                if (inputObj.getOutput2() < 0) {
                    continue;
                }
                inputlist.add(inputObj);
            }
        } else {
            /// new stock difficult to train need to remove the T.TO to see if it helps
            String subSymbol = null;
            if (symbol.length() != 0) {
                subSymbol = "RY.TO";
                for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
                    String stockN = ServiceAFweb.primaryStock[i];
                    if (stockN.equals(symbol)) {
                        subSymbol = null;
                        break;
                    }
                }
            }

            boolean trainInFile = true;
            if (trainInFile == true) {
                inputDatalist = NN1ProcessByTrend.NeuralNetGetNN3InputfromStaticCode("", subSymbol);

                if (inputDatalist != null) {
//                    logger.info("> NeuralNet NN1 " + BPnameSym + " " + inputDatalist.size());

                    for (int i = 0; i < inputDatalist.size(); i++) {
                        NNInputDataObj inputDObj = inputDatalist.get(i);
                        NNInputOutObj inputObj = new NNInputOutObj();
                        inputObj.setDateSt(inputDObj.getObj().getDateSt());
                        inputObj.setClose(inputDObj.getObj().getClose());
                        inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
                        inputObj.setInput1(inputDObj.getObj().getInput1());
                        inputObj.setInput2(inputDObj.getObj().getInput2());
                        inputObj.setInput3(inputDObj.getObj().getInput3());
                        inputObj.setInput4(inputDObj.getObj().getInput4());
                        inputObj.setInput5(inputDObj.getObj().getInput5());
                        inputObj.setInput6(inputDObj.getObj().getInput6());
                        inputObj.setInput7(inputDObj.getObj().getInput7());
                        inputObj.setInput8(inputDObj.getObj().getInput8());
                        inputObj.setInput9(inputDObj.getObj().getInput9());
                        inputObj.setInput10(inputDObj.getObj().getInput10());
                        inputObj.setInput11(inputDObj.getObj().getInput11());
                        inputObj.setInput12(inputDObj.getObj().getInput12());
                        inputObj.setInput13(inputDObj.getObj().getInput13());
                        //////
                        inputObj.setOutput1(inputDObj.getObj().getOutput1());
                        inputObj.setOutput2(inputDObj.getObj().getOutput2());
                        inputObj.setOutput3(inputDObj.getObj().getOutput3());
                        inputObj.setOutput4(inputDObj.getObj().getOutput4());
                        if (inputObj.getOutput1() < 0) {
                            continue;
                        }
                        if (inputObj.getOutput2() < 0) {
                            continue;
                        }
                        inputlist.add(inputObj);
                    }
                }
            }
            boolean trainAllInFile = true;
            if (trainAllInFile == true) {
                inputDatalist = NeuralNetAllStockGetNN3InputfromStaticCode(symbol, null);

                if (inputDatalist != null) {
//                    logger.info("> NeuralNetAllStock " + BPnameSym + " " + inputDatalist.size());

                    for (int i = 0; i < inputDatalist.size(); i++) {
                        NNInputDataObj inputDObj = inputDatalist.get(i);
                        NNInputOutObj inputObj = new NNInputOutObj();
                        inputObj.setDateSt(inputDObj.getObj().getDateSt());
                        inputObj.setClose(inputDObj.getObj().getClose());
                        inputObj.setTrsignal(inputDObj.getObj().getTrsignal());
                        inputObj.setInput1(inputDObj.getObj().getInput1());
                        inputObj.setInput2(inputDObj.getObj().getInput2());
                        inputObj.setInput3(inputDObj.getObj().getInput3());
                        inputObj.setInput4(inputDObj.getObj().getInput4());
                        inputObj.setInput5(inputDObj.getObj().getInput5());
                        inputObj.setInput6(inputDObj.getObj().getInput6());
                        inputObj.setInput7(inputDObj.getObj().getInput7());
                        inputObj.setInput8(inputDObj.getObj().getInput8());
                        inputObj.setInput9(inputDObj.getObj().getInput9());
                        inputObj.setInput10(inputDObj.getObj().getInput10());
                        inputObj.setInput11(inputDObj.getObj().getInput11());
                        inputObj.setInput12(inputDObj.getObj().getInput12());
                        inputObj.setInput13(inputDObj.getObj().getInput13());
                        //////
                        inputObj.setOutput1(inputDObj.getObj().getOutput1());
                        inputObj.setOutput2(inputDObj.getObj().getOutput2());
                        inputObj.setOutput3(inputDObj.getObj().getOutput3());
                        inputObj.setOutput4(inputDObj.getObj().getOutput4());
                        if (inputObj.getOutput1() < 0) {
                            continue;
                        }
                        if (inputObj.getOutput2() < 0) {
                            continue;
                        }
                        inputlist.add(inputObj);
                    }
                }
            }

            ArrayList<AFneuralNetData> objDataList = new ArrayList();

            if (BPnameTR.equals(BPnameSym)) {
                ;
            } else {
                objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym);
                if (objDataList != null) {
                    logger.info("> TRtrainingNNNeuralNetProcess " + BPnameSym + " " + inputlist.size() + " " + objDataList.size());
                    for (int i = 0; i < objDataList.size(); i++) {
                        String dataSt = objDataList.get(i).getData();
                        NNInputOutObj input;
                        try {
                            input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                            inputlist.add(input);
                        } catch (Exception ex) {
                        }
                    }
                }
            }

        }

        if (inputlist.size() == 0) {
            return 0;
        }
        NNTrainObj nnTraining = TradingNNprocess.trainingNNsetupTraining(inputlist, nnName);

        String NNnameSt = nnTraining.getNameNN() + "_" + nnNameSym;
        nnTraining.setNameNN(NNnameSt);
        nnTraining.setSymbol(nnNameSym);

        /// start training or continue training           
        /// start training or continue training
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        return TRprocessImp.TrainingNNBP(serviceAFWeb, nnNameSym, nnName, nnTraining, nnError);
    }

    public static ArrayList<NNInputDataObj> NeuralNetGetNN3InputfromStaticCode(String symbol, String subSymbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST1);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST2);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST3);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST4);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST5);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST6);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST7);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST8);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST9);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST10);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST11);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST12);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST13);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST14);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST15);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST16);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST17);
            inputBuf.append(nn30Data.TR_NN30_INPUTLIST18);
//            inputBuf.append(nn30Data.TR_NN30_INPUTLIST19); // check nn3 data
//            inputBuf.append(nn30Data.TR_NN30_INPUTLIST20); // check nn3 data

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
            logger.info("> NeuralNetGetNN3InputfromStaticCode - exception " + ex);
        }
        return null;
    }

    public static ArrayList<NNInputDataObj> NeuralNetAllStockGetNN3InputfromStaticCode(String symbol, String subSymbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST1);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST2);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST3);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST4);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST5);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST6);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST7);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST8);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST9);
            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST10); // check nn3 data 
//            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST11);
//            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST12);
//            inputBuf.append(nn30AllData.TR_NN30_ALLINPUTLIST13);  // check nn3 data  

//            inputBuf.append(nn30AllData.TR_NN3_ALLINPUTLIST12);
//            inputBuf.append(nn30AllData.TR_NN3_ALLINPUTLIST13);
//            inputBuf.append(nn30AllData.TR_NN3_ALLINPUTLIST14); // check nn3 data           
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
            logger.info("> NeuralNetAllStockGetNN3InputfromStaticCode - exception " + ex);
        }
        return null;
    }

    public ArrayList<NNInputDataObj> getAccountStockTRListHistoryTrendNN30(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI,
            String stockidsymbol, NNTrainObj nnTraining, boolean lastDateOutput) {

        if ((thObjListMACD == null) || (thObjListMV == null)) {
            return null;
        }
        if (thObjListMACD.size() != thObjListMV.size()) {
            return null;
        }
        if (thObjListRSI.size() != thObjListRSI.size()) {
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

        for (int i = 0; i < thObjListMACD.size(); i++) {

            if (i + 1 == thObjListMACD.size()) {
                if (lastDateOutput == true) {
                    processLastDate = true;
                }
            }
            NNInputOutObj inputList = new NNInputOutObj();

            StockTRHistoryObj thObjMACD = thObjListMACD.get(i);
            if (i == 0) {
                prevThObj = thObjMACD;
            }

            int signal = thObjMACD.getTrsignal();
            boolean contProcess = false;
            if (signal != prevThObj.getTrsignal()) {
                contProcess = true;
            }
            if (processLastDate == true) {
                contProcess = true;
            }

            if (contProcess == true) {

                // setup input parameter in inputList
                inputList = this.setupInputNN30(i, signal, thObjListMACD, thObjListMV, thObjListRSI);
                if (inputList == null) {
                    continue;
                }

                double output = getNNtrend4OutputClose(i, thObjListMACD);
                if ((output == -1) || (output == 0)) {
                    inputList.setOutput1(-1);
                    inputList.setOutput2(-1);
                } else {
                    //if output < 0.2
                    inputList.setOutput1(0.1);
                    inputList.setOutput2(0.1);
                    if (output > 0) {
                        if (output > 0.2) {
                            inputList.setOutput1(0.9);
                            inputList.setOutput2(0.1);
                        }

                    } else {  //if (output < 0) {
                        output = -output;
                        if (output > 0.2) {
                            inputList.setOutput1(0.1);
                            inputList.setOutput2(0.9);
                        }
                    }
                }

                NNInputDataObj objDataCur = new NNInputDataObj();
                objDataCur.setUpdatedatel(thObjMACD.getUpdateDatel());
                objDataCur.setObj(inputList);
                if (objDataPrev != null) {
                    trInputList.add(objDataPrev.getObj());
                    inputDatalist.add(objDataPrev);
                }

                prevThObj = thObjMACD;
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

        ArrayList<NNInputDataObj> inputRetDatalist = new ArrayList<NNInputDataObj>();
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
                                StockTRHistoryObj thObjMACD = thObjListMACD.get(index);
                                int signalIndex = thObjMACD.getTrsignal();
                                if (signalIndex == signal) {
                                    index = k;
                                    break;
                                }
                            }

                            ProcessNN1 nn1 = new ProcessNN1();
                            inputList = nn1.setupInputNN1(index, signal, thObjListMACD, thObjListMV, thObjListRSI);
                            if (inputList == null) {
                                continue;
                            }
                            inputList.setOutput1(output1);
                            inputList.setOutput2(output2);

                            StockTRHistoryObj thObjMACDIndex = thObjListMACD.get(index);
                            inputDaObj.setUpdatedatel(thObjMACDIndex.getUpdateDatel());
                            inputDaObj.setObj(inputList);
                            inputRetDatalist.add(inputDaObj);
//                                logger.info("> getAccountStockTR MACD NN3 add " + inputDaObj.getObj().getDateSt());

                        }
                    }

                }
            }
        }
        return inputRetDatalist;

    }

    private NNInputOutObj setupInputNN30(int i, int signal, ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI) {
        NNInputOutObj inputList = new NNInputOutObj();
        inputList = TradingNNprocess.getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);
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
        ArrayList<Double> closeArray = TradingNNprocess.getNNnormalizeInputClose(i, thObjListMACD);
        inputList.setInput6(closeArray.get(0));
        inputList.setInput7(closeArray.get(1));
        inputList.setInput8(closeArray.get(2));
        inputList.setInput9(closeArray.get(3));
        inputList.setInput10(closeArray.get(4));

        return inputList;
    }

    // data history from  old to more recent
    // get next 5 days close price
    public static int TREND_Day = 4;

    public static double getNNtrend4OutputClose(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {

        if (thObjListMACD == null) {
            return -1;
        }
        // need to match specialOverrideRule3 futureDay
        int futureDay = TREND_Day;
        int cIndex = index + futureDay;

        if (cIndex >= thObjListMACD.size()) {
            return -1;
        }

        float close = 0;
        close = thObjListMACD.get(index).getClose();
        close += thObjListMACD.get(index - 1).getClose();
        close += thObjListMACD.get(index - 2).getClose();
        float closeOutput0 = close / 3;

        close = thObjListMACD.get(cIndex).getClose();
        close += thObjListMACD.get(cIndex - 1).getClose();
        close += thObjListMACD.get(cIndex - 2).getClose();
        float closeOutput = close / 3;

        double closef = (closeOutput - closeOutput0) / closeOutput0;
        closef = closef * 100;

        closef = closef * 15;   // factore of 15 to make it more valid for NN

        int temp = 0;
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0) {
            if (closef > 0.9) {
                closef = 0.9;
            }
            if (closef < 0.1) {
                closef = 0.1;
            }
        } else {
            if (closef < -0.9) {
                closef = -0.9;
            }
            if (closef > -0.1) {
                closef = -0.1;
            }
        }
        return closef;

    }

}
