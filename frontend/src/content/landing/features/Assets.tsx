import IndustryLayout, {
  IndustryLayoutProps
} from '../../../layouts/IndustryLayout';
import { Inventory2, Timeline, FactCheck } from '@mui/icons-material';

const assetsData: IndustryLayoutProps = {
  pageTitle: 'Open-Source Asset Management Software',
  headerTitle: 'The #1 Open-Source CMMS for Asset Management',
  headerSubtitle:
    'Track equipment, monitor performance, and schedule preventive maintenance across your organization using a fully open-source and self-hosted CMMS—accessible on mobile and desktop.',
  headerImageUrl: '/static/images/features/asset-hero.png',
  canonicalPath: 'features/assets',

  advantages: [
    {
      title: 'Centralize all assets',
      description:
        'Keep every machine, facility, and component organized in one open-source system with full lifecycle visibility and unlimited customization.',
      icon: Inventory2
    },
    {
      title: 'Prevent failures before they happen',
      description:
        'Automate preventive maintenance schedules, inspections, and part replacements to reduce downtime and extend asset lifespan.',
      icon: Timeline
    },
    {
      title: 'Stay compliant and audit-ready',
      description:
        'Maintain complete maintenance history, warranties, and performance logs to meet compliance and operational standards with confidence.',
      icon: FactCheck
    }
  ],

  features: [
    {
      title: 'Organize and track unlimited assets',
      description:
        'Create structured asset hierarchies, attach documents, scan QR codes, and instantly access asset details from the field using our open-source mobile app.',
      imageUrl: '/static/images/features/assets-organization.png'
    },
    {
      title: 'Automate preventive maintenance',
      description:
        'Schedule recurring maintenance, inspections, and part replacements to keep equipment running efficiently without manual reminders.',
      imageUrl: '/static/images/features/assets-preventive.png'
    },
    {
      title: 'View complete asset history and performance',
      description:
        'Track work orders, downtime, maintenance costs, and lifecycle metrics to understand which assets drive productivity and which require attention.',
      imageUrl: '/static/images/features/assets-history.png'
    }
  ],

  testimonials: [],
  faqs: [
    {
      question: 'What is asset maintenance management?',
      answer:
        'Asset maintenance management is the process of tracking, maintaining, and optimizing physical equipment throughout its lifecycle to reduce downtime, control costs, and improve operational efficiency.'
    },
    {
      question: 'Can Atlas CMMS manage preventive maintenance schedules?',
      answer:
        'Yes. Atlas CMMS allows you to automate preventive maintenance, inspections, and recurring tasks for every asset in your organization.'
    },
    {
      question: 'Is Atlas CMMS fully open source and self-hosted?',
      answer:
        'Absolutely. Atlas CMMS is an open-source, self-hosted platform that gives you full control over your data, customization, and infrastructure.'
    }
  ],
  relatedContent: [],
  pageDescription:
    'Open-source asset management without the enterprise price tag. Track equipment history, reduce downtime, and manage everything from one self-hosted, powerful CMMS.'
};

function AssetsPage() {
  return <IndustryLayout {...assetsData} />;
}

export default AssetsPage;
