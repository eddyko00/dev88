/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.processaccounting.AccountingService;
import com.afweb.service.*;
import com.example.herokudemo.AFwebService;

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
public class ControllerAccounting {

    private static AFwebService afWebService = new AFwebService();
    private static AccountingService accountingService = new AccountingService();

    public static void getHelpSystem(ArrayList<String> arrayString) {
        //

    }

    public static void getHelpInfo(ArrayList<String> arrayString) {
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&rate=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/tax?payment=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/earning?payment=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/deprecation?payment=&rate=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/utility?payment=&year=&reason=&comment=");

        arrayString.add("/cust/{username}/uisys/{custid}/accounting/report?name=&year=&namerpt=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/removeaccounting?year=");

        arrayString.add("/cust/{username}/uisys/{custid}/accounting/entry/{id}");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/entry/{id}/remove");
    }

    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/update", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntry(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "balance", required = false) String balanceSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "rate", required = false) String rateSt,
            @RequestParam(value = "year", required = false) String yearSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.updateAccountingEntryPaymentBalance(afWebService, username, paymentSt, balanceSt, reasonSt, rateSt, yearSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    //"/cust/{username}/uisys/{custid}/accounting/tax?payment=&reason=&comment=
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/tax", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryTAX(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "year", required = false) String yearSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.insertAccountTAX(afWebService, username, paymentSt, reasonSt, yearSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/cash", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryCash(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "year", required = false) String yearSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.insertAccountCash(afWebService, username, paymentSt, reasonSt, yearSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    //"/cust/{username}/uisys/{custid}/accounting/earning?payment=&reason=&comment=
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/earning", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryEearning(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "year", required = false) String yearSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.insertAccountEarning(afWebService, username, paymentSt, reasonSt, yearSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/removeaccounting", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryRemoveAll(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "year", required = false) String yearSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.removeAccounting(afWebService, username, yearSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/yearend", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryYearEnd(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "year", required = false) String yearSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.AccountingYearEnd(afWebService, username, yearSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    //"/cust/{username}/uisys/{custid}/accounting/deprecation?payment=&rate=&reason=&comment="
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/deprecation", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryDeprecation(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "rate", required = false) String rateSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.updateAccountingExDeprecation(afWebService, username, paymentSt, rateSt, reasonSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    //"/cust/{username}/uisys/{custid}/accounting/utility?payment=&year=&reason=&comment="
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/utility", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int updateAccoundingEntryUtility(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "payment", required = false) String paymentSt,
            @RequestParam(value = "curyear", required = false) String yearSt,
            @RequestParam(value = "reason", required = false) String reasonSt,
            @RequestParam(value = "comment", required = false) String commentSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    //updating the real customer in custSt not the addmin user
                    int result = accountingService.updateAccountingExUtility(afWebService, username, paymentSt, yearSt, reasonSt, commentSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return 0;
    }

    //("/cust/{username}/uisys/{custid}/accounting/report?name=&year=&namerpt=");
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/report", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AccReportObj getUIAccountReport(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @RequestParam(value = "name", required = false) String nameSt,
            @RequestParam(value = "year", required = false) String yeatSt,
            @RequestParam(value = "namerpt", required = false) String namerptSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }
        int year = 0;
        if (yeatSt != null) {
            year = Integer.parseInt(yeatSt);
        }

        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (custidSt.equals(cust.getId() + "")) {
                if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                    AccReportObj accReportObj = accountingService.getAccountingReportByCustomerByName(afWebService, username, null, nameSt, year, namerptSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return accReportObj;
                }
            }
        }
        return null;
    }

    //("/cust/{username}/uisys/{custid}/accounting/entry/{id}");
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/entry/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AccEntryObj getUIAccountReportId(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @PathVariable("id") String idSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return null;
        }

        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (custidSt.equals(cust.getId() + "")) {
                if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                    AccEntryObj accEntry = accountingService.getAccountingEntryByCustomerById(afWebService, username, null, idSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return accEntry;
                }
            }
        }
        return null;
    }

    //("/cust/{username}/uisys/{custid}/accounting/entry/{id}/remove");
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/accounting/entry/{id}/remove", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int removeUIAccountReportId(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt,
            @PathVariable("id") String idSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return 0;
        }

        CustomerObj cust = afWebService.getCustomerPassword(username, null);
        if (cust != null) {
            if (custidSt.equals(cust.getId() + "")) {
                if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                    ////////

                    int ret = accountingService.removeAccountingEntryById(afWebService, username, null, idSt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return ret;
                }
            }
        }
        return 0;
    }

}
