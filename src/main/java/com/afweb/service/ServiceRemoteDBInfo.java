/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service;

import com.afweb.model.stock.*;

import com.afweb.service.db.*;
import com.afweb.util.CKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;

import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import static org.apache.http.protocol.HTTP.USER_AGENT;

/**
 *
 * @author eddy
 */
public class ServiceRemoteDBInfo {

    public static Logger logger = Logger.getLogger("ServiceRemoteDBInfo");
    public static String CMD = "cmd";
    public static String CMDPOST = "sqlreq";

    public int getExecuteRemoteListDB_Mysql(ArrayList<String> sqlCMDList, String remoteURL) {
//        log.info("postExecuteListRemoteDB_Mysql sqlCMDList " + sqlCMDList.size());
        String postSt = "";
        int MAXPostNumSize = 18;
        int MAXByteSize = 1800;
        int postSize = 0;
        for (int i = 0; i < sqlCMDList.size(); i++) {

            postSize++;
            if ((postSize > MAXPostNumSize) || (postSt.length() > MAXByteSize)) {
                try {

                    int ret = postExecuteListRemoteDB_Mysql(postSt, remoteURL);
//                    if (getEnv.checkLocalPC() == true) {
//                        logger.info("postExecuteListRemoteDB_Mysql  " + postSize);
//                    }
                    if (ret == 0) {
                        return ret;
                    }
                    postSize = 0;
                    postSt = "";
                    // seems fail for AWARDSPACE
                    ServiceAFweb.AFSleep1Sec(5);

                } catch (Exception ex) {
                    logger.info("postExecuteListRemoteDB_Mysql exception " + ex);
                    return 0;
                }
            }

            if (postSt.length() == 0) {
                postSt = sqlCMDList.get(i);
                continue;
            }
            postSt += "~" + sqlCMDList.get(i);
        }
        try {
            if (postSt.length() == 0) {
                return 1;
            }
            int ret = postExecuteListRemoteDB_Mysql(postSt, remoteURL);
            return ret;
        } catch (Exception ex) {
            logger.info("postExecuteListRemoteDB_Mysql exception " + ex);
        }
        return 0;

    }

    private int postExecuteListRemoteDB_Mysql(String sqlCMDList, String remoteURL) throws Exception {
        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
//        log.info("postExecuteListRemoteDB_Mysql " + sqlCMDList);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "3");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMDList);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");

            if ((beg >= end) || (beg == -1)) {
                logger.info("postExecuteListRemoteDB_Mysql output " + sqlCMDList);
                return -1;
            }
            output = output.substring(beg + 3, end);
//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = dataArray[0];
            if (output == null) {
                logger.info("postExecuteListRemoteDB_Mysql array" + sqlCMDList);
                return 0;
            }
            if (output.length() == 0) {
                return 0;
            }
            return Integer.parseInt(output);

        } catch (Exception ex) {
            logger.info("postExecuteListRemoteDB_Mysql exception " + ex);

            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
            throw ex;
        }
    }

    public int postExecuteRemoteDB_RemoteMysql(String sqlCMD, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);

