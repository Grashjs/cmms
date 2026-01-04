package com.grash.repository;

import com.grash.model.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    List<Asset> findByCompany_Id(Long id);

    List<Asset> findByCompany_Id(Long id, Sort sort);

    List<Asset> findByCompany_IdAndParentAssetIsNull(Long id, Pageable pageable);

    List<Asset> findByParentAsset_Id(Long id, Sort sort);

    Integer countByParentAsset_Id(Long id);

    List<Asset> findByLocation_Id(Long id);

    List<Asset> findByNameIgnoreCaseAndCompany_Id(String assetName, Long companyId);

    Optional<Asset> findByIdAndCompany_Id(Long id, Long companyId);

    Optional<Asset> findByNfcIdAndCompany_Id(String nfcId, Long companyId);

    Optional<Asset> findByBarCodeAndCompany_Id(String data, Long id);

    List<Asset> findByCompany_IdAndCreatedAtBefore(Long id, Date date);

    void deleteByCompany_IdAndIsDemoTrue(Long companyId);

    @Query("SELECT CASE WHEN COUNT(a) > :threshold THEN true ELSE false END " +
            "FROM Asset a WHERE a.company.id = :companyId")
    boolean hasMoreThan(@Param("companyId") Long companyId, @Param("threshold") Long threshold);

}

