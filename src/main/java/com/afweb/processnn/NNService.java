/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.dbnndata.NNetdataImp;
import com.afweb.model.*;
import com.afweb.model.account.AccountObj;

import com.afweb.model.stock.*;
import com.afweb.nn.*;

import com.afweb.nnsignal.*;

import com.afweb.processstockinfo.StockInfoProcess;

import com.afweb.service.ServiceAFweb;
import com.afweb.service.ServiceAFwebREST;

import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author koed
 */
public class NNService {

    protected static Logger logger = Logger.getLogger("EmailService");
    private ServiceAFwebREST serviceAFwebREST = new ServiceAFwebREST();
    private NNetdataImp nndataImp = new NNetdataImp();

    public RequestObj SQLRequestNN(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String nameST = "";

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
                case ServiceAFweb.AllNeuralNet:
                    nameST = nndataImp.getAllNeuralNetDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;
                case ServiceAFweb.AllNeuralNetData:
                    nameST = nndataImp.getAllNeuralNetDataDBSQL(sqlObj.getReq());
                    sqlObj.setResp(nameST);
                    return sqlObj;

                case ServiceAFweb.NeuralNetDataObj: //NeuralNetDataObj = 120; //"120";      

                    try {
                        String BPname = sqlObj.getReq();
                        ArrayList<AFneuralNetData> retArray = nndataImp.getNeuralNetDataObj(BPname, 0);
                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);

                    } catch (Exception ex) {
                    }
                    return sqlObj;

