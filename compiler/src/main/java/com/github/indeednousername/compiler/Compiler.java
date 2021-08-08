package com.github.indeednousername.compiler;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject.Kind;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * original idea comes from https://github.com/medallia/javaone2016/blob/master/src/main/java/com/medallia/codegen/SimpleJavaCompiler.java
 */
public class Compiler {

    private static final String CLASSPATH = System.getProperty("java.class.path");

    /**
     * We keep an initialized copy for each thread.
     */
    private static final ThreadLocal<Compiler> BY_THREAD = ThreadLocal.withInitial(Compiler::new);

    public static volatile boolean dump = Boolean.getBoolean("compiler.java.dump");
    public static volatile String dumpDir = System.getProperty("compiler.java.dump.dir");

    private final JavaCompiler compiler;

    private final StandardJavaFileManager standardJavaFileManager;

    private Compiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
        standardJavaFileManager = compiler.getStandardFileManager(null, null, null);
    }

    public static List<Compiled> compile(List<Source> sources) {
        final List<Compiled> compileds = BY_THREAD.get().compile0(sources);
        dump(sources, compileds);
        return compileds;
    }

    private static void dump(List<Source> sources, List<Compiled> compileds) {
        if (dump) {
            try {
                sources.stream().forEach(s -> {
                    try {
                        final File file = new File(dumpDir + "/sources/");
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        final FileWriter fileWriter = new FileWriter(file + "/" +
                                s.getClazzName().replace(".", "_") + ".java", UTF_8);
                        fileWriter.write(s.getContent());
                        fileWriter.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Could not dump sources", e);
                    }

                });

                compileds.stream().forEach(s -> {
                    try {
                        final File file = new File(dumpDir + "/classes/");
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        final FileWriter fileWriter = new FileWriter(file +"/"+ s.getFQN().replace(".", "_") + ".class", UTF_8);
                        fileWriter.write(new String(s.getByteCode(), UTF_8));
                        fileWriter.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Could not dump classes", e);
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static URI toUri(String path) {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("exception parsing uri", e);
        }
    }

    private List<Compiled> compile0(List<Source> sources) {
        final List<JavaFileObject> compilationUnits = sources.stream().map(s -> new JavaStringSource(toUri("Generated"), s.getContent())).collect(Collectors.toList());


        final InMemoryFileManager fileManager = new InMemoryFileManager(standardJavaFileManager);
        final StringWriter output = new StringWriter();
        final CompilationTask task = compiler.getTask(output, fileManager, null, Arrays.asList("-g", "-proc:none", "-classpath", CLASSPATH), null, compilationUnits);

        if (!task.call()) {
            throw new IllegalArgumentException("Compilation failed:\n" + output + "\n Source code: \n" + sources.stream().map(s -> s.getContent()).collect(Collectors.joining("\n")));
        }

        int classCount = fileManager.output.size();
        if (classCount == sources.size() || (classCount > 0)) {
            return fileManager.output.stream().map(c -> new Compiled(c)).collect(Collectors.toList());
        }
        throw new IllegalArgumentException("Compilation yielded an unexpected number of classes: " + classCount + " expectation was " + sources.size());

    }

    /**
     * Emulates a source file based on a arbitrary String that should java source code.
     */
    private static class JavaStringSource extends SimpleJavaFileObject {
        private final String contents;

        private JavaStringSource(URI uri, String contents) {
            super(uri, Kind.SOURCE);
            this.contents = contents;
        }

        @Override
        public String getName() {
            return uri.getRawSchemeSpecificPart();
        }

        /**
         * Ignore the file name for public classes
         */
        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return true;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return contents;
        }
    }

    /**
     * Bytecode that was compiled by the java compiler.
     */
    private static class JavaByteCode extends SimpleJavaFileObject {
        private ByteArrayOutputStream outputStream;

        private JavaByteCode(URI uri) {
            super(uri, Kind.CLASS);
        }

        @Override
        public String getName() {
            return uri.getRawSchemeSpecificPart();
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream = new ByteArrayOutputStream();
        }
    }

    private static class InMemoryFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private List<JavaByteCode> output = new ArrayList<>();

        InMemoryFileManager(StandardJavaFileManager target) {
            super(target);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
            final JavaByteCode file = new JavaByteCode(toUri(className));
            output.add(file);
            return file;
        }
    }

    public static class Source {

        private final Supplier<String> code;
        private final String clazzName;

        public Source(String clazzName, Supplier<String> code) {
            this.clazzName = clazzName;
            this.code = code;
        }

        public String getClazzName() {
            return clazzName;
        }

        public String getContent() {
            return code.get();
        }
    }

    public static class Compiled {

        private final JavaByteCode clazzFile;

        public Compiled(JavaByteCode c) {
            this.clazzFile = c;
        }

        public byte[] getByteCode() {
            return clazzFile.outputStream.toByteArray();
        }

        public String getFQN() {
            return clazzFile.getName();
        }
    }
}

