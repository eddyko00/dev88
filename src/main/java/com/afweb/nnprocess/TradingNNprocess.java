/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;

import com.afweb.nn.*;
import com.afweb.service.ServiceAFweb;
import com.afweb.stock.StockInternet;

import com.afweb.util.*;

import com.afweb.util.TimeConvertion;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class TradingNNprocess {

    protected static Logger logger = Logger.getLogger("TradingNNprocess");
    // data history from  old to more recent
    // get next 5 days close price
    public static int TREND_Day = 4;

    public static String cfg_stockNNretrainNameArray = "cfg_stockNNretrainNameArray";
    private static ArrayList stockNNretrainNameArray = new ArrayList();
//    private ServiceAFweb serviceAFWeb = null;

    public ArrayList reLearnInputStockNNprocessNameArray(ServiceAFweb serviceAFWeb) {

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (stockNNretrainNameArray != null && stockNNretrainNameArray.size() > 0) {
            return stockNNretrainNameArray;
        }

        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            stockNameArray.add(0, "HOU.TO");
            ArrayList stockTRNameArray = new ArrayList();
            if (ServiceAFweb.nn1testflag == true) {
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String sym = (String) stockNameArray.get(i);
                    String symTR = sym + "#" + ConstantKey.INT_TR_NN1;
                    stockTRNameArray.add(symTR);
                }
            }
            if (ServiceAFweb.nn2testflag == true) {
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String sym = (String) stockNameArray.get(i);
                    String symTR = sym + "#" + ConstantKey.INT_TR_NN2;
                    stockTRNameArray.add(symTR);
                }
            }
            if (ServiceAFweb.nn3testflag == true) {
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String sym = (String) stockNameArray.get(i);
                    String symTR = sym + "#" + ConstantKey.INT_TR_NN3;
                    stockTRNameArray.add(symTR);
                }
            }            
            setStockNNretrainNameArray(stockTRNameArray);
        }
        return stockNNretrainNameArray;
    }

    public void ProcessReLearnInputNeuralNet(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessReLearnInputNeuralNet";

        reLearnInputStockNNprocessNameArray(serviceAFWeb);
        if (stockNNretrainNameArray == null) {
            return;
        }
        if (stockNNretrainNameArray.size() == 0) {
            return;
        }
        String printName = "";
        for (int i = 0; i < stockNNretrainNameArray.size(); i++) {
            printName += stockNNretrainNameArray.get(i) + ",";
        }
        logger.info("ProcessReLearnInputNeuralNet " + printName);
//        logger.info("> ProcessReTrainNeuralNet stockNNretrainNameArray size " + stockNNretrainNameArray.size());
        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "NNRE_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessReTrainNeuralNet");
        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }
//        logger.info("ProcessReLearnInputNeuralNet " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long LastServUpdateTimer = System.currentTimeMillis();
            int timeout = 15;
            if (ServiceAFweb.processNeuralNetFlag == true) {
                timeout = timeout * 3;
            }
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, timeout); // add 3 minutes

