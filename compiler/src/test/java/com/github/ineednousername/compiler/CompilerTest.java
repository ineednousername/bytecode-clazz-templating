package com.github.ineednousername.compiler;

import com.github.indeednousername.compiler.BytecodeClassLoader;
import com.github.indeednousername.compiler.Compiler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class CompilerTest {

    @Test
    public void dumpCode(){
        try {
            String code1 = "class Dump1{}";
            String code2 = "package even.has.a.pkg; class Dump2{}";
            final List<Compiler.Source> sources = Arrays.asList(
                    new Compiler.Source("Dump1", () -> code1),
                    new Compiler.Source("even.has.a.pkg.Dump2", () -> code2)
            );
            System.setProperty("compiler.java.dump","true");
            System.setProperty("compiler.java.dump.dir",System.getProperty("user.dir")+"/target/"+System.currentTimeMillis());

            Compiler.compile(sources);
        }finally {
            System.setProperty("compiler.java.dump","false");
            System.setProperty("compiler.java.dump.dir","null");

        }


    }

    @Test
    public void testClassNoBody() throws Exception {
        String code = "class NoBody{}";
        final List<Compiler.Source> sources = Arrays.asList(
                new Compiler.Source("NoBody", () -> code)
        );
        final List<Compiler.Compiled> compiled = Compiler.compile(sources);
        final BytecodeClassLoader clazzloader = new BytecodeClassLoader(ClassLoader.getSystemClassLoader(), compiled);

        final Class<?> noBody = clazzloader.findClass("NoBody");
        assertThat(noBody.getSuperclass(), is(equalTo(Object.class)));
        assertThat(noBody.getDeclaredMethods().length, is(0));
        assertThat(noBody.getDeclaredFields().length, is(0));
        assertThat(noBody.getDeclaredAnnotations().length, is(0));
        assertThat(noBody.getDeclaredClasses().length, is(0));
        assertThat(noBody.getDeclaredConstructors().length, is(1));//default constructor will be always declared.
    }

    @Test
    public void testClassAField() throws Exception {
        String code = "class AField{String abc;}";
        final List<Compiler.Source> sources = Arrays.asList(
                new Compiler.Source("AField", () -> code)
        );
        final List<Compiler.Compiled> compiled = Compiler.compile(sources);
        final BytecodeClassLoader clazzloader = new BytecodeClassLoader(ClassLoader.getSystemClassLoader(), compiled);

        final Class<?> noBody = clazzloader.findClass("AField");
        assertThat(noBody.getSuperclass(), is(equalTo(Object.class)));
        assertThat(noBody.getDeclaredMethods().length, is(0));
        assertThat(noBody.getDeclaredFields().length, is(1));
        assertThat(noBody.getDeclaredAnnotations().length, is(0));
        assertThat(noBody.getDeclaredClasses().length, is(0));
        assertThat(noBody.getDeclaredConstructors().length, is(1));//default constructor will be always declared.

        assertThat(noBody.getDeclaredFields()[0].getName(), is("abc"));
        assertThat(noBody.getDeclaredFields()[0].getType().getName(), is("java.lang.String"));
    }

    @Test
    public void compileCodeWithStaticDependencies() throws Exception {
        String code = "package hello.test.no.sense; public class TestImpl extends foo.bar.Bar implements foo.bar.Foo{ public TestImpl(String hello){System.out.println(hello);}}";
        final List<Compiler.Source> sources = Arrays.asList(
                new Compiler.Source("hello.test.no.sense.TestImpl", () -> code)
        );

        final List<Compiler.Compiled> compiled = Compiler.compile(sources);

        final BytecodeClassLoader clazzloader = new BytecodeClassLoader(ClassLoader.getSystemClassLoader(), compiled);

        final String impl = "hello.test.no.sense.TestImpl";
        final Object instance = invokeFirstCtor(clazzloader.findClass(impl), impl);
        final Object helloMethod = invokeHelloMethod(instance, impl);
        assertThat(helloMethod, is(equalTo("Bar says hello: hello.test.no.sense.TestImpl")));
    }

    @Test
    public void compileCodeWithGeneratedDependencies_TestImpl2_dependency_on_TestImpl() throws Exception {
        String class1Code = "package hello.test.no.sense; public class TestImpl extends foo.bar.Bar implements foo.bar.Foo{ public TestImpl(String hello){System.out.println(hello);}}";
        String class2Code = "package hello.test.no.sense; public class TestImpl2 extends hello.test.no.sense.TestImpl implements foo.bar.Foo{ public TestImpl2(String hello){super(\"hello2 \"+hello);}}";

        final List<Compiler.Source> sources = Arrays.asList(
                new Compiler.Source("hello.test.no.sense.TestImpl", () -> class1Code),
                new Compiler.Source("hello.test.no.sense.TestImpl", () -> class2Code)
        );

        final List<Compiler.Compiled> compile = Compiler.compile(sources);

        final BytecodeClassLoader aClass = new BytecodeClassLoader(ClassLoader.getSystemClassLoader(), compile);
        {
            final String impl = "hello.test.no.sense.TestImpl";
            final Object instance = invokeFirstCtor(aClass.findClass(impl), impl);
            final Object helloMethod = invokeHelloMethod(instance, impl);
            assertThat(helloMethod, is(equalTo("Bar says hello: hello.test.no.sense.TestImpl")));
        }

        {
            final String impl = "hello.test.no.sense.TestImpl2";
            final Object instance = invokeFirstCtor(aClass.findClass(impl), impl);
            final Object helloMethod = invokeHelloMethod(instance, impl);
            assertThat(helloMethod, is(equalTo("Bar says hello: hello.test.no.sense.TestImpl2")));
        }
    }

    private Object invokeFirstCtor(Class<?> aClass, Object... parameters) throws Exception{
        return aClass.getConstructors()[0].newInstance(parameters);
    }

    private Object invokeHelloMethod(Object instance, String parameter) throws Exception {
        return Arrays.stream(instance.getClass().getMethods()).filter(m -> "hello".equals(m.getName())).findFirst().get().invoke(instance, parameter);
    }

}
