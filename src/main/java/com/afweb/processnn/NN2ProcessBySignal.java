/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.model.nn.*;

import com.afweb.processnn.model.*;
import com.afweb.processsignal.TradingSignalProcess;
import com.afweb.model.ConstantKey;
import com.afweb.model.SymbolNameObj;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;

import com.afweb.nnBP.NNBPservice;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class NN2ProcessBySignal {

    public static Logger logger = Logger.getLogger("NN2Process");
    NNetService nnservice = new NNetService();
///////////////////////////////

    public void processNN2InputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////
        boolean flagIntitNN1Input = true;
        if (flagIntitNN1Input == true) {

            TradingSignalProcess.forceToInitleaningNewNN = true;  // must be true all for init learning             
            TradingSignalProcess.forceToGenerateNewNN = false;
//            logger.info("> processInputNeuralNet TR ADX1... ");
//            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_ADX1);
//            logger.info("> processInputNeuralNet TR ADX2... ");
//            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_ADX2);

            logger.info("> processInputNeuralNet TR EMA1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_EMA1);
            logger.info("> processInputNeuralNet TR EMA2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_EMA2);
            // need to debug to generate the java first time
            TradingSignalProcess.forceToGenerateNewNN = true;

            TradingSignalProcess.forceToErrorNewNN = true;
//            // start training
            NeuralNetNN2CreateDB(serviceAFWeb, ConstantKey.TR_NN2);
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreateJava(serviceAFWeb, ConstantKey.TR_NN2);

            TradingSignalProcess.forceToGenerateNewNN = false;
//            // start training
//            // TrainingNNBP inputpattern 1748
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreateJava(serviceAFWeb, ConstantKey.TR_NN2);
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreateJava(serviceAFWeb, ConstantKey.TR_NN2);
//            logger.info("> processInputNeuralNet TR NN1 end....... ");

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
            logger.info("> initTrainNeuralNetNumber tr_" + TR_Name + " " + serviceAFWeb.initTrainNeuralNetNumber);

            String symbol = "";
            String symbolL[] = ServiceAFweb.primaryStock;
            for (int i = 0; i < symbolL.length; i++) {
                symbol = symbolL[i];
                ArrayList<NNInputDataObj> InputList = getTrainingNN2dataProcess(serviceAFWeb, symbol, TR_Name, size);
            }
        }
    }

//    private void NeuralNetAllStockInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
//        int sizeYr = 2;
//        for (int j = 0; j < sizeYr; j++) { //4; j++) {
//            int size = 20 * CKey.MONTH_SIZE * j;
////                writeArrayNeuralNet.clear();
//            serviceAFWeb.initTrainNeuralNetNumber = j + 1;
//            logger.info("> NeuralNetAllStockInputTesting tr_" + TR_Name + " " + serviceAFWeb.initTrainNeuralNetNumber);
//
//            String symbol = "";
//            String symbolL[] = ServiceAFweb.allStock;
//            for (int i = 0; i < symbolL.length; i++) {
//                symbol = symbolL[i];
//                ArrayList<NNInputDataObj> InputList = getTrainingNN2dataProcess(serviceAFWeb, symbol, TR_Name, size);
//            }
//        }
//    }

    public ArrayList<NNInputDataObj> getTrainingNN2dataProcess(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> getTrainingNNdataProcess tr_" + tr + " " + NormalizeSym);

        SymbolNameObj symObj = new SymbolNameObj(symbol);
        String NormalizeSymbol = symObj.getYahooSymbol();

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        if (offset == 0) {
            size1yearAll = size1yearAll / 2;
        }

        AFstockObj stockObj = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
        if ((stockObj == null) || (stockObj.getAfstockInfo() == null)) {
            String msg = "> getTrainingNNdataProcess symbol " + NormalizeSymbol + " - null";
            logger.info(msg);

            if (ServiceAFweb.mydebugtestflag == true) {
                return null;
            }
            throw new ArithmeticException(msg);
        }
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.InfGetStockHistorical(NormalizeSymbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

//        if (tr == ConstantKey.INT_TR_ADX1) {
        if (tr == ConstantKey.INT_TR_EMA2) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN2 NN2 = new ProcessNN2();
//            inputList = NN2.trainingNN2dataADX1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
            inputList = NN2.trainingNN2dataEMA2(serviceAFWeb, NormalizeSymbol, StockArray, offset, CKey.MONTH_SIZE);
//        } else if (tr == ConstantKey.INT_TR_ADX2) {
        } else if (tr == ConstantKey.INT_TR_EMA1) {
            ProcessNN2 NN2 = new ProcessNN2();
//            inputList = NN2.trainingNN2dataADX2(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
            inputList = NN2.trainingNN2dataEMA1(serviceAFWeb, NormalizeSymbol, StockArray, offset, CKey.MONTH_SIZE);

        }
        String nnName = ConstantKey.TR_NN2;
        String BPname = CKey.NN_version + "_" + nnName;

        // ignor first and last
        int len = inputList.size();
        if (len <= 2) {
            return null;
        }

        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(NormalizeSymbol);
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
                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "adx"
                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
                        + "\",\"" + NormalizeSymbol + "\"";

            }
            String stDispaly = st.replaceAll("\"", "");
            writeArray.add(stDispaly);
        }
        writeArray.add(stTitle.replaceAll("\"", ""));

        Collections.reverse(writeArray);
        Collections.reverse(inputList);

        if (getEnv.checkLocalPC() == true) {
            String NN21 = TradingSignalProcess.NN2_FILE_1; //"_NN21_";
//            if (tr == ConstantKey.INT_TR_ADX2) {
            if (tr == ConstantKey.INT_TR_EMA2) {
                NN21 = TradingSignalProcess.NN2_FILE_2; //"_NN22_";
            }
            String filename = ServiceAFweb.FileLocalDebugPath + NormalizeSymbol + NN21 + ServiceAFweb.initTrainNeuralNetNumber + ".csv";

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
                ArrayList<AFneuralNetData> objList = serviceAFWeb.NnGetNeuralNetDataObjByStockId(BPname, NormalizeSymbol, stockId, objData.getUpdatedatel());
                if ((objList == null) || (objList.size() == 0)) {
                    serviceAFWeb.NnAddNeuralNetDataObject(BPname, NormalizeSymbol, stockId, objData);
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

    public void NeuralNetProcessNN2Testing(ServiceAFweb serviceAFWeb) {
        ///////////////////////////////////////////////////////////////////////////////////
        // read new NN data
        serviceAFWeb.forceNNReadFileflag = true; // should be true to get it from file instead from db

        boolean initTrainNeuralNet = true;
        if (initTrainNeuralNet == true) {

            double errorNN = CKey.NN2_ERROR_THRESHOLD;
            String nnName = ConstantKey.TR_NN2;
            String BPname = CKey.NN_version + "_" + nnName;

            boolean flagInit = true;
            if (flagInit == true) {
                AFneuralNet afNeuralNet = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPname, 0);
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
                    String weightSt = (CKey.NN2_WEIGHT_0);
                    afNeuralNet.setWeight(weightSt);

//                    String refname = CKey.NN_version + "_" + ConstantKey.TR_NN200;
//                    serviceAFWeb.setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
                    nnservice.setNeuralNetObjWeight1(serviceAFWeb, afNeuralNet);
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using NN2_WEIGHT_0");
                } else {
                    String weightSt = afNeuralNet.getWeight();
                    if ((weightSt == null) || (weightSt.length() == 0)) {
                        AFneuralNet afNeuralNet0 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPname, 0);
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
//                            serviceAFWeb.setCreateNeuralNetObjSameObj1(BPname, refname, weightSt);
                            nnservice.setNeuralNetObjWeight1(serviceAFWeb, afNeuralNet);
                        }
                    }
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using DB");
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                retflag = TrainingNN2NeuralNetData(serviceAFWeb, ConstantKey.TR_NN2, nnName, "", errorNN);

                if (retflag == 1) {
                    break;
                }
                logger.info(">>> initTrainNeuralNet " + i);
            }
        }
    }

    public int TrainingNN2NeuralNetData(ServiceAFweb serviceAFWeb, String nnName, String nnNameSym, String symbol, double nnError) {
        String BPnameSym = CKey.NN_version + "_" + nnNameSym;

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release        
        AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPnameSym, 0);
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

        logger.info("> TRtrainingNeuralNet " + BPnameSym + " Statue=" + nnObj1.getStatus() + " Type=" + nnObj1.getType());

        String BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN2;

        return TrainingNNNeuralNetProcess(serviceAFWeb, BPnameTR, nnName, nnNameSym, symbol, nnError);
    }

    public int TrainingNNNeuralNetProcess(ServiceAFweb serviceAFWeb, String BPnameTR, String nnName, String nnNameSym, String symbol, double nnError) {
        ServiceAFweb.lastfun = "TrainingNNNeuralNetProcess";

        String BPnameSym = CKey.NN_version + "_" + nnNameSym;
        ArrayList<NNInputOutObj> inputlist = new ArrayList();

        //must set ot reading DB
        ServiceAFweb.forceNNReadFileflag = false;
        //must set ot reading DB
        ArrayList<NNInputDataObj> inputDatalist = new ArrayList();
        if (ServiceAFweb.forceNNReadFileflag == true) {

        } else {
            /// new stock difficult to train need to remove the T.TO to see if it helps
            String subSymbol = null;
//            if (symbol.length() != 0) {
//                subSymbol = "RY.TO";
//                for (int i = 0; i < ServiceAFweb.primaryStock.length; i++) {
//                    String stockN = ServiceAFweb.primaryStock[i];
//                    if (stockN.equals(symbol)) {
//                        subSymbol = null;
//                        break;
//                    }
//                }
//            }

            inputDatalist = GetNN2InputBasefromDB(serviceAFWeb, "", subSymbol, nnName);

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

            inputDatalist = GetNN2InputOtherfromDB(serviceAFWeb, "", subSymbol, nnName);
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

            ArrayList<AFneuralNetData> objDataList = new ArrayList();

            if (BPnameTR.equals(BPnameSym)) {
                ;
            } else {
                objDataList = serviceAFWeb.NnNeuralNetDataObjSystem(BPnameSym);
                if (objDataList != null) {
                    logger.info("> TRtrainingNNNeuralNetProcess " + BPnameSym + " " + inputlist.size() + " " + objDataList.size());
                    for (int i = 0; i < objDataList.size(); i++) {
                        String dataSt = objDataList.get(i).getData();
                        NNInputOutObj input;
                        try {
                            input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                            inputlist.add(input);
                        } catch (IOException ex) {
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
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        return TRprocessImp.TrainingNNBP(serviceAFWeb, nnNameSym, nnName, nnTraining, nnError);
    }

    public ArrayList<NNInputDataObj> GetNN2InputBasefromDB(ServiceAFweb serviceAFWeb, String symbol, String subSymbol, String nnName) {

        ArrayList<NNInputDataObj> inputlist = new ArrayList();

        TradingNNData nndata = new TradingNNData();
        nndata.getNNBaseDataDB(serviceAFWeb, nnName, inputlist);
        return inputlist;
    }

    public ArrayList<NNInputDataObj> GetNN2InputOtherfromDB(ServiceAFweb serviceAFWeb, String symbol, String subSymbol, String nnName) {

        ArrayList<NNInputDataObj> inputlist = new ArrayList();

        TradingNNData nndata = new TradingNNData();
        nndata.getNNOtherDataDB(serviceAFWeb, nnName, inputlist);
        return inputlist;

    }

    public boolean NeuralNetNN2CreateDB(ServiceAFweb serviceAFWeb, String nnName) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        logger.info("> NeuralNetNN2CreatJavaDB ");
        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, nnName, stockInputMap);

        TradingNNData nndata = new TradingNNData();
        nndata.saveNNBaseDataDB(serviceAFWeb, nnName, stockInputMap);
        return true;

    }

    public boolean NeuralNetNN2CreateJava(ServiceAFweb serviceAFWeb, String nnName) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, nnName, stockInputMap);
            TradingNNData nndata = new TradingNNData();
            nndata.saveNNBaseDataDB(serviceAFWeb, nnName, stockInputMap);

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
                    + "public class NN2Data {\n"
                    + "\n"
                    + "    public static String " + nnName + "_WEIGHT_0 = \"\"\n");
            int sizeline = 1000;
            int len = weightSt.length();
            ////// ignore the /n at the end - inserted by FileReadText 
            len = len - 2;
            ////// ignore the /n at the end - inserted by FileReadText  

            int beg = 0;

            int end = sizeline;

            if (end <= len) {
                ;
            } else {
                end = len;
            }
            while (true) {
                String st = weightSt.substring(beg, end);
                st = "+ \"" + st + "\"\n";
                msgWrite.append(st);
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

            msgWrite.append(""
                    + "}\n"
                    ///
                    + ""
            );
            fileN = ServiceAFweb.FileLocalDebugPath + "NN2Data.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

//////////////////////////////////////////////////
    private static ArrayList stockNNprocessNameArray = new ArrayList();
    private static ArrayList stockNNinputNameArray = new ArrayList();

    private ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb, AccountObj accountObj) {
        if (stockNNprocessNameArray != null && stockNNprocessNameArray.size() > 0) {
            return stockNNprocessNameArray;
        }

        ArrayList stockNameArray = serviceAFWeb.InfGetStockINfioNameList(accountObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            ArrayList stockTRNameArray = new ArrayList();
            for (int i = 0; i < stockNameArray.size(); i++) {
                String sym = (String) stockNameArray.get(i);

                if (ServiceAFweb.mydebugtestNN3flag == true) {
                    if (ServiceAFweb.SysCheckSymbolDebugTest(sym) == false) {
                        continue;
                    }
                }

                String symTR = sym + "#" + ConstantKey.INT_TR_NN2;
                stockTRNameArray.add(symTR);
            }

            stockNNprocessNameArray = stockTRNameArray;
        }
        return stockNNprocessNameArray;
    }

    public void ProcessTrainNN2NeuralNetBySign(ServiceAFweb serviceAFWeb) {

        AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
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
        logger.info("ProcessTrainNN2NeuralNetBySign " + printName);

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NN2_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.InfoSetLockName(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }

//        logger.info("ProcessTrainNeuralNet " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long LastServUpdateTimer = System.currentTimeMillis();
            int timeout = 15;
            if (ServiceAFweb.processNeuralNetFlag == true) {
                timeout = timeout * 10;
            }
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, timeout); // add 3 minutes

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

                String[] symbolArray = symbolTR.split("#");
                if (symbolArray.length >= 0) {

                    String symbol = symbolArray[0];
                    //////////////////////
                    // just for testing
//                    if (ServiceAFweb.mydebugtestflag == true) {
//                        symbol = "HOU.TO";
//                    }
                    int TR_NN = Integer.parseInt(symbolArray[1]);  // assume TR_NN1

                    AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(symbol);

                    if (stock == null) {
                        stockNNprocessNameArray.remove(0);
                        continue;
                    }
                    if (stock.getAfstockInfo() == null) {
                        stockNNprocessNameArray.remove(0);
                        continue;
                    }
                    this.TrainNN2NeuralNetBySign(serviceAFWeb, symbol, TR_NN, stockNNprocessNameArray);

                }
            }  // end for loop
            serviceAFWeb.InfoRemoveLockName(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
        logger.info("> ProcessTrainNN2NeuralNetBySign ... done");
    }

    public void TrainNN2NeuralNetBySign(ServiceAFweb serviceAFWeb, String symbol, int TR_NN, ArrayList stockNNprocessNameArray) {
        String LockStock = "NN2_TR_" + symbol; // + "_" + trNN;
        LockStock = LockStock.toUpperCase();

        long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
        long lockReturnStock = 1;

        lockReturnStock = serviceAFWeb.InfoSetLockName(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");

//      logger.info("ProcessTrainNeuralNet " + LockStock + " LockStock " + lockReturnStock);
        if (lockReturnStock == 0) {
            if (stockNNprocessNameArray != null) {
                stockNNprocessNameArray.remove(0);
            }
            return;
        }

        if (lockReturnStock > 0) {
            try {
                String nnName = ConstantKey.TR_NN2;
                String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;

                AFneuralNet nnObj1 = ProcessTrainSignalNeuralNet(serviceAFWeb, BPnameSym, TR_NN, symbol);

                if (nnObj1 != null) {
                    if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
                        if (stockNNprocessNameArray != null) {
                            stockNNprocessNameArray.remove(0);
                        }
                        serviceAFWeb.NnDeleteNeuralNet1(BPnameSym);

//                                    if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
//                                        /// need to create the table to reduce the memeory in DB
//                                        serviceAFWeb.deleteNeuralNet1Table();
//                                    } else {
//                                        serviceAFWeb.deleteNeuralNet1(BPnameSym);
//                                    }
                    }
                }

            } catch (Exception ex) {
                logger.info("> ProcessTrainNN2NeuralNetBySign Exception " + ex.getMessage());
            }
            serviceAFWeb.InfoRemoveLockName(LockStock, ConstantKey.NN_TR_LOCKTYPE);
//                        logger.info("ProcessTrainNeuralNet " + LockStock + " unLock LockStock ");
        }
    }

    public AFneuralNet ProcessTrainSignalNeuralNet(ServiceAFweb serviceAFWeb, String BPnameSym, int TR_NN, String symbol) {
        this.Process1TrainNeuralNet(serviceAFWeb, TR_NN, BPnameSym, symbol);
        // first one is initial and the second one is to execute
        this.Process1TrainNeuralNet(serviceAFWeb, TR_NN, BPnameSym, symbol);

        AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPnameSym, 0);
        return nnObj1;
    }

    private void Process1TrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String BPnameSym, String symbol) {

        AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPnameSym, 0);
        if (nnObj1 == null) {
            inputStockNeuralNetBySignal(serviceAFWeb, TR_NN, symbol);
            return;
        }
        if (nnObj1.getStatus() == ConstantKey.INITIAL) {
            inputStockNeuralNetBySignal(serviceAFWeb, TR_NN, symbol);
        }
        if (nnObj1.getStatus() == ConstantKey.COMPLETED) {
//            stockNNprocessNameArray.remove(0);
            return;
        }
        stockTrainNeuralNet(serviceAFWeb, TR_NN, symbol);
    }

    public int inputStockNeuralNetBySignal(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        boolean nnsym = true;
        if (nnsym == true) {
            int totalAdd = 0;
            int totalDup = 0;
            String nnName = ConstantKey.TR_NN2;
            String nnNameSym = nnName + "_" + symbol;
            String BPnameSym = CKey.NN_version + "_" + nnNameSym;
            try {
                AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPnameSym, 0);
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
                String nnWeight = CKey.NN2_WEIGHT_0;
                String nnCreateSt = "Static Src Code";
                if (TR_Name == ConstantKey.INT_TR_NN2) {
                    /////try to use DB first
                    String BPnameBase = CKey.NN_version + "_" + nnName;
                    AFneuralNet afNeuralNetBase = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameBase, 0);
                    if (afNeuralNetBase != null) {
                        String weigthDBbase = afNeuralNetBase.getWeight();
                        if (weigthDBbase.length() != 0) {
                            nnWeight = weigthDBbase;
                            nnCreateSt = "Static DB";
                        }
                    }
                    if (nnWeight.length() == 0) {
                        return 0;
                    }
                    nnTemp.createNet(nnWeight);
                    String[] strNetArray = nnWeight.split(";");
                    version = strNetArray[0];
                    middlelayer = strNetArray[4];
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
                    inputlistSym1 = getTrainingNN2dataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_EMA1, 0);
                    inputlistSym2 = getTrainingNN2dataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_EMA2, 0);
                }
                inputlistSym.addAll(inputlistSym1);
                inputlistSym.addAll(inputlistSym2);

