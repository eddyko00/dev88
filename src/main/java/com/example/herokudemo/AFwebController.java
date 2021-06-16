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
public class AFwebController {

    private static AFwebService afWebService = new AFwebService();

    //    @RequestMapping("/index")
//    public String indexMessage() {
//        return "index";
//    }
    @GetMapping("/")
    public String index() {
        return "Hello there! I'm running v" + CKey.iis_ver;
    }

        /////////////////////////////////////////////////////////////////////////    
    @RequestMapping(value = "helphelp", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList SystemHelpPage() {

        ArrayList arrayString = new ArrayList();

        arrayString.add("/server");
//        arrayString.add("/server/filepath");
//        arrayString.add("/server/filepath/set?path=&name=&string= ");
//        arrayString.add("/server/filepath/read?path=&name=");
//        arrayString.add("/server/url0 - 0-local, 1- Heroku, 2- OP");
//        arrayString.add("/server/url0/set?url=stop");
//        arrayString.add("/server/dburl");
//        arrayString.add("/server/dburl/set?url=");         
        arrayString.add("/helphelp");
        arrayString.add("/st?length={0 for all}");
        arrayString.add("/st/{symbol}");
        arrayString.add("/st/{symbol}/history?length={0 for all}");
        arrayString.add("/st/add/{symbol}");
        arrayString.add("/st/remove/{symbol}");
        arrayString.add("/st/deleteinfo/{symbol}");
        arrayString.add("/st/cleanallinfo");
        //
        arrayString.add("/cust/add?email={email}&pass={pass}&firstName={firstName}&lastName={lastName}&plan=");
        arrayString.add("/cust/login?email={email}&pass={pass}");
//        arrayString.add("/cust/{username}/login&pass={pass}");

        arrayString.add("/cust/{username}/acc");
        arrayString.add("/cust/{username}/acc/{accountid}");

        arrayString.add("/cust/{username}/acc/{accountid}/emailcomm?length={0 for all} - default 20");
        arrayString.add("/cust/{username}/acc/{accountid}/emailcomm/removeemail?idlist=");
        arrayString.add("/cust/{username}/acc/{accountid}/comm?length={0 for all} - default 20");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/add?data=");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/remove?idlist=");
        arrayString.add("/cust/{username}/acc/{accountid}/comm/remove/{id}");

        arrayString.add("/cust/{username}/acc/{accountid}/clearfundbalance");
        arrayString.add("/cust/{username}/acc/{accountid}/fundbestlist");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/add");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/remove");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st?length={0 for all} - default 20");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr/{trname}/tran/history/chart?year=");
        arrayString.add("/cust/{username}/acc/{accountid}/fundlink/{accfundid}/st/{stockid or symbol}/tr/{trname}/perf?length=");

        arrayString.add("/cust/{username}/acc/{accountid}/billing?length= (default/Max 12)");
        arrayString.add("/cust/{username}/acc/{accountid}/billing/{billid}/remove");
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
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/update?payment=&balance=&reason=&rate=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/tax?payment=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/earning?payment=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/deprecation?payment=&rate=&reason=&comment=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/utility?payment=&year=&reason=&comment=");

        arrayString.add("/cust/{username}/uisys/{custid}/accounting/report?name=&year=&namerpt=");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/removeaccounting?year=");

        arrayString.add("/cust/{username}/uisys/{custid}/accounting/entry/{id}");
        arrayString.add("/cust/{username}/uisys/{custid}/accounting/entry/{id}/remove");

        arrayString.add("/cust/{username}/uisys/{custid}/lock");
        arrayString.add("/cust/{username}/uisys/{custid}/timer");
        arrayString.add("/cust/{username}/uisys/{custid}/cust/{customername}/update?status=&payment=&balance=&reason=");

        arrayString.add("/cust/{username}/sys/cust/{customername}/status/{status}/substatus/{substatus}");
        arrayString.add("/cust/{username}/sys/cust/{customername}/removecustomer");
        arrayString.add("/cust/{username}/sys/custchangeapi?email={email}");
        arrayString.add("/cust/{username}/sys/custchangefund?email={email}");

        arrayString.add("/cust/{username}/sys/expiredcustlist?length={0 for all}");
        arrayString.add("/cust/{username}/sys/expiredStocklist?length={0 for all}");

        arrayString.add("/cust/{username}/sys/stop");
        arrayString.add("/cust/{username}/sys/clearlock");
        arrayString.add("/cust/{username}/sys/start");
        arrayString.add("/cust/{username}/sys/resetdb");

        arrayString.add("/cust/{username}/sys/clearnninput");
        arrayString.add("/cust/{username}/sys/clearallnntran");
        arrayString.add("/cust/{username}/sys/clearnn2tran?tr=");

        arrayString.add("/cust/{username}/sys/autonnflag");
        arrayString.add("/cust/{username}/sys/autonnflag/enable");
        arrayString.add("/cust/{username}/sys/autonnflag/disable");

        arrayString.add("/cust/{username}/sys/globalfundmgr");
        arrayString.add("/cust/{username}/sys/performfundmgr");
        arrayString.add("/cust/{username}/sys/processfundmgr");

//        arrayString.add("/cust/{username}/sys/deletenn1table");
        //DB Backup
//        arrayString.add("/cust/{username}/sys/downloaddb");
        //DB restore
//        arrayString.add("/cust/{username}/sys/cleandb");
//        arrayString.add("/cust/{username}/sys/restoredb");
//        arrayString.add("/cust/{username}/sys/request");
        arrayString.add("/cust/{username}/sys/lock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/renewlock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/value/{lockdate}/comment/{comment}/setlock");
        arrayString.add("/cust/{username}/sys/lock/{lockname}/type/{type}/removelock");

