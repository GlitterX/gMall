package com.gmall.catalog.interfaces.http;

import com.gmall.catalog.application.CatalogCommandService;
import com.gmall.catalog.application.CatalogQueryModels;
import com.gmall.catalog.application.CatalogQueryModels.DecorationProductView;
import com.gmall.catalog.application.CatalogQueryModels.MerchantOfferProductView;
import com.gmall.catalog.application.CatalogQueryModels.ProductDetailView;
import com.gmall.catalog.application.CatalogQueryModels.SourceProductView;
import com.gmall.catalog.application.CatalogQueryService;
import com.gmall.catalog.application.CreateSourceProductCommand;
import com.gmall.catalog.application.DecorationProjectionRequest;
import com.gmall.catalog.application.DeriveMerchantOfferProductCommand;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import com.gmall.catalog.application.UpdateMerchantOfferProductCommand;
import com.gmall.catalog.application.UpdateSourceProductCommand;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogCommandService catalogCommandService;
    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogCommandService catalogCommandService,
                             CatalogQueryService catalogQueryService) {
        this.catalogCommandService = catalogCommandService;
        this.catalogQueryService = catalogQueryService;
    }

    @PostMapping("/admin/source-products")
    public SourceProductView createSourceProduct(@RequestBody CreateSourceProductCommand command) {
        return catalogCommandService.createSourceProduct(command);
    }

    @PostMapping("/merchant/source-products/{sourceProductId}/offer-products")
    public MerchantOfferProductView deriveMerchantOfferProduct(@PathVariable String sourceProductId,
                                                               @RequestBody DeriveMerchantOfferProductCommand command) {
        return catalogCommandService.deriveMerchantOfferProduct(sourceProductId, command);
    }

    @PutMapping("/admin/source-products/{sourceProductId}")
    public SourceProductView updateSourceProduct(@PathVariable String sourceProductId,
                                                 @RequestBody UpdateSourceProductCommand command) {
        return catalogCommandService.updateSourceProduct(sourceProductId, command);
    }

    @PatchMapping("/merchant/offer-products/{merchantOfferProductId}")
    public MerchantOfferProductView updateMerchantOfferProduct(@PathVariable String merchantOfferProductId,
                                                               @RequestBody UpdateMerchantOfferProductCommand command) {
        return catalogCommandService.updateMerchantOfferProduct(merchantOfferProductId, command);
    }

    @GetMapping("/internal/products/{productViewId}")
    public ProductDetailView getProduct(@PathVariable String productViewId,
                                        @RequestParam String locale) {
        return catalogQueryService.getProduct(productViewId, locale);
    }

    @GetMapping("/internal/offer-skus/{merchantOfferSkuId}")
    public CatalogQueryModels.OfferSkuMappingView getOfferSkuMapping(@PathVariable String merchantOfferSkuId) {
        return catalogQueryService.getOfferSkuMapping(merchantOfferSkuId);
    }

    @GetMapping("/internal/skus/{businessSkuType}/{businessSkuId}/mapping")
    public CatalogQueryModels.BusinessSkuMappingView getBusinessSkuMapping(@PathVariable String businessSkuType,
                                                                           @PathVariable String businessSkuId) {
        return catalogQueryService.getBusinessSkuMapping(businessSkuType, businessSkuId);
    }

    @GetMapping("/admin/source-products/{sourceProductId}")
    public CatalogQueryModels.SourceProductManagementView getSourceProductManagement(@PathVariable String sourceProductId) {
        return catalogQueryService.getSourceProductManagement(sourceProductId);
    }

    @GetMapping("/admin/source-products")
    public CatalogQueryModels.PageResult<CatalogQueryModels.SourceProductSummaryView> listSourceProductManagement(@RequestParam String ownerId,
                                                                                                                    @RequestParam(required = false) String ownerType,
                                                                                                                    @RequestParam(required = false) String sourceMode,
                                                                                                                    @RequestParam(required = false) String productStatus,
                                                                                                                    @RequestParam(defaultValue = "1") int pageNo,
                                                                                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                                                                                    @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                                                                                    @RequestParam(defaultValue = "DESC") String sortDirection) {
        return catalogQueryService.listSourceProductManagement(ownerId, ownerType, sourceMode, productStatus, pageNo, pageSize, sortBy, sortDirection);
    }

    @GetMapping("/merchant/offer-products/{merchantOfferProductId}")
    public CatalogQueryModels.MerchantOfferProductManagementView getMerchantOfferProductManagement(@PathVariable String merchantOfferProductId) {
        return catalogQueryService.getMerchantOfferProductManagement(merchantOfferProductId);
    }

    @GetMapping("/merchant/offer-products")
    public CatalogQueryModels.PageResult<CatalogQueryModels.MerchantOfferProductSummaryView> listMerchantOfferProductManagement(@RequestParam String merchantId,
                                                                                                                               @RequestParam(required = false) String relationId,
                                                                                                                               @RequestParam(required = false) String offerStatus,
                                                                                                                               @RequestParam(defaultValue = "1") int pageNo,
                                                                                                                               @RequestParam(defaultValue = "20") int pageSize,
                                                                                                                               @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                                                                                               @RequestParam(defaultValue = "DESC") String sortDirection) {
        return catalogQueryService.listMerchantOfferProductManagement(merchantId, relationId, offerStatus, pageNo, pageSize, sortBy, sortDirection);
    }

    @PostMapping("/internal/projections/decoration")
    public List<DecorationProductView> getDecorationProducts(@RequestBody DecorationProjectionRequest request) {
        return catalogQueryService.getDecorationProducts(request.productViewIds(), request.locale());
    }
}
