/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

import com.afweb.model.*;
import com.afweb.model.account.*;
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

    private ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb) {
        if (custProcessNameArray != null && custProcessNameArray.size() > 0) {
            return custProcessNameArray;
        }
        ArrayList custNameArray = serviceAFWeb.getAccountImp().getCustomerNList(0);
        if (custNameArray != null) {
            custProcessNameArray = custNameArray;
        }
        return custProcessNameArray;
    }

    public void processUserBillingAll(ServiceAFweb serviceAFWeb) {
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

        LockName = "BILL_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
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
                ArrayList custNameList = serviceAFWeb.getCustomerObjByNameList(custName);
                if (custNameList != null) {
                    if (custNameList.size() == 1) {

                        CustomerObj customer = (CustomerObj) custNameList.get(0);
                        if ((customer.getType() == CustomerObj.INT_ADMIN_USER)
                                || (customer.getType() == CustomerObj.INT_FUND_USER)) {
                            ;
                        } else {
                            try {
                                this.updateUserBilling(serviceAFWeb, customer);
                            } catch (Exception ex) {
                                logger.info("> updateUserBillingAll Exception " + ex.getMessage());
                            }
                        }
                    }
                }
                custProcessNameArray.remove(0);

            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
//        logger.info("> updateUserBillingAll ... done");
    }
