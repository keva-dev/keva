# Developer Guide

## Build requirements

- [OpenJDK 11](https://openjdk.java.net/projects/jdk/11/)
- Consider using [Jabba](https://github.com/shyiko/jabba) for convenient installation of JDK.

## How to build

We use [Gradle](https://gradle.org/) to build Keva.
The following command will compile Keva, run tests and generate JARs:

```bash
$ ./gradlew --parallel build
```

## Setting up your IDE

You can import Keva into your IDE ([IntelliJ IDEA](https://www.jetbrains.com/idea/) or [Eclipse](https://www.eclipse.org/)) as a Gradle project.

- IntelliJ IDEA - See [Importing Project from Gradle Model](https://www.jetbrains.com/help/idea/gradle.html#gradle_import_project_start)
- Eclipse - Use [Buildship Gradle Integration](https://marketplace.eclipse.org/content/buildship-gradle-integration)

IntelliJ IDEA is our primary IDE for developing Keva.

## Always make the build pass

Make sure your change does not break the build.

- Run `./gradlew --parallel build` locally.
- It is likely that you'll encounter some Checkstyle or Javadoc errors.
  Please fix them because otherwise the build will be broken.

## Avoid redundancy

Avoid using redundant keywords. To list a few:

- `final` method modifier in a `final` class
- `static` or `public` modifier in an `interface`
- `public` method modifier in a package-local or private class
- `private` constructor modifier in an `enum`
- field access prefixed with `this.` where unnecessary

## Use `public` only when necessary

The classes, methods and fields that are not meant to be used by a user should not be
public. Use the most restrictive modifier wherever possible, such as `private`,
package-local and `protected`, so that static analysis tools can find dead code easily.

## Organize

Organize class members carefully for readability, using **top-down** approach.
Although there's no absolute rule of thumb, it's usually like:

- `static` fields
- `static` methods
- member fields
- constructors
- member methods
- utility methods (both `static` and member)
- inner classes

## Check `null`

Do explicit `null`-check on the parameters of user-facing public methods.
Always use `@lombok.NonNull` to do a `null`-check.

```java
import lombok.NonNull

@Override
public int process(@NonNull String text) {
    // ...
}
```

### Use `@Nullable`

Use `@Nullable` annotation for nullable parameters and return types.

### Avoid redundant null checks

Avoid unnecessary `null`-checks, including the hidden checks in `Objects.hashCode()` and `Objects.equals()`.

```java
public final class MyClass {
    private final String name;

    public MyClass(String name) {
        // We are sure 'name' is always non-null.
        this.name = requireNonNull(name, "name");
    }

    @Override
    public int hashCode() {
        // OK
        return name.hashCode();
        // Not OK
        return Objects.hash(name);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        ... usual type check ...
        // OK
        return name.equals(((MyClass) obj).name);
        // Not OK
        return Objects.equals(name, ((MyClass) obj).name);
    }
}
```

## Use meaningful exception messages

When raising an exception, specify meaningful message which gives an explicit clue
about what went wrong.

```java
switch (fileType) {
    case TXT: ... break;
    case XML: ... break;
    default:
        // Note that the exception message contains the offending value
        // as well as the expected values.
        throw new IllegalStateException(
                "unsupported file type: " + fileType +
                 " (expected: " + FileType.TXT + " or " + FileType.XML + ')');
}
```

## Validate

Do explicit validation on the parameters of user-facing public methods.
When raising an exception, always specify the detailed message in the following format:

```java
public void setValue(int value) {
    if (value < 0) {
        // Note that the exception message contains the offending value
        // as well as the expected value.
        throw new IllegalArgumentException("value: " + value + " (expected: >= 0)");
    }
}
```

## Prefer JDK API

Prefer using plain JDK API when the same behavior can be achieved with the same
amount of code.

```java
// Prefer A (JDK) - less indirection
Map<String, String> map = new HashMap<>();   // A (JDK)
Map<String, String> map = Maps.newHashMap(); // B (Guava)

// Prefer B (Guava) - simpler yet more efficient
List<String> list = Collections.unmodifiableList(  // A (JDK)
        otherList.stream().filter(...).collect(Collectors.toList()));
List<String> list = otherList.stream().filter(...) // B (Guava)
        .collect(toImmutableList());
```

## Prefer early-return style

Prefer 'early return' code style for readability.

```java
// Great
public void doSomething(String value) {
    if (value == null) {
        return;
    }

    // Do the actual job
}

// Not great
public void doSomething(String value) {
    if (value != null) {
        // Do the actual job
    }
}
```

However, when the 'normal' execution path is very simple, this may also look beautiful:

```java
public void doSomething(String value) {
    if (value != null) {
        return value.trim();
    } else {
        return null;
    }
}
```

## Prefer `MoreObjects.toStringHelper()`

Prefer `MoreObjects.toStringHelper()` to hand-written `toString()` implementation.
However, consider writing hand-written or caching `toString()` implementation
in performance-sensitive places.

## Think aesthetics

Do not insert an empty line that hurts code aesthetics.

```java
// OK
if (...) {
    doSomething();
}

// Not OK
if (...) {
    doSomething();
                        // <-- Remove this extra line.
}
```

Similarly, do not use two or more consecutive empty lines.

```java
// OK
public void a() { ... }

public void b() { ... }

// Not OK
public void a() { ... }

                        // <-- Remove this extra line.
public void b() { ... }
```
