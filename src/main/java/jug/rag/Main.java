package jug.rag;


import dev.langchain4j.service.TokenStream;

import java.util.Scanner;



public class Main {
    public static void main(String[] args) throws Exception {


        BookChat bookChat = BookChatInitializer.initialize("papers", false);

        String prompt = "";
        System.out.print("Prompt: ");
        while (!Thread.currentThread().isInterrupted()) {
            Scanner scanner = new Scanner(System.in);
            prompt = scanner.nextLine();
            if (prompt.equalsIgnoreCase("bye!")) {
                System.out.println("Goodbye!");
                break;
            }
            TokenStream tokenStream = bookChat.chat(prompt);
            tokenStream.onNext(System.out::print)
                    .onComplete(ignored -> System.out.print("\n\nPrompt: "))
                    .onError(Throwable::printStackTrace)
                .start();

        }
    }
}