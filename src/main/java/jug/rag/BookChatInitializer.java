package jug.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.time.Duration;
import java.util.List;

public class BookChatInitializer {
    public static BookChat initialize(String directory, boolean initialize) throws Exception {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore = BookInitializer.initializeBooks(directory, embeddingModel, initialize);


        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.7)
                .build();

        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3:latest")
                .timeout(Duration.ofMinutes(5))
                .build();


        ContentRetriever printingRetriever = query -> {
            List<Content> retrieved = contentRetriever.retrieve(query);
            retrieved.forEach(content -> {
                System.out.println("Retrieved content: " + content.textSegment().text());
            });
            System.out.println("====================================");
            return retrieved;
        };

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(printingRetriever)

                .contentInjector(new DefaultContentInjector(PromptTemplate.from(
                        "We have provided context information below. \n" +
                        "---------------------\n" +
                        "{{contents}}" +
                        "\n---------------------\n" +
                        "Given this information, please answer the question: {{userMessage}}\n" +
                        "Don't give an answer unless it is supported by the context above.\n" +
                                "Do not tell that you're using the context. \n" )))
                .build();


        return AiServices.builder(BookChat.class)
                .retrievalAugmentor(retrievalAugmentor)
                .streamingChatLanguageModel(model)

                .build();
    }
}
