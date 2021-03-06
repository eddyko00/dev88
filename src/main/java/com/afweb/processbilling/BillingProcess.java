/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processbilling;

import com.afweb.dbaccount.AccountImp;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.processcustacc.CustAccService;
import com.afweb.service.*;
import com.afweb.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Date;
import java.sql.Timestamp;
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
 * @author eddyko
 */
public class BillingProcess {

    public static final int NO_PAYMENT_1 = 55;
    public static final int NO_PAYMENT_2 = 56;

    protected static Logger logger = Logger.getLogger("BillingProcess");
    private static ArrayList custProcessNameArray = new ArrayList();
    AccountImp accountImp = new AccountImp();

    private ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb) {
        if (custProcessNameArray != null && custProcessNameArray.size() > 0) {
            return custProcessNameArray;
        }
        ArrayList custNameArray = accountImp.getCustomerNList(0);
        if (custNameArray != null) {
            custProcessNameArray = custNameArray;
        }
        return custProcessNameArray;
    }

    public void processUserBillingAll(ServiceAFweb serviceAFWeb) {
        ServiceAFweb.lastfun = "processUserBillingAll";
        logger.info("> updateUserBillingAll ");

        UpdateStockNNprocessNameArray(serviceAFWeb);
        if (custProcessNameArray == null) {
            return;
        }
        if (custProcessNameArray.size() == 0) {
            return;
        }

//        String printName = "";
//        for (int i = 0; i < custProcessNameArray.size(); i++) {
//            printName += custProcessNameArray.get(i) + ",";
//        }
//        logger.info("processUserBillingAll " + printName);
        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "BP_" + CKey.AF_SYSTEM; // + ServiceAFweb.getServerObj().getServerName();
//        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.SysSetLockName(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (ServiceAFweb.mydebugtestflag == true) {
            lockReturn = 1;
        }
        if (lockReturn > 0) {
            long LastServUpdateTimer = System.currentTimeMillis();
            long lockDate5Min = TimeConvertion.addMinutes(LastServUpdateTimer, 15); // add 3 minutes

            while (true) {
                long currentTime = System.currentTimeMillis();
                if (testing == true) {
                    currentTime = 0;
                }
                if (lockDate5Min < currentTime) {
//                    logger.info("ProcessTrainNeuralNet exit after 15 minutes");
                    break;
                }

                if (custProcessNameArray.size() == 0) {
                    break;
                }
                String custName = (String) custProcessNameArray.get(0);
                CustomerObj customer = serviceAFWeb.AccGetCustomerObjByName(custName);
                if (customer != null) {
                    if ((customer.getType() == CustomerObj.INT_ADMIN_USER)) {
                        //                            || (customer.getType() == CustomerObj.INT_FUND_USER)) {
                        ;
                    } else {
                        try {
                            this.updateUserBilling(serviceAFWeb, customer);
                        } catch (Exception ex) {
                            logger.info("> updateUserBillingAll Exception " + ex.getMessage());
                        }
                    }

                }
                custProcessNameArray.remove(0);

            }  // end for loop
            serviceAFWeb.SysLockRemoveLockName(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
//        logger.info("> updateUserBillingAll ... done");
    }
////////////////////////////////////////////////////

    public static boolean isSystemAccount(CustomerObj customer) {
        boolean byPassPayment = false;
        if ((customer.getType() == CustomerObj.INT_ADMIN_USER)) {
            byPassPayment = true;
        }
        if (customer.getUsername().equals(CKey.FUND_MANAGER_USERNAME)) {
            byPassPayment = true;
        } else if (customer.getUsername().equals(CKey.INDEXFUND_MANAGER_USERNAME)) {
            byPassPayment = true;
        } else if (customer.getUsername().equals(CKey.G_USERNAME)) {
            byPassPayment = true;
        } else if (customer.getUsername().equals(CKey.API_USERNAME)) {
            byPassPayment = true;
        }
        return byPassPayment;
    }

    public int updateUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer) {
        ServiceAFweb.lastfun = "updateUserBilling";

        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
        Date currDate = new java.sql.Date(currentDate.getTime());
        if (customer == null) {
            return 0;
        }

        AccountObj account = accountImp.getAccountByType(customer.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = accountImp.getBillingByCustomerAccountID(customer.getUsername(), null, account.getId(), 2);
        if (billingObjList == null) {
            // create first bill 
            createUserBilling(serviceAFWeb, customer, account, null);
            return 0;
        }
        if (billingObjList.size() == 0) {
            // create first bill 
            createUserBilling(serviceAFWeb, customer, account, null);
            return 0;
        }
        boolean firstBill = false;
        if (billingObjList.size() == 1) {
            firstBill = true;
        }
        BillingObj billing = billingObjList.get(0);

        // check if current bill cycle expire
        if (currDate.getTime() < billing.getUpdatedatel()) {
            return 0;
        }

        int status = billing.getStatus();

        float userBalance = customer.getBalance();
        float fPayment = customer.getPayment();

        boolean createBillFlag = true;
        boolean sendMsg = false;
        String msg = "";

        String custN = customer.getEmail();
        if ((custN == null) || (custN.length() == 0)) {
            custN = "customer";
        }
        custN = customer.getFirstname() + "_" + custN;

        if (status == ConstantKey.INITIAL) {
            boolean byPassPayment = BillingProcess.isSystemAccount(customer);

            if (byPassPayment == true) {
                userBalance = fPayment;
            }
            BillData billData = null;
            float credit = 0;
            String billingDataSt = billing.getData();
            try {
                if ((billingDataSt != null) && (billingDataSt.length() > 0)) {
                    billingDataSt = billingDataSt.replaceAll("#", "\"");
                    billData = new ObjectMapper().readValue(billingDataSt, BillData.class);
                    credit = billData.getCredit();
                }
            } catch (Exception ex) {
            }

            if (userBalance >= fPayment) {
                //the remaining goes to the next invoice.
                userBalance = userBalance - fPayment;
                customer.setBalance(userBalance);
                customer.setPayment(0);

                // transaction
                int result = SysCustStatusPaymentBalance(serviceAFWeb, customer.getUsername(), null, customer.getPayment() + "", customer.getBalance() + "");

                billing.setStatus(ConstantKey.COMPLETED);

                billing.setBalance(fPayment);
                result = accountImp.updateAccountBillingStatusPaymentData(billing.getId(), billing.getStatus(), billing.getPayment(), billing.getBalance(), billing.getData());
                // transaction
                // send email disable
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                String currency = formatter.format(fPayment);
                msg = "The " + custN + " account bill id (" + billing.getId() + ") payment " + currency + " is completed! Thank you.";
                sendMsg = true;
                logger.info("Billing***Completed user " + custN + ", billing id " + billing.getId());

                //////// reset feature list to clear delfund
                processFeat(serviceAFWeb, customer);

            } else {
//                Date entryDate = billing.getUpdatedatedisplay();
                long billcycleDate = billing.getUpdatedatel();
                long dateWeek = TimeConvertion.nextWeek(billcycleDate);

                if (firstBill == true) {
                    dateWeek = TimeConvertion.nextWeek(dateWeek);
                }
                int subStatus = billing.getSubstatus();
                if (subStatus == NO_PAYMENT_1) {
                    if (currDate.getTime() > dateWeek) {
                        if (customer.getStatus() != ConstantKey.DISABLE) {
                            if (subStatus != NO_PAYMENT_2) {
//                            if (fPayment < 2) {
//                                //ignore if payment less than 4 dollor
//                                billing.setSubstatus(NO_PAYMENT_2);
//                                accountImp.updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());
//                                return 1;
//                            }
                                billing.setSubstatus(NO_PAYMENT_2);

                                customer.setStatus(ConstantKey.DISABLE);
                                int result = SysCustStatusPaymentBalance(serviceAFWeb, customer.getUsername(), customer.getStatus() + "", null, null);
                                result = accountImp.updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());

                                // send email disable
                                msg = "The " + custN + " account had been disabled due to outstanding payment! Thank you for using IIS.";
                                sendMsg = true;
                                logger.info("Billing***Disable user " + custN + ", billing id " + billing.getId());
                            }
                        }
                    }
                }
                if (currDate.getTime() > billcycleDate) {
                    if ((subStatus == NO_PAYMENT_1) || (subStatus == NO_PAYMENT_2)) {
                        ;
                    } else {
                        billing.setSubstatus(NO_PAYMENT_1);
                        int result = accountImp.updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());

                        // send email reminder
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                        String currency = formatter.format(fPayment);
                        msg = "The " + custN + " account  bill (" + billing.getId() + ") has past due " + currency + " amount! Please submit the payment now to iisweb88@gmail.com.";
                        sendMsg = true;
                        logger.info("Billing***PastDue user " + custN + ", billing id " + billing.getId());
                    }
                }

            }
            createBillFlag = false;
        }
