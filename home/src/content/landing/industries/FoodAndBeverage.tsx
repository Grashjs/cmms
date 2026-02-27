import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const foodBeverageData: IndustryLayoutProps = {
  pageTitle: 'Open Source Food & Beverage Maintenance Software',
  headerTitle: 'Maintenance Management for Food & Beverage Operations',
  headerSubtitle:
    'Ensure product quality, meet safety regulations, and keep production running with an open-source CMMS built for food processing plants, factories, and distribution facilities—available in the cloud or self-hosted.',
  headerImageUrl: '/static/images/industries/food-hero.jpg',
  canonicalPath:
    'industries/open-source-food-and-beverage-maintenance-software',

  kpis: [
    {
      title: 'Reduction in unplanned production downtime',
      value: '45',
      type: 'percentage'
    },
    {
      title: 'Increase in preventive maintenance completion',
      value: '50',
      type: 'percentage'
    },
    {
      title: 'Improvement in equipment reliability and uptime',
      value: '42',
      type: 'percentage'
    }
  ],

  companyLogos: [
    'https://www.datocms-assets.com/38028/1624813847-unilever-dark-blue.svg',
    'https://www.datocms-assets.com/38028/1761261081-pepsi-logo.svg',
    'https://www.datocms-assets.com/38028/1624814005-aramark-logo.svg',
    'https://www.datocms-assets.com/38028/1624814104-subway-logo.svg',
    'https://www.datocms-assets.com/38028/1606253522-marriottinternational.svg'
  ],

  features: [
    {
      title: 'Preventive maintenance for hygienic production',
      description:
        'Automate maintenance schedules for mixers, conveyors, refrigeration systems, and packaging lines to avoid contamination risks and unexpected shutdowns.',
      imageUrl:
        'https://atlas-cmms.com/assets/features/preventive-maintenance.png',
      learnMoreUrl: '/features/preventive-maintenance'
    },
    {
      title: 'Traceable work orders and compliance records',
      description:
        'Maintain complete maintenance logs, inspections, and corrective actions to support food safety standards, audits, and regulatory compliance.',
      imageUrl: 'https://atlas-cmms.com/assets/features/work-orders.png',
      learnMoreUrl: '/features/work-orders'
    },
    {
      title: 'Spare parts and cold-chain asset monitoring',
      description:
        'Track spare parts availability, monitor critical refrigeration assets, and reduce spoilage caused by equipment failure or delayed repairs.',
      imageUrl: 'https://atlas-cmms.com/assets/features/assets.png',
      learnMoreUrl: '/features/assets'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS helped us maintain strict food safety standards while significantly reducing unexpected equipment downtime.',
      author: 'Quality & Maintenance Manager',
      company: 'Food Processing Company'
    },
    {
      text: 'Self-hosting Atlas CMMS allowed us to keep full control of operational data while improving preventive maintenance execution.',
      author: 'Plant Operations Director',
      company: 'Beverage Manufacturing Facility'
    }
  ],

  faqs: [
    {
      question: 'Why is CMMS important for food and beverage companies?',
      answer:
        'Food and beverage operations require strict hygiene, traceability, and equipment reliability. A CMMS centralizes maintenance planning, inspections, and asset history to ensure safety, compliance, and continuous production.'
    },
    {
      question: 'Can Atlas CMMS support regulatory compliance and audits?',
      answer:
        'Yes. Atlas CMMS stores maintenance records, inspection reports, and corrective actions, helping teams prepare for food safety audits and regulatory requirements.'
    },
    {
      question:
        'Is Atlas CMMS suitable for refrigerated and cold-chain equipment?',
      answer:
        'Absolutely. Atlas CMMS tracks maintenance schedules, failures, and spare parts for refrigeration and temperature-sensitive assets to reduce spoilage risks.'
    },
    {
      question: 'Can Atlas CMMS be deployed on-premise in food factories?',
      answer:
        'Yes. Atlas CMMS is fully self-hosted and Docker-ready, making it ideal for facilities with strict security, privacy, or network isolation requirements.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source food and beverage maintenance for ultimate safety and uptime. Automate inspections, meet strict compliance standards, and own your production data with a self-hosted CMMS.'
};

function FoodBeveragePage() {
  return <IndustryLayout {...foodBeverageData} />;
}

export default FoodBeveragePage;

