/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.service.db;

import com.vpumlmodel.account.account;

/**
 *
 * @author eddy
 */
public class CustomerRDB {

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    private account[] account;
    private String id;
    private String username;
    private String password;
    private String type;
    private String status;
    private String substatus;
    private String startdate;
    private String firstname;
    private String lastname;
    private String email;
    private String payment;
    private String balance;
    private String portfolio;
    private String data;
    private String updatedatedisplay;
    private String updatedatel;

    /**
     * @return the account
     */
    public account[] getAccount() {
        return account;
    }

    /**
     * @param account the account to set
     */
    public void setAccount(account[] account) {
        this.account = account;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the substatus
     */
    public String getSubstatus() {
        return substatus;
    }

    /**
     * @param substatus the substatus to set
     */
    public void setSubstatus(String substatus) {
        this.substatus = substatus;
    }

    /**
     * @return the startdate
     */
    public String getStartdate() {
        return startdate;
    }

    /**
     * @param startdate the startdate to set
     */
    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the balance
     */
    public String getBalance() {
        return balance;
    }

    /**
     * @param balance the balance to set
     */
    public void setBalance(String balance) {
        this.balance = balance;
    }

    /**
     * @return the updatedatedisplay
     */
    public String getUpdatedatedisplay() {
        return updatedatedisplay;
    }

    /**
     * @param updatedatedisplay the updatedatedisplay to set
     */
    public void setUpdatedatedisplay(String updatedatedisplay) {
        this.updatedatedisplay = updatedatedisplay;
    }

    /**
     * @return the updatedatel
     */
    public String getUpdatedatel() {
        return updatedatel;
    }

    /**
     * @param updatedatel the updatedatel to set
     */
    public void setUpdatedatel(String updatedatel) {
        this.updatedatel = updatedatel;
    }

    
    /**
     * @return the payment
     */
    public String getPayment() {
        return payment;
    }

    /**
     * @param payment the payment to set
     */
    public void setPayment(String payment) {
        this.payment = payment;
    }

    /**
     * @return the portfolio
     */
    public String getPortfolio() {
        return portfolio;
    }

    /**
     * @param portfolio the portfolio to set
     */
    public void setPortfolio(String portfolio) {
        this.portfolio = portfolio;
    }
}
