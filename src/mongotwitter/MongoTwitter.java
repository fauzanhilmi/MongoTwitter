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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private final String address = "167.205.35.19";
    private final String database = "fauzan";
    private MongoDatabase db = null;
    
    private String nick;
    
    public MongoTwitter() {
        String url = "mongodb://"+address;
        MongoClientURI uri = new MongoClientURI(url);
        MongoClient mc = new MongoClient(uri);
        db = mc.getDatabase(database);
    }
    
    public boolean signup(String username, String password) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc!=null) {
            System.out.println("* Signup failed : Username already exists");
            return false;
        }
        else {
            Document doc = new Document("username",username)
                    .append("password",password);
            users.insertOne(doc);
            System.out.println("* User @"+username+" is succesfully signed up");
            //autologin
            nick = username;
            System.out.println("* Welcome to MongoTwitter, @"+username+"!");
            return true;
        }
    }
    
    public boolean login(String username, String password) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(and(eq("username",username),eq("password",password))).first();
        if(oldDoc==null) {
            System.out.println("* Login failed : Either username does not exist or the password didn't match");
            return false;
        }
        else {
            nick = username;
            System.out.println("* Login succeed");
            System.out.println("* Welcome back, @"+username+"!");
            return true;
        }
    }
    
    public void follow(String followed) {
        if(followed.equals(nick)) {
            System.out.println("* Follow failed : You cannot follow yourself");
        }
        else {
            MongoCollection<Document> users = db.getCollection("users");
            Document oldDoc = users.find(eq("username",followed)).first();
            if(oldDoc==null) {
                System.out.println("* Follow failed : Username does not exist");
            }
            else {
                MongoCollection<Document> friends = db.getCollection("friends");
                Document frDoc = friends.find(and(eq("username",nick),eq("friend",followed))).first();
                if(frDoc!=null) {
                    System.out.println("* Follow failed : You already followed @"+followed);
                }
                else {
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
                    System.out.println("* You successfully followed "+followed);
                }
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
        
        List<Document> timelineList = new ArrayList<>();
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
        
        System.out.println("* You tweeted \""+body+"\" at "+time);        
    }
    
    public void showUserline(String username) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc==null) {
            System.out.println("* Show userline failed : Username does not exist");
        }
        else {
            MongoCollection<Document> userline = db.getCollection("userline");
            List<Document> userlineList = userline.find(eq("username",username)).into(new ArrayList<Document>());
            if(userlineList.isEmpty()) {
                System.out.println("* "+username+"'s userline is empty");
            }
            else {
                MongoCollection<Document> tweets = db.getCollection("tweets");
                List<Date> timeList = new ArrayList<>();
                List<String> bodyList = new ArrayList<>();
                for(Document doc : userlineList) {
                    Date time = (Date) doc.get("time");
                    ObjectId tweet_id = (ObjectId) doc.get("tweet_id");
                    Document tweetDoc = tweets.find(eq("tweet_id",tweet_id)).first();
                    String body = (String) tweetDoc.get("body");

                    timeList.add(time);
                    bodyList.add(body);
                }

                System.out.println("* @"+username+"'s userline");
                for(int i=0; i<timeList.size(); i++) {
                    System.out.println("["+timeList.get(i)+"] "+bodyList.get(i));
                }
            }
        }
    }
    
    public void showTimeline(String username) {
        MongoCollection<Document> users = db.getCollection("users");
        Document oldDoc = users.find(eq("username",username)).first();
        if(oldDoc==null) {
            System.out.println("* Show timeline failed : Username does not exist");
        }
        else {
            MongoCollection<Document> timeline = db.getCollection("timeline");
            List<Document> timelineList = timeline.find(eq("username",username)).into(new ArrayList<Document>());
            if(timelineList.isEmpty()) {
                System.out.println("* @"+username+"'s timeline is empty");
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
                
                System.out.println("* @"+username+"'s timeline");
                for(int i=0; i<timeList.size(); i++) {
                    System.out.println("["+timeList.get(i)+"] @"+nameList.get(i)+" : "+bodyList.get(i));
                }
            }
        }
    }
    
    public void printMenu1() {
        System.out.println("* Welcome to MongoTwitter!");
        System.out.println("* To proceed, use any of available commands below");
        System.out.println("> Type 'SIGNUP <username> <password>' to sign up for new user");
        System.out.println("> Type 'LOGIN <username> <password>' to login for existing user");
        System.out.println("> Type 'EXIT' to quit");
    }
    
    public void printMenu2() {
        System.out.println("* To proceed, use any of available commands below");        
        System.out.println("> Type 'FOLLOW <username>' to follow someone");
        System.out.println("> Type 'TWEET <tweet>' to tweet something");
        System.out.println("> Type 'USERLINE <username>' to show someone's userline");
        System.out.println("> Type 'TIMELINE <username>' to show someone's timeline");
        System.out.println("> Type 'EXIT' to quit");
    }
    
    public static void main(String[] args) throws IOException {
        MongoTwitter mt = new MongoTwitter();
        
        String input = null;
        String command = null;
        String unsplittedParams = null;
        boolean isLogin = false;
        mt.printMenu1();
        do {
            input = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
            if(input.isEmpty()) {
                System.out.println("* Try again");
            }
            else {
                String[] parameters = new String[0];
                int whitespaceIdx = input.indexOf(" ");
                
                if (whitespaceIdx > -1) {
                    command = input.substring(0, whitespaceIdx);
                    unsplittedParams = input.substring(whitespaceIdx + 1);
                    parameters = unsplittedParams.split(" ");
                } else {
                    command = input;
                }
                
                if (command.equalsIgnoreCase("SIGNUP") && parameters.length == 2) {
                    isLogin = mt.signup(parameters[0],parameters[1]);
                }
                else if (command.equalsIgnoreCase("LOGIN") && parameters.length == 2) {
                    isLogin = mt.login(parameters[0],parameters[1]);
                }
                else if(!command.equalsIgnoreCase("EXIT")) {
                    System.out.println("* The command is not recognized. Try again");
                }
            }    
        } while (!command.equalsIgnoreCase("EXIT") && !isLogin);
        
        if(isLogin) {
            mt.printMenu2();
            input = null;
            command  = null;
            do {
                input = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
                if(input.isEmpty()) {
                    System.out.println("* Try again");
                }
                else {
                    String[] parameters = new String[0];
                    int whitespaceIdx = input.indexOf(" ");
                    if (whitespaceIdx > -1) {
                        command = input.substring(0, whitespaceIdx);
                        unsplittedParams = input.substring(whitespaceIdx + 1);
                        parameters = unsplittedParams.split(" ");
                    } else {
                        command = input;
                    }
                    
                    if (command.equalsIgnoreCase("FOLLOW") && parameters.length == 1) {
                        mt.follow(parameters[0]);
                    }
                    else if (command.equalsIgnoreCase("TWEET") && parameters.length >= 1) {
                        mt.tweet(unsplittedParams);
                    }
                    else if(command.equalsIgnoreCase("USERLINE") && parameters.length == 1) {
                        mt.showUserline(parameters[0]);
                    }
                    else if(command.equalsIgnoreCase("TIMELINE") && parameters.length == 1) {
                        mt.showTimeline(parameters[0]);
                    }
                    else if(!command.equalsIgnoreCase("EXIT")) {
                        System.out.println("* The command is not recognized. Try again");
                    }
                }
            } while(!command.equalsIgnoreCase("EXIT"));
        }
        System.out.println("* Thankyou for using MongoTwitter");
        System.out.println("* Goodbye!");
    }
}
