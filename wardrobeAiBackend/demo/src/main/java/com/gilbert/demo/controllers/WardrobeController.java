package com.gilbert.demo.controllers;

import com.gilbert.demo.model.ClothingItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wardrobe")
@Tag(name = "Wardrobe Assistant", description = "API for an AI Stylist to search a user's closet")
public class WardrobeController {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @GetMapping("/search")
    @Operation(summary = "Search for clothing", description = "Finds items in the wardrobe based on a search term like 'formal' or 'summer'.")
    public List<ClothingItem> search(@Parameter(description = "Description of the clothing needed") @RequestParam String query) {

        // Simple match query for the demo
        Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m
                        .fields("item_name", "description")
                        .query(query)))
                .build();

        SearchHits<ClothingItem> hits = elasticsearchOperations.search(searchQuery, ClothingItem.class);
        return hits.stream().map(SearchHit::getContent).toList();
    }

    @PostMapping("/add")
    @Operation(summary = "Add a clothing item", description = "Adds a new clothing item to the wardrobe.")
    public ClothingItem add(@RequestBody ClothingItem item) {
        return elasticsearchOperations.save(item);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all items", description = "Retrieves all clothing items in the wardrobe.")
    public List<ClothingItem> getAll() {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .build();
        SearchHits<ClothingItem> hits = elasticsearchOperations.search(query, ClothingItem.class);
        return hits.stream().map(SearchHit::getContent).toList();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete an item", description = "Removes a clothing item from the wardrobe by ID.")
    public String delete(@PathVariable String id) {
        return elasticsearchOperations.delete(id, ClothingItem.class);
    }
}