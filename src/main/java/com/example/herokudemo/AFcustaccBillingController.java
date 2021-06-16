/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.account.*;
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

/**
 *
 * @author koed
 */
//https://www.baeldung.com/spring-cors
@CrossOrigin(origins = "*", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:8383")
@RestController
public class AFcustaccBillingController {

    private static AFwebService afWebService = new AFwebService();

    public static void getHelpSystem(ArrayList<String> arrayString) {
        //

    }

    public static void getHelpInfo(ArrayList<String> arrayString) {
        arrayString.add("/cust/{username}/acc/{accountid}/billing?length= (default/Max 12)");
        arrayString.add("/cust/{username}/acc/{accountid}/billing/{billid}/remove");
    }

    // "/cust/{username}/acc/{accountid}/billing?length="
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/billing", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<BillingObj> getAccountBillingList(
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
        int length = 12;
        if (lengthSt != null) {
            length = Integer.parseInt(lengthSt);
            if (length > 12) {
                length = 12;
            }
        }

        ArrayList<BillingObj> billingObjList = afWebService.getBillingByCustomerAccountID(username, null, accountid, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return billingObjList;
    }

    // "/cust/{username}/acc/{accountid}/billing/{billid}/remove"
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/billing/{billid}/remove", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountBillingDel(
            @PathVariable("username") String username,
            @PathVariable("accountid") String accountid,
            @PathVariable("billid") String billid,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }

        int ret = afWebService.removeBillingByCustomerAccountID(username, null, accountid, billid);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return ret;
    }

}