                case ServiceAFweb.NeuralNetDataObjStockid: //NeuralNetDataObj = 121; //"121";        
                    try {
                        String BPname = sqlObj.getReq();

                        String stockID = sqlObj.getReq1();
                        int stockId121 = Integer.parseInt(stockID);

                        String updatedateSt = sqlObj.getReq2();
                        long updatedatel = Long.parseLong(updatedateSt);

                        ArrayList<AFneuralNetData> retArray = nndataImp.getNeuralNetDataObj(BPname, stockId121, updatedatel);
                        nameST = new ObjectMapper().writeValueAsString(retArray);
                        sqlObj.setResp("" + nameST);
                    } catch (Exception ex) {
                    }
                    return sqlObj;
                ////////////////////////////                    
                default:
                    return null;
            }
        } catch (Exception ex) {
            logger.info("> StockInfoSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    public void setNNetDataDataSource(DataSource dataSource, String URL) {
        nndataImp.setNNetDataDataSource(dataSource, URL);
    }

    public int initNNetDataDB(ServiceAFweb serviceAFWeb) {
        return nndataImp.initNNetDataDB();
    }
///////////////////////////    

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int length) {
        return nndataImp.getNeuralNetDataObj(name, length);
    }

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int stockId, long updatedatel) {
        return nndataImp.getNeuralNetDataObj(name, stockId, updatedatel);
    }

    public int deleteNeuralNetDataByBPname(String name) {
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
        return nndataImp.deleteNeuralNetData(name);
    }

    public int insertNeuralNetDataObject(AFneuralNetData neuralNetData) {
        return nndataImp.insertNeuralNetDataObject(neuralNetData);
    }

    private int insertNeuralNetDataObject(String name, int stockId, String data, long updatedatel) {
        return nndataImp.insertNeuralNetDataObject(name, stockId, data, updatedatel);
    }

    public int updateNeuralNetStatus0(String name, int status, int type) {
        return nndataImp.updateNeuralNetStatus0(name, status, type);
    }

    public int deleteNeuralNet0Table() {
        return nndataImp.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet1Table() {
        return nndataImp.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet0Rel(String name) {
        return nndataImp.deleteNeuralNet0Rel(name);
    }

    public int deleteNeuralNet1(String name) {
        return nndataImp.deleteNeuralNet1(name);
    }

    public int deleteNeuralNetDataTable() {
        return nndataImp.deleteNeuralNetDataTable();
    }

    public int updateNeuralNetStatus1(String name, int status, int type) {
        return nndataImp.updateNeuralNetStatus1(name, status, type);
    }

    public int updateNeuralNetDataObject(String name, int stockId, NNInputDataObj objData) {
        return nndataImp.updateNeuralNetDataObject(name, stockId, objData);
    }

    public int updateNeuralNetRef0(String name, ReferNameData refnameData) {
        return nndataImp.updateNeuralNetRef0(name, refnameData);
    }

    public int setCreateNeuralNetObj1(String name, String weight) {
        return nndataImp.setCreateNeuralNetObj1(name, weight);
    }

    public int updateNeuralNetRef1(String name, ReferNameData refnameData) {
        return nndataImp.updateNeuralNetRef1(name, refnameData);
    }

    public int deleteNeuralNetDataObjById(int id) {
        return nndataImp.deleteNeuralNetDataObjById(id);
    }

    //////////////////////////////////////////
    public String SystemClearNNinput(ServiceAFweb serviceAFWeb) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;

        retSatus = NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN1);
//            retSatus = NNProcessImp.ClearStockNNinputNameArray(this, ConstantKey.TR_NN2);

        return "" + retSatus;
    }

    public boolean SystemDeleteNN1Table(ServiceAFweb serviceAFWeb) {
        logger.info(">SystemDeleteNN1Table start ");
        nndataImp.deleteNeuralNet1Table();
        logger.info(">SystemDeleteNN1Table end ");
        return true;
    }

    public String SystemClearNNtran(ServiceAFweb serviceAFWeb, int tr) {

        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;
        if (tr == ConstantKey.SIZE_TR) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_MACD);
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_MV);
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_RSI);
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN1);
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN2);
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN3);
        } else if (tr == ConstantKey.INT_TR_ACC) {
            retSatus = NNProcessImp.ClearStockNNTranHistoryAllAcc(serviceAFWeb, ConstantKey.TR_ACC, "");
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_ACC);
        } else if (tr == ConstantKey.INT_TR_MACD) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_MACD);
        } else if (tr == ConstantKey.INT_TR_MV) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_MV);
        } else if (tr == ConstantKey.INT_TR_RSI) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_RSI);
        } else if (tr == ConstantKey.INT_TR_NN1) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN1);
        } else if (tr == ConstantKey.INT_TR_NN2) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN2);
        } else if (tr == ConstantKey.INT_TR_NN3) {
            retSatus = NNProcessImp.ClearStockNNTranHistory(serviceAFWeb, ConstantKey.TR_NN3);
        }

        return "" + retSatus;
    }

    public int releaseNeuralNetObj(ServiceAFweb serviceAFWeb, String name) {
        logger.info("> releaseNeuralNetObj " + name);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
//        return getStockImp().releaseNeuralNetObj(name);
        return nndataImp.releaseNeuralNetBPObj(name);
    }

    public AFneuralNet getNeuralNetObjWeight0(ServiceAFweb serviceAFWeb, String name, int type) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return nndataImp.getNeuralNetObjWeight0(name);
    }

    public AFneuralNet getNeuralNetObjWeight1(ServiceAFweb serviceAFWeb, String name, int type) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return nndataImp.getNeuralNetObjWeight1(name);
    }

    public int setNeuralNetObjWeight0(ServiceAFweb serviceAFWeb, AFneuralNet nn) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty

        int ret = nndataImp.setCreateNeuralNetObjRef0(nn.getName(), nn.getWeight(), nn.getRefname());
        return ret;
    }

    public int setNeuralNetObjWeight1(ServiceAFweb serviceAFWeb, AFneuralNet nn) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty
        return nndataImp.setCreateNeuralNetObj1(nn.getName(), nn.getWeight());
    }
