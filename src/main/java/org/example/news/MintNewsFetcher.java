package org.example.news;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MintNewsFetcher {

    private static final String MINT_MARKET_URL =
        "https://www.livemint.com/market";

    public List<NewsArticle> fetch() throws Exception {

        System.out.println("Fetching Mint market news...");

        Document document = Jsoup.connect(MINT_MARKET_URL)
            .userAgent(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                    "AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) " +
                    "Chrome/151.0.0.0 Safari/537.36"
            )
            .referrer("https://www.google.com/")
            .timeout(30_000)
            .followRedirects(true)
            .get();

        System.out.println(
            "Mint page title: " + document.title()
        );

        List<NewsArticle> articles =
            new ArrayList<>();

        Set<String> urls =
            new HashSet<>();

        Elements links =
            document.select("a[href]");

        System.out.println(
            "Total links found: " + links.size()
        );

        for (Element link : links) {

            String title =
                link.text().trim();

            String url =
                link.absUrl("href");

            if (title.isBlank()) {
                continue;
            }

            if (url.isBlank()) {
                continue;
            }

            /*
             * Only Mint URLs.
             */
            if (!url.contains("livemint.com")) {
                continue;
            }

            /*
             * Ignore very short navigation text.
             */
            if (title.length() < 25) {
                continue;
            }

            /*
             * Ignore duplicate URLs.
             */
            if (!urls.add(url)) {
                continue;
            }

            /*
             * Try to identify actual article URLs.
             *
             * Mint article URLs normally contain:
             *
             * /market/
             * /companies/
             * /money/
             * /industry/
             */

            if (!isArticleUrl(url)) {
                continue;
            }

            StockSymbolDetector detector =
                new StockSymbolDetector();

            String text =
                title + " " + link.text();

            List<String> symbols =
                detector.detect(text);

            articles.add(
                new NewsArticle(
                    "MINT",
                    title,
                    url,
                    symbols
                )
            );
        }

        System.out.println(
            "Mint articles found: "
                + articles.size()
        );

        return articles;
    }

    private boolean isArticleUrl(String url) {

        return url.contains("/market/")
            || url.contains("/companies/")
            || url.contains("/industry/");
    }

    public record NewsArticle(
        String source,
        String title,
        String url,
        List<String> symbols
    ) {
    }

    public static void main(String[] args) {

        MintNewsFetcher fetcher =
            new MintNewsFetcher();

        try {

            List<NewsArticle> articles =
                fetcher.fetch();

            System.out.println();
            System.out.println(
                "========== MINT NEWS =========="
            );

            int count = 0;

            for (NewsArticle article : articles) {

                System.out.println();
                System.out.println(
                    "--------------------------------"
                );

                System.out.println(
                    "SOURCE : "
                        + article.source()
                );

                System.out.println(
                    "TITLE  : "
                        + article.title()
                );

                System.out.println(
                    "URL    : "
                        + article.url()
                );
                System.out.println(
                    "SYMBOLS: "
                        + article.symbols()
                );

                count++;

                if (count >= 30) {
                    break;
                }
            }

            System.out.println();
            System.out.println(
                "Total displayed: " + count
            );

        } catch (Exception e) {

            System.err.println(
                "ERROR FETCHING MINT NEWS"
            );

            e.printStackTrace();
        }
    }
}