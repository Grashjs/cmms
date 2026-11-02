package com.grash.dto.offline;

public record MobileOfflineAsset(Long id, String name, String customId, String description, String barCode,
                                  String nfcId, String status, String model, String serialNumber,
                                  String manufacturer, String power, String area) {}
