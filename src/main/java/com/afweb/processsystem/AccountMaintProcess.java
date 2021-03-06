/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processsystem;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;

import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.dbsys.SysDB;
import com.afweb.util.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class AccountMaintProcess {

    protected static Logger logger = Logger.getLogger("AccountMaintProcess");

    private static int acTimerCnt = 0;

//    private static ArrayList accountIdNameArray = new ArrayList();
//    private static ArrayList accountFundIdNameArray = new ArrayList();
    ///
//    public void InitSystemData() {
//        acTimerCnt = 0;
//        accountIdNameArray = new ArrayList();
//    }
    public void ProcessSystemMaintance(ServiceAFweb serviceAFWeb) {
        acTimerCnt++;
        if (acTimerCnt < 0) {
            acTimerCnt = 0;
        }
//        logger.info(">>>>>>>>>>>>>> ProcessSystemMaintance " + acTimerCnt);
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ACC_" + CKey.AF_SYSTEM;
        long lockReturn = serviceAFWeb.SysSetLockName(LockName, ConstantKey.ACC_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessSystemMaintance");
        if (lockReturn > 0) {

            if ((acTimerCnt % 2) == 0) {
                // disable customer will be handle by billing process
                // disable cusotmer with no activity in 2 days
//                ProcessCustomerDisableMaintance(serviceAFWeb);

                // reomve customer with no activity in 4 days  
                ProcessCustomerRemoveMaintance(serviceAFWeb);
                // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
                ProcessAdminAddRemoveStock(serviceAFWeb);
                //delete stock if disable
                ProcessStockInfodeleteMaintance(serviceAFWeb);
            } else if ((acTimerCnt % 3) == 0) {
                ;
            } else {
                // delete stock based on all customer account exclude the ADMIN_USERNAME account 
                ProcessStockkMaintance(serviceAFWeb);

                // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
                ProcessAdminAddRemoveStock(serviceAFWeb);

                // cleanup Lock entry pass 30 min
                ProcessAllLockCleanup(serviceAFWeb);
                // cleanup Lock entry pass 30 min

            }
        }
        serviceAFWeb.SysLockRemoveLockName(LockName, ConstantKey.ACC_LOCKTYPE);

    }
    //////////////////////////////////////////////

    public void ProcessStockInfodeleteMaintance(ServiceAFweb serviceAFWeb) {
//        logger.info(">>>>>>>>>>>>>> ProcessStockInfodeleteMaintance ");
        ArrayList stockRemoveList = serviceAFWeb.SysGetRemoveStockNameList(20);  // status = ConstantKey.COMPLETED      
        //delete stock if disable
        if (stockRemoveList != null) {
            if (stockRemoveList.size() > 0) {
                int numCnt = 0;
                logger.info("> ProcessStockInfodeleteMaintance stockRemoveList " + stockRemoveList.size());
                try {
                    for (int i = 0; i < stockRemoveList.size(); i++) {
                        String symbol = (String) stockRemoveList.get(i);
                        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(symbol);
                        // check transaction
                        boolean hasTran = serviceAFWeb.AccCheckTRListByStockID(stock.getId() + "");
                        if (hasTran == false) {
                            serviceAFWeb.StoDeleteStockServ(stock);
                            logger.info("> stockN Delete  " + stock.getSymbol());
                        }
                        numCnt++;
                        if (numCnt > 10) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }

        ArrayList stockNDisableList = serviceAFWeb.SysGetDisableStockNameList(20);
        // delete stock info if disable
        if (stockNDisableList != null) {
            if (stockNDisableList.size() > 0) {
                int numCnt = 0;
                logger.info("> ProcessStockInfodeleteMaintance stockNDisableList " + stockNDisableList.size());

                try {

                    for (int i = 0; i < stockNDisableList.size(); i++) {
                        String symbol = (String) stockNDisableList.get(i);
                        /// do not call this because multiple web use the same info. 
                        /// delete by manual only
//                        serviceAFWeb.InfDeleteStockInfoBySym(symbol);

                        AFstockObj stock = serviceAFWeb.StoGetStockObjBySym(symbol);
                        stock.setStatus(ConstantKey.COMPLETED);
                        //send SQL update
                        String sockUpdateSQL = SysDB.SQLupdateStockStatus(stock);
                        ArrayList sqlList = new ArrayList();
                        sqlList.add(sockUpdateSQL);
                        serviceAFWeb.StoUpdateSQLArrayList(sqlList);
                        logger.info("> stockN Disable  " + stock.getSymbol());

                        numCnt++;
                        if (numCnt > 10) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                }

            }
        }

    }

    public void ProcessCustomerRemoveMaintance(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessCustomerRemoveMaintance";
//        logger.info(">>>>>>>>>>>>>> ProcessCustomerRemoveMaintance " + acTimerCnt);
        // reomve customer with no activity in 4 days        
        ArrayList custList = serviceAFWeb.AccGetExpiredCustomerList(0);
        if (custList == null) {
            return;
        }
        int numCnt = 0;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        long cust15DayAgo = TimeConvertion.addDays(dateNowLong, -15); // 15 day ago and no update             
        for (int i = 0; i < custList.size(); i++) {
            CustomerObj custObj = (CustomerObj) custList.get(i);

            // should be disable by ProcessCustomerDisableMaintance after 2 day in activity
            if (custObj.getStatus() != ConstantKey.DISABLE) {
                continue;
            }
            if (custObj.getUpdatedatel() < cust15DayAgo) {

                //remove customer
                serviceAFWeb.AccRemoveCustomer(custObj.getUsername());

                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
                java.sql.Date d = new java.sql.Date(dateNowLong);
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTdate = format.format(d);

                String msg = ESTdate + " " + custObj.getUsername() + " Customer removed - 15 day after expired.";
                if (custObj.getUsername().equals(CKey.E_USERNAME)) {
                    ;
                } else {
                }
                numCnt++;
                if (numCnt > 10) {
                    break;
                }

            }

        }
    }

////////////////////
    private void ProcessAllLockCleanup(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessAllLockCleanup";

        logger.info(">>>>> ProcessAllLockCleanup " + acTimerCnt);
        // clean up old lock name
        // clean Lock entry pass 30 min
        ArrayList<AFLockObject> lockArray = serviceAFWeb.SysLockGetAll();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        int numCnt = 0;
        if (lockArray != null) {
            for (int i = 0; i < lockArray.size(); i++) {
                AFLockObject lockObj = lockArray.get(i);
                long lastUpdate = lockObj.getLockdatel();
                long lastUpdateAdd30 = TimeConvertion.addMinutes(lastUpdate, 30); // remove lock for 30min

                if (lockObj.getType() == ConstantKey.ADMIN_SIGNAL_LOCKTYPE) {
                    // TR Best takes a long time
                    lastUpdateAdd30 = TimeConvertion.addMinutes(lastUpdate, SysDB.MaxMinuteAdminSignalTrading); // remove lock for 90min
                }
                if (lastUpdateAdd30 < dateNow.getTimeInMillis()) {
                    serviceAFWeb.SysLockRemoveLockName(lockObj.getLockname(), lockObj.getType());
                    numCnt++;
                    if (numCnt > 10) {
                        break;
                    }
                }
            }
        }
    }

    private void ProcessStockkMaintance(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessStockkMaintance";

        // delete stock based on all customer account exclude the ADMIN_USERNAME account 
        // do Simulation trading
//        logger.info(">>>>>>>>>>>>>> ProcessStockkMaintance " + acTimerCnt);
        AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        ArrayList StockNameList = serviceAFWeb.StoGetAllOpenStockNameServ();

        if (StockNameList == null) {
            return;
        }
        ArrayList AllAccountStockNameList = serviceAFWeb.AccGetAllAccountStockNameListExceptAdmin(accountAdminObj.getId());

        if (AllAccountStockNameList == null) {
            return;
        }
        ArrayList addedList = new ArrayList();
        ArrayList removeList = new ArrayList();
        int numCnt = 0;
        boolean result = compareStockList(AllAccountStockNameList, StockNameList, addedList, removeList);
        if (result == true) {
            //addedList should be 0
            for (int i = 0; i < removeList.size(); i++) {
                String NormalizeSymbol = (String) removeList.get(i);
                logger.info("> ProcessStockkMaintance remove stock " + NormalizeSymbol);
                serviceAFWeb.StoDisableStockServ(NormalizeSymbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }
            }
        }
    }

    public void ProcessAdminAddRemoveStock(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessAdminAddRemoveStock";

        // add or remove stock in ADMIN_USERNAME account based on all stocks in the system
//        logger.info("> ProcessAdminAccount ......... ");
        AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }

        ArrayList StockNameList = serviceAFWeb.StoGetAllOpenStockNameServ();

        if (StockNameList == null) {
            return;
        }

        ArrayList AccountStockNameList = serviceAFWeb.AccGetAccountStockNameListServ(accountAdminObj.getId());
        if (AccountStockNameList == null) {
            return;
        }

        ServiceAFweb.getServerObj().setTotalStock(StockNameList.size());
        ServiceAFweb.getServerObj().setTotalStockAcc(AccountStockNameList.size());

        int numCnt = 0;
        ArrayList addedList = new ArrayList();
        ArrayList removeList = new ArrayList();
        boolean result = compareStockList(StockNameList, AccountStockNameList, addedList, removeList);
        if (result == true) {
            for (int i = 0; i < addedList.size(); i++) {
                String symbol = (String) addedList.get(i);
                int resultAdd = serviceAFWeb.AccAddAccountStockByCustAccServ(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
                logger.info("> AdminAddRemoveStock add TR stock " + symbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }
                ServiceAFweb.AFSleep();

            }
            /////////
            for (int i = 0; i < removeList.size(); i++) {
                String symbol = (String) removeList.get(i);
                int resultRemove = serviceAFWeb.AccRemoveAccountStockByUserNameAccId(CKey.ADMIN_USERNAME, null, accountAdminObj.getId() + "", symbol);
                logger.info("> AdminAddRemoveStock remove TR stock " + symbol);
                numCnt++;
                if (numCnt > 10) {
                    break;
                }

                ServiceAFweb.AFSleep();

            }
        }
    }

    public static boolean compareStockList(ArrayList<String> masterList, ArrayList<String> current, ArrayList<String> addedList, ArrayList<String> removeList) {
        try {
            for (String a : masterList) {
                if (!current.contains(a)) {
                    addedList.add(a);
                }
            }
            for (String a : current) {
                if (!masterList.contains(a)) {
                    removeList.add(a);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