//            for (int i = 0; i < 10; i++) {
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
                    logger.info("ProcessReLearnInputNeuralNet exit after 5 minutes");
                    break;
                }
                if (stockNNretrainNameArray.size() == 0) {
                    break;
                }

                String symbolTR = (String) stockNNretrainNameArray.get(0);
                stockNNretrainNameArray.remove(0);

                String[] symbolArray = symbolTR.split("#");
                if (symbolArray.length >= 0) {

                    String symbol = symbolArray[0];

                    int trNN = Integer.parseInt(symbolArray[1]);

                    AFstockObj stock = serviceAFWeb.getStockRealTime(symbol);
                    if (stock != null) {
                        this.ReLearnInputNeuralNet(serviceAFWeb, symbol, trNN);
                    }
                }
            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessReLearnInputNeuralNet " + LockName + " unlock LockName");
        }
        logger.info("> ProcessReLearnInputNeuralNet ... done");
    }

    public void ReLearnInputNeuralNet(ServiceAFweb serviceAFWeb, String symbol, int trNN) {
        ServiceAFweb.lastfun = "ReLearnInputNeuralNet";

        String LockStock = "NNRE_TR_" + symbol + "_" + trNN;
        LockStock = LockStock.toUpperCase();

        long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
        long lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessReTrainNeuralNet");
//        if (ServiceAFweb.nn3testflag == true) {
//            lockReturnStock = 1;
//        }
//      logger.info("ProcessReLearnInputNeuralNet " + LockStock + " LockStock " + lockReturnStock);
        if (lockReturnStock > 0) {
            try {
                if (trNN == ConstantKey.INT_TR_NN1) {
                    NN1ProcessBySignal nn1Process = new NN1ProcessBySignal();
                    nn1Process.ReLearnNN1StockNeuralNetData(serviceAFWeb, trNN, symbol);
                }
                if (trNN == ConstantKey.INT_TR_NN2) {
                    NN2ProcessBySignal nn2Process = new NN2ProcessBySignal();
                    nn2Process.ReLearnNN2StockNeuralNetData(serviceAFWeb, trNN, symbol);
                }
                if (trNN == ConstantKey.INT_TR_NN3) {
                    NN3ProcessBySignal nn3Process = new NN3ProcessBySignal();
                    nn3Process.ReLearnNN3StockNeuralNetData(serviceAFWeb, trNN, symbol);
                }
            } catch (Exception ex) {
                logger.info("> PReLearnInputNeuralNet Exception" + ex.getMessage());
            }
            serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
//          logger.info("ProcessReLearnInputNeuralNet " + LockStock + " unLock LockStock ");
        }
    }

    public int ClearStockNNTranHistory(ServiceAFweb serviceAFWeb, String nnName) {
        return ClearStockNNTranHistory(serviceAFWeb, nnName, "");
    }

    public int ClearStockNNData(ServiceAFweb serviceAFWeb, AccountObj accountAdminObj) {
        logger.info("> ClearStockNNData ");

        if (accountAdminObj == null) {
            return -1;
        }

        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            for (int i = 0; i < stockNameArray.size(); i++) {
                String symbol = (String) stockNameArray.get(i);

                String nnName = ConstantKey.TR_NN1;
                String BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
                serviceAFWeb.getStockImp().deleteNeuralNetDataByBPname(BPname);

                nnName = ConstantKey.TR_NN2;
                BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
                serviceAFWeb.getStockImp().deleteNeuralNetDataByBPname(BPname);
            }
        }
        logger.info("> ClearStockNNData ..... Done");
        return 0;
    }

    public int ClearStockNNTranHistoryAllAcc(ServiceAFweb serviceAFWeb, String nnName, String sym) {
        logger.info("> ClearStockNNTranHistoryAllAcc " + nnName);

        ArrayList accNameArray = serviceAFWeb.SystemAllOpenAccountIDList();
        if (accNameArray != null) {
            for (int j = 0; j < accNameArray.size(); j++) {
                String accIdSt = (String) accNameArray.get(j);
                int accountId = Integer.parseInt(accIdSt);
                AccountObj accObj = serviceAFWeb.SystemAccountObjByAccountID(accountId);
                if (accObj.getType() == AccountObj.INT_ADMIN_ACCOUNT) {
                    continue;
                }
                ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accObj.getId());
                if (stockNameArray != null) {
                    for (int i = 0; i < stockNameArray.size(); i++) {
                        String symbol = (String) stockNameArray.get(i);
                        if (sym.length() != 0) {
                            if (!sym.equals(symbol)) {
                                continue;
                            }
                        }
                        AFstockObj stock = serviceAFWeb.getStockRealTime(symbol);
                        serviceAFWeb.getAccountImp().clearAccountStockTranByAccountID(accObj, stock.getId(), nnName);

                        ServiceAFweb.AFSleep();
                    }
                }
                ServiceAFweb.AFSleep();
            }
        }
        logger.info("> ClearStockNNTranHistoryAllAcc done..." + nnName);
        return 0;
    }

    public int ClearStockNNTranHistory(ServiceAFweb serviceAFWeb, String nnName, String sym) {
        logger.info("> ClearStockNNTranHistory " + nnName);
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return -1;
        }
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            for (int i = 0; i < stockNameArray.size(); i++) {
                String symbol = (String) stockNameArray.get(i);
                if (sym.length() != 0) {
                    if (!sym.equals(symbol)) {
                        continue;
                    }
                }
                AFstockObj stock = serviceAFWeb.getStockRealTime(symbol);
                serviceAFWeb.getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);

                ServiceAFweb.AFSleep();
            }
        }
        logger.info("> ClearStockNNTranHistory done..." + nnName);
        return 0;
    }

    public int ClearStockNN_inputNameArray(ServiceAFweb serviceAFWeb, String nnName) {
//        logger.info("> ClearStockNNinputNameArray ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return -1;
        }
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            for (int i = 0; i < stockNameArray.size(); i++) {
                String symbol = (String) stockNameArray.get(i);
                String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
                if (nnObj1 != null) {
                    // clear the input Neural network
                    serviceAFWeb.getStockImp().updateNeuralNetStatus1(BPnameSym, ConstantKey.INITIAL, 0);
                }
                ServiceAFweb.AFSleep();
            }
        }
        logger.info("> ClearStockNNinputNameArray Done " + nnName);
        return 0;
    }

    public static NNTrainObj trainingNNsetupTraining(ArrayList<NNInputOutObj> inputlist, String nnName) {
        ServiceAFweb.lastfun = "trainingNNsetupTraining";

//        logger.info("> trainingNNsetupTraining ");
        int inputListSize = inputlist.size();

        // Make sure to update this when adding new input
        // Make sure to update this when adding new input        
        int inputSize = CKey.NN_INPUT_SIZE;
        int outputSize = CKey.NN_OUTPUT_SIZE;

        // Make sure to update this when adding new input
        // Make sure to update this when adding new input         
        double[][] inputpattern = new double[inputListSize][inputSize];
        double[][] targetpattern = new double[inputListSize][outputSize];
        double[][] outputpattern = new double[inputListSize][outputSize];
        double[][] response = new double[inputListSize][outputSize];
        for (int i = 0; i < inputlist.size(); i++) {
            NNInputOutObj obj = inputlist.get(i);
            inputpattern[i][0] = obj.getInput1();
            inputpattern[i][1] = obj.getInput2();
            inputpattern[i][2] = obj.getInput3();
            inputpattern[i][3] = obj.getInput4();
            inputpattern[i][4] = obj.getInput5();
            inputpattern[i][5] = obj.getInput6();
            inputpattern[i][6] = obj.getInput7();
            inputpattern[i][7] = obj.getInput8();
            inputpattern[i][8] = obj.getInput9();
            inputpattern[i][9] = obj.getInput10();
//            inputpattern[i][10] = obj.getInput11();
//            inputpattern[i][11] = obj.getInput12();

            //  remember to update this when output size change CKey.NN_OUTPUT_SIZE;   
//            targetpattern[i] = obj.getOutput1();
//            targetpattern[i] = obj.getOutput1();
            targetpattern[i][0] = obj.getOutput1();
            targetpattern[i][1] = obj.getOutput2();

            outputpattern[i][0] = obj.getOutput1();
            outputpattern[i][1] = obj.getOutput2();

        }
        NNTrainObj nnTraining = new NNTrainObj();
        nnTraining.setNameNN(CKey.NN_version);
        nnTraining.setInputpattern(inputpattern);
        nnTraining.setOutputpattern(outputpattern);
        nnTraining.setResponse(response);
        nnTraining.setTargetpattern(targetpattern);
//        
        return nnTraining;
    }

    public static ArrayList<Double> getNNnormalizeStInputClose(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {
        if (thObjListMACD == null) {
            return null;
        }

        float close_0 = 0;
        float close_1 = 0;
        float close_2 = 0;
        float close_3 = 0;
        float close_4 = 0;
        float close_5 = 0;
        ArrayList<Float> parm1NormalList = new ArrayList();  // close normalize
        for (int k = 0; k < 8; k++) { //k < 30; k++) { // 20; k++) { //25; k++) {
            if ((index - k) < 0) {
                break;
            }
            StockTRHistoryObj thObjMVtmp = thObjListMACD.get(index - k);
            float close = thObjMVtmp.getClose();
            parm1NormalList.add(close);
            switch (k) {
                case 0:
                    close_0 = close;
                    break;
                case 1:
                    close_1 = close;
                    break;
                case 2:
                    close_2 = close;
                    break;
                case 3:
                    close_3 = close;
                    break;
                case 4:
                    close_4 = close;
                    break;
                case 5:
                    close_5 = close;
                    break;
                default:
                    break;
            }
        }
        NNormalObj parm1Normal = new NNormalObj();
        parm1Normal.initHighLow(parm1NormalList);

        ArrayList closeArray = new ArrayList();
        double closef = parm1Normal.getNormalizeValue(close_0);
        int temp = 0;
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_1);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_2);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_3);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_4);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_5);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);

        return closeArray;
    }

    public static ArrayList<Double> getNNnormalizeInputVolume(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {
        if (thObjListMACD == null) {
            return null;
        }

        float vol_0 = 0;
        float vol_1 = 0;
        float vol_2 = 0;
        float vol_3 = 0;

        ArrayList<Float> parm1NormalList = new ArrayList();  // close normalize
        for (int k = 0; k < 7; k++) {
            if ((index - k) < 0) {
                break;
            }
            StockTRHistoryObj thObjMVtmp = thObjListMACD.get(index - k);
            float vol = thObjMVtmp.getVolume();
            parm1NormalList.add(vol);
            switch (k) {
                case 0:
                    vol_0 = vol;
                    break;
                case 1:
                    vol_1 = vol;
                    break;
                case 2:
                    vol_2 = vol;
                    break;
                case 3:
                    vol_3 = vol;
                    break;
                default:
                    break;
            }
        }
        NNormalObj parm1Normal = new NNormalObj();
        parm1Normal.initHighLow(parm1NormalList);

        ArrayList closeArray = new ArrayList();
        double closef = parm1Normal.getNormalizeValue(vol_0);
        int temp = 0;
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(vol_1);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(vol_2);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(vol_3);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        return closeArray;
    }

    public static ArrayList<Double> getNNnormalizeInputClose(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {
        if (thObjListMACD == null) {
            return null;
        }

        float close_0 = 0;
        float close_1 = 0;
        float close_2 = 0;
        float close_3 = 0;
        float close_4 = 0;

        ArrayList<Float> parm1NormalList = new ArrayList();  // close normalize
        for (int k = 0; k < 7; k++) { //k < 30; k++) { // 20; k++) { //25; k++) {
            if ((index - k) < 0) {
                break;
            }
            StockTRHistoryObj thObjMVtmp = thObjListMACD.get(index - k);
            float close = thObjMVtmp.getClose();
            parm1NormalList.add(close);
            switch (k) {
                case 0:
                    close_0 = close;
                    break;
                case 1:
                    close_1 = close;
                    break;
                case 2:
                    close_2 = close;
                    break;
                case 3:
                    close_3 = close;
                    break;
                case 4:
                    close_4 = close;
                    break;
                default:
                    break;
            }
        }
        NNormalObj parm1Normal = new NNormalObj();
        parm1Normal.initHighLow(parm1NormalList);

        ArrayList closeArray = new ArrayList();
        double closef = parm1Normal.getNormalizeValue(close_0);
        int temp = 0;
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_1);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_2);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_3);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        closef = parm1Normal.getNormalizeValue(close_4);
        temp = (int) closef;
        closef = temp;
        closef = closef / 100;
        if (closef > 0.9) {
            closef = 0.9;
        }
        if (closef < 0.1) {
            closef = 0.1;
        }
        closeArray.add(closef);
        return closeArray;
    }

    /////////////// warning/////////////////
    // assume the getParm1() contain the indicator value
    // thObjListRSI is using by RSI and ADX indicator
    /////////////// warning/////////////////    
    public static NNInputOutObj getNNnormalizeInput(int index, ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI) {

        if ((thObjListMACD == null) || (thObjListMV == null)) {
            return null;
        }

        NNInputOutObj inputNNobj = new NNInputOutObj();

        StockTRHistoryObj thObjMV = thObjListMV.get(index);
        StockTRHistoryObj thObjMACD = thObjListMACD.get(index);
        StockTRHistoryObj thObjRSI = thObjListRSI.get(index);

        int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use
        ////////// initialize normailiZation
        double parm2;   // LTerm normalize 
        ArrayList<Float> parm2NormalList = new ArrayList();  // ema2050 normalize         
        double parm3;  // ema2050 normalize
        ArrayList<Float> parm3NormalList = new ArrayList();  // STerm normalize
        double parm4;  // macd normalize
        ArrayList<Float> parm4NormalList = new ArrayList();  // macd normalize
        double parm5;  // signal normalize
        ArrayList<Float> parm5NormalList = new ArrayList();  // signal normalize     

        if (index < 15) {  // check next line k < 30;
            return null;
        }
        for (int k = 0; k < 30; k++) { //25; k++) {            
            if ((index - k) < 0) {
                break;
            }
            StockTRHistoryObj thObjMVtmp = thObjListMV.get(index - k);
            float param2 = thObjMVtmp.getParm3(); // LTerm normalize
//            float param2 = thObjMVtmp.getParm4(); // STerm normalize            
            parm2NormalList.add(param2);
            float param3 = thObjMVtmp.getParm1(); // ema2050 normalize
            parm3NormalList.add(param3);
            StockTRHistoryObj thObjMACDtmp = thObjListMACD.get(index - k);
            float param4 = thObjMACDtmp.getParm1(); // macd normalize
            parm4NormalList.add(param4);
            StockTRHistoryObj thObjRSItmp = thObjListRSI.get(index - k);
            float param5 = thObjRSItmp.getParm1(); // RSI normalize
            parm5NormalList.add(param5);
        }
        NNormalObj parm2Normal = new NNormalObj();
        parm2Normal.initHighLow(parm2NormalList);
        NNormalObj parm3Normal = new NNormalObj();
        parm3Normal.initHighLow(parm3NormalList);
        NNormalObj parm4Normal = new NNormalObj();
        parm4Normal.initHighLow(parm4NormalList);
        NNormalObj parm5Normal = new NNormalObj();
        parm5Normal.initHighLow(parm5NormalList);
        ////////// initialize normailiZation

        float LTerm = thObjMV.getParm3();
        parm2 = parm2Normal.getNormalizeValue(LTerm);
//
        float ema2050 = thObjMV.getParm1();
        parm3 = parm3Normal.getNormalizeValue(ema2050);

        float MACD = thObjMACD.getParm1();
        parm4 = parm4Normal.getNormalizeValue(MACD);
        float RSI = thObjRSI.getParm1();
        parm5 = parm5Normal.getNormalizeValue(RSI);
        /////////////////////////////
        int temp = 0;
        temp = (int) parm2;
        parm2 = temp;
        parm2 = parm2 / 100;
        if (parm2 > 0.9) {
            parm2 = 0.9;
        }
        if (parm2 < 0.1) {
            parm2 = 0.1;
        }
        inputNNobj.setInput2(parm2);
        temp = (int) parm3;
        parm3 = temp;
        parm3 = parm3 / 100;
        if (parm3 > 0.9) {
            parm3 = 0.9;
        }
        if (parm3 < 0.1) {
            parm3 = 0.1;
        }
        inputNNobj.setInput3(parm3);
        temp = (int) parm4;
        parm4 = temp;
        parm4 = parm4 / 100;
        if (parm4 > 0.9) {
            parm4 = 0.9;
        }
        if (parm4 < 0.1) {
            parm4 = 0.1;
        }
        inputNNobj.setInput4(parm4);

        temp = (int) parm5;
        parm5 = temp;
        parm5 = parm5 / 100;
        if (parm5 > 0.9) {
            parm5 = 0.9;
        }
        if (parm5 < 0.1) {
            parm5 = 0.1;
        }
        inputNNobj.setInput5(parm5);

        float close = thObjMV.getClose();
        inputNNobj.setClose(close);

        Calendar setDate = TimeConvertion.getCurrentCalendar(thObjMACD.getUpdateDatel());
        String stdate = new Timestamp(setDate.getTime().getTime()).toString();
        stdate = stdate.substring(0, 10);

        inputNNobj.setDateSt(stdate);

        return inputNNobj;
    }

    private static double separateLevel(double parm) {
        if (parm > 0.6) {
            return 0.9;
        } else if (parm < 0.3) {
            return 0.1;
        }
        return 0.5;
    }