//        log.info("postExecuteRemoteDB_RemoteMysql " + sqlCMD);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "2");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMD);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");

            if ((beg >= end) || (beg == -1)) {
                logger.info("postExecuteRemoteDB_RemoteMysql fail " + sqlCMD);
                return -1;
            }
            output = output.substring(beg + 3, end);
            if (output.length() > 2) {
                logger.info("postExecuteRemoteDB_RemoteMysql output " + output);
            }
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = dataArray[0];
            if (output == null) {
                logger.info("postExecuteRemoteDB_RemoteMysql fail" + sqlCMD);
                return 0;
            }
            if (output.length() == 0) {
                return 0;
            }
            return Integer.parseInt(output);

        } catch (Exception ex) {
            logger.info("postExecuteRemoteDB_Mysql exception " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
            throw ex;
        }

    }

    public int getCountRowsRemoteDB_RemoteMysql(String sqlTable, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
        try {
            String subResourcePath = remoteURL;
            // create hash map
            HashMap newmap = new HashMap();
            newmap.put(CMD, "1");

            HashMap newbodymap = new HashMap();
            String sqlcmd = "SELECT COUNT(0) AS c FROM " + sqlTable;
            newbodymap.put(CMDPOST, sqlcmd);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");

            if ((beg >= end) || (beg == -1)) {
                return -1;
            }
            output = output.substring(beg + 3, end);

//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = "[";
            int recSize = 1;
            for (int i = 0; i < dataArray.length; i += recSize) {
                output += "{";
                output += "\"c\":\"" + dataArray[i] + "\"";
                if (i + recSize >= dataArray.length) {
                    output += "}";
                } else {
                    output += "},";
                }
            }
            output += "]";
//            log.info("getCountRowsInTable output " + output);
            ArrayList<CountRowsRDB> arrayDB = null;
            try {
                CountRowsRDB[] arrayItem = new ObjectMapper().readValue(output, CountRowsRDB[].class);
                List<CountRowsRDB> listItem = Arrays.<CountRowsRDB>asList(arrayItem);
                arrayDB = new ArrayList<CountRowsRDB>(listItem);
            } catch (IOException ex) {
                logger.info("getCountRowsInTable exception " + output);
                return -1;
            }
            int countR = arrayDB.get(0).getCount();
            return countR;

        } catch (Exception ex) {
            logger.info("getCountRowsInTable exception " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
            throw ex;
        }

    }

    public ArrayList<AFstockInfo> getStockInfoSqlRemoteDB_RemoteMysql(String sqlCMD, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
//        log.info("getStockInfoSqlRemoteDB_RemoteMysql " + sqlCMD);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "1");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMD);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");
            // create hash map
            if (beg > end) {
                return null;
            }
            output = output.substring(beg + 3, end);
            if (output.length() == 0) {
                return null;
            }
//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = "[";

            int recSize = 11;  // remember to add 1 more of the last dataArray
            for (int i = 0; i < dataArray.length; i += recSize) {
                output += "{";
                output += "\"id\":\"" + dataArray[i] + "\",";
                output += "\"entrydatedisplay\":\"" + dataArray[i + 1] + "\",";
                output += "\"entrydatel\":\"" + dataArray[i + 2] + "\",";
                output += "\"fopen\":\"" + dataArray[i + 3] + "\",";
                output += "\"fclose\":\"" + dataArray[i + 4] + "\",";
                output += "\"high\":\"" + dataArray[i + 5] + "\",";
                output += "\"low\":\"" + dataArray[i + 6] + "\",";
                output += "\"volume\":\"" + dataArray[i + 7] + "\",";
                output += "\"adjustclose\":\"" + dataArray[i + 8] + "\",";
                output += "\"sym\":\"" + dataArray[i + 9] + "\",";
                output += "\"stockid\":\"" + dataArray[i + 10] + "\"";

                if (i + recSize >= dataArray.length) {
                    output += "}";
                } else {
                    output += "},";
                }
            }
            output += "]";
            return getStockInfoSqlRemoteDB_Process(output);

        } catch (Exception ex) {
//            logger.info("getStockInfoSqlRemoteDB_RemoteMysql exception " + sqlCMD + " " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
//            throw ex;
        }
        return null;
    }

    private ArrayList<AFstockInfo> getStockInfoSqlRemoteDB_Process(String output) {
        if (output.equals("")) {
            return null;
        }
        ArrayList<StockInfoRDB> arrayDB = null;
        ArrayList<AFstockInfo> arrayReturn = new ArrayList();
        try {
            StockInfoRDB[] arrayItem = new ObjectMapper().readValue(output, StockInfoRDB[].class);
            List<StockInfoRDB> listItem = Arrays.<StockInfoRDB>asList(arrayItem);
            arrayDB = new ArrayList<StockInfoRDB>(listItem);

            for (int i = 0; i < arrayDB.size(); i++) {
                StockInfoRDB rs = arrayDB.get(i);

                AFstockInfo stocktmp = new AFstockInfo();
                stocktmp.setEntrydatel(Long.parseLong(rs.getEntrydatel()));
                stocktmp.setEntrydatedisplay(new java.sql.Date(stocktmp.getEntrydatel()));
                stocktmp.setFopen(Float.parseFloat(rs.getFopen()));
                stocktmp.setFclose(Float.parseFloat(rs.getFclose()));
                stocktmp.setHigh(Float.parseFloat(rs.getHigh()));
                stocktmp.setLow(Float.parseFloat(rs.getLow()));
                stocktmp.setVolume(Float.parseFloat(rs.getVolume()));
                stocktmp.setAdjustclose(Float.parseFloat(rs.getAdjustclose()));
                stocktmp.setSym(rs.getSym());
                stocktmp.setStockid(Integer.parseInt(rs.getStockid()));
                stocktmp.setId(Integer.parseInt(rs.getId()));

                arrayReturn.add(stocktmp);
            }
            return arrayReturn;
        } catch (IOException ex) {
            logger.info("getStockInfoSqlRemoteDB_Process exception " + output);
            return null;
        }
    }
