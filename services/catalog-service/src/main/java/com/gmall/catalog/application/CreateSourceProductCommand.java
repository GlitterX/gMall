package com.gmall.catalog.application;

import java.util.List;

public record CreateSourceProductCommand(String ownerType,
                                         String ownerId,
                                         String sourceMode,
                                         String categoryId,
                                         String brandId,
                                         ProductContentDocument productContent,
                                         List<SourceSkuInput> skus) {
}
