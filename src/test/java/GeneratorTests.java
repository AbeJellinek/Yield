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
}
