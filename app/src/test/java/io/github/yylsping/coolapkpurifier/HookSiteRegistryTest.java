package io.github.yylsping.coolapkpurifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.Test;

public final class HookSiteRegistryTest {
    @Test
    public void sameDescriptorFromIndependentLoadersIsNotDeduplicated() throws Exception {
        String name = LoaderHookFixture.class.getName();
        byte[] bytes = classBytes(name);
        Method first = new ByteArrayLoader(name, bytes).loadClass(name).getMethod("bar");
        Method second = new ByteArrayLoader(name, bytes).loadClass(name).getMethod("bar");

        assertEquals(first.toGenericString(), second.toGenericString());
        assertFalse(first.getDeclaringClass() == second.getDeclaringClass());
        HookSiteRegistry<String> registry = new HookSiteRegistry<>();
        registry.put("feature", first, "L1");
        assertTrue(registry.contains("feature", first));
        assertFalse(registry.contains("feature", second));
        registry.put("feature", second, "L2");
        assertEquals(2, registry.size());

        Method firstAgain = first.getDeclaringClass().getMethod("bar");
        assertTrue(registry.contains("feature", firstAgain));
        registry.put("feature", firstAgain, "L1-again");
        assertEquals(2, registry.size());
    }

    private static byte[] classBytes(String className) throws Exception {
        String resource = "/" + className.replace('.', '/') + ".class";
        try (InputStream input = HookSiteRegistryTest.class.getResourceAsStream(resource);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) {
                throw new IllegalStateException("missing " + resource);
            }
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static final class ByteArrayLoader extends ClassLoader {
        private final String targetName;
        private final byte[] bytes;

        ByteArrayLoader(String targetName, byte[] bytes) {
            super(null);
            this.targetName = targetName;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!targetName.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
