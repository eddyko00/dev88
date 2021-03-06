/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.processcustacc.AccountTranProcess;
import com.afweb.processsignal.TradingSignalProcess;
import com.afweb.model.nn.*;

import com.afweb.dbnndata.NNetdataImp;
import com.afweb.model.*;
import com.afweb.model.account.AccountObj;

import com.afweb.model.stock.*;
import com.afweb.nnBP.NNBPservice;

import com.afweb.processcache.ECacheService;

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
public class NNetService {

    protected static Logger logger = Logger.getLogger("NNetService");
    private ServiceAFwebREST serviceAFwebREST = new ServiceAFwebREST();
    private NNetdataImp nndataImp = new NNetdataImp();

    public RequestObj SQLRequestNN(ServiceAFweb serviceAFWeb, RequestObj sqlObj) {

        String nnSt = "";
        String nameST = "";
        AFneuralNet afNeuralNet = null;
        int result = 0;

        try {
            String typeCd = sqlObj.getCmd();
            int type = Integer.parseInt(typeCd);

            switch (type) {
//
                case ServiceAFweb.SetNeuralNetObjWeight0:
                    try {
                        nnSt = sqlObj.getReq();
                        afNeuralNet = new ObjectMapper().readValue(nnSt, AFneuralNet.class);
                        result = setNeuralNetObjWeight0(serviceAFWeb, afNeuralNet);

                        sqlObj.setResp("" + result);

                    } catch (Exception ex) {
                        sqlObj.setResp("" + -1);
                    }
                    return sqlObj;
                case ServiceAFweb.SetNeuralNetObjWeight1:
                    try {
                        nnSt = sqlObj.getReq();
                        afNeuralNet = new ObjectMapper().readValue(nnSt, AFneuralNet.class);
                        result = setNeuralNetObjWeight1(serviceAFWeb, afNeuralNet);

                        sqlObj.setResp("" + result);

                    } catch (Exception ex) {
                        sqlObj.setResp("" + -1);
                    }
                    return sqlObj;
//                case ServiceAFweb.AllNeuralNet:
//                    nameST = nndataImp.getAllNeuralNetDBSQL(sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;
//                case ServiceAFweb.AllNeuralNetData:
//                    nameST = nndataImp.getAllNeuralNetDataDBSQL(sqlObj.getReq());
//                    sqlObj.setResp(nameST);
//                    return sqlObj;

//                case ServiceAFweb.NeuralNetDataObj: //NeuralNetDataObj = 120; //"120";      
//
//                    try {
//                        String BPname = sqlObj.getReq();
//                        ArrayList<AFneuralNetData> retArray = nndataImp.getNeuralNetDataObj(BPname, 0);
//                        nameST = new ObjectMapper().writeValueAsString(retArray);
//                        sqlObj.setResp("" + nameST);
//
//                    } catch (Exception ex) {
//                    }
//                    return sqlObj;
//                case ServiceAFweb.NeuralNetDataObjStockid: //NeuralNetDataObj = 121; //"121";        
//                    try {
//                        String BPname = sqlObj.getReq();
//
//                        String stockID = sqlObj.getReq1();
//                        int stockId121 = Integer.parseInt(stockID);
//
//                        String updatedateSt = sqlObj.getReq2();
//                        long updatedatel = Long.parseLong(updatedateSt);
//
//                        ArrayList<AFneuralNetData> retArray = nndataImp.getNeuralNetDataObjByStockId(BPname, stockId121, updatedatel);
//                        nameST = new ObjectMapper().writeValueAsString(retArray);
//                        sqlObj.setResp("" + nameST);
//                    } catch (Exception ex) {
//                    }
//                    return sqlObj;
                ////////////////////////////                    
                default:
                    return null;
            }
        } catch (Exception ex) {
            logger.info("> StockInfoSQLRequest exception " + sqlObj.getCmd() + " - " + ex.getMessage());
        }
        return null;
    }

