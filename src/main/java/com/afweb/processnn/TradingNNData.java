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
    public int processSetDataRefName(ServiceAFweb serviceAFWeb) {
     return 1;   
    }
    
    private int saveDBneuralnetDataPro(ServiceAFweb serviceAFWeb, String tableName) {

        ArrayList<String> idList = getDBDataTableId(serviceAFWeb, tableName);
        int len = idList.size();
        ArrayList<String> writeArray = new ArrayList();
        logger.info("> saveDBneuralnetDataPro " + len);
//        int fileCont = 0;
//        int loopCnt = 0;
        if (len > 0) {
            for (int id = 0; id < len; id += 500) {
                String first = idList.get(id);
                if ((id + 500) < len) {
                    String last = idList.get(id - 1 + 500);
                    int ret = saveDBneuralnetData_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    ServiceAFweb.AFSleep();
                }
                if ((id + 500) >= len) {
                    String last = idList.get(len - 1);
                    int ret = saveDBneuralnetData_2(serviceAFWeb, tableName, first, last, writeArray);
                    if (ret == 0) {
                        return 0;
                    }
                    break;
                }
//                loopCnt++;
//                if (loopCnt > 10) {
//                    FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
//                    fileCont++;
//                    loopCnt = 0;
//                    writeArray.clear();
//                }
            }
//            FileUtil.FileWriteTextArray(ServiceAFweb.FileLocalPath + tableName + "_" + fileCont + ".txt", writeArray);
            return 1;
        }

        return 0;
    }

    private int saveDBneuralnetData_2(ServiceAFweb serviceAFWeb, String tableName, String first, String last, ArrayList<String> writeArray) {
        try {
            logger.info("> saveDBneuralnetDataPro - " + first + " " + last);

//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllNeuralNetData + "");
            String sql = "select * from " + tableName + " where id >= " + first + " and id <= " + last + " order by id asc";
            if (first.equals(last)) {
                sql = "select * from " + tableName + " where id = " + first;
            }
//            sqlObj.setReq(sql);
//
//            RequestObj sqlObjresp = serviceAFWeb.SysSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
            
            String output = serviceAFWeb.NnGetAllNeuralNetDataDBSQL(sql);
            if (output == null) {
                return 0;
            }
            ArrayList<AFneuralNetData> array = null;
            AFneuralNetData[] arrayItem = new ObjectMapper().readValue(output, AFneuralNetData[].class);
            List<AFneuralNetData> listItem = Arrays.<AFneuralNetData>asList(arrayItem);
            array = new ArrayList<AFneuralNetData>(listItem);

            for (int i = 0; i < array.size(); i++) {
                AFneuralNetData obj = array.get(i);
                String st = new ObjectMapper().writeValueAsString(obj);
                writeArray.add(st);
            }
            return 1;

        } catch (Exception ex) {
            logger.info("> saveDBneuralnetDataPro " + ex);
        }
        return 0;
    }
//

    private ArrayList<String> getDBDataTableId(ServiceAFweb serviceAFWeb, String table) {
        try {
//            RequestObj sqlObj = new RequestObj();
//            sqlObj.setCmd(ServiceAFweb.AllIdInfo + "");
            String sql = "select id from " + table + " order by id asc";
            return serviceAFWeb.NnGetAllIdNNetDataSQL(sql);

//            sqlObj.setReq(sql);
//            RequestObj sqlObjresp = serviceAFWeb.SystemSQLRequest(sqlObj);
//            String output = sqlObjresp.getResp();
//            ArrayList<String> array = null;
//
//            String[] arrayItem = new ObjectMapper().readValue(output, String[].class);
//            List<String> listItem = Arrays.<String>asList(arrayItem);
//            array = new ArrayList<String>(listItem);
//            return array;
        } catch (Exception ex) {
            logger.info("> getDBDataTableId " + ex);
        }
        return null;

    }    
///////    
}
