/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processcustacc;


import com.afweb.model.*;
import com.afweb.model.account.*;


import com.afweb.service.ServiceAFweb;
import com.afweb.util.TimeConvertion;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class CommService {

    protected static Logger logger = Logger.getLogger("CommService");

    public int addCommByCustAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String data) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return serviceAFWeb.getAccountImp().addCommByCustomerAccountID(UserName, Password, accountid, data);
        } catch (Exception e) {
        }
        return 0;
    }

    public ArrayList<CommObj> getCommByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return serviceAFWeb.getAccountImp().getCommSignalSplitByCustomerAccountID(UserName, Password, accountid, length);
        } catch (Exception e) {
        }
        return null;

    }

    public int removeCommByID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String IDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            int id = Integer.parseInt(IDSt);
            return serviceAFWeb.getAccountImp().removeAccountCommByID(UserName, Password, accountid, id);
        } catch (Exception e) {
        }
        return 0;
    }

    public int removeAllCommByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return serviceAFWeb.getAccountImp().removeCommByCustomerAccountIDType(UserName, Password, accountid, ConstantKey.INT_TYPE_COM_SIGNAL);
        } catch (Exception e) {
        }
        return 0;
    }

    public int removeAllCommBy1Month(ServiceAFweb serviceAFWeb) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long last1monthbefore = TimeConvertion.addMonths(dateNow.getTimeInMillis(), -1); // last 1 month before

        serviceAFWeb.getAccountImp().removeCommByTimebefore(last1monthbefore, ConstantKey.INT_TYPE_COM_SIGNAL);
        return 1;

    }
    
}
