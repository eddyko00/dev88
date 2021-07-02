/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.processaccounting;

import com.afweb.dbaccount.AccountImp;
import com.afweb.model.*;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;
import com.afweb.util.*;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 *
 * @author koed
 */
public class AccountingService {

    protected static Logger logger = Logger.getLogger("AccountingService");
    private AccountingProcess accounting = new AccountingProcess();
    AccountImp accountImp = new AccountImp();
    
    public int updateAccountingEntryPaymentBalance(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String balanceSt,
            String reasonSt, String rateSt, String yearSt, String commentSt) {
        ServiceAFweb.lastfun = "updateAccountingPaymentBalance";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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
                        ret = accounting.addTransferWithDrawRevenueTax(serviceAFWeb, customer, payment, year, entryName + " " + commSt);
                    } else {
                        ret = accounting.addTransferExpenseTax(serviceAFWeb, customer, payment, rate, year, commSt);
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
                    ret = accounting.addTransferRevenueTax(serviceAFWeb, customer, balance, year, commSt);

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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public int insertAccountTAX(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
        ServiceAFweb.lastfun = "insertAccountTAX";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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
                    commSt += "System TAX change " + currency;

//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//
//                        }
//                    }
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
                    ret = accounting.addTransferPayTax(serviceAFWeb, customer, payment, commSt);

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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public int insertAccountCash(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
        ServiceAFweb.lastfun = "insertAccountCash";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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
                    commSt += "System Cash change " + currency;

//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//
//                        }
//                    }
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
                    ret = accounting.addTransferCash(serviceAFWeb, customer, payment, year, commSt);

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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public int insertAccountEarning(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String reasonSt, String yearSt, String commentSt) {
        ServiceAFweb.lastfun = "insertAccountEarning";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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
                    commSt += "System Retained Earning change " + currency;

//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//
//                        }
//                    }
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
                    ret = accounting.addTransferEarning(serviceAFWeb, customer, payment, year, commSt);

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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public int removeAccounting(ServiceAFweb serviceAFWeb, String customername, String yearSt) {
        ServiceAFweb.lastfun = "removeAccounting";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
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
            if (year == -99) {
                return accountImp.removeAccountingAll();
            }

            int newYear = 0;
            if (year != 0) {
                newYear = year * 12;
            }

            // begin 2021 01 01  (updatedatel)  end 2021 12 31
            long BeginingYear = DateUtil.getFirstDayCurrentYear();
            long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);

            if (newYear != 0) {
                BeginingYear = TimeConvertion.addMonths(BeginingYear, newYear);
                EndingYear = TimeConvertion.addMonths(EndingYear, newYear);
            }

            EndingYear = TimeConvertion.addDays(EndingYear, -1);
            return accountImp.removeAccounting(BeginingYear, EndingYear);
        } catch (Exception e) {
        }
        return 0;
    }

    public int AccountingYearEnd(ServiceAFweb serviceAFWeb, String customername, String yearSt) {
        ServiceAFweb.lastfun = "insertAccountEarning";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
            if (customer == null) {
                return 0;
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

            return accounting.closingYearEnd(serviceAFWeb, customer, year);

        } catch (Exception e) {
        }
        return 0;
    }

    public int updateAccountingExDeprecation(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String rateSt, String reasonSt, String commentSt) {
        ServiceAFweb.lastfun = "updateAccountingExDeprecation";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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

//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//
//                        }
//                    }
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
                    ret = accounting.addTransferDepreciation(serviceAFWeb, customer, payment, rate, commSt);
                    ret = 1;
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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public int updateAccountingExUtility(ServiceAFweb serviceAFWeb, String customername, String paymentSt, String yearSt, String reasonSt, String commentSt) {
        ServiceAFweb.lastfun = "updateAccountingExUtility";
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        customername = customername.toUpperCase();
        NameObj nameObj = new NameObj(customername);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = accountImp.getCustomerPasswordNull(UserName);
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

//                    if (reasonSt != null) {
//                        if (reasonSt.length() > 0) {
//
//                        }
//                    }
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
                    ret = accounting.addTransferUtilityExpense(serviceAFWeb, customer, payment, year, commSt);

                    ret = 1;
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
                accountImp.addAccountMessage(accountAdminObj, ConstantKey.ACCT_TRAN, msg);

            }
            return ret;

        } catch (Exception e) {

        }
        return 0;
    }

    public AccReportObj getAccountingReportByCustomerByName(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String name, int year, String namerptSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.SysGetCustomerPassword(UserName, Password);
            if (customer != null) {
                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {

                    if (name != null) {
                        if (name.length() > 0) {
                            AccReportObj accReport = accounting.getAccountReportYearByName(serviceAFWeb, name, year);
                            return accReport;
                        }
                    }
                    String namerpt = "income";
                    if (namerptSt != null) {
                        if (namerptSt.length() > 0) {
                            namerpt = namerptSt;
                        }
                    }
                    AccountingProcess accProc = new AccountingProcess();
                    AccReportObj accReport = null;
                    if (namerpt.equals("balance")) {
                        accReport = accProc.getAccountBalanceReportYear(serviceAFWeb, year, namerptSt);
                    } else if (namerpt.equals("deprecation")) {
                        accReport = accProc.getAccountBusinessReportYear(serviceAFWeb, year, namerptSt);
                    } else {
                        accReport = accProc.getAccountReportYear(serviceAFWeb, year, namerptSt);
                    }
                    return accReport;
                }
            }

        } catch (Exception e) {
        }
        return null;
    }

    public AccEntryObj getAccountingEntryByCustomerById(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String idSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return null;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.SysGetCustomerPassword(UserName, Password);
            if (customer != null) {
                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {
                    int id = Integer.parseInt(idSt);

                    AccEntryObj accEntry = accounting.getAccountingEntryById(serviceAFWeb, id);
                    return accEntry;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public int removeAccountingEntryById(ServiceAFweb serviceAFWeb, String EmailUserName, String Password, String idSt) {
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            return 0;
        }

        NameObj nameObj = new NameObj(EmailUserName);
        String UserName = nameObj.getNormalizeName();
        try {
            CustomerObj customer = serviceAFWeb.SysGetCustomerPassword(UserName, Password);
            if (customer != null) {
                if (customer.getUsername().equals(CKey.ADMIN_USERNAME)) {
                    int id = Integer.parseInt(idSt);
                    return accounting.removeAccountingEntryById(serviceAFWeb, id);

                }
            }
        } catch (Exception e) {
        }
        return 0;
    }

}
