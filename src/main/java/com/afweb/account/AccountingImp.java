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
import java.text.NumberFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.Locale;

import java.util.logging.Logger;

/**
 *
 * @author eddyko
 */
public class AccountingImp {

    protected static Logger logger = Logger.getLogger("accAPI");

    public static int MAX_SIZE = 100;
// next year    
//https://courses.lumenlearning.com/sac-finaccounting/chapter/journalizing-and-posting-closing-entries/
//    
//https://www.accountingcoach.com/accounting-basics/explanation/5
//Balance Sheet accounts:
//
//Asset accounts (Examples: Cash, Accounts Receivable, Supplies, Equipment)
//Liability accounts (Examples: Notes Payable, Accounts Payable, Wages Payable)
//Stockholders' Equity accounts (Examples: Common Stock, Retained Earnings)
    public static String A_CASH = "cash";
    public static int INT_A_CASH = 10;

    public static String A_ACC_RECEIVABLE = "acc_receivable";
    public static int INT_A_ACC_RECEIVABLE = 11;
    public static String A_ACC_PREPAID = "acc_prepaid";
    public static int INT_A_ACC_PREPAID = 12;

    public static String A_EQUIPMENT = "equipment";
    public static int INT_A_EQUIPMENT = 14;
    public static String L_ACC_PAYABLE = "acc_payable";  // must end with _payable for blance calculation
    public static int INT_L_ACC_PAYABLE = 15;
    public static String L_TAX_PAYABLE = "sale_tax_payable"; // must end with _payable for blance calculation
    public static int INT_L_TAX_PAYABLE = 16;
    public static String E_RET_EARNING = "retained_earnings_lastyear";
    public static int INT_E_RET_EARNING = 17;
//    public static String B_BUSINESS = "profit_loss_acc";

    public static String Asset_accounts[] = {A_CASH, A_ACC_PREPAID, A_ACC_RECEIVABLE, A_EQUIPMENT};
    public static String Liability_accounts[] = {L_ACC_PAYABLE, L_TAX_PAYABLE};
    public static String Equity_accounts[] = {E_RET_EARNING};

    public static int Asset_accountsId[] = {INT_A_CASH, INT_A_ACC_PREPAID, INT_A_ACC_RECEIVABLE, INT_A_EQUIPMENT};
    public static int Liability_accountsId[] = {INT_L_ACC_PAYABLE, INT_L_TAX_PAYABLE};
    public static int Equity_accountsId[] = {};
//    public static String Business_accounts[] = {B_BUSINESS};
    //
    //Revenue accounts (Examples: Service Revenues, Investment Revenues)
    //Expense accounts (Examples: Wages Expense, Rent Expense, Depreciation Expense)
    public static String R_REVENUE = "revenues";
    public static int INT_R_REVENUE = 50;
    public static String R_F_INCOME = "finance_income";
    public static int INT_R_F_INCOME = 51;

    public static String R_SRV_REVENUE = "service_Revenues";
    public static int INT_R_SRV_REVENUE = 52;
    public static String EX_EXPENSE = "expense";
    public static int INT_EX_EXPENSE = 53;
    public static String EX_T50EXPENSE = "non_taxable";// 50% on claim on meals and entertainment
    public static int INT_EX_T50EXPENSE = 54;
    public static String EX_DEPRECIATION = "depreciation";
    public static int INT_EX_DEPRECIATION = 55;
    public static String EX_WAGES = "Wages_expense";
    public static int INT_EX_WAGES = 56;

    public static String Revenue_accounts[] = {R_REVENUE, R_F_INCOME, R_SRV_REVENUE};
    public static String Expense_accounts[] = {EX_EXPENSE, EX_T50EXPENSE, EX_DEPRECIATION, EX_WAGES};

    public static int Revenue_accountsId[] = {INT_R_REVENUE, INT_R_F_INCOME, INT_R_SRV_REVENUE};
    public static int Expense_accountsId[] = {INT_EX_EXPENSE, INT_EX_T50EXPENSE, INT_EX_DEPRECIATION, INT_EX_WAGES};

    public static String YEAR_DEPRECIATION = "yearly_depreciation";
    public static int INT_YEAR_DEPRECIATION = 100;
    public static String YEAR_EXPENSE = "yearly_expense";
    public static int INT_YEAR_EXPENSE = 101;

    public static String depreciation_accounts[] = {YEAR_DEPRECIATION, YEAR_EXPENSE, E_RET_EARNING};
    public static int depreciation_accountsId[] = {INT_YEAR_DEPRECIATION, INT_YEAR_EXPENSE, INT_E_RET_EARNING};

    public static String SYS_TOTAL = "Total";
    public static int INT_SYS_TOTAL = 11;

    public static double GST = 0.13;
//////////////////////////////////////////////////
//////////////////////////////////////////////////    

    public int removeAccountingEntryById(ServiceAFweb serviceAFWeb, int id) {
        return serviceAFWeb.getAccountImp().removeAccountingByTypeId(ConstantKey.INT_ACC_TRAN, id);
    }

    public AccEntryObj getAccountingEntryById(ServiceAFweb serviceAFWeb, int id) {
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getAccountingByTypeId(ConstantKey.INT_ACC_TRAN, id);

        if (billingObjList == null) {
            return null;
        }

        if (billingObjList.size() > 0) {
            BillingObj accTran = billingObjList.get(0);
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(accTran.getId());
            String dateSt = accTran.getUpdatedatedisplay().toString();
            accEntry.setDateSt(dateSt);
            accEntry.setName(accTran.getName());
            accEntry.setDebit(accTran.getPayment());
            accEntry.setCredit(accTran.getBalance());
            accEntry.setComment(accTran.getData());

            return accEntry;
        }
        return null;
    }

