package com.yotam.customer.helpmydevice;

import android.text.TextUtils;

public class Customer {
    private String firstName;
    private String lastName;
    private String img;
    private String phoneNumber;

    public Customer(){
    }

    public Customer(String firstName, String lastName, String img, String phoneNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.img = img;
        this.phoneNumber = phoneNumber;
    }

    public Customer(String firstName, String lastName, String phoneNumber){
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName(){
        String fullName = TextUtils.isEmpty(lastName) ? firstName : firstName + " " + lastName;
        return fullName;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }
}
