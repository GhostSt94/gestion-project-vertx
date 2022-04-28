package com.example.starter.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.mongo.*;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

public class DbService extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    JsonObject config=new JsonObject()
      .put("db_name","testDB")
      .put("connection_string","mongodb://localhost");
    MongoClient client = MongoClient.create(vertx, config);
    final String COLLECTION="projects";
    final String COLLECTION_CLIENTS="clients";
    final String COLLECTION_USERS="users";

    EventBus eb=vertx.eventBus();
    //Project service
    eb.consumer("get.project.all.db",msg->{
      JsonObject query=new JsonObject().put("type","project");
      client.find(COLLECTION,query,res -> {
        if (res.succeeded()){
          JsonArray arr=new JsonArray();
          res.result().stream().forEach(obj->{
            arr.add(obj);
          });
          msg.reply(arr);
        }else {
          msg.fail(404,"Not found");
        }
      });
    });
    eb.consumer("get.project.query.db",msg->{
      JsonObject query= (JsonObject) msg.body();
      query.put("type","project");
      client.find(COLLECTION,query,res -> {
        if (res.succeeded()){
          if(res.result().size()!=0) {
            JsonArray arr = new JsonArray();
            res.result().stream().forEach(obj -> {
              arr.add(obj);
            });
            msg.reply(arr);
          }else {
            msg.fail(404,"Not found");
          }
        }else {
          msg.fail(404,"Not found");
        }
      });
    });
    eb.consumer("add.project.db",msg->{
      JsonObject data= (JsonObject) msg.body();
      client.insert(COLLECTION,data,res -> {
        if(res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Error");
        }
      });
    });
    eb.consumer("add.project.file.db",msg->{
      JsonObject message= (JsonObject) msg.body();
      JsonObject query=new JsonObject()
        .put("_id",message.getString("project_id"));
      JsonObject data=new JsonObject()
        .put("$set",new JsonObject()
          .put("file",message.getString("file_name")));

      client.findOneAndUpdate(COLLECTION,query,data,res -> {
        if(res.succeeded()){
          msg.reply(res.result().getString("file"));
        }else {
          msg.fail(404,"Error");
        }
      });
    });
    eb.consumer("get.project.db",msg->{
      String id= msg.body().toString();
      JsonObject query=new JsonObject().put("_id",id);
      client.findOne(COLLECTION,query,null,res -> {
        if (res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Not found");
        }
      });
    });
    eb.consumer("update.project.db",msg->{
      JsonObject data= (JsonObject) msg.body();
      JsonObject setData=new JsonObject()
        .put("$set",data);
      String id= data.getString("_id");
      JsonObject query=new JsonObject().put("_id",id);
      client.findOneAndUpdate(COLLECTION,query,setData,res -> {
        if (res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(400,"Error");
        }
      });
    });
    eb.consumer("delete.project.db",msg->{
      String id= msg.body().toString();
      JsonObject query=new JsonObject().put("_id",id);
      client.findOneAndDelete("projects",query,res -> {
        if (res.succeeded()){
          JsonObject query2=new JsonObject()
            .put("type","facture")
              .put("id_project",id);
          client.find(COLLECTION,query2,resData->{
            if (resData.succeeded()){
              resData.result().stream().forEach(obj -> {
                eb.send("delete.facture",obj.getString("_id"));
              });
            }
          });
          msg.reply(res.result().getString("file"));
        }else {
          msg.fail(404,"Not found");
        }
      });
    });
    eb.consumer("delete.project.file.db",msg->{
      JsonObject query=new JsonObject().put("_id",msg.body().toString());
      JsonObject setData=new JsonObject()
        .put("$unset",new JsonObject().put("file",""));
      client.findOneAndUpdate(COLLECTION,query,setData,res -> {
        if(res.succeeded()){
          msg.reply(res.result().getString("file"));
        }
      });
    });
    //Facture service
    eb.consumer("add.facture.db",msg->{
      JsonObject data= (JsonObject) msg.body();
      client.insert(COLLECTION,data,res -> {
        if(res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Error");
        }
      });
    });
    eb.consumer("add.facture.file.db",msg->{
      JsonObject message= (JsonObject) msg.body();
      JsonObject query=new JsonObject()
        .put("_id",message.getString("facture_id"));
      JsonObject data=new JsonObject()
        .put("$set",new JsonObject()
          .put("file",message.getString("file_name")));

      client.findOneAndUpdate(COLLECTION,query,data,res -> {
        if(res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Error");
        }
      });
    });
    eb.consumer("delete.facture.db",msg->{
      String id= msg.body().toString();
      JsonObject query=new JsonObject()
        .put("_id",id);
      client.findOneAndDelete(COLLECTION,query,res -> {
        if(res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Error");
        }
      });
    });
    eb.consumer("get.facture.all.db",msg->{
      String id=msg.body().toString();
      JsonObject query=new JsonObject()
        .put("type","facture")
        .put("id_project",id);
      client.find(COLLECTION,query, res -> {
        if (res.succeeded()){
          JsonArray arr=new JsonArray();
          res.result().stream().forEach(obj->{
            arr.add(obj);
          });
          msg.reply(arr);
        }else {
          msg.fail(404,"Not found");
        }
      });
    });
    //Client Service
    eb.consumer("add.client.db",msg->{
      JsonObject data= (JsonObject)msg.body();
      client.findOne(COLLECTION_CLIENTS,new JsonObject().put("nom",data.getString("nom")),null,res -> {
        if(res.succeeded() && res.result()==null){
            client.insert(COLLECTION_CLIENTS,data,res2 -> {
              if(res2.succeeded()){
                msg.reply(res2.result());
              }else {
                msg.fail(404,"Error");
              }
            });
        }else {
          msg.fail(403,"Client exist");
        }
      });
      /**/
    });
    eb.consumer("get.client.db",msg->{
      JsonObject query=new JsonObject().put("_id",msg.body().toString());
      client.findOne(COLLECTION_CLIENTS,query,null,res->{
        if(res.succeeded()&&res.result()!=null){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Client not Found");
        }
      });
    });
    eb.consumer("update.client.db",msg->{
      JsonObject data= (JsonObject) msg.body();
      JsonObject setData=new JsonObject()
        .put("$set",data);
      JsonObject query=new JsonObject().put("_id",data.getString("_id"));
      data.remove("_id");
      client.findOneAndUpdate(COLLECTION_CLIENTS,query,setData,res->{
        if(res.succeeded()){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Client not Found");
        }
      });
    });
    eb.consumer("delete.client.db",msg->{
      JsonObject query=new JsonObject().put("_id",msg.body().toString());
      client.findOneAndDelete(COLLECTION_CLIENTS,query,res->{
        if(res.succeeded()&&res.result()!=null){
          msg.reply(res.result());
        }else {
          msg.fail(404,"Client not Found");
        }
      });
    });
    eb.consumer("get.client.all.db",msg->{
      /*client.distinct("projects","client",String.class.getName(), res -> {
        if (res.succeeded()){
          msg.reply(new JsonObject().put("clients",res.result().getList()));
        }else {
          msg.fail(404,"ERROR");
        }
      });*/
      JsonObject fields=new JsonObject().put("nom",true);
      client.findWithOptions(COLLECTION_CLIENTS,new JsonObject(),new FindOptions().setFields(fields),res -> {
        if(res.succeeded()){
          JsonArray arr=new JsonArray(res.result());
          msg.reply(arr);
        }else {
          msg.fail(404,"ERROR");
        }
      });
    });


    //auth
    MongoAuthenticationOptions options = new MongoAuthenticationOptions();
    eb.consumer("register.user",msg->{
      JsonObject credentials= (JsonObject) msg.body();

      MongoUserUtil us=MongoUserUtil.create(client,options,null);
      us.createUser(credentials.getString("username"), credentials.getString("password"),res -> {
        if(res.succeeded()){
          System.out.println(res.result());
          msg.reply(res.result());
        }else {
          msg.fail(403,"Error registering");
        }
      });
      /*MongoAuthentication authenticationProvider =
        MongoAuthentication.create(client, options);
      authenticationProvider.authenticate(credentials,userAsyncResult -> {
        if(userAsyncResult.succeeded()){
          System.out.println(""+userAsyncResult.result().principal());
        }else {
          System.out.println(userAsyncResult.cause());
        }
      });*/
    });

    eb.consumer("login.user",msg-> {

      JsonObject credentials = (JsonObject) msg.body();

      MongoAuthentication authenticationProvider=MongoAuthentication.create(client,options);
      authenticationProvider.authenticate(credentials)
        .onSuccess(user -> {
          msg.reply(user.principal());
        })
        .onFailure(err -> {
          msg.fail(403,err.getMessage());
        });
    });
  }
}
