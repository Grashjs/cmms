// Pricing plans
import { TFunction } from 'react-i18next';

export const getPricingPlans = (
  t: TFunction
): {
  id: string;
  name: string;
  price: string;
  description: string;
  popular: boolean;
  features: string[];
  link?: string;
}[] => [
  {
    id: 'basic',
    name: t('pricing.plan_basic_name'),
    price: t('free'),
    description: t('pricing.plan_basic_description'),
    popular: false,
    features: [
      t('pricing.feature_unlimited_work_orders'),
      t('pricing.feature_custom_tasks'),
      t('pricing.feature_unlimited_request_user_licenses'),
      t('asset_management')
    ]
  },
  {
    id: 'starter',
    name: t('pricing.plan_starter_name'),
    price: '10',
    description: t('pricing.plan_starter_description'),
    popular: false,
    features: [
      t('pricing.feature_everything_in_basic_plus'),
      t('pricing.feature_custom_checklists'),
      t('pricing.feature_inventory_management_costing'),
      t('pricing.feature_time_and_manpower_tracking'),
      t('pricing.feature_thirty_day_analytics_reporting')
    ]
  },
  {
    id: 'professional',
    name: t('pricing.plan_professional_name'),
    price: '15',
    description: t('pricing.plan_professional_description'),
    popular: true,
    features: [
      t('pricing.feature_everything_in_starter_plus'),
      t('pricing.feature_multiple_inventory_lines'),
      t('signature'),
      t('pricing.feature_customizable_request_portal'),
      t('pricing.feature_mobile_offline_mode'),
      t('pricing.feature_advanced_analytics_reporting')
    ]
  },
  {
    id: 'business',
    name: t('pricing.plan_business_name'),
    price: '40',
    description: t('pricing.plan_business_description'),
    popular: false,
    features: [
      'Everything in Professional plus:',
      t('workflow_automation'),
      t('pricing.feature_purchase_order_management'),
      t('pricing.feature_multi_site_module_support'),
      t('API_ACCESS_feature'),
      t('pricing.feature_custom_work_order_statuses'),
      t('pricing.feature_custom_integrations_support'),
      t('custom_dashboards'),
      t('ROLE_feature'),
      t('SSO')
    ]
  }
];

export const getPlanFeatureCategories = (
  t: TFunction
): {
  name: string;
  features: {
    name: string;
    availability: { [key: string]: boolean | string };
  }[];
}[] => [
  {
    name: t('work_orders'),
    features: [
      {
        name: t('pricing.feature_work_order_management'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_recurring_work_orders'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_custom_categories'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_data_importing'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('preventive_maintenance'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('checklists'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_time_and_cost_tracking'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('signature'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_work_order_configuration'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_custom_work_order_statuses'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('workflow_automation'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('request_system'),
    features: [
      {
        name: t('pricing.feature_internal_requests'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_external_request_portal'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_customizable_request_portal'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('pricing.category_locations_assets_parts'),
    features: [
      {
        name: t('pricing.feature_location_management'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('asset_management'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_barcode_scanning'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_inventory_management'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('meter_reading'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('FILE_feature'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_custom_asset_statuses'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_multiple_inventory_lines'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_asset_downtime_tracking'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_asset_depreciation_tracking'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_asset_warranty_tracking'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_purchase_orders_management'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_asset_check_in_out'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('pricing.category_mobile_offline'),
    features: [
      {
        name: t('pricing.feature_work_order_availability'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_updating_status'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_updating_tasks'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_work_order_drafts'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('Analytics'),
    features: [
      {
        name: t('pricing.feature_full_drill_down_reporting_history'),
        availability: {
          basic: true,
          starter: t('pricing.period_thirty_days'),
          professional: t('pricing.period_full'),
          business: t('full'),
          'sh-free': true,
          'sh-professional': t('full'),
          'sh-business': 'Full'
        }
      },
      {
        name: t('pricing.feature_pdf_csv_exporting'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_itemized_time_reporting'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_user_login_reports'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('requests_analysis'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_parts_consumption_reports'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('custom_dashboards'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_asset_downtime_reports'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_equipment_reliability_reports'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_multi_site_modules'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('pricing.category_integrations'),
    features: [
      {
        name: t('API_ACCESS_feature'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_business_integrations'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_custom_integrations'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('people_teams'),
    features: [
      {
        name: t('pricing.feature_unlimited_view_only_users'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_unlimited_requesters'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_unlimited_vendors'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_unlimited_customers'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('teams'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('ROLE_feature'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('SSO'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('updates'),
    features: [
      {
        name: t('push_notifications'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_daily_email_digest'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('email_notifications'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('pricing.category_support'),
    features: [
      {
        name: t('pricing.feature_articles'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_training_webinars'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_community_support_discord'),
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_email_support'),
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_chat_phone_support'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_priority_support'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_implementation_training'),
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-business': true
        }
      },
      {
        name: t('pricing.feature_dedicated_account_manager'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  },
  {
    name: t('pricing.category_customization'),
    features: [
      {
        name: t('pricing.feature_custom_development'),
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-business': true
        }
      }
    ]
  }
];
// Self-Hosted Pricing plans

export const getSelfHostedPlans = (
  t: TFunction
): {
  id: string;
  name: string;
  price: string;
  description: string;
  popular: boolean;
  features: string[];
}[] => {
  return [
    {
      id: 'sh-free',
      name: t('pricing.sh_plan_basic_name'),
      price: t('free'),
      description: t('pricing.sh_plan_basic_description'),
      popular: false,
      features: [
        t('pricing.sh_feature_core_work_order_management'),
        t('pricing.sh_feature_asset_inventory_tracking'),
        t('pricing.feature_preventive_maintenance_recurring_work_orders'),
        t('pricing.sh_feature_local_data_storage')
      ]
    },
    {
      id: 'sh-professional',
      name: t('pricing.sh_plan_professional_name'),
      price: '15',
      description: t('pricing.sh_plan_professional_description'),
      popular: true,
      features: [
        t('pricing.sh_feature_everything_in_basic_plus'),
        t('pricing.sh_feature_unlimited_assets_checklists'),
        t('FILE_feature'),
        t('meter_reading'),
        t('pricing.sh_feature_nfc_barcode_scanning'),
        t('pricing.sh_feature_email_support')
      ]
    },
    {
      id: 'sh-business',
      name: t('pricing.sh_plan_business_name'),
      price: '40',
      description: t('pricing.sh_plan_business_description'),
      popular: false,
      features: [
        t('pricing.sh_feature_everything_in_professional_plus'),
        t('pricing.sh_feature_multi_instance_management'),
        t('SSO'),
        t('pricing.sh_feature_custom_user_roles'),
        t('workflow_automation'),
        // 'Webhook Integration',
        t('API_ACCESS_feature'),
        t('pricing.sh_feature_priority_implementation_support')
      ]
    }
  ];
};
