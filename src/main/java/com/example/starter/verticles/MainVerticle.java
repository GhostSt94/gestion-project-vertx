package com.example.starter.verticles;

import com.example.starter.handler.AuthHandler;
import com.example.starter.handler.ClientsHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {

  public static final String PATH_FACTURE_FILES="src/main/resources/FactureFiles/";
  public static final String PATH_PROJECT_FILES="src/main/resources/ProjectFiles/";
  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    FileSystem fs= vertx.fileSystem();
    EventBus eb= vertx.eventBus();
    Router router=Router.router(vertx);

    try {
      SockJSBridgeOptions options = new SockJSBridgeOptions();
      PermittedOptions address = new PermittedOptions().setAddressRegex("[^\n]+");
      options.addInboundPermitted(address);
      options.addOutboundPermitted(address);

      SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
      router.mountSubRouter("/eventbus",sockJSHandler.bridge(options, be -> {
        if (be.type() == BridgeEventType.REGISTER) {
          System.out.println("sockJs: connected");
        }
        be.complete(true);
      }));
    }catch (Exception e){
      System.out.println(e.toString());
    }


    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create("http://localhost:8080")
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.PUT)
      .allowedMethod(HttpMethod.DELETE));

    //receive and save facture file in resources & DB (file_name)
    router.post("/facture/save/:factureId").handler(ctx -> {
      try {
        String facture_id = ctx.pathParam("factureId");
        Set<FileUpload> fileUploadSet = ctx.fileUploads();
        if (fileUploadSet == null || fileUploadSet.isEmpty()) {
          System.out.println("no file found");
        } else {
          for (FileUpload f : fileUploadSet) {
            fs.readFile(f.uploadedFileName(), buffer -> {
              String file_name = new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + "_" + f.fileName();
              fs.writeFile(PATH_FACTURE_FILES + file_name, buffer.result(), res -> {
                if (res.succeeded()) {
                  fs.delete(f.uploadedFileName());
                  JsonObject obj = new JsonObject()
                    .put("facture_id", facture_id)
                    .put("file_name", file_name);
                  eb.request("add.facture.file.db", obj, resp -> {
                    if (resp.succeeded()) {
                      ctx.response().setStatusCode(200).end("File Saved");
                    } else {
                      ctx.response().setStatusCode(400).end("Error saving file");
                    }
                  });
                } else {
                  ctx.response().setStatusCode(400).end("Error writing file");
                }
              });
            });
          }
        }
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //receive and save project file in resources & DB (file_name)
    router.post("/project/save/:projectId").handler(ctx -> {
      try {
        String project_id = ctx.pathParam("projectId");
        Set<FileUpload> fileUploadSet = ctx.fileUploads();
        if (fileUploadSet == null || fileUploadSet.isEmpty()) {
          System.out.println("no file found");
        } else {
          for (FileUpload f : fileUploadSet) {
            fs.readFile(f.uploadedFileName(), buffer -> {
              String file_name = new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + "_" + f.fileName();
              fs.writeFile(PATH_PROJECT_FILES + file_name, buffer.result(), res -> {
                if (res.succeeded()) {
                  fs.delete(f.uploadedFileName());
                  JsonObject obj = new JsonObject()
                    .put("project_id", project_id)
                    .put("file_name", file_name);
                  eb.request("add.project.file.db", obj, resp -> {
                    if (resp.succeeded()) {
                      fs.delete(PATH_PROJECT_FILES + resp.result().body());
                      ctx.response().setStatusCode(200).end("File Saved");
                    } else {
                      ctx.response().setStatusCode(400).end("Error saving file");
                    }
                  });
                } else {
                  ctx.response().setStatusCode(400).end("Error writing file");
                }
              });
            });
          }
        }
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });

    //return file to front using fileName
    router.get("/files/facture/:fileName").handler(ctx->{
      try {
        String file_name=ctx.pathParam("fileName");
        ctx.response().sendFile(PATH_FACTURE_FILES+file_name);
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    router.get("/files/project/:fileName").handler(ctx->{
      try {
        String file_name = ctx.pathParam("fileName");
        ctx.response().sendFile(PATH_PROJECT_FILES + file_name);
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });

    //Auth
    router.post("/auth/register").handler(AuthHandler::register);
    router.post("/auth/login").handler(AuthHandler::login);

    //clients
    router.get("/clients").handler(ClientsHandler::clients);
    router.post("/clients").handler(ClientsHandler::addClient);
    router.get("/clients/:id").handler(ClientsHandler::getClient);
    router.put("/clients/:id").handler(ClientsHandler::updateClient);
    router.delete("/clients/:id").handler(ClientsHandler::deleteClient);

    //get all projects from DB
    eb.consumer("get.project.all",msg -> {
      try {
        eb.request("get.project.all.db", "", res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "Error");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //get all projects from DB using query (Filter)
    eb.consumer("get.project.query",msg -> {
      try {
        JsonObject query = new JsonObject(msg.body().toString());
        eb.request("get.project.query.db", query, res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "Error");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //add a project to DB
    eb.consumer("add.project",msg -> {
      try {
        JsonObject data = (JsonObject) msg.body();
        eb.request("add.project.db", data, res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "failed");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //get a project using id
    eb.consumer("get.project",msg -> {
      try {
        eb.request("get.project.db", msg.body(), res -> {
          if (res.succeeded()) {
            if (res.result().body() == null)
              msg.fail(404, "Not found");

            msg.reply(res.result().body());
          } else {
            msg.fail(404, "Not found");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //update a project
    eb.consumer("update.project",msg -> {
      try {
        JsonObject data = (JsonObject) msg.body();
        eb.request("update.project.db", data, res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(400, "Error");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //delete a project and related file
    eb.consumer("delete.project",msg -> {
      try {
        eb.request("delete.project.db", msg.body(), res -> {
          if (res.succeeded()) {
            fs.delete(PATH_PROJECT_FILES + res.result().body());
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "Not found");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //delete project file
    eb.consumer("delete.project.file",msg -> {
      try {
        eb.request("delete.project.file.db", msg.body(), res -> {
          if (res.succeeded()) {
            fs.delete(PATH_PROJECT_FILES + res.result().body());
            msg.reply(res.result().body() + " removed");
          } else {
            msg.fail(404, "Not found");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //add Facture to DB
    eb.consumer("add.facture",msg -> {
      try {
        JsonObject data = (JsonObject) msg.body();
        eb.request("add.facture.db", data, res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "failed");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //delete facture and related file
    eb.consumer("delete.facture",msg -> {
      try {
        eb.request("delete.facture.db", msg.body(), res -> {
          if (res.succeeded()) {
            JsonObject response = (JsonObject) res.result().body();
            if (response.getString("file") != null) {
              fs.delete(PATH_FACTURE_FILES + response.getString("file"));
            }
            msg.reply(response);
          } else {
            msg.fail(404, "failed");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });
    //get all factures from DB
    eb.consumer("get.facture.all",msg -> {
      try {
        eb.request("get.facture.all.db", msg.body(), res -> {
          if (res.succeeded()) {
            msg.reply(res.result().body());
          } else {
            msg.fail(404, "Error");
          }
        });
      }catch (Exception e){
        System.out.println(e.toString());
      }
    });




    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
