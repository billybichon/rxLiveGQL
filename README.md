# rxLiveGQL #

[![Platform](https://img.shields.io/badge/platform-Java-brightgreen.svg?style=flat)](https://www.java.com/en/)
[![Technology](https://img.shields.io/badge/technology-GraphQL-blue.svg?style=flat)](https://graphql.org/)
[![Technology](https://img.shields.io/badge/technology-ReactiveX-blue.svg?style=flat)](http://https://reactivex.io/)
[![License](http://img.shields.io/badge/license-MIT-yellow.svg?style=flat)](https://opensource.org/licenses/MIT)


ReactiveX java library in order to use GraphQL Subscription with RxJava2 on WebSocket based on Apollo Protocol.

## Features ##
- [x] connect to a GraphQL server
- [ ] Handle init payload
- [x] Subscribe/Unsubscribe which return Observables
- [x] Close connection
- [x] Error handling (partial)
- [x] Encoders / Decoders (generic objects)

## Requirements ##

jdk version >= 1.8

## How to install ##

### Gradle ###

```gradle
dependency {
  compile 'com.github.billybichon:rx-livegql:1.0'
}
```

### maven ###

```maven
<dependency>
  <groupId>com.github.billybichon</groupId>
  <artifactId>rx-livegql</artifactId>
  <version>1.0</version>
</dependency>
```

### manually ###

Just copy the `rx-livegql-1.0.jar` inside your libs folder and be sure to use:
```gradle
dependency {
  compile fileTree(dir: 'libs', include: ['*.jar'])
}
```

## How to use ##

##### Connection #####
```java
RxLiveGQL rxliveGQL = new RxLiveGQL();

rxLiveGQl.connect("ws://your.url").subscribe((str) -> {
  // successfully connect to the server on the first next
}, throwable -> {
  // an error occurred while connecting with the server
}, () -> {
  // on complete, the connection is successfully closed
});
```

#### Initialization ####

```java
rxliveGQL.initServer().subscribe(() -> {
  // successfully initialize the connection with the server
}, throwable -> {
  // an error occurred while initialize the connection with the server
});
```

##### subscription #####
```java
// subscribe
rxLiveGQL.subscribe("query to subscribe", "tag").subscribe(str -> {
  // str contains the data of the subscription
}, throwable -> {
  // some error occurred or the connection has been closed
});

// Unsubscribe
rxLiveGQL.unsubscribe("tag").subscribe(() -> {
  // successfully unsusbcribe
}, throwable -> {
  // some error occurred
});
```

##### subscription with generic object #####
```java
// subscribe
rxLiveGQL.subscribe("query to subscribe", "tag", Obj.Decoder.class).subscribe(obj -> {
  // obj contains the data of the subscription transform into an object by the decoder
}, throwable -> {
  // some error occurred or the connection has been closed
});

// Unsubscribe
rxLiveGQL.unsubscribe("tag").subscribe(() -> {
  // successfully unsubscribe
}, throwable -> {
  // some error occurred
});
```

##### close connection #####
```java
rxLiveGQL.closeConnection().subscribe(() -> {
  // successfully close connection
}, throwable -> {
  // some error occurred
});
```

## Dependency ##

Libraries on which liveGQL depends:
  - [RxJava](https://github.com/ReactiveX/RxJava) used to implement ReactiveX design
  - [Tyrus](https://github.com/tyrus-project/tyrus) for managing websocket
  - [Gson](https://github.com/google/gson) for handling json object

## Bugs ##
  - Not sure all errors are catched.
