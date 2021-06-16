/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processaccounting;

import com.afweb.accprocess.*;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class AccountingService {
        protected static Logger logger = Logger.getLogger("AccountingService");

        public int updateAccountingEntryPaymentBalance(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String balanceSt,
            String reasonSt, String rateSt, String yearSt, String commentSt) {
        ServiceAFweb.lastfun = "updateAccountingPaymentBalance";
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.getAccountImp().getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
            }
            String comment = "";
            if (commentSt != null) {
                comment = commentSt;
            }

            float payment = 0;
            String commSt = "";
            int ret = 0;
            if (paymentSt != null) {
                if (!paymentSt.equals("")) {
                    payment = Float.parseFloat(paymentSt);
                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(payment);
                    commSt += "System expense change " + currency;

                    ////////update accounting entry
                    String entryName = "";
                    if (reasonSt != null) {
                        if (reasonSt.length() > 0) {
                            entryName = reasonSt;
                        }
                    }
                    if (comment.length() > 0) {
                        commSt = comment;
                    }
                    float rate = 100;
                    if (rateSt != null) {
                        if (rateSt.length() > 0) {
                            try {
                                rate = Float.parseFloat(rateSt);
                            } catch (Exception e) {
                            }
                        }
                    }
                    int year = 0;
                    if (yearSt != null) {
                        if (yearSt.length() > 0) {
                            try {
                                year = Integer.parseInt(yearSt);
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (entryName.equals(AccountingProcess.E_USER_WITHDRAWAL)) {
                        ret = serviceAFWeb.getAccounting().addTransferWithDrawRevenueTax(serviceAFWeb, customer, payment, year, entryName + " " + commSt);
                    } else {
                        ret = serviceAFWeb.getAccounting().addTransferExpenseTax(serviceAFWeb, customer, payment, rate, year, commSt);
                    }
                }
            }
            float balance = 0;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(balance);

                    commSt += "System revenue change " + currency;

                    ////////update accounting entry
                    String entryName = "";
                    if (reasonSt != null) {
                        if (reasonSt.length() > 0) {
                            entryName = reasonSt;
                        }
                    }

                    if (comment.length() > 0) {
                        commSt = comment;
                    }
                    int year = 0;
                    if (yearSt != null) {
                        if (yearSt.length() > 0) {
                            try {
                                year = Integer.parseInt(yearSt);
                            } catch (Exception e) {
                            }
                        }
                    }
                    ret = serviceAFWeb.getAccounting().addTransferRevenueTax(serviceAFWeb, customer, balance, year, commSt);

                }
            }
            if (ret == 1) {
                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTtime = format.format(d);

                String msg = ESTtime + " " + commSt;

                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                serviceAFWeb.getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    //http://localhost:8080/cust/admin1/sys/cust/eddy/update?substatus=10&investment=0&balance=15&?reason=
    public int updateAddCustStatusPaymentBalance(ServiceAFweb serviceAFWeb, String customername,
            String statusSt, String paymentSt, String balanceSt, String yearSt, String reasonSt) {
        if (serviceAFWeb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.getAccountImp().getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
            }
            ArrayList accountList = serviceAFWeb.getAccountList(UserName, null);

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
            String emailSt = "";
            int status = -9999;
            if (statusSt != null) {
                if (!statusSt.equals("")) {
                    status = Integer.parseInt(statusSt);
                    String st = "Disabled";
                    if (status == ConstantKey.OPEN) {
                        st = "Enabled";
                    }
                    emailSt += "\n\r " + customername + " Accout Status change to " + st;
                }
            }
            float payment = -9999;
            if (paymentSt != null) {
                if (!paymentSt.equals("")) {
                    payment = Float.parseFloat(paymentSt);
                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(payment);
                    emailSt += "\n\r " + customername + " Accout invoice bill adjust " + currency;

                }
            }
            float balance = -9999;
            if (balanceSt != null) {
                if (!balanceSt.equals("")) {
                    balance = Float.parseFloat(balanceSt);

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
                    String currency = formatter.format(balance);
                    emailSt += "\n\r " + customername + " Accout balance adjust " + currency;

                    ////////update accounting entry
                    String entryName = "";
                    if (reasonSt != null) {
                        if (reasonSt.length() > 0) {
                            entryName = reasonSt;
                        }
                    }
                    if (customer != null) {
                        boolean byPassPayment = BillingProcess.isSystemAccount(customer);
                        if (byPassPayment == false) {
                            int year = 0;
                            if (yearSt != null) {
                                if (yearSt.length() > 0) {
                                    try {
                                        year = Integer.parseInt(yearSt);
                                    } catch (Exception e) {
                                    }
                                }
                            }

                            if (entryName.equals(AccountingProcess.E_USER_WITHDRAWAL)) {
                                // UI will set payment to negative 
                                float withdraw = -balance;
                                int ret = serviceAFWeb.getAccounting().addTransferWithDrawRevenueTax(serviceAFWeb, customer, withdraw, year, entryName + " " + emailSt);
                            } else if (entryName.equals(AccountingProcess.R_USER_PAYMENT)) {
                                int ret = serviceAFWeb.getAccounting().addTransferRevenueTax(serviceAFWeb, customer, balance, year, emailSt);
                            }
                        }
                    }

                }
            }
            int ret = serviceAFWeb.getAccountImp().updateAddCustStatusPaymentBalance(UserName, status, payment, balance);
            if (ret == 1) {
                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                java.sql.Date d = new java.sql.Date(TimeConvertion.currentTimeMillis());
//                                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                DateFormat format = new SimpleDateFormat(" hh:mm a");
                format.setTimeZone(tz);
                String ESTtime = format.format(d);

                String msg = ESTtime + " " + emailSt;

                serviceAFWeb.getAccountImp().addAccountMessage(accountObj, ConstantKey.ACCT_TRAN, msg);
                AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
                serviceAFWeb.getAccountImp().addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

                // send email
                DateFormat formatD = new SimpleDateFormat("M/dd/yyyy hh:mm a");
                formatD.setTimeZone(tz);
                String ESTdateD = formatD.format(d);
                String msgD = ESTdateD + " " + emailSt;
                serviceAFWeb.getAccountImp().addAccountEmailMessage(accountObj, ConstantKey.ACCT_TRAN, msgD);

            }
            return ret;

        } catch (Exception e) {
        }
        return 0;
    }

}
