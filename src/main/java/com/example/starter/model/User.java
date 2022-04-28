package com.example.starter.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.mongo.MongoUserUtil;

import java.util.List;

public class User implements io.vertx.ext.auth.User {

  @Override
  public JsonObject attributes() {
    return null;
  }

  @Override
  public io.vertx.ext.auth.User isAuthorized(Authorization authorization, Handler<AsyncResult<Boolean>> handler) {
    return null;
  }

  @Override
  public JsonObject principal() {
    return null;
  }

  @Override
  public void setAuthProvider(AuthProvider authProvider) {

  }

  @Override
  public io.vertx.ext.auth.User merge(io.vertx.ext.auth.User user) {
    return null;
  }
}
