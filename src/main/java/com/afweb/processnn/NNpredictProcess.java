/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.processnn.model.ProcessNN3;
import com.afweb.processnn.model.ProcessNN2;
import com.afweb.processnn.model.ProcessNN1;
import com.afweb.processnn.model.ProcessNN00;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;
import com.afweb.model.stock.*;


import com.afweb.service.ServiceAFweb;
import com.afweb.signal.NNObj;

import java.util.ArrayList;

import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class NNpredictProcess {

    protected static Logger logger = Logger.getLogger("NNCal");

    public static NNObj NNpredict(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {

        NNObj nn = new NNObj();

        switch (TR_Name) {
            case ConstantKey.INT_TR_NN1:
                return ProcessNN1.NNpredictNN1(serviceAFWeb, accountObj, stock,  StockRecArray, DataOffset);
            case ConstantKey.INT_TR_NN91: // mirror to NN1
                return ProcessNN1.NNpredictNN1(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);

            case ConstantKey.INT_TR_NN2:
                return ProcessNN2.NNpredictNN2(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);
            case ConstantKey.INT_TR_NN92: // mirror to NN2
                return ProcessNN2.NNpredictNN2(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);

            case ConstantKey.INT_TR_NN3:
                return ProcessNN3.NNpredictNN3(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);
            case ConstantKey.INT_TR_NN93: // mirror to NN3
                return ProcessNN3.NNpredictNN3(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);

////////////////////////////////////////////////////                
            case ConstantKey.INT_TR_NN30:
                return ProcessNN00.NNpredictNN30(serviceAFWeb, accountObj, stock, StockRecArray, DataOffset);

            default:
                break;
        }
        return nn;
    }

    ///////
}
