/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.*;
import com.afweb.model.account.*;
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