    public AccReportObj getAccountReportYearByName(ServiceAFweb serviceAFWeb, String name, int year) {
        int lastYear = 0;
        if (year != 0) {
            lastYear = year * 12;
        }
        AccReportObj reportObj = new AccReportObj();

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        long BeginingYear = DateUtil.getFirstDayCurrentYear();
        long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);

        if (lastYear != 0) {
            BeginingYear = TimeConvertion.addMonths(BeginingYear, lastYear);
            EndingYear = TimeConvertion.addMonths(EndingYear, lastYear);
        }

        EndingYear = TimeConvertion.addDays(EndingYear, -1);

        reportObj.setBeginl(BeginingYear);
        reportObj.setBegindisplay(new java.sql.Date(BeginingYear));
        reportObj.setEndl(EndingYear);
        reportObj.setEnddisplay(new java.sql.Date(EndingYear));

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().
                getAccountingByNameType(name, ConstantKey.INT_ACC_TRAN, BeginingYear, EndingYear, MAX_SIZE);
        if (billingObjList == null) {
            return reportObj;
        }

        ArrayList accTotalEntryBal = new ArrayList();
        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(accTran.getId());
            String dateSt = accTran.getUpdatedatedisplay().toString();
            accEntry.setDateSt(dateSt);
            accEntry.setName(accTran.getName());
            //        billObj.setPayment(debit);
            //        billObj.setBalance(credit);
            accEntry.setDebit(accTran.getPayment());
            accEntry.setCredit(accTran.getBalance());
            accEntry.setComment(accTran.getData());
            accTotalEntryBal.add(accEntry);
        }
        reportObj.setAccEntryBal(accTotalEntryBal);
        return reportObj;
    }

    public int closingYearEnd(ServiceAFweb serviceAFWeb, CustomerObj customer, int year) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

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

        long NextYearTran = TimeConvertion.addDays(EndingYear, 1);

        // get all Asset_accountsId and Liability_accountsId to next year
        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getAccountingByTypeTime(ConstantKey.INT_ACC_TRAN, BeginingYear, EndingYear, 0);
        if (billingObjList == null) {
            billingObjList = new ArrayList();
        }
        ArrayList<AccEntryObj> accTotalEntryBal = new ArrayList();

        Date curDate = new java.sql.Date(TimeConvertion.currentTimeMillis());
        String curDateSt = curDate.toString();

        ArrayList<AccEntryObj> Revenue_accountsList = new ArrayList();
        for (int i = 0; i < Revenue_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Revenue_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Revenue_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Revenue_accountsList.add(accEntry);
        }

        ArrayList<AccEntryObj> Expense_accountsList = new ArrayList();
        for (int i = 0; i < Expense_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Expense_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Expense_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Expense_accountsList.add(accEntry);
        }

        float totalRevenue_accounts = 0;
        float totalExpense_accounts = 0;
        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int k = 0; k < Revenue_accountsList.size(); k++) {
                AccEntryObj accEntryT = Revenue_accountsList.get(k);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();
                    float total = 0;
                    total = credit - debit;
                    totalRevenue_accounts += total;

                }
            }

            for (int m = 0; m < Expense_accountsList.size(); m++) {
                AccEntryObj accEntryT = Expense_accountsList.get(m);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();
                    float total = 0;
                    total = debit - credit;
                    totalExpense_accounts += total;

                }
            }
        }

        float equity = totalRevenue_accounts - totalExpense_accounts;
        float amount = equity;
        long trantimeNextYear = NextYearTran;
        String data = "Year End Closing";

        if (amount >= 0) {
            String tranData = " debit " + E_RET_EARNING + " :" + 0 + "  credit " + E_RET_EARNING + ":" + amount + " year=" + year + " ";
            data = tranData + data;

            int result = serviceAFWeb.getAccountImp().addAccountingEntry(E_RET_EARNING, accountAdminObj, 0, (float) amount, data, trantimeNextYear);

        } else {
            amount = -amount;
            String tranData = " debit " + E_RET_EARNING + " :" + amount + "  credit " + E_RET_EARNING + ":" + 0 + " year=" + year + " ";
            data = tranData + data;

            int result = serviceAFWeb.getAccountImp().addAccountingEntry(E_RET_EARNING, accountAdminObj, (float) amount, 0, data, trantimeNextYear);
        }

/////////////////////////////////////////////////
        ArrayList<AccEntryObj> Asset_accountsList = new ArrayList();
        for (int i = 0; i < Asset_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Asset_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Asset_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Asset_accountsList.add(accEntry);
        }

        ArrayList<AccEntryObj> Liability_accountsList = new ArrayList();
        for (int i = 0; i < Liability_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Liability_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Liability_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Liability_accountsList.add(accEntry);
        }

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int k = 0; k < Asset_accountsList.size(); k++) {
                AccEntryObj accEntryT = Asset_accountsList.get(k);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();

                    float total = 0;
                    total = credit - debit;
                    total += accEntryT.getTotal();
                    accEntryT.setTotal(total);
                }
            }

