/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processnn;

import com.afweb.model.*;

import com.afweb.model.stock.AFneuralNet;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class NNService {

    protected static Logger logger = Logger.getLogger("EmailService");

    public String SystemClearNNinput(ServiceAFweb serviceAFWeb) {
        TradingNNprocess NNProcessImp = new TradingNNprocess();
        int retSatus = 0;

        retSatus = NNProcessImp.ClearStockNN_inputNameArray(serviceAFWeb, ConstantKey.TR_NN1);
//            retSatus = NNProcessImp.ClearStockNNinputNameArray(this, ConstantKey.TR_NN2);

        return "" + retSatus;
    }

    public boolean SystemDeleteNN1Table(ServiceAFweb serviceAFWeb) {
        logger.info(">SystemDeleteNN1Table start ");
        serviceAFWeb.getStockImp().deleteNeuralNet1Table();
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
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
//        return getStockImp().releaseNeuralNetObj(name);
        return serviceAFWeb.getStockImp().releaseNeuralNetBPObj(name);
    }

    public AFneuralNet getNeuralNetObjWeight0(ServiceAFweb serviceAFWeb, String name, int type) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return serviceAFWeb.getStockImp().getNeuralNetObjWeight0(name);
    }

    public AFneuralNet getNeuralNetObjWeight1(ServiceAFweb serviceAFWeb, String name, int type) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        return serviceAFWeb.getStockImp().getNeuralNetObjWeight1(name);
    }

    public int setNeuralNetObjWeight0(ServiceAFweb serviceAFWeb, AFneuralNet nn) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty

        int ret = serviceAFWeb.getStockImp().setCreateNeuralNetObjRef0(nn.getName(), nn.getWeight(), nn.getRefname());
        return ret;
    }

    public int setNeuralNetObjWeight1(ServiceAFweb serviceAFWeb, AFneuralNet nn) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        // assume only 1 of the weight is set and the other are empty
        // assume only 1 of the weight is set and the other are empty
        return serviceAFWeb.getStockImp().setCreateNeuralNetObj1(nn.getName(), nn.getWeight());
    }

}
