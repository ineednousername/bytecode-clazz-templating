package com.github.indeednousername.templating;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.context.MapValueResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TemplateEngine {

    private static final TemplateEngine engine = new TemplateEngine();
    private final Handlebars handlebars;

    private TemplateEngine() {
        this.handlebars = new Handlebars();
    }

    public static List<Supplier<String>> transform(List<TemplateData> templates) {
        return templates.stream().map(t -> engine.transform(t)).collect(Collectors.toList());
    }

    private Supplier<String> transform(TemplateData t) {
        return () -> {
            try {
                Context context = Context
                        .newBuilder(t.getVariables())
                        .resolver(
                                MapValueResolver.INSTANCE
                        )
                        .build();
                return handlebars.compileInline(t.getTemplate()).apply(context);
            } catch (IOException e) {
                throw new RuntimeException("Could not transform template " + t, e);
            }
        };
    }

    public static class TemplateData {

        private final Supplier<String> template;
        private final Map<String, Object> variables;

        public TemplateData(Supplier<String> template, Map<String, Object> variables) {
            this.template = template;
            this.variables = variables;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }

        public String getTemplate() {
            return template.get();
        }

        @Override
        public String toString() {
            return "TemplateData{" +
                    "template=" + template +
                    ", variables=" + variables +
                    '}';
        }
    }
}
