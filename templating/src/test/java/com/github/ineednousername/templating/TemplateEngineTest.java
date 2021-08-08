package com.github.ineednousername.templating;

import com.github.indeednousername.templating.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TemplateEngineTest {

    @Test
    public void testSimpleTemplating() {
        final List<Supplier<String>> foo = TemplateEngine.transform(Arrays.asList(
                new TemplateEngine.TemplateData(() -> "package {{package}}; class {{name}}{}",
                        Map.of(
                                "name", "Foo",
                                "package", "foo.bar"
                        )
                )
        ));

        final String template = foo.get(0).get();

        assertThat(template, is(equalTo("package foo.bar; class Foo{}")));
    }

    @Test
    public void testNestedTemplating() {
        final List<Supplier<String>> foo = TemplateEngine.transform(Arrays.asList(
                new TemplateEngine.TemplateData(() -> "package {{template.package}}; class {{template.name}}{}",
                        Map.of("template", Map.of(
                                "name", "HelloWorld",
                                "package", "some.other.pkg"
                        ))
                )
        ));

        final String template = foo.get(0).get();

        assertThat(template, is(equalTo("package some.other.pkg; class HelloWorld{}")));
    }
}
