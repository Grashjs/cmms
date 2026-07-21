package com.grash.factory;

import com.grash.model.Asset;
import com.grash.model.enums.AssetStatus;

public final class AssetFactory {

    private AssetFactory() {
    }

    public static Asset createAsset(String name) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setStatus(AssetStatus.OPERATIONAL);
        return asset;
    }
}
