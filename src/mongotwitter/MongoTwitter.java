/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mongotwitter;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;

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
    
    public void tweet(String body) {
        final ObjectId tweet_id = new ObjectId();
        final Date time = new Date();
        MongoCollection<Document> tweets = db.getCollection("tweets");
        MongoCollection<Document> userline = db.getCollection("userline");
        MongoCollection<Document> timeline = db.getCollection("timeline");
        MongoCollection<Document> followers = db.getCollection("followers");
        
        Document tweetDoc = new Document("tweet_id",tweet_id)
                .append("username",nick)
                .append("body",body);
        
        Document userDoc = new Document("username",nick)
                .append("time",time)
                .append("tweet_id",tweet_id);
        
        List<Document> timelineList = new ArrayList<Document>();
        List<Document> followerList = followers.find(eq("username",nick)).into(new ArrayList<Document>());
        for(Document doc : followerList) {
            String follower = (String) doc.get("follower");
            Document timeDoc = new Document("username",follower)
                    .append("time",time)
                    .append("tweet_id",tweet_id);
            timelineList.add(timeDoc);
        }
        
        tweets.insertOne(tweetDoc);
        userline.insertOne(userDoc);
        timeline.insertMany(timelineList);
        
        System.out.println("You tweeted \""+body+"\" at "+time);        
    }
    
    public void showUserline(String username) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc==null) {
            System.out.println("Show userline failed : Username does not exist");
        }
        else {
            MongoCollection<Document> userline = db.getCollection("userline");
            List<Document> userlineList = userline.find(eq("username",username)).into(new ArrayList<Document>());
            if(userlineList.isEmpty()) {
                System.out.println(username+"'s userline is empty");
            }
            else {
                MongoCollection<Document> tweets = db.getCollection("tweets");
                List<Date> timeList = new ArrayList<Date>();
                List<String> bodyList = new ArrayList<String>();
                for(Document doc : userlineList) {
                    Date time = (Date) doc.get("time");
                    ObjectId tweet_id = (ObjectId) doc.get("tweet_id");
                    Document tweetDoc = tweets.find(eq("tweet_id",tweet_id)).first();
                    String body = (String) tweetDoc.get("body");

                    timeList.add(time);
                    bodyList.add(body);
                }

                for(int i=0; i<timeList.size(); i++) {
                    System.out.println("("+timeList.get(i)+") "+bodyList.get(i));
                }
            }
        }
    }
    
    public void showTimeline(String username) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc==null) {
            System.out.println("Show timeline failed : Username does not exist");
        }
        else {
            MongoCollection<Document> timeline = db.getCollection("timeline");
            List<Document> timelineList = timeline.find(eq("username",username)).into(new ArrayList<Document>());
            if(timelineList.isEmpty()) {
                System.out.println(username+"'s timeline is empty");
            }
            else {
                MongoCollection<Document> tweets = db.getCollection("tweets");
                List<Date> timeList = new ArrayList<Date>();
                List<String> nameList = new ArrayList<String>();
                List<String> bodyList = new ArrayList<String>();
                for(Document doc : timelineList) {
                    Date time = (Date) doc.get("time");
                    ObjectId tweet_id = (ObjectId) doc.get("tweet_id");
                    Document tweetDoc = tweets.find(eq("tweet_id",tweet_id)).first();
                    String name = (String) tweetDoc.get("username");
                    String body = (String) tweetDoc.get("body");
                    
                    timeList.add(time);
                    nameList.add(name);
                    bodyList.add(body);
                }
                
                for(int i=0; i<timeList.size(); i++) {
                    System.out.println("("+timeList.get(i)+") "+nameList.get(i)+" : "+bodyList.get(i));
                }
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
        mt.login("beta","1234");
        mt.follow("tegar");
//        mt.tweet("hahaha");
          mt.showTimeline("beta");
    }
}