//////////////////

    public ArrayList getAllLockSqlRemoteDB_RemoteMysql(String sqlCMD, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
//        log.info("getAllLockSqlRemoteDB_RemoteMysql " + sqlCMD);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "1");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMD);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");
            // create hash map
            if (beg > end) {
                return null;
            }
            output = output.substring(beg + 3, end);
            if (output.length() == 0) {
                return null;
            }
//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = "[";
// "create table lockobject (id int(10) not null auto_increment, lockname varchar(255) not null unique, type int(10) not null, 
//lockdatedisplay date, lockdatel bigint(20), comment varchar(255), primary key (id))");

            int recSize = 6;
            for (int i = 0; i < dataArray.length; i += recSize) {
                output += "{";
                output += "\"id\":\"" + dataArray[i] + "\",";
                output += "\"lockname\":\"" + dataArray[i + 1] + "\",";
                output += "\"type\":\"" + dataArray[i + 2] + "\",";
                output += "\"lockdatedisplay\":\"" + dataArray[i + 3] + "\",";
                output += "\"lockdatel\":\"" + dataArray[i + 4] + "\",";
                output += "\"comment\":\"" + dataArray[i + 5] + "\"";

                if (i + recSize >= dataArray.length) {
                    output += "}";
                } else {
                    output += "},";
                }
            }
            output += "]";
            return getAllLockSqlRemoteDB_Process(output);

        } catch (Exception ex) {
//            logger.info("getAllLockSqlRemoteDB exception " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
//            throw ex;
        }
        return null;
    }

    private ArrayList<AFLockObject> getAllLockSqlRemoteDB_Process(String output) {
        if (output.equals("")) {
            return null;
        }
        ArrayList<LockObjectRDB> arrayDB = null;
        ArrayList<AFLockObject> arrayReturn = new ArrayList();
        try {
            LockObjectRDB[] arrayItem = new ObjectMapper().readValue(output, LockObjectRDB[].class);
            List<LockObjectRDB> listItem = Arrays.<LockObjectRDB>asList(arrayItem);
            arrayDB = new ArrayList<LockObjectRDB>(listItem);

            for (int i = 0; i < arrayDB.size(); i++) {
                LockObjectRDB rs = arrayDB.get(i);

                AFLockObject lock = new AFLockObject();
                lock.setLockname(rs.getLockname());
                lock.setType(Integer.parseInt(rs.getType()));
                lock.setLockdatel(Long.parseLong(rs.getLockdatel()));
                lock.setLockdatedisplay(new java.sql.Date(lock.getLockdatel()));

                lock.setId(Integer.parseInt(rs.getId()));
                lock.setComment(rs.getComment());

                String tzid = "America/New_York"; //EDT
                TimeZone tz = TimeZone.getTimeZone(tzid);
                Date d = new Date(lock.getLockdatel());
                DateFormat format = new SimpleDateFormat("M/dd/yyyy hh:mm a z");
                format.setTimeZone(tz);
                String ESTdate = format.format(d);
                lock.setUpdateDateD(ESTdate);

                arrayReturn.add(lock);
            }
            return arrayReturn;
        } catch (IOException ex) {
            logger.info("getAllLockSqlRemoteDB exception " + output);
            return null;
        }

    }
