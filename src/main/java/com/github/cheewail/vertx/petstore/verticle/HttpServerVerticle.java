package com.github.cheewail.vertx.petstore.verticle;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.api.model.Pet;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    private final Integer defaultPort = 8080;

    private PetService petService;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        logger.info("HttpServerVerticle starting...");
        super.start();

        petService = PetService.createProxy(vertx, "pet.service");

        createApiTempFile("petstore.yaml").onSuccess(file -> {
            RouterBuilder.create(vertx, file).onSuccess(routerBuilder -> {
                routerBuilder.operation("createPets").handler(this::createPets);
                routerBuilder.operation("listPets").handler(this::listPets);
                routerBuilder.operation("showPetById").handler(this::showPetById);

                Router router = routerBuilder.createRouter();
                healthCheckBuilder(router);

                ConfigRetriever configRetriever = ConfigRetriever.create(vertx,
                        new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("sys")));
                configRetriever.getConfig().onSuccess(config -> {
                    vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(config.getInteger("http.port", defaultPort), result -> {
                                if (result.succeeded()) {
                                    startPromise.complete();
                                    logger.info("HttpServerVerticle listening on port {}",
                                            config.getInteger("http.port", defaultPort));
                                } else {
                                    startPromise.fail(result.cause());
                                    logger.info("HttpServerVerticle failed to start: " + result.cause().getMessage());
                                }
                            });
                }).onFailure(err -> {
                    logger.info(err.getMessage());
                });
            })
            .onFailure(err -> {
                logger.info(err.getMessage());
            });
        }).onFailure(err -> {
            logger.info(err.getMessage());
        });
    }

    // Health Check
    private void healthCheckBuilder(Router router) {
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

        healthCheckHandler.register("pet.service", promise ->
                vertx.eventBus().request("health.pet.service", "ping")
                        .onSuccess(msg -> {
                            promise.complete(Status.OK());
                        })
                        .onFailure(err -> {
                            promise.complete(Status.KO());
                        }));

        router.get("/health").handler(healthCheckHandler);
    }

    // Publisher for createPets
    private void createPets(RoutingContext routingContext) {
        RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

        petService.createPets(result -> {
            if (result.succeeded()) {
                if (result.result() != null) {
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(201)
                            .end();
                } else {
                    routingContext.response()
                            .setStatusCode(204)
                            .end();
                }
            } else {
                routingContext.response()
                        .setStatusCode(500)
                        .end();
            }
        });
    }

    // Publisher for listPets
    private void listPets(RoutingContext routingContext) {
        RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        JsonObject json = requestParameters.toJson().getJsonObject("query");

        petService.listPets(json.getValue("limit") == null ? null : (json.getLong("limit").intValue()), result -> {
            if (result.succeeded()) {
                if (result.result() != null) {
                    List<JsonObject> list = result.result().stream().map(Pet::toJson).collect(Collectors.toList());
                    JsonArray array = new JsonArray(list);
                    routingContext.response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(array.encodePrettily());
                } else {
                    routingContext.response()
                            .setStatusCode(204)
                            .end();
                }
            } else {
                routingContext.response()
                        .setStatusCode(500)
                        .end();
            }
        });
    }

    // Publisher for showPetById
    private void showPetById(RoutingContext routingContext) {
        RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
        JsonObject json = requestParameters.toJson().getJsonObject("path");
        try {
            Long petId = Long.parseLong(json.getString("petId"));
            petService.showPetById(petId, result -> {
                if (result.succeeded()) {
                    if (result.result() != null) {
                        routingContext.response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .end(result.result().toJson().encodePrettily());
                    } else {
                        routingContext.response()
                                .setStatusCode(204)
                                .end();
                    }
                } else {
                    routingContext.response()
                            .setStatusCode(404)
                            .end();
                }
            });
        } catch(NumberFormatException e) {
            routingContext.response()
                    .setStatusCode(404)
                    .end();
        }
    }

    // Helper method
    Future<String> createApiTempFile(String file) {
        Promise<String> promise = Promise.promise();
        vertx.fileSystem().createTempDirectory("")
                .onSuccess(dir -> {
                    File targetFile = new File(dir+"/"+file);
                    try {
                        FileUtils.copyInputStreamToFile(
                                this.getClass().getClassLoader().getResourceAsStream(file),
                                targetFile);
                        promise.complete(targetFile.getAbsolutePath());
                    } catch (IOException e) {
                        promise.fail(e);
                    }
                });
        return promise.future();
    }

}
