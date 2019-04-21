package com.yotam.customer.helpmydevice;

import java.util.Calendar;

// Class defines Comment object
public class Comment{

    private String commenterUser; // String of commenter user id
    private String body;
    private String commenterEmail;
    private String currentPhone;
    private String img; // String of commenter image path
    private long timeStamp; // long of time of comment
    private String currentFullName; // String of commenter current full name
    private boolean isTechnician; // boolean determines if a technician commented or a user

    public Comment() { // Empty constructor due to firebase requirements
    }

    public Comment(String user, String img, String body, boolean isTechnician, String commenterEmail) {
        this.commenterUser = user;
        this.body = body;
        this.timeStamp = Calendar.getInstance().getTimeInMillis(); // gets current time im milliseconds
        this.isTechnician = isTechnician;
        this.img = img;
        this.commenterEmail = commenterEmail;
    }

    public Comment(String user, String body, boolean isTechnician, String commenterEmail) {
        this.commenterUser = user;
        this.body = body;
        this.timeStamp = Calendar.getInstance().getTimeInMillis(); // gets current time im milliseconds
        this.isTechnician = isTechnician;
        this.commenterEmail = commenterEmail;
    }



    public String getBody() {
        return body;
    }

    public String getCurrentPhone() {
        return currentPhone;
    }

    public void setCurrentPhone(String currentPhone) {
        this.currentPhone = currentPhone;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isTechnician() {
        return isTechnician;
    }

    public void setTechnician(boolean technician) {
        isTechnician = technician;
    }

    public void updateTime(){
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
    }

    public String getCommenterUser() {
        return commenterUser;
    }

    public void setCommenterUser(String commenterUser) {
        this.commenterUser = commenterUser;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getCurrentFullName() {
        return currentFullName;
    }

    public void setCurrentFullName(String currentFullName) {
        this.currentFullName = currentFullName;
    }

    public String getCommenterEmail() {
        return commenterEmail;
    }

    public void setCommenterEmail(String commenterEmail) {
        this.commenterEmail = commenterEmail;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commenterUser='" + commenterUser + '\'' +
                ", body='" + body + '\'' +
                ", commenterEmail='" + commenterEmail + '\'' +
                ", img='" + img + '\'' +
                ", timeStamp=" + timeStamp +
                ", currentFullName='" + currentFullName + '\'' +
                ", isTechnician=" + isTechnician +
                '}';
    }
}
