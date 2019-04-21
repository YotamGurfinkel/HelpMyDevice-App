package com.yotam.customer.helpmydevice;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Post {
    private String title;
    private String email;
    private String description;
    private boolean edited;
    private String img;
    private long timeStamp;
    private List<Comment> comments;
    private String userId;
    private boolean isSolved;
    private String postId;

    public Post() {

    }

    public Post(String title, String description, String img, String userId, String postId, String email) {
        this.title = title;
        this.description = description;
        this.img = img;
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
        this.userId = userId;
        this.postId = postId;
        this.comments = new ArrayList<>();
        this.isSolved = false;
        this.edited = false;
        this.email = email;
    }

    public Post(String title, String description, String userId, String postId, String email){
        this.title = title;
        this.description = description;
        this.img = null;
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
        this.userId = userId;
        this.postId = postId;
        this.comments = new ArrayList<>();
        this.edited = false;
        this.isSolved = false;
        this.email = email;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public void setCommentsList(List<Comment> comments){
        this.comments = comments;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public void updateTime(){
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
    }

    public Long getTimestamp(){
        return this.timeStamp;
    }

    public void setTimestamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public void addComment(Comment comment){
        if(this.comments == null)
            this.comments = new ArrayList<>();
        this.comments.add(comment);
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    public boolean isSolved() {
        return isSolved;
    }

    public void setSolved(boolean solved) {
        isSolved = solved;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Post{" +
                "title='" + title + '\'' +
                ", email='" + email + '\'' +
                ", description='" + description + '\'' +
                ", img='" + img + '\'' +
                ", timeStamp=" + timeStamp +
                ", comments=" + comments +
                ", userId='" + userId + '\'' +
                ", isSolved=" + isSolved +
                ", postId='" + postId + '\'' +
                '}';
    }
}
