/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processemail;

import com.afweb.account.AccountImp;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.util.CKey;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class EmailService {

    protected static Logger logger = Logger.getLogger("EmailService");
    AccountImp accountImp = new AccountImp();

    public ArrayList<CommObj> getCommEmaiByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, int length) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return accountImp.getCommEmailByCustomerAccountID(UserName, Password, accountid, length);
        } catch (Exception e) {
        }
        return null;

    }

    public int removeAllEmailByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            if (UserName.equals(CKey.ADMIN_USERNAME)) {
                int accountid = Integer.parseInt(AccountIDSt);
                return accountImp.removeAllCommByType(ConstantKey.INT_TYPE_COM_EMAIL);
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public int removeAllEmailByID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt, String IDSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            int id = Integer.parseInt(IDSt);
            return accountImp.removeAccountCommByID(UserName, Password, accountid, id);
        } catch (Exception e) {
        }
        return 0;
    }

}
