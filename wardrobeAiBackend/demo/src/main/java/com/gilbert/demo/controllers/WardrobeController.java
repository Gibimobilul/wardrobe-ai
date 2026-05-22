package com.gilbert.demo.controllers;

import com.gilbert.demo.model.ClothingItem;
import com.gilbert.demo.service.CouchDBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private CouchDBService couchDBService;

    @GetMapping("/search")
    @Operation(summary = "Search for clothing", description = "Finds items across all categories based on a search term like 'formal' or 'summer'.")
    public List<ClothingItem> search(
            @Parameter(description = "Description of the clothing needed") @RequestParam String query) {
        return couchDBService.searchAllCategories(query);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all items", description = "Retrieves all clothing items across all categories.")
    public List<ClothingItem> getAll() {
        return couchDBService.findAllCategories();
    }

    @GetMapping("/{category}")
    @Operation(summary = "Get items by category", description = "Retrieves all clothing items in a specific category.")
    public List<ClothingItem> getByCategory(@PathVariable String category) {
        return couchDBService.findAll(category);
    }

    @PostMapping("/{category}/add")
    @Operation(summary = "Add a clothing item", description = "Adds a new clothing item to the specified category.")
    public ResponseEntity<?> add(@PathVariable String category, @RequestBody ClothingItem item) {
        if (!couchDBService.getValidDatabases().contains(category)) {
            return ResponseEntity.badRequest().body("Unknown category: " + category);
        }
        return ResponseEntity.ok(couchDBService.save(category, item));
    }

    @DeleteMapping("/{category}/{id}")
    @Operation(summary = "Delete an item", description = "Removes a clothing item from its category by ID.")
    public ResponseEntity<?> delete(@PathVariable String category, @PathVariable String id) {
        if (!couchDBService.getValidDatabases().contains(category)) {
            return ResponseEntity.badRequest().body("Unknown category: " + category);
        }
        return ResponseEntity.ok(couchDBService.delete(category, id));
    }
}
