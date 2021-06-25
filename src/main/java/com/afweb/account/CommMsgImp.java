/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.account;

import com.afweb.model.ConstantKey;
import com.afweb.model.account.*;

import com.afweb.service.ServiceAFweb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author eddy
 */
public class CommMsgImp {

    protected static Logger logger = Logger.getLogger("CommMsgImp");
    AccountImp accountImp = new AccountImp();
    
    public int AddCommObjMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, String name, int type, CommData commDataObj) {
        try {
            return accountImp.addAccountCommMessage(accountObj, name, type, commDataObj);
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, String name, String messageData) {
        try {
            logger.info("> AddCommMessage  " + accountObj.getAccountname() + " " + messageData);
            return accountImp.addAccountMessage(accountObj, name, messageData);

        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommAPISignalMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr,
            String ESTtime, String symbol, String sig) {
        try {
            ArrayList<String> msgL = new ArrayList();
            msgL.add(ESTtime);
            msgL.add(symbol);
            msgL.add(sig);
            String messageData = new ObjectMapper().writeValueAsString(msgL);
            messageData = messageData.replaceAll("\"", "#");
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                logger.info("> AddCommMessage  " + accountObj.getAccountname() + " " + messageData);
                return accountImp.addAccountMessage(accountObj, ConstantKey.COM_SIGNAL, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommSignalMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                logger.info("> AddCommMessage  " + accountObj.getAccountname() + " " + messageData);
                return accountImp.addAccountMessage(accountObj, ConstantKey.COM_SIGNAL, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddEmailBillingCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                return accountImp.addAccountEmailMessage(accountObj, ConstantKey.COM_BILLMSG, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddEmailBillingCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddEmailCommMessage(ServiceAFweb serviceAFWeb, AccountObj accountObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                return accountImp.addAccountEmailMessage(accountObj, ConstantKey.COM_EMAIL, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddEmailCommMessage exception " + e.getMessage());
        }
        return 0;
    }

    public int AddCommPUBSUBMessage(ServiceAFweb serviceAFWeb, AccountObj accFundObj, TradingRuleObj tr, String messageData) {
        try {
            if (tr.getType() == ConstantKey.INT_TR_ACC) {
                return accountImp.addAccountPUBSUBMessage(accFundObj, ConstantKey.COM_PUB, messageData);
            }
        } catch (Exception e) {
            logger.info("> AddCommMessage exception " + e.getMessage());
        }
        return 0;
    }

}
