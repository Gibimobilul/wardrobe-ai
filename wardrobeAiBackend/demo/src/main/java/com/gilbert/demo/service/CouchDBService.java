package com.gilbert.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilbert.demo.model.ClothingItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
@Slf4j
public class CouchDBService {

    @Value("${couchdb.primary.url}")
    private String primaryUrl;

    @Value("${couchdb.secondary.url}")
    private String secondaryUrl;

    @Value("${couchdb.databases}")
    private List<String> databases;

    @Value("${couchdb.username}")
    private String username;

    @Value("${couchdb.password}")
    private String password;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CouchDBService(RestTemplate couchDbRestTemplate) {
        this.restTemplate = couchDbRestTemplate;
    }

    // ---- Public API ----

    public ClothingItem save(String database, ClothingItem item) {
        item.setCategory(database);
        return withFailover(
                () -> doSave(primaryUrl, database, item),
                () -> doSave(secondaryUrl, database, item)
        );
    }

    public List<ClothingItem> findAll(String database) {
        return withFailover(
                () -> doFindAll(primaryUrl, database),
                () -> doFindAll(secondaryUrl, database)
        );
    }

    public List<ClothingItem> search(String database, String query) {
        return withFailover(
                () -> doSearch(primaryUrl, database, query),
                () -> doSearch(secondaryUrl, database, query)
        );
    }

    public String delete(String database, String id) {
        return withFailover(
                () -> doDelete(primaryUrl, database, id),
                () -> doDelete(secondaryUrl, database, id)
        );
    }

    public List<ClothingItem> findAllCategories() {
        return databases.stream()
                .flatMap(db -> findAll(db).stream())
                .toList();
    }

    public List<ClothingItem> searchAllCategories(String query) {
        return databases.stream()
                .flatMap(db -> search(db, query).stream())
                .toList();
    }

    public Set<String> getValidDatabases() {
        return new HashSet<>(databases);
    }

    // ---- Private implementation ----

    private ClothingItem doSave(String baseUrl, String database, ClothingItem item) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<ClothingItem> request = new HttpEntity<>(item, headers);

        ResponseEntity<JsonNode> response;
        if (item.getId() != null && !item.getId().isBlank()) {
            // PUT to update an existing document (requires _rev in the body)
            String url = baseUrl + "/" + database + "/" + item.getId();
            response = restTemplate.exchange(url, HttpMethod.PUT, request, JsonNode.class);
        } else {
            // POST to let CouchDB generate the ID
            String url = baseUrl + "/" + database;
            response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        }

        JsonNode body = response.getBody();
        if (body != null) {
            if (item.getId() == null || item.getId().isBlank()) {
                item.setId(body.path("id").asText());
            }
            item.setRev(body.path("rev").asText());
        }
        return item;
    }

    private List<ClothingItem> doFindAll(String baseUrl, String database) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = baseUrl + "/" + database + "/_all_docs?include_docs=true";
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, request, JsonNode.class);
        return parseAllDocsResponse(response.getBody(), database);
    }

    private List<ClothingItem> doSearch(String baseUrl, String database, String query) {
        HttpHeaders headers = buildHeaders();
        // Mango query: case-insensitive regex match on item_name or description
        Map<String, Object> mangoQuery = Map.of(
                "selector", Map.of(
                        "$or", List.of(
                                Map.of("item_name", Map.of("$regex", "(?i)" + query)),
                                Map.of("description", Map.of("$regex", "(?i)" + query))
                        )
                )
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(mangoQuery, headers);
        String url = baseUrl + "/" + database + "/_find";
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, request, JsonNode.class);
        return parseFindResponse(response.getBody(), database);
    }

    private String doDelete(String baseUrl, String database, String id) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Fetch the current revision first
        String docUrl = baseUrl + "/" + database + "/" + id;
        ResponseEntity<JsonNode> getResponse = restTemplate.exchange(docUrl, HttpMethod.GET, request, JsonNode.class);
        String rev = getResponse.getBody().path("_rev").asText();

        // Delete using the revision token
        String deleteUrl = docUrl + "?rev=" + rev;
        restTemplate.exchange(deleteUrl, HttpMethod.DELETE, request, JsonNode.class);
        return id;
    }

    private List<ClothingItem> parseAllDocsResponse(JsonNode body, String database) {
        List<ClothingItem> items = new ArrayList<>();
        if (body == null) return items;
        for (JsonNode row : body.path("rows")) {
            String id = row.path("id").asText("");
            if (id.startsWith("_design/")) continue;
            JsonNode doc = row.path("doc");
            ClothingItem item = objectMapper.convertValue(doc, ClothingItem.class);
            item.setCategory(database);
            items.add(item);
        }
        return items;
    }

    private List<ClothingItem> parseFindResponse(JsonNode body, String database) {
        List<ClothingItem> items = new ArrayList<>();
        if (body == null) return items;
        for (JsonNode doc : body.path("docs")) {
            ClothingItem item = objectMapper.convertValue(doc, ClothingItem.class);
            item.setCategory(database);
            items.add(item);
        }
        return items;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> T withFailover(Supplier<T> primary, Supplier<T> secondary) {
        try {
            return primary.get();
        } catch (ResourceAccessException e) {
            log.warn("Primary CouchDB node at {} unreachable ({}), failing over to {}",
                    primaryUrl, e.getMessage(), secondaryUrl);
            return secondary.get();
        }
    }
}
