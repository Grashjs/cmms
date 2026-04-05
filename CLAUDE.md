
## Yapılacak: Şirket Özellik Override Sistemi

Plan + Override mimarisi:
- Her şirket bir plan seçer (Free/Starter/Professional/Business)
- Plan bazlı özellikler otomatik gelir (PlanFeatures enum)
- Superadmin panelinden her özellik ayrıca açılıp kapatılabilir

Gerekli değişiklikler:
1. DB: company_feature_overrides tablosu (company_id, feature, enabled)
2. Backend: CompanyService'e getEffectiveFeatures metodu ekle
3. Backend: SuperAdminController'a PATCH /superadmin/companies/{id}/features endpoint
4. Frontend: CompanyDetail.tsx'e özellik toggle listesi ekle
5. Frontend: hasFeature fonksiyonu override'ları da kontrol etmeli

PlanFeatures enum:
PREVENTIVE_MAINTENANCE, CHECKLIST, FILE, PURCHASE_ORDER, METER,
REQUEST_CONFIGURATION, ADDITIONAL_TIME, ADDITIONAL_COST, ANALYTICS,
REQUEST_PORTAL, SIGNATURE, ROLE, WORKFLOW, API_ACCESS, WEBHOOK, IMPORT_CSV