//                ArrayList<NNInputDataObj> inputL = new ArrayList();
//                inputL = GetNN2InputfromStaticCode(serviceAFWeb, symbol, null, nnName);
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
                        ArrayList<AFneuralNetData> objList = serviceAFWeb.NnGetNeuralNetDataObjByStockId(BPnameSym, "", 0, objData.getUpdatedatel());
                        if ((objList == null) || (objList.size() == 0)) {
                            serviceAFWeb.NnAddNeuralNetDataObject(BPnameSym, "", 0, objData);
                            totalAdd++;
                            continue;
                        }
                        totalDup++;

//                        if (CKey.NN_DEBUG == true) {
////                            logger.info("> inputStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
//                        }
                    }
                }

                ReferNameData refData = new ReferNameData();
//                String refName = "";
                AFneuralNet nnObj0 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
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
//                            logger.info("> inputStockNeuralNetData create existing Symbol ");
                            //just for testing                           
                            nnTemp.createNet(stWeight0);
                            refData = nnservice.getReferNameData(nnObj0);
                            nnCreateSt = "Existing symbol DB";
//                            refName = nnObj0.getRefname();
                        } else {

                        }
                    }
                } else {

                }
                logger.info(">>>>>>>> inputStockNeuralNetData create - " + nnCreateSt);
                logger.info("> inputStockNeuralNetData v" + version + " " + middlelayer + " " + nnName + " " + BPnameSym + "  toAdd=" + totalAdd + " toDup=" + totalDup);

                String weightSt = nnTemp.getNetObjSt();
                int ret = serviceAFWeb.NnCreateNeuralNetObj1(BPnameSym, weightSt);
                if (refData.getmError() != 0) {
                    logger.info("> inputStockNeuralNet  " + BPnameSym + " refMinError " + refData.getmError());
                    serviceAFWeb.NnUpdateNeuralNetRef1(BPnameSym, refData);
                }
