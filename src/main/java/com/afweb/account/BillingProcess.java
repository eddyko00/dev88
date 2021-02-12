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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class BillingProcess {
//    public static final int OPEN = 0;
//    public static final String INT_ST_OPEN = "0";
//    public static final String MSG_CLOSE = "CLOSE";
//    public static final int CLOSE = 1;
//    public static final String INT_ST_CLOSE = "1";
//    public static final String MSG_ENABLE = "ENABLE";
//    public static final int ENABLE = 0;
//    public static final String INT_ST_ENABLE = "0";
//    public static final String MSG_DISABLE = "DISABLE";
//    public static final int DISABLE = 1;
//    public static final String INT_ST_DISABLE = "1";
//    //
//    public static final String MSG_INITIAL = "INITIAL";
//    public static final int INITIAL = 2;
//    public static final String INT_ST_INITIAL = "2";
//    //
//    public static final String MSG_PENDING = "PENDING";
//    public static final int PENDING = 3;
//    public static final String INT_ST_PENDING = "3";
//    //
//    public static final String MSG_PROCESS = "PROCESS";
//    public static final int PROCESS = 4;
//    public static final String INT_ST_PROCESS = "4";
//    //
//    public static final String MSG_FAIL = "FAIL";
//    public static final int FAIL = 5;
//    public static final String INT_ST_FAIL = "5";
//    //    
//    public static final String MSG_DELETE = "DELETE";
//    public static final int DELETE = 9;
//    public static final String INT_ST_DELETE = "9";
//    public static final String MSG_CANCEL = "CANCEL";
//    public static final int CANCEL = 10;
//    public static final String INT_ST_CANCEL = "10";
//    //
//    public static final String MSG_COMPLETE = "COMPLETE";
//    public static final int COMPLETE = 11;
//    public static final String INT_ST_COMPLETE = "11";
//    public static final String MSG_PARTIAL_COMPLETE = "PARTIAL_COMPLETE";
//    public static final int PARTIAL_COMPLETE = 12;
//    public static final String INT_ST_PARTIAL_COMPLETE = "12";
//    public static final int NO_PAYMENT_1 = 55;
//    public static final int NO_PAYMENT_2 = 56;
//    //Billing type
//    public static final int BILLING_SYSTEM = 121;
//    public static final int BILLING_MONTHLY = 122;
//    public static final int BILLING_SERVICE = 125;
//
//    public static String getBillingType(int type) {
//        String bType = "MTM";
//        switch (type) {
//            case BILLING_SYSTEM:
//                bType = "SYS";
//                break;
//
//            case BILLING_SERVICE:
//                bType = "SRV";
//                break;
//        }
//        return bType;
//    }
    //Payment method

    public static final int PAYMENT_INIT = 100;
    public static final int PAYMENT_CREDIT_CARD = 101;
    public static final int PAYMENT_PAY_PAL = 102;
    public static final int PAYMENT_CHEQUE = 103;
    public static final int PAYMENT_CASH = 104;
    public static final int PAYMENT_CREDIT = 105;
    public static final int PAYMENT_ADJUST = 106;

    protected static Logger logger = Logger.getLogger("BillingProcess");
    private static ArrayList stockNNprocessNameArray = new ArrayList();

    private ArrayList UpdateStockNNprocessNameArray(ServiceAFweb serviceAFWeb) {
        if (stockNNprocessNameArray != null && stockNNprocessNameArray.size() > 0) {
            return stockNNprocessNameArray;
        }
        ArrayList stockNameArray = serviceAFWeb.getAccountImp().getCustomerNList(0);
        if (stockNameArray != null) {
            stockNNprocessNameArray = stockNameArray;
        }
        return stockNNprocessNameArray;
    }

    public void processUserBillingAll(ServiceAFweb serviceAFWeb) {
        logger.info("> updateUserBillingAll ");

        UpdateStockNNprocessNameArray(serviceAFWeb);
        if (stockNNprocessNameArray == null) {
            return;
        }
        if (stockNNprocessNameArray.size() == 0) {
            return;
        }

        String printName = "";
        for (int i = 0; i < stockNNprocessNameArray.size(); i++) {
            printName += stockNNprocessNameArray.get(i) + ",";
        }
        logger.info("ProcessTrainNN2NeuralNetBySign " + printName);

        String LockName = null;
        Calendar dateNow = TimeConvertion.getCurrentCalendar();
        long lockDateValue = dateNow.getTimeInMillis();

        LockName = "BILL_" + ServiceAFweb.getServerObj().getServerName();
        LockName = LockName.toUpperCase().replace(CKey.WEB_SRV.toUpperCase(), "W");
        long lockReturn = serviceAFWeb.setLockNameProcess(LockName, ConstantKey.NN_LOCKTYPE, lockDateValue, ServiceAFweb.getServerObj().getSrvProjName() + "_ProcessTrainNeuralNet");
        boolean testing = false;
        if (testing == true) {
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

                if (stockNNprocessNameArray.size() == 0) {
                    break;
                }
                String custName = (String) stockNNprocessNameArray.get(0);
                ArrayList custNameList = serviceAFWeb.getCustomerObjByNameList(custName);
                if (custNameList == null) {
                    stockNNprocessNameArray.remove(0);
                    continue;
                }
                if (custNameList.size() == 0) {
                    stockNNprocessNameArray.remove(0);
                    continue;
                }
                CustomerObj customer = (CustomerObj) custNameList.get(0);

                String symbolTR = (String) stockNNprocessNameArray.get(0);

            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
        logger.info("> updateUserBillingAll ... done");

//        String[] namelist = SQLObject.getUserAll();
//        if (namelist == null) {
//            return CKey.WS_SUCCESS;
//        }
//
//        for (int i = 1; i < namelist.length; i++) {
//            if (CKey.isSystemEnable() == false) {
//                return CKey.WS_SUCCESS;
//            }
//
//            String name = namelist[i];
//            updateUserBilling(name);
////            updateMemberBalance(name);
//
//        }
//
//        return CKey.WS_SUCCESS;
    }
//

    public String disableUserBillingID(String username, String billId) {
        return ""; //CKey.WS_SUCCESS;
    }

    public String updateUserBilling(String username) {
//        AFcustomer customer = null;
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//
//        // just for testing
////        System.out.println("Debug updateUserBilling "+username);
////        long d = 41;
////        username = "silveruser@iisystem.com";
////        long testDate = d * 1000 * 60 * 60 * 24;
////        date = new java.sql.Date(currentDate.getTime() + testDate);
//        // just for testing
//
//        Date Billcycle = date;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        if (customer == null) {
//            return CKey.WS_FAIL;
//        }
//
//        String email = customer.getEmail();
//
//        AFbilling billing = Billing.getlastMTMBilling(username);
//
//
//        if (billing != null) {
//
//            // check if expire
//            int status = billing.getStatus();
//            // used for non-payment status
//            int paymentStatue = billing.getPayStatus();
//            float userBalance = customer.getBalance();
//            boolean employeeFlag = false;
//            String employee = customer.getEmployeeNo();
//            if (employee != null) {
//                if (employee.equals("1")) {
//                    employeeFlag = true;
//                }
//            }
//            float fPayment = (billing.getInvoice() + billing.getInvoice1()) - billing.getCredit();
//            String billingId = billing.getBillingId();
//            if (status == CKey.DISABLE) {
//                //no actiion
//                ;
//
//            } else if (status == CKey.INITIAL) {
//
//                if (userBalance >= fPayment) {
//                    //the remaining goes to the next invoice.
//                    userBalance = userBalance - fPayment;
//                    customer.setBalance(userBalance);
//                    customer.save();
//                    return completeBilling(username, billingId, fPayment);
//
//                } else {
//
//                    if (employeeFlag == true) {
//                        return completeBilling(username, billingId, fPayment);
//                    }
//
//                    Date entryDate = billing.getEntryDate();
//                    long dateWeek = TimeConvertion.nextWeek(entryDate.getTime());
//                    if (date.getTime() > dateWeek) {
//                        if (customer.getStatus() != CKey.DISABLE) {
//                            if (paymentStatue != CKey.NO_PAYMENT_2) {
//                                billing.setPayStatus(CKey.NO_PAYMENT_2);
//                                billing.save();
//                                //********
//                                // send email disable
//                                //********
//                                customer.setStatus(CKey.DISABLE);
//                                customer.save();
//                                Billing.paymentloginfo("updateUserBilling", "***Disable user " + username + ", billing id" + billing.getBillingId());
//
//                                //clear the billing status.
////                                billing.setStatus(CKey.DISABLE);
//                                billing.save();
//
//                                String msg = "The username had been disabled!\r\nThank you for using IIS.\r\n\r\n";
//                                com.afund.message.emailsystem.sendEmailRegistrationError(username, "", email, msg);
//
//                            }
//                        }
//                    } else if (date.getTime() > entryDate.getTime()) {
//                        if (paymentStatue != CKey.NO_PAYMENT_1) {
//                            billing.setPayStatus(CKey.NO_PAYMENT_1);
//                            billing.save();
//                            // send email reminder
//                            String msg = Billing.getReminderMessage(username, billing.getBillingId(), fPayment);
//                            com.afund.message.emailsystem.sendEmailPayment(username, "", email, msg);
//                        }
//                    }
//                    return CKey.WS_SUCCESS;
//                }
//            } else if (status == CKey.COMPLETE) {
//                Date expireDate = billing.getExpiryDate();
//                Billcycle = expireDate;
//                long date3day = TimeConvertion.addDays(date.getTime(), 3);
//
////                if (date.getTime() < expireDate.getTime()) {
//                if (date3day < expireDate.getTime()) {
//                    // Not yet expire
//                    if (customer.getStatus() == CKey.DISABLE) {
//                        customer.setStatus(CKey.ENABLE);
//                        customer.save();
//                    }
//                    return CKey.WS_SUCCESS;
//                }
//            }
//
//        }
//
//        customer.setStatus(CKey.ENABLE);
//        customer.save();
//
//        // need to create bill payment
//        int type = customer.getType();
//        int pendingType = customer.getpType();
//
//        float fInvoice = CKey.FEATURE_BASIC;
//        float fInvoice1 = 0;
//        int feature = CKey.F_TYPE_BASIC;
//
//        float fCredit = 0;
//
//        // Update pending service plan 
//        if (pendingType != 0) {
//            if (pendingType != type) {
//                type = pendingType;
//                customer.setType(type);
//            }
//        }
//        //only change once
//        customer.setpType(0); // reset pending flag
//        customer.save();
//
//        switch (type) {
//            case CKey.INT_CLIENT_SILVER_USER:
//                fInvoice = CKey.FEATURE_SILVER;
//                feature = CKey.F_TYPE_BASIC_SILVER;
//
//                break;
//            case CKey.INT_CLIENT_GOLD_USER:
//                fInvoice = CKey.FEATURE_GOLD;
//                feature = CKey.F_TYPE_BASIC_GOLD;
//
//                break;
//            case CKey.INT_API_USER:
//                fInvoice = CKey.FEATURE_API;
//                feature = CKey.F_TYPE_BASIC_API;
//                break;
//            case CKey.INT_FUND_USER:
//                fInvoice = CKey.FEATURE_FUNDMGR;
//                feature = CKey.F_TYPE_BASIC_FUND;
//                break;
//
////            case CKey.INT_ADMIN_USER:
////            default:
////                return CKey.WS_SUCCESS;
//        }

//        return processBilling(customer, feature, fInvoice, fInvoice1, fCredit, Billcycle);
        return "";
    }

//    private String processBilling(AFcustomer customer, int feature, float fInvoice, float fInvoice1, float fCredit, Date Billcycle) {
//
//        String username = customer.getUserName();
//
//        try {
//
//            String mirrorAccountName = SQLObject.getMirrorAccountFromUsername(username);
//            if (mirrorAccountName != null) {
//                AFtradingAccount mirrorAccount = TradingAccount.getTradingAccount(mirrorAccountName);
//                if (mirrorAccount != null) {
//                    String mirrorUsername = mirrorAccount.getaFcustomer().getUserName();
//                    String reference = mirrorUsername;
//
//                    AFtradingOpt mirrorTradeOption = TradingAccount.getAccountTradeOption(mirrorAccount);
//                    float fee = mirrorTradeOption.getMirrorAccountFee();
//
//                    float fFeatureInvoice1 = CKey.FEATURE_MIRROR_FUND;
//
//                    if (fee == -1) {
//                        fFeatureInvoice1 = 0;
//                    } else if (fee == 0) {
//                        fFeatureInvoice1 = CKey.FEATURE_MIRROR_FUND;
//                    } else {
//                        fFeatureInvoice1 = fee;
//                    }
//                    int iBillingType = CKey.BILLING_SERVICE;
//                    int iPaymentType = CKey.PAYMENT_ADJUST;
//
//                    String sentMsg = "";
//                    String ret = AcceptUserInvoice(username, iBillingType, reference, iPaymentType, fFeatureInvoice1, sentMsg, Billcycle);
//                    if (ret.equals(CKey.WS_FAIL)) {
//                        System.out.println("processBilling cannot set AcceptUserInvoice");
//                    }
//                    //need to clear the mirror list for the username account
//                    String AccountName = SQLObject.getAccountFromMirrorAccount(username, mirrorAccountName);
//                    AFtradingAccount tradingAccount = TradingAccount.getTradingAccount(AccountName);
//
//                    if (tradingAccount != null) {
//                        AFtradingOpt tradeOption = TradingAccount.getAccountTradeOption(tradingAccount);
//                        tradeOption.setCurrMirrorList(mirrorAccountName);
//                        tradeOption.save();
//                    }
//                }
//
//            }
//
//
//
//            BillingInfo serviceBilling = Billing.getAllServiceAfterMTMBilling(username);
//            fInvoice1 += serviceBilling.getInvoice1();
//            fCredit += serviceBilling.getCredit();
//
//            BillingInfo systemBilling = Billing.getAllSystemAfterMTMBilling(username);
//            fInvoice1 += systemBilling.getInvoice1();
//            fCredit += systemBilling.getCredit();
//
//            PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//
//            try {
//                String billingId = "" + CKey.getUniqueId();
//                //if (lastBill.length == 1) {
//                AFbilling payment = new AFbilling();
//
//                payment.setBillingId(billingId);
//                payment.setType(CKey.BILLING_MONTHLY); //BILLING_MONTHLY
//                payment.setMethod(CKey.PAYMENT_INIT); //PAYMENT_PAY_PAL
//                payment.setFeature(feature);
//                payment.setEntryDate(Billcycle);
//                payment.setEntryDateValue("" + Billcycle.getTime());
//                long dateValue = TimeConvertion.getNextMonth(Billcycle.getTime());
//                payment.setExpiryDate(new java.sql.Date(dateValue));
//                payment.setExpiryDateValue("" + dateValue);
//                payment.setReference(" ");
//                payment.setnStock(0);
//
//                if (customer.getType() == CKey.INT_API_USER) {
//                    int lastMonthNStock = 0;
//                    int currentNStock = 0;
//                    String[] auditS = SQLObject.getLatestAuditStock(customer.getUserName());
//
//                    if (auditS != null) {
//                        if (auditS.length > 3) {
//
//                            int id = Integer.parseInt(auditS[1]);
//                            // get current stock holding - add stock will update this field
//                            currentNStock = Integer.parseInt(auditS[3]);
//
//                            long cur = TimeConvertion.getCurrentTimeStamp().getTime();
//                            long prev = TimeConvertion.getPreviousMonth(cur);
//                            // get number of stock that is added between the current time Bill cycle and 1 month ago
//                            // total number of stock that had been added
//                            String[] lastMonthauditS = SQLObject.getLatestMonthAuditStock(customer.getUserName(), cur, prev);
//                            if (lastMonthauditS != null) {
//                                lastMonthNStock = lastMonthauditS.length - 1;
//                            }
//                        }
//                    }
//
//                    int nStock = currentNStock;
//                    if (lastMonthNStock > currentNStock) {
//                        nStock = lastMonthNStock;
//                    }
//                    payment.setnStock(nStock);
//                    float apiInvoice = BusinessRule.businessgetAPIinvoice(currentNStock, lastMonthNStock);
//                    applog.debugLog("updateUserBillingAll", "API user invoice = " + apiInvoice + ", c=" + currentNStock + ", l=" + lastMonthNStock);
//
//                    fInvoice += apiInvoice;
//                }
//
//                String employee = customer.getEmployeeNo();
//                if (employee != null) {
//                    if (employee.equals("1")) {
//                        fCredit = fInvoice + fInvoice1;
//                    }
//                }
//
//                // need to clear the ending???????????
//                fInvoice = (float) ((int) (fInvoice * 1000 + 5) / 10) / 100;
//                fInvoice1 = (float) ((int) (fInvoice1 * 1000 + 5) / 10) / 100;
//                fCredit = (float) ((int) (fCredit * 1000 + 5) / 10) / 100;
//
//                payment.setStatus(CKey.INITIAL);
//                payment.setPayStatus(CKey.INITIAL);
//                payment.setInvoice(fInvoice);
//                payment.setInvoice1(fInvoice1); // feature
//                payment.setCredit(fCredit);
//
//                payment.setBalance(0);
//                payment.setComment("");
//                payment.setaFcustomer(customer);
//
//
//                float userBalance = customer.getBalance();
//                if (userBalance < 0) {
//                    Billing.paymentloginfo("updateUserBilling", "***user " + username + ", userBalance " + userBalance);
//
//                    fInvoice1 = fInvoice1 - userBalance; // add to invoice for outstanding amount
//                    payment.setInvoice1(fInvoice1);
//
//                    userBalance = 0;
//                    customer.setBalance(userBalance);
//                    customer.save();
//                }
//
//                if ((fInvoice + fInvoice1) > 0) {
//
//                    if ((fInvoice + fInvoice1) > fCredit) {
//                        //userBalance can only be 0 or > 0
//                        if (userBalance > 0) {
//                            Billing.paymentloginfo("updateUserBilling", "***user " + username + ", userBalance " + userBalance);
//                            float paymentBalance = ((fInvoice + fInvoice1) - fCredit);
//                            if (userBalance >= paymentBalance) {
//                                //deduct from userbalance
//                                userBalance = userBalance - paymentBalance;
//                                fCredit = paymentBalance + fCredit;
//                            } else {
//                                // used up all the user balance into credit
//                                fCredit = userBalance + fCredit;
//                                userBalance = 0;
//                            }
//                            payment.setCredit(fCredit);
//                            customer.setBalance(userBalance);
//                            customer.save();
//
//                        }
//                    }
//
//                    // business rule - if < $1
//                    if (((fInvoice + fInvoice1) - fCredit) < 1) {
//                        fCredit = (fInvoice + fInvoice1);
//                        payment.setCredit(fCredit);
//                    }
//
//                    if ((fInvoice + fInvoice1) <= fCredit) {
//                        // no payment 
//                        payment.setStatus(CKey.COMPLETE);
//                        payment.setMethod(CKey.PAYMENT_CREDIT);
//                    } else { // send email for the payment request
//                        float fBalance = (fInvoice + fInvoice1) - fCredit;
//                        String paymentMsg = Billing.getPaymentMessage(username, billingId, fBalance);
//                        String emailAddr = customer.getEmail();
//                        String firstname = "";
//                        com.afund.message.emailsystem.sendEmailPayment(username, firstname, emailAddr, paymentMsg);
//
//                    }
//                }
//                payment.save();
//
//                t.commit();
//
//                Billing.auditlog("updateUserBilling", username, payment);
//
//                return CKey.WS_SUCCESS;
//
//            } catch (Exception e) {
//                applog.debugLog("updateUserBilling exception", "" + username);
//                t.rollback();
//            }
//
//        } catch (PersistentException ex) {
//            ex.printStackTrace();
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            return CKey.WS_FAIL;
//        }
//        return CKey.WS_SUCCESS;
//    }
    private String completeBilling(String username, String billingId, float fPayment) {
        String reference = "0"; // admin user ID
        int iPaymentType = 0; //CKey.PAYMENT_PAY_PAL;
        String sentMsg = "";
        String ret = AcceptUserPayment(username, billingId, reference, iPaymentType, fPayment, sentMsg);
        return ret;
    }

    public String updateUserPayment(String username, String billingId, int iBillingType, int feature, float fInvoice, float fInvoice1, float fCredit, String sentMsg) {
//        applog.debugLog("updateUserPayment", " " + username + " - " + feature + " - " + fInvoice + " - " + fInvoice1 + " - " + fCredit);
//
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = new AFbilling();
//
//                    payment.setBillingId(billingId);
//                    payment.setType(iBillingType);
//                    payment.setMethod(CKey.PAYMENT_INIT); //PAYMENT_PAY_PAL
//                    payment.setFeature(feature);
//                    payment.setEntryDate(date);
//                    payment.setEntryDateValue("" + date.getTime());
//                    long dateValue = TimeConvertion.getNextMonth(currentDate.getTime());
//                    payment.setExpiryDate(new java.sql.Date(dateValue));
//                    payment.setExpiryDateValue("" + dateValue);
//                    // businessRule
//
//                    payment.setReference(" ");
//
//                    payment.setStatus(CKey.INITIAL);
//                    payment.setPayStatus(CKey.INITIAL);
//                    payment.setInvoice(fInvoice);
//                    payment.setInvoice1(fInvoice1);
//
//                    payment.setCredit(fCredit);
//
//                    payment.setBalance(0);
//                    payment.setComment("");
//                    payment.setaFcustomer(customer);
//
//                    if ((fInvoice + fInvoice1) > 0) {
//                        if ((fInvoice + fInvoice1) <= fCredit) {
//                            // no payment 
//                            payment.setStatus(CKey.COMPLETE);
//                            payment.setMethod(CKey.PAYMENT_CREDIT);
//
//                            customer.setStatus(CKey.ENABLE);
//                            customer.save();
//                        }
//                    } else {//if (fInvoice == 0) {
//                        // no payment 
//                        payment.setStatus(CKey.COMPLETE);
//                        payment.setMethod(CKey.PAYMENT_CREDIT);
//
//                        customer.setStatus(CKey.ENABLE);
//                        customer.save();
//                    }
//                    payment.setnStock(0);
//                    payment.save();
//                    t.commit();
//
//                    Billing.auditlog("updateUserPayment", username, payment);
//
//                    // send email payment
//                    String emailAddr = customer.getEmail();
//                    String firstname = "";
//                    if (customer.getaFcustInfo() != null) {
//                        firstname = customer.getaFcustInfo().getFirstName();
//                    }
//                    if (emailAddr.length() > 0) {
//                        String paymentMsg = sentMsg;
//                        if (paymentMsg != null) {
//                            float fBalance = fInvoice - fCredit;
//
//                            if (paymentMsg.length() == 0) {
//                                paymentMsg = Billing.getPaymentMessage(username, billingId, fBalance);
//                            }
//
//                            com.afund.message.emailsystem.sendEmailPayment(username, firstname, emailAddr, paymentMsg);
//                        }
//                    }
//
//                    return CKey.WS_SUCCESS;
//                } catch (Exception e) {
//                    t.rollback();
//                }
//
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }

        return "";
    }

    public String updateUserAdjustment(String username, String billingId, int iBillingType, int feature, float fInvoice, float fInvoice1, float fCredit, String sentMsg) {
//        applog.debugLog("updateUserAdjustment", " " + username + " - " + feature + " - " + fInvoice + " - " + fInvoice1 + " - " + fCredit);
//
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = new AFbilling();
//
//                    payment.setBillingId(billingId);
//                    payment.setType(iBillingType);
//                    payment.setMethod(CKey.PAYMENT_ADJUST);
//                    payment.setFeature(feature);
//                    payment.setEntryDate(date);
//                    payment.setEntryDateValue("" + date.getTime());
//                    long dateValue = TimeConvertion.getNextMonth(currentDate.getTime());
//                    payment.setExpiryDate(new java.sql.Date(dateValue));
//                    payment.setExpiryDateValue("" + dateValue);
//
//                    payment.setReference(" ");
//
//                    payment.setStatus(CKey.COMPLETE);
//                    payment.setPayStatus(CKey.COMPLETE);
//                    payment.setInvoice(fInvoice);
//                    payment.setInvoice1(fInvoice1);
//
//                    payment.setCredit(fCredit);
//
//                    payment.setBalance(0);
//                    payment.setComment("");
//                    payment.setaFcustomer(customer);
//
//                    payment.setnStock(0);
//                    payment.save();
//                    t.commit();
//
//                    Billing.auditlog("updateUserPayment", username, payment);
//
//
//                    return CKey.WS_SUCCESS;
//                } catch (Exception e) {
//                    t.rollback();
//                }
//
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }
//
//        return CKey.WS_FAIL;
        return "";
    }

    public String getUserPayment(String username, String billingId) {
//        applog.debugLog("getUserPayment", " " + username + " - " + billingId);
//        String ret = "";
//        if (billingId.length() == 0) {
//            return CKey.WS_BILL_INVALID_ID_MSG;
//        }
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        if (customer != null) {
//            AFbilling payment = null;
//            try {
//                payment = AFbillingFactory.loadAFbillingByQuery("billingId='" + billingId + "'", null);
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            if (payment != null) {
//                payment.refresh();
//            }
//            if (payment == null) {
//                return CKey.WS_BILL_INVALID_ID_MSG;
//            } else {
//                ret = ""
//                        + "BillingId - " + payment.getBillingId()
//                        + ", method - " + payment.getMethod()
//                        + ", type - " + payment.getType()
//                        + ", entryDate - " + payment.getEntryDate().toString()
//                        + "\nstatus - " + payment.getStatus()
//                        + ", RC - " + payment.getFeature()
//                        + ", payStatus - " + payment.getPayStatus()
//                        + ", invoice - " + payment.getInvoice()
//                        + ", invoice1 - " + payment.getInvoice1()
//                        + ", credit - " + payment.getCredit()
//                        + ", balance - " + payment.getBalance()
//                        + ", nStock - " + payment.getnStock()
//                        + "";
//
//            }
//        }
//        return ret;
        return "";
    }

    public String AcceptUserPayment(String username, String billingId, String reference, int iPaymentType, float fPayment, String sentMsg) {
//        applog.debugLog("AcceptUserPayment", " " + username + " - " + billingId + " - " + reference + " - " + iPaymentType + " - " + fPayment);
//
//        if (billingId.length() == 0) {
//            return CKey.WS_BILL_INVALID_ID_MSG;
//        }
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = null;
//
//                    payment = AFbillingFactory.loadAFbillingByQuery("billingId='" + billingId + "'", null);
//                    if (payment != null) {
//                        payment.refresh();
//                    }
//
//                    if (payment == null) {
//                        Billing.paymentloginfo("AcceptUserPayment", "***Fail to find billing id" + billingId + " - " + iPaymentType + " - " + fPayment);
//                        return CKey.WS_BILL_INVALID_ID_MSG;
//                    } else {
//                        payment.setMethod(iPaymentType); //PAYMENT_PAY_PAL
//                        payment.setReference(reference);
//                        if (payment.getStatus() == CKey.COMPLETE) {
//                            Billing.paymentloginfo("AcceptUserPayment", "***Billing Status invalid for billing id " + billingId + " - " + payment.getStatus() + " - " + iPaymentType + " - " + fPayment);
//                            return CKey.WS_BILL_PROCESSED_MSG;
//                            // Already paid and we should not allow this
//                        }
//                        float invoice = payment.getInvoice();
//                        float invoice1 = payment.getInvoice1();
//                        float credit = payment.getCredit();
//                        float balance = payment.getBalance();
//
//                        // need to clear the ending???????????
//                        invoice = (float) ((int) (invoice * 1000 + 5) / 10) / 100;
//                        invoice1 = (float) ((int) (invoice1 * 1000 + 5) / 10) / 100;
//                        credit = (float) ((int) (credit * 1000 + 5) / 10) / 100;
//
//                        balance += fPayment;
//
//                        if (((invoice + invoice1) - credit) > balance) {
//                            Billing.paymentloginfo("AcceptUserPayment", "***Invoice greater than Payment for billing id " + billingId + " - " + iPaymentType + " - " + fPayment);
//                            float paymentinvoice = ((invoice + invoice1) - credit) - balance;
//                            if (customer.getBalance() > paymentinvoice) {
//                                paymentinvoice = customer.getBalance() - paymentinvoice;
//                                paymentinvoice = (float) ((int) (paymentinvoice * 100)) / 100;
//                                customer.setBalance(paymentinvoice);
//                                customer.save();
//                                payment.setStatus(CKey.COMPLETE);
//                            }
//                        }
//
//                        if (((invoice + invoice1) - credit) <= balance) {
//                            float paymentCredit = balance - ((invoice + invoice1) - credit);
//                            int ipaymentCredit = (int) paymentCredit;
//                            // if greater than a dollar
//                            if (ipaymentCredit > 1) {
//                                Billing.paymentloginfo("AcceptUserPayment", "***Payment greater than Invoice for billing id " + billingId + " - " + iPaymentType + " - " + fPayment);
//                                paymentCredit = customer.getBalance() + paymentCredit;
//                                paymentCredit = (float) ((int) (paymentCredit * 100)) / 100;
//
//                                customer.setBalance(paymentCredit);
//                                customer.save();
//                            }
//
//                            payment.setStatus(CKey.COMPLETE);
//                        }
//
//                        payment.setBalance(balance);
//                        Billing.auditlog("AcceptUserPayment", username, payment);
//
//                        int customerStaus = customer.getStatus();
//                        String newCustomerMsg = "";
//                        if (customerStaus != CKey.ENABLE) {
//                            newCustomerMsg = "\r\nYour account setup is in progress. Please wait for few minitues before login....\r\n";
//                        }
//
//                        customer.setStatus(CKey.ENABLE);
//                        customer.save();
//
//                        payment.save();
//                        t.commit();
//
//                        String emailAddr = customer.getEmail();
//                        String firstname = "";
//                        if (customer.getaFcustInfo() != null) {
//                            firstname = customer.getaFcustInfo().getFirstName();
//                        }
//                        if (emailAddr.length() > 0) {
//                            String paymentMsg = "\r\nThank you for your payment (id " + billingId + ") of the amount $" + fPayment + "\r\n\r\n";
//                            if (sentMsg != null) {
//                                if (sentMsg.length() == 0) {
//                                    paymentMsg += sentMsg;
//                                }
//                            }
//                            paymentMsg += newCustomerMsg;
//                            com.afund.message.emailsystem.sendEmailPayment(username, firstname, emailAddr, paymentMsg);
//
//                        }
//
//                        return CKey.WS_SUCCESS;
//                    }
//                } catch (Exception e) {
//                    t.rollback();
//                }
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        return CKey.WS_FAIL;
        return "";
    }

    public String AcceptUserBalance(String username, String reference, int iPaymentType, float fCredit, String sentMsg) {
//        applog.debugLog("AcceptUserCredit", " " + username + " - " + reference + " - " + iPaymentType + " - " + fCredit);
//
//        String billingId = "" + CKey.getUniqueId();
//        int iBillingType = CKey.BILLING_SYSTEM;
//
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = new AFbilling();
//
//                    payment.setBillingId(billingId);
//                    payment.setType(iBillingType);
//                    payment.setMethod(CKey.PAYMENT_ADJUST);
//                    payment.setFeature(0);
//                    payment.setEntryDate(date);
//                    payment.setEntryDateValue("" + date.getTime());
//                    long dateValue = TimeConvertion.getNextMonth(currentDate.getTime());
//                    payment.setExpiryDate(new java.sql.Date(dateValue));
//                    payment.setExpiryDateValue("" + dateValue);
//
//                    payment.setReference(" ");
//
//                    payment.setStatus(CKey.COMPLETE);
//                    payment.setPayStatus(CKey.COMPLETE);
//                    payment.setInvoice(0);
//                    payment.setInvoice1(0);
//
//                    payment.setCredit(0);
//
//                    payment.setBalance(fCredit);
//                    payment.setComment("");
//                    payment.setaFcustomer(customer);
//
//                    payment.setnStock(0);
//                    payment.save();
//
//                    // fCredit can be postive or negative
//                    float balance = customer.getBalance() + fCredit;
//                    customer.setBalance(balance);
//                    customer.save();
//                    //commit the changes
//                    t.commit();
//
//                    Billing.auditlog("AcceptUserBalance", username, payment);
//                    if (fCredit > 0) { // negative - email not required
//                        String emailAddr = customer.getEmail();
//                        String firstname = "";
//                        if (customer.getaFcustInfo() != null) {
//                            firstname = customer.getaFcustInfo().getFirstName();
//                        }
//                        if (emailAddr.length() > 0) {
//                            String paymentMsg = "\r\nThank you for your payment (id " + billingId + ") of the amount $" + fCredit + "\r\n\r\n";
//                            if (sentMsg != null) {
//                                if (sentMsg.length() == 0) {
//                                    paymentMsg += sentMsg;
//                                }
//                            }
//                            com.afund.message.emailsystem.sendEmailPayment(username, firstname, emailAddr, paymentMsg);
//
//                        }
//                    }
//                    return CKey.WS_SUCCESS;
//                } catch (Exception e) {
//                    t.rollback();
//                }
//
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }
//
//
//
//        return CKey.WS_FAIL;
        return "";
    }
//

    public String AcceptUserInvoice(String username, int iBillingType, String reference, int iPaymentType, float fInvoice1, String sentMsg) {
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//        return AcceptUserInvoice(username, iBillingType, reference, iPaymentType, fInvoice1, sentMsg, date);
//    }
//
//    // always in invoice1 for billing service
//    public String AcceptUserInvoice(String username, int iBillingType, String reference, int iPaymentType, float fInvoice1, String sentMsg, Date date) {
//        applog.debugLog("AcceptUserCredit", " " + username + " - " + reference + " - " + iPaymentType + " - " + fInvoice1);
//
//        String billingId = "" + CKey.getUniqueId();
////        int iBillingType = CKey.BILLING_SERVICE;
//
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = new AFbilling();
//
//                    payment.setBillingId(billingId);
//                    payment.setType(iBillingType);
//                    payment.setMethod(CKey.PAYMENT_ADJUST);
//                    payment.setFeature(0);
//                    payment.setEntryDate(date);
//                    payment.setEntryDateValue("" + date.getTime());
//                    long dateValue = TimeConvertion.getNextMonth(date.getTime());
//                    payment.setExpiryDate(new java.sql.Date(dateValue));
//                    payment.setExpiryDateValue("" + dateValue);
//
//                    payment.setReference(reference);
//
//                    payment.setStatus(CKey.COMPLETE);
//                    payment.setPayStatus(CKey.COMPLETE);
//                    payment.setInvoice(0);
//                    payment.setInvoice1(fInvoice1); //must use invoice1 field
//
//                    payment.setCredit(0);
//
//                    payment.setBalance(0);
//                    payment.setComment("");
//                    payment.setaFcustomer(customer);
//
//                    payment.setnStock(0);
//                    payment.save();
//                    t.commit();
//                    Billing.auditlog("AcceptUserInvoice", username, payment);
//
//                    return CKey.WS_SUCCESS;
//                } catch (Exception e) {
//                    t.rollback();
//                }
//
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }
//
//        return CKey.WS_FAIL;
        return "";
    }

    public String AcceptUserCredit(String username, String reference, int iPaymentType, float fCredit, String sentMsg) {
//        applog.debugLog("AcceptUserCredit", " " + username + " - " + reference + " - " + iPaymentType + " - " + fCredit);
//
//        String billingId = "" + CKey.getUniqueId();
//        int iBillingType = CKey.BILLING_SERVICE;
//
//        AFcustomer customer = null;
//        try {
//            customer = AFcustomerFactory.loadAFcustomerByQuery("userName='" + username + "'", null);
//        } catch (PersistentException ex) {
//            Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
//        java.sql.Date date = new java.sql.Date(currentDate.getTime());
//
//        if (customer != null) {
//            try {
//                PersistentTransaction t = com.trading.message.StockTradingPersistentManager.instance().getSession().beginTransaction();
//                try {
//                    AFbilling payment = new AFbilling();
//
//                    payment.setBillingId(billingId);
//                    payment.setType(iBillingType);
//                    payment.setMethod(CKey.PAYMENT_ADJUST);
//                    payment.setFeature(0);
//                    payment.setEntryDate(date);
//                    payment.setEntryDateValue("" + date.getTime());
//                    long dateValue = TimeConvertion.getNextMonth(currentDate.getTime());
//                    payment.setExpiryDate(new java.sql.Date(dateValue));
//                    payment.setExpiryDateValue("" + dateValue);
//
//                    payment.setReference(" ");
//
//                    payment.setStatus(CKey.COMPLETE);
//                    payment.setPayStatus(CKey.COMPLETE);
//                    payment.setInvoice(0);
//                    payment.setInvoice1(0);
//
//                    payment.setCredit(fCredit);
//
//                    payment.setBalance(0);
//                    payment.setComment("");
//                    payment.setaFcustomer(customer);
//
//                    payment.setnStock(0);
//                    payment.save();
//                    t.commit();
//                    Billing.auditlog("AcceptUserCredit", username, payment);
//
//                    return CKey.WS_SUCCESS;
//                } catch (Exception e) {
//                    t.rollback();
//                }
//
//            } catch (PersistentException ex) {
//                Logger.getLogger(TradingSystem.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }
//
//        return CKey.WS_FAIL;
        return "";
    }

}
