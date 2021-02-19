/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.mail;

import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koed
 */
public class EmailProcess {

    protected static Logger logger = Logger.getLogger("EmailProcess");

    private static ArrayList accountFundIdNameArray = new ArrayList();

    public void ProcessEmailAccount(ServiceAFweb serviceAFWeb) {

//        logger.info("> UpdateAccountSignal ");
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        if (accountAdminObj == null) {
            return;
        }
        if (accountFundIdNameArray == null) {
            accountFundIdNameArray = new ArrayList();
        }
        if (accountFundIdNameArray.size() == 0) {
            ArrayList accountIdList = serviceAFWeb.SystemAllOpenAccountIDList();
            if (accountIdList == null) {
                return;
            }
            accountFundIdNameArray = accountIdList;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();
        String LockName = "ALL_EMAIL";
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
                if (accountFundIdNameArray.size() == 0) {
                    break;
                }
                try {
                    String accountIdSt = (String) accountFundIdNameArray.get(0);

                    int accountId = Integer.parseInt(accountIdSt);
                    AccountObj accountObj = serviceAFWeb.getAccountImp().getAccountObjByAccountID(accountId);
                    if (accountObj != null) {
                        if (accountObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                            int ret = EmailTradingAccount(serviceAFWeb, accountObj);
                            if (ret == 2) {
                                break; // try again next time 
                            }
                            if (ret == 1) {
                                break; // only allow send 1 at a time
                            }
                            if (ret == 0) {
                                accountFundIdNameArray.remove(0);
                            }
                        } else {
                            accountFundIdNameArray.remove(0);
                        }
                    }
                } catch (Exception e) {
                    logger.info("> ProcessEmailAccount Exception " + e.getMessage());
                }
            }
            serviceAFWeb.removeNameLock(LockName, ConstantKey.FUND_LOCKTYPE);
        }
    }

    public int EmailTradingAccount(ServiceAFweb serviceAFWeb, AccountObj accObj) {
        CustomerObj cust = serviceAFWeb.getAccountImp().getCustomerByAccount(accObj);
        String emailAddr = cust.getEmail();
        if ((emailAddr != null) && (emailAddr.length() > 0)) {
            if (validate(emailAddr) == false) {
                emailAddr = "";
            }
        } else {
            emailAddr = "";
        }

        if (cust.getUsername().equals(CKey.G_USERNAME)) {
            emailAddr = ServiceAFweb.UU_Str;
        }

        if (accObj.getType() == AccountObj.INT_TRADING_ACCOUNT) {
            ArrayList<CommObj> commList = serviceAFWeb.getAccountImp().getComObjByType(accObj.getId(),
                    ConstantKey.INT_TYPE_COM_EMAIL, 0);
            if (commList != null) {
                if (commList.size() > 0) {
                    for (int i = 0; i < commList.size(); i++) {
                        CommObj comObj = commList.get(i);
                        try {
                            if (ServiceAFweb.processEmailFlag == true) {
                                if ((emailAddr != null) && (emailAddr.length() > 0)) {
                                    GmailSender sender = new GmailSender();
                                    sender.setSender(ServiceAFweb.UA_Str, ServiceAFweb.PA_Str);
                                    sender.addRecipient(emailAddr);
                                    sender.setSubject("IISWeb - "+comObj.getName());
                                    sender.setBody(comObj.getData());
                                    sender.send();
                                    logger.info("> EmailTradingAccount id " + accObj.getId() + " " + emailAddr + " size " + commList.size());
                                }
                                // remove comObj;
                                serviceAFWeb.getAccountImp().removeCommByCommID(comObj.getId());
                                return 1; // successful
                            }
                            return 1;
                        } catch (Exception ex) {
                            logger.info("> EmailTradingAccount Exception ...." + ex.getMessage());
                        }
                        //update error count
                        int subSt = comObj.getSubstatus();
                        if (subSt > 4) {
                            serviceAFWeb.getAccountImp().removeCommByCommID(comObj.getId());
                            return 2; // error                            
                        }
                        comObj.setSubstatus(subSt + 1);
                        serviceAFWeb.getAccountImp().updateAccountCommSubStatusById(comObj);
                        return 2;
                    }
                }
            }
        }
        return 0;
    }

    private Pattern pattern;
    private Matcher matcher;

    private static final String EMAIL_PATTERN
            = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public EmailProcess() {
        pattern = Pattern.compile(EMAIL_PATTERN);
    }

    /**
     * Validate hex with regular expression
     *
     * @param hex hex for validation
     * @return true valid hex, false invalid hex
     */
    public boolean validate(final String hex) {

        matcher = pattern.matcher(hex);
        return matcher.matches();

    }
/////////////////////////
}
