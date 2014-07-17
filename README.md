Yield
=====

An implementation of the generator pattern in Java. Licensed under the MIT license.

Example
-------

```java
Generator<Integer> generator = Generator.on(yield -> {
    yield.value(1);
    yield.value(2);
    yield.value(3);
});

for (int i : generator) {
    System.out.println(i);
}
```