///////////////////////////            
            for (int m = 0; m < Liability_accountsList.size(); m++) {
                AccEntryObj accEntryT = Liability_accountsList.get(m);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();

                    float total = 0;
                    total = credit - debit;

                    total += accEntryT.getTotal();
                    accEntryT.setTotal(total);
                }
            }
        }

        for (int m = 0; m < Asset_accountsList.size(); m++) {
            AccEntryObj accEntryT = Asset_accountsList.get(m);

            float total = accEntryT.getTotal();
            if (total != 0) {
                // add to next year
                if (total > 0) {
                    amount = total;
                    String tranData = " debit " + accEntryT.getName() + " :" + 0 + "  credit " + accEntryT.getName() + ":" + amount + " year=" + year + " ";
                    data = tranData + data;
                    int result = serviceAFWeb.getAccountImp().addAccountingEntry(accEntryT.getName(), accountAdminObj, 0, (float) amount, data, trantimeNextYear);

                } else if (total < 0) {
                    amount = -total;
                    String tranData = " debit " + accEntryT.getName() + " :" + amount + "  credit " + accEntryT.getName() + ":" + 0 + " year=" + year + " ";
                    data = tranData + data;
                    int result = serviceAFWeb.getAccountImp().addAccountingEntry(accEntryT.getName(), accountAdminObj, (float) amount, 0, data, trantimeNextYear);

                }
            }

        }

        for (int m = 0; m < Liability_accountsList.size(); m++) {
            AccEntryObj accEntryT = Liability_accountsList.get(m);

            float total = accEntryT.getTotal();
            if (total != 0) {
                // add to next year
                if (total > 0) {
                    amount = total;
                    String tranData = " debit " + accEntryT.getName() + " :" + 0 + "  credit " + accEntryT.getName() + ":" + amount + " year=" + year + " ";
                    data = tranData + data;
                    int result = serviceAFWeb.getAccountImp().addAccountingEntry(accEntryT.getName(), accountAdminObj, 0, (float) amount, data, trantimeNextYear);

                } else if (total < 0) {
                    amount = -total;
                    String tranData = " debit " + accEntryT.getName() + " :" + amount + "  credit " + accEntryT.getName() + ":" + 0 + " year=" + year + " ";
                    data = tranData + data;
                    int result = serviceAFWeb.getAccountImp().addAccountingEntry(accEntryT.getName(), accountAdminObj, (float) amount, 0, data, trantimeNextYear);

                }
            }

        }
//////////////////

        ArrayList<AccEntryObj> depreciation_accountsList = new ArrayList();
        for (int i = 0; i < depreciation_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            if (depreciation_accounts[i].equals(E_RET_EARNING)) {
                continue;
            }
            accEntry.setId(depreciation_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(depreciation_accounts[i]);
            accTotalEntryBal.add(accEntry);

            depreciation_accountsList.add(accEntry);
        }

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);
            for (int m = 0; m < depreciation_accountsList.size(); m++) {
                AccEntryObj accEntryT = depreciation_accountsList.get(m);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();

                    float total = 0;
                    total = credit - debit;
                    total += accEntryT.getTotal();
                    accEntryT.setTotal(total);
                    if (total == 0) {
                        continue;
                    }
                    if (accEntryT.getName().equals(YEAR_EXPENSE)) {
                        String dataSt = accTran.getData();
                        AccDeprecateObj accData = serviceAFWeb.getAccountImp().getAccDeprecateObj(dataSt);
                        int yearEnd = accData.getYearCnt() - 1;
                        float monthCost = accData.getMonCost();
                        float orgAmount = accData.getOrgAmount();
                        data = accData.getData();

                        amount = accTran.getBalance();
                        if (yearEnd == 0) {

                            float remain = amount; //last year remaining
                            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_ACC_PREPAID, accountAdminObj, (float) remain, data, trantimeNextYear);

                        }
                        if (yearEnd > 0) {
                            double curExpense = monthCost * 12;
                            curExpense = (Math.round(curExpense * 100.0) / 100.0);
                            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_ACC_PREPAID, accountAdminObj, (float) curExpense, data, trantimeNextYear);
                            // Keep the YEAR_DEPRECIATION for next year
                            double remain = amount - curExpense;
                            remain = (Math.round(remain * 100.0) / 100.0);
//                            String tranData = " T1:" + EX_EXPENSE + " " + A_ACC_PREPAID + " ";
//                            data = tranData + data;
                            result = serviceAFWeb.getAccountImp().addAccountingEntryYear(YEAR_EXPENSE, accountAdminObj, (float) amount, (float) remain, yearEnd, monthCost, orgAmount, data, trantimeNextYear);
                        }
                    }
                    if (accEntryT.getName().equals(YEAR_DEPRECIATION)) {
                        String dataSt = accTran.getData();
                        AccDeprecateObj accData = serviceAFWeb.getAccountImp().getAccDeprecateObj(dataSt);
                        int yearEnd = accData.getYearCnt() - 1;
                        float rate = accData.getRate();
                        double orgAmount = accData.getOrgAmount();
                        double orgExpense = orgAmount * rate / 100;
                        data = accData.getData();

                        amount = accTran.getBalance();
                        if (yearEnd == 0) {
                            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_DEPRECIATION, A_EQUIPMENT, accountAdminObj, (float) amount, data, trantimeNextYear);
                            return result;
                        }
                        if (orgExpense >= amount) {
                            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_DEPRECIATION, A_EQUIPMENT, accountAdminObj, (float) amount, data, trantimeNextYear);
                            return result;
                        }
                        if (yearEnd > 0) {
                            double curExpense = amount * rate / 100;
                            curExpense = (Math.round(curExpense * 100.0) / 100.0);

                            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_DEPRECIATION, A_EQUIPMENT, accountAdminObj, (float) curExpense, data, trantimeNextYear);

                            // Keep the YEAR_DEPRECIATION for next year
                            double remain = amount - curExpense;
                            remain = (Math.round(remain * 100.0) / 100.0);
