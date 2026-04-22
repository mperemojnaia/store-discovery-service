package com.jumbo.closeststores.controller.mapper;

import com.jumbo.closeststores.api.model.StoreItem;
import com.jumbo.closeststores.api.model.StoreResponse;
import com.jumbo.closeststores.model.StoreLocatorResult;
import com.jumbo.closeststores.model.StoreWithDistance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreResponseMapper {

    @Mapping(source = "store.city", target = "city")
    @Mapping(source = "store.postalCode", target = "postalCode")
    @Mapping(source = "store.street", target = "street")
    @Mapping(source = "store.street2", target = "street2")
    @Mapping(source = "store.street3", target = "street3")
    @Mapping(source = "store.addressName", target = "addressName")
    @Mapping(source = "store.uuid", target = "uuid")
    @Mapping(source = "store.longitude", target = "longitude")
    @Mapping(source = "store.latitude", target = "latitude")
    @Mapping(source = "store.complexNumber", target = "complexNumber")
    @Mapping(source = "store.showWarningMessage", target = "showWarningMessage")
    @Mapping(source = "store.todayOpen", target = "todayOpen")
    @Mapping(source = "store.locationType", target = "locationType")
    @Mapping(source = "store.collectionPoint", target = "collectionPoint")
    @Mapping(source = "store.sapStoreID", target = "sapStoreID")
    @Mapping(source = "store.todayClose", target = "todayClose")
    @Mapping(source = "distanceKm", target = "distance")
    StoreItem toStoreItem(StoreWithDistance storeWithDistance);

    default StoreResponse toStoreResponse(StoreLocatorResult result) {
        StoreResponse response = new StoreResponse();
        response.setStores(toStoreItems(result.stores()));
        response.setDistanceType(
                StoreResponse.DistanceTypeEnum.fromValue(result.distanceStrategy().getValue()));
        return response;
    }

    List<StoreItem> toStoreItems(List<StoreWithDistance> stores);
}
