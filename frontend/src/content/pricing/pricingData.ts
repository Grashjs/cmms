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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Recurring Work Orders',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Categories',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Data Importing',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Preventive Maintenance',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Checklists',
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Time and Cost Tracking',
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Signature Capture',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Work Order Configuration',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Work Order Statuses',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Workflow Automation',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'External Request Portal',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Customizable Request Portal',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Barcode Scanning',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Inventory Management',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Meter Readings',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'File Upload',
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Asset Statuses',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Multiple Inventory Lines',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Downtime Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Depreciation Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Warranty Tracking',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Purchase Orders',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Check In/Check Out',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
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
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Updating Status',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Updating Tasks',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Work Order Drafts',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
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
          business: 'Full',
          'sh-free': true,
          'sh-professional': 'Full',
          'sh-enterprise': 'Full'
        }
      },
      {
        name: 'PDF and CSV Exporting',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Itemized Time Reporting',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'User Log-in Reports',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Requests Analysis',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Parts Consumption Reports',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Dashboards',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Asset Downtime Reports',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Equipment Reliability Reports',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Multi-site Modules',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
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
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Business Integrations(SAP/Oracle/Causal AI...)',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Integrations',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Unlimited Requesters',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Unlimited Vendors',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Unlimited Customers',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Teams',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Custom Roles',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'SSO',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Daily Email Digest',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Email Notifications',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
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
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Training Webinars',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Community Support (Discord)',
        availability: {
          basic: true,
          starter: true,
          professional: true,
          business: true,
          'sh-free': true,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Email Support',
        availability: {
          basic: false,
          starter: true,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Chat/Phone Support',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Priority Support',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      },
      {
        name: 'Implementation & Training',
        availability: {
          basic: false,
          starter: false,
          professional: true,
          business: true,
          'sh-free': false,
          'sh-professional': true,
          'sh-enterprise': true
        }
      },
      {
        name: 'Dedicated Account Manager',
        availability: {
          basic: false,
          starter: false,
          professional: false,
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
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
          business: true,
          'sh-free': false,
          'sh-professional': false,
          'sh-enterprise': true
        }
      }
    ]
  }
];
// Self-Hosted Pricing plans
export const selfHostedPlans: {
  id: string;
  name: string;
  price: string;
  description: string;
  popular: boolean;
  features: string[];
}[] = [
  {
    id: 'sh-free',
    name: 'Basic',
    price: 'Free',
    description:
      'For small teams needing full data sovereignty and basic maintenance tracking on their own infrastructure.',
    popular: false,
    features: [
      'Core Work Order Management',
      'Asset & Inventory Tracking',
      'Preventive Maintenance',
      'Local Data Storage'
    ]
  },
  {
    id: 'sh-professional',
    name: 'Professional',
    price: '15',
    description:
      'Ideal for growing plants that require advanced checklists, workflows, and deeper system integration.',
    popular: true,
    features: [
      'Everything in Basic plus:',
      'Custom Checklists & Procedures',
      'Automated Workflows',
      'Webhook Integration',
      'Meter Readings',
      'NFC & Barcode Scanning',
      'Email & Community Support'
    ]
  },
  {
    id: 'sh-enterprise',
    name: 'Enterprise',
    price: '100',
    description:
      'Full-scale operations requiring multi-instance management, custom branding, and top-tier security.',
    popular: false,
    features: [
      'Everything in Professional plus:',
      'Multi-Instance Management',
      'Single Sign-On (SSO)',
      'Custom User Roles',
      'White-label / Custom Branding',
      'Priority Implementation Support'
    ]
  }
];
