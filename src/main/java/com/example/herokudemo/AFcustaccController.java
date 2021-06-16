/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFstockObj;
import com.afweb.service.ServiceAFweb;
import com.afweb.service.ServiceRemoteDB;
import com.afweb.util.CKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author koed
 */
//https://www.baeldung.com/spring-cors
@CrossOrigin(origins = "*", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:8383")
@RestController
public class AFcustaccController {

    private static AFwebService afWebService = new AFwebService();

    public static void getHelpSystem(ArrayList<String> arrayString) {
        arrayString.add("/cust/{username}/sys/cust/{customername}/status/{status}/substatus/{substatus}");
        arrayString.add("/cust/{username}/sys/cust/{customername}/removecustomer");
        arrayString.add("/cust/{username}/sys/custchangeapi?email={email}");
        arrayString.add("/cust/{username}/sys/custchangefund?email={email}");
    }

    public static void getHelpInfo(ArrayList<String> arrayString) {

        arrayString.add("/cust/{username}/login&pass={pass}");
        arrayString.add("/cust/{username}/acc");
        arrayString.add("/cust/{username}/acc/{accountid}");

        arrayString.add("/cust/{username}/acc/{accountid}/comm?length={0 for all} - default 20");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/add?data=");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/remove?idlist=");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/remove/{id}");

        arrayString.add("/cust/{username}/acc/{accountid}/banner?ver=");
        arrayString.add("/cust/{username}/acc/{accountid}/custacc");
        arrayString.add("/cust/{username}/acc/{accountid}/custupdate?email=&pass=&firstName=&lastName=&plan=");

        arrayString.add("/cust/{username}/acc/{accountid}/stname");
        arrayString.add("/cust/{username}/acc/{accountid}/st?trname=&filter= (Max 50)&length= (default 20 Max 50)");
        arrayString.add("/cust/{username}/acc/{accountid}/st/add/{symbol}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/remove/{symbol}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/addsymbol?symbol={symbol}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/removesymbol?symbol={symbol}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/linktr/{linkopt or trname}");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/history");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/history/chart?month=");
//        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/history/chartfile");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/clear");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/{signal}/order");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/perf");
        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/perf/history");
//        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/perf/history/display");
//        arrayString.add("/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/history/chart?path={filePath}");

        arrayString.add("/cust/{username}/uisys/{custid}/custnlist?length={0 for all} - default 20");
        arrayString.add("/cust/{username}/uisys/{custid}/custlist?name=");
        arrayString.add("/cust/{username}/uisys/{custid}/custlist?length={0 for all} - default 20");

    }

    ///cust/add?email={email}&pass={pass}&firstName={firstName}&lastName={lastName}&plan=
    @RequestMapping(value = "/cust/add", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    LoginObj addCustomerPassword(
            @RequestParam(value = "email", required = true) String emailSt,
            @RequestParam(value = "pass", required = true) String passSt,
            @RequestParam(value = "firstName", required = false) String firstNameSt,
            @RequestParam(value = "lastName", required = false) String lastNameSt,
            @RequestParam(value = "plan", required = false) String planSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            LoginObj loginObj = new LoginObj();
            loginObj.setCustObj(null);
            WebStatus webStatus = new WebStatus();
            webStatus.setResultID(100);
            loginObj.setWebMsg(webStatus);
            return loginObj;
        }
//       SUCC = 1;  EXISTED = 2; FAIL =0;
        LoginObj loginObj = afWebService.addCustomerPassword(emailSt, passSt, firstNameSt, lastNameSt, planSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return loginObj;
    }

    @RequestMapping(value = "/cust/login", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    LoginObj getCustObjLogin(
            @RequestParam(value = "email", required = true) String emailSt,
            @RequestParam(value = "pass", required = true) String passSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            LoginObj loginObj = new LoginObj();
            loginObj.setCustObj(null);
            WebStatus webStatus = new WebStatus();
            webStatus.setResultID(100);
            loginObj.setWebMsg(webStatus);
            return loginObj;
        }
        if (emailSt == null) {
            return null;
        }
        if (passSt == null) {
            return null;
        }
        if (emailSt.equals("11")) {
            emailSt = "admin1";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        } else if (emailSt.equals("22")) {
            emailSt = "fundmgr";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        } else if (emailSt.equals("33")) {
            emailSt = "indexmgr";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        } else if (emailSt.equals("00")) {
            emailSt = "admin1";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        } else if (emailSt.equals("111")) {
            emailSt = "admin1";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        } else if (emailSt.equals("1111")) {
            emailSt = "admin1";
            if (passSt.equals("00")) {
                passSt = "Passw0rd";
            }
        }

        LoginObj loginObj = afWebService.getCustomerEmailLogin(emailSt, passSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return loginObj;
    }

    //"/cust/{username}/acc/{accountid}/custupdate?email=&pass=&firstName=&lastName=&plan="
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/custupdate", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    LoginObj updateCustomerPassword(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "email", required = false) String emailSt,
            @RequestParam(value = "pass", required = false) String passSt,
            @RequestParam(value = "firstName", required = false) String firstNameSt,
            @RequestParam(value = "lastName", required = false) String lastNameSt,
            @RequestParam(value = "plan", required = false) String planSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            LoginObj loginObj = new LoginObj();
            loginObj.setCustObj(null);
            WebStatus webStatus = new WebStatus();
            webStatus.setResultID(100);
            loginObj.setWebMsg(webStatus);
            return loginObj;
        }
//       SUCC = 1;  EXISTED = 2; FAIL =0;
        LoginObj loginObj = afWebService.updateCustomerPassword(username, accountid, emailSt, passSt, firstNameSt, lastNameSt, planSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return loginObj;
    }

    //"/cust/{username}/acc/{accountid}/custacc"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/custacc", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    LoginObj getCustomerAcc(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            LoginObj loginObj = new LoginObj();
            loginObj.setCustObj(null);
            WebStatus webStatus = new WebStatus();
            webStatus.setResultID(100);
            loginObj.setWebMsg(webStatus);
            return loginObj;
        }
