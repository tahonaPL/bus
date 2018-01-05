# Tahona - Event Bus #


### Install ###

```xml
        <dependency>
            <groupId>pl.tahona</groupId>
            <artifactId>bus</artifactId>
            <version>1.0.0</version>
        </dependency>
```

```
    compile "pl.tahona:bus:1.0.0"
```

### How to use ###

Registration of handler:

```java
class UserController {

    @Subscribe
    public void handleEvent(MyEvent ev) {
        ..Do nice things...
    }
}
```


Create Event Bus
```java
final UserController subscriber = new UserController();

EventBus eventBus = new EventBus();
eventBus.subscribe(subscriber);
```

Inform others
```java
final MyEvent event = new MyEvent();
eventBus.inform(event);
```