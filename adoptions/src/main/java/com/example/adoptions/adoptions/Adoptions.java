package com.example.adoptions.adoptions;

import com.example.adoptions.adoptions.validation.Validation;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;

@Import(AdoptionsBeanRegistrar.class)
@Configuration
class Adoptions {

//    @Bean
//    IncompleteEventPublicationsRunner incompleteEventPublications(IncompleteEventPublications incompleteEventPublications) {
//        return new IncompleteEventPublicationsRunner(incompleteEventPublications);
//    }
}

class AdoptionsBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(@NonNull BeanRegistry registry, @NonNull Environment env) {

        registry.registerBean(IncompleteEventPublicationsRunner.class,
                spec -> spec
                        .description("reprocesses incomplete missed events")
                        .supplier(supplierContext -> {
                            var inc = supplierContext.bean(IncompleteEventPublications.class);
                            return new IncompleteEventPublicationsRunner(inc);
                        })
        );
    }
}

//@Description( "...")
//@Component
class IncompleteEventPublicationsRunner implements ApplicationRunner {

    private final IncompleteEventPublications publications;

    IncompleteEventPublicationsRunner(IncompleteEventPublications publications) {
        this.publications = publications;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        IO.println("running IncompleteEventPublicationsRunner");
        this.publications.resubmitIncompletePublications(e -> true);
    }
}

interface DogRepository extends ListCrudRepository<@NonNull Dog, @NonNull Integer> {

    Collection<Dog> findByOwner(String name);
}

@Service
@Transactional
class AdoptionsService {

    private final DogRepository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    AdoptionsService(DogRepository repository, ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    void adopt(int dogId, String owner) {
        this.repository.findById(dogId).ifPresent(dog -> {
            var updated = this.repository.save(new Dog(dog.id(), dog.name(), owner, dog.description()));
            this.applicationEventPublisher.publishEvent(new DogAdoptedEvent(dogId));
            IO.println("adopted " + updated);
        });
    }
}

// look mom, no Lombok!!!
record Dog(@Id int id, String name, String owner, String description) {
}

@Controller
@ResponseBody
class DogsController {

    private final DogRepository dogRepository;

    DogsController(DogRepository dogRepository) {
        this.dogRepository = dogRepository;
    }

    @GetMapping("/dogs")
    Collection<Dog> getDogs(Principal principal) {
        return this.dogRepository.findByOwner(principal.getName());
    }
}

@Controller
@ResponseBody
class MeController {

    @GetMapping("/me")
    Map<String, String> me(Principal principal) {
        return Map.of("name", principal.getName());
    }
}

@Controller
@ResponseBody
class AdoptionsController {

    private final Validation validation;

    private final AdoptionsService adoptionsService;

    AdoptionsController(Validation validation, AdoptionsService adoptionsService) {
        this.validation = validation;
        this.adoptionsService = adoptionsService;
    }

    @PostMapping("/dogs/{dogId}/adoptions")
    void adopt(@PathVariable int dogId, @RequestParam String owner) {
        this.adoptionsService.adopt(dogId, owner);
    }

}
