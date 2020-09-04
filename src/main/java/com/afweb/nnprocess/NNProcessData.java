/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.util.CKey;
import com.afweb.model.*;
import com.afweb.model.account.AccountObj;
import com.afweb.model.account.TradingRuleObj;

import com.afweb.model.stock.*;
import com.afweb.nn.*;
import com.afweb.service.*;

import com.afweb.signal.*;
import com.afweb.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class NNProcessData {

    public static Logger logger = Logger.getLogger("NNProcess");

    public void processNeuralNet(ServiceAFweb serviceAFWeb) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        boolean flagNeuralnetInput = false;
        if (flagNeuralnetInput == true) {            
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);

        }
        boolean flagNeuralnetTrain = false;
        if (flagNeuralnetTrain == true) {
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
            for (int k = 0; k < 100; k++) {
                NNProcessImp.ProcessTrainNeuralNet(serviceAFWeb);
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

        boolean flagTestNeuralnetTrain = false;
        if (flagTestNeuralnetTrain == true) {
            String symbol = "HOU.TO";

            int nnTRN = ConstantKey.INT_TR_NN1;
            String nnName = ConstantKey.TR_NN1;

            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
//            getStockImp().updateNeuralNetStatus1(BPnameSym, ConstantKey.INITIAL, 0);
//            NNProcessImp.inputReTrainStockNeuralNetData(this, nnTRN, symbol);
            NNProcessImp.inputStockNeuralNetData(serviceAFWeb, nnTRN, symbol);
            NNProcessImp.stockTrainNeuralNet(serviceAFWeb, nnTRN, symbol);
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

    // training neural net input data
    // create neural net input data
    //     
    private void NeuralNetInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        int sizeYr = 2;
        for (int j = 0; j < sizeYr; j++) { //4; j++) {
            int size = 20 * CKey.MONTH_SIZE * j;
//                writeArrayNeuralNet.clear();
            serviceAFWeb.initTrainNeuralNetNumber = j + 1;
            trainingNNdataAll(serviceAFWeb, TR_Name, size);
        }
    }

    public void trainingNNdataAll(ServiceAFweb serviceAFWeb, int tr, int offset) {
        logger.info("> trainingNNdataAll ");
        String symbol = "";
        String symbolL[] = ServiceAFweb.primaryStock;
        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];
            ArrayList<NNInputDataObj> InputList = getTrainingNNdataProcess(serviceAFWeb, symbol, tr, offset);
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
            inputList = nn1.trainingNN1dataMACD1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE + 2);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            nnName = ConstantKey.TR_NN2;
            ProcessNN2 nn2 = new ProcessNN2();
            inputList = nn2.trainingNN2dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE + 2);
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
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
                errorNN = CKey.NN2_ERROR_THRESHOLD;
            }
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
                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
                    retflag = TRprocessImp.TRtrainingNN2NeuralNetData(serviceAFWeb, nnName, errorNN);
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
}
