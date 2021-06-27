/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.dbnndata;


import com.afweb.model.ConstantKey;
import com.afweb.model.stock.*;
import com.afweb.nn.NNInputDataObj;
import com.afweb.nn.NNInputOutObj;
import com.afweb.nnBP.NNBPservice;
import com.afweb.service.ServiceAFweb;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author eddyko
 */
public class NNetdataImp {

    protected static Logger logger = Logger.getLogger("StockInfoImp");
    private NNetdataDB nndatadb = new NNetdataDB();

    
    public void setNNetDataDataSource( DataSource dataSource, String URL) {
        nndatadb.setDataSource(dataSource, URL);
    }
    
    public String getAllNeuralNetDBSQL(String sql) {
        return nndatadb.getAllNeuralNetDBSQL(sql);
    }

    public String getAllNeuralNetDataDBSQL(String sql) {
        return nndatadb.getAllNeuralNetDataDBSQL(sql);
    }

    public int updateNeuralNetDataObject(String name, int stockId, NNInputDataObj objData) {
        NNInputOutObj obj = objData.getObj();
        String stData;
        try {
            stData = new ObjectMapper().writeValueAsString(obj);
            return insertNeuralNetDataObject(name, stockId, stData, objData.getUpdatedatel());
        } catch (JsonProcessingException ex) {
        }
        return 0;
    }

    public int deleteNeuralNetDataByBPname(String name) {
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
//        logger.info(">>>>>>>>> deleteNeuralNetDataByBPname " + name);
        return nndatadb.deleteNeuralNetData(name);
    }

    public int insertNeuralNetDataObject(AFneuralNetData neuralNetData) {
        return nndatadb.insertNeuralNetDataObject(neuralNetData);
    }

    public int insertNeuralNetDataObject(String name, int stockId, String data, long updatedatel) {
        return nndatadb.insertNeuralNetDataObject(name, stockId, data, updatedatel);
    }

    public int updateNeuralNetStatus0(String name, int status, int type) {
        return nndatadb.updateNeuralNetStatus0(name, status, type);
    }

    public int deleteNeuralNet0Table() {
        return nndatadb.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet1Table() {
        return nndatadb.deleteNeuralNet1Table();
    }

    public int deleteNeuralNet0Rel(String name) {
        return nndatadb.deleteNeuralNet0Rel(name);
    }

    public int deleteNeuralNet1(String name) {
        return nndatadb.deleteNeuralNet1(name);
    }

    public int deleteNeuralNetDataTable() {
        return nndatadb.deleteNeuralNetDataTable();
    }

    public int updateNeuralNetStatus1(String name, int status, int type) {
        return nndatadb.updateNeuralNetStatus1(name, status, type);
    }

    public int deleteNeuralNetData(String name) {
        return nndatadb.deleteNeuralNetData(name);
    }

    public int releaseNeuralNetBPObj(String name) {

        ///NeuralNetObj1 transition
        ///NeuralNetObj0 release
        String nameSt = nndatadb.getNeuralNetName1(name);
        if (nameSt != null) {

            AFneuralNet nnObj1 = getNeuralNetObjWeight1(name);
            NNBPservice nnTemp = new NNBPservice();
            nnTemp.createNet(nnObj1.getWeight());
            nnTemp.setInputpattern(null);
            nnTemp.setOutputpattern(null);
            String weightSt = nnTemp.getNetObjSt();
            int ret = setCreateNeuralNetObj0(name, weightSt);
            if (ret == 1) {
                nndatadb.updateNeuralNetStatus0(name, ConstantKey.OPEN, 0);

                setCreateNeuralNetObj1(name, "");
                return nndatadb.updateNeuralNetStatus1(name, ConstantKey.COMPLETED, 0);
            }
        }
        return 0;
    }

    // return 0 for error
    public int updateNeuralNetRef0(String name, ReferNameData refnameData) {
        String nameSt = "";

        try {
            nameSt = new ObjectMapper().writeValueAsString(refnameData);
            nameSt = nameSt.replaceAll("\"", "#");
        } catch (JsonProcessingException ex) {
            return 0;
        }
        return nndatadb.updateNeuralNetRef0(name, nameSt);
    }

    public int updateNeuralNetRef1(String name, ReferNameData refnameData) {
        String nameSt = "";

        try {
            nameSt = new ObjectMapper().writeValueAsString(refnameData);
            nameSt = nameSt.replaceAll("\"", "#");
        } catch (JsonProcessingException ex) {
            return 0;
        }
        return nndatadb.updateNeuralNetRef1(name, nameSt);
    }

    //desc
    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int length) {
        return nndatadb.getNeuralNetDataObj(name, length);
    }

    public ArrayList<AFneuralNetData> getNeuralNetDataObj(String name, int stockId, long updatedatel) {
        return nndatadb.getNeuralNetDataObj(name, stockId, updatedatel);
    }

    public int deleteNeuralNetDataObjById(int id) {
        return nndatadb.deleteNeuralNetDataObjById(id);
    }

    public AFneuralNet getNeuralNetObjWeight0(String name) {
        AFneuralNet nn = nndatadb.getNeuralNetObjWeight0(name);
//        if (CKey.WEIGHT_COMPASS == true) {
        if (nn != null) {
            String weightSt = nn.getWeight();
            if (weightSt != null) {
                if (weightSt.length() > 0) {
                    if (weightSt.indexOf("%1F%C2") != -1) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                    }
                    nn.setWeight(weightSt);
                }
            }
        }
//        }
        return nn;
    }

    public AFneuralNet getNeuralNetObjWeight1(String name) {
        AFneuralNet nn = nndatadb.getNeuralNetObjWeight1(name);
//        if (CKey.WEIGHT_COMPASS == true) {
        if (nn != null) {
            String weightSt = nn.getWeight();
            if (weightSt != null) {
                if (weightSt.length() > 0) {
                    if (weightSt.indexOf("%1F%C2") != -1) {
                        weightSt = ServiceAFweb.decompress(weightSt);
                    }
                    nn.setWeight(weightSt);
                }
            }
        }
//        }
        return nn;
    }

    public int setCreateNeuralNetObj0(String name, String weight) {

        return nndatadb.setCreateNeuralNetObj0(name, weight);
    }

    public int setCreateNeuralNetObjRef0(String name, String weight, String RefName) {
//        if (CKey.WEIGHT_COMPASS == true) {
//            if (weight != null) {
//                if (weight.length() > 0) {
//                    String weightSt = ServiceAFweb.compress(weight);
//                    weight = weightSt;
//                }
//            }
//        }
        return nndatadb.setCreateNeuralNetObRefj0(name, weight, RefName);
    }

    public int setCreateNeuralNetObj1(String name, String weight) {
//        if (CKey.WEIGHT_COMPASS == true) {
//            if (weight != null) {
//                if (weight.length() > 0) {
//                    String weightSt = ServiceAFweb.compress(weight);
//                    weight = weightSt;
//                }
//            }
//        }
        int ret = nndatadb.setCreateNeuralNetObj1(name, weight);
        if (ret == 1) {
            return nndatadb.updateNeuralNetStatus1(name, ConstantKey.OPEN, 0);
        }
        return 0;
    }

}
