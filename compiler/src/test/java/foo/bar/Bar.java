package foo.bar;

public class Bar implements Foo{
    @Override
    public String hello(String parameter) {
        return "Bar says hello: "+parameter;
    }
}
