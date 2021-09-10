package com.github.cheewail.vertx.petstore.service.impl;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.api.model.Pet;
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
    public void createPets(Handler<AsyncResult<Pet>> handler) {
        Long id = nextId.getAndIncrement();
        repo.put(id, new Pet(id, Long.toString(id), Long.toString(id)));
        handler.handle(Future.succeededFuture(repo.get(id)));
    }

    @Override
    public void listPets(Integer limit, Handler<AsyncResult<List<Pet>>> handler) {
        handler.handle(Future.succeededFuture(repo.values().stream().collect(Collectors.toList())));
    }

    @Override
    public void showPetById(Long id, Handler<AsyncResult<Pet>> handler) {
        Pet pet = repo.get(id);
        if (pet == null) {
            handler.handle(Future.failedFuture("404"));
        } else {
            handler.handle(Future.succeededFuture(repo.get(id)));
        }
    }

}