///////////////

    public ArrayList getAllNameSqlRemoteDB_RemoteMysql(String sqlCMD, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
//        log.info("getAllNameSqlRemoteDB_RemoteMysql " + sqlCMD);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "1");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMD);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");
            // create hash map
            if (beg > end) {
                return null;
            }
            output = output.substring(beg + 3, end);
            ArrayList<String> retArray = new ArrayList();
            if (output.length() == 0) {
                return retArray;
            }

//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = "[";
            int recSize = 1;
            for (int i = 0; i < dataArray.length; i += recSize) {
                output += "{";
                output += "\"name\":\"" + dataArray[i] + "\"";
                if (i + recSize >= dataArray.length) {
                    output += "}";
                } else {
                    output += "},";
                }
            }
            output += "]";
            return getAllNameSqlRemoteDB_Process(output);

        } catch (Exception ex) {
            logger.info("getAllNameSqlRemoteDB exception " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
            throw ex;
        }
    }

    private ArrayList<String> getAllNameSqlRemoteDB_Process(String output) {
        if (output.equals("")) {
            return null;
        }
        ArrayList<NameRDB> arrayDB = null;
        ArrayList<String> arrayReturn = new ArrayList();
        try {
            NameRDB[] arrayItem = new ObjectMapper().readValue(output, NameRDB[].class);
            List<NameRDB> listItem = Arrays.<NameRDB>asList(arrayItem);
            arrayDB = new ArrayList<NameRDB>(listItem);

            for (int i = 0; i < arrayDB.size(); i++) {
                NameRDB nameRDB = arrayDB.get(i);
                arrayReturn.add(nameRDB.getName());
            }
            return arrayReturn;
        } catch (IOException ex) {
            logger.info("getAllNameSqlRemoteDB exception " + output);
            return null;
        }
    }

    public ArrayList getAllIdSqlRemoteDB_RemoteMysql(String sqlCMD, String remoteURL) throws Exception {

        ServiceAFweb.getServerObj().setCntRESTrequest(ServiceAFweb.getServerObj().getCntRESTrequest() + 1);
//        log.info("getAllIdSqlRemoteDB_RemoteMysql " + sqlCMD);
        try {
            String subResourcePath = remoteURL;
            HashMap newmap = new HashMap();
            newmap.put(CMD, "1");

            HashMap newbodymap = new HashMap();
            newbodymap.put(CMDPOST, sqlCMD);

            String output = sendRequest_remotesql(METHOD_POST, subResourcePath, newmap, newbodymap);

            int beg = output.indexOf("~~ ");
            int end = output.indexOf(" ~~");
            // create hash map
            if (beg > end) {
                return null;
            }
            output = output.substring(beg + 3, end);
            ArrayList<String> retArray = new ArrayList();
            if (output.length() == 0) {
                return retArray;
            }

//            String[] dataArray = output.split("~");
            String[] dataArray = splitIncludeEmpty(output, '~');
            output = "[";
            int recSize = 1;
            for (int i = 0; i < dataArray.length; i += recSize) {
                output += "{";
                output += "\"id\":\"" + dataArray[i] + "\"";
                if (i + recSize >= dataArray.length) {
                    output += "}";
                } else {
                    output += "},";
                }
            }
            output += "]";
            return getAllIdSqlRemoteDB_Process(output);

        } catch (Exception ex) {
            logger.info("getAllIdSqlRemoteDB_RemoteMysql exception " + ex);
            ServiceAFweb.getServerObj().setCntRESTexception(ServiceAFweb.getServerObj().getCntRESTexception() + 1);
            throw ex;
        }
    }

    private ArrayList<String> getAllIdSqlRemoteDB_Process(String output) {
        if (output.equals("")) {
            return null;
        }
        ArrayList<IdRDB> arrayDB = null;
        ArrayList<String> arrayReturn = new ArrayList();
        try {
            IdRDB[] arrayItem = new ObjectMapper().readValue(output, IdRDB[].class);
            List<IdRDB> listItem = Arrays.<IdRDB>asList(arrayItem);
            arrayDB = new ArrayList<IdRDB>(listItem);

            for (int i = 0; i < arrayDB.size(); i++) {
                IdRDB nameRDB = arrayDB.get(i);
                arrayReturn.add(nameRDB.getId());
            }
            return arrayReturn;
        } catch (IOException ex) {
            logger.info("getAllIdSqlRemoteDB exception " + output);
            return null;
        }
    }

    /////////////////////////////////////////////////////////////
    // operations names constants
    private static final String METHOD_POST = "post";
    private static final String METHOD_GET = "get";

    private String sendRequest_remotesql(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams) throws Exception {
        String response = null;
        for (int i = 0; i < 4; i++) {
            try {
                response = sendRequest_Process_Mysql(method, subResourcePath, queryParams, bodyParams);

                if (response != null) {
                    return response;
                }
            } catch (Exception ex) {
            }
            logger.info("sendRequest " + method + " Rety " + (i + 1));

            if (i == 0) {
                String bodyElement = "";
                if (bodyParams != null && !bodyParams.isEmpty()) {
                    String bodyTmp = "";
                    for (String key : bodyParams.keySet()) {
                        bodyTmp = bodyParams.get(key);
                        bodyTmp = bodyTmp.replaceAll("&", "-");
                        bodyTmp = bodyTmp.replaceAll("%", "%25");
                        bodyElement = key + "=" + bodyTmp;
                    }
                }
                if (bodyElement.length() > 100) {
                    bodyElement = bodyElement.substring(90);
                }
                logger.info("sendRequest " + bodyElement);
            }
            ServiceAFweb.AFSleep1Sec(4);

        }
        response = sendRequest_Process_Mysql(method, subResourcePath, queryParams, bodyParams);

        return response;
    }

    private String sendRequest_Process_Mysql(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams)
            throws Exception {
        try {

//            String URLPath = getURL_PATH() + subResourcePath;
            String URLPath = subResourcePath;

            String webResourceString = "";
            // assume only one param
            if (queryParams != null && !queryParams.isEmpty()) {
                for (String key : queryParams.keySet()) {
                    webResourceString = "?" + key + "=" + queryParams.get(key);
                }
            }

            String bodyElement = "";

            if (bodyParams != null && !bodyParams.isEmpty()) {
                String bodyTmp = "";
                for (String key : bodyParams.keySet()) {
                    bodyTmp = bodyParams.get(key);
                    bodyTmp = bodyTmp.replaceAll("&", "-");
                    bodyTmp = bodyTmp.replaceAll("%", "%25");
                    bodyElement = key + "=" + bodyTmp;
                }

            }

            URLPath += webResourceString;
            URL request = new URL(URLPath);
            //just for testing
//                log.info("Request:: " +URLPath);     
            boolean flagD = true;
            if (bodyElement.indexOf("select * from stockinfo where") == -1) {
                flagD = false;
            }
            if (bodyElement.indexOf("select * from stock where") == -1) {
                flagD = false;
            }
            if (flagD == true) {
                logger.info("Request Code:: " + bodyElement);
            }
            HttpURLConnection con = null; //(HttpURLConnection) request.openConnection();
            if (CKey.PROXY == true) {
                //////Add Proxy 
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ServiceAFweb.PROXYURL, 8080));
                con = (HttpURLConnection) request.openConnection(proxy);
                //////Add Proxy 
            } else {
                con = (HttpURLConnection) request.openConnection();
            }
            if (method.equals(METHOD_POST)) {
                con.setRequestMethod("POST");
            } else {
                con.setRequestMethod("GET");
            }
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
//                con.setRequestProperty("Content-Type", "application/json; utf-8");

            if (method.equals(METHOD_POST)) {
                // For POST only - START
                con.setDoOutput(true);
                OutputStream os = con.getOutputStream();
                byte[] input = bodyElement.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
                os.close();
                // For POST only - END
            }

            int responseCode = con.getResponseCode();

            if (responseCode != 200) {
                logger.info("Response Code:: " + responseCode);
            }
            if (responseCode >= 200 && responseCode < 300) {
                ;
            } else {
                // 406
                Exception e = new Exception();
                throw e;
//                logger.info("Response Code:: " + responseCode);
//                logger.info("bodyElement :: " + bodyElement);
//                return null;
            }
            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                return response.toString();
            } else {
                logger.info("POST request not worked");
            }

        } catch (Exception e) {
            logger.info("Error sending REST request:" + e);
            throw e;
        }
        return null;
    }

    public static String[] splitIncludeEmpty(String inputStr, char delimiter) {
        if (inputStr == null) {
            return null;
        }
        if (inputStr.charAt(inputStr.length() - 1) == delimiter) {
            // the 000webhostapp always add extra ~ at the end see the source
            inputStr += "End";
            String[] tempString = inputStr.split("" + delimiter);
            int size = tempString.length - 1;
            String[] outString = new String[size];
            for (int i = 0; i < size; i++) {
                outString[i] = tempString[i];
            }
            return outString;
        }
        return inputStr.split("" + delimiter);
    }

