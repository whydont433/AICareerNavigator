package navigator.vacancy;

import org.jsoup.Jsoup;


public class HtmlCleaner {

    public static String clean(String html) {

        if (html == null) {
            return "";
        }

        return Jsoup.parse(html).text();
    }
}