package com.github.indeednousername.compiler;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BytecodeClassLoader extends URLClassLoader {
    private final Map<String, byte[]> extraClassDefs;

    public BytecodeClassLoader(ClassLoader parent, List<Compiler.Compiled> compiledItems) {
        super(new URL[0], parent);
        this.extraClassDefs = new HashMap<>(compiledItems.stream().collect(Collectors.toMap(Compiler.Compiled::getFQN, Compiler.Compiled::getByteCode)));
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        byte[] classBytes = this.extraClassDefs.remove(name);
        if (classBytes != null) {
            return defineClass(name, classBytes, 0, classBytes.length);
        }
        return super.findClass(name);
    }

}
