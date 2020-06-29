/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.util.CKey;
import com.afweb.model.*;
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
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        boolean flagNeuralnetInput = false;
        if (flagNeuralnetInput == true) {
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_NN2);

        }
        boolean flagNeuralnetTrain = false;
        if (flagNeuralnetTrain == true) {
            // start training
            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);

        }
        boolean flagNeuralnetCreateJava = false;
        if (flagNeuralnetCreateJava == true) {
            NeuralNetCreatJava(serviceAFWeb);

        }
        boolean flaginpTrainData = false;
        if (flaginpTrainData == true) {
            initTrainingNeuralNetFile(serviceAFWeb, ConstantKey.INT_TR_NN1);
        }

//        boolean retrainupdateinputflag = false;
//        if (retrainupdateinputflag == true) {
//            updateDBneuralnetDataProcess(serviceAFWeb);
//
//        }
        ///////////////////////////////////////////////////////////////////////////////////   
        ///////////////////////////////////////////////////////////////////////////////////   
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
    }

    // training neural net input data
    // create neural net input data
    //     
    private void NeuralNetInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        int sizeYr = 3;
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
//    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};
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
                        + "\",\"" + "output"
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
                    logger.info(">>> NeuralNetProcessTesting " + BPname + " using DB");
                }
            }

            for (int i = 0; i < 20; i++) {
                int retflag = 0;
                if (TR_Name == ConstantKey.INT_TR_NN1) {
                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, nnName, errorNN);
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

    private boolean NeuralNetCreatJava(ServiceAFweb serviceAFWeb) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();

        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {
            TRprocessImp.getStaticJavaInputDataFromFile(serviceAFWeb, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            String fileN = ServiceAFweb.FileLocalDebugPath + "NNBP_V1_TR_NN1_nnWeight0.txt";
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
                            + "    public static String NN1_INPUTLIST" + index + " = \"\"\n"
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

    public void initTrainingNeuralNetFile(ServiceAFweb serviceAFWeb, int TR_Name) {
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
        if (getEnv.checkLocalPC() == true) {
            //get sample
            String nnName = ConstantKey.TR_NN1;
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
            }

            boolean flagClearInput = true;
            if (flagClearInput == true) {
                // delete TR nn1 transaction
                String BPname = CKey.NN_version + "_" + nnName;
                serviceAFWeb.getStockImp().deleteNeuralNetData(BPname);
            }

            ArrayList<NNInputDataObj> inputlist = new ArrayList();

            for (int i = 1; i < 20; i++) {
                String nnFileName = ServiceAFweb.FileLocalNNPath + "/" + nnName + i + ".csv";
                logger.info("> initTrainingNeuralNet1 " + nnFileName);
                boolean ret = TRprocessImp.readTrainingNeuralNet1(serviceAFWeb, inputlist, nnName, nnFileName);
                if (i == 0) {
                    continue;
                }
                if (ret == false) {
                    break;
                }
            }
        }
    }
//
//    public int updateDBneuralnetDataProcess(ServiceAFweb serviceAFWeb) {
//        String tableName = "neuralnetdata";
//        try {
//            ArrayList<String> writeArray = new ArrayList();
//            String fName = FileLocalPath + tableName + ".txt";
//            if (FileUtil.FileTest(fName) == false) {
//                return 0;
//            }
//            FileUtil.FileReadTextArray(fName, writeArray);
//            ArrayList<String> writeSQLArray = new ArrayList();
//            logger.info("> updateDBneuralnetDataProcess " + tableName + " " + writeArray.size());
//            int totalAdd = 0;
//            int totalDup = 0;
//            for (int i = 0; i < writeArray.size(); i++) {
//                String output = writeArray.get(i);
//                AFneuralNetData objData = new ObjectMapper().readValue(output, AFneuralNetData.class);
//                boolean flagtype = true;
//                if (flagtype == true) {
//                    if (objData.getType() != 0) {
//                        totalDup++;
//                        continue;
//                    }
//                }
//                ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(objData.getName(), objData.getType(), objData.getUpdatedatel());
//                if (objList == null) {
//                    continue;
//                }
//                if (objList.size() != 0) {
//                    totalDup++;
//                    continue;
//                }
//                serviceAFWeb.getStockImp().insertNeuralNetDataObject(objData);
//                totalAdd++;
//            }
//            logger.info("> updateDBneuralnetDataProcess  totalAdd=" + totalAdd + " totalDup=" + totalDup);
//
//            return 1;
//
//        } catch (IOException ex) {
//            logger.info("> restoreDBneuralnetDataProcess - exception " + ex);
//        }
//        return 0;
//    }

}