//                if (refName != null) {
//                    if (refName.length() > 0) {
//                        // just for testing
////                    refName = "" + CKey.NN1_ERROR_THRESHOLD;
//                        logger.info("> inputStockNeuralNet  " + BPnameSym + " refError " + refName);
//                        serviceAFWeb.updateNeuralNetRef1(BPnameSym, refName);
//                    }
//                }

//                logger.info("> inputStockNeuralNet " + BPnameSym + " inputlist=" + inputlist.size() + " ...Done");
                return ret;

            } catch (Exception e) {
                logger.info("> inputStockNeuralNet exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;

    }

    public int stockTrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String symbol) {
        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
//        logger.info("> processStockNeuralNet " + TR_Name + " " + symbol);

        boolean nnsymTrain = true;
        if (nnsymTrain == true) {
            String nnName = ConstantKey.TR_NN2;
            double errorNN = CKey.NN2_ERROR_THRESHOLD;

            String nnNameSym = nnName + "_" + symbol;
            String BPname = CKey.NN_version + "_" + nnNameSym;
            try {

                AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight1(serviceAFWeb, BPname, 0);
                if (nnObj1 != null) {
                    if (nnObj1.getStatus() != ConstantKey.OPEN) {
                        return -1;
                    }
                }
                ReferNameData refData = new ReferNameData();
                refData = nnservice.getReferNameData(nnObj1);
                if (refData.getmError() != 0) {
                    errorNN = refData.getmError();
//                    logger.info("> stockTrainNeuralNet override new error " + BPname + " " + errorNN);
                }
//                String refName = nnObj1.getRefname();
//                if (refName.length() > 0) {
//                    try {
//                        double refError = Double.parseDouble(refName);
//                        errorNN = refError + 0.0001;
//                        logger.info("> stockTrainNeuralNet override new error " + BPname + " " + errorNN);
//                    } catch (Exception ex) {
//
//                    }
//                }
                int retflag = 0;
                if (TR_NN == ConstantKey.INT_TR_NN2) {
                    retflag = TrainingNN2NeuralNetData(serviceAFWeb, ConstantKey.TR_NN2, nnNameSym, symbol, errorNN);
                }
//                logger.info("> processStockNeuralNet ... Done");
                return retflag;
            } catch (Exception e) {
                logger.info("> stockTrainNeuralNet exception " + BPname + " - " + e.getMessage());
            }
        }
        return -1;
    }

    public ArrayList<NNInputDataObj> getTrainingNN2dataStock(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        int size1yearAll = 20 * 12 * 2 + (50 * 3);

//        logger.info("> trainingNN " + symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.InfGetStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;
//        if (tr == ConstantKey.INT_TR_ADX1) {
        if (tr == ConstantKey.INT_TR_EMA2) {

            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN2 NN2 = new ProcessNN2();
//            inputList = NN2.trainingNN2dataADX1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);            
            inputList = NN2.trainingNN2dataEMA2(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
//        } else if (tr == ConstantKey.INT_TR_ADX2) {
        } else if (tr == ConstantKey.INT_TR_EMA1) {
            ProcessNN2 NN2 = new ProcessNN2();
//            inputList = NN2.trainingNN2dataADX2(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
            inputList = NN2.trainingNN2dataEMA1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);

        }

        // ignor first and last
        int len = inputList.size();
        if (len <= 2) {
            return null;
        }
        inputList.remove(len - 1);
        inputList.remove(0);

        return inputList;
    }

    public int ReLearnNN2StockNeuralNetData(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        ServiceAFweb.lastfun = "ReLearnNN2StockNeuralNetData";
        boolean nnsym = true;
        if (nnsym == true) {
            int totalAdd = 0;
            int totalDup = 0;
            String nnName = ConstantKey.TR_NN2;

            if (checkNN2Ready(serviceAFWeb, symbol, false) == false) {
                return 0;
            }

            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            try {
                AFneuralNet nnObj0 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
                if (nnObj0 == null) {
                    return 0;
                }

                logger.info("> ReLearnNN2StockNeuralNetData " + BPnameSym);

                ArrayList<NNInputOutObj> inputlist = new ArrayList();

                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();

                int size1yearAll = 20 * 12 * 5 + (50 * 3);
                ArrayList<AFstockInfo> StockArray = serviceAFWeb.InfGetStockHistorical(symbol, size1yearAll);
                //StockArray assume recent date to old data 
                ArrayList<NNInputDataObj> inputlistSym2 = getReTrainingNN2dataStockReTrain(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, StockArray, 0);
                inputlistSym.addAll(inputlistSym2);

                // cannot change base
//                ArrayList<NNInputDataObj> inputL = new ArrayList();
//
//                inputL = GetNN1InputBasefromDB(serviceAFWeb, symbol, null, nnName);
//                if (inputL != null) {
//                    if (inputL.size() > 0) {
//                        for (int k = 0; k < inputL.size(); k++) {
//                            NNInputDataObj inputLObj = inputL.get(k);
//                            for (int m = 0; m < inputlistSym.size(); m++) {
//                                NNInputDataObj inputSymObj = inputlistSym.get(m);
//
//                                String longSt = inputLObj.getUpdatedatel() + "";
//                                if (inputLObj.getUpdatedatel() == inputSymObj.getUpdatedatel()) {
//                                    if (inputLObj.getObj().getOutput1() == inputSymObj.getObj().getOutput1()) {
//                                        if (inputLObj.getObj().getOutput2() == inputSymObj.getObj().getOutput2()) {
//                                            inputlistSym.remove(m);
//                                            totalDup++;
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
                ArrayList writeArray = new ArrayList();

                if (inputlistSym != null) {
                    //merge inputlistSym

                    for (int i = 0; i < inputlistSym.size(); i++) {
                        NNInputOutObj inputObj = inputlistSym.get(i).getObj();

                        String nameST = new ObjectMapper().writeValueAsString(inputObj);
                        writeArray.add(nameST);

                        inputlist.add(inputObj);
                        // save into db
                        // save into db
                        // save into db
                        // save into db
                        NNInputDataObj objData = inputlistSym.get(i);

                        ArrayList<AFneuralNetData> objList = serviceAFWeb.NnGetNeuralNetDataObjByStockId(BPnameSym, "", 0, objData.getUpdatedatel());
                        if ((objList == null) || (objList.size() == 0)) {
                            ;
                        } else {

                            AFneuralNetData nnData = objList.get(0);
                            String dataSt = nnData.getData();
                            NNInputOutObj input;
                            input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);

                            if (input.getOutput1() == objData.getObj().getOutput1()) {
                                if (input.getOutput2() == objData.getObj().getOutput2()) {
                                    totalDup++;
                                    continue;

                                }
                            }

                            serviceAFWeb.NnDeleteNeuralNetDataObjById(nnData.getId());
//                            if (ServiceAFweb.mydebugtestflag == true) {
//                                try {
//
//                                    String objListSt = new ObjectMapper().writeValueAsString(objList);
//                                    System.out.println("objListSt:" + objListSt);
//                                    String objDataSt = new ObjectMapper().writeValueAsString(objData);
//                                    System.out.println("objDataSt:" + objDataSt);
//                                } catch (Exception ex) {
//
//                                }
//                            }

                        }
                        serviceAFWeb.NnAddNeuralNetDataObject(BPnameSym, "", 0, objData);
                        totalAdd++;
                        writeArray.add(nameST);
                        continue;
                    }
                }
                // redue multiple task update the same ref condition
                nnObj0 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
                ReferNameData refData = nnservice.getReferNameData(nnObj0);
                int cnt = refData.getnRLCnt();
                if (cnt < 0) {
                    cnt = 0;
                }
                if (cnt < 90) {
                    cnt += 1;
                }
                refData.setnRLCnt(cnt);
                refData.setnRLearn(totalAdd);
                serviceAFWeb.NnUpdateNeuralNetRef0(BPnameSym, refData);

                logger.info("> ReLearnNN2StockNeuralNetData Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);

//                if (getEnv.checkLocalPC() == true) {
//                    boolean flag = false;
//                    if (flag == true) {
//                        String NN12 = "_NN1_retarin_";
//                        String filename = ServiceAFweb.FileLocalDebugPath + symbol + NN12 + ".csv";
//                        FileUtil.FileWriteTextArray(filename, writeArray);
//                    }
//                }
                return 1;
            } catch (Exception e) {
                logger.info("> ReLearnNN2StockNeuralNetData exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;
    }

//    public int ReLearnNN2StockNeuralNetData(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
//        ServiceAFweb.lastfun = "ReLearnNN2StockNeuralNetData";
//
//        boolean nnsym = true;
//        if (nnsym == true) {
//            int totalAdd = 0;
//            int totalDup = 0;
//            String nnName = ConstantKey.TR_NN2;
//
//            if (checkNN2Ready(serviceAFWeb, symbol, false) == false) {
//                return 0;
//            }
//
//            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            try {
//                AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPnameSym, 0);
//                if (nnObj0 == null) {
//                    return 0;
//                }
//
//                logger.info("> inputReTrainStockNeuralNetData " + BPnameSym);
//
//                ArrayList<NNInputOutObj> inputlist = new ArrayList();
//
//                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
//                int size1yearAll = 20 * 12 * 5 + (50 * 3);
//
//                ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
//                //StockArray assume recent date to old data   
//                ArrayList<NNInputDataObj> inputlistSym2 = getReTrainingNN2dataStockReTrain(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, StockArray, 0);
//                inputlistSym.addAll(inputlistSym2);
//
//                ArrayList<NNInputDataObj> inputL = new ArrayList();
//                inputL = GetNN2InputBasefromDB(serviceAFWeb, symbol, null, nnName);
//                if (inputL != null) {
//                    if (inputL.size() > 0) {
//                        for (int k = 0; k < inputL.size(); k++) {
//                            NNInputDataObj inputLObj = inputL.get(k);
//                            for (int m = 0; m < inputlistSym.size(); m++) {
//                                NNInputDataObj inputSymObj = inputlistSym.get(m);
//                                if (inputLObj.getUpdatedatel() == inputSymObj.getUpdatedatel()) {
//                                    if (inputLObj.getObj().getOutput1() == inputSymObj.getObj().getOutput1()) {
//                                        if (inputLObj.getObj().getOutput2() == inputSymObj.getObj().getOutput2()) {
//                                            inputlistSym.remove(m);
//                                            totalDup++;
//                                            break;
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//
//                ArrayList writeArray = new ArrayList();
//
//                if (inputlistSym != null) {
//                    //merge inputlistSym
//
//                    for (int i = 0; i < inputlistSym.size(); i++) {
//                        NNInputOutObj inputObj = inputlistSym.get(i).getObj();
//
//                        String nameST = new ObjectMapper().writeValueAsString(inputObj);
//                        writeArray.add(nameST);
//
//                        inputlist.add(inputObj);
//                        // save into db
//                        // save into db
//                        // save into db
//                        // save into db
//                        NNInputDataObj objData = inputlistSym.get(i);
//                        ArrayList<AFneuralNetData> objList = serviceAFWeb.getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
//                        if ((objList == null) || (objList.size() == 0)) {
//                            ;
//                        } else {
//                            AFneuralNetData nnData = objList.get(0);
//                            serviceAFWeb.deleteNeuralNetDataObjById(nnData.getId());
//
//                        }
//                        serviceAFWeb.updateNeuralNetDataObject(BPnameSym, 0, objData);
//                        totalAdd++;
//                        writeArray.add(nameST);
//                        continue;
//                    }
//                }
//
//                // redue multiple task update the same ref condition
//                nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPnameSym, 0);
//                ReferNameData refData = serviceAFWeb.getReferNameData(nnObj0);
//                int cnt = refData.getnRLCnt();
//                if (cnt < 0) {
//                    cnt = 0;
//                }
//                if (cnt < 90) {
//                    cnt += 1;
//                }
//                refData.setnRLCnt(cnt);
//                refData.setnRLearn(totalAdd);
//                serviceAFWeb.updateNeuralNetRef0(BPnameSym, refData);
//
//                logger.info("> inputReTrainStockNeuralNetData Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
//
//                if (getEnv.checkLocalPC() == true) {
//                    boolean flag = false;
//                    if (flag == true) {
//                        String nn12 = "_NN2_retarin_";
//                        String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + ".csv";
//                        FileUtil.FileWriteTextArray(filename, writeArray);
//                    }
//                }
//                return 1;
//            } catch (Exception e) {
//                logger.info("> inputReTrainStockNeuralNetData exception " + BPnameSym + " - " + e.getMessage());
//            }
//        }
//        return -1;
//    }
    public ArrayList<NNInputDataObj> getReTrainingNN2dataStockReTrain(ServiceAFweb serviceAFWeb, String symbol, int tr, ArrayList<AFstockInfo> StockArray, int offset) {
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;

        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_NN2) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date    
            ProcessNN2 NN2 = new ProcessNN2();
            inputList = NN2.RetrainingNN2dataReTrain(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        }

        // ignor first and last
        int len = inputList.size();
        if (len <= 2) {
            return null;
        }
        inputList.remove(len - 1);
        inputList.remove(0);

        return inputList;
    }

    public boolean checkNN2Ready(ServiceAFweb serviceAFWeb, String symbol, boolean CheckRefData) {
        TradingSignalProcess TSproc = new TradingSignalProcess();
        AFneuralNet nnObj0 = TSproc.testNeuralNet0Symbol(serviceAFWeb, ConstantKey.TR_NN2, symbol);
        if (nnObj0 == null) {
            return false;
        }
        if (nnObj0.getStatus() != ConstantKey.OPEN) {
            return false;
        }

        if (CheckRefData == true) {
            ReferNameData refData = nnservice.getReferNameData(nnObj0);
            int numReLearn = refData.getnRLearn();
            if (numReLearn == -1) {
                return false;
            }
            if (numReLearn > 4) {
                return false;
            }
            if (refData.getnRLCnt() < 4) {
                return false;
            }

        }
//        nnObj0 = testNeuralNet0Symbol(serviceAFWeb, ConstantKey.TR_NN40, symbol);
        nnObj0 = TSproc.testNeuralNet0Symbol(serviceAFWeb, ConstantKey.TR_NN30, symbol);
        if (nnObj0 == null) {
            return false;
        }
        if (nnObj0.getStatus() != ConstantKey.OPEN) {
            return false;
        }
        return true;
    }

}
