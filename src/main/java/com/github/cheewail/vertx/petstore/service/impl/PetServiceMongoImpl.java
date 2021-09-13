package com.github.cheewail.vertx.petstore.service.impl;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.api.model.Pet;
import com.github.javafaker.Animal;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.serviceproxy.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class PetServiceMongoImpl implements PetService {

    static final Logger logger = LoggerFactory.getLogger(PetServiceMongoImpl.class);

    private final int listPetslimit = 100;

    private final MongoClient mongoClient;
    private final JsonObject config;

    public PetServiceMongoImpl(Vertx vertx, JsonObject config) {
        this.config = config;
        JsonObject mongoconfig = new JsonObject()
                .put("connection_string", config.getString("mongo.uri"))
                .put("db_name", config.getString("mongo.dbname"));

        this.mongoClient = MongoClient.createShared(vertx, mongoconfig);
    }

    @Override
    public void createPets(Handler<AsyncResult<String>> handler) {
        Faker faker = new Faker();
        Name name = faker.name();
        Animal animal = faker.animal();
        Pet pet = new Pet(Integer.toUnsignedLong(name.hashCode()), name.firstName(), animal.name());
        JsonObject json = pet.toJson();
        logger.debug("mongoClient.save()");
        mongoClient.save("pet", json, result -> {
            if (result.succeeded()) {
                handler.handle(Future.succeededFuture(result.result()));
            } else {
                handler.handle(ServiceException.fail(503, "DB not reachable."));
            }
        });
    }

    @Override
    public void listPets(Integer limit, Handler<AsyncResult<List<Pet>>> handler) {
        FindOptions findOptions = new FindOptions();
        if (limit != null) {
            findOptions.setLimit(limit);
        } else {
            findOptions.setLimit(config.getInteger("mongo.list.max", listPetslimit));
        }
        logger.debug("mongoClient.findWithOptions()");
        mongoClient.findWithOptions("pet", new JsonObject(), findOptions, result -> {
            if (result.succeeded()) {
                handler.handle(Future.succeededFuture(result.result().stream().map(Pet::new).collect(Collectors.toList())));
            } else {
                handler.handle(ServiceException.fail(503, "DB not reachable."));
            }
        });
    }

    @Override
    public void showPetById(String petId, Handler<AsyncResult<Pet>> handler) {
        JsonObject filter = new JsonObject()
                .put("_id", petId);
        JsonObject projection = new JsonObject()
                .put("_id", 0)
                .put("id", 1)
                .put("name", 1)
                .put("tag", 1);
        logger.debug("mongoClient.findOne()");
        mongoClient.findOne("pet", filter, projection, result -> {
            if (result.succeeded()) {
                if (result.result() != null) {
                    handler.handle(Future.succeededFuture(new Pet(result.result())));
                } else {
                    handler.handle(Future.succeededFuture());
                }
            } else {
                handler.handle(ServiceException.fail(503, "DB not reachable."));
            }
        });
    }

    @Override
    public void healthCheck(Handler<AsyncResult<Void>> handler) {
        logger.debug("mongoClient.runCommand(ping)");
        mongoClient.runCommand("ping", new JsonObject().put("ping","1"), result -> {
            if (result.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

}
