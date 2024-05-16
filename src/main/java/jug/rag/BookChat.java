package jug.rag;

import dev.langchain4j.service.TokenStream;

public interface BookChat {
     TokenStream chat(String message);
}
