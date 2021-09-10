package com.github.cheewail.vertx.petstore.verticle;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.impl.PetServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PetStoreVerticle extends AbstractVerticle {

    @Override
    public void start() {
        PetService service = new PetServiceImpl();

        new ServiceBinder(vertx)
                .setAddress("pet.service")
                .register(PetService.class, service);

        // Health Check
        vertx.eventBus().consumer("health.pet.service").handler(message -> {
            message.reply("pong");
        });
    }

}
