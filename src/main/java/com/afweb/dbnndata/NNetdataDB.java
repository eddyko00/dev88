/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.dbnndata;

import com.afweb.model.ConstantKey;
import com.afweb.model.stock.*;

import com.afweb.service.ServiceAFweb;

import com.afweb.service.*;

import com.afweb.util.CKey;
import com.afweb.util.TimeConvertion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.sql.DataSource;
import java.util.logging.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author eddyko
 */
public class NNetdataDB {

    protected static Logger logger = Logger.getLogger("NNetdataDB");

    private static JdbcTemplate jdbcTemplate;
    private static DataSource dataSource;
    private static String remoteURL = "";
    private ServiceRemoteDBnndata remoteDB = new ServiceRemoteDBnndata();

//    private StockInfoDB stockinfodb = new StockInfoDB();
    /**
     * @return the dataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * @return the jdbcTemplate
     */
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * @param jdbcTemplate the jdbcTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSource(DataSource dataSource, String URL) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.remoteURL = URL;
        this.dataSource = dataSource;
    }

////////////////////////////  
/////////////////////////////////////////////////
    public boolean cleanNNdataInfoDB() {
        try {
            processExecuteDB("drop table if exists dummynndata1");
            int result = initNNdataDB();
            if (result == -1) {
                return false;
            }
            processExecuteDB("drop table if exists dummynndata1");
            return true;
        } catch (Exception e) {
            logger.info("> cleanNNdataInfoDB Table exception " + e.getMessage());
        }
        return false;
    }

    public static String createDummyNNdatatable() {
        String sqlCMD = "";
        if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
            sqlCMD = "create table dummynndata1 (id int identity not null, primary key (id))";
        }
        if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
            sqlCMD = "create table dummynndata1 (id int(10) not null auto_increment, primary key (id))";
        }
        return sqlCMD;
    }

    public int initNNdataDB() {

        int total = 0;
        logger.info(">>>>> initNNdataDB Table creation");
        try {

            boolean initDBflag = false;
            if (initDBflag == true) {
                processExecuteDB("drop table if exists dummynndata1");
            }
            total = getCountRowsInTable(getJdbcTemplate(), "dummynndata1");
        } catch (Exception e) {
            logger.info("> initNNdataDB Table exception");
            total = -1;
        }
        if (total >= 0) {
            return 1;  // already exist
        }

        try {

            // sequency is important
            ArrayList dropTableList = new ArrayList();
            dropTableList.add("drop table if exists dummynndata1");
            dropTableList.add("drop table if exists neuralnet1");
            dropTableList.add("drop table if exists neuralnetdata");

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultDropList = ExecuteSQLArrayList(dropTableList);

            ArrayList createTableList = new ArrayList();
            if ((CKey.SQL_DATABASE == CKey.MSSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_MS_SQL)) {
                createTableList.add("create table dummynndata1 (id int identity not null, primary key (id))");
                createTableList.add("create table neuralnet1 (id int identity not null, name varchar(255) not null unique, refname varchar(255) not null, status int not null, type int not null, weight text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
                createTableList.add("create table neuralnetdata (id int identity not null, name varchar(255) not null, status int not null, type int not null, data text null, updatedatedisplay date null, updatedatel bigint not null, primary key (id))");
            }

            if ((CKey.SQL_DATABASE == CKey.DIRECT__MYSQL) || (CKey.SQL_DATABASE == CKey.REMOTE_PHP_MYSQL) || (CKey.SQL_DATABASE == CKey.LOCAL_MYSQL)) {
                createTableList.add("create table dummynndata1 (id int(10) not null auto_increment, primary key (id))");
                createTableList.add("create table neuralnet1 (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
                createTableList.add("create table neuralnetdata (id int(10) not null auto_increment, name varchar(255) not null, status int(10) not null, type int(10) not null, data text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
            }

            //must use this ExecuteSQLArrayList to exec one by one for 2 db 
            boolean resultCreate = ExecuteSQLArrayList(createTableList);

            logger.info("> initNNdataDB Done - result " + resultCreate);
            total = getCountRowsInTable(getJdbcTemplate(), "neuralnetdata");
            return 0;  // new database

        } catch (Exception e) {
            logger.info("> initNNdataDB Table exception " + e.getMessage());
        }
        return -1;
    }
////////////////////////////////////////////////////
    public int deleteNeuralNet0Table() {
        try {
            processExecuteDB("drop table if exists neuralnet");
            processExecuteDB("create table neuralnet (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null,status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
            return 1;
        } catch (Exception ex) {
        }
        return 0;
    }

    public int deleteNeuralNet1Table() {
        try {
            processExecuteDB("drop table if exists neuralnet1");
            processExecuteDB("create table neuralnet1 (id int(10) not null auto_increment, name varchar(255) not null unique, refname varchar(255) not null, status int(10) not null, type int(10) not null, weight text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
            return 1;
        } catch (Exception ex) {
        }
        return 0;
    }

    public int deleteNeuralNetDataTable() {

        try {
            processExecuteDB("drop table if exists neuralnetdata");
            processExecuteDB("create table neuralnetdata (id int(10) not null auto_increment, name varchar(255) not null, status int(10) not null, type int(10) not null, data text, updatedatedisplay date, updatedatel bigint(20) not null, primary key (id))");
            return 1;
        } catch (Exception ex) {
        }
        return 0;
    }

    public int updateNeuralNetRef0(String name, String refname) {
        try {
            String sqlCMD = "update neuralnet set refname='" + refname + "'" + " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> updateNeuralNetRef0 exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public int updateNeuralNetRef1(String name, String refname) {
        try {
            String sqlCMD = "update neuralnet1 set refname='" + refname + "'" + " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> updateNeuralNetRef1 exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public int updateNeuralNetStatus0(String name, int status, int type) {
        try {
            String sqlCMD = "update neuralnet set status=" + status + ", type=" + type + " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> updateNeuralNetStatus exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public int updateNeuralNetStatus1(String name, int status, int type) {
        try {
            String sqlCMD = "update neuralnet1 set status=" + status + ", type=" + type + " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> updateNeuralNetStatus exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public static String insertNeuralNet(String table, AFneuralNet newN) {

        newN.setUpdatedatedisplay(new java.sql.Date(newN.getUpdatedatel()));
        String sqlCMD = "insert into " + table + " (name, refname, status, type, weight, updatedatedisplay, updatedatel, id) VALUES "
                + "('" + newN.getName() + "','" + newN.getRefname() + "'," + newN.getStatus() + "," + newN.getType() + ",'" + newN.getWeight() + "'"
                + ",'" + newN.getUpdatedatedisplay() + "'," + newN.getUpdatedatel() + "," + newN.getId() + ")";
        return sqlCMD;
    }

    public int deleteNeuralNet0Rel(String name) {
        try {
            String deleteSQL = "delete from neuralnet where name='" + name + "'";
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteNeuralNet1 exception " + e.getMessage());
        }
        return 0;
    }

    public int deleteNeuralNet1(String name) {
        try {
            String deleteSQL = "delete from neuralnet1 where name='" + name + "'";
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteNeuralNet1 exception " + e.getMessage());
        }
        return 0;
    }

    public int deleteNeuralNetData(String name) {
        try {
            String deleteSQL = "delete from neuralnetdata where name='" + name + "'";
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteNeuralNetData exception " + e.getMessage());
        }
        return 0;
    }

    public static String insertNeuralNetData(String table, AFneuralNetData newN) {
        String dataSt = newN.getData();
        dataSt = dataSt.replaceAll("\"", "#");
        newN.setUpdatedatedisplay(new java.sql.Date(newN.getUpdatedatel()));
        String sqlCMD = "insert into " + table + " (name, status, type, data, updatedatedisplay, updatedatel, id) VALUES "
                + "('" + newN.getName() + "'," + newN.getStatus() + "," + newN.getType() + ",'" + dataSt + "'"
                + ",'" + newN.getUpdatedatedisplay() + "'," + newN.getUpdatedatel() + "," + newN.getId() + ")";
        return sqlCMD;
    }

    public int insertNeuralNetDataObject(AFneuralNetData nData) {
        try {
            String dataSt = nData.getData();
            dataSt = dataSt.replaceAll("\"", "#");
            String sqlCMD = "insert into neuralnetdata (name, status, type, data, updatedatedisplay, updatedatel) VALUES "
                    + "('" + nData.getName() + "'," + nData.getStatus() + "," + nData.getType() + ",'" + dataSt + "'"
                    + ",'" + new java.sql.Date(nData.getUpdatedatel()) + "'," + nData.getUpdatedatel() + ")";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> insertNeuralNetDataObject exception " + nData.getName() + " - " + e.getMessage());
        }
        return 0;
    }

    public int insertNeuralNetDataObject(String name, int stockId, String data, long updatedatel) {
        try {
            data = data.replaceAll("\"", "#");
            String sqlCMD = "insert into neuralnetdata (name, status, type, data, updatedatedisplay, updatedatel) VALUES "
                    + "('" + name + "'," + ConstantKey.OPEN + "," + stockId + ",'" + data + "'"
                    + ",'" + new java.sql.Date(updatedatel) + "'," + updatedatel + ")";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> insertNeuralNetDataObject exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    private int insertNeuralNetObject0(String name, String weight) {
        try {

            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
            String sqlCMD = "insert into neuralnet(name, refname, status, type, weight, updatedatedisplay, updatedatel) VALUES "
                    + "('" + name + "',''," + ConstantKey.OPEN + "," + ConstantKey.OPEN + ",'" + weight + "'"
                    + ",'" + new java.sql.Date(dateDefault.getTimeInMillis()) + "'," + dateDefault.getTimeInMillis() + ")";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> insertNeuralNetObject exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    private int insertNeuralNetObjectRef0(String name, String weight, String refName) {
        try {

            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
            String sqlCMD = "insert into neuralnet(name, refname, status, type, weight, updatedatedisplay, updatedatel) VALUES "
                    + "('" + name + "','" + refName + "'," + ConstantKey.OPEN + "," + ConstantKey.OPEN + ",'" + weight + "'"
                    + ",'" + new java.sql.Date(dateDefault.getTimeInMillis()) + "'," + dateDefault.getTimeInMillis() + ")";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> insertNeuralNetObject exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    private int insertNeuralNetObject1(String name, String weight) {
        try {

            Calendar dateDefault = TimeConvertion.getDefaultCalendar();
            String sqlCMD = "insert into neuralnet1(name, refname, status, type, weight, updatedatedisplay, updatedatel) VALUES "
                    + "('" + name + "',''," + ConstantKey.OPEN + "," + 0 + ",'" + weight + "'"
                    + ",'" + new java.sql.Date(dateDefault.getTimeInMillis()) + "'," + dateDefault.getTimeInMillis() + ")";
            return processUpdateDB(sqlCMD);

        } catch (Exception e) {
            logger.info("> insertNeuralNetObject exception " + name + " - " + e.getMessage());
        }
        return 0;
    }

    public int setCreateNeuralNetObj0(String name, String weight) {
        try {
            if (weight == null) {
                weight = "";
            }
            weight = weight.trim();

            String nameSt = getNeuralNetName0(name);
            Calendar dateDefault = TimeConvertion.getCurrentCalendar();
            if (nameSt == null) {
                return insertNeuralNetObject0(name, weight);
            }

            String sqlCMD = "update neuralnet set weight='" + weight + "'";
            sqlCMD += ",updatedatedisplay='" + new java.sql.Date(dateDefault.getTimeInMillis()) + "', updatedatel=" + dateDefault.getTimeInMillis();
            sqlCMD += " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception ex) {
            logger.info("> setCreateNeuralNetObj0 exception " + ex.getMessage());
        }
        return 0;
    }

    public int setCreateNeuralNetObRefj0(String name, String weight, String refName) {
        try {
            if (weight == null) {
                weight = "";
            }
            weight = weight.trim();

            String nameSt = getNeuralNetName0(name);
            Calendar dateDefault = TimeConvertion.getCurrentCalendar();
            if (nameSt == null) {
                return insertNeuralNetObjectRef0(name, weight, refName);
            }

            String sqlCMD = "update neuralnet set weight='" + weight + "',refname='" + refName + "'";
            sqlCMD += ",updatedatedisplay='" + new java.sql.Date(dateDefault.getTimeInMillis()) + "', updatedatel=" + dateDefault.getTimeInMillis();
            sqlCMD += " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception ex) {
            logger.info("> setCreateNeuralNetObj0 exception " + ex.getMessage());
        }
        return 0;
    }

    public int setCreateNeuralNetObj1(String name, String weight) {
        try {
            if (weight == null) {
                weight = "";
            }
            weight = weight.trim();

            String nameSt = getNeuralNetName1(name);
            Calendar dateDefault = TimeConvertion.getCurrentCalendar();
            if (nameSt == null) {
                return insertNeuralNetObject1(name, weight);
            }

            String sqlCMD = "update neuralnet1 set weight='" + weight + "'";
            sqlCMD += ",type=" + 0;
            sqlCMD += ",updatedatedisplay='" + new java.sql.Date(dateDefault.getTimeInMillis()) + "', updatedatel=" + dateDefault.getTimeInMillis();
            sqlCMD += " where name='" + name + "'";
            return processUpdateDB(sqlCMD);

        } catch (Exception ex) {
            logger.info("> setCreateNeuralNetObj1 exception " + ex.getMessage());
        }
        return 0;
    }

    public String getNeuralNetName0(String name) {
        String sql = "select name as name from neuralnet where name='" + name + "'";
        ArrayList entries = getAllNameSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                String nameSt = (String) entries.get(0);
                return nameSt;
            }
        }
        return null;
    }

    public String getNeuralNetName1(String name) {
        String sql = "select name as name from neuralnet1 where name='" + name + "'";
        ArrayList entries = getAllNameSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                String nameSt = (String) entries.get(0);
                return nameSt;
            }
        }
        return null;
    }

    public String getAllNeuralNetDataDBSQL(String sql) {
        try {
            ArrayList<AFneuralNet> entries = getAllNeuralNetDataSQL(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (JsonProcessingException ex) {
        }
        return null;
    }

    public String getAllNeuralNetDBSQL(String sql) {
        try {
            ArrayList<AFneuralNet> entries = getAllNeuralNetSQL(sql);
            String nameST = new ObjectMapper().writeValueAsString(entries);
            return nameST;
        } catch (JsonProcessingException ex) {
        }
        return null;
    }

    private ArrayList getAllNeuralNetDataSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllNeuralNetDataSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return nnList;
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            List<AFneuralNetData> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public AFneuralNetData mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AFneuralNetData nn = new AFneuralNetData();
                    nn.setId(rs.getInt("id"));
                    nn.setName(rs.getString("name"));
                    nn.setStatus(rs.getInt("status"));
                    nn.setType(rs.getInt("type"));

                    String stData = rs.getString("data");
                    stData = stData.replaceAll("#", "\"");
                    nn.setData(stData);

                    nn.setUpdatedatedisplay(new java.sql.Date(rs.getDate("updatedatedisplay").getTime()));
                    nn.setUpdatedatel(rs.getLong("updatedatel"));

                    return nn;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllNeuralNetDataSQL exception " + e.getMessage());
        }
        return null;
    }

    public int deleteNeuralNetDataObjById(int id) {
        String deleteSQL = "delete from neuralnetdata where id=" + id;
        try {
            return processUpdateDB(deleteSQL);
        } catch (Exception e) {
            logger.info("> deleteNeuralNetDataObj exception " + e.getMessage());
        }
        return 0;
    }

    public ArrayList getNeuralNetDataObj(String name, int stockId, long updatedatel) {
        String sql = "select * from neuralnetdata where name='" + name + "' and type=" + stockId + " and updatedatel=" + updatedatel;
        ArrayList entries = getAllNeuralNetDataSQL(sql);
        return entries;
    }

    //desc
    public ArrayList getNeuralNetDataObj(String name, int length) {
        String sql = "select * from neuralnetdata where name='" + name + "'" + " order by updatedatel desc";

        sql = ServiceAFweb.getSQLLengh(sql, length);
        ArrayList entries = getAllNeuralNetDataSQL(sql);
        return entries;
    }

    private ArrayList getAllNeuralNetSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllNeuralNetSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return nnList;
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            List<AFneuralNet> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public AFneuralNet mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AFneuralNet nn = new AFneuralNet();
                    nn.setId(rs.getInt("id"));
                    nn.setName(rs.getString("name"));
                    nn.setRefname(rs.getString("refname"));
                    nn.setStatus(rs.getInt("status"));
                    nn.setType(rs.getInt("type"));
                    nn.setWeight(rs.getString("weight"));
                    nn.setUpdatedatedisplay(new java.sql.Date(rs.getDate("updatedatedisplay").getTime()));
                    nn.setUpdatedatel(rs.getLong("updatedatel"));

                    return nn;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllNeuralNetSQL exception " + e.getMessage());
        }
        return null;
    }

    public AFneuralNet getNeuralNetObjWeight0(String name) {
        String sql = "select * from neuralnet where name='" + name + "'";
        ArrayList entries = getAllNeuralNetSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                AFneuralNet nn = (AFneuralNet) entries.get(0);
                return nn;
            }
        }
        return null;
    }

    public AFneuralNet getNeuralNetObjWeightRefname1(String refname) {
        String sql = "select * from neuralnet1 where refname='" + refname + "'";
        ArrayList entries = getAllNeuralNetSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                AFneuralNet nn = (AFneuralNet) entries.get(0);
                return nn;
            }
        }
        return null;
    }

    public AFneuralNet getNeuralNetObjWeight1(String name) {
        String sql = "select * from neuralnet1 where name='" + name + "'";
        ArrayList entries = getAllNeuralNetSQL(sql);
        if (entries != null) {
            if (entries.size() == 1) {
                AFneuralNet nn = (AFneuralNet) entries.get(0);
                return nn;
            }
        }
        return null;
    }

////////////////////////////////////////////////////    
    
    private boolean ExecuteSQLArrayList(ArrayList SQLTran) {
        String SQL = "";
        try {
            for (int i = 0; i < SQLTran.size(); i++) {
                SQL = (String) SQLTran.get(i);
                logger.info("> ExecuteSQLArrayList " + SQL);
                processExecuteDB(SQL);
            }
            return true;
        } catch (Exception e) {
            logger.info("> ExecuteSQLArrayList exception " + e.getMessage());
        }
        return false;

    }

    public int updateSQLInfoArrayList(ArrayList SQLTran) {

        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int ret = remoteDB.getExecuteRemoteListDB_Mysql(SQLTran, remoteURL);
            if (ret == 0) {
                return 0;
            }
            return 1;
        }

        try {
            for (int i = 0; i < SQLTran.size(); i++) {
                String SQL = (String) SQLTran.get(i);
                getJdbcTemplate().update(SQL);

                if ((i % 100) == 0) {
                    ServiceAFweb.AFSleep();
                }
            }
            return 1;
        } catch (Exception e) {
            logger.info("> UpdateSQLlList exception " + e.getMessage());
        }
        return 0;

    }
/////////////////////////////////////////////////


    /// Need to fix the stockId to sym
    public ArrayList getNeuralNetDataObjByStockId(String name, int stockId, long updatedatel) {
        String sql = "select * from neuralnetdata where name='" + name + "' and type=" + stockId + " and updatedatel=" + updatedatel;
        ArrayList entries = getAllNeuralNetDataSQL(sql);
        return entries;
    }



    /////////////////////////////
    
    public ArrayList getAllNameSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllNameSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return nnList;
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            List<String> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                    String name = rs.getString("name");
                    return name;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllNameSQL exception " + e.getMessage());
        }
        return null;
    }
    
//    public ArrayList getAllSymbolSQL(String sql) {
//        if (ServiceAFweb.checkCallRemoteMysql() == true) {
//            ArrayList nnList;
//            try {
//                nnList = remoteDB.getAllSymbolSqlRemoteDB_RemoteMysql(sql, remoteURL);
//                return nnList;
//            } catch (Exception ex) {
//            }
//            return null;
//        }
//
//        try {
//            List<String> entries = new ArrayList<>();
//            entries.clear();
//            entries = this.jdbcTemplate.query(sql, new RowMapper() {
//                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
//                    String name = rs.getString("symbol");
//                    return name;
//                }
//            });
//            return (ArrayList) entries;
//        } catch (Exception e) {
//            logger.info("> getAllNameSQL exception " + e.getMessage());
//        }
//        return null;
//    }
//    
    public ArrayList getAllIdSQL(String sql) {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            ArrayList nnList;
            try {
                nnList = remoteDB.getAllIdSqlRemoteDB_RemoteMysql(sql, remoteURL);
                return nnList;
            } catch (Exception ex) {
            }
            return null;
        }

        try {
            List<String> entries = new ArrayList<>();
            entries.clear();
            entries = this.jdbcTemplate.query(sql, new RowMapper() {
                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                    String name = rs.getString("id");
                    return name;
                }
            });
            return (ArrayList) entries;
        } catch (Exception e) {
            logger.info("> getAllIdSQL exception " + e.getMessage());
        }
        return null;
    }

    public int getCountRowsInTable(JdbcTemplate jdbcTemplate, String tableName) throws Exception {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int count = remoteDB.getCountRowsRemoteDB_RemoteMysql(tableName, remoteURL);
            return count;
        }

        Integer result = jdbcTemplate.queryForObject("select count(0) from " + tableName, Integer.class);
        return (result != null ? result : 0);
    }

    public int processUpdateDB(String sqlCMD) throws Exception {
        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int ret = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD, remoteURL);
            return ret;
        }

//        logger.info("> processUpdateDB " + sqlCMD);
        getJdbcTemplate().update(sqlCMD);
        return 1;
    }

    public void processExecuteDB(String sqlCMD) throws Exception {
//        logger.info("> processExecuteDB " + sqlCMD);

        if (ServiceAFweb.checkCallRemoteMysql() == true) {
            int count = remoteDB.postExecuteRemoteDB_RemoteMysql(sqlCMD, remoteURL);
            return;
        }

        getJdbcTemplate().execute(sqlCMD);
    }

}
