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

        String printName = "";
        for (int i = 0; i < custProcessNameArray.size(); i++) {
            printName += custProcessNameArray.get(i) + ",";
        }
        logger.info("processUserBillingAll " + printName);

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
        logger.info("> updateUserBillingAll ... done");
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
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getBillingByCustomerAccountID(customer.getUsername(), null, account.getId());
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
                result = serviceAFWeb.getAccountImp().updateAccountBillingStatusPaymentData(billing.getId(), billing.getStatus(), billing.getPayment(), billing.getBalance(), "");
                // transaction
                // send email disable
                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                String currency = formatter.format(fPayment);
                msg = "The " + custName + " account payment " + currency + " completed!\r\nThank you.\r\n\r\n";
                sendMsg = true;
                logger.info("Billing***Completed user " + custName + ", billing id " + billing.getId());

            } else {
//                Date entryDate = billing.getUpdatedatedisplay();
                long billcycleDate = billing.getUpdatedatel();
                long dateWeek = TimeConvertion.nextWeek(billcycleDate);
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
                            msg = "The " + custName + " account had been disabled!\r\nThank you for using IIS.\r\n\r\n";
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
                        msg = "The " + custName + " account has past due " + currency + " amount!\r\nPlease submit the payment now.\r\n\r\n";
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
        int retCreatebill = createUserBilling(serviceAFWeb, customer, account, billing);
        if (retCreatebill == 1) {
            // send email reminder            
            msg = "The " + custName + " account billing invoice ready!\r\nPlease submit the payment now.\r\n\r\n";
            sendMsg = true;
        }
        
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

        Timestamp cDate = TimeConvertion.getCurrentTimeStamp();
        Date curDate = new java.sql.Date(cDate.getTime());
        long date3day = TimeConvertion.addDays(curDate.getTime(), 3);

        if (date3day > billCycleDate) {
            float payment = customer.getPayment();
            int subType = account.getSubstatus();
            float fInvoice = 0;
            switch (subType) {
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

            customer.setPayment(payment);
            int result = 0;
            // first bill alreay add the payment
            if (billing != null) {
                result = serviceAFWeb.systemCustStatusPaymentBalance(customer.getUsername(), null, customer.getPayment() + "", null);
            }
            result = serviceAFWeb.getAccountImp().addAccountBilling(customer.getUsername(), account, payment, balance, "", billCycleDate);

            int billId = 0;
            if (billing != null) {
                billId = billing.getId();
            }
            logger.info("Billing***BillingReady user " + customer.getUsername() + ", billing id " + billId + ", payment=" + payment);

            return result;
        }
        return 0;
    }

}
