package com.cloud.google.config;

import java.util.Date;

public class Configuration {

  private String URL;
  private String product;
  private String iconURL;

  private Date lastUpdate;

  public Configuration() {
  }

  public Configuration(String URL, String product, String iconURL, Date lastUpdate) {
    this.URL = URL;
    this.product = product;
    this.iconURL = iconURL;
    this.lastUpdate = lastUpdate;
  }

  public String getURL() {
    return URL;
  }

  public void setURL(String URL) {
    this.URL = URL;
  }

  public String getProduct() {
    return product;
  }

  public void setProduct(String product) {
    this.product = product;
  }

  public String getIconURL() {
    return iconURL;
  }

  public void setIconURL(String iconURL) {
    this.iconURL = iconURL;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }
}
