package com.gilbert.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Generates getters, setters, equals, hashCode, and toString
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "wardrobe")
public class ClothingItem {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "item_name")
    private String itemName;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] descriptionVector;
}