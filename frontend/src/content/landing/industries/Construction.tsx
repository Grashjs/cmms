import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const constructionData: IndustryLayoutProps = {
  pageTitle: 'Open Source Construction Equipment Maintenance Software',
  headerTitle: 'Keep Heavy Iron Moving without the Heavy Fees',
  headerSubtitle:
    'The first open-source CMMS built for real job sites. Track heavy equipment, manage field work orders, and stay compliant—deploy in the cloud or run it on infrastructure you control.',
  headerImageUrl: '/static/images/industries/construction-hero.jpg',
  canonicalPath: 'industries/open-source-construction-maintenance-software',

  kpis: [
    {
      title: 'Reduction in field downtime',
      value: '28',
      type: 'percentage'
    },
    {
      title: 'Equipment lifespan extension',
      value: '15',
      type: 'percentage'
    },
    {
      title: 'Ownership of maintenance data',
      value: '100',
      type: 'percentage'
    }
  ],

  companyLogos: [
    // Representing independent contractors and heavy equipment collectives
    '/static/images/logos/contractor-alliance.svg',
    '/static/images/logos/heavy-iron-rentals.svg',
    '/static/images/logos/infrastructure-builders.svg'
  ],

  features: [
    {
      title: 'Rugged Asset Tracking',
      description:
        'Manage everything from excavators to power tools. Use the open API to integrate with GPS telematics and track engine hours automatically for more accurate PM scheduling.',
      imageUrl: 'https://atlas-cmms.com/assets/features/heavy-equipment.png',
      learnMoreUrl: '/features/assets'
    },
    {
      title: 'Offline-First Field Work Orders',
      description:
        'Construction sites don’t always have Wi-Fi. Our mobile-ready portal allows technicians to log repairs and safety inspections offline, syncing as soon as they reach a signal.',
      imageUrl: 'https://atlas-cmms.com/assets/features/offline-mode.png',
      learnMoreUrl: '/features/work-orders'
    },
    {
      title: 'Multi-Site Project Sync',
      description:
        'Organize your fleet by project code or geographic region. Move assets between job sites and maintain a continuous service history that stays with the machine, not the vendor.',
      imageUrl: 'https://atlas-cmms.com/assets/features/project-sync.png'
    }
  ],

  testimonials: [
    {
      text: 'Proprietary CMMS costs were eating into our project margins. Switching to Atlas gave us full enterprise features with zero per-user licensing costs.',
      author: 'Fleet Manager',
      company: 'General Contractors Inc.'
    },
    {
      text: 'Having the source code allowed our IT team to build a custom inspection module that mirrors our specific OSHA safety requirements perfectly.',
      author: 'Safety Director',
      company: 'Civil Works Group'
    }
  ],

  faqs: [
    {
      question: 'How does Atlas handle engine hour tracking?',
      answer:
        'Atlas CMMS allows for manual meter reading updates via mobile or automated ingestion via our open REST API, making it easy to trigger maintenance based on actual machine usage.'
    },
    {
      question: 'Is it really free for unlimited users?',
      answer:
        'Yes. As an open-source platform, you can add 5 or 500 technicians and subcontractors without any change to your software costs. You only pay for your own hosting infrastructure.'
    },
    {
      question: 'Can we use this for safety inspections?',
      answer:
        'Absolutely. You can create custom digital checklists for daily walk-arounds, crane inspections, and heavy equipment safety audits, keeping you compliant and audit-ready.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source CMMS for construction that keeps your heavy iron moving. Track assets across job sites, manage field work orders offline, and own your maintenance data.'
};

function ConstructionAtlasPage() {
  return <IndustryLayout {...constructionData}></IndustryLayout>;
}

export default ConstructionAtlasPage;
