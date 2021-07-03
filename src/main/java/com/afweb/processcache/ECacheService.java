package com.afweb.processcache;

import com.afweb.model.stock.AFneuralNet;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author eddyko
 */
public class ECacheService {

    protected static Logger logger = Logger.getLogger("ECacheService");

    public static ECache cacheImp = new ECache();
    public static boolean cacheFlag = true;

    public static int TTL =5000;
    public int putNeuralNetObjWeight0(String name, AFneuralNet aFneuralNet) {
        cacheImp.put(name, aFneuralNet, TTL);
        return 1;
    }

    public AFneuralNet getNeuralNetObjWeight0(String name) {
        return cacheImp.get(name);
    }

}