//                            String tranData = " T1:" + EX_DEPRECIATION + " " + L_ACC_PAYABLE + " ";
//                            data = tranData + data;

                            result = serviceAFWeb.getAccountImp().addAccountingEntryRate(YEAR_DEPRECIATION, accountAdminObj, (float) amount, (float) remain, yearEnd, rate, (float) orgAmount, data, trantimeNextYear);
                        }
                    }

                }
            }
        }
        return 1;
    }

    // income statement       
    public AccReportObj getAccountReportYear(ServiceAFweb serviceAFWeb, int year, String namerptSt) {
        int lastYear = 0;
        if (year != 0) {
            lastYear = year * 12;
        }

        AccReportObj reportObj = new AccReportObj();

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        long BeginingYear = DateUtil.getFirstDayCurrentYear();
        long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);

        if (lastYear != 0) {
            BeginingYear = TimeConvertion.addMonths(BeginingYear, lastYear);
            EndingYear = TimeConvertion.addMonths(EndingYear, lastYear);
        }

        EndingYear = TimeConvertion.addDays(EndingYear, -1);

        reportObj.setBeginl(BeginingYear);
        reportObj.setBegindisplay(new java.sql.Date(BeginingYear));
        reportObj.setEndl(EndingYear);
        reportObj.setEnddisplay(new java.sql.Date(EndingYear));
        reportObj.setName("Income Statement");

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getAccountingByTypeTime(ConstantKey.INT_ACC_TRAN, BeginingYear, EndingYear, 0);
        if (billingObjList == null) {
            billingObjList = new ArrayList();
        }
        ArrayList<AccEntryObj> accTotalEntryBal = new ArrayList();

        Date curDate = new java.sql.Date(TimeConvertion.currentTimeMillis());
        String curDateSt = curDate.toString();

//    public static String Revenue_accounts[] = {R_REVENUE, R_F_INCOME, R_SRV_REVENUE};
//    public static String Expense_accounts[] = {EX_EXPENSE, EX_T50EXPENSE, EX_DEPRECIATION, EX_WAGES};
//    
        AccEntryObj accEntryCash = new AccEntryObj();
        accEntryCash.setId(INT_A_CASH);
        accEntryCash.setDateSt(curDateSt);
        accEntryCash.setName(A_CASH);
        accTotalEntryBal.add(accEntryCash);

        AccEntryObj accEntrySpace = new AccEntryObj();
        accEntrySpace.setId(-1);
        accEntrySpace.setDateSt(curDateSt);
        accEntrySpace.setName("Revenue_accounts");
        accTotalEntryBal.add(accEntrySpace);

        ArrayList<AccEntryObj> Revenue_accountsList = new ArrayList();
        for (int i = 0; i < Revenue_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Revenue_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Revenue_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Revenue_accountsList.add(accEntry);
        }

        AccEntryObj accEntrySpace1 = new AccEntryObj();
        accEntrySpace1.setId(-1);
        accEntrySpace1.setDateSt(curDateSt);
        accEntrySpace1.setName("Expense_accounts");
        accTotalEntryBal.add(accEntrySpace1);

        ArrayList<AccEntryObj> Expense_accountsList = new ArrayList();
        for (int i = 0; i < Expense_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Expense_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Expense_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Expense_accountsList.add(accEntry);
        }

        AccEntryObj accTotalEntry = new AccEntryObj();
        accTotalEntry.setId(INT_SYS_TOTAL);
        accTotalEntry.setDateSt(curDateSt);
        accTotalEntry.setName(SYS_TOTAL);

        float totalRevenue_accounts = 0;
        float totalExpense_accounts = 0;
        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int k = 0; k < Revenue_accountsList.size(); k++) {
                AccEntryObj accEntryT = Revenue_accountsList.get(k);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();
                    float total = 0;
                    total = credit - debit;
                    totalRevenue_accounts += total;
                }
            }

            for (int m = 0; m < Expense_accountsList.size(); m++) {
                AccEntryObj accEntryT = Expense_accountsList.get(m);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();
                    float total = 0;
                    total = debit - credit;
                    totalExpense_accounts += total;
                }
            }
        }

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int j = 0; j < accTotalEntryBal.size(); j++) {
                AccEntryObj accEntryT = accTotalEntryBal.get(j);
                if (accEntryT.getName().equals(accTran.getName())) {
                    //        billObj.setPayment(debit);
                    //        billObj.setBalance(credit);
                    accEntryT.setDebit(accEntryT.getDebit() + accTran.getPayment());
                    accEntryT.setCredit(accEntryT.getCredit() + accTran.getBalance());

                    // already included in the first line
                    if (accEntryT.getName().equals(A_CASH)) {
                        continue;
                    }
                    accTotalEntry.setDebit(accTotalEntry.getDebit() + accTran.getPayment());
                    accTotalEntry.setCredit(accTotalEntry.getCredit() + accTran.getBalance());

                }
            }
        }
        accTotalEntryBal.add(accTotalEntry);

        AccEntryObj accEntrySpace2 = new AccEntryObj();
        accEntrySpace2.setId(-1);
        accEntrySpace2.setDateSt(curDateSt);
        accEntrySpace2.setName("Account: Retained Earnings");
        accTotalEntryBal.add(accEntrySpace2);

        AccEntryObj accEntryEarning = new AccEntryObj();
        accEntryEarning.setId(-1);

        accEntryEarning.setDateSt(curDateSt);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        float equity = totalRevenue_accounts - totalExpense_accounts;
        String currency = formatter.format(equity);
        accEntryEarning.setName("Retained Earnings total = " + currency);

        accTotalEntryBal.add(accEntryEarning);

        reportObj.setAccTotalEntryBal(accTotalEntryBal);

        return reportObj;
    }

    // balance sheet
    public AccReportObj getAccountBalanceReportYear(ServiceAFweb serviceAFWeb, int year, String namerptSt) {
        int newYear = 0;
        if (year != 0) {
            newYear = year * 12;
        }

        AccReportObj reportObj = new AccReportObj();

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        long BeginingYear = DateUtil.getFirstDayCurrentYear();
        long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);

        if (newYear != 0) {
            BeginingYear = TimeConvertion.addMonths(BeginingYear, newYear);
            EndingYear = TimeConvertion.addMonths(EndingYear, newYear);
        }

        EndingYear = TimeConvertion.addDays(EndingYear, -1);

        reportObj.setBeginl(BeginingYear);
        reportObj.setBegindisplay(new java.sql.Date(BeginingYear));
        reportObj.setEndl(EndingYear);
        reportObj.setEnddisplay(new java.sql.Date(EndingYear));
        reportObj.setName("Balance Sheet Statement");

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getAccountingByTypeTime(ConstantKey.INT_ACC_TRAN, BeginingYear, EndingYear, 0);
        if (billingObjList == null) {
            billingObjList = new ArrayList();
        }
        ArrayList<AccEntryObj> accTotalEntryBal = new ArrayList();

        Date curDate = new java.sql.Date(TimeConvertion.currentTimeMillis());
        String curDateSt = curDate.toString();

