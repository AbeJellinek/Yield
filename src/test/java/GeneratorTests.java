import com.babblery.yield.Generator;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GeneratorTests {
    @Test
    public void generatorShouldProduceValues() {
        Generator<Integer> generator = Generator.on(yield -> {
            yield.value(1);
            yield.value(2);
            yield.value(3);

            yield.end();
            yield.value(5);
        });

        Iterator<Integer> ints = generator.iterator();
        assertEquals(1, (int) ints.next());
        assertEquals(2, (int) ints.next());
        assertEquals(3, (int) ints.next());
        assertFalse(ints.hasNext());
    }

    @Test
    public void generatorShouldBeIterable() {
        Generator<String> generator = Generator.on(yield -> yield.value("value"));

        for (String s : generator) {
            assertEquals("value", s);
        }
    }

    @Test
    public void generatorShouldNotInfinitelyLoop() {
        Generator<String> generator = Generator.on(yield -> {
            while (true) {
                yield.value("value");
            }
        });

        Stream<String> stream = generator.stream().limit(10);
        Iterator<String> iter = stream.iterator();
        int i = 0;

        while (iter.hasNext()) {
            String next = iter.next();
            assertEquals("value", next);
            i++;
        }

        assertEquals(10, i);
    }

    @Test
    public void generatorShouldAcceptExecutor() {
        Generator<String> generator = Generator.on(yield -> yield.value("value"), Executors.newCachedThreadPool());

        for (String s : generator) {
            assertEquals("value", s);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void generatorShouldThrowIfEmpty() {
        Generator<String> generator = Generator.on(yield -> {
            // no-op
        });
        Iterator<String> iter = generator.iterator();
        iter.next();
    }
}
