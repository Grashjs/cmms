package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the decrypted license data from a license.lic file.
 * Structure follows Keygen.sh license file payload format.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DecryptedLicenseData {

    /**
     * The license data object containing license attributes and relationships.
     */
    @JsonProperty("data")
    private LicenseDataObject data;

    /**
     * Included related resources (licenses, products, etc.).
     */
    @JsonProperty("included")
    private List<IncludedResource> included;

    /**
     * Metadata about the license file including issued and expiry timestamps.
     */
    @JsonProperty("meta")
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
    public static class LicenseDataObject {
        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("attributes")
        private Map<String, Object> attributes;

        @JsonProperty("relationships")
        private Map<String, Object> relationships;

        @JsonProperty("links")
        private Map<String, String> links;
    }

    /**
     * Included resource representing related entities.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncludedResource {
        @JsonProperty("id")
        private String id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("attributes")
        private LicenseAttributes attributes;

        @JsonProperty("relationships")
        private Map<String, Object> relationships;

        @JsonProperty("links")
        private Map<String, String> links;
    }

    /**
     * License attributes from included resources.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LicenseAttributes {
        private String name;

        private String key;

        private String code;

        private String expiry;

        private String status;

        private Map<String, Object> metadata;

        private String created;

        private String updated;
    }

    /**
     * License file metadata including timing information.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LicenseFileMeta {
        @JsonProperty("issued")
        private String issued;

        @JsonProperty("expiry")
        private String expiry;

        @JsonProperty("ttl")
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
