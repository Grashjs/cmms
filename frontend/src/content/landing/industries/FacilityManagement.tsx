import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const facilityManagementData: IndustryLayoutProps = {
  pageTitle: 'Open Source Facility Maintenance Software',
  headerTitle: 'Maintenance Management for Real-World Facilities',
  headerSubtitle:
    'Plan, track, and optimize maintenance across buildings, assets, and teams—deployed on your own infrastructure or instantly in the cloud',
  headerImageUrl: '/static/images/industries/facility-hero.jpg',
  kpis: [
    {
      title: 'Improvement in work order completion rates',
      value: '53',
      type: 'percentage'
    },
    {
      title: 'Increase in equipment uptime',
      value: '38',
      type: 'percentage'
    },
    {
      title: 'Increase in inspections completed on time\n',
      value: '49',
      type: 'percentage'
    }
  ],
  companyLogos: [
    'https://www.datocms-assets.com/38028/1606253591-yamahalogo.svg',
    'https://www.datocms-assets.com/38028/1624813847-unilever-dark-blue.svg',
    'https://www.datocms-assets.com/38028/1606253522-marriottinternational.svg',
    'https://www.datocms-assets.com/38028/1624814616-mccormick-icon.svg',
    'https://www.datocms-assets.com/38028/1761261081-pepsi-logo.svg',
    'https://www.datocms-assets.com/38028/1624814005-aramark-logo.svg',
    'https://www.datocms-assets.com/38028/1624814104-subway-logo.svg'
  ], // keep empty or add community / open-source adopters later
  features: [
    {
      title: 'Centralized maintenance operations',
      description:
        'Manage work orders, assets, locations, and technicians from a single CMMS designed for day-to-day maintenance reality.',
      imageUrl: 'https://atlas-cmms.com/assets/features/work-orders.png',
      learnMoreUrl: '/features/work-orders'
    },
    {
      title: 'Preventive maintenance that actually runs',
      description:
        'Schedule recurring maintenance based on time, usage, or custom rules to reduce breakdowns and extend asset lifespan.',
      imageUrl:
        'https://atlas-cmms.com/assets/features/preventive-maintenance.png',
      learnMoreUrl: '/features/preventive-maintenance'
    },
    {
      title: 'Asset-first structure',
      description:
        'Track equipment history, costs, documents, and relationships between assets without ERP-level complexity.',
      imageUrl: 'https://atlas-cmms.com/assets/features/assets.png',
      learnMoreUrl: '/features/assets'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS helped us move from spreadsheets to a structured maintenance workflow without locking us into a vendor.',
      author: 'Maintenance Supervisor',
      company: 'Manufacturing Facility'
    },
    {
      text: 'Being able to self-host Atlas CMMS was a decisive factor for our IT and compliance requirements.',
      author: 'Technical Lead',
      company: 'Infrastructure Services'
    }
  ],

  faqs: [
    {
      question: 'What makes Atlas CMMS different from other CMMS tools?',
      answer:
        'Atlas CMMS is open source, self-hostable, and designed for organizations that want full control over their data, workflows, and infrastructure.'
    },
    {
      question: 'Is Atlas CMMS suitable for facility maintenance?',
      answer:
        'Yes. Atlas CMMS supports buildings, equipment, locations, preventive maintenance, corrective work orders, and multi-site operations.'
    },
    {
      question: 'Can Atlas CMMS be deployed on-premise?',
      answer:
        'Absolutely. Atlas CMMS can run fully on-premise using Docker, or be deployed in the cloud depending on your needs.'
    }
  ],

  relatedContent: []
};

function FacilityManagementPage() {
  return (
    <IndustryLayout {...facilityManagementData}>
      {/* Additional custom content can be added here as children */}
    </IndustryLayout>
  );
}

export default FacilityManagementPage;
