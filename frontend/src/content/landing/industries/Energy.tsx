import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const energyUtilitiesData: IndustryLayoutProps = {
  pageTitle: 'Open Source Energy & Utilities Maintenance Software',
  headerTitle: 'Maintenance Management for Energy & Utility Operations',
  headerSubtitle:
    'Monitor, maintain, and optimize critical infrastructure — from power generation to distribution networks — with a CMMS you can self-host or run in the cloud.',
  headerImageUrl: '/static/images/industries/energy-hero.jpg',
  canonicalPath: 'industries/open-source-energy-utilities-maintenance-software',

  companyLogos: [],

  features: [
    {
      title: 'Critical infrastructure reliability',
      description:
        'Manage maintenance for power plants, substations, water systems, and network equipment with full visibility into assets, failures, and interventions.',
      imageUrl: 'https://atlas-cmms.com/assets/features/assets.png',
      learnMoreUrl: '/features/assets'
    },
    {
      title: 'Preventive & compliance-driven maintenance',
      description:
        'Automate inspections, regulatory checks, and preventive maintenance schedules to reduce downtime and stay compliant with safety standards.',
      imageUrl:
        'https://atlas-cmms.com/assets/features/preventive-maintenance.png',
      learnMoreUrl: '/features/preventive-maintenance'
    },
    {
      title: 'Field-ready work order management',
      description:
        'Coordinate technicians across sites, track interventions in real time, and maintain a complete service history for every critical asset.',
      imageUrl: 'https://atlas-cmms.com/assets/features/work-orders.png',
      learnMoreUrl: '/features/work-orders'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS gave us full control over maintenance for distributed energy assets without depending on expensive proprietary systems.',
      author: 'Operations Manager',
      company: 'Renewable Energy Provider'
    },
    {
      text: 'Self-hosting Atlas CMMS helped us meet strict security and compliance requirements in the utilities sector.',
      author: 'IT Infrastructure Lead',
      company: 'Regional Utility Company'
    }
  ],
  kpis: [
    {
      title:
        'Reduction in technician time spent filing work orders and locating asset information',
      value: '90',
      type: 'percentage'
    },
    {
      title: 'Return on Investment',
      value: '315',
      type: 'percentage'
    },
    {
      title: 'Savings from avoided production downtime',
      value: '450K',
      type: 'money'
    }
  ],
  faqs: [
    {
      question: 'Is Atlas CMMS suitable for energy and utility infrastructure?',
      answer:
        'Yes. Atlas CMMS supports complex asset hierarchies, preventive maintenance, inspections, and multi-site operations required in energy and utility environments.'
    },
    {
      question: 'Can Atlas CMMS handle compliance and safety inspections?',
      answer:
        'Absolutely. You can schedule recurring inspections, track maintenance history, and document interventions to support regulatory compliance and audits.'
    },
    {
      question:
        'Does Atlas CMMS support on-premise deployment for secure environments?',
      answer:
        'Yes. Atlas CMMS is open source and fully self-hostable using Docker, making it suitable for secure or isolated infrastructure networks.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source utility maintenance for a resilient grid. Manage critical assets, ensure regulatory compliance, and coordinate field teams with a secure, self-hosted CMMS.'
};

function EnergyPage() {
  return (
    <IndustryLayout {...energyUtilitiesData}>
      {/* Additional custom content can be added here as children */}
    </IndustryLayout>
  );
}

export default EnergyPage;
