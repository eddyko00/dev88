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
import com.afweb.nnBP.NNBPservice;
import com.afweb.service.ServiceAFweb;
import com.afweb.signal.ProcessNN1;
import com.afweb.signal.ProcessNN2;
import com.afweb.signal.TrandingSignalProcess;

import com.afweb.util.CKey;
import com.afweb.util.FileUtil;
import com.afweb.util.StringTag;

import com.afweb.util.TimeConvertion;
import com.afweb.util.getEnv;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class TradingNNprocess {

    protected static Logger logger = Logger.getLogger("TradingNNprocess");

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
            setStockNNretrainNameArray(stockTRNameArray);
        }
        return stockNNretrainNameArray;
    }

    public void ProcessReLearnInputNeuralNet(ServiceAFweb serviceAFWeb) {

        if (stockNNretrainNameArray == null) {
            return;
        }
        if (stockNNretrainNameArray.size() == 0) {
            return;
        }
        logger.info("> ProcessReTrainNeuralNet stockNNretrainNameArray size " + stockNNretrainNameArray.size());
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
        logger.info("ProcessReLearnInputNeuralNet " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {
            long currentTime = System.currentTimeMillis();
            long lockDate1Min = TimeConvertion.addMinutes(currentTime, 1);

//            for (int i = 0; i < 10; i++) {
            while (true) {
                if (CKey.NN_DEBUG != true) {
                    currentTime = System.currentTimeMillis();
                    if (lockDate1Min < currentTime) {
                        break;
                    }
                }
                if (stockNNretrainNameArray.size() == 0) {
                    break;
                }
                try {
                    String symbolTR = (String) stockNNretrainNameArray.get(0);
                    stockNNretrainNameArray.remove(0);

                    String[] symbolArray = symbolTR.split("#");
                    if (symbolArray.length >= 0) {

                        String symbol = symbolArray[0];
                        int trNN = Integer.parseInt(symbolArray[1]);

                        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
                        if (stock != null) {

                            String LockStock = "NNRE_TR_" + symbol + "_" + trNN;
                            LockStock = LockStock.toUpperCase();

                            long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                            long lockReturnStock = serviceAFWeb.setLockNameProcess(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessReTrainNeuralNet");
                            if (testing == true) {
                                lockReturnStock = 1;
                            }
                            logger.info("ProcessReLearnInputNeuralNet " + LockStock + " LockStock " + lockReturnStock);
                            if (lockReturnStock > 0) {
                                inputReTrainStockNeuralNetData(serviceAFWeb, trNN, symbol);
                                serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
                                logger.info("ProcessReLearnInputNeuralNet " + LockStock + " unLock LockStock ");
                                //////////
                                int cfgId = 0;;
                                String cfgName = TradingNNprocess.cfg_stockNNretrainNameArray;
                                ArrayList<CommObj> commObjArry = serviceAFWeb.getAccountImp().getComObjByCustName(cfgId, cfgName);
                                if (commObjArry != null) {
                                    CommObj commObj = commObjArry.get(0);
                                    String dataSt = "";
                                    try {
                                        dataSt = new ObjectMapper().writeValueAsString(stockNNretrainNameArray);
                                    } catch (JsonProcessingException ex) {
                                    }
                                    dataSt = StringTag.replaceAll("\"", "^", dataSt);
                                    commObj.setData(dataSt);
                                    serviceAFWeb.getAccountImp().updateCommByCustNameById(commObj);
                                }
                                ////////////
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.info("> ProcessReLearnInputNeuralNet Exception" + ex.getMessage());
                }
            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
            logger.info("ProcessAdminSignalTrading " + LockName + " unlock LockName");
        }
//        logger.info("> ProcessTrainNeuralNet ... done");
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
                serviceAFWeb.getStockImp().deleteNeuralNetData(BPname);

                nnName = ConstantKey.TR_NN2;
                BPname = CKey.NN_version + "_" + nnName + "_" + symbol;
                serviceAFWeb.getStockImp().deleteNeuralNetData(BPname);
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
                        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
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
                AFstockObj stock = serviceAFWeb.getRealTimeStockImp(symbol);
                serviceAFWeb.getAccountImp().clearAccountStockTranByAccountID(accountAdminObj, stock.getId(), nnName);

                ServiceAFweb.AFSleep();
            }
        }
        logger.info("> ClearStockNNTranHistory done..." + nnName);
        return 0;
    }

    public int ClearStockNNinputNameArray(ServiceAFweb serviceAFWeb, String nnName) {
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

    public ArrayList<NNInputDataObj> getTrainingNNdataStockReTrain(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        int size1yearAll = 20 * 12 * 5 + (50 * 3);

//        logger.info("> trainingNN " + symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_NN1) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date    
            inputList = trainingNN1dataReTrain(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            inputList = trainingNN2dataReTrain(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
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

    public ArrayList<NNInputDataObj> getTrainingNNdataStock(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        int size1yearAll = 20 * 12 * 2 + (50 * 3);

//        logger.info("> trainingNN " + symbol);
        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
        ArrayList<NNInputDataObj> inputList = null;

        if (tr == ConstantKey.INT_TR_NN1) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date            
            ProcessNN1 nn1 = new ProcessNN1();
            inputList = nn1.trainingNN1dataMACD1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE); // 14
        } else if (tr == ConstantKey.INT_TR_NN2) {
            //StockArray assume recent date to old data  
            //StockArray assume recent date to old data              
            //trainingNN1dataMACD will return oldest first to new date
            //trainingNN1dataMACD will return oldest first to new date 
            ProcessNN2 nn2 = new ProcessNN2();
            inputList = nn2.trainingNN2dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE);
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

//    public void trainingNNdataAll(ServiceAFweb serviceAFWeb, int tr, int offset) {
//        logger.info("> trainingNNdataAll ");
//        this.serviceAFWeb = serviceAFWeb;
//        String symbol = "";
////    public static String primaryStock[] = {"AAPL", "SPY", "DIA", "QQQ", "HOU.TO", "HOD.TO", "T.TO", "FAS", "FAZ", "RY.TO", "XIU.TO"};
//        String symbolL[] = ServiceAFweb.primaryStock;
//        for (int i = 0; i < symbolL.length; i++) {
//            symbol = symbolL[i];
//            ArrayList<NNInputDataObj> InputList = getTrainingNNdataProcess(serviceAFWeb, symbol, tr, offset);
//        }
//
//    }
//    public ArrayList<NNInputDataObj> getTrainingNNdataProcess(ServiceAFweb serviceAFWeb, String symbol, int tr, int offset) {
//        logger.info("> getTrainingNNdataProcess " + symbol);
//        this.serviceAFWeb = serviceAFWeb;
//
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
//        symbol = symbol.replace(".", "_");
//
//        int size1yearAll = 20 * 12 * 5 + (50 * 3);
//
//        ArrayList<AFstockInfo> StockArray = serviceAFWeb.getStockHistorical(symbol, size1yearAll);
//        ArrayList<NNInputDataObj> inputList = null;
//
//        String nnName = ConstantKey.TR_NN1;
//        if (tr == ConstantKey.INT_TR_NN1) {
//            nnName = ConstantKey.TR_NN1;
//            //StockArray assume recent date to old data  
//            //StockArray assume recent date to old data              
//            //trainingNN1dataMACD will return oldest first to new date
//            //trainingNN1dataMACD will return oldest first to new date            
//            ProcessNN1 nn1 = new ProcessNN1();
//            inputList = nn1.trainingNN1dataMACD1(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE + 2);
//        } else if (tr == ConstantKey.INT_TR_NN2) {
//            nnName = ConstantKey.TR_NN2;
//            ProcessNN2 nn2 = new ProcessNN2();
//            inputList = nn2.trainingNN2dataMACD(serviceAFWeb, symbol, StockArray, offset, CKey.MONTH_SIZE + 2);
//        }
//
//        String BPname = CKey.NN_version + "_" + nnName;
//        boolean forceNN2flag = true;
//        if (forceNN2flag) {
//            BPname = CKey.NN_version + "_" + ConstantKey.TR_NN1;
//        }
//        // ignor first and last
//        int len = inputList.size();
//        if (len <= 2) {
//            return null;
//        }
//        SymbolNameObj symObj = new SymbolNameObj(symbol);
//        String NormalizeSymbol = symObj.getYahooSymbol();
//        AFstockObj stock = serviceAFWeb.getRealTimeStockImp(NormalizeSymbol);
//        if (stock == null) {
//            return inputList;
//        }
//        int stockId = stock.getId();
//
//        ArrayList writeArray = new ArrayList();
//        String stTitle = "";
//        int nnInputSize = CKey.NN_INPUT_SIZE;  // just for search refrence no use        
//        for (int i = 0; i < inputList.size(); i++) {
//            NNInputDataObj objData = inputList.get(i);
//            NNInputOutObj obj = objData.getObj();
//
//            String st = "\"" + stockId + "\",\"" + objData.getUpdatedatel() + "\",\"" + obj.getDateSt() + "\",\"" + obj.getClose() + "\",\"" + obj.getTrsignal()
//                    + "\",\"" + obj.getOutput1()
//                    + "\",\"" + obj.getInput1()
//                    + "\",\"" + obj.getInput2()
//                    + "\",\"" + obj.getInput3()
//                    + "\",\"" + obj.getInput4()
//                    + "\",\"" + obj.getInput5()
//                    + "\",\"" + obj.getInput6()
//                    + "\",\"" + obj.getInput7() + "\",\"" + obj.getInput8()
//                    + "\",\"" + obj.getInput9() + "\",\"" + obj.getInput10()
//                    // + "\",\"" + obj.getInput11() + "\",\"" + obj.getInput12()
//                    + "\"";
//
//            if (i == 0) {
//                st += ",\"last\"";
//            }
//
//            if (i + 1 >= inputList.size()) {
//                st += ",\"first\"";
//            }
//
//            if (i == 0) {
//                stTitle = "\"" + "stockId" + "\",\"" + "Updatedatel" + "\",\"" + "Date" + "\",\"" + "close" + "\",\"" + "signal"
//                        + "\",\"" + "output"
//                        + "\",\"" + "macd TSig"
//                        + "\",\"" + "LTerm"
//                        + "\",\"" + "ema2050" + "\",\"" + "macd" + "\",\"" + "rsi"
//                        + "\",\"" + "close-0" + "\",\"" + "close-1" + "\",\"" + "close-2" + "\",\"" + "close-3" + "\",\"" + "close-4"
//                        + "\",\"" + symbol + "\"";
//
//            }
//            String stDispaly = st.replaceAll("\"", "");
//            writeArray.add(stDispaly);
//        }
//        writeArray.add(stTitle.replaceAll("\"", ""));
//
//        Collections.reverse(writeArray);
//        Collections.reverse(inputList);
//
//        if (getEnv.checkLocalPC() == true) {
//            String nn12 = "_nn1_";
//            if (tr == ConstantKey.INT_TR_NN2) {
//                nn12 = "_nn2_";
//            }
//            String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + ServiceAFweb.initTrainNeuralNetNumber + ".csv";
//
//            FileUtil.FileWriteTextArray(filename, writeArray);
////            ServiceAFweb.writeArrayNeuralNet.addAll(writeArray);
//
//        }
//        inputList.remove(len - 1);
//        inputList.remove(0);
//
//        //////// do not save in DB, only files
//        //////// do not save in DB, only files
//        //////// do not save in DB, only files
//        boolean inputSaveFlag = false;
//        if (inputSaveFlag == true) {
//            int totalAdd = 0;
//            int totalDup = 0;
//            for (int i = 0; i < inputList.size(); i++) {
//                NNInputDataObj objData = inputList.get(i);
//                ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPname, stockId, objData.getUpdatedatel());
//                if ((objList == null) || (objList.size() == 0)) {
//                    serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPname, stockId, objData);
//                    totalAdd++;
//                    continue;
//                }
//                totalDup++;
//                boolean flag = false;
//                if (flag == true) {
//                    if (CKey.NN_DEBUG == true) {
//                        logger.info("> getTrainingNNdataProcess duplicate " + BPname + " " + symbol + " " + objData.getObj().getDateSt());
//                    }
//                }
//            }
//            logger.info("> getTrainingNNdataProcess " + BPname + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
//        }
//        return inputList;
//    }
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN1dataReTrain(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_NN1);

        TradingRuleObj trObjMACD1 = new TradingRuleObj();
        trObjMACD1.setTrname(ConstantKey.TR_NN1);
        trObjMACD1.setType(ConstantKey.INT_TR_NN1);

        trObjMACD1.setAccount(trObjMACD.getAccount());
        trObjMACD1.setStockid(trObjMACD.getStockid());

        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD1, StockArray, offset, monthSize);

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
//                if (monthSize > 5) {
//                    ArrayList<String> writeArray = new ArrayList();
//                    ArrayList<String> displayArray = new ArrayList();
//                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListMACD, writeArray, displayArray);
//                    boolean flagHis = false;
//                    if (flagHis == true) {
//                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + ConstantKey.TR_NN1 + "_tran.csv", writeArray);
//                    }
//                    serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListMACD, symbol, ConstantKey.TR_NN1, null);
//                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = NNProcessImp.getAccountStockTRListHistoryMACDNN(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, true);

        return inputDatalist;
    }

    //StockArray assume recent date to old data
    //StockArray assume recent date to old data
    //StockArray assume recent date to old data   
    public ArrayList<NNInputDataObj> trainingNN2dataReTrain(ServiceAFweb serviceAFWeb, String sym, ArrayList<AFstockInfo> StockArray, int offset, int monthSize) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
//        logger.info("> trainingNN ");
//        this.serviceAFWeb = serviceAFWeb;
        String username = CKey.ADMIN_USERNAME;
        String accountid = "1";
        String symbol = sym;
//        ArrayList<NNInputOutObj> inputlist = new ArrayList<NNInputOutObj>();

        NNTrainObj nnTrSym = new NNTrainObj();
        TradingRuleObj trObjMACD = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_NN1);

        TradingRuleObj trObjMACD1 = new TradingRuleObj();
        trObjMACD1.setTrname(ConstantKey.TR_NN2);
        trObjMACD1.setType(ConstantKey.INT_TR_NN2);

        trObjMACD1.setAccount(trObjMACD.getAccount());
        trObjMACD1.setStockid(trObjMACD.getStockid());

        ArrayList<StockTRHistoryObj> thObjListMACD = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMACD1, StockArray, offset, monthSize);

        if (getEnv.checkLocalPC() == true) {
            if (CKey.NN_DEBUG == true) {
                if (monthSize > 5) {
//                    ArrayList<String> writeArray = new ArrayList();
//                    ArrayList<String> displayArray = new ArrayList();
//                    int ret = serviceAFWeb.getAccountStockTRListHistoryDisplayProcess(thObjListMACD, writeArray, displayArray);
//                    boolean flagHis = false;
//                    if (flagHis == true) {
//                        FileUtil.FileWriteTextArray(serviceAFWeb.FileLocalDebugPath + symbol + "_" + ConstantKey.TR_NN1 + "_tran.csv", writeArray);
//                    }
//                    serviceAFWeb.getAccountStockTRListHistoryChartProcess(thObjListMACD, symbol, ConstantKey.TR_NN1, null);
                }
            }
        }

        TradingRuleObj trObjMV = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_MV);
        ArrayList<StockTRHistoryObj> thObjListMV = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjMV, StockArray, offset, monthSize);

        TradingRuleObj trObjRSI = serviceAFWeb.getAccountStockByTRname(username, null, accountid, symbol, ConstantKey.TR_RSI);
        ArrayList<StockTRHistoryObj> thObjListRSI = TRprocessImp.ProcessTRHistoryOffset(serviceAFWeb, trObjRSI, StockArray, offset, monthSize);

        ArrayList<NNInputDataObj> inputDatalist = NNProcessImp.getAccountStockTRListHistoryMACDNN(thObjListMACD, thObjListMV, thObjListRSI, symbol, nnTrSym, true);

        return inputDatalist;
    }

