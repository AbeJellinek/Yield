import com.babblery.yield.Generator;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GeneratorTests {
    @Test
    public void generatorShouldProduceValues() {
        Generator<Integer> generator = Generator.on(yield -> {
            yield.value(1);
            yield.value(2);
            yield.value(3);
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

        generator.stream().limit(10).forEach(s -> assertEquals("value", s));
    }
}
