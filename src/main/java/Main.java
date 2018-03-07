package main.java;

import main.java.data.TextData;

public class Main {

    public static void main(String... args) {
        TextData d = new TextData();
        d.genFromFile("wiki_000", true);
    }
}