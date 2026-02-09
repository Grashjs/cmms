'use client';
import IndustryLayout, {
  IndustryLayoutProps
} from '../../../../layouts/IndustryLayout';

const atlasHospitalityData: IndustryLayoutProps = {
  pageTitle: 'Open Source Hotel & Resort Maintenance Software',
  headerTitle: 'Host Your Own Guest-Centric Maintenance Platform',
  headerSubtitle:
    'A modern maintenance solution for hotels and resorts that protects your data, supports unlimited users, and keeps room turnover seamless—available in the cloud or self-hosted',
  headerImageUrl: '/static/images/industries/hospitality-hero.jpg',
  canonicalPath: 'industries/open-source-hospitality-maintenance-software',

  kpis: [
    {
      title: 'Savings on subscription fees',
      value: '100',
      type: 'percentage'
    },
    {
      title: 'Faster room turnaround',
      value: '25',
      type: 'percentage'
    },
    {
      title: 'Control over proprietary data',
      value: '100',
      type: 'percentage'
    }
  ],

  companyLogos: [
    // Use generic or community-contributed partner styles
    '/static/images/logos/independent-hotels-assoc.svg',
    '/static/images/logos/boutique-stay-group.svg',
    '/static/images/logos/eco-resort-collective.svg'
  ],

  features: [
    {
      title: 'Anonymous Guest Reporting',
      description:
        'Deploy QR codes in guest rooms that link to a lightweight web portal. Guests can report issues without creating an account, keeping your response times fast and frictionless.',
      imageUrl: 'https://atlas-cmms.com/assets/features/guest-portal.png'
    },
    {
      title: 'Preventive Housekeeping Cycles',
      description:
        'Schedule deep-cleaning rotations and mechanical inspections based on occupancy data. Ensure your high-traffic assets—from chillers to elevators—never fail during a full house.',
      imageUrl: 'https://atlas-cmms.com/assets/features/pm-scheduling.png',
      learnMoreUrl: '/docs/preventive-maintenance'
    },
    {
      title: 'Unlimited Staff & Contractor Seats',
      description:
        'Stop paying per head. Add your entire housekeeping, engineering, and third-party vendor teams to the platform without increasing your monthly overhead.',
      imageUrl: 'https://atlas-cmms.com/assets/features/unlimited-users.png'
    }
  ],

  testimonials: [
    {
      text: 'Moving to Atlas allowed us to keep our guest maintenance records on-site. We saved enough on licensing to hire an additional technician.',
      author: 'VP of Engineering',
      company: 'Heritage Hotel Collection'
    },
    {
      text: 'The open-source nature meant we could build a custom integration with our smart-lock system that no other CMMS supported.',
      author: 'Lead Developer',
      company: 'Urban Stay Apartments'
    }
  ],

  faqs: [
    {
      question: 'Why choose open-source for hospitality?',
      answer:
        'Privacy and cost. Most hotels are locked into expensive per-user contracts. Atlas CMMS gives you the freedom to scale across hundreds of rooms without scaling your software bill.'
    },
    {
      question: 'Where is our data stored?',
      answer:
        'Wherever you want. You can host Atlas on your own local servers or a private cloud, ensuring guest data is never processed by a third-party vendor.'
    },
    {
      question: 'Can we customize the interface?',
      answer:
        'Yes. Since the code is open, you can white-label the guest portal with your hotel’s branding and colors for a consistent luxury experience.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source hospitality maintenance built for the modern hotelier. Empower your guests, simplify room turnover, and slash licensing fees with a self-hosted, scalable CMMS.'
};

function HospitalityAtlasPage() {
  return <IndustryLayout {...atlasHospitalityData}></IndustryLayout>;
}

export default HospitalityAtlasPage;
