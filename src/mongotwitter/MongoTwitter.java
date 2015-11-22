/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongotwitter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import java.util.Date;
import java.util.Set;
import org.bson.Document;

/**
 *
 * @author fauzanhilmi
 */
public class MongoTwitter {
    private String address = "167.205.35.19";
    private String database = "fauzan";
    private MongoDatabase db = null;
    
    private String nick;
    private boolean isLogin;
    
    public MongoTwitter() {
        String url = "mongodb://"+address;
        MongoClientURI uri = new MongoClientURI(url);
        MongoClient mc = new MongoClient(uri);
        db = mc.getDatabase(database);

        isLogin = false;
    }
    
    public void signup(String username, String password) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc!=null) {
            System.out.println("Signup failed : Username already exists");
        }
        else {
            Document doc = new Document("username",username)
                    .append("password",password);
            users.insertOne(doc);
            System.out.println("Username "+username+" is succesfully signed up");
            //autologin
            nick = username;
            isLogin = true;
            System.out.println("Welcome to the app, "+username+"!");
        }
    }
    
    public void login(String username, String password) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(and(eq("username",username),eq("password",password))).first();
        if(oldDoc==null) {
            System.out.println("Login failed : Either username does not exist or the password didn't match");
        }
        else {
            nick = username;
            isLogin = true;
            System.out.println("Login succeed");
            System.out.println("Welcome back, "+username+"!");
        }
    }
    
    public void follow(String followed) {
        if(followed.equals(nick)) {
            System.out.println("Follow failed : You cannot follow yourself");
        }
        else {
            MongoCollection<Document> users = db.getCollection("users");
            Document oldDoc = users.find(eq("username",followed)).first();
            if(oldDoc==null) {
                System.out.println("Follow failed : Username does not exist");
            }
            else {
                MongoCollection<Document> friends = db.getCollection("friends");
                MongoCollection<Document> followers = db.getCollection("followers");
                Date ts = new Date();
                Document friendDoc = new Document("username",nick)
                        .append("friend",followed)
                        .append("since",ts);
                Document followerDoc = new Document("username",followed)
                        .append("follower",nick)
                        .append("since", ts);
                friends.insertOne(friendDoc);
                followers.insertOne(followerDoc);
                System.out.println("You are successfully followed "+followed);
            }
        }
    }
    
    public void printMenu() {
        System.out.println("Welcome to MongoTwitter");
        System.out.println("Type any command below\n\n");        
        System.out.println("SIGNUP <username> <password>");
        System.out.println("FOLLOW <follower_username> <followed_username>");
        System.out.println("TWEET <username> <tweet>");
        System.out.println("USERLINE <username>");
        System.out.println("TIMELINE <username>");
        System.out.println("EXIT\n\n");
    }
    
    
    public static void main(String[] args) {
        MongoTwitter mt = new MongoTwitter();
        mt.login("fauzan","1234");
        mt.follow("tegar");
    }
}
