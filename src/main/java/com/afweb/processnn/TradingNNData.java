/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.model.nn.*;
import com.afweb.model.stock.AFneuralNetData;

import com.afweb.service.*;
import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class TradingNNData {

    public static Logger logger = Logger.getLogger("TradingNNData");
    NNetService nnSrv = new NNetService();

    public int saveNNBaseDataDB(ServiceAFweb serviceAFWeb, String nnName, HashMap<String, ArrayList> stockInputMap) {
        ServiceAFweb.lastfun = "saveNNBaseDataDB";
        String BPnameSym = CKey.NN_version + "_" + nnName;
        try {

            ArrayList<AFneuralNetData> objDataList = serviceAFWeb.NnNeuralNetDataObjSystem(BPnameSym);
            if (objDataList.size() > 300) {
                // already saved
                logger.info(">>>>>>>>>>>> saveNNBaseDataDB " + BPnameSym + " No Save. Already exist.");
                return 1;
            }

            int added = 0;
            int total = 0;
            ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
            for (String sym : stockInputMap.keySet()) {
                inputlistSym = stockInputMap.get(sym);
                if (inputlistSym == null) {
                    continue;
                }

                total += inputlistSym.size();
//                logger.info("> saveNNdataDB - " + sym + " " + inputlistSym.size());
                for (int i = 0; i < inputlistSym.size(); i++) {
                    NNInputDataObj objData = inputlistSym.get(i);
                    serviceAFWeb.NnAddNeuralNetDataObject(BPnameSym, "", 0, objData);
                    added++;

                }
            }
            logger.info(">>>>>>>>>>>> saveNNdataDB - " + BPnameSym + " added " + added + " of " + total);
            logger.info(">>>>>>>>>>>> saveNNdataDB - " + BPnameSym + " added " + added + " of " + total);
            return 1;
        } catch (Exception ex) {
            logger.info("> saveNNdataDB - exception - " + BPnameSym + " " + ex);
        }
        return 0;
    }

    public int getNNBaseDataDB(ServiceAFweb serviceAFWeb, String nnName, ArrayList<NNInputDataObj> inputlist) {
        ServiceAFweb.lastfun = "getNNBaseDataDB";
        ArrayList<AFneuralNetData> objDataList = new ArrayList();
        String BPnameSym = CKey.NN_version + "_" + nnName;
        try {
            objDataList = serviceAFWeb.NnNeuralNetDataObjSystem(BPnameSym);
            if (objDataList != null) {
                logger.info("> getNNdataDB " + BPnameSym + " " + objDataList.size());
                for (int i = 0; i < objDataList.size(); i++) {
                    String dataSt = objDataList.get(i).getData();
                    NNInputOutObj input;
                    input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                    NNInputDataObj inputObj = new NNInputDataObj();
                    inputObj.setObj(input);
                    inputObj.setUpdatedatel(objDataList.get(i).getUpdatedatel());
                    inputlist.add(inputObj);
                }
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveNNdataDB - exception - " + BPnameSym + " " + ex);
        }
        return 0;
    }

    public int getNNOtherDataDB(ServiceAFweb serviceAFWeb, String nnName, ArrayList<NNInputDataObj> inputlist) {
        String symbol = "";
        String symbolL[] = ServiceAFweb.primaryStock;
        for (int i = 0; i < symbolL.length; i++) {
            symbol = symbolL[i];

            ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
            int ret = this.getNNOtherDataDBProcess(serviceAFWeb, nnName, symbol, inputlistSym, 4);
//            logger.info("> getNNOtherDataDB " + nnName + " " + inputlistSym.size());
            if (ret == 1) {
                inputlist.addAll(inputlistSym);
            }
        }
        return 1;

    }

    private int getNNOtherDataDBProcess(ServiceAFweb serviceAFWeb, String nnName, String symbol, ArrayList<NNInputDataObj> inputlist, int length) {
        ServiceAFweb.lastfun = "getNNOtherDataDB";
        ArrayList<AFneuralNetData> objDataList = new ArrayList();
        String BPnameSym = CKey.NN_version + "_" + nnName + "_" + symbol;
        try {
            //order by desc 
            objDataList = serviceAFWeb.NnGetNeuralNetDataObj(BPnameSym, length);
            if (objDataList != null) {
//                logger.info("> getNNOtherDataDB " + BPnameSym + " " + objDataList.size());
                for (int i = 0; i < objDataList.size(); i++) {
                    String dataSt = objDataList.get(i).getData();
                    NNInputOutObj input;
                    input = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);
                    NNInputDataObj inputObj = new NNInputDataObj();
                    inputObj.setObj(input);
                    inputObj.setUpdatedatel(objDataList.get(i).getUpdatedatel());
                    inputlist.add(inputObj);
                }
                return 1;
            }
        } catch (Exception ex) {
            logger.info("> saveNNdataDB - exception - " + BPnameSym + " " + ex);
        }
        return 0;
    }

////////
    public int processClearDataRefName(ServiceAFweb serviceAFWeb) {

        String tableName = "neuralnetdata";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        logger.info("> processClearDataRefName " + len);
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = clearDBneuralnetData(tableName, first, last);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = clearDBneuralnetData(tableName, first, last);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
            }
            return 1;
        }
        return 0;
    }

    private int clearDBneuralnetData(String tableName, String first, String last) {
        try {
//            logger.info("> saveDBneuralnetDataPro - " + first + " " + last);
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }

            String output = nnSrv.getAllNeuralNetDataDBSQL(sql);
            if (output == null) {
                return 0;
            }
            ArrayList<AFneuralNetData> array = null;
            AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class);
            List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
            array = new ArrayList<AFneuralNetData>(listItem);

            NNetService nnSrv = new NNetService();
            logger.info("> clearDBneuralnetData " + array.size());

            for (int i = 0; i < array.size(); i++) {
                AFneuralNetData objData = array.get(i);
                String st = "";
                nnSrv.updateNeuralNetDataRefName(objData.getId(), st);
                ServiceAFweb.AFSleep();
                ServiceAFweb.AFSleep();
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> clearDBneuralnetData " + ex);
        }
        return 0;
    }
