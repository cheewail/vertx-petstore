# Vert.x based Petstore Demo
Using Vert.x Service Proxies to expose services on the event bus and reducing the code required to invoke them.

### To build with Maven
```shell
mvn package
```

### To build Docker image
```shell
docker build -t cheewail/petstore -f src/docker/Dockerfile .
```

### To run the Docker
```shell
docker run -p 8080:8080 cheewail/petstore
```
