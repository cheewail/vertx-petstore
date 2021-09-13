package com.github.cheewail.vertx.petstore;

import com.github.cheewail.vertx.petstore.verticle.HttpServerVerticle;
import com.github.cheewail.vertx.petstore.verticle.PetStoreVerticle;
import io.reactiverse.junit5.web.WebClientOptionsInject;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.reactiverse.junit5.web.TestRequest.emptyResponse;
import static io.reactiverse.junit5.web.TestRequest.jsonBodyResponse;
import static io.reactiverse.junit5.web.TestRequest.responseHeader;
import static io.reactiverse.junit5.web.TestRequest.statusCode;
import static io.reactiverse.junit5.web.TestRequest.testRequest;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class SystemTest {
    static final Logger logger = LoggerFactory.getLogger(SystemTest.class);

    @WebClientOptionsInject
    public WebClientOptions opts = new WebClientOptions()
            .setDefaultHost("localhost")
            .setDefaultPort(8080);

    @BeforeAll
    static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        Checkpoint httpServerVerticleDeployed = testContext.checkpoint();
        Checkpoint petStoreVerticleDeployed   = testContext.checkpoint();

        vertx.deployVerticle(HttpServerVerticle.class.getName())
                .onSuccess( ar -> {
                    httpServerVerticleDeployed.flag();
                });
        vertx.deployVerticle(PetStoreVerticle.class.getName())
                .onSuccess( ar -> {
                    petStoreVerticleDeployed.flag();
                });
    }

    @Test
    public void health(WebClient client, VertxTestContext testContext) {
        final JsonObject status = new JsonObject()
                .put("id", "pet.service")
                .put("status", "UP");
        final JsonArray checks = new JsonArray()
                .add(status);
        final JsonObject health = new JsonObject()
                .put("checks", checks)
                .put("outcome", "UP")
                .put("status",  "UP");

        testRequest(client.get("/health"))
                .expect(
                        statusCode(200),
                        responseHeader("content-type", "application/json;charset=UTF-8"),
                        jsonBodyResponse(health)
                )
                .send(testContext);
    }

    @Test
    public void createPets(WebClient client, VertxTestContext testContext) {
        testRequest(client.post("/pets"))
                .expect(
                        statusCode(201),
                        responseHeader("content-length", "0"),
                        emptyResponse(),
                        res -> assertNotNull(res.getHeader("x-petId")),
                        res -> assertNull(res.getHeader("content-type"))
                )
                .send(testContext);
    }

    @Test
    public void createPetsAndGetPets(WebClient client, VertxTestContext testContext) {
        final Checkpoint checkpoint = testContext.checkpoint(2);
        testRequest(client.post("/pets"))
                .expect(
                        statusCode(201),
                        responseHeader("content-length", "0"),
                        emptyResponse(),
                        res -> assertNotNull(res.getHeader("x-petId")),
                        res -> assertNull(res.getHeader("content-type"))
                )
                .send(testContext, checkpoint)
                .onComplete(ar ->
                        testRequest(client.get("/pets/" + ar.result().headers().get("x-petId")))
                                .expect(
                                        statusCode(200),
                                        responseHeader("content-type", "application/json")
                                )
                                .send(testContext, checkpoint)
                );
    }

    @Test
    public void listPets(WebClient client, VertxTestContext testContext) {
        testRequest(client.get("/pets"))
                .expect(
                        statusCode(200),
                        responseHeader("content-type", "application/json")
                )
                .send(testContext);
    }

    @Test
    public void listPetsWithLimit(WebClient client, VertxTestContext testContext) {
        final Checkpoint checkpoint = testContext.checkpoint(4);
        testRequest(client.post("/pets")).expect(statusCode(201)).send(testContext, checkpoint).onComplete(ar1 -> {
            testRequest(client.post("/pets")).expect(statusCode(201)).send(testContext, checkpoint).onComplete(ar2 -> {
                testRequest(client.post("/pets")).expect(statusCode(201)).send(testContext, checkpoint).onComplete(ar3 -> {
                    testRequest(client.get("/pets?limit=2"))
                            .expect(
                                    statusCode(200),
                                    responseHeader("content-type", "application/json"),
                                    res -> assertEquals(2, res.bodyAsJsonArray().stream().count())
                            )
                            .send(testContext, checkpoint);
                });
            });
        });
    }

    @Test
    public void showPetByIdNotFound(WebClient client, VertxTestContext testContext) {
        testRequest(client.get("/pets/bad"))
                .expect(
                        statusCode(404),
                        responseHeader("content-length", "0"),
                        emptyResponse()
                )
                .send(testContext);
    }

}
