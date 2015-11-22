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
    
    public MongoTwitter() {
        String url = "mongodb://"+address;
        MongoClientURI uri = new MongoClientURI(url);
        MongoClient mc = new MongoClient(uri);
        db = mc.getDatabase(database);
//        MongoIterable<String> colls = db.listCollectionNames();
//        for (String s : colls) {
//            System.out.println(s);
//        }
    }
    
    public void signup(String username, String password) {
        MongoCollection<Document> users = db.getCollection("users");
        Document doc = new Document("username",username)
                   .append("password",password);
        users.insertOne(doc);
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
        mt.signup("fauzan","hahahihi");
    }
}
