import { alpha } from '@mui/material/styles';

// Pricing plans
export const pricingPlans: {
  id: string;
  name: string;
  price: string;
  description: string;
  popular: boolean;
  features: string[];
  link?: string;
}[] = [
  {
    id: 'basic',
    name: 'Basic',
    price: 'Free',
    description:
      'Teams looking to track assets and create fundamental preventive maintenance schedules with procedures.',
    popular: false,
    features: [
      'Unlimited work orders',
      'Recurring work orders',
      'Custom tasks',
      'Unlimited Request User Licenses',
      'Asset Management'
    ]
  },
  {
    id: 'starter',
    name: 'Starter',
    price: '10',
    description:
      'Teams looking to build efficient and strong preventive maintenance through machine status and manpower visibility.',
    popular: false,
    features: [
      'Everything in Basic plus:',
      'Preventive Maintenance Optimization',
      'Custom Checklists',
      'Inventory management/Costing',
      'Time and Manpower Tracking',
      '30 day Analytics & Reporting'
    ]
  },
  {
    id: 'professional',
    name: 'Professional',
    price: '15',
    description:
      'Departments that need to leverage insights and analytics to drive further maintenance growth and productivity.',
    popular: true,
    features: [
      'Everything in Starter plus:',
      'Multiple Inventory Lines',
      'Signature Capture',
      'Customizable Request Portal',
      'Mobile Offline Mode',
      'Advanced Analytics & Reporting'
    ]
  },
  {
    id: 'business',
    name: 'Business',
    price: 'Custom Pricing',
    description:
      'Organizations ready to capture maintenance & operations data to manage multiple locations & system customization.',
    popular: false,
    features: [
      'Everything in Professional plus:',
      'Workflow Automation',
      'Downtime Tracking',
      'Reliability Tracking',
      'Purchase Order Management',
      'Multi-site Module Support',
      'Standard API Access',
      'Custom Work Order Statuses',
      'Custom Integrations Support',
      'Customizable Dashboards',
      'Custom Roles',
      'Single Sign On'
    ]
  }
];

export const planFeatureCategories = [
  {
    name: 'Work Orders',
    features: [
      {
        name: 'Work Order Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Recurring Work Orders',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Custom Categories',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Data Importing',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Preventive Maintenance',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Checklists',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Time and Cost Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Signature Capture',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Work Order Configuration',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Custom Work Order Statuses',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Workflow Automation',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      }
    ]
  },
  {
    name: 'Work Requests',
    features: [
      {
        name: 'Internal Requests',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'External Request Portal',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true
        }
      },
      {
        name: 'Customizable Request Portal',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true
        }
      }
    ]
  },
  {
    name: 'Locations, Assets, and Parts',
    features: [
      {
        name: 'Location Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Barcode Scanning',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Inventory Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Meter Readings',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'File Upload',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Custom Asset Statuses',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true
        }
      },
      {
        name: 'Multiple Inventory Lines',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Downtime Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Depreciation Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Warranty Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Purchase Orders',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Check In/Check Out',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      }
    ]
  },
  {
    name: 'Mobile Offline',
    features: [
      {
        name: 'Work Order Availability',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Updating Status',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Updating Tasks',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Work Order Drafts',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      }
    ]
  },
  {
    name: 'Analytics',
    features: [
      {
        name: 'Full Drill-Down Reporting & History',
        availability: {
          basic: true,
          starter: '30 Days',
          professional: 'Full',
          business: 'Full'
        }
      },
      {
        name: 'PDF and CSV Exporting',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Itemized Time Reporting',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'User Log-in Reports',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Requests Analysis',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Parts Consumption Reports',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Custom Dashboards',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Asset Downtime Reports',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Equipment Reliability Reports',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Multi-site Modules',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      }
    ]
  },
  {
    name: 'Integrations',
    features: [
      {
        name: 'API Access',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Business Integrations(SAP/Oracle/Causal AI...)',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Custom Integrations',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      }
    ]
  },
  {
    name: 'Users and Teams',
    features: [
      {
        name: 'Unlimited View-Only Users',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Unlimited Requesters',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Unlimited Vendors',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Unlimited Customers',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Teams',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Custom Roles',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'SSO',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      }
    ]
  },
  {
    name: 'Updates',
    features: [
      {
        name: 'Push Notifications',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Daily Email Digest',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Email Notifications',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      }
    ]
  },
  {
    name: 'Support',
    features: [
      {
        name: 'Articles',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Training Webinars',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Community Support (Discord)',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Email Support',
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true
        }
      },
      {
        name: 'Chat/Phone Support',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true
        }
      },
      {
        name: 'Priority Support',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      },
      {
        name: 'Implementation & Training',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true
        }
      },
      {
        name: 'Dedicated Account Manager',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      }
    ]
  },
  {
    name: 'Customization',
    features: [
      {
        name: 'Custom development',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true
        }
      }
    ]
  }
];
