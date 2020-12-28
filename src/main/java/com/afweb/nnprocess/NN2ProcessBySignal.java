/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.model.ConstantKey;
import com.afweb.model.SymbolNameObj;
import com.afweb.model.stock.AFneuralNetData;
import com.afweb.model.stock.AFstockInfo;
import com.afweb.model.stock.AFstockObj;
import com.afweb.nn.NNInputDataObj;
import com.afweb.nn.NNInputOutObj;
import static com.afweb.nnprocess.NN1ProcessBySignal.logger;
import com.afweb.service.ServiceAFweb;
import com.afweb.signal.ProcessNN1;
import com.afweb.signal.ProcessNN2;
import com.afweb.signal.TrandingSignalProcess;
import com.afweb.util.CKey;
import com.afweb.util.FileUtil;
import com.afweb.util.getEnv;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author eddyko
 */
public class NN2ProcessBySignal {

///////////////////////////////
    public void processInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ////////////////////////////////////////////
        boolean flagIntitNN1Input = true;
        if (flagIntitNN1Input == true) {

            TrandingSignalProcess.forceToInitleaningNewNN = true;  // must be true all for init learning             
            TrandingSignalProcess.forceToGenerateNewNN = false;
            logger.info("> processInputNeuralNet TR NN1... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI1);
            logger.info("> processInputNeuralNet TR NN2... ");
            NeuralNetInputTesting(serviceAFWeb, ConstantKey.INT_TR_RSI2);
            // need to debug to generate the java first time
            TrandingSignalProcess.forceToGenerateNewNN = true;

            TrandingSignalProcess.forceToErrorNewNN = true;
//            // start training
//            // TrainingNNBP inputpattern 1748
//            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
//            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
//
//            TrandingSignalProcess.forceToGenerateNewNN = false;
//            // start training
//            // TrainingNNBP inputpattern 1748
//            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
//            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
//            NeuralNetProcessTesting(serviceAFWeb, ConstantKey.INT_TR_NN1);
//            NeuralNetCreatJava(serviceAFWeb, ConstantKey.TR_NN1);
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
            logger.info("> initTrainNeuralNetNumber " + serviceAFWeb.initTrainNeuralNetNumber);
            String symbol = "";
            String symbolL[] = ServiceAFweb.primaryStock;
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
            String nn12 = "_nn3_";
            if (tr == ConstantKey.INT_TR_RSI2) {
                nn12 = "_nn4_";
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

}
