//package com.huoli.trip.central.web.config;
//
//
//import com.huoli.trip.common.util.ConfigGetter;
//import com.mongodb.MongoClientOptions;
//import com.mongodb.WriteConcern;
//import com.mongodb.client.MongoClients;
//import org.springframework.boot.autoconfigure.mongo.MongoProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.MongoDbFactory;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
//
//import java.net.URLEncoder;
//import java.net.UnknownHostException;
//import java.nio.charset.StandardCharsets;
//
//@Configuration
//public class MongoConfig {
//
//    private MongoTemplate createMongoTemplate(MongoProperties mongoProperties) throws UnknownHostException {
//        MongoClientOptions options = MongoClientOptions.builder()
//                .connectionsPerHost(1200).maxConnectionIdleTime(20000)
//                .writeConcern(WriteConcern.UNACKNOWLEDGED).build();
//        //MongoClient mongoClient = new MongoClient(mongoProperties.getHost(),options);
//        com.mongodb.client.MongoClient mongoClient = MongoClients.create(mongoProperties.getUri());
//        MongoDbFactory mongoDbFactory = new SimpleMongoClientDbFactory(mongoClient, mongoProperties.getDatabase());
//        return new MongoTemplate(mongoDbFactory);
//    }
//
//    private MongoProperties createMongoProperties(String prefix) {
//        String host = ConfigGetter.getByFileItemString(ConstConfig.MONGO_FILE, prefix + ".host");
//        Integer port = ConfigGetter.getByFileItemInteger(ConstConfig.MONGO_FILE, prefix + ".port");
//        String database = ConfigGetter.getByFileItemString(ConstConfig.MONGO_FILE, prefix + ".dbname");
//        String username = ConfigGetter.getByFileItemString(ConstConfig.MONGO_FILE, prefix + ".username");
//        String password = ConfigGetter.getByFileItemString(ConstConfig.MONGO_FILE, prefix + ".password");
//        String replicaSet = ConfigGetter.getByFileItemString(ConstConfig.MONGO_FILE, prefix + ".replicaSet");
//
//        String uri;
//        try {
//            uri = "mongodb://" + URLEncoder.encode(username, StandardCharsets.UTF_8.name()) + ":" + URLEncoder.encode(password, StandardCharsets.UTF_8.name()) + "@" + replicaSet + "/" + database;
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//        MongoProperties prop = new MongoProperties();
//        prop.setDatabase(database);
//        prop.setUri(uri);
//        return prop;
//    }
//
//    @Bean
//    public MongoProperties flightMongoProperties() {
//        return createMongoProperties("ticket.mongo");
//    }
//
//    @Bean(name = "flightMongoTemplate")
//    public MongoTemplate flightMongoTemplate() throws UnknownHostException {
//        MongoProperties mongoProperties = flightMongoProperties();
//        return createMongoTemplate(mongoProperties);
//    }
//
//}
