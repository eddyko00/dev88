/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.processstockinfo.StockInfoService;

import com.afweb.service.*;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

//https://www.baeldung.com/spring-cors
@CrossOrigin(origins = "*", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:8383")
@RestController
/**
 *
 * @author eddy
 */
public class ControllerStockInfo {

    private static AFwebService afWebService = new AFwebService();
    private static StockInfoService stockInfoService = new StockInfoService();

    public static void getHelpSystem(ArrayList<String> arrayString) {

    }

    public static void getHelpInfo(ArrayList<String> arrayString) {
        arrayString.add("/st/{symbol}/history?length={0 for all}");
        arrayString.add("/st/{symbol}/stocksplit?split=");
        arrayString.add("/st/{symbol}/stocksplitstatus?value=");
        arrayString.add("/st/{symbol}/updateinfo");
        arrayString.add("/st/{symbol}/deleteinfo");
        arrayString.add("/st/cleanallinfo");
    }

    @RequestMapping(value = "/st/{symbol}/history", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getStockHistory(
            @PathVariable("symbol") String symbol,
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
        ArrayList stockInfoList = afWebService.InfGetStockHistorical(symbol, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return stockInfoList;
    }

    @RequestMapping(value = "/st/{symbol}/stocksplitstatus", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getStockSplitStatus(
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "value", required = true) String valueSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int value = 0; //20;
        if (valueSt != null) {
            value = Integer.parseInt(valueSt);
        }

        int ret = afWebService.SysStockSplitStatus(symbol, value);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    @RequestMapping(value = "/st/{symbol}/stocksplit", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getStockSplit(
            @PathVariable("symbol") String symbol,
            @RequestParam(value = "split", required = true) String valueSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int value = 0; //20;
        if (valueSt != null) {
            value = Integer.parseInt(valueSt);
        }
        // split 1:3 stock more price /3 (value 3)  
        // Split 3:1 stock less price *3 (value -3)        
        int ret = afWebService.SysGetStockSplit(symbol, value);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

    @RequestMapping(value = "/st/{symbol}/updateinfo", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateStockInfo(
            @PathVariable("symbol") String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = stockInfoService.updateStockInfo(afWebService, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/st/{symbol}/deleteinfo", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int deleteStockInfo(
            @PathVariable("symbol") String symbol,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = stockInfoService.removeStockInfo(afWebService, symbol);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

    @RequestMapping(value = "/st/cleanallinfo", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int cleanAllStockInfo(
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }
        int result = stockInfoService.deleteAllStockInfo(afWebService);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return result;
    }

//    
}
