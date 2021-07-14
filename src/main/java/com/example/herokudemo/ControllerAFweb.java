/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.herokudemo;

import com.afweb.dbnndata.NNetdataDB;
import com.afweb.dbsys.SysDB;
import com.afweb.dbstockinfo.StockInfoDB;
import com.afweb.model.*;
import com.afweb.model.account.*;
import com.afweb.model.stock.AFLockObject;
import com.afweb.processcustacc.CustAccService;
import com.afweb.service.ServiceAFweb;
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
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author koed
 */
//https://www.baeldung.com/spring-cors
@CrossOrigin(origins = "*", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:8383")
@RestController
public class ControllerAFweb {

    private static AFwebService afWebService = new AFwebService();
    private static CustAccService custaccService = new CustAccService();

    @GetMapping("/")
    public String index() {
        return "Hello there! I'm running v" + CKey.iis_ver;
    }

    @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus serverPing() {
        WebStatus msg = new WebStatus();

        msg = afWebService.SysServerPing();
        return msg;
    }

    /////////////////////////////////////////////////////////////////////////    
    public static void getHelpInternal(ArrayList<String> arrayString) {
        arrayString.add("/server/filepath");
        arrayString.add("/server/filepath/set?path=&name=&string= ");
        arrayString.add("/server/filepath/read?path=&name=");
        arrayString.add("/server/timerurl");
        arrayString.add("/server/timerurl/set?url=stop");

        arrayString.add("/server/dburl");        
        arrayString.add("/server/dburl/setsysdb?path=");                
        arrayString.add("/server/dburl/setinfonndb?path=");

//        arrayString.add("/cust/{username}/sys/downloaddb");
//        arrayString.add("/cust/{username}/sys/restoredb");
        arrayString.add("/cust/{username}/sys/cleandb");
        arrayString.add("/cust/{username}/sys/request");
    }

    public static void getHelpSystem(ArrayList<String> arrayString) {
        arrayString.add("/server");
        arrayString.add("/helphelp");

        arrayString.add("/cust/{username}/sys/stop");
        arrayString.add("/cust/{username}/sys/clearlock");
        arrayString.add("/cust/{username}/sys/clearlockinfo");
        arrayString.add("/cust/{username}/sys/start");
        arrayString.add("/cust/{username}/sys/resetdb");

        arrayString.add("/cust/{username}/uisys/{custid}/lock");
        arrayString.add("/cust/{username}/uisys/{custid}/timer");
//        arrayString.add("/cust/{username}/uisys/{custid}/cust/{customername}/update?status=&payment=&balance=&reason=");

        arrayString.add("/cust/{username}/sys/lock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/renewlock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/value/{lockdate}/comment/{comment}/setlock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/removelock");

    }

    public static void getHelpInfo(ArrayList<String> arrayString) {

    }

    public static void getHelpHelp(ArrayList<String> arrayString) {
        arrayString.add("--System DevOp--");
        getHelpSystem(arrayString);
        getHelpInfo(arrayString);

        ControllerStock.getHelpSystem(arrayString);
        ControllerStock.getHelpInfo(arrayString);

        ControllerStockInfo.getHelpSystem(arrayString);
        ControllerStockInfo.getHelpInfo(arrayString);

        ControllerNN.getHelpSystem(arrayString);
        ControllerNN.getHelpInfo(arrayString);

        ControllerAccounting.getHelpSystem(arrayString);
        ControllerAccounting.getHelpInfo(arrayString);

        arrayString.add("--User Interface--");
        ControllerCustAcc.getHelpSystem(arrayString);
        ControllerCustAcc.getHelpInfo(arrayString);

        ControllerCustAccFund.getHelpSystem(arrayString);
        ControllerCustAccFund.getHelpInfo(arrayString);

        ControllerBilling.getHelpSystem(arrayString);
        ControllerBilling.getHelpInfo(arrayString);

        ControllerEmail.getHelpSystem(arrayString);
        ControllerEmail.getHelpInfo(arrayString);

    }

    public static void getHelpAPI(ArrayList<String> arrayString) {

        arrayString.add("--User Interface--");

        ControllerCustAcc.getHelpInfo(arrayString);

        ControllerCustAccFund.getHelpInfo(arrayString);

        ControllerBilling.getHelpInfo(arrayString);

        ControllerEmail.getHelpInfo(arrayString);

    }

    public static void getHelpHelpAll(ArrayList<String> arrayString) {
        getHelpInternal(arrayString);

        getHelpHelp(arrayString);
    }

    @RequestMapping(value = "helphelp", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList SystemHelpPage() {

        ArrayList arrayString = new ArrayList();
        getHelpHelp(arrayString);
        return arrayString;
    }

    @RequestMapping(value = "helphelpsystem", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList SystemHelpAllPage() {

        ArrayList arrayString = new ArrayList();
        getHelpHelpAll(arrayString);

        return arrayString;
    }

    @RequestMapping(value = "/helpapi", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList SystemAPIHelpPage() {

        ArrayList arrayString = new ArrayList();
        getHelpAPI(arrayString);
        return arrayString;
    }
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////

    @RequestMapping(value = "/cust/{username}/sys/mysql", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getmysql(
            @PathVariable("username") String username,
            @RequestBody String input
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        RequestObj sqlObj = new RequestObj();
        try {
            sqlObj = new ObjectMapper().readValue(input, RequestObj.class);
        } catch (IOException ex) {
            return "";
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
//                System.out.println(sqlObj.getReq());
                if (sqlObj.getCmd().equals("1")) {
                    return afWebService.SysRemoteGetMySQL(sqlObj.getReq());
                } else if (sqlObj.getCmd().equals("2")) {
                    return afWebService.SysUpdateFromRemoteMySQL(sqlObj.getReq());
                } else if (sqlObj.getCmd().equals("3")) {
                    return afWebService.SysUpdateFromRemoteMySQLList(sqlObj.getReq());
                }
            }
        }
        return "";
    }

    @RequestMapping(value = "/server", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getServerObj() {

        return afWebService.getServerList();
    }

    @RequestMapping(value = "/server/filepath", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerFileP() {
        return ServiceAFweb.FileLocalPath;
    }

    @RequestMapping(value = "/server/sysfilepath", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerFileDir() {
        String userDirectory = Paths.get("").toAbsolutePath().toString();
        return userDirectory;
    }

    @RequestMapping(value = "/server/filepath/set", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerfileP(
            @RequestParam(value = "path", required = false) String pathSt,
            @RequestParam(value = "name", required = false) String nameSt,
            @RequestParam(value = "string", required = false) String St,
            HttpServletRequest request, HttpServletResponse response
    ) {

        if (pathSt != null) {
            if (pathSt.length() > 0) {
                ServiceAFweb.FileLocalPath = pathSt.trim();
                return "done...";
            }
        }
        if (St != null) {
            if (St.length() > 0) {
                String fileName = "sys.txt";
                if (nameSt != null) {
                    if (nameSt.length() > 0) {
                        fileName = nameSt;
                    }
                }
                ArrayList msgWrite = new ArrayList();
                msgWrite.add(St);
                boolean ret = ServiceAFweb.SysFilePut(fileName, msgWrite);
                return "done...";
            }
        }
        return "";
    }

    @RequestMapping(value = "/server/filepath/read", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String readServerfileP(
            @RequestParam(value = "path", required = false) String pathSt,
            @RequestParam(value = "name", required = true) String nameSt,
            HttpServletRequest request, HttpServletResponse response
    ) {

        if (pathSt != null) {
            if (pathSt.length() > 0) {
                ServiceAFweb.FileLocalPath = pathSt.trim();
                return "done...";
            }
        }
        if (nameSt != null) {
            if (nameSt.length() > 0) {
                ArrayList msgRead = new ArrayList();
                boolean ret = ServiceAFweb.SysFilePut(nameSt, msgRead);
                StringBuffer msgWrite = new StringBuffer();
                for (int i = 0; i < msgRead.size(); i++) {
                    msgWrite.append(msgRead.get(i));
                }

                return msgWrite.toString();
            }
        }
        return "";
    }

    @RequestMapping(value = "/server/dburl", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getServerDBURL() {
        ArrayList arrayString = new ArrayList();
        arrayString.add(" DBURL:" + CKey.SERVER_DB_URL);
        arrayString.add(" SystemDB:" + SysDB.remoteURL);
        arrayString.add(" StockInfoDB:" + StockInfoDB.remoteURL);
        arrayString.add(" NnetDB:" + NNetdataDB.remoteURL);

        return arrayString;
    }
//

    @RequestMapping(value = "/server/dburl/setsysdb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerDBURL(
            @RequestParam(value = "path", required = true) String pathSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        if (pathSt.equals("default")) {
            CKey.SERVER_DB_URL = CKey.URL_PATH_HERO_DBDB_PHP + CKey.WEBPOST_HERO_PHP;

        } else {
            CKey.SERVER_DB_URL = pathSt;
        }
        afWebService.SysDataSourceSystem(afWebService.dataSource, CKey.SERVER_DB_URL);
        afWebService.AccDataSource(afWebService.dataSource, CKey.SERVER_DB_URL);
        
        return pathSt;
    }

    @RequestMapping(value = "/server/dburl/setinfonndb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerDBURLInfo(
            @RequestParam(value = "path", required = true) String pathSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        if (pathSt.equals("default")) {
            CKey.dbInfoNNURL = CKey.URL_PATH_HERO_4_DBDB_PHP + CKey.WEBPOST_HERO_4_PHP;

        } else {
            CKey.dbInfoNNURL = pathSt;
        }
        afWebService.NnDataSourceNNnet(afWebService.dataSource, CKey.dbInfoNNURL);
        afWebService.InfSetDataSource(afWebService.dataSource, CKey.dbInfoNNURL);
        return pathSt;
    }

    @RequestMapping(value = "/server/timerurl", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerURL() {
        String url0 = RESTtimer.serverURL_0;
        if (url0.length() == 0) {
            url0 = CKey.SERVER_TIMMER_URL;
        }
        return url0;
    }

    @RequestMapping(value = "/server/timerurl/set", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerURL(
            @RequestParam(value = "url", required = true) String urlSt,
            HttpServletRequest request, HttpServletResponse response
    ) {

        RESTtimer.serverURL_0 = urlSt.trim();
        return "done...";
    }

    //////////////
    @RequestMapping(value = "/cust/{username}/sys/stop", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemStop(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();

        if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
            msg.setResponse(afWebService.SysStop());
            msg.setResult(true);
            return msg;
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/start", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemStart(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(afWebService.SysStart());
                msg.setResult(true);
                return msg;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(afWebService.SysStart());
                msg.setResult(true);
                return msg;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/cleandb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemCleanDB(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        if (CKey.UI_ONLY == true) {
            return null;
        }
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(afWebService.SysCleanDBData());
                msg.setResult(true);
                return msg;
            }
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(afWebService.SysCleanDBData());
                msg.setResult(true);
                return msg;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/resetdb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemResetDB(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped
        if (CKey.UI_ONLY == true) {
            return null;
        }
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(afWebService.SysDropDBData());
                msg.setResult(true);
                return msg;
            }
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(afWebService.SysDropDBData());
                msg.setResult(true);
                return msg;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/clearlockInfo", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemClearLockInfo(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(afWebService.SysClearLockInfo());
                msg.setResult(true);
                return msg;
            }
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(afWebService.SysClearLockInfo());
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/clearlock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus SystemClearLock(@PathVariable("username") String username) {
        WebStatus msg = new WebStatus();
        // remote is stopped
        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                msg.setResponse(afWebService.SysClearLock());
                msg.setResult(true);
                return msg;
            }
        }
        CustomerObj cust = afWebService.SysGetCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                msg.setResponse(afWebService.SysClearLock());
                msg.setResult(true);
                return msg;
            }
        }

        return null;
    }

//    @RequestMapping(value = "/cust/{username}/sys/downloaddb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
//    public @ResponseBody
//    WebStatus SystemDownloadDB(@PathVariable("username") String username) {
//        WebStatus msg = new WebStatus();
//        // remoote is stopped
//        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
//            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
//                msg.setResponse(afWebService.SystemDownloadDBData());
//                msg.setResult(true);
//                return msg;
//            }
//        }
//        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
//        if (cust != null) {
//            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
//                msg.setResponse(afWebService.SystemDownloadDBData());
//                msg.setResult(true);
//                return msg;
//            }
//        }
//        return null;
//
//    }
//
//    ///// Restore DB need the following
//    ////  SystemStop
//    ////  SystemCleanDBData
//    ////  SystemUploadDBData
//    ///// Restore DB need the following   
//    @RequestMapping(value = "/cust/{username}/sys/restoredb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
//    public @ResponseBody
//    WebStatus SystemRestoreDB(@PathVariable("username") String username) {
//        WebStatus msg = new WebStatus();
//        // remote is stopped
//        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
//            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
//                msg.setResponse(afWebService.SystemRestoreDBData());
//                msg.setResult(true);
//                return msg;
//            }
//        }
//
//        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
//        if (cust != null) {
//            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
//                msg.setResponse(afWebService.SystemRestoreDBData());
//                msg.setResult(true);
//                return msg;
//            }
//        }
//        return null;
//    }
    //"/cust/{username}/uisys/{custid}/lock"
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/lock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getUILockAll(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    ArrayList result = afWebService.SysLockGetAll();
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return result;
                }
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/lock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList getLockAll(
            @PathVariable("username") String username
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                ArrayList result = afWebService.SysLockGetAll();
                return result;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                ArrayList result = afWebService.SysLockGetAll();
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/lock/{lockname}/type/{type}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    AFLockObject getLockName(
            @PathVariable("username") String username,
            @PathVariable("lockname") String lockname,
            @PathVariable("type") String type
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int locktype = Integer.parseInt(type);
                AFLockObject result = afWebService.SysLockGetName(lockname, locktype);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return null;
    }

    @RequestMapping(value = "/cust/{username}/sys/lock/{lockname}/type/{type}/renewlock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setRenewLock(
            @PathVariable("username") String username,
            @PathVariable("lockname") String name,
            @PathVariable("type") String type
    ) {

        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int locktype = Integer.parseInt(type);
                int result = afWebService.SysLockRenew(name, locktype);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/sys/lock/{lockname}/type/{type}/value/{lockdate}/comment/{comment}/setlock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int setLockName(
            @PathVariable("username") String username,
            @PathVariable("lockname") String name,
            @PathVariable("type") String type,
            @PathVariable("lockdate") String lockdate,
            @PathVariable("comment") String comment
    ) {

        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int locktype = Integer.parseInt(type);
                long lockdatel = Long.parseLong(lockdate);
                int result = afWebService.SysSetLockName(name, locktype, lockdatel, comment);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

    @RequestMapping(value = "/cust/{username}/sys/lock/{lockname}/type/{type}/removelock", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    int getRemoveLock(
            @PathVariable("username") String username,
            @PathVariable("lockname") String name,
            @PathVariable("type") String type
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                int locktype = Integer.parseInt(type);
                int result = afWebService.SysLockRemoveLockName(name, locktype);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return result;
            }
        }
        return 0;
    }

///////////////////////////
    @RequestMapping(value = "/cust/{username}/sys/request", method = RequestMethod.POST, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    RequestObj SystemSQLRequest(
            @PathVariable("username") String username,
            @RequestBody String input
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);
        RequestObj sqlReq = null;
        try {
            sqlReq = new ObjectMapper().readValue(input, RequestObj.class);
        } catch (IOException ex) {
            return null;
        }

        if (ServiceAFweb.getServerObj().isSysMaintenance() == true) {
            if (username.toLowerCase().equals(CKey.ADMIN_USERNAME.toLowerCase())) {
                RequestObj sqlResp = afWebService.SysSQLRequest(sqlReq);
                return sqlResp;
            }
        }

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                RequestObj sqlResp = afWebService.SysSQLRequest(sqlReq);
                ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                return sqlResp;
            }
        }
        return null;
    }

//////////////////////////////////////////////////////
    @RequestMapping(value = "/timerhandler", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus timerHandlerREST(
            @RequestParam(value = "resttimerMsg", required = false) String resttimerMsg
    ) {

        WebStatus msg = new WebStatus();
        msg.setResult(true);
        msg.setResultID(ConstantKey.ENABLE);

        //process timer handler
        int timerCnt = afWebService.AFtimerHandler(resttimerMsg);

        msg.setResponse("timerCnt " + timerCnt);
        return msg;
    }

    //"/cust/{username}/uisys/{custid}/timer"
    @RequestMapping(value = "/cust/{username}/uisys/{custid}/timer", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus getUITimer(
            @PathVariable("username") String username,
            @PathVariable("custid") String custidSt
    ) {
        ServiceAFweb.getServerObj().setCntControRequest(ServiceAFweb.getServerObj().getCntControRequest() + 1);

        CustomerObj cust = afWebService.SysGetCustomerPassword(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
                if (custidSt.equals(cust.getId() + "")) {
                    WebStatus msg = new WebStatus();
                    msg.setResult(true);
                    msg.setResultID(ConstantKey.ENABLE);

                    //process timer handler
                    int timerCnt = afWebService.AFtimerHandler("starttimer");

                    msg.setResponse("timerCnt " + timerCnt);
                    ServiceAFweb.getServerObj().setCntControlResp(ServiceAFweb.getServerObj().getCntControlResp() + 1);
                    return msg;
                }
            }
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////    
    @RequestMapping(value = "/timer")
    public ModelAndView timerPage() {
        ModelAndView model = new ModelAndView("helloWorld");

        model.addObject("message", AFwebService.getServerObj().getServerName() + " " + AFwebService.getServerObj().getVerString() + "</br>"
                + AFwebService.getServerObj().getLastServUpdateESTdate() + "</br>"
                + AFwebService.getServerObj().getTimerMsg() + "</br>" + AFwebService.getServerObj().getTimerThreadMsg());
        return model;
    }

    @RequestMapping(value = "/timerthread", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus timerThread() {

        WebStatus msg = new WebStatus();
        msg.setResult(true);
        msg.setResultID(ConstantKey.ENABLE);

        //process timer handler
        int timerCnt = afWebService.AFtimerThread();

        msg.setResponse("timerCnt " + timerCnt);

        return msg;
    }

}