//    public static String Revenue_accounts[] = {R_REVENUE, R_F_INCOME, R_SRV_REVENUE};
//    public static String Expense_accounts[] = {EX_EXPENSE, EX_T50EXPENSE, EX_DEPRECIATION, EX_WAGES};
//    
        AccEntryObj accEntryCash = new AccEntryObj();
        accEntryCash.setId(INT_A_CASH);
        accEntryCash.setDateSt(curDateSt);
        accEntryCash.setName(A_CASH);
        accTotalEntryBal.add(accEntryCash);

        float totalAsset_accounts = 0;
        float totalLiability_accounts = 0;
//    
        AccEntryObj accEntrySpace = new AccEntryObj();
        accEntrySpace.setId(-1);
        accEntrySpace.setDateSt(curDateSt);
        accEntrySpace.setName("Asset_accounts");
        accTotalEntryBal.add(accEntrySpace);

        ArrayList<AccEntryObj> Asset_accountsList = new ArrayList();
        for (int i = 0; i < Asset_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Asset_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Asset_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Asset_accountsList.add(accEntry);
        }

        AccEntryObj accTotalEntryAsset_accounts = new AccEntryObj();
        accTotalEntryAsset_accounts.setId(-1);
        accTotalEntryAsset_accounts.setDateSt(curDateSt);
        accTotalEntryAsset_accounts.setName("Asset accounts total=");
        accTotalEntryBal.add(accTotalEntryAsset_accounts);

        AccEntryObj accEntrySpace1 = new AccEntryObj();
        accEntrySpace1.setId(-1);
        accEntrySpace1.setDateSt(curDateSt);
        accEntrySpace1.setName("Liability_accounts");
        accTotalEntryBal.add(accEntrySpace1);

        ArrayList<AccEntryObj> Liability_accountsList = new ArrayList();
        for (int i = 0; i < Liability_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(Liability_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(Liability_accounts[i]);
            accTotalEntryBal.add(accEntry);

            Liability_accountsList.add(accEntry);
        }
        AccEntryObj accTotalEntryLiability_accounts = new AccEntryObj();
        accTotalEntryLiability_accounts.setId(-1);
        accTotalEntryLiability_accounts.setDateSt(curDateSt);
        accTotalEntryLiability_accounts.setName("Liability accounts total=");
        accTotalEntryBal.add(accTotalEntryLiability_accounts);

        AccEntryObj accEntrySpace2 = new AccEntryObj();
        accEntrySpace2.setId(-1);
        accEntrySpace2.setDateSt(curDateSt);
        accEntrySpace2.setName("Equity_accounts");
        accTotalEntryBal.add(accEntrySpace2);

//        for (int i = 0; i < Equity_accounts.length; i++) {
//            AccEntryObj accEntry = new AccEntryObj();
//            accEntry.setId(Equity_accountsId[i]);
//            accEntry.setDateSt(curDateSt);
//            accEntry.setName(Equity_accounts[i]);
//            accTotalEntryBal.add(accEntry);
//        }
        AccEntryObj accTotalEntryEquity = new AccEntryObj();
        accTotalEntryEquity.setId(-1);
        accTotalEntryEquity.setDateSt(curDateSt);
        accTotalEntryEquity.setName("Owner's Equity accounts total=");
        accTotalEntryBal.add(accTotalEntryEquity);

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int k = 0; k < Asset_accountsList.size(); k++) {
                AccEntryObj accEntryT = Asset_accountsList.get(k);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();

                    float total = 0;
                    total = debit - credit;

                    totalAsset_accounts += total;
                }
            }
            for (int m = 0; m < Liability_accountsList.size(); m++) {
                AccEntryObj accEntryT = Liability_accountsList.get(m);
                if (accEntryT.getName().equals(accTran.getName())) {
                    float debit = accEntryT.getDebit() + accTran.getPayment();
                    float credit = accEntryT.getCredit() + accTran.getBalance();

                    float total = 0;
                    if (accEntryT.getName().indexOf("_payable") != -1) {
                        // Liability_accounts
                        total = credit - debit;
                    } else {
                        total = debit - credit;
                    }
                    totalLiability_accounts += total;
                }
            }
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        String currency = formatter.format(totalAsset_accounts);
        accTotalEntryAsset_accounts.setName("Asset accounts total = " + currency);
        currency = formatter.format(totalLiability_accounts);
        accTotalEntryLiability_accounts.setName("Liability accounts total = " + currency);

        float equity = totalAsset_accounts - totalLiability_accounts;
        currency = formatter.format(equity);
        accTotalEntryEquity.setName("Owner's Equity accounts total = " + currency);

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);
            for (int j = 0; j < accTotalEntryBal.size(); j++) {
                AccEntryObj accEntryT = accTotalEntryBal.get(j);
                if (accEntryT.getName().equals(accTran.getName())) {
                    //        billObj.setPayment(debit);
                    //        billObj.setBalance(credit);
                    accEntryT.setDebit(accEntryT.getDebit() + accTran.getPayment());
                    accEntryT.setCredit(accEntryT.getCredit() + accTran.getBalance());

                    if (accEntryT.getName().indexOf("_payable") != -1) {
                        // Liability_accounts
                        accEntryT.setTotal(accEntryT.getCredit() - accEntryT.getDebit());
                    } else {
                        accEntryT.setTotal(accEntryT.getDebit() - accEntryT.getCredit());
                    }
                    // already included in the first line
//                    if (accEntryT.getName().equals(A_CASH)) {
//                        continue;
//                    }
                }
            }
        }