    public void setNNDataDataSource(DataSource dataSource, String URL) {
        nndataImp.setNNDataDataSource(dataSource, URL);
    }

    public boolean restNNdataDB(ServiceAFweb serviceAFWeb) {
        return nndataImp.restNNdataDB();
    }

    public boolean cleanNNdataDB(ServiceAFweb serviceAFWeb) {
        return nndataImp.cleanNNdataDB();
    }

    public int initNNetDataDB(ServiceAFweb serviceAFWeb) {
        return nndataImp.initNNetDataDB();
    }

    public int updateSQLNNArrayList(ServiceAFweb serviceAFWeb, ArrayList SQLTran) {
        return nndataImp.updateSQLNNArrayList(SQLTran);
    }
///////////////////////////    

    public ArrayList<String> getAllIdNNetDataSQL(String sql) {
        return nndataImp.getAllIdNNSQL(sql);
    }

    public String getAllNeuralNetDBSQL(String sql) {
        return nndataImp.getAllNeuralNetDBSQL(sql);
    }

    public String getAllNeuralNetDataDBSQL(String sql) {
        return nndataImp.getAllNeuralNetDataDBSQL(sql);
    }

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int length) {
        return nndataImp.getNeuralNetDataObj(name, length);
    }
    public ArrayList<AFneuralNetData> getNeuralNetDataObjByRef(String name, String refName) {
        return nndataImp.getNeuralNetDataObjByRef(name, refName);
    }
    public ArrayList<AFneuralNetData> getNeuralNetDataObjByStockId(String name, String refname, int stockId, long updatedatel) {
        return nndataImp.getNeuralNetDataObjByStockId(name, refname, stockId, updatedatel);
    }

    public int deleteNeuralNetDataByBPname(String name) {
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
        return nndataImp.deleteNeuralNetData(name);
    }

    public int insertNeuralNetDataObject(AFneuralNetData neuralNetData) {
        return nndataImp.insertNeuralNetDataObject(neuralNetData);
    }