////////////////////////////////////////////////////////////
    public static int cntNN = 0;

    public void AFprocessNeuralNet(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "AFprocessNeuralNet";

        if (serviceAFWeb.processNeuralNetFlag == true) {
            cntNN++;
            TradingNNprocess NNProcessImp = new TradingNNprocess();
            NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
            NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
            NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

            serviceAFWeb.nn1testflag = true;
            serviceAFWeb.nn2testflag = true;

            if (cntNN == 1) {
                nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(serviceAFWeb);
                return;
            } else if (cntNN == 2) {
                nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(serviceAFWeb);
                return;
            } else if (cntNN == 3) {
                nn30trend.ProcessTrainNeuralNetNN30ByTrend(serviceAFWeb);
                return;
            } else if (cntNN == 4) {
                NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);

                cntNN = 0;
                return;

            }

            cntNN = 0;
        }
    }

    public boolean processNewLearnNeuralNet(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "processNewLearnNeuralNet";

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN30ProcessByTrend nn1trend = new NN30ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            logger.info("Start processNewLearnNeuralNet.....Stock Size " + stockNameArray.size());
            for (int i = 0; i < stockNameArray.size(); i++) {

                String symbol = (String) stockNameArray.get(i);
                AFstockObj stock = serviceAFWeb.getStockBySymServ(symbol);
                if (stock == null) {
                    continue;
                }
                if (stock.getAfstockInfo() == null) {
                    continue;
                }

                boolean chk1 = nn1ProcBySig.checkNN1Ready(serviceAFWeb, symbol, true);
                boolean chk2 = nn2ProcBySig.checkNN2Ready(serviceAFWeb, symbol, true);
                if ((chk1 == true) && (chk2 == true)) {
                    continue;
                }

                String LockStock = "NN_New_" + symbol; // + "_" + trNN;
                LockStock = LockStock.toUpperCase();

                long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                long lockReturnStock = 1;

                lockReturnStock = serviceAFWeb.setLockNameServ(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "processNewLearnNeuralNet");

                if (lockReturnStock == 0) {
                    continue;
                }
                if (lockReturnStock > 0) {
                    try {

                        if (chk1 == false) {
                            nn1trend.TrainNN30NeuralNetByTrend(serviceAFWeb, symbol, ConstantKey.INT_TR_NN30, null);
                        }

                        if (chk1 == false) {
                            // process train symbol
                            nn1trend.TrainNN30NeuralNetByTrend(serviceAFWeb, symbol, ConstantKey.INT_TR_NN30, null);

                            for (int j = 0; j < 4; j++) {
                                nn1ProcBySig.TrainNN1NeuralNetBySign(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1, null);
                                NNProcessImp.ReLearnInputNeuralNet(serviceAFWeb, symbol, ConstantKey.INT_TR_NN1);
                            }
//                            logger.info("End processNewLearnNeuralNet.....NN1 " + symbol);
//                            return true;
                        } else if (chk2 == false) {
                            // process train symbol
                            for (int j = 0; j < 4; j++) {
                                nn2ProcBySig.TrainNN2NeuralNetBySign(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2, null);

                                NNProcessImp.ReLearnInputNeuralNet(serviceAFWeb, symbol, ConstantKey.INT_TR_NN2);
                            }

//                            logger.info("End processNewLearnNeuralNet.....NN2 " + symbol);
//                            return true;
                        }
                    } catch (Exception ex) {

                    }
                    serviceAFWeb.removeNameLock(LockStock, ConstantKey.NN_TR_LOCKTYPE);
                    return true;
                }

            }
        }
        logger.info("End processNewLearnNeuralNet.....");
        return false;
    }

    public void processNeuralNetTrain(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "processNeuralNetTrain";
        StockInfoProcess stockProcess = new StockInfoProcess();

        TradingSignalProcess TRprocessImp = new TradingSignalProcess();
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

        NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();

        TradingSignalProcess.forceToGenerateNewNN = false;
        if (serviceAFWeb.initLocalRemoteNN == true) {
            while (true) {
                processInitLocalRemoteNN(serviceAFWeb);

                logger.info("> Waiting 60 minutes ........");
                try {
                    Thread.sleep(30 * 1000 * 60);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (serviceAFWeb.processNeuralNetFlag == true) {
            int num = 0;
            while (true) {
                if (num == 0) {
                    boolean ret = processNewLearnNeuralNet(serviceAFWeb);
                    if (ret == false) {
                        num++;
                    }
                } else {
                    AFprocessNeuralNet(serviceAFWeb);
                    num++;
                    if (num > 2) {
                        num = 0;
                    }
                    if (cntNN == 0) {
                        num = 0;
                    }
                }

                logger.info("> Waiting 30 sec cntNN " + cntNN + "........");
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        int k = 0;
        while (true) {
            k++;
            boolean exitflag = true;

////////////////////////////////////////////////////////////////////////////
            if (serviceAFWeb.flagNNLearningSignal == true) {
                if (serviceAFWeb.nn1testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 1)) {
                        NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN1);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 1 cycle " + k);
                    nn1ProcBySig.ProcessTrainNN1NeuralNetBySign(serviceAFWeb);
                    logger.info("> ProcessTrainNeuralNet NN 1 end... cycle " + k);

                }
                if (serviceAFWeb.nn2testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN2);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 2 cycle " + k);

                    nn2ProcBySig.ProcessTrainNN2NeuralNetBySign(serviceAFWeb);
                    logger.info("> ProcessTrainNeuralNet NN 2 end... cycle " + k);

                }
                if (serviceAFWeb.nn3testflag == true) {
                    exitflag = false;
                    if (((k % 5) == 0) || (k == 0)) {
                        NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN3);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 3 cycle " + k);

                    nn3ProcBySig.ProcessTrainNN3NeuralNetBySign(serviceAFWeb);
                    logger.info("> ProcessTrainNeuralNet NN 3 end... cycle " + k);

                }
                if (serviceAFWeb.nn30testflag == true) {
                    exitflag = false;

                    if (((k % 5) == 0) || (k == 1)) {
                        NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN30);
                    }
                    logger.info("> ProcessTrainNeuralNet NN 30 cycle " + k);
                    nn30trend.ProcessTrainNeuralNetNN30ByTrend(serviceAFWeb);
                    logger.info("> ProcessTrainNeuralNet NN 30 end... cycle " + k);

                }
            }
////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////          
            if (serviceAFWeb.flagNNReLearning == true) {
                exitflag = false;

                logger.info("> ProcessReLeanInput NN 1 cycle " + k);
                NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);
                logger.info("> ProcessReLeanInput end... cycle " + k);

            }

////////////////////////////////////////////////////////////////////////////            
            if (serviceAFWeb.processRestinputflag == true) {
                if ((serviceAFWeb.nn1testflag == true) && (serviceAFWeb.nn2testflag == true)) {
                    // clear NN1 and NN2
                    logger.info("> processNNInputNeuralNet Clear NN DB..");
                    String nnName = ConstantKey.TR_NN1;
                    removeNeuralNetDataAllNNSymbolByTR(serviceAFWeb, nnName);

                    nnName = ConstantKey.TR_NN30;
                    removeNeuralNetDataAllNNSymbolByTR(serviceAFWeb, nnName);

                    nnName = ConstantKey.TR_NN2;
                    removeNeuralNetDataAllNNSymbolByTR(serviceAFWeb, nnName);

                    nnName = ConstantKey.TR_NN3;
                    removeNeuralNetDataAllNNSymbolByTR(serviceAFWeb, nnName);
                }

                if (serviceAFWeb.nn1testflag == true) {
                    logger.info("> processNN1InputNeuralNet Rest input..");
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn1ProcBySig.processNN1InputNeuralNet(serviceAFWeb);

                    nn30trend.processNN30InputNeuralNetTrend(serviceAFWeb);

                }
                if (serviceAFWeb.nn2testflag == true) {
                    logger.info("> processNN2InputNeuralNet Rest input..");
                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn2ProcBySig.processNN2InputNeuralNet(serviceAFWeb);

//                    nn2trend.processNN40InputNeuralNetTrend(this);
//                    nn2trend.processAllNN40StockInputNeuralNetTrend(this);
                    ///////////////////////////////
                }
                if (serviceAFWeb.nn3testflag == true) {
                    logger.info("> processNNInputNeuralNet Clear NN DB..");
                    String nnName = ConstantKey.TR_NN3;
                    removeNeuralNetDataAllNNSymbolByTR(serviceAFWeb, nnName);
                    logger.info("> processNN3InputNeuralNet Rest input..");

                    exitflag = true;
                    /// reset weight0 and use latest stock
                    /// remember to update nnData and nn3Data and version                
                    nn3ProcBySig.processNN3InputNeuralNet(serviceAFWeb);

//                    nn2trend.processNN40InputNeuralNetTrend(this);
//                    nn2trend.processAllNN40StockInputNeuralNetTrend(this);
                    ///////////////////////////////
                }

                logger.info("> processNN1InputNeuralNet Edn..");
                return;
            }
////////////////////////////////////////////////////////////////////////////
            if (serviceAFWeb.processRestAllStockflag == true) {
                exitflag = true;
                ///////////////////////////////   
                String symbolL[] = ServiceAFweb.primaryStock;

                ServiceAFweb.CreateStaticStockHistoryServ(serviceAFWeb, symbolL, "nnAllStock", "NN_ST");

                /////////////////////
                ArrayList<String> APIStockNameList = new ArrayList();
                ArrayList<AccountObj> accountAPIObjL = serviceAFWeb.getAccountListServ(CKey.API_USERNAME, null);
                if (accountAPIObjL != null) {
                    if (accountAPIObjL.size() > 0) {
                        AccountObj accountAPIObj = accountAPIObjL.get(0);
                        APIStockNameList = serviceAFWeb.SystemAccountStockNameList(accountAPIObj.getId());
                    }
                }
                String symbolPriL[] = ServiceAFweb.primaryStock;
                if (APIStockNameList.size() > 0) {
                    for (int i = 0; i < symbolPriL.length; i++) {
                        String sym = symbolPriL[i];
                        if (APIStockNameList.contains(sym)) {
                            APIStockNameList.remove(sym);
                        }
                    }
                    APIStockNameList.remove("T.T");
                }
                String symbolLallSt[] = ServiceAFweb.allStock;
                if (APIStockNameList.size() > 0) {
                    for (int i = 0; i < symbolLallSt.length; i++) {
                        String sym = symbolLallSt[i];
                        if (APIStockNameList.contains(sym)) {
                            continue;
                        }
                        APIStockNameList.add(sym);
                    }
                }
                /*ArrayList to Array Conversion */
                String SymbolAllOther[] = APIStockNameList.toArray(new String[APIStockNameList.size()]);
                ServiceAFweb.CreateStaticStockHistoryServ(serviceAFWeb, SymbolAllOther, "nnAllStock_1", "NN_ST1");

                return;
            }
////////////////////////////////////////////////////////////////////////////

            if (serviceAFWeb.processNNSignalAdmin == true) {
                exitflag = false;
                logger.info("> processNNSignalAdmin  cycle " + k);
                AccountTranProcess accountTranP = new AccountTranProcess();
                accountTranP.ProcessAdminSignalTrading(serviceAFWeb);

                serviceAFWeb.ProcessAllAccountTradingSignal(serviceAFWeb);
                serviceAFWeb.updateAllStockInfoSrv();
                logger.info("> processNNSignalAdmin end... cycle " + k);
            }
////////////////////////////////////////////////////////////////////////////
            if (exitflag == true) {
                break;
            }
            logger.info("> Waiting 30 sec........");
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

    public int removeNeuralNetDataAllNNSymbolByTR(ServiceAFweb serviceAFWeb, String TRname) {
        logger.info("> removeNeuralNetDataAllNNSymbolByTR Reset input.." + TRname);

        String BPname = CKey.NN_version + "_" + TRname;
        nndataImp.deleteNeuralNetDataByBPname(BPname);

        return removeNeuralNetDataSymbolListByTR(serviceAFWeb, TRname);
    }

    public int removeNeuralNetDataSymbolListByTR(ServiceAFweb serviceAFWeb, String TRname) {
        logger.info("> removeNeuralNetDataSymbolListByTR Reset input.." + TRname);

        String BPname = CKey.NN_version + "_" + TRname;

        AccountObj accountObj = serviceAFWeb.getAdminObjFromCache();
        ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());

        if (stockNameArray != null) {
            logger.info("> removeNeuralNetDataSymbolListByTR Stock " + stockNameArray.size());

            for (int i = 0; i < stockNameArray.size(); i++) {
                String symbol = (String) stockNameArray.get(i);

                String BPnameSym = CKey.NN_version + "_" + TRname + "_" + symbol;
                nndataImp.deleteNeuralNetDataByBPname(BPnameSym);
            }
        }

        return 1;
    }

    boolean initLRnn = false;

    public void processInitLocalRemoteNN(ServiceAFweb serviceAFWeb) {
        logger.info("> processInitLocalRemoteNN ");
        StockInfoProcess stockProcess = new StockInfoProcess();

        try {
            if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
                return;
            }
            if (initLRnn == false) {
                initLRnn = true;
                ArrayList<String> StockNameRemoteList = new ArrayList();

                AccountObj accountObj = serviceAFWeb.getAdminObjFromCache();
                ArrayList<String> stockNameArray1 = serviceAFwebREST.getRESTAccountStockNameList(CKey.ADMIN_USERNAME,
                        accountObj.getId() + "", CKey.URL_PATH_HERO);
                logger.info("> remote dB stock:" + stockNameArray1.size());
                StockNameRemoteList.addAll(stockNameArray1);
                ArrayList<String> stockNameArray2 = serviceAFwebREST.getRESTAccountStockNameList(CKey.ADMIN_USERNAME,
                        accountObj.getId() + "", CKey.URL_PATH_HERO_1);
                logger.info("> remote dB1 stock:" + stockNameArray2.size());
                StockNameRemoteList.addAll(stockNameArray2);

                ArrayList<AccountObj> accountAPIObjL = serviceAFWeb.getAccountListServ(CKey.API_USERNAME, null);
                if (accountAPIObjL == null) {
                    return;
                }
                if (accountAPIObjL.size() == 0) {
                    return;
                }
////////////////////////////////
                // Add or remove stock
                AccountObj accountAPIObj = accountAPIObjL.get(0);
                ArrayList<String> APIStockNameList = serviceAFWeb.SystemAccountStockNameList(accountAPIObj.getId());
                if (APIStockNameList == null) {
                    return;
                }
                logger.info("> API stock:" + APIStockNameList.size() + " remote dB stock:" + StockNameRemoteList.size());
                ArrayList addedList = new ArrayList();

                ArrayList removeList = new ArrayList();
                boolean result = AccountTranProcess.compareStockList(StockNameRemoteList, APIStockNameList, addedList, removeList);
                if (result == true) {
                    for (int i = 0; i < addedList.size(); i++) {
                        String symbol = (String) addedList.get(i);
                        if (symbol.equals("T_T")) {
                            continue;
                        }
                        int resultAdd = serviceAFWeb.addAccountStockByCustAccServ(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Add API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                    for (int i = 0; i < removeList.size(); i++) {
                        String symbol = (String) removeList.get(i);
                        int resultRemove = serviceAFWeb.removeAccountStockByUserNameAccIdServ(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Remove API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                }
////////////////////////////////////////////////                
                ////update all stock                
                serviceAFWeb.ProcessAdminAddRemoveStock(serviceAFWeb);
                serviceAFWeb.ProcessAdminAddRemoveStock(serviceAFWeb);

                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountAdminObj.getId());

                String printName = "";
                for (int i = 0; i < stockNameArray.size(); i++) {
                    printName += stockNameArray.get(i) + ",";
                }
                logger.info("processInitLocalRemoteNN " + printName);
////////////////////////////////////////////////                
                ////update remote Neural Net
                String URL = CKey.URL_PATH_HERO;
                String nnName = ConstantKey.TR_NN1;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN2;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN30;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
////////////////
////////////////
                URL = CKey.URL_PATH_HERO_1;
                nnName = ConstantKey.TR_NN1;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN2;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
                nnName = ConstantKey.TR_NN30;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, URL);
////////////////////////////////////////////////  

                logger.info("> update  stock:" + stockNameArray.size());
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String symbol = (String) stockNameArray.get(i);
                    if (symbol.equals("T_T")) {
                        continue;
                    }
                    int re = stockProcess.updateAllStockProcess(serviceAFWeb, symbol, false);
                    if ((i % 5) == 0) {
                        logger.info("> updated: " + i);
                    }
                }
                serviceAFWeb.ProcessAdminAddRemoveStock(serviceAFWeb);

            }

        } catch (Exception ex) {
            logger.info("> processInitLocalRemoteNN Exception " + ex.getMessage());
        }
    }

    private int updateRESTNNWeight0(ServiceAFweb serviceAFWeb, ArrayList<String> APIStockNameList, String nnName, String URL) {
        if (APIStockNameList == null) {
            return 0;
        }

        logger.info("> updateRESTNNWeight0 " + nnName + " " + APIStockNameList.size() + " " + URL);

        String BPnameSym = CKey.NN_version + "_" + nnName;
        NNService nnservice = new NNService();
        AFneuralNet nnObj1 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
        if (nnObj1 != null) {
            serviceAFwebREST.setNeuralNetObjWeight0(nnObj1, URL);
        }

        for (int i = 0; i < APIStockNameList.size(); i++) {
            String symbol = (String) APIStockNameList.get(i);
            if (symbol.equals("T_T")) {
                continue;
            }

            ///*****Make sure the DB name is HOU.TO.
            ///*****Make sure the DB name is RY.TO.
            ///*****Make sure the DB name is .
            BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
            try {
                nnObj1 = nnservice.getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
                if (nnObj1 != null) {

                    int ret = serviceAFwebREST.setNeuralNetObjWeight0(nnObj1, URL);
                    ServiceAFweb.AFSleep1Sec(3);
                    if (ret != 1) {
                        logger.info("> updateRESTNNWeight0 " + BPnameSym + " ret=" + ret);
                    }
                } else {

                    logger.info("> updateRESTNNWeight0 not found " + BPnameSym);
                }
            } catch (Exception ex) {
                logger.info("> updateRESTNNWeight0 Exception " + ex.getMessage());
            }

        }
        return 1;
    }

    public void fileNNInputOutObjList(ServiceAFweb serviceAFWeb, ArrayList<NNInputDataObj> inputList, String symbol, int stockId, String filename) {
        if (getEnv.checkLocalPC() == false) {
            return;
        }
        if (inputList != null) {
            //merge inputlistSym
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

            FileUtil.FileWriteTextArray(filename, writeArray);
        }
    }
///////////////////////////////

}