//        accTotalEntryBal.add(accTotalEntry);
        reportObj.setAccTotalEntryBal(accTotalEntryBal);

        return reportObj;
    }

    // deprecation sheet
    public AccReportObj getAccountDeprecationReportYear(ServiceAFweb serviceAFWeb, int year, String namerptSt) {
        int lastYear = 0;
        if (year != 0) {
            lastYear = year * 12;
        }

        AccReportObj reportObj = new AccReportObj();

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        long BeginingYear = DateUtil.getFirstDayCurrentYear();
        long EndingYear = TimeConvertion.addMonths(BeginingYear, 12);

        if (lastYear != 0) {
            BeginingYear = TimeConvertion.addMonths(BeginingYear, lastYear);
            EndingYear = TimeConvertion.addMonths(EndingYear, lastYear);
        }

        EndingYear = TimeConvertion.addDays(EndingYear, -1);

        reportObj.setBeginl(BeginingYear);
        reportObj.setBegindisplay(new java.sql.Date(BeginingYear));
        reportObj.setEndl(EndingYear);
        reportObj.setEnddisplay(new java.sql.Date(EndingYear));
        reportObj.setName("Deprecation Statement");

        // begin 2021 01 01  (updatedatel)  end 2021 12 31
        ArrayList<BillingObj> billingObjList = serviceAFWeb.getAccountImp().getAccountingByTypeTime(ConstantKey.INT_ACC_TRAN, BeginingYear, EndingYear, 0);
        if (billingObjList == null) {
            billingObjList = new ArrayList();
        }
        ArrayList<AccEntryObj> accTotalEntryBal = new ArrayList();

        Date curDate = new java.sql.Date(TimeConvertion.currentTimeMillis());
        String curDateSt = curDate.toString();

//    public static String Revenue_accounts[] = {R_REVENUE, R_F_INCOME, R_SRV_REVENUE};
//    public static String Expense_accounts[] = {EX_EXPENSE, EX_T50EXPENSE, EX_DEPRECIATION, EX_WAGES};
//    
        AccEntryObj accEntryCash = new AccEntryObj();
        accEntryCash.setId(INT_A_CASH);
        accEntryCash.setDateSt(curDateSt);
        accEntryCash.setName(A_CASH);
        accTotalEntryBal.add(accEntryCash);

//    
        AccEntryObj accEntrySpace = new AccEntryObj();
        accEntrySpace.setId(-1);
        accEntrySpace.setDateSt(curDateSt);
        accEntrySpace.setName("depreciation_accounts");
        accTotalEntryBal.add(accEntrySpace);

        for (int i = 0; i < depreciation_accounts.length; i++) {
            AccEntryObj accEntry = new AccEntryObj();
            accEntry.setId(depreciation_accountsId[i]);
            accEntry.setDateSt(curDateSt);
            accEntry.setName(depreciation_accounts[i]);
            accTotalEntryBal.add(accEntry);
        }

        AccEntryObj accTotalEntry = new AccEntryObj();
        accTotalEntry.setId(INT_SYS_TOTAL);
        accTotalEntry.setDateSt(curDateSt);
        accTotalEntry.setName(SYS_TOTAL);

        for (int i = 0; i < billingObjList.size(); i++) {
            BillingObj accTran = billingObjList.get(i);

            for (int j = 0; j < accTotalEntryBal.size(); j++) {
                AccEntryObj accEntryT = accTotalEntryBal.get(j);
                if (accEntryT.getName().equals(accTran.getName())) {
                    //        billObj.setPayment(debit);
                    //        billObj.setBalance(credit);
                    accEntryT.setDebit(accEntryT.getDebit() + accTran.getPayment());
                    accEntryT.setCredit(accEntryT.getCredit() + accTran.getBalance());

                    accEntryT.setTotal(accEntryT.getDebit() - accEntryT.getCredit());

                    // already included in the first line
//                    if (accEntryT.getName().equals(A_CASH)) {
//                        continue;
//                    }
                    accTotalEntry.setDebit(accTotalEntry.getDebit() + accTran.getPayment());
                    accTotalEntry.setCredit(accTotalEntry.getCredit() + accTran.getBalance());

                    accTotalEntry.setTotal(accTotalEntry.getDebit() - accTotalEntry.getCredit());

                }
            }
        }
//        accTotalEntryBal.add(accTotalEntry);
        reportObj.setAccTotalEntryBal(accTotalEntryBal);

        return reportObj;
    }

