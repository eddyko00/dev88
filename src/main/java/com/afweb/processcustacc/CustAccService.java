/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processcustacc;

import com.afweb.account.CommMsgImp;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class CustAccService {

    protected static Logger logger = Logger.getLogger("CustAccService");

    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj addCustomerPassword(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String FirstName, String LastName, String planSt) {
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        loginObj.setWebMsg(webStatus);
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        boolean validEmail = NameObj.isEmailValid(EmailUserName);
        if (validEmail == true) {
            CustomerObj newCustomer = new CustomerObj();
            newCustomer.setUsername(UserName);
            newCustomer.setPassword(Password);
            newCustomer.setType(CustomerObj.INT_CLIENT_BASIC_USER);
            newCustomer.setEmail(EmailUserName);
            newCustomer.setFirstname(FirstName);
            newCustomer.setLastname(LastName);
            int plan = 0;
            plan = Integer.parseInt(planSt);
            if (plan == ConstantKey.INT_PP_BASIC) {
                ;
            } else if (plan == ConstantKey.INT_PP_STANDARD) {
                ;
            } else if (plan == ConstantKey.INT_PP_PEMIUM) {
                ;
            }

            // result 1 = success, 2 = existed,  0 = fail
            int result = serviceAFWeb.getAccountImp().addCustomer(newCustomer, plan);
            if (result == 1) {
                CustomerObj custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
                if (custObj != null) {
                    // set pending for new customer
                    if (custObj.getStatus() == ConstantKey.OPEN) {
                        custObj.setStatus(ConstantKey.PENDING);
                        serviceAFWeb.getAccountImp().updateCustStatusSubStatus(custObj, custObj.getStatus(), custObj.getSubstatus());
                    }
                }
            }
//
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            Calendar dateNow = TimeConvertion.getCurrentCalendar();
            long dateNowLong = dateNow.getTimeInMillis();
            java.sql.Date d = new java.sql.Date(dateNowLong);
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTdate = format.format(d);
            String msg = ESTdate + " " + newCustomer.getUsername() + " Cust signup Result:" + result;
            CommMsgImp commMsg = new CommMsgImp();

            commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//            
            webStatus.setResultID(result);
            return loginObj;
        }
        webStatus.setResultID(0);
        return loginObj;
    }

}
