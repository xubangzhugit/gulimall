package com.izhiliu.erp.web.rest.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Utility class for handling pagination.
 *
 * <p>
 * Pagination uses the same principles as the <a href="https://developer.github.com/v3/#pagination">GitHub API</a>,
 * and follow <a href="http://tools.ietf.org/html/rfc5988">RFC 5988 (Link header)</a>.
 */
public final class PaginationUtil {

    private PaginationUtil() {
    }

    public static HttpHeaders fillTotalCount(Integer totalCount) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Integer.toString(totalCount));
        return headers;
    }


    public static <T> HttpHeaders generatePaginationHttpHeaders(Page<T> page, String baseUrl) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        String link = "";
        if ((page.getNumber() + 1) < page.getTotalPages()) {
            link = "<" + generateUri(baseUrl, page.getNumber() + 1, page.getSize()) + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getNumber()) > 0) {
            link += "<" + generateUri(baseUrl, page.getNumber() - 1, page.getSize()) + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getTotalPages() > 0) {
            lastPage = page.getTotalPages() - 1;
        }
        link += "<" + generateUri(baseUrl, lastPage, page.getSize()) + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, page.getSize()) + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }

    public static <T> HttpHeaders generatePaginationHttpHeaders(IPage<T> page, String baseUrl) {
        // 补差价
        page.setCurrent(page.getCurrent() - 1);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotal()));
        String link = "";
        if ((page.getCurrent() + 1) < page.getPages()) {
            link = "<" + generateUri(baseUrl, new Long(page.getCurrent() + 1).intValue(), new Long(page.getSize()).intValue()) + ">; rel=\"next\",";
        }
        // prev link
        if ((page.getCurrent()) > 0) {
            link += "<" + generateUri(baseUrl, new Long(page.getCurrent() - 1).intValue(), new Long(page.getSize()).intValue()) + ">; rel=\"prev\",";
        }
        // last and first link
        int lastPage = 0;
        if (page.getPages() > 0) {
            lastPage = new Long(page.getPages() - 1).intValue();
        }
        link += "<" + generateUri(baseUrl, lastPage, new Long(page.getSize()).intValue()) + ">; rel=\"last\",";
        link += "<" + generateUri(baseUrl, 0, new Long(page.getSize()).intValue()) + ">; rel=\"first\"";
        headers.add(HttpHeaders.LINK, link);
        return headers;
    }

    public static <T> IPage<T> toIPage(Pageable pageable) {
        // 补差价
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    public static <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage(Pageable pageable) {
        // 补差价
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
    }

    private static String generateUri(String baseUrl, int page, int size) {
        return UriComponentsBuilder.fromUriString(baseUrl).queryParam("page", page).queryParam("size", size).toUriString();
    }
}
