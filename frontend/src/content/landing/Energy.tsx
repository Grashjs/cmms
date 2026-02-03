import IndustryLayout from 'src/layouts/IndustryLayout';

const facilityManagementData = {
  pageTitle: 'Open Source Facility Management Software',
  headerTitle: 'Energy & Utilities Maintenance Software',
  headerSubtitle: 'Protect your critical infrastructure and services',
  headerImageUrl:
    'https://limble.com/wp-content/uploads/industries_energy_and_utilities_header.webp',
  companyLogos: [
    'https://limblecmms.com/wp-content/uploads/client-color-energy-ecoenergy.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-energy-primagaz.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-energy-pinonmidstream.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-energy-ethosenergy.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-energy-geoquipmarine.svg'
  ],
  features: [
    {
      title: 'Modernize work orders',
      description:
        'Leave post-its and emails behind for an easy-to-adopt digital work order system accessible from wherever you happen to be.',
      imageUrl:
        'https://limble.com/wp-content/uploads/Industry-preventive-image.png',
      learnMoreUrl: 'https://limblecmms.com/cmms/work-order-software/'
    },
    {
      title: 'Automate PMs',
      description:
        'Streamline preventive maintenance and parts reordering so you’re never out of commission during crucial production hours.',
      imageUrl:
        'https://limble.com/wp-content/uploads/Industry-parts-image-2.png',
      learnMoreUrl:
        'https://limblecmms.com/cmms/preventive-maintenance-software/'
    }
  ],
  testimonials: [
    {
      text: 'The City of Lompoc’s water treatment plant transformed preventive maintenance and reduced time to resolution.',
      author: 'City of Lompoc',
      company: ''
    },
    {
      text: 'Allagash Brewery used Limble’s metrics to get buy-in for a technical role they knew they needed.',
      author: 'Allagash Brewery',
      company: ''
    }
  ],
  faqs: [
    {
      question: 'What is CMMS software?',
      answer:
        'CMMS (Computerized Maintenance Management System) software helps businesses manage, automate, and streamline all of their maintenance operations.'
    },
    {
      question: 'Is Limble the same as an ERP?',
      answer:
        'Enterprise Resource Planning (ERP) is a comprehensive software solution streamlining cross-departmental operations with specific modules, while CMMS is a specialized tool focusing on maintenance management with highly customizable features. Businesses can choose to enhance their ERP by integrating it with a CMMS.'
    }
  ],
  relatedContent: [
    {
      title: 'What is Utility Asset Management?',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-productive-1.png',
      url: 'https://limble.com/maintenance-definitions/utility-asset-management/'
    },
    {
      title: '7 Key Trends in Utility Asset Management',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-machine.png',
      url: 'https://limble.com/blog/utility-asset-management-trends/'
    },
    {
      title: 'The Basics of Oil and Gas Maintenance',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-steps.png',
      url: 'https://limble.com/blog/the-basics-of-oil-and-gas-maintenance/'
    }
  ]
};

function EnergyPage() {
  return <IndustryLayout {...facilityManagementData} />;
}

export default EnergyPage;
