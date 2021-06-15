/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.CKey;

import com.afweb.util.TimeConvertion;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class PUBSUBprocess {

    protected static Logger logger = Logger.getLogger("PUBSUBprocess");

    private static ArrayList<CommObj> accountCommArray = new ArrayList();

    public void ProcessPUBSUBAccount(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessPUBSUBAccount";
        logger.info("> ProcessPUBSUBAccount ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        if (accountCommArray == null) {
            accountCommArray = new ArrayList();
        }
        if (accountCommArray.size() == 0) {
            ArrayList<CommObj> comObjList = serviceAFWeb.getAccountImp().getCommPubByCustomer(CKey.ADMIN_USERNAME, null, 50);
            if (comObjList == null) {
                return;
            }
            accountCommArray = comObjList;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ALL_PUBSUB";
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.FUND_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessAllAccountTradingSignal");
        if (CKey.NN_DEBUG == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {

            long currentTime = System.currentTimeMillis();
            long lockDate2Min = TimeConvertion.addMinutes(currentTime, 2);

            for (int k = 0; k < 10; k++) {
                currentTime = System.currentTimeMillis();
                if (lockDate2Min < currentTime) {
                    break;
                }
                if (accountCommArray.size() == 0) {
                    break;
                }
                try {
                    CommObj comObj = accountCommArray.get(0);
                    if (comObj == null) {
                        continue;
                    }
                    int accountId = comObj.getAccountid();
                    AccountObj accountObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accountId);
                    if (accountObj != null) {
                        if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                            int ret = SendPUBSUBTradingAccount(serviceAFWeb, accountObj, comObj);
                        }
                    }
                    serviceAFWeb.getAccountImp().removeCommByCommID(comObj.getId());
                } catch (Exception e) {
                    logger.info("> ProcessPUBSUBAccount Exception " + e.getMessage());
                }

                accountCommArray.remove(0);
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int SendPUBSUBTradingAccount(ServiceAFweb serviceAFWeb, AccountObj accFundObj, CommObj comObj) {
        ServiceAFweb.lastfun = "SendPUBSUBTradingAccount";
        String fundName = "#fund" + accFundObj.getId() + "#";
        ArrayList<CustomerObj> custObjList = serviceAFWeb.getAccountImp().getCustomerFundPortfolio(fundName, 0);
        if (custObjList == null) {
            return 0;
        }
        for (int i = 0; i < custObjList.size(); i++) {
            CustomerObj custObj = custObjList.get(i);
            ArrayList<AccountObj> accObjList = serviceAFWeb.getAccountImp().getAccountListByCustomerId(custObj.getId());
            if (accObjList == null) {
                continue;
            }
            for (int j = 0; j < accObjList.size(); j++) {
                AccountObj accountObj = accObjList.get(j);
                if (accountObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                    CommMsgImp commMsg = new CommMsgImp();
                    commMsg.AddCommMessage(serviceAFWeb, accountObj, ConstantKey.COM_FUNDMSG, comObj.getData());
                    break;
                }
            }
        }
        return 1;
    }

}
