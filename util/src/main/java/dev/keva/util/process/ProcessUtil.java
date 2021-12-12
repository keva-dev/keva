package dev.keva.util.process;

import lombok.extern.slf4j.Slf4j;
import sun.management.VMManagement;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
public class ProcessUtil {
    @SuppressWarnings("all")
    public static int getProcessId() {
        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            VMManagement management = (VMManagement) jvm.get(runtime);
            Method method = management.getClass().getDeclaredMethod("getProcessId");
            method.setAccessible(true);
            return (int) method.invoke(management);
        } catch (Exception e) {
            log.error("Cannot get PID", e);
            return -1;
        }
    }
}
