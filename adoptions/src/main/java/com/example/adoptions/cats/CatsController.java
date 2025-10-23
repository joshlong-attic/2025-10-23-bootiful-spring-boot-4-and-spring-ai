package com.example.adoptions.cats;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@ResponseBody
class CatsController {

    private final CatFactsApiClient client;

    private final AtomicInteger counter = new AtomicInteger(0);

    CatsController(CatFactsApiClient client) {
        this.client = client;
    }

    @GetMapping(value = "/cats",version = "1.0")
    Collection<String> oldCatsApi() {
        return this.client
                .facts()
                .facts()
                .stream()
                .map(CatFact::fact)
                .toList();
    }

    @ConcurrencyLimit(10)
    @Retryable(maxAttempts = 4)
    @GetMapping(value = "/cats",version = "1.1")
    CatFacts facts() {

        if (this.counter.incrementAndGet() < 3) {
            IO.println("ooops!");
            throw new IllegalStateException("oops!");
        }
        IO.println("cats!");
        return this.client.facts();
    }
}

@EnableResilientMethods
@Configuration
@ImportHttpServices(CatFactsApiClient.class)
class CatsConfiguration {
}

interface CatFactsApiClient {

    @GetExchange("https://www.catfacts.net/api")
    CatFacts facts();
}

record CatFacts(Collection<CatFact> facts) {
}

record CatFact(String fact) {
}