//////////////////////////
//    private String sendRequest_Process_Ms_sql(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams)
//            throws Exception {
//
//        String URLPath = getURL_PATH() + subResourcePath;
//
//        String webResourceString = "";
//        // assume only one param
//        if (queryParams != null && !queryParams.isEmpty()) {
//            for (String key : queryParams.keySet()) {
//                webResourceString = "?" + key + "=" + queryParams.get(key);
//            }
//        }
//        URLPath += webResourceString;
//
//        BufferedReader in = null;
//        String resultString = null;
//        try {
//            HttpClient client = new DefaultHttpClient();
//            if (CKey.PROXY == true) {
//                //////Add Proxy 
//                HttpHost proxy = new HttpHost(ServiceAFweb.PROXYURL, 8080, "http");
//                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//            }
//            HttpPost requestPost = new HttpPost(URLPath);
//
//            List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
//            if (bodyParams != null && !bodyParams.isEmpty()) {
//                for (String key : bodyParams.keySet()) {
//                    String bodyElement = bodyParams.get(key);
//                    bodyElement = bodyElement.replaceAll("&", "-");
//                    bodyElement = bodyElement.replaceAll("%", "%25");
//                    postParameters.add(new BasicNameValuePair(key, bodyElement));
//                }
//            }
//            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParameters);
//
//            requestPost.setEntity(formEntity);
//
//            HttpResponse response = client.execute(requestPost);
//            in = new BufferedReader(
//                    new InputStreamReader(
//                            response.getEntity().getContent()));
//
//            StringBuffer sb = new StringBuffer("");
//            String line = "";
//            String NL = System.getProperty("line.separator");
//            while ((line = in.readLine()) != null) {
//                sb.append(line + NL);
//            }
//            in.close();
//
//            resultString = sb.toString();
//            return resultString;
//
//        } catch (Exception e) {
//            // Do something about exceptions
//            e.printStackTrace();
//            throw new Exception("Error send REST request");
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//////////////////////////
//    private String sendRequest_remotesql(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams) {
//        try {
//            if (subResourcePath.indexOf("https") != -1) {
//                return this.https_sendRequest_Process_Ssns(method, subResourcePath, queryParams, bodyParams);
//            }
//            return this.http_sendRequest_Process_Ssns(method, subResourcePath, queryParams, bodyParams);
//        } catch (Exception ex) {
////            Logger.getLogger(SsnsService.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return null;
//    }
//
//    private String https_sendRequest_Process_Ssns(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams)
//            throws Exception {
//        try {
//
//            String URLPath = subResourcePath;
//
//            String webResourceString = "";
//            // assume only one param
//            if (queryParams != null && !queryParams.isEmpty()) {
//                for (String key : queryParams.keySet()) {
//                    webResourceString = "?" + key + "=" + queryParams.get(key);
//                }
//            }
//
//            String bodyElement = "";
//            if (bodyParams != null) {
//                bodyElement = new ObjectMapper().writeValueAsString(bodyParams);
//            }
//
//            URLPath += webResourceString;
//            URL request = new URL(URLPath);
//
//            HttpsURLConnection con = null; //(HttpURLConnection) request.openConnection();
//
//            if (CKey.PROXY == true) {
//                //////Add Proxy 
//                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ServiceAFweb.PROXYURL, 8080));
//                con = (HttpsURLConnection) request.openConnection(proxy);
//                //////Add Proxy 
//            } else {
//                con = (HttpsURLConnection) request.openConnection();
//            }
//
////            if (URLPath.indexOf(":8080") == -1) {
////            String authStr = "APP_SELFSERVEUSGBIZSVC" + ":" + "soaorgid";
////            // encode data on your side using BASE64
////            byte[] bytesEncoded = Base64.encodeBase64(authStr.getBytes());
////            String authEncoded = new String(bytesEncoded);
////            con.setRequestProperty("Authorization", "Basic " + authEncoded);
////            }
//            if (method.equals(METHOD_POST)) {
//                con.setRequestMethod("POST");
//            } else if (method.equals(METHOD_GET)) {
//                con.setRequestMethod("GET");
//            }
//            con.setRequestProperty("User-Agent", USER_AGENT);
////            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
//            con.setRequestProperty("Content-Type", "application/json");
//            con.setRequestProperty("Accept", "application/json");
//
//            if (method.equals(METHOD_POST)) {
//
////                con.setRequestMethod("POST");
////                con.addRequestProperty("Accept", "application/json");
////                con.addRequestProperty("Connection", "close");
////                con.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
////                con.addRequestProperty("Content-Length", String.valueOf(bodyElement.length()));
////                con.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
//                con.setDoInput(true);
//                // For POST only - START                
//                con.setDoOutput(true);
//                OutputStream os = con.getOutputStream();
//                byte[] input = bodyElement.getBytes("utf-8");
//                os.write(input, 0, input.length);
//                os.flush();
//                os.close();
//                // For POST only - END
//            }
//
//            int responseCode = con.getResponseCode();
//            if (responseCode != 200) {
//                System.out.println("Response Code:: " + responseCode);
//            }
//            if (responseCode >= 200 && responseCode < 300) {
//                ;
//
//            } else {
////                System.out.println("Response Code:: " + responseCode);
////                System.out.println("bodyElement :: " + bodyElement);
//                return null;
//            }
//
//            if (responseCode == HttpURLConnection.HTTP_OK) { //success
//                BufferedReader in = new BufferedReader(new InputStreamReader(
//                        con.getInputStream()));
//                String inputLine;
//
//                StringBuffer response = new StringBuffer();
//
//                while ((inputLine = in.readLine()) != null) {
//
//                    response.append(inputLine);
//                }
//                in.close();
//                // print result
//                return response.toString();
//            } else {
//                logger.info("POST request not worked");
//            }
//
//        } catch (Exception e) {
////            logger.info("Error sending REST request:" + e);
//            throw e;
//        }
//        return null;
//    }
//
//    private String http_sendRequest_Process_Ssns(String method, String subResourcePath, Map<String, String> queryParams, Map<String, String> bodyParams)
//            throws Exception {
//        try {
//
////            String URLPath = subResourcePath;
//            String URLPath = getURL_PATH() + subResourcePath;
//            String webResourceString = "";
//            // assume only one param
//            if (queryParams != null && !queryParams.isEmpty()) {
//                for (String key : queryParams.keySet()) {
//                    webResourceString = "?" + key + "=" + queryParams.get(key);
//                }
//            }
//
//            String bodyElement = "";
//            if (bodyParams != null) {
//                bodyElement = new ObjectMapper().writeValueAsString(bodyParams);
//            }
//
//            URLPath += webResourceString;
//            URL request = new URL(URLPath);
//
//            HttpURLConnection con = null; //(HttpURLConnection) request.openConnection();
//
//            if (CKey.PROXY == true) {
//                //////Add Proxy 
//                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ServiceAFweb.PROXYURL, 8080));
//                con = (HttpURLConnection) request.openConnection(proxy);
//                //////Add Proxy 
//            } else {
//                con = (HttpURLConnection) request.openConnection();
//            }
//
////            if (URLPath.indexOf(":8080") == -1) {
////            String authStr = "APP_SELFSERVEUSGBIZSVC" + ":" + "soaorgid";
////            // encode data on your side using BASE64
////            byte[] bytesEncoded = Base64.encodeBase64(authStr.getBytes());
////            String authEncoded = new String(bytesEncoded);
////            con.setRequestProperty("Authorization", "Basic " + authEncoded);
////            }
//            if (method.equals(METHOD_POST)) {
//                con.setRequestMethod("POST");
//            } else if (method.equals(METHOD_GET)) {
//                con.setRequestMethod("GET");
//            }
//            con.setRequestProperty("User-Agent", USER_AGENT);
////            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
////            con.setRequestProperty("Content-Type", "application/json; utf-8");
//            con.setRequestProperty("Content-Type", "application/json");
//            con.setRequestProperty("Accept", "application/json");
//
//            if (method.equals(METHOD_POST)) {
//                con.setDoOutput(true);
//                try (OutputStream os = con.getOutputStream()) {
//                    byte[] input = bodyElement.getBytes("utf-8");
//                    os.write(input, 0, input.length);
//                    os.flush();
//                    os.close();
//                }
//
//            }
//
//            int responseCode = con.getResponseCode();
//            if (responseCode != 200) {
//                System.out.println("Response Code:: " + responseCode);
//            }
//            if (responseCode >= 200 && responseCode < 300) {
//                ;
//            } else {
////                System.out.println("Response Code:: " + responseCode);
////                System.out.println("bodyElement :: " + bodyElement);
//                return null;
//            }
//            if (responseCode == HttpURLConnection.HTTP_OK) { //success
//                BufferedReader in = new BufferedReader(new InputStreamReader(
//                        con.getInputStream()));
//                String inputLine;
//
//                StringBuffer response = new StringBuffer();
//
//                while ((inputLine = in.readLine()) != null) {
//
//                    response.append(inputLine);
//                }
//                in.close();
//                // print result
//                return response.toString();
//            } else {
//                logger.info("POST request not worked");
//            }
//
//        } catch (Exception e) {
////            logger.info("Error sending REST request:" + e);
//            throw e;
//        }
//        return null;
//    }
}
