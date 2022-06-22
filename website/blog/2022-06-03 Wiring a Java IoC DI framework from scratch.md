---
slug: build-java-ioc-di-framework-from-scratch
title: Build Java IoC/DI framework from scratch
authors: [tu]
tags: [java, ioc]
---

When developing Keva project, I was struggled at finding a suitable IoC/DI framework: choose between [Spring](https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/beans.html), [Guice](https://github.com/google/guice), and others.
While Spring is a popular choice, it is not a good choice for a project with a small number of components and need to start fast.
On the other hand, Guice is also a popular choice, seems like it will start faster than Spring (because no need to scan class path for components),
but I personally don't like its APIs with a lot of boilerplate (define explicit bindings, etc.).

Finally, I've decided to build a Java IoC/DI framework from scratch, with Spring's IoC API and just contains the bare minimum logics of a DI framework.
That means to remove almost the "magic" part of Spring IoC, and just focus on the core logics: create and manage beans, and inject dependencies.

## Why need a DI/IoC?

While some others can prefer writing code without DI/IoC: manually init instance/component and manually inject them,
just like below:

```java
var svc = new ShippingService(new ProductLocator(), 
   new PricingService(), new InventoryService(), 
   new TrackingRepository(new ConfigProvider()), 
   new Logger(new EmailLogger(new ConfigProvider())));
```

Many don't realize that their dependencies chain can become nested, and it quickly becomes unwieldy to wire them up manually.
Even with factories (factory pattern), the duplication of your code is just not worth it.

DI/IoC can help to init instance/component and inject them, and it's also automatically wire them up, so you don't have to write code manually.
It also can be used to decouple the classes and improve testability, so we can get many of the benefits.

But is that (IoC framework) creates magic? Yes, if you can trust the fact that this code does its job,
then you can safely skip all of that property wrapping mumbo-jumbo. You've got other problems to solve.

## How Keva IoC works

Since Keva IoC is writing from scratch, I can control how magic the IoC framework will be, thus remove the unnecessary magic likes: bean lifecycle, property wrapping, etc.

For just the bare minimal logics of a DI framework, it contains:

- Scan beans (scan the `@Component` annotated classes)
- Get the `beans` definitions, then create the `beans`
- Store `beans` in a "bean container"
- Scan the `@Autowire` annotations, then automatically inject dependencies
## Implement Keva IoC

Create annotation `@Component` first:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
```

Create annotation `@Autowired`:

```java
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
}
```

Since `@Autowired` is injected by type, but dependency injection may also be injected by name, the annotation `@Qualifier` is created:

```java
@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Qualifier {
    String value() default "";
}
```

How to scan beans? We need a package helps to scan all the class in th `classpath`, [org.reflections](https://github.com/ronmamo/reflections) is a good choice.

```java
public static List<Class<?>> getClasses(String packageName) {
    List<Class<?>> classes=new ArrayList<>();
    String path = packageName.replace('.','/');
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URI pkg = Objects.requireNonNull(classLoader.getResource(path)).toURI();
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        dirs.add(new File(resource.getFile()));
    }
    for (File directory : dirs){
        classes.addAll(findClasses(directory,packageName));
    }
    return classes;
}
```

We have a `BeanContainer` class to store and manage all the beans:

```java
public class BeanContainer {
    public final Map<Class<?>, Map<String, Object>> beans = new HashMap<>(10);
    // ...
```

After scanned and created all the `beans`, next we have to scan all the `@Autowire` annotations, and inject the dependencies:

```java
private void fieldInject(Class<?> clazz, Object classInstance) {
    Set<Field> fields = FinderUtil.findFields(clazz, Autowired.class);
    for (Field field : fields) {
        String qualifier = field.isAnnotationPresent(Qualifier.class) ? field.getAnnotation(Qualifier.class).value() : null;
        Object fieldInstance = _getBean(field.getType(), field.getName(), qualifier, true);
        field.set(classInstance, fieldInstance);
    }
}
```

That's basically the core logics of Keva IoC, for more details, please refer to [Keva IoC source code](https://github.com/keva-dev/keva-ioc/).

## KevaIoC usage

Let's say we have an interface `Engine.java`:

```java
public interface Engine {
    String getName();
}
```

And we have a class `V8Engine.java` that implements `Engine`:

```java
@Component
public class V8Engine implements Engine {
    public String getName() {
        return "V8";
    }
}
```

And `SpiderMonkeyEngine.java` also implements `Engine`:

```java
@Component
public class SpiderMonkeyEngine implements Engine {
    public String getName() {
        return "SpiderMonkey";
    }
}
```

And a `Browser.java` class that need to inject an `Engine` implementation:

```java
@Component
public class Browser {
    @Autowired
    String version;
    
    Engine engine;
    BrowserRenderer renderer;

    @Autowired
    public Browser(@Qualifier("v8Engine") Engine engine, BrowserRenderer renderer) {
        this.engine = engine;
        this.renderer = renderer;
    }

    public String run() {
        return renderer.render("This browser run on " + engine.getName());
    }
    
    public String getVersion() {
        return renderer.render("Browser version: " + version);
    }
}
```

And the `Main.class` be like:

```java
public class Main {
    public static void main(String[] args) {
        KevaIoC context = KevaIoC.initBeans(Main.class);
        Browser browser = context.getBean(Browser.class);
        System.out.println(browser.run());
    }
}
```

The APIs basically looks the same as Spring IoC, only the actual implementation is simpler and more concise, with less magic.
Still the Keva codebase is clean and easy to understand based on elegant Spring IoC's API similar, and the startup time remains very fast due to its simplicity.

## Summary

Some of the Keva IoC's main features are:

- Spring-like annotation-support, no XML
- Fast startup time, small memory footprint (see performance section soon)
- Pocket-sized, only basic features (no bean's lifecycle, no "Spring's magic")
- Less opinionated, support mount existing beans (means can integrate well with other IoC/DI frameworks)

Supported annotations:

- `@ComponentScan`
- `@Component`
- `@Configuration`
- `@Bean`
- `@Autowired` (supports field injection, setter injection and constructor injection)
- `@Qualifier`
- Support mount existing beans via `.initBeans(Main.class, beanOne, beanTwo...)` static method

KevaIoC is very fit for small applications, that has to have small memory footprint, small jar size and fast startup time,
for example plugins, (embedded) standalone application, integration tests, jobs, Android applications, etc.

Maybe in the future if more logic needed from Keva, I'll add more "magic" features, like bean's lifecycle, etc, but for now, it's enough.
