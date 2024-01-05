package com.cloud.google;


import com.cloud.google.config.Configuration;
import com.cloud.google.config.FirestoreManager;
import com.cloud.google.config.MessageData;
import com.google.api.services.chat.v1.model.Message;
import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private final static Logger LOG = LoggerFactory.getLogger(Main.class);
  private final static List<String> FEEDS = Arrays.stream(System.getenv("FEEDS").split(","))
      .toList();


  private final static String CHAT_URL = System.getenv("CHAT_URL");
  private static final Gson gson = new Gson();
  private static final HttpClient client = HttpClient.newHttpClient();

  private static final FirestoreManager firestoreManager = new FirestoreManager();

  public static void main(String[] args) throws Exception {
    LOG.info("Chat URL {}", CHAT_URL);
    LOG.info("FEEDS {}", FEEDS);
    List<Configuration> allFeeds;
    //Retreive all feeds from Env Variables
    if (FEEDS.size() == 1 && FEEDS.get(0).equals("ALL")) {
      allFeeds = firestoreManager.getAll();
    } else {
      allFeeds = FEEDS.stream().distinct()
          .map(s -> firestoreManager.getConfigByProductName(s)).collect(Collectors.toList());
    }

    //Map  configuration to product feed ( parse the feed)

    boolean result = allFeeds.stream().map(ProductFeed::new)
        .filter(productFeed ->
            productFeed.getSyndFeed().getPublishedDate().toInstant()
                .isAfter(productFeed.getProduct().getLastUpdate().toInstant()))
        .peek(productFeed -> productFeed.createCards())
        .flatMap(productFeed -> productFeed.getMessages().stream())
        .map(Main::sendNotification)
        .allMatch(bool -> bool);

    firestoreManager.close();
    if (result) {
      System.exit(0);
    } else {
      System.exit(-1);
    }

  }

  private static Boolean sendNotification(MessageData messageData) {

    Message message = messageData.getMessage();
    String json = gson.toJson(message);
    LOG.info(json);
    HttpRequest request = HttpRequest.newBuilder(
            URI.create(CHAT_URL))
        .header("accept", "application/json; charset=UTF-8")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
    HttpResponse<String> response = null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (response.statusCode() < 400) {
      Configuration product = messageData.getProductFeed().getProduct();
      product.setLastUpdate(messageData.getProductFeed().getSyndFeed().getPublishedDate());
      return firestoreManager.udpateProduct(product);
    }
    return false;
  }


}