//        if (status == ConstantKey.COMPLETED) {
//            // check for next bill
//            createUserBilling(serviceAFWeb, customer, billing);
//        }
        // check for next bill

        if (sendMsg == true) {
            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            DateFormat format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTtime = format.format(d);

            String msgSt = ESTtime + " " + msg;

            accountImp.addAccountMessage(account, ConstantKey.COM_BILLMSG, msg);
            AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
            accountImp.addAccountMessage(accountAdminObj, ConstantKey.COM_BILLMSG, msg);

            // send email
            DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
            formatD.setTimeZone(tz);
            String ESTdateD = formatD.format(d);
            String msgD = ESTdateD + " " + msg;
            accountImp.addAccountEmailMessage(account, ConstantKey.COM_BILLMSG, msgD);
        }
//        
        if (createBillFlag == true) {
            int retCreatebill = createUserBilling(serviceAFWeb, customer, account, billing);
        }
        return 1;
    }

    public int processFeat(ServiceAFweb serviceAFWeb, CustomerObj customer) {
        ServiceAFweb.lastfun = "processFeat";

        String portfolio = customer.getPortfolio();
        CustPort custPortfilio = null;
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            }
        } catch (Exception ex) {
        }
        if (custPortfilio == null) {
            return 0;
        }
        ArrayList<String> featL = custPortfilio.getFeatL();
        if (featL == null) {
            return 0;
        }
        ArrayList<String> featNewL = this.testfeatL(featL);
        custPortfilio.setFeatL(featNewL);
        try {

            String portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            accountImp.updateCustomerPortfolio(customer.getUsername(), portfStr);
            return 1;
        } catch (Exception ex) {
        }
        return 1;
    }

    private ArrayList<String> testfeatL(ArrayList<String> featOrgL) {
        ArrayList<String> featL = new ArrayList();

        if (featL == null) {
            return featL;
        }
        featL.addAll(featOrgL);
        ArrayList<String> delfeatL = new ArrayList();
        for (int i = 0; i < featL.size(); i++) {
            String feat = featL.get(i);
            if (feat.indexOf("delfund") != -1) {
                String defFundIdSt = feat.replace("delfund", "");
                try {
                    int defFundId = Integer.parseInt(defFundIdSt);
                    delfeatL.add("delfund" + defFundId);
                    delfeatL.add("fund" + defFundId);
                } catch (Exception e) {
                }
            }
        }
        if (delfeatL.size() == 0) {
            return featL;
        }
        for (int i = 0; i < delfeatL.size(); i++) {
            String delfeat = featL.get(i);
            for (int j = 0; j < featL.size(); j++) {
                String feat = featL.get(j);
                if (delfeat.equals(feat)) {
                    featL.remove(j);
                }
            }
        }
        return featL;
    }

    public int createUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer, AccountObj account, BillingObj billing) {
        ServiceAFweb.lastfun = "createUserBilling";
        if (customer.getType() == CustomerObj.INT_ADMIN_USER) {
            return 1;
        }
        Date startDate = customer.getStartdate();
        long billCycleDate = startDate.getTime();

        if (billing != null) {
            long lastBillDate = billing.getUpdatedatel();
            billCycleDate = TimeConvertion.addMonths(lastBillDate, 1);
        }
        BillData billData = new BillData();

        Timestamp cDate = TimeConvertion.getCurrentTimeStamp();
        Date curDate = new java.sql.Date(cDate.getTime());
        long date5day = TimeConvertion.addDays(curDate.getTime(), 5);

        if (date5day > billCycleDate) {

            String custN = customer.getEmail();
            if ((custN == null) || (custN.length() == 0)) {
                custN = "customer";
            }
            custN = customer.getFirstname() + "_" + custN;

            String portfolio = customer.getPortfolio();
            String portfStr;
            CustPort custPortfilio = new CustPort();
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                } else {
                    portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                    accountImp.updateCustomerPortfolio(customer.getUsername(), portfStr);

                }
            } catch (Exception ex) {
                logger.info("createUserBilling exception ");
            }

            if (custPortfilio.getnPlan() != -1) {

                int plan = custPortfilio.getnPlan();

//                int substatus = account.getSubstatus();
//                float investment = account.getInvestment();
//                float balance = account.getBalance();
//                float servicefee = account.getServicefee();
//                accountImp.updateAccountStatusByAccountID(account.getId(), substatus, investment, balance, servicefee);
                // update the plan
                customer.setSubstatus(plan);
                accountImp.updateCustStatusSubStatus(customer, customer.getStatus(), customer.getSubstatus());

            }

            float payment = customer.getPayment();
            float prevOwning = payment;

            int subType = customer.getSubstatus();  // price plan
            float fInvoice = 0;
            switch (subType) {
                case ConstantKey.INT_PP_BASIC:
                    billData.setFeat(ConstantKey.PP_BASIC);
                    fInvoice = ConstantKey.INT_PP_BASIC_PRICE;
                    break;
                case ConstantKey.INT_PP_STANDARD:
                    billData.setFeat(ConstantKey.PP_STANDARD);
                    fInvoice = ConstantKey.INT_PP_STANDARD_PRICE;
                    break;
                case ConstantKey.INT_PP_PEMIUM:
                    billData.setFeat(ConstantKey.PP_PEMIUM);
                    fInvoice = ConstantKey.INT_PP_PEMIUM_PRICE;
                    break;
                case ConstantKey.INT_PP_DELUXEX2:
                    billData.setFeat(ConstantKey.PP_DELUXEX2);
                    fInvoice = ConstantKey.INT_PP_DELUXEX2_PRICE;
                    break;
                case ConstantKey.INT_PP_API:
                    billData.setFeat(ConstantKey.PP_API);
                    fInvoice = ConstantKey.INT_PP_API_PRICE;
                    break;
            }
            billData.setCurPaym(fInvoice);  ///// for the plan invoice
            customer.setPayment(fInvoice);
            int result = 0;

            //Add feature
            float featureInvoice = 0;
            ArrayList<String> featL = custPortfilio.getFeatL();
            ArrayList<String> featNewL = this.testfeatL(featL);
            int featCnt = featNewL.size();
            if (featCnt > 0) {
                featureInvoice = (featCnt * FUND30_FeaturePrice);
                billData.setService(featureInvoice);
                fInvoice += featureInvoice;
            }
            if (custPortfilio.getServ() > 0) {
                billData.setService(billData.getService() + custPortfilio.getServ());
                fInvoice += custPortfilio.getServ();
            }
            if (custPortfilio.getCred() > 0) {
                billData.setCredit(billData.getCredit() + custPortfilio.getCred());
            }
            try {
                custPortfilio.setnPlan(-1);
                custPortfilio.setServ(0);
                custPortfilio.setCred(0);
                portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                accountImp.updateCustomerPortfolio(customer.getUsername(), portfStr);
            } catch (JsonProcessingException ex) {
            }

            // first bill alreay add the payment
            // but the next bill need to add prev owning
            boolean firstBill = false;
            if (billing != null) {
                billData.setPrevOwn(prevOwning);
                payment = fInvoice + prevOwning - billData.getCredit();
                customer.setPayment(payment);
                result = SysCustStatusPaymentBalance(serviceAFWeb, customer.getUsername(), null, customer.getPayment() + "", null);
            } else {
                // first bill
                firstBill = true;
                if (payment == 0) {
                    payment = fInvoice - billData.getCredit();
                    customer.setPayment(payment);
                    result = SysCustStatusPaymentBalance(serviceAFWeb, customer.getUsername(), null, customer.getPayment() + "", null);
                }
            }

            String data = "";
            String nameSt = "";
            try {

                nameSt = new ObjectMapper().writeValueAsString(billData);
                nameSt = nameSt.replaceAll("\"", "#");
                data = nameSt;
            } catch (JsonProcessingException ex) {
            }

            float balance = 0;
            result = accountImp.addAccountBilling(customer.getUsername(), account, payment, balance, data, billCycleDate);

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.sql.Date d = new java.sql.Date(billCycleDate);
            DateFormat format = new SimpleDateFormat("M/dd/yyyy");
            format.setTimeZone(tz);
            String billcycleESTtime = format.format(d);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            String currency = formatter.format(payment);
            String msg = "The " + custN + " account bill on " + billcycleESTtime + " invoice for the amount " + currency + " is ready! Please submit the payment now.";

            tzid = "America/New_York"; //EDT
            tz = TimeZone.getTimeZone(tzid);
            d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTtime = format.format(d);

            String msgSt = ESTtime + " " + msg;
