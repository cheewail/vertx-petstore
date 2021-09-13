package com.github.cheewail.vertx.petstore.service.impl;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.api.model.Pet;
import com.github.javafaker.Animal;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class PetServiceImpl implements PetService {

    private final Map<Long, Pet> repo;
    private final AtomicLong nextId = new AtomicLong(1L);

    public PetServiceImpl() {
        repo = new HashMap<>();
    }

    @Override
    public void createPets(Handler<AsyncResult<String>> handler) {
        Long id = nextId.getAndIncrement();
        Faker faker = new Faker();
        Name name = faker.name();
        Animal animal = faker.animal();
        Pet pet = new Pet(id, name.firstName(), animal.name());
        repo.put(id, pet);
        handler.handle(Future.succeededFuture(Long.toString(id)));
    }

    @Override
    public void listPets(Integer limit, Handler<AsyncResult<List<Pet>>> handler) {
        if (limit == null) {
            handler.handle(Future.succeededFuture(repo.values().stream().collect(Collectors.toList())));
        } else {
            handler.handle(Future.succeededFuture(repo.values().stream().limit(limit).collect(Collectors.toList())));
        }
    }

    @Override
    public void showPetById(String petId, Handler<AsyncResult<Pet>> handler) {
        try {
            Pet pet = repo.get(Long.parseLong(petId));
            if (pet != null) {
                handler.handle(Future.succeededFuture(pet));
            } else {
                handler.handle(Future.succeededFuture());
            }
        } catch (NumberFormatException e) {
            handler.handle(Future.succeededFuture());
        }
    }

    @Override
    public void healthCheck(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture());
    }

}
