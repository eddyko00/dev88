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

    public static Logger logger = Logger.getLogger("NNProcess");

    public int updateNNdataDB(ServiceAFweb serviceAFWeb, String nnName, HashMap<String, ArrayList> stockInputMap) {
        try {
            int added = 0;
            int total = 0;
            ArrayList<NNInputDataObj> inputlistSym = new ArrayList();
            for (String sym : stockInputMap.keySet()) {
                inputlistSym = stockInputMap.get(sym);
                if (inputlistSym == null) {
                    continue;
                }
                total += inputlistSym.size();
                String BPnameSym = CKey.NN_version + "_" + nnName;
                for (int i = 0; i < inputlistSym.size(); i++) {
                    NNInputDataObj objData = inputlistSym.get(i);
                    ArrayList<AFneuralNetData> objList = serviceAFWeb.getStockImp().getNeuralNetDataObj(BPnameSym, 0, objData.getUpdatedatel());
                    if ((objList == null) || (objList.size() == 0)) {
                        serviceAFWeb.getStockImp().updateNeuralNetDataObject(BPnameSym, 0, objData);
                        added++;
                    }
                }
            }
            logger.info("> updateNNdataDB - added " + added + " of " + total);
            return 1;
        } catch (Exception ex) {
            logger.info("> updateNNdataDB - exception " + ex);
        }
        return 0;
    }
}
