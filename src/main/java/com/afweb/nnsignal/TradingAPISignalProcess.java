/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.nnsignal;

import com.afweb.account.*;
import com.afweb.model.*;

import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.service.ServiceAFweb;

import com.afweb.util.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class TradingAPISignalProcess {

    protected static Logger logger = Logger.getLogger("TradingAPISignalProcess");

    private static ArrayList<String> stockSignalNameArray = new ArrayList();

    public void InitSystemData() {
        stockSignalNameArray = new ArrayList();
    }

    private ArrayList UpdateStockSignalNameArray(ServiceAFweb serviceAFWeb) {
        if (stockSignalNameArray != null && stockSignalNameArray.size() > 0) {
            return stockSignalNameArray;
        }
        ArrayList<CustomerObj> custList = serviceAFWeb.getCustomerByType(CustomerObj.INT_API_USER);
        if (custList == null) {
            return stockSignalNameArray;
        }

        for (int i = 0; i < custList.size(); i++) {
            CustomerObj cust = custList.get(i);
            AccountObj accountObj = serviceAFWeb.getAccountByType(cust.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);

            ArrayList stockNameArray = serviceAFWeb.SystemAccountStockNameList(accountObj.getId());
            ArrayList stockNameAccIdArray = new ArrayList();
            if (stockNameArray != null) {
                for (int j = 0; j < stockNameArray.size(); j++) {
                    String sym = (String) stockNameArray.get(j);
                    sym = accountObj.getId() + "#" + sym;
                    stockNameAccIdArray.add(sym);
                }
                stockSignalNameArray.addAll(stockNameAccIdArray);
            }
        }
        //make it random for multiple API users
        Collections.shuffle(stockSignalNameArray);
        return stockSignalNameArray;
    }

    public void ProcessAPISignalTrading(ServiceAFweb serviceAFWeb) {
//        logger.info("> ProcessAPISignalTrading ");
//        this.serviceAFWeb = serviceAFWeb;
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        UpdateStockSignalNameArray(serviceAFWeb);
        if (stockSignalNameArray == null) {
            return;
        }
        if (stockSignalNameArray.size() == 0) {
            return;
        }

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "API_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");

        long lockReturn = 1;
        lockReturn = serviceAFWeb.setLockNameServ(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");

        boolean testing = false;
        if (testing == true) {
            lockReturn = 1;
        }
//        logger.info("ProcessAPISignalTrading " + LockName + " LockName " + lockReturn);
        if (lockReturn > 0) {

            long currentTime = System.currentTimeMillis();
            long lockDate5Min = TimeConvertion.addMinutes(currentTime, 5);
            logger.info("ProcessAPISignalTrading for 5 minutes stocksize=" + stockSignalNameArray.size());

            for (int i = 0; i < 30; i++) {
                currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
                    break;
                }
                if (stockSignalNameArray.size() == 0) {
                    break;
                }

                try {
                    String accIdSymbol = (String) stockSignalNameArray.get(0);
                    stockSignalNameArray.remove(0);
                    String[] accIdSymList = accIdSymbol.split("#");

                    String accIdSt = accIdSymList[0];
                    String symbol = accIdSymList[1];

                    int accId = Integer.parseInt(accIdSt);
                    AccountObj accountObj = serviceAFWeb.getAccountByAccountID(accId);

                    AFstockObj stock = serviceAFWeb.getStockBySymServ(symbol);
                    if (stock != null) {
                        if (stock.getSubstatus() == ConstantKey.STOCK_SPLIT) {
                            logger.info("> ProcessAPISignalTrading return stock split " + symbol);
                            continue;
                        }
                    }
                    if (stock == null) {
                        logger.info("> ProcessAPISignalTrading return stock null ");
                        continue;
                    }

                    String LockStock = "API_" + symbol;
                    LockStock = LockStock.toUpperCase();

                    long lockDateValueStock = TimeConvertion.getCurrentCalendar().getTimeInMillis();
                    long lockReturnStock = serviceAFWeb.setLockNameServ(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE, lockDateValueStock, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAdminSignalTrading");
                    if (testing == true) {
                        lockReturnStock = 1;
                    }
//                    logger.info("ProcessAdminSignalTrading " + LockStock + " LockStock " + lockReturnStock);
                    if (lockReturnStock > 0) {

                        boolean ret = this.checkStock(serviceAFWeb, symbol);
                        if (ret == true) {

                            TradingRuleObj trObj = serviceAFWeb.SystemAccountStockIDByTRname(accountAdminObj.getId(), stock.getId(), ConstantKey.TR_NN1);
                            if (trObj != null) {
                                AccountTranImp accountTran = new AccountTranImp();
                                accountTran.updateTradingsignal(serviceAFWeb, accountAdminObj, accountObj, symbol);
                                accountTran.updateTradingTransaction(serviceAFWeb, accountObj, symbol);

                            }
                        }

                        serviceAFWeb.removeNameLock(LockStock, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
//                        logger.info("ProcessAPISignalTrading " + LockStock + " unLock LockStock ");
                    }
                } catch (Exception ex) {
                    logger.info("> ProcessAPISignalTrading Exception" + ex.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.ADMIN_SIGNAL_LOCKTYPE);
//            logger.info("ProcessAPISignalTrading " + LockName + " unlock LockName");
        }
    }

    public boolean checkStock(ServiceAFweb serviceAFWeb, String NormalizeSymbol) {
        AFstockObj stock = serviceAFWeb.getStockBySymServ(NormalizeSymbol);
        if (stock == null) {
            return false;
        }
        if (stock.getStatus() != ConstantKey.OPEN) {
            return false;
        }
        if (stock.getAfstockInfo() == null) {
            return false;
        }
        return true;
    }

    /////////////
}
