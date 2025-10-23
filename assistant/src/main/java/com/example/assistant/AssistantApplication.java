package com.example.assistant;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.util.List;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }

    @Bean
    PromptChatMemoryAdvisor promptChatMemoryAdvisor(DataSource dataSource) {
        var jdbc = JdbcChatMemoryRepository
                .builder()
                .dataSource(dataSource)
                .build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(jdbc)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }

    @Bean
    McpSyncClient schedulerMcpClient() {
        var mcp = McpClient
                .sync(HttpClientSseClientTransport.builder("http://localhost:8083").build())
                .build();
        mcp.initialize();
        return mcp;
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(vectorStore);
    }
}

// Pooch Palace

// look mom, no Lombok!!!
record Dog(@Id int id, String name, String owner, String description) {
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}


@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(
            QuestionAnswerAdvisor qa,
            McpSyncClient schedulerMcpClient ,
            DogRepository dogRepository,
            VectorStore vectorStore,

            PromptChatMemoryAdvisor promptChatMemoryAdvisor,
            ChatClient.Builder ai) {

        if (false)
            dogRepository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });

        var system = """
                You are an AI powered assistant to help people adopt a dog from the adoption\s
                agency named Pooch Palace with locations in Nuremberg, Seoul, Tokyo, Singapore, Paris,\s
                Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
                will be presented below. If there is no information, then return a polite response suggesting we\s
                don't have any dogs available.
                """;

        this.ai = ai
                .defaultSystem(system)
                .defaultAdvisors(qa, promptChatMemoryAdvisor)
                .defaultToolCallbacks(new SyncMcpToolCallbackProvider(schedulerMcpClient))
                .build();
    }

    @GetMapping("/{user}/ask")
    String ask(@PathVariable String user,
               @RequestParam String question) {

        return this.ai
                .prompt()
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, user))
                .call()
                .content();
        //.entity(DogAdoptionSuggestion.class);
    }
}


record DogAdoptionSuggestion(int id, String name, String description) {
}