//    private int insertNeuralNetDataObject(String name, int stockId, String data, long updatedatel) {
//        return nndataImp.insertNeuralNetDataObject(name, stockId, data, updatedatel);
//    }
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
    public int updateNeuralNetDataRefName(int id, String refname) {
        return nndataImp.updateNeuralNetDataRefName(id, refname);
    }
    
    public int addNeuralNetDataObject(String name, String sym, int stockId, NNInputDataObj objData) {
        return nndataImp.addNeuralNetDataObject(name, sym, stockId, objData);
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
    public ReferNameData getReferNameData(AFneuralNet nnObj0) {
        ReferNameData refData = new ReferNameData();
        String refName = nnObj0.getRefname();
        try {
            if ((refName != null) && (refName.length() > 0)) {
                refName = refName.replaceAll("#", "\"");
                refData = new ObjectMapper().readValue(refName, ReferNameData.class);
                return refData;
            }
        } catch (Exception ex) {
        }
        return refData;
    }

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

    public int releaseNeuralNetObj(ServiceAFweb serviceAFWeb, String name) {
        logger.info("> releaseNeuralNetObj " + name);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release
        String nameSt = nndataImp.getNeuralNetName1(name);
        if (nameSt != null) {

            AFneuralNet nnObj1 = nndataImp.getNeuralNetObjWeight1(name);
            NNBPservice nnTemp = new NNBPservice();
            nnTemp.createNet(nnObj1.getWeight());
            nnTemp.setInputpattern(null);
            nnTemp.setOutputpattern(null);
            String weightSt = nnTemp.getNetObjSt();
            int ret = nndataImp.setCreateNeuralNetObj0(name, weightSt);
            if (ret == 1) {
                nndataImp.updateNeuralNetStatus0(name, ConstantKey.OPEN, 0);

                setCreateNeuralNetObj1(name, "");
                return nndataImp.updateNeuralNetStatus1(name, ConstantKey.COMPLETED, 0);
            }
        }
        return 0;
    }

    public AFneuralNet getNeuralNetObjWeight0(ServiceAFweb serviceAFWeb, String name, int type) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        if (ECacheService.cacheFlag == true) {
            AFneuralNet aFneuralNet = ECacheService.getNeuralNetObjWeight0(name);
            if (aFneuralNet == null) {
                aFneuralNet = nndataImp.getNeuralNetObjWeight0(name);
                if (aFneuralNet != null) {
                    ECacheService.putNeuralNetObjWeight0(name, aFneuralNet);
                }
            }
            return aFneuralNet;
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

    public void AFprocessNeuralNetData(ServiceAFweb serviceAFWeb) {
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
                if ((NNProcessImp.stockNNretrainNameArray != null)
                        && (NNProcessImp.stockNNretrainNameArray.size() > 0)) {
                    NNProcessImp.ProcessReLearnInputNeuralNet(serviceAFWeb);
                }
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

        AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
        ArrayList stockNameArray = serviceAFWeb.InfGetStockINfioNameList(accountAdminObj.getId());

        if (stockNameArray != null) {
            logger.info("Start processNewLearnNeuralNet.....Stock Size " + stockNameArray.size());
            for (int i = 0; i < stockNameArray.size(); i++) {

                String symbol = (String) stockNameArray.get(i);
                AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(symbol);
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

                lockReturnStock = serviceAFWeb.InfoSetLockName(LockStock, ConstantKey.NN_TR_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "processNewLearnNeuralNet");

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
                    serviceAFWeb.InfoRemoveLockName(LockStock, ConstantKey.NN_TR_LOCKTYPE);
                    return true;
                }

            }
        }
        logger.info("End processNewLearnNeuralNet.....");
        return false;
    }

    public void AFprocessNeuralNetTrain(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "processNeuralNetTrain";

        TradingNNprocess NNProcessImp = new TradingNNprocess();
        NN1ProcessBySignal nn1ProcBySig = new NN1ProcessBySignal();
        NN30ProcessByTrend nn30trend = new NN30ProcessByTrend();
        NN2ProcessBySignal nn2ProcBySig = new NN2ProcessBySignal();

        NN3ProcessBySignal nn3ProcBySig = new NN3ProcessBySignal();

        TradingSignalProcess.forceToGenerateNewNN = false;
        if (serviceAFWeb.initLocalRemoteNN == true) {
            while (true) {
                AFprocessInitLocalRemoteNN(serviceAFWeb);

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
                    AFprocessNeuralNetData(serviceAFWeb);
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
                //////////////////
                CKey.hou3to1 = true; //false;      
                CKey.hod1to4 = true; //false; 
                /////////////////
                ServiceAFweb.SysCreateStaticStockHistoryServ(serviceAFWeb, symbolL, "nnAllStock", "NN_ST");

                ArrayList<String> APIStockNameList = new ArrayList();
                if (CKey.dbinfonnflag == true) {
                    String SymbolAllOther[] = APIStockNameList.toArray(new String[APIStockNameList.size()]);
                    ServiceAFweb.SysCreateStaticStockHistoryServ(serviceAFWeb, SymbolAllOther, "nnAllStock_1", "NN_ST1");
                    return;
                }

                /////////////////////
                ArrayList<AccountObj> accountAPIObjL = serviceAFWeb.AccGetAccountList(CKey.API_USERNAME, null);
                if (accountAPIObjL != null) {
                    if (accountAPIObjL.size() > 0) {
                        AccountObj accountAPIObj = accountAPIObjL.get(0);
                        APIStockNameList = serviceAFWeb.InfGetStockINfioNameList(accountAPIObj.getId());
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
                ServiceAFweb.SysCreateStaticStockHistoryServ(serviceAFWeb, SymbolAllOther, "nnAllStock_1", "NN_ST1");

                return;
            }
////////////////////////////////////////////////////////////////////////////

            if (serviceAFWeb.processNNSignalAdmin == true) {
                exitflag = false;
                logger.info("> processNNSignalAdmin  cycle " + k);
                AccountTranProcess accountTranP = new AccountTranProcess();
                accountTranP.ProcessAdminSignalTrading(serviceAFWeb);

                serviceAFWeb.ProcessAllAccountTradingSignal(serviceAFWeb);
                serviceAFWeb.ProcessUpdateAllStockInfo();
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

        AccountObj accountObj = serviceAFWeb.SysGetAdminObjFromCache();
        ArrayList stockNameArray = serviceAFWeb.InfGetStockINfioNameList(accountObj.getId());

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

    public void AFprocessInitLocalRemoteNN(ServiceAFweb serviceAFWeb) {
        logger.info("> processInitLocalRemoteNN ");
        StockInfoProcess stockProcess = new StockInfoProcess();

        try {
            if (CKey.SQL_DATABASE != CKey.LOCAL_MYSQL) {
                return;
            }
            if (initLRnn == false) {
                initLRnn = true;
                ArrayList<String> StockNameRemoteList = new ArrayList();
                ArrayList<String> stockNameArray1 = new ArrayList();
                ArrayList<String> stockNameArray2 = new ArrayList();
                AccountObj accountObj = serviceAFWeb.SysGetAdminObjFromCache();

                for (int i = 0; i < 15; i++) {
                    String remoteURL = CKey.URL_PATH_HERO;

                    stockNameArray1 = serviceAFwebREST.
                            RESTGetAccountStockNameList(serviceAFWeb, remoteURL, CKey.ADMIN_USERNAME, accountObj.getId());
                    logger.info("> remote dB stock:" + stockNameArray1.size());

                    remoteURL = CKey.URL_PATH_HERO_1;
                    stockNameArray2 = serviceAFwebREST.
                            RESTGetAccountStockNameList(serviceAFWeb, remoteURL, CKey.ADMIN_USERNAME, accountObj.getId());
                    logger.info("> remote dB1 stock:" + stockNameArray2.size());

                    if ((stockNameArray1.size() > 0) && (stockNameArray2.size() > 0)) {
                        break;
                    }
                    ServiceAFweb.AFSleep1Sec(10);
                }
                if ((stockNameArray1.size() > 0) && (stockNameArray2.size() > 0)) {
                    ;
                } else {
                    logger.info("> processInitLocalRemoteNN - Please try again...");
                    return;
                }

                StockNameRemoteList.addAll(stockNameArray1);
                StockNameRemoteList.addAll(stockNameArray2);
                ArrayList<AccountObj> accountAPIObjL = serviceAFWeb.AccGetAccountList(CKey.API_USERNAME, null);
                if (accountAPIObjL == null) {
                    return;
                }
                if (accountAPIObjL.size() == 0) {
                    return;
                }
////////////////////////////////
                // Add or remove stock
                AccountObj accountAPIObj = accountAPIObjL.get(0);
                ArrayList<String> APIStockNameList = serviceAFWeb.InfGetStockINfioNameList(accountAPIObj.getId());
                if (APIStockNameList == null) {
                    return;
                }
                logger.info("> API stock:" + APIStockNameList.size() + " remote dB stock:" + StockNameRemoteList.size());
                ArrayList addedList = new ArrayList();

                ArrayList removeList = new ArrayList();
                boolean result = AccountTranProcess.compareStockList(StockNameRemoteList, APIStockNameList, addedList, removeList);
                if (result == true) {
                    addedList.remove("T_T");
                    addedList.remove("T_T");
                    addedList.remove("T_T");
                    logger.info("> Stock addedList " + addedList.size());
                    for (int i = 0; i < addedList.size(); i++) {
                        String symbol = (String) addedList.get(i);
                        if (symbol.equals("T_T")) {
                            continue;
                        }
                        int resultAdd = serviceAFWeb.AccAddAccountStockByCustAccServ(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Add API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                    logger.info("> Stock removeList " + removeList.size());
                    for (int i = 0; i < removeList.size(); i++) {
                        String symbol = (String) removeList.get(i);
                        int resultRemove = serviceAFWeb.AccRemoveAccountStockByUserNameAccId(CKey.API_USERNAME, null, accountAPIObj.getId() + "", symbol);
                        logger.info("> Remove API stock " + symbol);

                        ServiceAFweb.AFSleep();

                    }
                }
////////////////////////////////////////////////                
                ////update all stock                
                serviceAFWeb.ProcessAdminAddRemoveStock(serviceAFWeb);
                serviceAFWeb.ProcessAdminAddRemoveStock(serviceAFWeb);

                AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
                ArrayList stockNameArray = serviceAFWeb.InfGetStockINfioNameList(accountAdminObj.getId());

                String printName = "";
                for (int i = 0; i < stockNameArray.size(); i++) {
                    printName += stockNameArray.get(i) + ",";
                }
                logger.info("processInitLocalRemoteNN " + printName);
////////////////////////////////////////////////                

                ////update remote Neural Net
                String RestURL = CKey.URL_PATH_HERO;

                String nnName = ConstantKey.TR_NN1;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
                nnName = ConstantKey.TR_NN2;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
                nnName = ConstantKey.TR_NN30;
                this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
////////////////
////////////////
                if (CKey.dbinfonnflag == true) {
                    ;
                } else {
                    RestURL = CKey.URL_PATH_HERO_1;

                    nnName = ConstantKey.TR_NN1;
                    this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
                    nnName = ConstantKey.TR_NN2;
                    this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
                    nnName = ConstantKey.TR_NN30;
                    this.updateRESTNNWeight0(serviceAFWeb, stockNameArray, nnName, RestURL);
                }
////////////////////////////////////////////////  

                logger.info("> update  stock:" + stockNameArray.size());
                for (int i = 0; i < stockNameArray.size(); i++) {
                    String symbol = (String) stockNameArray.get(i);
                    if (symbol.equals("T_T")) {
                        continue;
                    }
                    int re = stockProcess.updateAllStockInfoProcess(serviceAFWeb, symbol);
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

    private int updateRESTNNWeight0(ServiceAFweb serviceAFWeb, ArrayList<String> APIStockNameList, String nnName, String RestURL) {
        if (APIStockNameList == null) {
            return 0;
        }

        logger.info("> updateRESTNNWeight0 " + nnName + " " + APIStockNameList.size() + " " + RestURL);

        String BPnameSym = CKey.NN_version + "_" + nnName;

        AFneuralNet nnObj1 = getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);
        if (nnObj1 != null) {
            serviceAFwebREST.RESTSetNeuralNetObjWeight0(serviceAFWeb, RestURL, nnObj1);
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
                nnObj1 = getNeuralNetObjWeight0(serviceAFWeb, BPnameSym, 0);

                if (nnObj1 != null) {
                    if (checkReady(nnObj1, nnName) == false) {
                        logger.info("> updateRESTNNWeight0 " + BPnameSym + " not ready");
                    } else {
                        int ret = serviceAFwebREST.RESTSetNeuralNetObjWeight0(serviceAFWeb, RestURL, nnObj1);
                        ServiceAFweb.AFSleep1Sec(3);
                        if (ret != 1) {
                            logger.info("> updateRESTNNWeight0 " + BPnameSym + " ret=" + ret);
                        }
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

    public boolean checkReady(AFneuralNet nnObj1, String nnName) {
        if (nnName.equals(ConstantKey.TR_NN30)) {
            return true;
        }
        ReferNameData refData = getReferNameData(nnObj1);
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
        return true;
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
