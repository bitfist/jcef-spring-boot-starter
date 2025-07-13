package io.github.bitfist.jcef.spring.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("🛣️  DefaultCefQueryRouter Tests")
class DefaultCefQueryRouterTest {

    @CefQueryHandler("/greeter")
    static class Greeter {
        @CefQueryHandler("/hello/{name}")
        String hello(String name) {
            return "Hello " + name;
        }

        @CefQueryHandler("/double/{value}")
        int doubleIt(int value) {
            return value * 2;
        }

        @CefQueryHandler("/echo")
        Person echo(Person p) {
            return p;
        }
    }

    static class Person {
        public String name;
        public int age;
        public Person() {}
        public Person(String n, int a) { this.name = n; this.age = a; }
    }

    private DefaultCefQueryRouter router;

    @BeforeEach
    void setup() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        Greeter greeter = new Greeter();
        when(ctx.getBeansWithAnnotation(CefQueryHandler.class)).thenReturn(Map.of("greeter", greeter));
        router = new DefaultCefQueryRouter(ctx);
        router.initialize();
    }

    @Test
    @DisplayName("🏷️  Resolves path variables correctly")
    void resolvesPathVariable() throws Exception {
        CefQueryJson q = new CefQueryJson();
        q.setRoute("/greeter/hello/John");
        Object res = router.handleQuery(q);
        assertEquals("Hello John", res);
    }

    @Test
    @DisplayName("🔢  Converts primitive parameters")
    void convertsPrimitiveParameters() throws Exception {
        CefQueryJson q = new CefQueryJson();
        q.setRoute("/greeter/double/21");
        Object res = router.handleQuery(q);
        assertEquals(42, res);
    }

    @Test
    @DisplayName("📦  Converts JSON payload into custom type")
    void convertsPayloadToCustomType() throws Exception {
        CefQueryJson q = new CefQueryJson();
        q.setRoute("/greeter/echo");
        q.setPayload(Map.of("name", "Jane", "age", 30));
        Object res = router.handleQuery(q);
        assertNotNull(res);
        assertEquals(Person.class, res.getClass());
        Person p = (Person) res;
        assertEquals("Jane", p.name);
        assertEquals(30, p.age);
    }
}
