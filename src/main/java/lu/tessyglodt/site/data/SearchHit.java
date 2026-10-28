package lu.tessyglodt.site.data;

// One search result; the excerpt is HTML (escaped text with <mark> around the hits)
public record SearchHit(String name, String title, String excerpt) {
}