////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////
//////////////////////////////////////////////////
//https://accounting-simplified.com/financial/double-entry-accounting/    
//////////////////////////////////////////////////////////////////
    public int addTransferRevenueTax(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        double amountBeforeTax = amount * (1 - GST);
        double tax = amountBeforeTax * GST;
        double totalTax = Math.round(tax * 100);
        totalTax = totalTax / 100;
        amount = amount - tax;

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_CASH, R_REVENUE, accountAdminObj, (float) amount, data, trantime);

        result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_CASH, L_TAX_PAYABLE, accountAdminObj, (float) totalTax, data, trantime);

        return result;

    }

    public int addTransferPayTax(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(L_TAX_PAYABLE, A_CASH, accountAdminObj, (float) amount, data, trantime);
        return result;
    }


    public int addTransferWithDrawRevenueTax(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        double amountBeforeTax = amount * (1 - GST);
        double tax = amountBeforeTax * GST;
        double totalTax = Math.round(tax * 100);
        totalTax = totalTax / 100;
        amount = amount - tax;

        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(R_REVENUE, A_CASH, accountAdminObj, (float) amount, data, trantime);

        result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(L_TAX_PAYABLE, A_CASH, accountAdminObj, (float) totalTax, data, trantime);

        return result;

    }

//Interest received on bank deposit account    
    public int addTransferIncome(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_CASH, R_F_INCOME, accountAdminObj, (float) amount, data, trantime);
        return result;

    }

//////////////////////////////////////////////////
    public int addTransferCash(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        if (amount >= 0) {
            return this.addCashProfit(serviceAFWeb, customer, amount, year, data);
        }
        return this.addCashLoss(serviceAFWeb, customer, -amount, year, data);
    }
//If you made a profit for the year, the profit and loss account would have a credit balance   

    public int addCashLoss(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        String tranData = " debit " + A_CASH + " :" + 0 + "  credit " + A_CASH + ":" + amount + " year=" + year + " ";
        data = tranData + data;

        if (year != 0) {
            trantime = TimeConvertion.addMonths(trantime, year * 12);
        }
        int result = serviceAFWeb.getAccountImp().addAccountingEntry(A_CASH, accountAdminObj, 0, (float) amount, data, trantime);
        return result;

    }

//If, however, the business made a loss for the year, the profit and loss account would have a debit balance.       
    public int addCashProfit(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        String tranData = " debit " + A_CASH + " :" + amount + "  credit " + A_CASH + ":" + 0 + " year=" + year + " ";
        data = tranData + data;

        if (year != 0) {
            trantime = TimeConvertion.addMonths(trantime, year * 12);
        }
        int result = serviceAFWeb.getAccountImp().addAccountingEntry(A_CASH, accountAdminObj, (float) amount, 0, data, trantime);
        return result;

    }

//////////////////////////////////////////////////
//https://www.double-entry-bookkeeping.com/retained-earnings/retained-earnings-statement/   
//http://www.accounting-basics-for-students.com/-recording-retained-earnings-in-the-journal-.html   
//Ending balance = Beginning balance + Retained for the year
    public int addTransferEarning(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        if (amount >= 0) {
            return this.addRetainEarningProfit(serviceAFWeb, customer, amount, year, data);
        }
        return this.addRetainEarningLoss(serviceAFWeb, customer, -amount, year, data);
    }
//If you made a profit for the year, the profit and loss account would have a credit balance   

    public int addRetainEarningProfit(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        String tranData = " debit " + E_RET_EARNING + " :" + 0 + "  credit " + E_RET_EARNING + ":" + amount + " year=" + year + " ";
        data = tranData + data;
        if (year != 0) {
            trantime = TimeConvertion.addMonths(trantime, year * 12);
        }

        int result = serviceAFWeb.getAccountImp().addAccountingEntry(E_RET_EARNING, accountAdminObj, 0, (float) amount, data, trantime);
        return result;

    }

//If, however, the business made a loss for the year, the profit and loss account would have a debit balance.       
    public int addRetainEarningLoss(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        String tranData = " debit " + E_RET_EARNING + " :" + amount + "  credit " + E_RET_EARNING + ":" + 0 + " year=" + year + " ";
        data = tranData + data;
        if (year != 0) {
            trantime = TimeConvertion.addMonths(trantime, year * 12);
        }

        int result = serviceAFWeb.getAccountImp().addAccountingEntry(E_RET_EARNING, accountAdminObj, (float) amount, 0, data, trantime);
        return result;

    }

