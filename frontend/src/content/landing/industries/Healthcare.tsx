import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const healthcareData: IndustryLayoutProps = {
  pageTitle: 'Open Source Healthcare Maintenance Software',
  headerTitle: 'Reliable Maintenance for Hospitals and Healthcare Facilities',
  headerSubtitle:
    'Improve patient safety, maintain regulatory compliance, and reduce equipment downtime with an open-source CMMS designed for modern healthcare—available in the cloud or fully self-hosted.',
  headerImageUrl: '/static/images/industries/healthcare-hero.jpg',
  canonicalPath: 'industries/open-source-healthcare-maintenance-software',

  kpis: [
    {
      title: 'Reduction in critical equipment downtime',
      value: '40',
      type: 'percentage'
    },
    {
      title: 'Increase in preventive maintenance completion',
      value: '55',
      type: 'percentage'
    },
    {
      title: 'Faster response to maintenance requests',
      value: '35',
      type: 'percentage'
    }
  ],

  features: [
    {
      title: 'Preventive maintenance for medical equipment',
      description:
        'Schedule and automate maintenance for imaging devices, HVAC systems, laboratory equipment, and other critical assets to ensure continuous and safe operation.',
      imageUrl: '/static/images/features/preventive-maintenance.png',
      learnMoreUrl: '/features/preventive-maintenance'
    },
    {
      title: 'Traceable work orders and compliance history',
      description:
        'Maintain complete service records, inspections, and corrective actions to support audits, accreditation processes, and healthcare regulations.',
      imageUrl: '/static/images/features/work-orders.png',
      learnMoreUrl: '/features/work-orders'
    },
    {
      title: 'Asset lifecycle and spare parts management',
      description:
        'Track asset performance, maintenance costs, and spare parts availability to extend equipment lifespan and avoid unexpected failures.',
      imageUrl: '/static/images/features/assets.png',
      learnMoreUrl: '/features/assets'
    },
    {
      title: 'Custom dashboards for healthcare operations',
      description:
        'Monitor maintenance KPIs, compliance status, and team performance in real time with dashboards tailored to hospital and facility management teams.',
      imageUrl: '/static/images/features/reports-dashboards.png',
      learnMoreUrl: '/features/analytics'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS helped us keep critical medical equipment operational while simplifying compliance and audit preparation.',
      author: 'Biomedical Engineering Manager',
      company: 'Regional Hospital Network'
    },
    {
      text: 'Because Atlas CMMS is self-hosted, we maintain full control of sensitive operational data while improving maintenance efficiency across facilities.',
      author: 'Healthcare Facilities Director',
      company: 'Private Medical Center'
    }
  ],

  faqs: [
    {
      question: 'Why do hospitals need CMMS software?',
      answer:
        'Hospitals rely on complex medical equipment and infrastructure that must remain operational and compliant. A CMMS centralizes maintenance scheduling, asset tracking, and documentation to ensure safety, reliability, and regulatory readiness.'
    },
    {
      question: 'Can Atlas CMMS support healthcare compliance and audits?',
      answer:
        'Yes. Atlas CMMS stores inspection logs, maintenance history, and corrective actions, helping healthcare organizations prepare for internal reviews and external regulatory audits.'
    },
    {
      question: 'Is Atlas CMMS secure for sensitive healthcare environments?',
      answer:
        'Atlas CMMS can be fully self-hosted on your own infrastructure, giving healthcare organizations complete control over security, privacy, and data governance.'
    },
    {
      question: 'Does Atlas CMMS manage multiple hospitals or facilities?',
      answer:
        'Absolutely. Atlas CMMS supports multi-site asset tracking, centralized reporting, and standardized maintenance workflows across entire healthcare networks.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source healthcare maintenance that prioritizes patient safety. Keep critical medical equipment running, simplify audits, and maintain total data privacy with a self-hosted CMMS.'
};

function HealthcarePage() {
  return <IndustryLayout {...healthcareData} />;
}

export default HealthcarePage;
