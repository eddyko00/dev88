/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.service.*;

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
public class AFcustaccFundController {

    private static AFwebService afWebService = new AFwebService();

    // /cust/{username}/acc/{accountid}/fundlink/{accfundid}/add
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/add", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountStockFundAdd(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }

        int ret = afWebService.getFundAccountAddAccundFund(username, null, accountid, accfundid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    // /cust/{username}/acc/{accountid}/fundlink/{accfundid}/remove
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/remove", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountStockFundRemove(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }

        int ret = afWebService.getFundAccountRemoveAcocuntFund(username, null, accountid, accfundid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    //("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st?length={0 for all} - default 20")
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getAccountStockFundStockList(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
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
        }

        ArrayList returnList = afWebService.getFundStockListByAccountID(username, null, accountid, accfundid, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }
    // "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr}")

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockidsymbol}/tr", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    TradingRuleObj getAccountFundStockByTRname(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
            @PathVariable("stockidsymbol") String stockidsymbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        TradingRuleObj returnObj = afWebService.getFundAccountStockTRByTRname(username, null, accountid, accfundid, stockidsymbol, "TR_ACC");
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnObj;
    }

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockidsymbol}/tr/{trname}/tran", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getFundAccountStockTran(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
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
        ArrayList returnList = afWebService.getFundAccountStockTRTranListByAccountID(username, null, accountid, accfundid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    //("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr/{trname}/tran/history/chart?month=");   
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockidsymbol}/tr/{trname}/tran/history/chart", method = RequestMethod.GET, produces = {MediaType.IMAGE_JPEG_VALUE})
    public @ResponseBody
    byte[] getAccountFundStockHistoryChartDisplay(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
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

        byte[] ret = afWebService.getFundAccountStockTRLIstCurrentChartDisplay(username, null, accountid, accfundid, stockidsymbol, trname, monthSt);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return ret;
    }

    //("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr/{trname}/perf");
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockidsymbol}/tr/{trname}/perf", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getFundAccountStockTranPerf(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("accfundid") String accfundid,
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

        ArrayList returnList = afWebService.getFundAccountStockTRPerfList(username, null, accountid, accfundid, stockidsymbol, trname, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);

        return returnList;
    }

    @RequestMapping(value = "/cust/{username}/sys/globalfundmgr", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getSystemFundMgr(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse("System in Maintenance");
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse("" + afWebService.SystemFundResetGlobal());
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/performfundmgr", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getSystemPerfFundMgr(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse("System in Maintenance");
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse("" + afWebService.SystemFundSelectBest());
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/processfundmgr", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getProcessFundMgr(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse("System in Maintenance");
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse("" + afWebService.SystemFundPocessAddRemove());
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

}
