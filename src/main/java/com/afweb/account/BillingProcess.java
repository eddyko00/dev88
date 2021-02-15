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

    public static final int NO_PAYMENT_1 = 55;
    public static final int NO_PAYMENT_2 = 56;
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
                if (custNameList != null) {
                    if (custNameList.size() == 0) {

                        CustomerObj customer = (CustomerObj) custNameList.get(0);
                        if (customer.getType() != CustomerObj.INT_ADMIN_USER) {
                            this.updateUserBilling(serviceAFWeb, customer);
                        }
                    }
                }
                stockNNprocessNameArray.remove(0);

            }  // end for loop
            serviceAFWeb.removeNameLock(LockName, ConstantKey.NN_LOCKTYPE);
//            logger.info("ProcessTrainNeuralNet " + LockName + " unlock LockName");
        }
        logger.info("> updateUserBillingAll ... done");
    }
//

    public int updateUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer) {

        Timestamp currentDate = TimeConvertion.getCurrentTimeStamp();
        Date currDate = new java.sql.Date(currentDate.getTime());
        if (customer == null) {
            return 0;
        }

        AccountObj account = serviceAFWeb.getAccountImp().getAccountByType(customer.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);
        // get last bill
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(customer.getUsername(), null, account.getId());
        if (billingObjList == null) {
            return 0;
        }
        if (billingObjList.size() == 0) {
            // create first bill 
            createUserBilling(serviceAFWeb, customer, null);
            return 1;
        }
        BillingObj billing = billingObjList.get(0);

        // check if expire
        if (currDate.getTime() < billing.getUpdatedatel()) {
            return 0;
        }

        int status = billing.getStatus();

        float userBalance = customer.getBalance();
        float fPayment = customer.getPayment();

        if (status == ConstantKey.INITIAL) {
            if (userBalance >= fPayment) {
                //the remaining goes to the next invoice.
                userBalance = userBalance - fPayment;
                customer.setBalance(userBalance);
                customer.setPayment(0);

                // transaction
                int result = serviceAFWeb.updateCustAllStatus(customer.getUsername(), null, customer.getPayment() + "", customer.getBalance() + "");
                result = serviceAFWeb.getAccountImp().updateAccountBillingData(billing.getId(), ConstantKey.COMPLETED, fPayment, userBalance, "");
                // transaction

            } else {
//                Date entryDate = billing.getUpdatedatedisplay();
                long billcycleDate = billing.getUpdatedatel();
                long dateWeek = TimeConvertion.nextWeek(billcycleDate);
                int subStatus = billing.getStatus();
                if (billcycleDate > dateWeek) {
                    if (customer.getStatus() != ConstantKey.DISABLE) {
                        if (subStatus != NO_PAYMENT_2) {
                            billing.setSubstatus(NO_PAYMENT_2);

                            customer.setStatus(ConstantKey.DISABLE);
                            int result = serviceAFWeb.updateCustAllStatus(customer.getUsername(), customer.getStatus() + "", null, null);
                            result = serviceAFWeb.getAccountImp().updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());
                            //********
                            // send email disable
                            //********
                            String msg = "The " + customer.getUsername() + " account had been disabled!\r\nThank you for using IIS.\r\n\r\n";

                            logger.info("updateUserBilling***Disable user " + customer.getUsername() + ", billing id" + billing.getId());
                        }
                    }
                } else if (currDate.getTime() > billcycleDate) {
                    if ((subStatus != NO_PAYMENT_1) && (subStatus != NO_PAYMENT_2)) {
                        billing.setStatus(NO_PAYMENT_1);
                        int result = serviceAFWeb.getAccountImp().updateAccountBillingStatus(billing.getId(), billing.getStatus(), billing.getSubstatus());
                        // send email reminder
                        String msg = "The " + customer.getUsername() + " account has past due amount!\r\nPlease submit the payment now.\r\n\r\n";
                    }
                }

            }
        } else if (status == ConstantKey.COMPLETED) {
            // check for next bill
            createUserBilling(serviceAFWeb, customer, billing);
        }
        return 1;
    }

    public int createUserBilling(ServiceAFweb serviceAFWeb, CustomerObj customer, BillingObj billing) {
        if (customer.getType() == CustomerObj.INT_ADMIN_USER) {
            return 1;
        }
        
        AccountObj account = serviceAFWeb.getAccountImp().getAccountByType(customer.getUsername(), null, AccountObj.INT_TRADING_ACCOUNT);

        long billCycleDate = account.getUpdatedatel();
        if (billing != null) {
           long lastBillDate =  billing.getUpdatedatel();
           billCycleDate = TimeConvertion.addMonths(lastBillDate, 1);
        }
        Timestamp cDate = TimeConvertion.getCurrentTimeStamp();
        Date curDate = new java.sql.Date(cDate.getTime());
        long date3day = TimeConvertion.addDays(curDate.getTime(), 3);

        if (date3day > billCycleDate) {
            float payment = customer.getBalance();
            int type = account.getType();
            float fInvoice = 0;
            switch (type) {
                case ConstantKey.INT_PP_BASIC:
                    fInvoice = ConstantKey.INT_PP_BASIC_PRICE;
                    break;
                case ConstantKey.INT_PP_PREMIUM:
                    fInvoice = ConstantKey.INT_PP_REMIUM_PRICE;
                    break;
                case ConstantKey.INT_PP_DELUXE:
                    fInvoice = ConstantKey.INT_PP_DELUXE_PRICE;
                    break;
            }
            // first bill alreay add the payment
            if (billing != null) {
                payment += fInvoice;
            }
            float balance = 0;
            String msg = "";

            customer.setPayment(payment);
            int result = serviceAFWeb.updateCustAllStatus(customer.getUsername(), null, customer.getPayment() + "", null);
            serviceAFWeb.getAccountImp().addAccountBilling(customer.getUsername(), account, payment, balance, msg, billCycleDate);
        }
        return 0;
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

}
