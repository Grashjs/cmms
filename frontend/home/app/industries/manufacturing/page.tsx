'use client';
import IndustryLayout, {
  IndustryLayoutProps
} from '../../../../layouts/IndustryLayout';

const manufacturingData: IndustryLayoutProps = {
  pageTitle: 'Open Source Manufacturing Maintenance Software',
  headerTitle: 'Maintenance Management for Manufacturing Operations',
  headerSubtitle:
    'Reduce downtime, extend equipment life, and keep production running smoothly with an open-source CMMS built for modern factories—self-hosted or in the cloud',
  headerImageUrl: '/static/images/industries/manufacturing-hero.jpg',
  canonicalPath: 'industries/open-source-manufacturing-maintenance-software',

  kpis: [
    {
      title: 'Reduction in unplanned downtime',
      value: '47',
      type: 'percentage'
    },
    {
      title: 'Increase in preventive maintenance compliance',
      value: '52',
      type: 'percentage'
    },
    {
      title: 'Improvement in production asset availability',
      value: '41',
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
  ],

  features: [
    {
      title: 'Production-ready work order management',
      description:
        'Create, prioritize, and track maintenance work orders across machines, lines, and facilities to keep production flowing.',
      imageUrl: 'https://atlas-cmms.com/assets/features/work-orders.png',
      learnMoreUrl: '/features/work-orders'
    },
    {
      title: 'Preventive maintenance for critical equipment',
      description:
        'Automate maintenance schedules based on runtime, cycles, or calendar intervals to prevent costly breakdowns.',
      imageUrl:
        'https://atlas-cmms.com/assets/features/preventive-maintenance.png',
      learnMoreUrl: '/features/preventive-maintenance'
    },
    {
      title: 'Full asset history and cost tracking',
      description:
        'Monitor maintenance history, spare parts usage, downtime causes, and lifecycle costs for every production asset.',
      imageUrl: 'https://atlas-cmms.com/assets/features/assets.png',
      learnMoreUrl: '/features/assets'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS helped us significantly reduce machine downtime while keeping full control of our data on-premise.',
      author: 'Plant Maintenance Manager',
      company: 'Industrial Manufacturing Company'
    },
    {
      text: 'The preventive maintenance automation alone saved hours of manual planning every week.',
      author: 'Operations Engineer',
      company: 'Production Facility'
    }
  ],

  faqs: [
    {
      question: 'Is Atlas CMMS suitable for manufacturing environments?',
      answer:
        'Yes. Atlas CMMS is designed to manage machines, production lines, spare parts, preventive maintenance, and multi-site factory operations.'
    },
    {
      question: 'Can Atlas CMMS help reduce equipment downtime?',
      answer:
        'Absolutely. Preventive scheduling, work order tracking, and asset history insights help teams detect issues early and avoid unexpected failures.'
    },
    {
      question: 'Does Atlas CMMS support on-premise deployment for factories?',
      answer:
        'Yes. Atlas CMMS can be fully self-hosted using Docker, making it ideal for factories with strict security or network requirements.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source manufacturing maintenance that crushes downtime. Scale your production, automate PM cycles, and keep your factory floor running with a powerful, self-hosted CMMS.'
};

function ManufacturingPage() {
  return (
    <IndustryLayout {...manufacturingData}>
      {/* Additional custom content can be added here as children */}
    </IndustryLayout>
  );
}

export default ManufacturingPage;