//        arrayString.add("/cust/{username}/sys/neuralnet/{name}/release");
//        arrayString.add("/cust/{username}/sys/neuralnet/{name}/type/{type}/weight0");
//        arrayString.add("/cust/{username}/sys/neuralnet/{name}/type/{type}/weight1");
//        arrayString.add("/cust/{username}/sys/neuralnet/{name}/updateweight0");
//        arrayString.add("/cust/{username}/sys/neuralnet/{name}/updateweight1");
        return arrayString;
    }

    @RequestMapping(value = "/api/help", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    ArrayList SystemAPIHelpPage() {

        ArrayList arrayString = new ArrayList();
        //
        arrayString.add("/api/cust/add?email={email}&pass={pass}&firstName={firstName}&lastName={lastName}");
        arrayString.add("/api/cust/login?email={email}&pass={pass}");

        arrayString.add("/api/cust/{username}/acc");
        arrayString.add("/api/cust/{username}/acc/{accountid}");

        arrayString.add("/api/cust/{username}/acc/{accountid}/comm?length= (default/Max 20)");
        arrayString.add("/api/cust/{username}/acc/{accountid}/comm/remove?idlist= (-1 delete all)");
        arrayString.add("/api/cust/{username}/acc/{accountid}/comm/remove/{id}");

        arrayString.add("/api/cust/{username}/acc/{accountid}/billing?length= (default/Max 12)");

        arrayString.add("/api/cust/{username}/acc/{accountid}/stname");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st?trname=&filter= (Max 50)&length= (default 20 Max 50)");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/add/{symbol}");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/remove/{symbol}");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/addsymbol?symbol={symbol}");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/removesymbol?symbol={symbol}");

        arrayString.add("/api/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/linktr/{linkopt or trname}");
        arrayString.add("/api/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/tran");

        arrayString.add("/api/cust/{username}/acc/{accountid}/st/{stockid or symbol}/tr/{trname}/perf");

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
        CustomerObj cust = afWebService.getCustomerIgnoreMaintenance(username, null);
        if (cust != null) {
            if (cust.getType() == CustomerObj.INT_ADMIN_USER) {
//                System.out.println(sqlObj.getReq());
                if (sqlObj.getCmd().equals("1")) {
                    return afWebService.SystemRemoteGetMySQL(sqlObj.getReq());
                } else if (sqlObj.getCmd().equals("2")) {
                    return afWebService.SystemRemoteUpdateMySQL(sqlObj.getReq());
                } else if (sqlObj.getCmd().equals("3")) {
                    return afWebService.SystemRemoteUpdateMySQLList(sqlObj.getReq());
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
                boolean ret = ServiceAFweb.SystemFilePut(fileName, msgWrite);
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
                boolean ret = ServiceAFweb.SystemFilePut(nameSt, msgRead);
                StringBuffer msgWrite = new StringBuffer();
                for (int i = 0; i < msgRead.size(); i++) {
                    msgWrite.append(msgRead.get(i));
                }

                return msgWrite.toString();
            }
        }
        return "";
    }

    @RequestMapping(value = "/server/mysqldb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerLocalDbURL() {
        return ServiceAFweb.URL_LOCALDB;
    }

    @RequestMapping(value = "/server/mysqldb/set", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerLocalDbURL(
            @RequestParam(value = "url", required = true) String urlSt,
            HttpServletRequest request, HttpServletResponse response
    ) {

        ServiceAFweb.URL_LOCALDB = urlSt.trim();
        //restart ServiceAFweb
        afWebService.SystemStart();
        return "done...";
    }

    @RequestMapping(value = "/server/dburl", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerDBURL() {
        return ServiceRemoteDB.getURL_PATH();
    }


    @RequestMapping(value = "/server/dburl/setherodb", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerDBURLOPHERDB() {
        ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_HERO_DBDB_PHP + CKey.WEBPOST_HERO_PHP);
        return ServiceRemoteDB.getURL_PATH();
    }

    @RequestMapping(value = "/server/dburl/setop", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerDBURLOP() {
        ServiceRemoteDB.setURL_PATH(CKey.URL_PATH_OP_DB_PHP1 + CKey.WEBPOST_OP_PHP);
        return ServiceRemoteDB.getURL_PATH();
    }

    @RequestMapping(value = "/server/dburl/set", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerDBURL(
            @RequestParam(value = "url", required = true) String urlSt,
            HttpServletRequest request, HttpServletResponse response
    ) {
        ServiceRemoteDB.setURL_PATH(urlSt.trim());
        return "done...";
    }

    @RequestMapping(value = "/server/url0", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerURL() {
        String url0 = RESTtimer.serverURL_0;
        if (url0.length() == 0) {
            url0 = CKey.SERVER_TIMMER_URL;
        }
        return url0;
    }

    @RequestMapping(value = "/server/url0/sethero", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String getServerURLHERO() {
        RESTtimer.serverURL_0 = CKey.URL_PATH_HERO;
        return RESTtimer.serverURL_0;
    }

    @RequestMapping(value = "/server/url0/set", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    String setServerURL(
            @RequestParam(value = "url", required = true) String urlSt,
            HttpServletRequest request, HttpServletResponse response
    ) {

        RESTtimer.serverURL_0 = urlSt.trim();
        return "done...";
    }

    @RequestMapping(value = "/timerhandler", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    WebStatus timerHandlerREST(
            @RequestParam(value = "resttimerMsg", required = false) String resttimerMsg
    ) {

        WebStatus msg = new WebStatus();
        msg.setResult(true);
        msg.setResultID(ConstantKey.ENABLE);

        //process timer handler
        int timerCnt = afWebService.timerHandler(resttimerMsg);

        msg.setResponse("timerCnt " + timerCnt);
        return msg;
    }

}
