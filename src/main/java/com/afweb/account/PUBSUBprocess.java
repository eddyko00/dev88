/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.AccountObj;
import com.afweb.model.account.CommObj;
import com.afweb.model.account.CustomerObj;
import com.afweb.service.ServiceAFweb;
import com.afweb.util.CKey;
import com.afweb.util.StringTag;
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

    private static ArrayList accountdNameArray = new ArrayList();

    public void ProcessPUBSUBAccount(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "ProcessPUBSUBAccount";
        logger.info("> ProcessPUBSUBAccount ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        if (accountdNameArray == null) {
            accountdNameArray = new ArrayList();
        }
        if (accountdNameArray.size() == 0) {
            ArrayList accountIdList = serviceAFWeb.SystemAllOpenAccountIDList();
            if (accountIdList == null) {
                return;
            }
            accountdNameArray = accountIdList;
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
                if (accountdNameArray.size() == 0) {
                    break;
                }
                try {
                    String accountIdSt = (String) accountdNameArray.get(0);

                    int accountId = Integer.parseInt(accountIdSt);
                    AccountObj accountObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accountId);
                    if (accountObj != null) {
                        if (accountObj.getType() == AccountObj.INT_MUTUAL_FUND_ACCOUNT) {
                            if (accountObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                                int ret = SendPUBSUBTradingAccount(serviceAFWeb, accountObj);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.info("> ProcessPUBSUBAccount Exception " + e.getMessage());
                }
                accountdNameArray.remove(0);
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int SendPUBSUBTradingAccount(ServiceAFweb serviceAFWeb, AccountObj accObj) {
        ServiceAFweb.lastfun = "SendPUBSUBTradingAccount";
        return 0;
    }

}
