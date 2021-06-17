/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processcustacc;

import com.afweb.account.CommMsgImp;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockObj;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public LoginObj getCustomerEmailLogin(ServiceAFweb serviceAFWeb, String EmailUserName, String Password) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(-1);

        custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, Password);
        if (custObj != null) {
            webStatus.setResultID(custObj.getStatus());
            if (custObj.getStatus() != ConstantKey.OPEN) {
                custObj = null;
            }
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;
    }

    public ArrayList getAccountList(ServiceAFweb serviceAFWeb, String EmailUserName, String Password) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        return serviceAFWeb.getAccountImp().getAccountList(UserName, Password);

    }
    
   public LoginObj getCustomerAccLogin(ServiceAFweb serviceAFWeb, String EmailUserName, String AccountIDSt) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        CustomerObj custObj = null;

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, null, AccountIDSt);
        if (accountObj != null) {
            custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, null);
        }
        LoginObj loginObj = new LoginObj();
        if (custObj != null) {
            custObj.setPassword(CKey.MASK_PASS);
        }
        loginObj.setCustObj(custObj);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(1);
        if (custObj == null) {
            webStatus.setResultID(0);
        }
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }
    
    // result 1 = success, 2 = existed,  0 = fail
    public LoginObj updateCustomerPassword(ServiceAFweb serviceAFWeb, String EmailUserName, String AccountID, String Email, String Password, String FirstName, String LastName, String Plan) {

        CustomerObj custObj = null;
        LoginObj loginObj = new LoginObj();
        loginObj.setCustObj(null);
        WebStatus webStatus = new WebStatus();
        webStatus.setResultID(0);
        loginObj.setWebMsg(webStatus);
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return loginObj;
        }
        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();

        custObj = serviceAFWeb.getAccountImp().getCustomerPassword(UserName, null);
        if (custObj == null) {
            return loginObj;
        }
        if (custObj.getStatus() != ConstantKey.OPEN) {
            return loginObj;
        }

        String portfolio = custObj.getPortfolio();
        CustPort custPortfilio = new CustPort();
        if ((portfolio != null) && (portfolio.length() > 0)) {
            try {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            } catch (Exception ex) {
            }
        }

        if ((Email != null) && (Email.length() > 0)) {
            boolean validEmail = NameObj.isEmailValid(Email);
            if (validEmail == true) {
                custObj.setEmail(Email);
            } else {
                // error code 2 for invalid email address
                webStatus.setResultID(2);
                return loginObj;
            }
        }
        if ((Password != null) && (Password.length() > 0)) {
            // ignore "***"
            if (!Password.equals(CKey.MASK_PASS)) {
                custObj.setPassword(Password);
            }
            // error code 3 for invalid password
        }
        if ((FirstName != null) && (FirstName.length() > 0)) {
            custObj.setFirstname(FirstName);
        }
        if ((LastName != null) && (LastName.length() > 0)) {
            custObj.setLastname(LastName);
        }
        if ((Plan != null) && (Plan.length() > 0)) {
            try {

                int planid = Integer.parseInt(Plan);
                // update pending plan
                // -1 no change, 0, 10, 20
                if (planid == -1) {
                    // no change
                } else {
                    if (custObj.getType() == CustomerObj.INT_API_USER) {
                        ;
                    } else {
                        if ((planid == ConstantKey.INT_PP_BASIC) || (planid == ConstantKey.INT_PP_STANDARD)
                                || (planid == ConstantKey.INT_PP_PEMIUM) || (planid == ConstantKey.INT_PP_DELUXEX2)) {
                            custPortfilio.setnPlan(planid);
                        } else {
                            // error
                            return loginObj;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        int result = 0;
        try {
            int accountid = Integer.parseInt(AccountID);
            result = serviceAFWeb.getAccountImp().updateCustomer(custObj, accountid);

            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            result = serviceAFWeb.getAccountImp().updateCustomerPortfolio(custObj.getUsername(), portfStr);
        } catch (Exception ex) {
            logger.info("> updateCustomerPassword exception " + ex.getMessage());
        }

        String tzid = "America/New_York"; //EDT
        TimeZone tz = TimeZone.getTimeZone(tzid);
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long dateNowLong = dateNow.getTimeInMillis();
        java.sql.Date d = new java.sql.Date(dateNowLong);
        DateFormat format = new SimpleDateFormat(" hh:mm a");
        format.setTimeZone(tz);
        String ESTdate = format.format(d);
        String msg = ESTdate + " " + custObj.getUsername() + " Cust update Result:" + result;
        CommMsgImp commMsg = new CommMsgImp();
        commMsg.AddCommMessage(serviceAFWeb, accountAdminObj, ConstantKey.COM_SIGNAL, msg);
//                            
        webStatus.setResultID(result);
//        loginObj.setCustObj(custObj);
        loginObj.setWebMsg(webStatus);
        return loginObj;

    }
    
    
    public AccountObj getAccountByCustomerAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            int accountid = Integer.parseInt(AccountIDSt);
            return serviceAFWeb.getAccountImp().getAccountByCustomerAccountID(UserName, Password, accountid);
        } catch (Exception e) {
        }
        return null;

    }
    
   public ArrayList<AFstockObj> getStockNameListByAccountID(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String AccountIDSt) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return null;
        }
        AccountObj accountObj = getAccountByCustomerAccountID(serviceAFWeb, EmailUserName, Password, AccountIDSt);
        if (accountObj != null) {

            ArrayList stockNameList = serviceAFWeb.getAccountImp().getAccountStockNameList(accountObj.getId());
            return stockNameList;
        }
        return null;
    }
    

}