//

    public int processSetDataRefName(ServiceAFweb serviceAFWeb) {

        String tableName = "neuralnetdata";
        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        logger.info("> processSetDataRefName " + len);
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = updateDBneuralnetData(tableName, first, last);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = updateDBneuralnetData(tableName, first, last);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
            }
            return 1;
        }
        return 0;
    }

    private int updateDBneuralnetData(String tableName, String first, String last) {

        try {
//            logger.info("> saveDBneuralnetDataPro - " + first + " " + last);
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
            logger.info("> updateDBneuralnetData ");
            String output = nnSrv.getAllNeuralNetDataDBSQL(sql);
            if (output == null) {
                return 0;
            }
            ArrayList<AFneuralNetData> array = null;
            AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class);
            List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
            array = new ArrayList<AFneuralNetData>(listItem);

            NNetService nnSrv = new NNetService();
            logger.info("> updateDBneuralnetData " + array.size());
            for (int i = 0; i < array.size(); i++) {
                AFneuralNetData objData = array.get(i);
                String dataSt = objData.getData();
                NNInputOutObj obj = new ObjectMapper().readValue(dataSt, NNInputOutObj.class);

                String refName = ""
                        + obj.getInput1()
                        + "," + obj.getInput2()
                        + "," + obj.getInput3()
                        + "," + obj.getInput4()
                        + "," + obj.getInput5()
                        + "," + obj.getInput6()
                        + "," + obj.getInput7() + "," + obj.getInput8()
                        + "," + obj.getInput9() + "," + obj.getInput10()
                        //                        + "," + obj.getOutput1()
                        //                        + "," + obj.getOutput2()
                        + "";
                // check if already exist
                ArrayList<AFneuralNetData> nnDataObjL = nnSrv.getNeuralNetDataObjByRef(objData.getName(), refName);
                ServiceAFweb.AFSleep();
                if ((nnDataObjL != null) && (nnDataObjL.size() > 0)) {
                    // already exist
                    AFneuralNetData nnDataObj = nnDataObjL.get(0);
                    logger.info("> nnDataObj already exist " + objData.getName() + " " + nnDataObj.getUpdatedatedisplay() + " " + refName);

                } else {
                    nnSrv.updateNeuralNetDataRefName(objData.getId(), refName);
                }
                ServiceAFweb.AFSleep();
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> updateDBneuralnetData " + ex);
        }
        return 0;
    }
//

    private ArrayList<String> getDBDataTableId(ServiceAFweb serviceAFWeb, String table) {
        try {

            String sql = "select id from " + table + " order by id asc";
            return nnSrv.getAllIdNNetDataSQL(sql);

        } catch (Exception ex) {
            logger.info("> getDBDataTableId " + ex);
        }
        return null;

    }
///////    
}
