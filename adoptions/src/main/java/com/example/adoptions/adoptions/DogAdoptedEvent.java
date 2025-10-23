package com.example.adoptions.adoptions;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.core.GenericTransformer;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.dsl.Files;
import org.springframework.modulith.events.Externalized;

import java.io.File;

@Externalized(AdoptionsIntegrationFlowConfiguration.ADOPTIONS)
public record DogAdoptedEvent(int dogId) {
}

@Configuration
class AdoptionsIntegrationFlowConfiguration {

    static final String ADOPTIONS = "adoptionsChannel";

    @Bean(ADOPTIONS)
    DirectChannelSpec dogChannel() {
        return MessageChannels.direct();
    }

    @Bean
    IntegrationFlow dogAdoptionsIntegrationFlow(
            @Value("file://${user.home}/Desktop/outbound") File out,
            @Qualifier(ADOPTIONS) DirectChannelSpec dogChannel) {
        return IntegrationFlow
                .from(dogChannel)
//                .split()
//                .aggregate()
//                .enrich()
//                .route()
                .handle((GenericHandler<@NonNull DogAdoptedEvent>) (payload, headers) -> {
                    IO.println(payload);
                    headers.forEach((k, v) -> IO.println(k + ':' + v));
                    return payload;
                })
                .transform((GenericTransformer<DogAdoptedEvent, String>) source -> "dogId:" + source.dogId())
                .handle(Files.outboundAdapter(out).autoCreateDirectory(true))
                .get();
    }

}