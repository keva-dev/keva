package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionCommandTest extends BaseCommandTest {

    @Test
    void transaction() {
        try {
            val transaction = jedis.multi();
            transaction.set("abc", "123");
            transaction.set("def", "456");
            val exec = transaction.exec();
            assertNotNull(exec);
            assertEquals(2, exec.size());
            assertEquals("OK", exec.get(0));
            assertEquals("OK", exec.get(1));

            val getAbc = jedis.get("abc");
            val getDef = jedis.get("def");
            assertEquals("123", getAbc);
            assertEquals("456", getDef);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void transactionWatch() {
        try {
            jedis.watch("abc");
            val transaction = jedis.multi();
            transaction.set("abc", "123");
            transaction.set("abc", "456");
            val exec = transaction.exec();
            assertNotNull(exec);
            assertEquals(2, exec.size());
            assertEquals("OK", exec.get(0));
            assertEquals("OK", exec.get(1));

            val getAbc = jedis.get("abc");
            assertEquals("456", getAbc);
        } catch (Exception e) {
            fail(e);
        }
    }

}
