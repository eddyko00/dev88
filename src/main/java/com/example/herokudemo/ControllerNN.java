/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFneuralNet;
import com.afweb.processnn.NNetService;
import com.afweb.service.*;

import com.afweb.util.CKey;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import java.util.ArrayList;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
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
public class ControllerNN {

    private static AFwebService afWebService = new AFwebService();
    private static NNetService nnService = new NNetService();

    public static void getHelpSystem(ArrayList<String> arrayString) {
        arrayString.add("/cust/{username}/sys/clearnninput");
        arrayString.add("/cust/{username}/sys/clearallnntran");
        arrayString.add("/cust/{username}/sys/clearnn2tran?tr=");

        arrayString.add("/cust/{username}/sys/autonnflag");
        arrayString.add("/cust/{username}/sys/autonnflag/enable");
        arrayString.add("/cust/{username}/sys/autonnflag/disable");

        arrayString.add("/cust/{username}/sys/deletenn1table");

        arrayString.add("/cust/{username}/sys/neuralnet/{name}/release");
        arrayString.add("/cust/{username}/sys/neuralnet/{name}/type/{type}/weight0");
        arrayString.add("/cust/{username}/sys/neuralnet/{name}/type/{type}/weight1");
        arrayString.add("/cust/{username}/sys/neuralnet/{name}/updateweight0");
        arrayString.add("/cust/{username}/sys/neuralnet/{name}/updateweight1");
    }

    public static void getHelpInfo(ArrayList<String> arrayString) {

    }

    @RequestMapping(value = "/cust/{username}/sys/clearnninput", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemClearNNinput(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (afWebService.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(nnService.SystemClearNNinput(afWebService));
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(nnService.SystemClearNNinput(afWebService));
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/deletenn1table", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getDeleteNN1(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (afWebService.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse("" + nnService.SystemDeleteNN1Table(afWebService));
                msg.setResult(true);
                return msg;
            }
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse("" + nnService.SystemDeleteNN1Table(afWebService));
                msg.setResult(true);
                return msg;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/autonnflag", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getSystemNNFlag(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/autonnflag/enable", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus setSystemNNFlag(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                ServiceAFweb.processNeuralNetFlag = true;

                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                ServiceAFweb.processNeuralNetFlag = true;

                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/autonnflag/disable", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus setdisableSystemNNFlag(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                ServiceAFweb.processNeuralNetFlag = false;

                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                ServiceAFweb.processNeuralNetFlag = false;

                msg.setResponse("" + ServiceAFweb.processNeuralNetFlag);
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/clearallnntran", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemClearAllNNtran(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(nnService.SystemClearNNtran(afWebService, ConstantKey.SIZE_TR));
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(nnService.SystemClearNNtran(afWebService, ConstantKey.SIZE_TR));
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/clearnn2tran", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemClearNNtran(
            @PathVariable("username") String username,
            @RequestParam(value = "tr", required = false) String trSt
    ) {
        WebStatus msg = new WebStatus();
        // remote is stopped

        int defTR = ConstantKey.INT_TR_NN2;

        if (trSt != null) {
            defTR = Integer.parseInt(trSt);
        }

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {

                msg.setResponse(nnService.SystemClearNNtran(afWebService, defTR));
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(nnService.SystemClearNNtran(afWebService, defTR));
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/neuralnet/{name}/release", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int releaseNeuralNetObj(
            @PathVariable("username") String username,
            @PathVariable("name") String name
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {

                int result = nnService.releaseNeuralNetObj(afWebService, name);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/sys/neuralnet/{name}/type/{type}/weight0", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AFneuralNet getNeuralNetObjWeight0(
            @PathVariable("username") String username,
            @PathVariable("name") String name,
            @PathVariable("type") String type
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int nnType = Integer.parseInt(type);
                AFneuralNet result = nnService.getNeuralNetObjWeight0(afWebService, name, nnType);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/neuralnet/{name}/type/{type}/weight1", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AFneuralNet getNeuralNetObjWeight1(
            @PathVariable("username") String username,
            @PathVariable("name") String name,
            @PathVariable("type") String type
    ) {
        afWebService.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int nnType = Integer.parseInt(type);
                AFneuralNet result = nnService.getNeuralNetObjWeight1(afWebService, name, nnType);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/neuralnet/{name}/updateweight0", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setNeuralNetObjWeight0(
            @PathVariable("username") String username,
            @PathVariable("name") String name,
            @RequestBody String input
    ) {
        afWebService.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        AFneuralNet afNeuralNet = null;
        try {
//            int size = input.length();
//            input = input.substring(1, size - 1);
            afNeuralNet = new ObjectMapper().readValue(input, AFneuralNet.class);
        } catch (IOException ex) {
            return 0;
        }

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = nnService.setNeuralNetObjWeight0(afWebService, afNeuralNet);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/sys/neuralnet/{name}/updateweight1", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setNeuralNetObjWeight1(
            @PathVariable("username") String username,
            @PathVariable("name") String name,
            @RequestBody String input
    ) {
        afWebService.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        AFneuralNet afNeuralNet = null;
        try {
//            int size = input.length();
//            input = input.substring(1, size - 1);

            afNeuralNet = new ObjectMapper().readValue(input, AFneuralNet.class);
        } catch (IOException ex) {
            return 0;
        }

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int result = nnService.setNeuralNetObjWeight1(afWebService, afNeuralNet);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

}
