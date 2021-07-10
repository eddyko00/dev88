package com.afweb.util;

import com.afweb.nn.*;

public class CKey {

    //local pc
    public static String FileLocalPathTemp = "T:/Netbean/db/";
    public static String FileServerPathTemp = "/app/";

    public static final String ADMIN_USERNAME = "ADMIN1";
    public static final String FUND_MANAGER_USERNAME = "FUNDMGR";
    public static final String INDEXFUND_MANAGER_USERNAME = "INDEXMGR";
    public static final String API_USERNAME = "ADMAPI";
    public static final String AF_SYSTEM = "AFSYSTEM";
    public static final String G_USERNAME = "GUEST";
    public static final String E_USERNAME = "EDDY";
    public static final String MASK_PASS = "*****";

    public static final String MASTER_SRV = "MasterDBSRV";
    public static final String WEB_SRV = "WebSRV";
    //local pc
//    public static final boolean LocalPCflag = true; // true;

    //
    //////////////////////
    //
    public static boolean PROXY = false; //false; //true; 
    public static String PROXYURL_TMP = "webproxystatic-on.tslabc.tabceluabcs.com";
    public static boolean NN_DEBUG = false; //false; //true; 
    public static boolean UI_ONLY = false; //false Openshift; //true heroku;  
    /////////////////////
    /////////////////////
    /////////////////////
    public static boolean STATIC_STOCKH = true;      // must be true    
    public static boolean DELAY_RESTORE = false;  // true only for VM ware restore local
    public static boolean GET_STOCKHISTORY_SCREEN = false; //false //true    

    public static boolean backupFlag = false;
    public static boolean backupInfoFlag = false;
    public static boolean backupNNFlag = false;

    public static boolean restoreFlag = false;
    public static boolean restoreInfoFlag = false;
    public static boolean restoreNNFlag = false;

//////////////////    
    public static boolean hou3to1 = true; //false;      
    public static boolean hod1to4 = true; //false;   
    public static boolean dbinfonnflag = true; // true;
    public static String dbInfoNNURL = CKey.URL_PATH_HERO_4_DBDB_PHP + CKey.WEBPOST_HERO_4_PHP;
/////////////////    
    /////heroku
    /////heroku
    public static final String URL_PATH_HERO = "https://iiswebsrv.herokuapp.com";  // server timerhandler
    public static String WEBPOST_HERO_PHP = "/webgetresp.php";
    public static String URL_PATH_HERO_DBDB_PHP = "https://iiswebdb.herokuapp.com";
    /////heroku
    /////heroku
    public static final String URL_PATH_HERO_1 = "https://iiswebsrv1.herokuapp.com";  // server timerhandler
    public static String WEBPOST_HERO_1_PHP = "/webgetresp_1.php";
    public static String URL_PATH_HERO_1_DBDB_PHP = "https://iiswebdb1.herokuapp.com"; //    
    /////
    public static final String URL_PATH_HERO_2 = "https://iiswebsrv.herokuapp.com";  // server timerhandler
    public static String WEBPOST_HERO_2_PHP = "/webgetresp_2.php"; //freemysqlhosting
    public static String URL_PATH_HERO_2_DBDB_PHP = "https://iiswebdb.herokuapp.com";
    ////
    public static final String URL_PATH_HERO_4 = "http://ekphp1234.atwebpages.com";  // server timerhandler
    public static String WEBPOST_HERO_4_PHP = "/webgetresp_4.php"; //AWARDSPACE
    public static String URL_PATH_HERO_4_DBDB_PHP = "http://ekphp1234.atwebpages.com";
//
//
//***********    
//*********** 
    //////////////////////
//    public static final int REMOTE_MS_SQL = 3;////// do not use // http://eddyko00.freeasphost.net asp on freeasphost ma sql   
    // remember to update the application properties      
    public static final int LOCAL_MYSQL = 0; //jdbc:mysql://localhost:3306/db_sample     
    public static final int DIRECT__MYSQL = 1;   //jdbc:mysql://sql9.freesqldatabase.com:3306/sql9299052 direct mysql expire 3 days
    public static final int REMOTE_PHP_MYSQL = 2;
    public static final int REMOTE_PHP_1_MYSQL = 3;
    public static final int REMOTE_PHP_2_MYSQL = 4;
    public static final int REMOTE_PHP_3_MYSQL = 5;
    public static final int REMOTE_PHP_4_MYSQL = 6;

// default DB server and timerhandler
    public static int SQL_DATABASE = REMOTE_PHP_MYSQL;  //MYSQL direct db //REMOTE_MYSQL (for PHP DB proxy)    
    public static String SERVER_DB_URL = URL_PATH_HERO_DBDB_PHP;  // server timerhandler OTHER_DB1 = false;
    public static String SERVER_TIMMER_URL = URL_PATH_HERO;  // server timerhandler OTHER_DB1 = false;
/////////////////    
//    public static int SQL_DATABASE = REMOTE_PHP_1_MYSQL;  //MYSQL direct db //REMOTE_MYSQL (for PHP DB proxy)  
//    public static String SERVER_DB_URL = URL_PATH_HERO_1_DBDB_PHP;  // server timerhandler OTHER_DB1 = false;
//    public static String SERVER_TIMMER_URL = URL_PATH_HERO_1;  // serve    
//
//***********        
//*********** 
//**********    
//*********** 
//    public static final String SERVER_TIMER_URL = "https://iiswebtimer.herokuapp.com";  // server timerhandler    
    ////////////////////////////       
//https://www.thebalance.com/best-etfs-4173857
//https://www.etftrends.com/popular-etfs/
    public static String NN_version = "NNBP_V1";