//    public static NNTrainObj trainingNNdataSetupTraining(ArrayList<NNInputDataObj> inputlist) {
////        logger.info("> trainingNNsetupTraining ");
//
//        int inputListSize = inputlist.size();
//
//        // Make sure to update this when adding new input
//        // Make sure to update this when adding new input        
//        int inputSize = CKey.NN_INPUT_SIZE;
//        int outputSize = 1;
//        // Make sure to update this when adding new input
//        // Make sure to update this when adding new input         
//        double[][] inputpattern = new double[inputListSize][inputSize];
//        double[][] targetpattern = new double[inputListSize][outputSize];
//        double[][] outputpattern = new double[inputListSize][outputSize];
//        double[][] response = new double[inputListSize][outputSize];
//        for (int i = 0; i < inputlist.size(); i++) {
//            NNInputOutObj obj = inputlist.get(i).getObj();
//            inputpattern[i][0] = obj.getInput1();
//            inputpattern[i][1] = obj.getInput2();
//            inputpattern[i][2] = obj.getInput3();
//            inputpattern[i][3] = obj.getInput4();
//            inputpattern[i][4] = obj.getInput5();
//            inputpattern[i][5] = obj.getInput6();
//            inputpattern[i][6] = obj.getInput7();
//            inputpattern[i][7] = obj.getInput8();
//            inputpattern[i][8] = obj.getInput9();
//            inputpattern[i][9] = obj.getInput10();
////            inputpattern[i][10] = obj.getInput11();
////            inputpattern[i][11] = obj.getInput12();
//
//            targetpattern[i][0] = obj.getOutput1();
//            targetpattern[i][1] = obj.getOutput2();
//
//            outputpattern[i][0] = obj.getOutput1();
//            outputpattern[i][1] = obj.getOutput2();
//        }
//        NNTrainObj nnTraining = new NNTrainObj();
//        nnTraining.setNameNN(CKey.NN_version);
//        nnTraining.setInputpattern(inputpattern);
//        nnTraining.setOutputpattern(outputpattern);
//        nnTraining.setResponse(response);
//        nnTraining.setTargetpattern(targetpattern);
////        
//        return nnTraining;
//    }
    public static NNTrainObj trainingNNsetupTraining(ArrayList<NNInputOutObj> inputlist, String nnName) {
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

    // data history from  old to more recent
    // get next 5 days close price
    public static double getNNnormalizeStOutputClose(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {

        if (thObjListMACD == null) {
            return -1;
        }
        // need to match specialOverrideRule3 futureDay
        int futureDay = 5;
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

//    public static double getNNnormalizeStOutputCloseOld(int index, ArrayList<StockTRHistoryObj> thObjListMACD) {
//
//        if (thObjListMACD == null) {
//            return -1;
//        }
//        int futureDay = 5;
//        int cIndex = index + futureDay;
//
//        if (cIndex >= thObjListMACD.size()) {
//            return -1;
//        }
//        StockTRHistoryObj thObjMV0 = thObjListMACD.get(index);
//        double closeOutput0 = thObjMV0.getClose();
//        StockTRHistoryObj thObjMV5 = thObjListMACD.get(cIndex);
//        double closeOutput = thObjMV5.getClose();
//
//        double closef = (closeOutput - closeOutput0) / closeOutput0;
//        closef = closef * 100;
//
//        closef = closef * 15;   // factore of 15 to make it more valid for NN
//
//        int temp = 0;
//        temp = (int) closef;
//        closef = temp;
//        closef = closef / 100;
//        if (closef > 0) {
//            if (closef > 0.9) {
//                closef = 0.9;
//            }
//            if (closef < 0.1) {
//                closef = 0.1;
//            }
//        } else {
//            if (closef < -0.9) {
//                closef = -0.9;
//            }
//            if (closef > -0.1) {
//                closef = -0.1;
//            }
//        }
//        return closef;
//
//    }
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

    public static int checkNNsignalDecision(StockTRHistoryObj thObj, StockTRHistoryObj prevThObj) {
        if (prevThObj == null) {
            prevThObj = thObj;
        }
        int retDecision = -1;
        int pervSignal = prevThObj.getTrsignal();

        float pricePrev = prevThObj.getClose();
        float price = thObj.getClose();
        float percent = (price - pricePrev) / pricePrev;
        percent = percent * 100 * 15;
        float percentAbs = Math.abs(percent);
        if (percentAbs < 30) { //20){
            return -1;
        }

        if (pervSignal == ConstantKey.S_BUY) {
            retDecision = 0;
            if (thObj.getClose() > prevThObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }
        if (pervSignal == ConstantKey.S_SELL) {
            retDecision = 0;
            if (prevThObj.getClose() > thObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }

        return -1;
    }

    public static int checkNNsignalDecision(NNInputOutObj thObj, NNInputOutObj prevThObj) {
        if (prevThObj == null) {
            prevThObj = thObj;
        }
        int retDecision = -1;
        int pervSignal = prevThObj.getTrsignal();

        if (pervSignal == ConstantKey.S_BUY) {
            retDecision = 0;
            if (thObj.getClose() > prevThObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }
        if (pervSignal == ConstantKey.S_SELL) {
            retDecision = 0;
            if (prevThObj.getClose() > thObj.getClose()) {
                retDecision = 1;
            }
            return retDecision;
        }
        return -1;
    }

//    public int inputStockNeuralNetBySignal(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
//        boolean nnsym = true;
//        if (nnsym == true) {
//            int totalAdd = 0;
//            int totalDup = 0;
//            String nnName = ConstantKey.TR_NN1;
//            if (TR_Name == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//            }
//            String nnNameSym = nnName + "_" + symbol;
//
//            String BPnameSym = CKey.NN_version + "_" + nnNameSym;
//            try {
//                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPnameSym, 0);
//                String status = "NA";
//                if (nnObj1 != null) {
//                    if ((nnObj1.getStatus() == ConstantKey.OPEN) || (nnObj1.getStatus() == ConstantKey.COMPLETED)) {
//                        return 0;
//                    }
//                    status = "" + nnObj1.getStatus();
//                }
//
//                logger.info("> inputStockNeuralNetData " + BPnameSym + " Status=" + status);
//                NNBPservice nnTemp = new NNBPservice();
//
//                String middlelayer = "";
//                String version = "";
//                if (TR_Name == ConstantKey.INT_TR_NN1) {
//                    if (CKey.NN1_WEIGHT_0.length() == 0) {
//                        return 0;
//                    }
//                    nnTemp.createNet(CKey.NN1_WEIGHT_0);
//                    String weightSt = nnTemp.getNetObjSt();
//                    String[] strNetArray = CKey.NN1_WEIGHT_0.split(";");
//                    version = strNetArray[0];
//                    middlelayer = strNetArray[4];
//                } else if (TR_Name == ConstantKey.INT_TR_NN2) {
//                    if (CKey.NN2_WEIGHT_0.length() == 0) {
//                        return 0;
//                    }
//                    nnTemp.createNet(CKey.NN2_WEIGHT_0);
//                    String weightSt = nnTemp.getNetObjSt();
//                    String[] strNetArray = CKey.NN2_WEIGHT_0.split(";");
//                    version = strNetArray[0];
//                    middlelayer = strNetArray[4];
//                }
//                ArrayList<NNInputOutObj> inputlist = new ArrayList();
//
//                TradingNNprocess trainNN = new TradingNNprocess();
////                ArrayList<NNInputDataObj> inputlistSym = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, TR_Name, 0);
//                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
//                ArrayList<NNInputDataObj> inputlistSym1 = new ArrayList();
//                ArrayList<NNInputDataObj> inputlistSym2 = new ArrayList();
//
//                /// just for testing
//                inputlistSym1 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1, 0);
//                inputlistSym2 = trainNN.getTrainingNNdataStock(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, 0);
//
//                inputlistSym.addAll(inputlistSym1);
//                inputlistSym.addAll(inputlistSym2);
//
//                ArrayList<NNInputDataObj> inputL = new ArrayList();
//                boolean trainInFile = true;
//                if (trainInFile == true) {
//                    inputL = NeuralNetGetNN1InputfromStaticCode(symbol);
//                    if (inputL != null) {
//                        if (inputL.size() > 0) {
//                            logger.info("> inputStockNeuralNetData " + BPnameSym + " " + symbol + " " + inputL.size());
//                            for (int k = 0; k < inputL.size(); k++) {
//                                NNInputDataObj inputLObj = inputL.get(k);
//                                for (int m = 0; m < inputlistSym.size(); m++) {
//                                    NNInputDataObj inputSymObj = inputlistSym.get(m);
//                                    float output1 = (float) inputSymObj.getObj().getOutput1();
//                                    if ((output1 == 0) || (output1 == -1)) {
//                                        inputlistSym.remove(m);
//                                        break;
//                                    }
//                                    String inputLObD = inputLObj.getObj().getDateSt();
//                                    String inputSymObD = inputSymObj.getObj().getDateSt();
//                                    if (inputLObD.equals(inputSymObD)) {
//                                        inputlistSym.remove(m);
////                                        logger.info("> inputStockNeuralNetData " + BPnameSym + " " + symbol + " " + inputLObj.getUpdatedatel());
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//                if (inputlistSym != null) {
//                    //merge inputlistSym
//
//                    for (int i = 0; i < inputlistSym.size(); i++) {
//                        NNInputOutObj inputObj = inputlistSym.get(i).getObj();
//                        inputlist.add(inputObj);
//                        // save into db
//                        // save into db
//                        // save into db
//                        // save into db
//                        NNInputDataObj objData = inputlistSym.get(i);
//                        ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
//                        if ((objList == null) || (objList.size() == 0)) {
//                            serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
//                            totalAdd++;
//                            continue;
//                        }
//                        totalDup++;
//                        boolean flag = false;
//                        if (flag == true) {
//                            if (CKey.NN_DEBUG == true) {
//                                logger.info("> inputStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
//                            }
//                        }
//                    }
//                }
//
//                AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPnameSym, 0);
//                if (nnObj0 != null) {
//                    String stWeight0 = nnObj0.getWeight();
//
//                    if (stWeight0.length() > 0) {
//
//                        String[] strNetArraySym = stWeight0.split(";");
//                        String versionSym = strNetArraySym[0];
//                        String middlelayerSym = strNetArraySym[4];
//                        // reset to use TR Weight 0  if middel layer is different
//                        // reset to use TR Weight 0  if middel layer is different 
//                        // just for testing
////                        versionSym = "";
//                        // just for testing
//                        if (middlelayer.equals(middlelayerSym) && version.equals(versionSym)) {
//                            logger.info("> inputStockNeuralNetData create existing Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
//                            //just for testing                           
//                            nnTemp.createNet(stWeight0);
//                        } else {
//                            logger.info("> inputStockNeuralNetData create Static Base NN1_WEIGHT " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);
//                        }
//                    }
//                }
//
//                String weightSt = nnTemp.getNetObjSt();
////                
//                int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObj1(BPnameSym, weightSt);
////                logger.info("> inputStockNeuralNet " + BPnameSym + " inputlist=" + inputlist.size() + " ...Done");
//                return ret;
//
//            } catch (Exception e) {
//                logger.info("> inputStockNeuralNet exception " + BPnameSym + " - " + e.getMessage());
//            }
//        }
//        return -1;
//
//    }
    public int inputReTrainStockNeuralNetData(ServiceAFweb serviceAFWeb, int TR_Name, String symbol) {
        boolean nnsym = true;
        if (nnsym == true) {
            int totalAdd = 0;
            int totalDup = 0;
            String nnName = ConstantKey.TR_NN1;
//            if (TR_Name == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//            }
            String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            try {
                AFneuralNet nnObj0 = serviceAFWeb.getNeuralNetObjWeight0(BPnameSym, 0);
                if (nnObj0 == null) {
                    return 0;
                }

                logger.info("> inputReTrainStockNeuralNetData " + BPnameSym);

                ArrayList<NNInputOutObj> inputlist = new ArrayList();

                TradingNNprocess trainNN = new TradingNNprocess();
                ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
                //StockArray assume recent date to old data   
                ArrayList<NNInputDataObj> inputlistSym1 = trainNN.getTrainingNNdataStockReTrain(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1, 0);
                ArrayList<NNInputDataObj> inputlistSym2 = trainNN.getTrainingNNdataStockReTrain(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, 0);
                inputlistSym.addAll(inputlistSym1);
                inputlistSym.addAll(inputlistSym2);

                ArrayList<NNInputDataObj> inputL = new ArrayList();
                boolean trainInFile = true;
                if (trainInFile == true) {
                    inputL = NNProcessBySignal.NeuralNetGetNN1InputfromStaticCode(symbol);
                    if (inputL != null) {
                        if (inputL.size() > 0) {
                            for (int k = 0; k < inputL.size(); k++) {
                                NNInputDataObj inputLObj = inputL.get(k);
                                for (int m = 0; m < inputlistSym.size(); m++) {
                                    NNInputDataObj inputSymObj = inputlistSym.get(m);
                                    if (inputLObj.getUpdatedatel() == inputSymObj.getUpdatedatel()) {
                                        inputlistSym.remove(m);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

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
                        ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
                        if ((objList == null) || (objList.size() == 0)) {
                            serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
                            totalAdd++;
                            writeArray.add(nameST);
                            continue;
                        }
                        totalDup++;
                        boolean flag = false;
                        if (flag == true) {
                            if (CKey.NN_DEBUG == true) {
                                logger.info("> inputReTrainStockNeuralNetData duplicate " + BPnameSym + " " + symbol + " " + objData.getObj().getDateSt());
                            }
                        }
                    }
                }
                logger.info("> inputReTrainStockNeuralNetData Symbol " + BPnameSym + "  totalAdd=" + totalAdd + " totalDup=" + totalDup);

                if (getEnv.checkLocalPC() == true) {
                    boolean flag = false;
                    if (flag == true) {
                        String nn12 = "_nn1_retarin_";
                        String filename = ServiceAFweb.FileLocalDebugPath + symbol + nn12 + ".csv";
                        FileUtil.FileWriteTextArray(filename, writeArray);
                    }
                }
                return 1;
            } catch (Exception e) {
                logger.info("> inputReTrainStockNeuralNetData exception " + BPnameSym + " - " + e.getMessage());
            }
        }
        return -1;
    }

//    public int stockTrainNeuralNet(ServiceAFweb serviceAFWeb, int TR_NN, String symbol) {
//        TrandingSignalProcess TRprocessImp = new TrandingSignalProcess();
////        logger.info("> processStockNeuralNet " + TR_Name + " " + symbol);
//
//        boolean nnsymTrain = true;
//        if (nnsymTrain == true) {
//            String nnName = ConstantKey.TR_NN1;
//            double errorNN = CKey.NN1_ERROR_THRESHOLD;
//            if (TR_NN == ConstantKey.INT_TR_NN2) {
//                nnName = ConstantKey.TR_NN2;
//                errorNN = CKey.NN2_ERROR_THRESHOLD;
//            }
//            String nnNameSym = nnName + "_" + symbol;
//            String BPname = CKey.NN_version + "_" + nnNameSym;
//            try {
//
//                AFneuralNet nnObj1 = serviceAFWeb.getNeuralNetObjWeight1(BPname, 0);
//                if (nnObj1 != null) {
//                    if (nnObj1.getStatus() != ConstantKey.OPEN) {
//                        return -1;
//                    }
//                }
//                int retflag = 0;
//                if (TR_NN == ConstantKey.INT_TR_NN1) {
//                    retflag = TRprocessImp.TRtrainingNN1NeuralNetData(serviceAFWeb, ConstantKey.TR_NN1, nnNameSym, errorNN);
//                } else if (TR_NN == ConstantKey.INT_TR_NN2) {
//                    retflag = TRprocessImp.TRtrainingNN2NeuralNetData(serviceAFWeb, nnNameSym, errorNN);
//                }
////                logger.info("> processStockNeuralNet ... Done");
//                return retflag;
//            } catch (Exception e) {
//                logger.info("> stockTrainNeuralNet exception " + BPname + " - " + e.getMessage());
//            }
//        }
//        return -1;
//    }
    //////////////
    public ArrayList<NNInputDataObj> getAccountStockTRListHistoryMACDNN(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI,
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
                inputList = getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);
                if (inputList == null) {
                    continue;
                }
                double parm1 = -1;
                if (signal == ConstantKey.S_BUY) {
                    parm1 = 0.9;
                } else if (signal == ConstantKey.S_SELL) {
                    parm1 = 0.1;
                }

                inputList.setInput1(parm1);
                inputList.setTrsignal(signal);
                ArrayList<Double> closeArray = getNNnormalizeInputClose(i, thObjListMACD);
                inputList.setInput6(closeArray.get(0));
                inputList.setInput7(closeArray.get(1));
                inputList.setInput8(closeArray.get(2));
                inputList.setInput9(closeArray.get(3));
                inputList.setInput10(closeArray.get(4));

//                ArrayList<Double> closeArray = getNNnormalizeStInputClose(i, thObjListMACD);
//                inputList.setInput6(closeArray.get(0));
//                inputList.setInput7(closeArray.get(1));
//                inputList.setInput8(closeArray.get(2));
//                inputList.setInput9(closeArray.get(3));
//                inputList.setInput10(closeArray.get(4));
//                inputList.setInput1(closeArray.get(5));
                int retDecision = checkNNsignalDecision(thObjMACD, prevThObj);

                double output = retDecision;

                NNInputDataObj objDataCur = new NNInputDataObj();
                objDataCur.setUpdatedatel(thObjMACD.getUpdateDatel());
                objDataCur.setObj(inputList);

                if (objDataPrev != null) {
                    if (output == 1) {
                        objDataPrev.getObj().setOutput1(0.9);
                        objDataPrev.getObj().setOutput2(0.1);
                    } else if (output == 0) {
                        objDataPrev.getObj().setOutput1(0.1);
                        objDataPrev.getObj().setOutput2(0.9);
                    } else {
                        objDataPrev.getObj().setOutput1(0.1);
                        objDataPrev.getObj().setOutput2(0.1);
                    }
                    trInputList.add(objDataPrev.getObj());
                    inputDatalist.add(objDataPrev);

//                    if (getEnv.checkLocalPC() == true) {
//                        if (CKey.NN_DEBUG == true) {
//
//                            NNInputOutObj objP = objDataPrev.getObj();
//                            String st = "\"" + objP.getDateSt() + "\",\"" + objP.getClose() + "\",\"" + objP.getTrsignal()
//                                    + "\",\"" + objP.getOutput1()
//                                    + "\",\"" + objP.getInput1() + "\",\"" + objP.getInput2() + "\",\"" + objP.getInput3()
//                                    + "\",\"" + objP.getInput4() + "\",\"" + objP.getInput5() + "\",\"" + objP.getInput6()
//                                    + "\",\"" + objP.getInput7() + "\",\"" + objP.getInput8()
//                                    + "\",\"" + objP.getInput9() + "\",\"" + objP.getInput10()
//                                    + "\"";
//                            logger.info(i + "," + st);
//                        }
//                    }
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

        return inputDatalist;
    }

    public ArrayList<NNInputDataObj> getAccountStockTRListHistoryMACDNN3(ArrayList<StockTRHistoryObj> thObjListMACD, ArrayList<StockTRHistoryObj> thObjListMV, ArrayList<StockTRHistoryObj> thObjListRSI,
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
                inputList = getNNnormalizeInput(i, thObjListMACD, thObjListMV, thObjListRSI);
                if (inputList == null) {
                    continue;
                }
                double parm1 = -1;
                if (signal == ConstantKey.S_BUY) {
                    parm1 = 0.9;
                } else if (signal == ConstantKey.S_SELL) {
                    parm1 = 0.1;
                }

                inputList.setInput1(parm1);
                inputList.setTrsignal(signal);
                ArrayList<Double> closeArray = getNNnormalizeInputClose(i, thObjListMACD);
                inputList.setInput6(closeArray.get(0));
                inputList.setInput7(closeArray.get(1));
                inputList.setInput8(closeArray.get(2));
                inputList.setInput9(closeArray.get(3));
                inputList.setInput10(closeArray.get(4));

                double output = getNNnormalizeStOutputClose(i, thObjListMACD);
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

        return inputDatalist;
    }

//    public static ArrayList<NNInputDataObj> NeuralNetGetNN1InputfromStaticCode(String symbol) {
//        StringBuffer inputBuf = new StringBuffer();
//        ArrayList<NNInputDataObj> inputlist = new ArrayList();
//        try {
//            inputBuf.append(nnData.NN_INPUTLIST1);
//            inputBuf.append(nnData.NN_INPUTLIST2);
//            inputBuf.append(nnData.NN_INPUTLIST3);
//            inputBuf.append(nnData.NN_INPUTLIST4);
//            inputBuf.append(nnData.NN_INPUTLIST5);
//            inputBuf.append(nnData.NN_INPUTLIST6);
//            inputBuf.append(nnData.NN_INPUTLIST7);
//            inputBuf.append(nnData.NN_INPUTLIST8);
////            inputBuf.append(nnData.NN_INPUTLIST9); //need to check nnData file
//
//            String inputListSt = ServiceAFweb.decompress(inputBuf.toString());
//            HashMap<String, ArrayList> stockInputMap = new HashMap<String, ArrayList>();
//            stockInputMap = new ObjectMapper().readValue(inputListSt, HashMap.class);
//            if (symbol != "") {
//                inputlist = stockInputMap.get(symbol);
//                if (inputlist == null) {
//                    return null;
//                }
//                String inputListRawSt = new ObjectMapper().writeValueAsString(inputlist);
//                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
//                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
//                inputlist = new ArrayList<NNInputDataObj>(listItem);
//                return inputlist;
//            }
//
//            for (String sym : stockInputMap.keySet()) {
//                ArrayList<NNInputDataObj> inputL = stockInputMap.get(sym);
//                String inputListRawSt = new ObjectMapper().writeValueAsString(inputL);
//                NNInputDataObj[] arrayItem = new ObjectMapper().readValue(inputListRawSt, NNInputDataObj[].class);
//                List<NNInputDataObj> listItem = Arrays.<NNInputDataObj>asList(arrayItem);
//                inputL = new ArrayList<NNInputDataObj>(listItem);
//                inputlist.addAll(inputL);
//            }
//
//            return inputlist;
//        } catch (Exception ex) {
//            logger.info("> NeuralNetGetNN1InputfromStaticCode - exception " + ex);
//        }
//        return null;
//    }
    /**
     * @param aStockNNretrainNameArray the stockNNretrainNameArray to set
     */
    public static void setStockNNretrainNameArray(ArrayList aStockNNretrainNameArray) {
        stockNNretrainNameArray = aStockNNretrainNameArray;
    }

}