//       SUCC = 1;  EXISTED = 2; FAIL =0;
        LoginObj loginObj = afWebService.getCustomerAccLogin(username, accountid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return loginObj;
    }

    @RequestMapping(value = "/cust/{username}/acc", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountList(
            @PathVariable("username") String username,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList accountList = afWebService.getAccountList(username, null);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return accountList;
    }

    //("/cust/{username}/acc/{accountid}/fundbestlist");
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundbestlist", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<AccountObj> getAccountBestFundList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList<AccountObj> accList = afWebService.getFundAccounBestFundList(username, null);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return accList;
    }

    //"/cust/{username}/acc/{accountid}/fundlink");
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<AccountObj> getAccountFundList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList<AccountObj> accList = afWebService.getFundAccountByCustomerAccountID(username, null, accountid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return accList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AccountObj getAccount(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        AccountObj account = afWebService.getAccountByCustomerAccountID(username, null, accountid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return account;
    }

    // "/cust/{username}/acc/{accountid}/banner?ver="
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/banner?ver=", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<String> getAccountBannerList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "ver", required = false) String verSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList<String> messageList = new ArrayList();
        messageList.add("" + CKey.iis_ver);

        if (verSt != null) {
            float version = Float.parseFloat(verSt);
            if (CKey.iis_ver > version) {
                // return update messagemessage
                messageList.add("Please upgrade the app to version v1.1");
            }
        }

        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return messageList;
    }

    //"/cust/{username}/acc/{accountid}/clearfundbalance");
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/clearfundbalance", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountfundbalance(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }

        int ret = afWebService.SystemFundClearfundbalance(username, null, accountid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    // "/cust/{username}/acc/{accountid}/stname"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/stname", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockName_StockList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }

        ArrayList returnList = afWebService.getStockNameListByAccountID(username, null, accountid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    // "/cust/{username}/acc/{accountid}/st?trname=&filter= (Max 50)&length= (default 20 Max 50)"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStock_StockList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "trname", required = false) String trnameSt,
            @RequestParam(value = "filter", required = false) String filterSt,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
            if (length > 50) {
                length = 50;
            }
        }
        String trname = ConstantKey.TR_ACC;
        if (trnameSt != null) {
            trname = trnameSt;
        }
        ArrayList returnList = afWebService.getStockListByAccountIDTRname(username, null, accountid, trname, filterSt, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/addsymbol", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int addAccountStock_stockSym(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "symbol", required = true) String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        //MAX_ALLOW_STOCK_ERROR = 100 ; NEW = 1; EXISTED = 2
        int result = afWebService.addAccountStockByCustAcc(username, null, accountid, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/add/{symbol}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int addAccountStock_stock(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("symbol") String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = afWebService.addAccountStockByCustAcc(username, null, accountid, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/removesymbol", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int removeAccountStock_stockSym(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "symbol", required = true) String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = afWebService.removeAccountStockByUserNameAccId(username, null, accountid, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/remove/{symbol}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int removeAccountStock_stock(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("symbol") String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = afWebService.removeAccountStockByUserNameAccId(username, null, accountid, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AFstockObj getAccountStock_Stock(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        AFstockObj retObj = afWebService.getStockByAccountIDStockID(username, null, accountid, stockidsymbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return retObj;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/{signal}/order", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setAccountStockTran(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @PathVariable("signal") String signalSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int signal = Integer.parseInt(signalSt);
        int ret = afWebService.addAccountStockTran(username, null, accountid, stockidsymbol, trname, signal);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;

    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStock(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList returnList = afWebService.getAccountStockTRListByAccountID(username, null, accountid, stockidsymbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;

    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    TradingRuleObj getAccountStockByTRname(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        TradingRuleObj returnObj = afWebService.getAccountStockTRByTRname(username, null, accountid, stockidsymbol, trname);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnObj;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/linktr/{linktype}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setAccountStockTRoption(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @PathVariable("linktype") String linktypeST,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int ret = 0;
        ret = afWebService.setAccountStockTRoption(username, null, accountid, stockidsymbol, trname, linktypeST.toUpperCase());

        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    //"/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran?length=0"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockTran(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 0; //20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        ArrayList returnList = afWebService.getAccountStockTRTranListByAccountID(username, null, accountid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/history", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockHistory(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList returnList = afWebService.getAccountStockTRListHistory(username, null, accountid, stockidsymbol, trname);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/history/display", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockHistoryDisplay(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        ArrayList returnList = afWebService.getAccountStockTRListHistoryDisplay(username, null, accountid, stockidsymbol, trname);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/history/chartfile", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getAccountStockHistoryChart(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @RequestParam(value = "path", required = false) String pathSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return "";
        }

        String ret = afWebService.getAccountStockTRLIstCurrentChartFile(username, null, accountid, stockidsymbol, trname, pathSt);
//        String ret = afWebService.getAccountStockTRListHistoryChart(username, null, accountid, stockidsymbol, trname, pathSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }
    //"/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran/history/chart?month="

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/history/chart", method = RequestMethod.GET, produces = {MediaType.IMAGE_JPEG_VALUE})
    public @ResponseBody
    byte[] getAccountStockHistoryChartDisplay(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @RequestParam(value = "month", required = false) String monthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }

        byte[] ret = afWebService.getAccountStockTRLIstCurrentChartDisplay(username, null, accountid, stockidsymbol, trname, monthSt);
//        byte[] ret = afWebService.getAccountStockTRListHistoryChartDisplay(username, null, accountid, stockidsymbol, trname, pathSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/tran/clear", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountStockClrTran(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int ret = afWebService.getAccountStockTRClrTranByAccountID(username, null, accountid, stockidsymbol, trname);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    // "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/perf"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/perf", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockTranPerf(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 0;
        ArrayList returnList = afWebService.getAccountStockTRPerfList(username, null, accountid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/perf/history", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockTranPerfHistory(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 0;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        ArrayList returnList = afWebService.getAccountStockTRPerfHistory(username, null, accountid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/perf/historyreinvest", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockTranPerfHistoryReinvest(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 0;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        ArrayList returnList = afWebService.getAccountStockTRPerfHistoryReinvest(username, null, accountid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/st/{stockidsymbol}/tr/{trname}/perf/history/display", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockPerfHistoryDisplay(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            @PathVariable("trname") String trname,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }

        ArrayList returnList = afWebService.getAccountStockTRPerfHistoryDisplay(username, null, accountid, stockidsymbol, trname);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }
    ///cust/{username}/uisys/{custid}/custnlist?length={0 for all} - default 20");

    @RequestMapping(value = "/cust/{username}/uisys/{custid}/custnlist", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getUICustNList(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "length", required = false) String lengthSt) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        int length = 0; //20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (custidSt.equals(cust.getId() + "")) {
                if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                    ArrayList custNameList = afWebService.getCustomerNList(length);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return custNameList;
                }
            }
        }
        return null;

    }

    ///cust/{username}/uisys/{custid}/custlist?name&length={0 for all} - default 20");
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/custlist", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getUICustList(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "name", required = false) String nameSt,
            @RequestParam(value = "length", required = false) String lengthSt) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        int length = 0; //20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        ArrayList custObjList = new ArrayList();
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (custidSt.equals(cust.getId() + "")) {
                if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                    if (nameSt != null) {
                        NameObj nameObj = new NameObj(nameSt);
                        String UserName = nameObj.getNormalizeName();
                        CustomerObj cutObj = afWebService.getCustomerObjByName(UserName);
                        custObjList.add(cutObj);
                    } else {

                        custObjList = afWebService.getCustomerList(length);
                    }

                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return custObjList;
                }
            }
        }
        return null;

    }

    @RequestMapping(value = "/cust/{username}/sys/expiredcustlist", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getExpiredCustList(
            @PathVariable("username") String username,
            @RequestParam(value = "length", required = false) String lengthSt) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        int length = 0; //20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                ArrayList custNameList = afWebService.getExpiredCustomerList(length);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return custNameList;
            }
        }
        return null;

    }

    @RequestMapping(value = "/cust/{username}/sys/cust/{customername}/status/{status}/substatus/{substatus}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateCustomer(
            @PathVariable("username") String username,
            @PathVariable("customername") String customername,
            @PathVariable("status") String status,
            @PathVariable("substatus") String substatus
    ) {

        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (customername == null) {
            return 0;
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = afWebService.updateCustStatusSubStatus(customername, status, substatus);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

//        arrayString.add("/cust/{username}/sys/custchangeapi?email={email}"); 
    @RequestMapping(value = "/cust/{username}/sys/custchangeapi", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int ChAPICustomer(
            @PathVariable("username") String username,
            @RequestParam(value = "email", required = true) String emailSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        if (emailSt == null) {
            return 0;
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = afWebService.changeAPICustomer(emailSt);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

//        arrayString.add("/cust/{username}/sys/custchangefund?email={email}");  
    @RequestMapping(value = "/cust/{username}/sys/custchangefund", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int ChFundCustomer(
            @PathVariable("username") String username,
            @RequestParam(value = "email", required = true) String emailSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        if (emailSt == null) {
            return 0;
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = afWebService.changeFundCustomer(emailSt);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

//        arrayString.add("/cust/{username}/sys/cust/{customername}/removecustomer");    
    @RequestMapping(value = "/cust/{username}/sys/cust/{customername}/removecustomer", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int removeCustomer(
            @PathVariable("username") String username,
            @PathVariable("customername") String customername
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        if (customername == null) {
            return 0;
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = afWebService.removeCustomer(customername);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

    //"/cust/{username}/uisys/{custid}/cust/{customername}/update?status=&payment=&balance=&reason="
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/cust/{customername}/update", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateCustAllStatus(
            @PathVariable("username") String username,
            @PathVariable("customername") String customername,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "status", required = false) String statusSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "balance", required = false) String balanceSt,
            @RequestParam(value = "year", required = false) String yearSt,
            @RequestParam(value = "reason", required = false) String reasonSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (customername == null) {
            return 0;
        }
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = afWebService.updateAddCustStatusPaymentBalance(customername, statusSt, paymentSt, balanceSt, yearSt, reasonSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

////////////////////////////////////////////////
    //            arrayString.add("/cust/{username}/acc/{accountid}/comm/add?data=");  
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/comm/add", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountCommAdd(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "data", required = false) String dataSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return -1;
        }
        int ret = afWebService.addCommByCustAccountID(username, null, accountid, dataSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    //"/cust/{username}/acc/{accountid}/comm?length=" 
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/comm", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<CommObj> getAccountCommList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "length", required = false) String lengthSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int length = 20;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
            if (length > 20) {
                length = 20;
            }
        }
        ArrayList<CommObj> commObjList = afWebService.getCommByCustomerAccountID(username, null, accountid, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return commObjList;
    }

    //"/cust/{username}/acc/{accountid}/comm/remove?idlist=");
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/comm/remove", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountCommListRemove(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @RequestParam(value = "idlist", required = true) String idlist,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        if (idlist == null) {
            return 0;
        }
        if (idlist.length() == 0) {
            return 0;
        }
        int ret = 1;
        try {
            String[] idlistArray = idlist.split(",");
            for (int i = 0; i < idlistArray.length; i++) {
                String idSt = idlistArray[i];
                int comid = Integer.parseInt(idSt);
                if (comid == -1) {
                    ret = afWebService.removeAllCommByCustomerAccountID(username, null, accountid);
                } else {
                    ret = afWebService.removeCommByID(username, null, accountid, comid + "");
                }
            }
        } catch (Exception ex) {
            ret = 0;
        }

        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/comm/remove/{comid}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountCommListRemoveID(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("comid") String comid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int ret = afWebService.removeCommByID(username, null, accountid, comid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    ///////////////////////////////////////
}