//////////////////////////////////////////////////////////////////   
    /**
     * @param aStockNNretrainNameArray the stockNNretrainNameArray to set
     */
    public static void setStockNNretrainNameArray(ArrayList aStockNNretrainNameArray) {
        stockNNretrainNameArray = aStockNNretrainNameArray;
    }
////////////////////////////////////////////////
// https://www.sanfoundry.com/java-program-implement-checksum-method-small-string-messages-detect-if-received-message-same-transmitted/

    static int generateChecksum(String s) {
        String hex_value = new String();
        // 'hex_value' will be used to store various hex values as a string
        int x, i, checksum = 0;
        // 'x' will be used for general purpose storage of integer values
        // 'i' is used for loops
        // 'checksum' will store the final checksum
        for (i = 0; i < s.length() - 2; i = i + 2) {
            x = (int) (s.charAt(i));
            hex_value = Integer.toHexString(x);
            x = (int) (s.charAt(i + 1));
            hex_value = hex_value + Integer.toHexString(x);
            // Extract two characters and get their hexadecimal ASCII values
//            System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " + hex_value);
            x = Integer.parseInt(hex_value, 16);
            // Convert the hex_value into int and store it
            checksum += x;
            // Add 'x' into 'checksum'
        }
        if (s.length() % 2 == 0) {
            // If number of characters is even, then repeat above loop's steps
            // one more time.
            x = (int) (s.charAt(i));
            hex_value = Integer.toHexString(x);
            x = (int) (s.charAt(i + 1));
            hex_value = hex_value + Integer.toHexString(x);
//            System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " + hex_value);
            x = Integer.parseInt(hex_value, 16);
        } else {
            // If number of characters is odd, last 2 digits will be 00.
            x = (int) (s.charAt(i));
            hex_value = "00" + Integer.toHexString(x);
            x = Integer.parseInt(hex_value, 16);
//            System.out.println(s.charAt(i) + " : " + hex_value);
        }
        checksum += x;
        // Add the generated value of 'x' from the if-else case into 'checksum'
        hex_value = Integer.toHexString(checksum);
        // Convert into hexadecimal string
        if (hex_value.length() > 4) {
            // If a carry is generated, then we wrap the carry
            int carry = Integer.parseInt(("" + hex_value.charAt(0)), 16);
            // Get the value of the carry bit
            hex_value = hex_value.substring(1, 5);
            // Remove it from the string
            checksum = Integer.parseInt(hex_value, 16);
            // Convert it into an int
            checksum += carry;
            // Add it to the checksum
        }
        checksum = generateComplement(checksum);
        // Get the complement
        return checksum;
    }

    static boolean receive(String s, int checksum) {
        int generated_checksum = generateChecksum(s);
        // Calculate checksum of received data
        generated_checksum = generateComplement(generated_checksum);
        // Then get its complement, since generated checksum is complemented
        int syndrome = generated_checksum + checksum;
        // Syndrome is addition of the 2 checksums
        syndrome = generateComplement(syndrome);
        // It is complemented
//        System.out.println("Syndrome = " + Integer.toHexString(syndrome));
        if (syndrome == 0) {
//            System.out.println("Data is received without error.");
            return true;
        }
//      System.out.println("There is an error in the received data.");
        return false;

    }

    static int generateComplement(int checksum) {
        // Generates 15's complement of a hexadecimal value
        checksum = Integer.parseInt("FFFF", 16) - checksum;
        return checksum;
    }

    ////////////////////////////////////////////
    public static HashMap<String, ArrayList> stockInputMap = null;
    public static HashMap<String, ArrayList> stockInputMap_1 = null;

    public static boolean CreateAllStockHistoryJava(ServiceAFweb serviceAFWeb, String symbolL[], String fileName, String tagName) {
        HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();

        try {

            ProcessAllStockHistoryCreatJava(serviceAFWeb, symbolL, stockInputMap);

            String inputListRawSt = new ObjectMapper().writeValueAsString(stockInputMap);
            String inputListSt = ServiceAFweb.compress(inputListRawSt);

            StringBuffer msgWrite = new StringBuffer();
            msgWrite.append("" ///
                    + "package com.afweb.nn;\n"
                    + "import com.afweb.service.ServiceAFweb;\n"
                    + "\n"
                    + "/*  This file is generated by system. Do not modify. */\n"
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

            ////// end
            msgWrite.append(""
                    + "}\n"
                    ///
                    + ""
            );
            String fileN = ServiceAFweb.FileLocalDebugPath + fileName + ".java";
            FileUtil.FileWriteText(fileN, msgWrite);

            StringBuffer msgWrite_src = new StringBuffer();
            msgWrite_src.append("" ///
                    + "package com.afweb.nn;\n"
                    + "import com.afweb.service.ServiceAFweb;\n"
                    + "import com.fasterxml.jackson.databind.ObjectMapper;\n"
                    + "import java.util.ArrayList;\n"
                    + "import java.util.HashMap;\n"
                    + "/*  This file is generated by system. Do not modify. */\n"
                    + "public class " + fileName + "_src {\n"
                    + "\n");
            /*
             */
            String javaSt = "";
            javaSt = ""
                    + "public static HashMap<String, ArrayList> AllStockHistoryStaticCodeInit(HashMap<String, ArrayList> stockInputMap) {\n"
                    + "StringBuffer inputBuf = new StringBuffer();\n"
                    + "try {\n"
                    + "";

            msgWrite_src.append(javaSt + "\n");
            for (int i = 1; i < index + 1; i++) {
                javaSt = ""
                        + "inputBuf.append(" + fileName + "." + tagName + i + ");\n"
                        + "";
                msgWrite_src.append(javaSt);
            }
            javaSt = ""
                    + "String inputListSt = ServiceAFweb.decompress(inputBuf.toString());\n"
                    + "stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);\n"
                    + "return stockInputMap;\n"
                    + "} catch (Exception ex) {\n"
                    + "}\n"
                    + "return stockInputMap;\n"
                    + "}\n"
                    + "";
            msgWrite_src.append(javaSt + "\n");

            ////// end
            msgWrite_src.append(""
                    + "}\n"
                    ///
                    + ""
            );
            String fileN_src = ServiceAFweb.FileLocalDebugPath + fileName + "_src.java";
            FileUtil.FileWriteText(fileN_src, msgWrite_src);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public static void ProcessAllStockHistoryCreatJava(ServiceAFweb serviceAFWeb, String symbolL[], HashMap<String, ArrayList> stockInputMap) {
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
            logger.info("AllStockHistoryCreatJavaProcess " + stockNameArray.size());

            int sizeyear = 5 * 52 * 5;
            for (int k = 0; k < stockNameArray.size(); k++) {
                String symbol = (String) stockNameArray.get(k);

                String StFileName = ServiceAFweb.FileLocalDebugPath + symbol + ".txt";

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
                for (int j = 5; j < StockArray.size(); j++) {
                    try {
                        AFstockInfo obj = StockArray.get(j);
                        String st = new ObjectMapper().writeValueAsString(obj);
                        writeArray.add(st);
                    } catch (Exception ex) {
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
                    } catch (Exception ex) {
                    }
                }
                //////////
                if (StockArray == null) {
                    continue;
                }
                logger.info(">>> AllStockHistoryCreatJavaProcess " + symbol + " " + StockArray.size());
                stockInputMap.put(symbol, StockArray);

            } // loop for stockNameArray
        }
    }

    public static ArrayList<AFstockInfo> getAllStockHistory(String symbol) {
        if (stockInputMap == null) {
            stockInputMap = nnAllStock_src.AllStockHistoryStaticCodeInit(stockInputMap);
        }
        ArrayList<AFstockInfo> stockInfoList =  ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap);
        if (stockInfoList != null) {
            return stockInfoList;
        }
        return getAllStockHistory_1(symbol);
    }

    private static ArrayList<AFstockInfo> getAllStockHistory_1(String symbol) {
        if (stockInputMap_1 == null) {
            stockInputMap_1 = nnAllStock_1_src.AllStockHistoryStaticCodeInit(stockInputMap_1);
        }        
        return ProcessAllStockHistoryfromStaticCode(symbol, stockInputMap_1);

    }

    private static ArrayList<AFstockInfo> ProcessAllStockHistoryfromStaticCode(String symbol,
            HashMap<String, ArrayList> stockInMap) {

        ArrayList<AFstockInfo> inputlist = new ArrayList();

        String symbolL[] = ServiceAFweb.ignoreStock;
        for (int i = 0; i < symbolL.length; i++) {
            String ignoreSym = symbolL[i];
            if (ignoreSym.equals(symbol)) {
                return inputlist;
            }
        }

        if (stockInMap == null) {
            return inputlist;
        }

        if (symbol != "") {
            try {
                inputlist = stockInMap.get(symbol);
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
/////////////////////////////////////////
    // return stock history starting recent date to the old date
    public ArrayList<AFstockInfo> getAverageStockHistorical(ArrayList<AFstockInfo> StockArray) {
        if (StockArray == null) {
            return StockArray;
        }
        if (StockArray.size() == 0) {
            return StockArray;
        }
        ArrayList<AFstockInfo> StockArrayRet = new ArrayList();
        for (int i = 0; i < StockArray.size() - 2; i++) {

            AFstockInfo info = StockArray.get(i);
            AFstockInfo info1 = StockArray.get(i + 1);
            AFstockInfo info2 = StockArray.get(i + 2);
            float fclose = (info.getFclose() + info1.getFclose() + info2.getFclose()) / 3;
            float fopen = (info.getFopen() + info1.getFopen() + info2.getFopen()) / 3;
            float fhigh = (info.getHigh() + info1.getHigh() + info2.getHigh()) / 3;
            float flow = (info.getLow() + info1.getLow() + info2.getLow()) / 3;

            AFstockInfo infoNew = new AFstockInfo();
            infoNew.setAdjustclose(info.getAdjustclose());
            infoNew.setEntrydatedisplay(info.getEntrydatedisplay());
            infoNew.setEntrydatel(info.getEntrydatel());
            infoNew.setFclose(fclose);
            infoNew.setFopen(fopen);
            infoNew.setHigh(fhigh);
            infoNew.setLow(flow);
            infoNew.setStockid(info.getStockid());
            infoNew.setVolume(info.getVolume());
            StockArrayRet.add(infoNew);

            if ((i + 3) < StockArray.size()) {
                continue;
            }
            // make the size the same
            StockArrayRet.add(info1);
            StockArrayRet.add(info2);
        }
//        for (int i = 0; i < StockArray.size() - 1; i++) {
//
//            AFstockInfo info = StockArray.get(i);
//            AFstockInfo info1 = StockArray.get(i + 1);
//            float fclose = (info.getFclose() + info1.getFclose()) / 2;
//            float fopen = (info.getFopen() + info1.getFopen()) / 2;
//            float fhigh = (info.getHigh() + info1.getHigh()) / 2;
//            float flow = (info.getLow() + info1.getLow()) / 2;
//
//            AFstockInfo infoNew = new AFstockInfo();
//            infoNew.setAdjustclose(info.getAdjustclose());
//            infoNew.setEntrydatedisplay(info.getEntrydatedisplay());
//            infoNew.setEntrydatel(info.getEntrydatel());
//            infoNew.setFclose(fclose);
//            infoNew.setFopen(fopen);
//            infoNew.setHigh(fhigh);
//            infoNew.setLow(flow);
//            infoNew.setStockid(info.getStockid());
//            infoNew.setVolume(info.getVolume());
//            StockArrayRet.add(infoNew);
//
//            if ((i + 2) < StockArray.size()) {
//                continue;
//            }
//            // make the size the same
//            StockArrayRet.add(info1);
//
//        }

        return StockArrayRet;
    }
    
/////////////////////////////////////////    
}
