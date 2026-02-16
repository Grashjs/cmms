import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';

const educationData: IndustryLayoutProps = {
  pageTitle: 'Open Source Education Facility Management Software',
  headerTitle: 'Smart Maintenance for Schools, Colleges & Universities',
  headerSubtitle:
    'Ensure student safety and optimize campus operations with an open-source CMMS. Track every asset—from the boiler room to the classroom—without per-user licensing fees, in the cloud or fully self-hosted',
  headerImageUrl: '/static/images/industries/school-hero.jpg',
  canonicalPath: 'industries/open-source-education-maintenance-software',

  kpis: [
    {
      title: 'Faster emergency response time',
      value: '35',
      type: 'percentage'
    },
    {
      title: 'Reduction in annual repair costs',
      value: '22',
      type: 'percentage'
    },
    {
      title: 'Audit compliance accuracy',
      value: '100',
      type: 'percentage'
    }
  ],

  companyLogos: true,

  features: [
    {
      title: 'Campus-Wide Work Request Portal',
      description:
        'Give teachers and staff a simple way to report leaks, broken fixtures, or HVAC issues. No login required for requesters, keeping your hallways safe and functional.',
      imageUrl: 'https://atlas-cmms.com/assets/features/edu-requests.png'
    },
    {
      title: 'Regulatory & Safety Compliance',
      description:
        'Automate inspections for fire extinguishers, playground equipment, and lab safety. Maintain a digital paper trail for state audits and insurance requirements.',
      imageUrl: 'https://atlas-cmms.com/assets/features/compliance-tracking.png'
    },
    {
      title: 'Multi-Building Asset Mapping',
      description:
        'Organize maintenance by building, floor, or classroom. Track the lifecycle of expensive assets like HVAC units, boilers, and school bus fleets in one central database.',
      imageUrl: 'https://atlas-cmms.com/assets/features/asset-mapping.png',
      learnMoreUrl: '/features/assets'
    }
  ],

  testimonials: [
    {
      text: 'Switching to an open-source model allowed our school district to scale maintenance tracking to 12 campuses without the heavy burden of SaaS subscription fees.',
      author: 'Director of Facilities',
      company: 'Public School District'
    },
    {
      text: 'The ability to self-host ensures our student and faculty data stays within our private network, meeting our strict privacy protocols.',
      author: 'IT Administrator',
      company: 'State University'
    }
  ],

  faqs: [
    {
      question: 'How does Atlas CMMS handle limited school budgets?',
      answer:
        'Unlike proprietary software, Atlas CMMS is open-source. You save on recurring per-user seats, allowing you to reallocate those funds toward actual facility repairs and school supplies.'
    },
    {
      question: 'Can we manage multiple school sites in one instance?',
      answer:
        'Yes. Our hierarchical asset system allows you to manage an entire district or university system, with permission levels specific to each building’s maintenance lead.'
    },
    {
      question: 'Is student data protected?',
      answer:
        'Absolutely. By self-hosting Atlas CMMS, you have 100% ownership of your data. There is no third-party access to your facility records or staff lists.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source campus maintenance that saves your budget. Simplify school work requests, automate inspections, and manage facility assets with a self-hosted CMMS built for education.'
};

function EducationPage() {
  return <IndustryLayout {...educationData}></IndustryLayout>;
}

export default EducationPage;
