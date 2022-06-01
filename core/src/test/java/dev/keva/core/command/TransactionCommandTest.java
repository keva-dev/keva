package dev.keva.core.command;

import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransactionCommandTest extends BaseCommandTest {

    @Test
    void transaction() {
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
    }

    @Test
    void transactionWatch() {
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
    }

}
