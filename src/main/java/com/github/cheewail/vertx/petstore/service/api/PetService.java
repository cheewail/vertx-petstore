package com.github.cheewail.vertx.petstore.service.api;

import com.github.cheewail.vertx.petstore.service.api.model.Pet;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

@VertxGen
@ProxyGen
public interface PetService {

    void createPets(Handler<AsyncResult<String>> handler);

    void listPets(Integer limit, Handler<AsyncResult<List<Pet>>> handler);

    void showPetById(String petId, Handler<AsyncResult<Pet>> handler);

    void healthCheck(Handler<AsyncResult<Void>> handler);

    static PetService createProxy(Vertx vertx, String address) {
        return new PetServiceVertxEBProxy(vertx, address);
    }

}
