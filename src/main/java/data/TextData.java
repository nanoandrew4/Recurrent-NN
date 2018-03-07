package main.java.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TextData {

    private List<String> list;
    private LinkedList<String> sentenceList = new LinkedList<>();

    private ArrayList<long[]> sentenceStart = new ArrayList<>();
    private ArrayList<long[]> sentenceEnd = new ArrayList<>();

    private HashMap<String, FreqCount> map = new HashMap<>();
    private final int MIN_APPEARANCES = 8000;

    private final String SENTENCE_START = "SENTENCE_START", SENTENCE_END = "SENTENCE_END", UNKNOWN = "UNKNOWN";

    public TextData() {}

    public void genFromFile(String file, boolean serialize) {
        list = FileHandler.readLines(file);
        tokenize();
        generateSentenceVectors();
        if (serialize)
            FileHandler.serialize("training.dat", map, sentenceList, sentenceStart, sentenceEnd);
    }

    public void loadFromFile(String file) {
        Object[] objects = FileHandler.deserialize(file, 4);
        map = (HashMap<String, FreqCount>) objects[0];
        sentenceList = (LinkedList<String>) objects[1];
        sentenceStart = (ArrayList<long[]>) objects[2];
        sentenceEnd = (ArrayList<long[]>) objects[3];
    }

    private void tokenize() {
        int count = 0, initSize = list.size();
        long location = 3;

        System.out.println("Tokenizing file...");
        for (String s = list.get(0); list.size() > 1; s = list.get(0)) {
            if (count++ % 10000 == 0 && count != 1)
                System.out.print("\r%: " + ((float)(count) / (float)initSize) * 100f);

            list.remove(0);
            if (s.startsWith("<") || s.trim().equals(""))
                continue;

            String[] sentences = s.split("(?<=\\.!?)");
            for (String sentence : sentences)
                if (sentence.trim().length() > 10)
                    sentenceList.add(SENTENCE_START + " " + sentence.trim() + " " + SENTENCE_END);

            sentences = s.split("\\.!\\?");
            for (String sentence : sentences) {
                String[] words = sentence.split("((?<=\\W)|(?=\\W))");
                for (String word : words) {
                    if ("".equals(word.trim()))
                        continue;

                    FreqCount f = map.get(word.toLowerCase().trim());
                    if (f == null)
                        map.put(word, new FreqCount(word.toLowerCase().trim(), location++));
                    else if (!"".equals(word.trim()))
                        f.countIncrement();
                }
            }
        }
        System.out.println();
        printSampleData();
        removeInfrequent();
    }

    private void printSampleData() {
//        Random rand = new Random();
//
//        for (int i = 0; i < 20; i++)
//            System.out.println(sentenceList.get(rand.nextInt(sentenceList.size())));
//
//        FreqCount[] freq = new FreqCount[map.size()];
//        map.values().toArray(freq);
//
//        for (int i = 0; i < 20; i++)
//            System.out.println(freq[rand.nextInt(freq.length)].getStr());

        System.out.println("Sentences: " + sentenceList.size());
        System.out.println("Words before cleanup: " + map.size());
    }

    private void removeInfrequent() {
        LinkedList<String> list = new LinkedList<>();

        for (FreqCount f : map.values())
            if (f.getCount() < MIN_APPEARANCES)
                list.add(f.getStr());

        for (String s : list)
            map.remove(s);

        System.out.println("Words after cleanup: " + map.size());
    }

    private void generateSentenceVectors() {
        Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors() - 1];
        LinkedList[][] lists = new LinkedList[threads.length + 1][];

        int chunkSize = sentenceList.size() / (threads.length + 1);
        int lastChunk = 0;

        for (int t = 0; t < threads.length; t++) {
            int start = t * chunkSize;
            int end = lastChunk = start + chunkSize;
            int thread = t;
            threads[t] = new Thread(() -> lists[thread] = genSentVecThread(start, end, thread));
            threads[t].start();
        }

        lists[threads.length] = genSentVecThread(lastChunk, lastChunk + chunkSize, threads.length);

        for (int t = 0; t < threads.length;) {
            if (!threads[t].isAlive())
                t++;
            else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (LinkedList[] list : lists) {
            sentenceStart.addAll(list[0]);
            sentenceEnd.addAll(list[1]);
        }
    }

    private LinkedList[] genSentVecThread(int start, int end, int thread) {
        LinkedList<long[]> sentenceStart = new LinkedList<>();
        LinkedList<long[]> sentenceEnd = new LinkedList<>();

        for (int s = 0; s < end - start; s++) {
            if (s % 1000 == 0 && s != 0)
                System.out.print("\r" + thread + " %: " + (((float)s / (float)(end - start))) * 100f);

            String[] sentences = sentenceList.get(s + start).split("((?<=\\W)|(?=\\W))");
            sentenceStart.add(s, new long[sentences.length - 1]);
            sentenceEnd.add(s, new long[sentences.length - 1]);

            for (int w = 0; w < sentences.length - 1; w++) {
                if ("".equals(sentences[w].trim()))
                    continue;

                FreqCount f = map.get(sentences[w]);
                if (f == null) {
                    // Redundant
//                    if (SENTENCE_START.equals(sentence[w]))
//                        sentenceStart[s][w] = sentenceEnd[s][w] = 0;
                    if (SENTENCE_END.equals(sentences[w]))
                        sentenceStart.get(s)[w] = sentenceEnd.get(s)[w + 1] = 1;
                    else
                        sentenceStart.get(s)[w] = sentenceEnd.get(s)[w + 1] = 2;
                } else
                    sentenceStart.get(s)[w] = sentenceEnd.get(s)[w + 1] = f.getLocation();
            }
        }

        System.out.println("\rThread " + thread + " finished generating string indices");

        return new LinkedList[] {sentenceStart, sentenceEnd};
    }
}

class FreqCount implements Serializable{

    private String str;
    private long count = 0, location;

    FreqCount(String str, long location) {
        this.str = str;
        this.location = location;
    }

    void countIncrement() {
        count++;
    }

    long getCount() {
        return count;
    }

    String getStr() {
        return str;
    }

    public long getLocation() {
        return location;
    }
}