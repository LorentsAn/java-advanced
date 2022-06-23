package info.kgeorgiy.ja.lorents.crawler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import info.kgeorgiy.java.advanced.crawler.*;

public class WebCrawler implements Crawler {

    private final static String CORRECT_ARGUMENTS = "WebCrawler url [depth [downloads [extractors [perHost]]]]";
    private final static int DEFAULT_VALUE = 1;
    private final static int TIMEOUT = 60;

    private final ExecutorService downloaderService;
    private final ExecutorService extractorsServices;

    private final Downloader downloader;

    /**
     * {@link WebCrawler} constructor. Bypasses sites in width, loads pages and extracted links
     *
     * @param downloader {@link Downloader}, downloads websites.
     * @param downloaders number of downloader threads
     * @param extractors number of link extractors threads
     * @param perHost ---- redundant ---
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaderService = Executors.newFixedThreadPool(downloaders);
        this.extractorsServices = Executors.newFixedThreadPool(extractors);
    }

    private static int getArg(String[] args, int index) throws IOException {
        try {
            return args.length > index ? Integer.parseInt(args[index - 1]) : DEFAULT_VALUE;
        } catch (NumberFormatException e) {
            throw new IOException("Incorrect arguments, all fields must be an integer");
        }
    }

    /**
     * The main function that takes command line arguments and bypasses sites in width.
     * @param args - command line arguments. Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]
     */
    public static void main(String[] args) {
        if (args.length > 5 || args.length < 1) {
            System.err.println("Wrong arguments, use:" + CORRECT_ARGUMENTS);
        }
        String url = args[0];
        try {
            int depth = getArg(args, 2);
            int downloads = getArg(args, 3);
            int extractors = getArg(args, 4);
            int perHost = getArg(args, 5);

            try (WebCrawler crawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
                crawler.download(url, depth);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> downloadedUrl = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<String> waiting = new ConcurrentLinkedQueue<>();
        Phaser phaser = new Phaser(1);

        Set<String> extracted = ConcurrentHashMap.newKeySet();
        waiting.add(url);

        for (int i = depth; i > 0; i--) {
            List<String> process = List.copyOf(waiting);
            waiting.clear();
            process.stream()
                    .filter(extracted::add)
                    .forEach(link -> download(link, depth != 1, downloadedUrl, errors, waiting, phaser));
            phaser.arriveAndAwaitAdvance();
        }
        downloadedUrl.removeAll(errors.keySet());
        return new Result(List.copyOf(downloadedUrl), errors);
    }

    private void download(String url, boolean needExtraction, Set<String> downloadedUrl,
                          Map<String, IOException> errors, ConcurrentLinkedQueue<String> waiting, Phaser phaser) {
        final Runnable download = () -> {
            try {
                Document doc = downloader.download(url);
                downloadedUrl.add(url);
                if (needExtraction) {
                    Runnable extraction = () -> {
                        try {
                            waiting.addAll(doc.extractLinks());
                        } catch (IOException e) {
                            errors.put(url, e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    };
                    phaser.register();
                    extractorsServices.submit(extraction);
                }
            } catch (IOException e) {
                errors.put(url, e);
            } finally {
                phaser.arriveAndDeregister();
            }
        };

        final Queue<Runnable> waitingRun = new ConcurrentLinkedQueue<>();
        phaser.register();
        addTask(download, waitingRun);
    }

    private void addTask(Runnable task, Queue<Runnable> waiting) {
        waiting.add(task);
        Runnable executableTask = waiting.poll();
        if (executableTask != null) {
            downloaderService.submit(() -> {
                try {
                    executableTask.run();
                } catch (Exception e) {
                    // ignored
                }
            });
        }
    }

    private void close(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                    System.err.println("Unable to stop running tasks");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    @Override
    public void close() {
        close(downloaderService);
        close(extractorsServices);
    }
}