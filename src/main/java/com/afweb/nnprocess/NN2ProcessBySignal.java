/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.model.ConstantKey;
import com.afweb.model.SymbolNameObj;
import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.signal.*;
import com.afweb.util.*;
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
public class NN2ProcessBySignal {
    public static Logger logger = Logger.getLogger("NN2Process");
///////////////////////////////
    public void processInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////
        boolean flagIntitNN1Input = true;
        if (flagIntitNN1Input == true) {

            TrandingSignalProcess.forceToInitleaningNewNN = true;  // must be true all for init learning             
            TrandingSignalProcess.forceToGenerateNewNN = false;
            logger.info("> processInputNeuralNet TR RSI1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI1);
            logger.info("> processInputNeuralNet TR RSI2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI2);
            // need to debug to generate the java first time
            TrandingSignalProcess.forceToGenerateNewNN = true;

            TrandingSignalProcess.forceToErrorNewNN = true;
//            // start training
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreatJava(serviceAFWeb, ConstantKey.TR_NN2);
//
            TrandingSignalProcess.forceToGenerateNewNN = false;
//            // start training
//            // TrainingNNBP inputpattern 1748
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreatJava(serviceAFWeb, ConstantKey.TR_NN2);
            NeuralNetProcessNN2Testing(serviceAFWeb);
            NeuralNetNN2CreatJava(serviceAFWeb, ConstantKey.TR_NN2);
//            logger.info("> processInputNeuralNet TR NN1 end....... ");

        }
    }

    

    public void processAllNN2StockInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////

        logger.info("> processAllNN2StockInputNeuralNet TR RSI1... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI1);
        logger.info("> processAllNN2StockInputNeuralNet TR RSI2... ");
        NeuralNetAllStockInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI2);
//        
        NeuralNetAllStockNN2CreatJava(serviceAFWeb, ConstantKey.TR_NN2);
        logger.info("> processAllNN2StockInputNeuralNet TR NN2 end....... ");

        ////////////////////////////////////////////
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

    public ArrayList<NNInputDataObj> getTrainingNNdataProcess(ServiceAFweb serviceAFWeb, String NormalizeSym, int tr, int offset) {
        logger.info("> getTrainingNNdataProcess tr_" + tr + " " + NormalizeSym);

        String symbol = NormalizeSym.replace(".", "_");

        int size1yearAll = 20 * 12 * 5 + (50 * 3);
        if (offset == 0) {
            size1yearAll = size1yearAll / 2;
        }

        AFstockObj stockObj = serviceAFWeb.getStockImp().getRealTimeStock(NormalizeSym, null);
        if ((stockObj == null) || (stockObj.getAfstockInfo() == null)) {
            String msg = "> getTrainingNNdataProcess symbol " + symbol + " - null";
            logger.info(msg);

            if (ServiceAFweb.mydebugtestflag == true) {
                return null;
            }
            throw new ArithmeticException(msg);
        }
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_RSI1) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN2 nn2 = new ProcessNN2();
            inputList = nn2.trainingNN2dataRSI1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        } else if (tr == ConstantKey.INT_TR_RSI2) {
            ProcessNN2 nn2 = new ProcessNN2();
            inputList = nn2.trainingNN2dataRSI2(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);

        }
        String nnName = ConstantKey.TR_NN2;
        String BPname = CKey.NN_version + "_" + nnName;

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
            String nn12 = "_nn21_";
            if (tr == ConstantKey.INT_TR_RSI2) {
                nn12 = "_nn22_";
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


    private void NeuralNetProcessNN2Testing(ServiceAFweb serviceAFWeb) {
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
                    String weightSt = (CKey.NN2_WEIGHT_0);
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
                retflag = TRtrainingNN2NeuralNetData(serviceAFWeb, ConstantKey.TR_NN2, nnName, "", errorNN);

                if (retflag == 1) {
                    break;
                }
                logger.info(">>> initTrainNeuralNet " + i);
            }
        }
    }

    public int TRtrainingNN2NeuralNetData(ServiceAFweb serviceAFWeb, String nnName, String nnNameSym, String symbol, double nnError) {
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

        logger.info("> TRtrainingNeuralNet " + BPnameSym + " Statue=" + nnObj1.getStatus() + " Type=" + nnObj1.getType());

        String BPnameTR = CKey.NN_version + "_" + ConstantKey.TR_NN2;

        return TRtrainingNNNeuralNetProcess(serviceAFWeb, BPnameTR, nnName, nnNameSym, symbol, nnError);
    }

    public int TRtrainingNNNeuralNetProcess(ServiceAFweb serviceAFWeb, String BPnameTR, String nnName, String nnNameSym, String symbol, double nnError) {
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
          
                inputDatalist = NeuralNetGetNN2InputfromStaticCode("", subSymbol);

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
                inputDatalist = NeuralNetAllStockGetNN2InputfromStaticCode(symbol, null);

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
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        return TRprocessImp.TrainingNNBP(serviceAFWeb, nnNameSym, nnName, nnTraining, nnError);
    }


    public static ArrayList<NNInputDataObj> NeuralNetGetNN2InputfromStaticCode(String symbol, String subSymbol) {
  
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST1);
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST2);
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST3);
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST4);
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST5);
            inputBuf.append(nn2Data.TR_NN2_INPUTLIST6); //need to check nn2Data file
//            inputBuf.append(nn2Data.TR_NN2_INPUTLIST7);
//            inputBuf.append(nn2Data.TR_NN2_INPUTLIST8);
//            inputBuf.append(nn2Data.TR_NN2_INPUTLIST9); //need to check nn2Data file
//            inputBuf.append(nn2Data.TR_NN2_INPUTLIST10);


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

    public static ArrayList<NNInputDataObj> NeuralNetAllStockGetNN2InputfromStaticCode(String symbol, String subSymbol) {
        StringBuffer inputBuf = new StringBuffer();
        ArrayList<NNInputDataObj> inputlist = new ArrayList();
        try {
            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST1);
            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST2);
            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST3);
            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST4); //need to check nnData file
//            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST5);
//            inputBuf.append(nn2AllData.TR_NN2_ALLINPUTLIST6);


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
            logger.info("> NeuralNetAllStockGetNN2InputfromStaticCode - exception " + ex);
        }
        return null;
    }
    
    public boolean NeuralNetNN2CreatJava(ServiceAFweb serviceAFWeb, String nnName) {
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
                    + "public class nn2Data {\n"
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
            fileN = ServiceAFweb.FileLocalDebugPath + "nn2Data.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public boolean NeuralNetAllStockNN2CreatJava(ServiceAFweb serviceAFWeb, String nnName) {
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
                    + "public class nn2AllData {\n"
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
            String fileN = ServiceAFweb.FileLocalDebugPath + "nn2AllData.java";
            FileUtil.FileWriteText(fileN, msgWrite);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

}
