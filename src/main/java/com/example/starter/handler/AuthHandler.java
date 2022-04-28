package com.example.starter.handler;

import io.vertx.ext.web.RoutingContext;

public class AuthHandler {

  public static void register(RoutingContext ctx){
    ctx.vertx().eventBus().request("register.user",ctx.getBodyAsJson(), rep->{
      if(rep.succeeded()){
        ctx.response().setChunked(true).setStatusCode(200).send(rep.result().body().toString());
      }else{
        ctx.response().setStatusCode(403).end(rep.cause().getMessage());
      }
    });
  }

  public static void login(RoutingContext ctx){
    ctx.vertx().eventBus().request("login.user",ctx.getBodyAsJson(), rep->{
      if(rep.succeeded()){
        ctx.response().setChunked(true).setStatusCode(200).send(rep.result().body().toString());
      }else{
        ctx.response().setStatusCode(403).end(rep.cause().getMessage());
      }
    });
  }
}