//            String compassMsgSt = ServiceAFweb.compress(msgSt);
            accountImp.addAccountMessage(account, ConstantKey.COM_BILLMSG, msgSt);
            AccountObj accountAdminObj = serviceAFWeb.SysGetAdminObjFromCache();
            accountImp.addAccountMessage(accountAdminObj, ConstantKey.COM_BILLMSG, msgSt);

            // send email
            DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
            formatD.setTimeZone(tz);
            String ESTdateD = formatD.format(d);
            String msgD = ESTdateD + " " + msg;
            msgD += "<p>Payment is done through e.transfer from your bank to the iisweb payment email address (iisweb88@gmail.com)."
                    + "<br>The e.transfer question is the 'iisweb-' plus user login email (e.g. iisweb-email@domain.com) and the answer is the user login email.";

//            compassMsgSt = ServiceAFweb.compress(msgD);
            accountImp.addAccountEmailMessage(account, ConstantKey.COM_BILLMSG, msgD);

            logger.info("Billing***create user " + custN + ", billing cycle " + billcycleESTtime + ", payment=" + payment);

            /// Adding credit to other mutural fund user
            /// Adding credit to other mutural fund user            
            for (int i = 0; i < featNewL.size(); i++) {
                String feat = featNewL.get(i);
                if (feat.indexOf("fund") != -1) {
                    String FundIdSt = feat.replace("fund", "");
                    try {
                        int FundId = Integer.parseInt(FundIdSt);
                        AccountObj accFund = accountImp.getAccountByAccountID(FundId);
                        CustomerObj custFund = serviceAFWeb.SysGetCustomerByAccoutObj(accFund);
                        portfolio = custFund.getPortfolio();
                        custPortfilio = new CustPort();
                        if ((portfolio != null) && (portfolio.length() > 0)) {
                            portfolio = portfolio.replaceAll("#", "\"");
                            custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                        } else {
                            portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                            accountImp.updateCustomerPortfolio(custFund.getUsername(), portfStr);
                        }
                        //////update credit to the fund mgr
                        float featureFundP = FUND30_FeaturePrice / 2;
                        custPortfilio.setCred(custPortfilio.getCred() + featureFundP);
                        portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                        accountImp.updateCustomerPortfolio(custFund.getUsername(), portfStr);

                    } catch (Exception e) {
                    }
                }
            }

            if (customer.getType() == CustomerObj.INT_GUEST_USER) {
                if (firstBill == false) {
                    this.processMonthEvent(serviceAFWeb);

                }
            }
            return result;
        }
        return 0;
    }

    public void processMonthEvent(ServiceAFweb serviceAFWeb) {
        // update multal fund every month
        serviceAFWeb.AccFundSelectBest();
    }

    public static float FUND30_FeaturePrice = 30;

    public int updateFundFeat(ServiceAFweb serviceAFWeb, CustomerObj customer, AccountObj accFund) {
        ServiceAFweb.lastfun = "updateFundFeat";
        logger.info(">updateFundFeat " + accFund.getAccountname());

        // should be transaction problem
        // Need to refesh customer from DB to get the portfolio
        // Need to refesh customer from DB to get the portfolio        
        String name = customer.getUsername();
        customer = serviceAFWeb.AccGetCustomerObjByName(name);
        String portfolio = customer.getPortfolio();
        String portfStr;
        CustPort custPortfilio = new CustPort();
        try {
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            } else {
                portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                accountImp.updateCustomerPortfolio(customer.getUsername(), portfStr);
            }

            /// adding feature
            //proate to bill cycle
            //proate to bill cycle    
            Date startDate = customer.getStartdate();
            long billCycleDate = startDate.getTime();

            AccountObj account = accountImp.getAccountByType(customer.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
            // get last bill
            ArrayList<BillingObj> billingObjList = accountImp.getBillingByCustomerAccountID(customer.getUsername(), null, account.getId(), 2);
            boolean firstBill = false;
            if (billingObjList != null) {
                if (billingObjList.size() > 0) {
                    BillingObj billing = billingObjList.get(0);
                    billCycleDate = billing.getUpdatedatel();
                }
            }

            billCycleDate = TimeConvertion.endOfDayInMillis(billCycleDate);

            long NextbillCycleDate = TimeConvertion.addMonths(billCycleDate, 1);

            long currDate = TimeConvertion.getCurrentTimeStamp().getTime();
            currDate = TimeConvertion.endOfDayInMillis(currDate);

            int prorateDay = 0;
            if (billCycleDate > currDate) {
                long prorateDayl = (billCycleDate - currDate) / TimeConvertion.DAY_TIME_TICK;
                prorateDay = (int) prorateDayl;
            } else {
                long prorateDayl = (NextbillCycleDate - currDate) / TimeConvertion.DAY_TIME_TICK;
                prorateDay = (int) prorateDayl;
            }

            float featureP = 0;
            float featureFundP = 0;

            long nextmonthl = (NextbillCycleDate - billCycleDate) / TimeConvertion.DAY_TIME_TICK;
            int nextMonDay = (int) nextmonthl;

            float prorate = prorateDay * FUND30_FeaturePrice / nextMonDay;
            int iprorate = (int) (prorate * 100);
            float tempfeatureP = iprorate;
            tempfeatureP /= 100;
            if (tempfeatureP > 2) {
                featureP = tempfeatureP;

                iprorate = iprorate / 2;
                featureFundP = iprorate;
                featureFundP /= 100;
                featureFundP = featureP - featureFundP;
            }

            // update bill payment for the customer
            custPortfilio.setServ(custPortfilio.getServ() + featureP);
            portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            accountImp.updateCustomerPortfolio(customer.getUsername(), portfStr);

            CustomerObj custFund = serviceAFWeb.SysGetCustomerByAccoutObj(accFund);
            portfolio = custFund.getPortfolio();
            custPortfilio = new CustPort();
            if ((portfolio != null) && (portfolio.length() > 0)) {
                portfolio = portfolio.replaceAll("#", "\"");
                custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
            } else {
                portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                accountImp.updateCustomerPortfolio(custFund.getUsername(), portfStr);
            }
            //////update credit to the fund mgr
            custPortfilio.setCred(custPortfilio.getCred() + featureFundP);
            portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
            accountImp.updateCustomerPortfolio(custFund.getUsername(), portfStr);

        } catch (Exception ex) {
            logger.info("createUserBilling exception ");
        }

        return 0;
    }