//////////////////////////////////////////////////
//////////////////////////////////////////////////
    //https://www.double-entry-bookkeeping.com/accounts-receivable/accounts-receivable-journal-entries/
    //https://www.double-entry-bookkeeping.com/accounts-payable/accounts-payable-journal-entries/
    public int addAccReceivableRecordSale(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_ACC_RECEIVABLE, R_REVENUE, accountAdminObj, (float) amount, data, trantime);
        return result;

    }

    public int addAccReceivableCashPayment(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_CASH, A_ACC_RECEIVABLE, accountAdminObj, (float) amount, data, trantime);
        return result;

    }
//////////////////////////////////////////////////
//////////////////////////////////////////////////
    // Examples of Payroll Journal Entries For Wages

    public int addTransferPayroll(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();
        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_WAGES, A_CASH, accountAdminObj, (float) amount, data, trantime);
        return result;

    }

//////////////////////////////////////////////////
//////////////////////////////////////////////////    
    // Payment of utility bills
    public int addTransferUtilityExpense(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, int year, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();

        double accExpense = amount;
        if (year == 0) {
            // expense in currently
            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_CASH, accountAdminObj, (float) amount, data, trantime);
            return result;
        }

//        accExpense = amount * rate / 100;
        int yearMonth = 12 * year;
        double orgAmount = amount;
        int monNum = TimeConvertion.getMonthNum(trantime);
        monNum += 1; // start 1 - 12
        int remMonNum = (12 - monNum) + 1;  // include current month
        double monthCost = (amount / yearMonth);
        monthCost = (float) (Math.round(monthCost * 100.0) / 100.0);

        double curExpense = monthCost * remMonNum;
        curExpense = (float) (Math.round(curExpense * 100.0) / 100.0);

        String ExSt = "Expense: " + amount + " for " + year + " year. ";
        data = ExSt + data;

        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_ACC_PREPAID, A_CASH, accountAdminObj, (float) amount, data, trantime);

        result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_ACC_PREPAID, accountAdminObj, (float) curExpense, data, trantime);

        // Keep the YEAR_DEPRECIATION for next year
        double remain = amount - curExpense;
        remain = (Math.round(remain * 100.0) / 100.0);
        String tranData = " T1:" + A_ACC_PREPAID + " " + A_CASH + "  T2:" + EX_EXPENSE + " " + A_ACC_PREPAID + " ";
        data = tranData + data;

        result = serviceAFWeb.getAccountImp().addAccountingEntryYear(YEAR_EXPENSE, accountAdminObj, (float) amount, (float) remain, year, (float) monthCost, (float) orgAmount, data, trantime);

        return result;
    }

//////////////////////////////////////////////////
//////////////////////////////////////////////////        

    // meals and entertaining 50% for tax
    public int addTransferExpenseTax(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, float rate, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();

        long trantime = System.currentTimeMillis();

        double accExpense = amount;
        if ((rate == 100) || (rate == 0)) {
            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_CASH, accountAdminObj, (float) amount, data, trantime);
            return result;
        }
        accExpense = amount * rate / 100;

        String ExSt = "Expense: " + amount + " for rate " + rate + "%. ";
        data = ExSt + data;

        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_EXPENSE, A_CASH, accountAdminObj, (float) accExpense, data, trantime);

        // meals and entertaining 50% for tax
        double amountLeft = amount - accExpense;
        result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_T50EXPENSE, A_CASH, accountAdminObj, (float) amountLeft, data, trantime);

        return result;

    }

//////////////////////////////////////////////////
//////////////////////////////////////////////////    
    // Purchase of Equipment by cash
    // same for Depreciation
    public int addTransferDepreciation(ServiceAFweb serviceAFWeb, CustomerObj customer, double amount, float rate, String data) {
        AccountObj accountAdminObj = serviceAFWeb.getAdminObjFromCache();
        long trantime = System.currentTimeMillis();

        Date dateSt = new Date(trantime);
        String DepSt = "Deprecation: " + amount + " for rate" + rate + "% from " + dateSt.toString() + ". ";
        data = DepSt + data;

        if ((rate == 100) || (rate == 0)) {
            int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_EQUIPMENT, A_CASH, accountAdminObj, (float) amount, data, trantime);
            return result;
        }

        double exDeplication = amount * rate / 100;
        exDeplication = (Math.round(exDeplication * 100.0) / 100.0);
        int monNum = TimeConvertion.getMonthNum(trantime);
        monNum += 1; // start 1 - 12

        ////force 50% rule for first year depreciation
        monNum = 7;
        ////force 50% rule for first year depreciation

        int remMonNum = (12 - monNum) + 1;  // include current month
        double curExpense = (exDeplication / 12) * remMonNum;
        curExpense = (float) (Math.round(curExpense * 100.0) / 100.0);

        int result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(A_EQUIPMENT, A_CASH, accountAdminObj, (float) amount, data, trantime);

        result = serviceAFWeb.getAccountImp().addAccountingDoubleEntry(EX_DEPRECIATION, A_EQUIPMENT, accountAdminObj, (float) curExpense, data, trantime);

        // Keep the YEAR_DEPRECIATION for next year
        double orgAmount = amount;
        double remain = amount - curExpense;
        remain = (Math.round(remain * 100.0) / 100.0);
        String tranData = " T1:" + A_EQUIPMENT + " " + A_CASH + "  T2:" + EX_DEPRECIATION + " " + L_ACC_PAYABLE + " ";
        data = tranData + data;

        int MAX_YR = 5;
        result = serviceAFWeb.getAccountImp().addAccountingEntryRate(YEAR_DEPRECIATION, accountAdminObj, (float) amount, (float) remain, MAX_YR, rate, (float) orgAmount, data, trantime);

        return result;

    }

////////////////////////////////////////////////////////////////////
}
