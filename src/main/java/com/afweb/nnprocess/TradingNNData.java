/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnprocess;

import com.afweb.model.stock.AFneuralNetData;

import com.afweb.nn.*;
import com.afweb.service.*;
import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
        String BPnameSym = CKey.NN_version + "_" + nnName;
        try {            
            ArrayList<NNInputDataObj> inputlist = new ArrayList();
            
            ArrayList<AFneuralNetData> objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym);
            if (objDataList.size() > 1000) {
                // already saved
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

                for (int i = 0; i < inputlistSym.size(); i++) {
                    NNInputDataObj objData = inputlistSym.get(i);
                    serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
                    added++;
//                    ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
//                    if ((objList == null) || (objList.size() == 0)) {
//                        serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
//                        added++;
//                    }
                }
            }
            logger.info("> saveNNdataDB - " + BPnameSym + " added " + added + " of " + total);
            return 1;
        } catch (Exception ex) {
            logger.info("> saveNNdataDB - exception - " + BPnameSym + " " + ex);
        }
        return 0;
    }

    public int getNNBaseDataDB(ServiceAFweb serviceAFWeb, String nnName, ArrayList<NNInputDataObj> inputlist) {
        ArrayList<AFneuralNetData> objDataList = new ArrayList();
        String BPnameSym = CKey.NN_version + "_" + nnName;
        try {
            objDataList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym);
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

////////
}
