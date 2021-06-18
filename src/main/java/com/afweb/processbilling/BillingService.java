/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processbilling;

import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;

import java.util.ArrayList;

import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class BillingService {

    protected static Logger logger = Logger.getLogger("BillingService");

        public ArrayList<BillingObj> getBillingByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(UserName, Password, accountid, length);
            return billingObjList;
        } catch (Exception e) {
        }
        return null;

    }

    public int removeBillingByCustomerAccountID(ServiceAFweb serviceAFWeb,String EmailUserName, String Password, String AccountIDSt, String BillIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            int billid = Integer.parseInt(BillIDSt);
            int ret = serviceAFWeb.getAccountImp().removeBillingByCustomerAccountID(UserName, Password, accountid, billid);
            return ret;
        } catch (Exception e) {
        }
        return 0;
    }
    
    
}
