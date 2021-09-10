package com.github.cheewail.vertx.petstore.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
    static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start() {
        CompositeFuture.all(
                deployHelper(HttpServerVerticle.class.getName()),
                deployHelper(PetStoreVerticle.class.getName()))
                .onComplete(result -> {
            if(result.succeeded()){
                logger.info("All verticles deployed!");
            } else {
                logger.info("One or more verticle failed to deployed!");
            }
        });
    }

    private Future<Void> deployHelper(String name) {
        final Promise<Void> promise = Promise.promise();
        vertx.deployVerticle(name, res -> {
            if(res.failed()){
                logger.error("Failed to deploy verticle " + name);
                logger.error(res.cause().getMessage());
                try {
                    throw res.cause();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                promise.fail(res.cause());
            } else {
                logger.info("Deployed verticle " + name);
                promise.complete();
            }
        });

        return promise.future();
    }
}
