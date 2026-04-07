package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the decrypted license data from a license.lic file.
 * Structure follows Keygen.sh license file payload format.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Decrypted license data from a license.lic file, following Keygen.sh payload format")
public class DecryptedLicenseData {

    /**
     * The license data object containing license attributes and relationships.
     */
    @JsonProperty("data")
    @Schema(description = "The primary license data object")
    private LicenseDataObject data;

    /**
     * Included related resources (licenses, products, etc.).
     */
    @JsonProperty("included")
    @Schema(description = "Included related resources such as licenses and products")
    private List<IncludedResource> included;

    /**
     * Metadata about the license file including issued and expiry timestamps.
     */
    @JsonProperty("meta")
    @Schema(description = "Metadata about the license file including issued and expiry timestamps")
    private LicenseFileMeta meta;

    /**
     * Checks if the license file is valid based on issued and expiry timestamps.
     *
     * @return true if the license file is within its valid time window
     */
    public boolean isTimeValid() {
        if (meta == null) {
            return false;
        }

        Date issued = meta.getIssued();
        Date expiry = meta.getExpiry();
        Date now = new Date();

        // Check that issued is not in the future (clock tampering)
        if (issued != null && issued.after(now)) {
            return false;
        }

        // Check that expiry is not in the past
        if (expiry != null && expiry.before(now)) {
            return false;
        }

        return true;
    }

    /**
     * Gets the license key from the included license resource.
     */
    public String getLicenseKey() {
        if (included != null) {
            for (IncludedResource resource : included) {
                if ("licenses".equals(resource.getType())) {
                    return resource.getAttributes() != null ? resource.getAttributes().getKey() : null;
                }
            }
        }
        return null;
    }

    /**
     * Gets the license name from the included license resource.
     */
    public String getLicenseName() {
        return this.getData().getAttributes().get("name").toString();
    }

    /**
     * Gets the users count from the license metadata.
     */
    public int getUsersCount() {
        Map<String, Object> metadata = (Map<String, Object>) this.getData().getAttributes().get("metadata");
        return Integer.parseInt(metadata.get("usersCount").toString());
    }

    public Set<String> getEntitlements() {
        Map<String, Object> relationships = this.getData().getRelationships();
        Map<String, Object> entitlements = (Map<String, Object>) relationships.get("entitlements");
        List<Map<String, Object>> entitlementObjects = (List<Map<String, Object>>) entitlements.get("data");
        List<String> ids =
                entitlementObjects.stream().map(entitlementObject -> (String) entitlementObject.get("id")).toList();
        return ids.stream().map(id -> this.getIncluded().stream().filter(includedResource -> includedResource.getType().equals(
                "entitlements") && includedResource.getId().equals(id)).findFirst().get().getAttributes().getCode()).collect(Collectors.toSet());
    }

    /**
     * The license data object (machine or license document).
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Primary license data object containing ID, type, attributes, relationships, and links")
    public static class LicenseDataObject {
        @JsonProperty("id")
        @Schema(description = "Unique identifier for the license")
        private String id;

        @JsonProperty("type")
        @Schema(description = "Resource type, typically 'machine' or 'license'")
        private String type;

        @JsonProperty("attributes")
        @Schema(description = "Dynamic attributes map containing license properties")
        private Map<String, Object> attributes;

        @JsonProperty("relationships")
        @Schema(description = "Dynamic relationships map to related resources")
        private Map<String, Object> relationships;

        @JsonProperty("links")
        @Schema(description = "HATEOAS links for the resource")
        private Map<String, String> links;
    }

    /**
     * Included resource representing related entities.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Included resource representing a related entity in the license payload")
    public static class IncludedResource {
        @JsonProperty("id")
        @Schema(description = "Unique identifier for the resource")
        private String id;

        @JsonProperty("type")
        @Schema(description = "Resource type (e.g., 'licenses', 'products', 'entitlements')")
        private String type;

        @JsonProperty("attributes")
        @Schema(description = "Attributes specific to this resource type")
        private LicenseAttributes attributes;

        @JsonProperty("relationships")
        @Schema(description = "Relationships to other resources")
        private Map<String, Object> relationships;

        @JsonProperty("links")
        @Schema(description = "HATEOAS links for the resource")
        private Map<String, String> links;
    }

    /**
     * License attributes from included resources.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Attributes of an included license resource")
    public static class LicenseAttributes {
        @Schema(description = "Display name of the resource")
        private String name;

        @Schema(description = "License key value")
        private String key;

        @Schema(description = "Unique code identifier")
        private String code;

        @Schema(description = "Expiration timestamp")
        private String expiry;

        @Schema(description = "Status of the resource")
        private String status;

        @Schema(description = "Additional metadata")
        private Map<String, Object> metadata;

        @Schema(description = "Creation timestamp")
        private String created;

        @Schema(description = "Last updated timestamp")
        private String updated;
    }

    /**
     * License file metadata including timing information.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Metadata about the license file including issued and expiry timestamps")
    public static class LicenseFileMeta {
        @JsonProperty("issued")
        @Schema(description = "Timestamp when the license was issued")
        private String issued;

        @JsonProperty("expiry")
        @Schema(description = "Timestamp when the license expires")
        private String expiry;

        @JsonProperty("ttl")
        @Schema(description = "Time-to-live in seconds for the license file")
        private Long ttl;

        /**
         * Parses the issued timestamp to a Date object.
         */
        public Date getIssued() {
            if (issued == null || issued.isEmpty()) {
                return null;
            }
            try {
                return Date.from(java.time.Instant.parse(issued));
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Parses the expiry timestamp to a Date object.
         */
        public Date getExpiry() {
            if (expiry == null || expiry.isEmpty()) {
                return null;
            }
            try {
                return Date.from(java.time.Instant.parse(expiry));
            } catch (Exception e) {
                return null;
            }
        }
    }
}