    public static float SPLIT_VAL = (float) 1.5;
    public static int MONTH_SIZE = 16;
    public static int SHORT_MONTH_SIZE = 2;//3;
    public static double PREDICT_THRESHOLD = 0.6;
//    
    public static double NN1_ERROR_THRESHOLD = 0.156; //0.155; // 0.159; //0.172;  
    public static double NN2_ERROR_THRESHOLD = 0.138; //0.137; //0.155;// 
    public static double NN3_ERROR_THRESHOLD = 0.156;
    public static double NN30_ERROR_THRESHOLD = 0.227; //0.226; //0.228; //0.211;  

    public static double NN40_ERROR_THRESHOLD = 0.227;
//
    public static final int NN_OUTPUT_SIZE = 2;
    public static final int NN_INPUT_SIZE = 10;
    public static final int NN1_MIDDLE_SIZE = 110; //120; 

//    public static final boolean WEIGHT_COMPASS = false;    
    public static float iis_ver = (float) 1.1;

    // must match to the nnData and nn3Data version  make sure both 
    // must match to the nnData and nn3Data version  make sure both 
    public static String version = "0.1224";

    public static String NN1_WEIGHT_0 = NN1Data.TR_NN1_WEIGHT_0;
    public static String NN2_WEIGHT_0 = NN2Data.TR_NN2_WEIGHT_0;
    public static String NN3_WEIGHT_0 = NN3Data.TR_NN3_WEIGHT_0;

    public static String NN30_WEIGHT_0 = NN30Data.TR_NN30_WEIGHT_0;

    //////////////////////
    public static final int MSSQL = 1;/////// do not use //jdbc:sqlserver://sql.freeasphost.net\\MSSQL2016;databaseName=eddyko00_SampleDB

////////////////////////////  
////////////////////////////    
    public static final String COMMA = ",";
    public static final String MSG_DELIMITER = "~";
    public static final String QUOTE = "'";
    public static final String DASH = "-";
    public static final String DB_DELIMITER = "`";

    public static final int DATA6YEAR = 5 * 52 * 6;
    public static final int FAIL_STOCK_CNT = 160;

    public static final float TRADING_AMOUNT = 6000;
    public static final float TRADING_COMMISSION = 7;

    public static final String UA = "iabciswabcebemaiabcl";
    public static final String PA = "eabcddykabco100";
    public static final String UU = "eabcdabcdy.ko00@yahoo.ca";

////////////////////////////////////////////////////////////////////////////////    
    public CKey() {
    }

}
