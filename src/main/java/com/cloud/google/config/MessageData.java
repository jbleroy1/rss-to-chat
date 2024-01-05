package com.cloud.google.config;

import com.cloud.google.ProductFeed;
import com.google.api.services.chat.v1.model.Message;

public class MessageData {

  private Message message;
  private ProductFeed productFeed;

  public MessageData() {
  }

  public MessageData(Message message, ProductFeed productFeed) {
    this.message = message;
    this.productFeed = productFeed;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public ProductFeed getProductFeed() {
    return productFeed;
  }

  public void setProductFeed(ProductFeed productFeed) {
    this.productFeed = productFeed;
  }
}
