package com.example.graalvm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashSet;

@SpringBootApplication
@ImportRuntimeHints(GraalvmApplication.Hints.class)
//@RegisterReflectionForBinding(Cat.class)
public class GraalvmApplication {

    @Bean
    static PetsBeanFactoryInitializationAotProcessor petsBeanFactoryInitializationAotProcessor() {
        return new PetsBeanFactoryInitializationAotProcessor();
    }

    static class PetsBeanFactoryInitializationAotProcessor
            implements BeanFactoryInitializationAotProcessor {

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(
                ConfigurableListableBeanFactory beanFactory) {

            var serializable = new HashSet<String>();

            for (var beanName : beanFactory.getBeanDefinitionNames()) {
                var beanDefinition = beanFactory.getBeanDefinition(beanName);
                var type = beanFactory.getType(beanName);
                IO.println(beanName + ":" + type.getName());

                if (type.isAssignableFrom(Serializable.class) ||
                        Serializable.class.isAssignableFrom(type)
                ) {
                    serializable.add(type.getName());
                }
            }

            IO.println("-------------------");
            serializable.forEach(IO::println);
            return (generationContext, _) -> {
                for (var clzz : serializable) {
                    generationContext.getRuntimeHints()
                            .serialization()
                            .registerType(TypeReference.of(clzz));
                    IO.println("registering serialization hint for "+ clzz);
                }
            };
        }
    }


    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(Cat.class, MemberCategory.values());
            hints.resources().registerResource(MESSAGE);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(GraalvmApplication.class, args);
    }


    static final Resource MESSAGE = new ClassPathResource("/message");

    @Bean
    ApplicationRunner runner(ObjectMapper objectMapper) {
        return args -> {

            var cat = new Cat("Felix");
            IO.println(objectMapper.writeValueAsString(cat));

            var textOfMessage = MESSAGE.getContentAsString(Charset.defaultCharset());
            IO.println(textOfMessage);

        };
    }
}

record Cat(String name) {
}

// reflection
// jni
// resources
// serialization
// proxiesa

@Component
class DjKhalidAnotherOne implements Serializable {
}

@Component
class Cart implements Serializable {
}