/////////////////////////////////////////////////////////////////
    CustAccService custAccSrv = new CustAccService();

    public int SysCustStatusPaymentBalance(ServiceAFweb serviceAFWeb, String customername,
            String statusSt, String paymenttSt, String balanceSt) {

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = custAccSrv.getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
            }
            ArrayList accountList = serviceAFWeb.AccGetAccountList(UserName, null);

            if (accountList == null) {
                return 0;
            }
            AccountObj accountObj = null;
            for (int i = 0; i < accountList.size(); i++) {
                AccountObj accountTmp = (AccountObj) accountList.get(i);
                if (accountTmp.getType() == AccountObj.INT_TRADING_ACCOUNT) {
                    accountObj = accountTmp;
                    break;
                }
            }
            if (accountObj == null) {
                return 0;
            }

            int status = -9999;
            if (statusSt != null) {
                if (!statusSt.equals("")) {
                    status = Integer.parseInt(statusSt);
                }
            }
            float payment = -9999;
            if (paymenttSt != null) {
                if (!paymenttSt.equals("")) {
                    payment = Float.parseFloat(paymenttSt);
                }
            }
            float balance = -9999;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);
                }
            }
            return serviceAFWeb.AccUpdateCustStatusPaymentBalance(UserName, status, payment, balance);

        } catch (Exception e) {
        }
        return 0;
    }

    //////////////////////////////
}
