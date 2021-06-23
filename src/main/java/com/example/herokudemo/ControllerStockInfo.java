/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.account.*;
import com.afweb.model.stock.*;
import com.afweb.processstock.StockService;
import com.afweb.processstockinfo.StockInfoService;

import com.afweb.service.*;

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
import org.springframework.web.servlet.ModelAndView;

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
        arrayString.add("/st/deleteinfo/{symbol}");
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
        ArrayList stockInfoList = afWebService.getStockHistoricalServ(symbol, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return stockInfoList;
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

    @RequestMapping(value = "/st/deleteinfo/{symbol}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
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
//    
    
}