////////////////////////////////////////////////////

    public int updateUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer) {

        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
        Date currDate = new java.sql.Date(currentDate.getTime());
        if (customer == null) {
            return 0;
        }

        AccountObj account = serviceAFWeb.getAccountImp().getAccountByType(customer.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(customer.getUsername(), null, account.getId(), 2);
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

        boolean sendMsg = false;
        String msg = "";

        String custName = customer.getEmail();
        if ((custName == null) || (custName.length() == 0)) {
            custName = customer.getUsername();
        }

        if (status == ConstantKey.INITIAL) {
            // override payment
            if ((customer.getType() == CustomerObj.INT_ADMIN_USER)
                    || (customer.getType() == CustomerObj.INT_FUND_USER)
                    || (customer.getType() == CustomerObj.INT_GUEST_USER)) {
                userBalance = fPayment;
            }

            if (userBalance >= fPayment) {
                //the remaining goes to the next invoice.
                userBalance = userBalance - fPayment;
                customer.setBalance(userBalance);
                customer.setPayment(0);

                // transaction
                int result = serviceAFWeb.systemCustStatusPaymentBalance(customer.getUsername(), null, customer.getPayment() + "", customer.getBalance() + "");

                billing.setStatus(ConstantKey.COMPLETED);

                billing.setBalance(fPayment);
                result = serviceAFWeb.getAccountImp().updateAccountBillingStatusPaymentData(billing.getId(), billing.getStatus(), billing.getPayment(), billing.getBalance(), billing.getData());
                // transaction
                // send email disable
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                String currency = formatter.format(fPayment);
                msg = "The " + custName + " account bill id (" + billing.getId() + ") payment " + currency + " is completed! Thank you.";
                sendMsg = true;
                logger.info("Billing***Completed user " + custName + ", billing id " + billing.getId());

                ////////
                processFeat(customer);

            } else {
//                Date entryDate = billing.getUpdatedatedisplay();
                long billcycleDate = billing.getUpdatedatel();
                long dateWeek = TimeConvertion.nextWeek(billcycleDate);

                if (firstBill == true) {
                    dateWeek = TimeConvertion.nextWeek(dateWeek);
                }
                int subStatus = billing.getSubstatus();
                if (billcycleDate > dateWeek) {
                    if (customer.getStatus() != ConstantKey.DISABLE) {
                        if (subStatus != NO_PAYMENT_2) {
//                            if (fPayment < 2) {
//                                //ignore if payment less than 4 dollor
//                                billing.setSubstatus(NO_PAYMENT_2);
//                                serviceAFWeb.getAccountImp().updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());
//                                return 1;
//                            }
                            billing.setSubstatus(NO_PAYMENT_2);

                            customer.setStatus(ConstantKey.DISABLE);
                            int result = serviceAFWeb.systemCustStatusPaymentBalance(customer.getUsername(), customer.getStatus() + "", null, null);
                            result = serviceAFWeb.getAccountImp().updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());

                            // send email disable
                            msg = "The " + custName + " account had been disabled due to outstanding payment! Thank you for using IIS.";
                            sendMsg = true;
                            logger.info("Billing***Disable user " + custName + ", billing id " + billing.getId());
                        }
                    }
                } else if (currDate.getTime() > billcycleDate) {
                    if ((subStatus != NO_PAYMENT_1) && (subStatus != NO_PAYMENT_2)) {
                        billing.setSubstatus(NO_PAYMENT_1);
                        int result = serviceAFWeb.getAccountImp().updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());

                        // send email reminder
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                        String currency = formatter.format(fPayment);
                        msg = "The " + custName + " account  bill (" + billing.getId() + ") has past due " + currency + " amount! Please submit the payment now to iisweb88@gmail.com.";
                        sendMsg = true;
                        logger.info("Billing***PastDue user " + custName + ", billing id " + billing.getId());
                    }
                }

            }
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

            serviceAFWeb.getAccountImp().addAccountMessage(account, ConstantKey.COM_ACCBILLMSG, msg);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            serviceAFWeb.getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.COM_ACCBILLMSG, msg);

            // send email
            DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
            formatD.setTimeZone(tz);
            String ESTdateD = formatD.format(d);
            String msgD = ESTdateD + " " + msg;
            serviceAFWeb.getAccountImp().addAccountEmailMessage(account, ConstantKey.COM_ACCBILLMSG, msgD);
        }
        int retCreatebill = createUserBilling(serviceAFWeb, customer, account, billing);

        return 1;
    }

    public int processFeat(CustomerObj customer) {
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
            return 0;
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
        return 1;
    }

    public int createUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer, AccountObj account, BillingObj billing) {
        if (customer.getType() == CustomerObj.INT_ADMIN_USER) {
            return 1;
        }
        Date startDate = account.getStartdate();
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
            String custName = customer.getEmail();
            if ((custName == null) || (custName.length() == 0)) {
                custName = customer.getUsername();
            }

            String portfolio = customer.getPortfolio();
            String portfStr;
            CustPort custPortfilio = new CustPort();
            try {
                if ((portfolio != null) && (portfolio.length() > 0)) {
                    portfolio = portfolio.replaceAll("#", "\"");
                    custPortfilio = new ObjectMapper().readValue(portfolio, CustPort.class);
                } else {
                    portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                    serviceAFWeb.getAccountImp().updateCustomerPortfolio(customer.getUsername(), portfStr);

                }
            } catch (Exception ex) {
                logger.info("createUserBilling exception");
            }

            if (custPortfilio.getnPlan() != -1) {

                try {
                    int plan = custPortfilio.getnPlan();

                    int substatus = account.getSubstatus();
                    float investment = account.getInvestment();
                    float balance = account.getBalance();
                    float servicefee = account.getServicefee();
                    serviceAFWeb.getAccountImp().updateAccountStatusByAccountID(account.getId(), substatus, investment, balance, servicefee);

                    customer.setSubstatus(plan);
                    serviceAFWeb.getAccountImp().updateCustStatusSubStatus(customer, customer.getStatus(), customer.getSubstatus());

                    custPortfilio.setnPlan(-1);
                    portfStr = new ObjectMapper().writeValueAsString(custPortfilio);
                    serviceAFWeb.getAccountImp().updateCustomerPortfolio(customer.getUsername(), portfStr);

                } catch (JsonProcessingException ex) {
                }
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
                case ConstantKey.INT_PP_PREMIUM:
                    billData.setFeat(ConstantKey.PP_PREMIUM);
                    fInvoice = ConstantKey.INT_PP_REMIUM_PRICE;
                    break;
                case ConstantKey.INT_PP_DELUXE:
                    billData.setFeat(ConstantKey.PP_DELUXE);
                    fInvoice = ConstantKey.INT_PP_DELUXE_PRICE;
                    break;
            }

            billData.setCurPaym(fInvoice);
            customer.setPayment(payment);
            int result = 0;

            // first bill alreay add the payment
            // but the next bill need to add prev owning
            boolean firstBill = false;
            if (billing != null) {
                billData.setPrevOwn(prevOwning);
                payment = fInvoice + prevOwning;
                customer.setPayment(payment);
                result = serviceAFWeb.systemCustStatusPaymentBalance(custName, null, customer.getPayment() + "", null);
            } else {
                // first bill
                firstBill = true;
                if (payment == 0) {
                    payment = fInvoice;
                    customer.setPayment(payment);
                    result = serviceAFWeb.systemCustStatusPaymentBalance(custName, null, customer.getPayment() + "", null);
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
            result = serviceAFWeb.getAccountImp().addAccountBilling(custName, account, payment, balance, data, billCycleDate);

            String tzid = "America/New_York"; //EDT
            TimeZone tz = TimeZone.getTimeZone(tzid);
            java.sql.Date d = new java.sql.Date(billCycleDate);
            DateFormat format = new SimpleDateFormat("M/dd/yyyy");
            format.setTimeZone(tz);
            String billcycleESTtime = format.format(d);

            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            String currency = formatter.format(payment);
            String msg = "The " + custName + " account bill on " + billcycleESTtime + " invoice for the amount " + currency + " is ready! Please submit the payment now.";

            tzid = "America/New_York"; //EDT
            tz = TimeZone.getTimeZone(tzid);
            d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
            format = new SimpleDateFormat(" hh:mm a");
            format.setTimeZone(tz);
            String ESTtime = format.format(d);

            String msgSt = ESTtime + " " + msg;
//            String compassMsgSt = ServiceAFweb.compress(msgSt);
            serviceAFWeb.getAccountImp().addAccountMessage(account, ConstantKey.COM_ACCBILLMSG, msgSt);
            AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
            serviceAFWeb.getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.COM_ACCBILLMSG, msgSt);

            // send email
            DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
            formatD.setTimeZone(tz);
            String ESTdateD = formatD.format(d);
            String msgD = ESTdateD + " " + msg;
            msgD += "<p>Payment is done through e.transfer from your bank to the iisweb payment email address (iisweb88@gmail.com)."
                    + "<br>The e.transfer question is the 'iisweb-' plus user login email (e.g. iisweb-email@domain.com) and the answer is the user login email.";

//            compassMsgSt = ServiceAFweb.compress(msgD);
            serviceAFWeb.getAccountImp().addAccountEmailMessage(account, ConstantKey.COM_ACCBILLMSG, msgD);

            logger.info("Billing***create user " + custName + ", billing cycle " + billcycleESTtime + ", payment=" + payment);

            if (customer.getType() == CustomerObj.INT_GUEST_USER) {
                if (firstBill == false) {
                    // update multal fund every month
                    serviceAFWeb.SystemFundSelectBest();
                }
            }
            return result;
        }
        return 0;
    }

}
