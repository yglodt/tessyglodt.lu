package lu.tessyglodt.site.data;

import java.util.List;

// One page of search results, and how many pages match in all
public record SearchResults(List<SearchHit> hits, int total) {
}
