package com.example.starter.handler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ClientsHandler {

  public static void clients(RoutingContext ctx){
    ctx.vertx().eventBus().request("get.client.all.db","",res -> {
      if (res.succeeded()){
        JsonArray arr= (JsonArray) res.result().body();
        ctx.response().setStatusCode(200).send(arr.toBuffer());
      }else {
        ctx.response().setStatusCode(404);
      }
    });
  }

  public static void addClient(RoutingContext ctx){
    JsonObject data=ctx.getBodyAsJson();
    ctx.vertx().eventBus().request("add.client.db",data,rep->{
      if (rep.succeeded()){
        ctx.response().setStatusCode(200).send(rep.result().body().toString());
      }else {
        ctx.response().setStatusCode(403).end(rep.cause().getMessage());
      }
    });
  }

  public static void getClient(RoutingContext ctx){
    String id=ctx.pathParam("id");
    ctx.vertx().eventBus().request("get.client.db",id,res -> {
      if (res.succeeded()){
        JsonObject obj= (JsonObject) res.result().body();
        ctx.response().setStatusCode(200).send(obj.toBuffer());
      }else {
        ctx.response().setStatusCode(404).end(res.cause().getMessage());
      }
    });
  }

  public static void updateClient(RoutingContext ctx){
    String id=ctx.pathParam("id");
    JsonObject data=ctx.getBodyAsJson().put("_id",id);
    ctx.vertx().eventBus().request("update.client.db",data,res -> {
      if (res.succeeded()){
        JsonObject obj= (JsonObject) res.result().body();
        ctx.response().setStatusCode(200).send(obj.toBuffer());
      }else {
        ctx.response().setStatusCode(404).end(res.cause().getMessage());
      }
    });
  }

  public static void deleteClient(RoutingContext ctx){
    String id=ctx.pathParam("id");
    ctx.vertx().eventBus().request("delete.client.db",id,res -> {
      if (res.succeeded()){
        JsonObject obj= (JsonObject) res.result().body();
        ctx.response().setStatusCode(200).send(obj.toBuffer());
      }else {
        ctx.response().setStatusCode(404).end(res.cause().getMessage());
      }
    });
  }
}
