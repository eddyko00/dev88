/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.util.CKey;
import com.afweb.model.*;
import com.afweb.account.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.nn.*;
import static com.afweb.nnprocess.TradingNNprocess.logger;
import com.afweb.service.ServiceAFweb;

import com.afweb.signal.*;
import com.afweb.stock.*;
import com.afweb.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 *
 * @author eddyko
 */
public class NNProcess {

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
    }

    // training neural net input data
    // create neural net input data
    //     
    private void NeuralNetInputTesting(ServiceAFweb serviceAFWeb, int TR_Name) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();

        boolean createTrain = true;
        if (createTrain == true) {
            int sizeYr = 3;
            String nnName = ConstantKey.TR_NN1;
            if (TR_Name == ConstantKey.INT_TR_NN2) {
                nnName = ConstantKey.TR_NN2;
            }

            for (int j = 0; j < sizeYr; j++) { //4; j++) {
                int size = 20 * CKey.MONTH_SIZE * j;
//                writeArrayNeuralNet.clear();
                serviceAFWeb.initTrainNeuralNetNumber = j + 1;

                trainingNNdataAll(serviceAFWeb, TR_Name, size);

            }
        }
        // create neural net input data
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

    

    private void NeuralNetProcessTesting(ServiceAFweb serviceAFWeb,int TR_Name) {
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
    
    
}
