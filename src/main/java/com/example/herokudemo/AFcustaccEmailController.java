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
public class AFcustaccEmailController {

    private static AFwebService afWebService = new AFwebService();
    ///cust/add?email={email}&pass={pass}&firstName={firstName}&lastName={lastName}&plan=

    //"/cust/{username}/acc/{accountid}/emailcomm?length=" 
    @RequestMapping(value = "/cust/{username}/acc/{accountid}/emailcomm", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList<CommObj> getAccountEmailCommList(
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
        }
        ArrayList<CommObj> commObjList = afWebService.getCommEmaiByCustomerAccountID(username, null, accountid, length);
        ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
        return commObjList;
    }
    //"/cust/{username}/acc/{accountid}/emailcomm/removeemail?idlist=");

    @RequestMapping(value = "/cust/{username}/acc/{accountid}/emailcomm/removeemail", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getAccountCommListRemoveemail(
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
                    ret = afWebService.removeAllEmailByCustomerAccountID(username, null, accountid);
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

}
