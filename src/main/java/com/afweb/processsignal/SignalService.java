/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsignal;

import com.afweb.processnn.NNpredictProcess;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.signal.NNObj;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class SignalService {
    
    protected static Logger logger = Logger.getLogger("SignalService");
    TradingSignalProcess TRprocessImp = new TradingSignalProcess();
    
    public NNObj NNpredict(ServiceAFweb serviceAFWeb, int TR_Name, AccountObj accountObj, AFstockObj stock, ArrayList<AFstockInfo> StockRecArray, int DataOffset) {
        return NNpredictProcess.NNpredict(serviceAFWeb, TR_Name, accountObj, stock, StockRecArray, DataOffset);
    }
    
    public ArrayList<StockTRHistoryObj> ProcessTRHistory(ServiceAFweb serviceAFWeb, TradingRuleObj trObj, int lengthYr, int month) {
        return TRprocessImp.ProcessTRHistory(serviceAFWeb, trObj, lengthYr, month);
    }
    
    public ArrayList<PerformanceObj> ProcessTranPerfHistory(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length, boolean buyOnly) {
        return TRprocessImp.ProcessTranPerfHistory(serviceAFWeb, tranOrderList, stock, length, buyOnly);
    }
    
    public ArrayList<PerformanceObj> ProcessTranPerfHistoryReinvest(ServiceAFweb serviceAFWeb, ArrayList<TransationOrderObj> tranOrderList, AFstockObj stock, int length, boolean buyOnly) {
        return TRprocessImp.ProcessTranPerfHistoryReinvest(serviceAFWeb, tranOrderList, stock, length, buyOnly);
    }
    
    public void updateAdminTradingsignal(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        TRprocessImp.updateAdminTradingsignal(serviceAFWeb, accountObj, symbol);
    }
    
    public void upateAdminTransaction(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        TRprocessImp.upateAdminTransaction(serviceAFWeb, accountObj, symbol);
    }

    public void upateAdminPerformance(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        TRprocessImp.upateAdminPerformance(serviceAFWeb, accountObj, symbol);
    }

    public void upateAdminTRPerf(ServiceAFweb serviceAFWeb, AccountObj accountObj, String symbol) {
        TRprocessImp.upateAdminTRPerf(serviceAFWeb, accountObj, symbol);
    }    
    
    
}
