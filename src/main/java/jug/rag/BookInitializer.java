package jug.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocuments;

public class BookInitializer {
    public static EmbeddingStore<TextSegment> initializeBook(String filename, EmbeddingModel embeddingModel, boolean initialize) throws Exception {
        String indexName = "rag-" + embeddingModel.getClass().getSimpleName();
        OpenSearchEmbeddingStore embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl("http://localhost:9200")
                .indexName(indexName.toLowerCase())
                .build();

        if (!initialize) return embeddingStore;



        System.out.println("Indexing " + filename + " with " + embeddingModel.getClass().getSimpleName() + " model embeddings...");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(filename);
        Path documentPath = Paths.get(resource.toURI());

        Document document = loadDocument(documentPath, new ApacheTikaDocumentParser());
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(6000, 2000);
        List<TextSegment> textSegments = documentSplitter.split(document);

        textSegments.forEach(segment -> {
            try {
                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        System.out.println("Indexed " + textSegments.size() + " segments. Book:" + filename + " Embedding Model: " + embeddingModel.getClass().getSimpleName() + " Index: " + indexName);
        return embeddingStore;
    }

    public static EmbeddingStore<TextSegment> initializeBooks(String directory, EmbeddingModel embeddingModel, boolean initialize) throws Exception {
        String indexName = "rag-" + embeddingModel.getClass().getSimpleName();
        OpenSearchEmbeddingStore embeddingStore = OpenSearchEmbeddingStore.builder()
                .serverUrl("http://localhost:9200")
                .indexName(indexName.toLowerCase())
                .build();

        if (!initialize) return embeddingStore;

        System.out.println("Indexing books in " + directory + " with " + embeddingModel.getClass().getSimpleName() + " model embeddings...");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(directory);
        Path documentPath = Paths.get(resource.toURI());

        List<Document> documents = loadDocuments(documentPath, new ApacheTikaDocumentParser());
        documents.forEach(document -> {
                    DocumentSplitter documentSplitter = DocumentSplitters.recursive(4000, 200);
                    List<TextSegment> textSegments = documentSplitter.split(document);

                    textSegments.forEach(segment -> {
                        try {
                            Embedding embedding = embeddingModel.embed(segment).content();
                            embeddingStore.add(embedding, segment);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    });
                });
        System.out.println("Indexed " + documents.size() + " documents. Directory:" + directory + " Embedding Model: " + embeddingModel.getClass().getSimpleName() + " Index: " + indexName);
        return embeddingStore;
    }


    public static void main(String[] args) throws URISyntaxException {
        try {
            AllMiniLmL6V2EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

            initializeBook("the-adventures-of-sherlock-holmes.epub", embeddingModel, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
