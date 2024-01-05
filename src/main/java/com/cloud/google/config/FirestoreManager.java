package com.cloud.google.config;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import java.util.List;
import java.util.stream.Collectors;

public class FirestoreManager {

  private Firestore db;


  private static final String DBNAME = System.getenv("FIRESTORE_COLLECTION");
  ;

  private ApiFuture<QuerySnapshot> query;

  private List<Configuration> configurations;

  public FirestoreManager() {
    db = FirestoreOptions.getDefaultInstance().getService();
    query = db.collection(DBNAME).get();
    configurations = getAll();
  }

  public boolean udpateProduct(Configuration configuration) {
    ApiFuture<WriteResult> writeResultApiFuture = db.collection(DBNAME)
        .document(configuration.getProduct())
        .set(configuration);
    try {
      return writeResultApiFuture.get().getUpdateTime() != null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public List<Configuration> getAll() {
    if (configurations != null) {
      return configurations;
    }

    QuerySnapshot querySnapshot = null;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
    return documents.stream()
        .map(document -> {
          Configuration configuration = document.toObject(
              Configuration.class);
          configuration.setProduct(document.getId());
          return configuration;
        }).collect(Collectors.toList());
  }

  public Configuration getConfigByProductName(String name) {

    return configurations.stream().filter(configuration -> configuration.getProduct().equals(name))
        .findFirst().orElseThrow(() -> new RuntimeException(name + " is not recognized as a feed"));
  }


  public void close() throws Exception {
    if (db != null) {
      db.close();
    }

  }

}
