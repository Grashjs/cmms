package com.grash.controller;

import com.grash.model.Feature;
import com.grash.model.OwnUser;
import com.grash.model.UserFeaturePermission;
import com.grash.repository.FeatureRepository;
import com.grash.repository.UserFeaturePermissionRepository;
import com.grash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-features")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserFeatureController {
    
    private final UserFeaturePermissionRepository permissionRepository;
    private final FeatureRepository featureRepository;
    private final UserRepository userRepository;
    
    /**
     * Kullanıcının aktif özelliklerini getir
     * Eğer permission yoksa, tüm özellikler açık kabul edilir
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserFeatures(@PathVariable Long userId) {
        List<UserFeaturePermission> permissions = permissionRepository.findByUserId(userId);
        List<Feature> allFeatures = featureRepository.findByIsActiveTrue();
        
        Map<String, Object> response = new HashMap<>();
        
        if (permissions.isEmpty()) {
            // Hiç permission yoksa tüm özellikler açık
            Map<String, Boolean> features = allFeatures.stream()
                .collect(Collectors.toMap(Feature::getCode, f -> true));
            response.put("features", features);
            response.put("hasCustomPermissions", false);
        } else {
            // Custom permissions var
            Map<String, Boolean> features = permissions.stream()
                .collect(Collectors.toMap(
                    p -> p.getFeature().getCode(),
                    UserFeaturePermission::getIsEnabled
                ));
            response.put("features", features);
            response.put("hasCustomPermissions", true);
        }
        
        response.put("userId", userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Kullanıcıya özellik izni ver/kaldır
     */
    @PostMapping("/user/{userId}/feature/{featureCode}")
    public ResponseEntity<UserFeaturePermission> setUserFeature(
            @PathVariable Long userId,
            @PathVariable String featureCode,
            @RequestParam Boolean enabled) {
        
        OwnUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Feature feature = featureRepository.findByCode(featureCode)
            .orElseThrow(() -> new RuntimeException("Feature not found"));
        
        Optional<UserFeaturePermission> existing = 
            permissionRepository.findByUserIdAndFeatureCode(userId, featureCode);
        
        UserFeaturePermission permission;
        
        if (existing.isPresent()) {
            permission = existing.get();
            permission.setIsEnabled(enabled);
        } else {
            permission = new UserFeaturePermission();
            permission.setUser(user);
            permission.setFeature(feature);
            permission.setIsEnabled(enabled);
        }
        
        permission = permissionRepository.save(permission);
        return ResponseEntity.ok(permission);
    }
    
    /**
     * Kullanıcının tüm custom permission'larını sıfırla
     */
    @DeleteMapping("/user/{userId}/reset")
    public ResponseEntity<?> resetUserPermissions(@PathVariable Long userId) {
        List<UserFeaturePermission> permissions = permissionRepository.findByUserId(userId);
        permissionRepository.deleteAll(permissions);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All custom permissions removed. User now has default access.");
        response.put("deletedCount", permissions.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Tüm özellikleri listele
     */
    @GetMapping("/features")
    public ResponseEntity<List<Feature>> getAllFeatures() {
        List<Feature> features = featureRepository.findByIsActiveTrue();
        return ResponseEntity.ok(features);
    }
    
    /**
     * Kullanıcının bir özelliğe erişimi var mı?
     */
    @GetMapping("/user/{userId}/can-access/{featureCode}")
    public ResponseEntity<Boolean> canAccess(
            @PathVariable Long userId,
            @PathVariable String featureCode) {
        
        Optional<UserFeaturePermission> permission = 
            permissionRepository.findByUserIdAndFeatureCode(userId, featureCode);
        
        // Permission yoksa varsayılan: true (erişebilir)
        // Permission varsa: is_enabled değerine bak
        boolean canAccess = permission.map(UserFeaturePermission::getIsEnabled).orElse(true);
        
        return ResponseEntity.ok(canAccess);
    }
}
