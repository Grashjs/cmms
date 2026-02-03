import IndustryLayout from 'src/layouts/IndustryLayout';

const facilityManagementData = {
  pageTitle: 'Best Open-source CMMS Software for Facility Maintenance',
  headerTitle: 'Facility management software you can trust',
  headerSubtitle:
    'Manage maintenance, work orders, repairs, and preventive tasks from one cloud platform to stay compliant and keep facilities running smoothly.',
  headerImageUrl:
    'https://limble.com/wp-content/uploads/Industries_Facility_Management_Header.png',
  companyLogos: [
    'https://limblecmms.com/wp-content/uploads/client-color-facility-riteaid.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-facility-eataly.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-facility-nationalaviary.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-facility-g.svg',
    'https://limblecmms.com/wp-content/uploads/client-color-facility-goodwill.svg'
  ],
  features: [
    {
      title: 'Preventive maintenance',
      description:
        'Expertly manage PMs using detailed checklists and proactive scheduling to prevent costly, unexpected downtime.',
      imageUrl:
        'https://limble.com/wp-content/uploads/Industry-preventive-image.png',
      learnMoreUrl:
        'https://limblecmms.com/cmms/preventive-maintenance-software/'
    },
    {
      title: 'Spare parts inventory planning',
      description:
        'Automate reminders to reorder so you always have the replacement part you need, minimizing delays and maintenance costs.',
      imageUrl:
        'https://limble.com/wp-content/uploads/Industry-parts-image-2.png',
      learnMoreUrl:
        'https://limblecmms.com/cmms/spare-parts-inventory-software/'
    },
    {
      title: 'Collaborate with vendors, partners, and colleagues',
      description:
        'Invite others in your ecosystem to get involved with PM tasks, complete a work order, fulfill a purchase order, and get notifications of important events.',
      imageUrl: 'https://limble.com/wp-content/uploads/work-order-orange.png',
      learnMoreUrl: 'https://limblecmms.com/cmms/work-order-software/'
    },
    {
      title: 'Customized dashboards and reporting',
      description:
        'Track all your maintenance and facilities KPIs using customizable dashboards and share data-driven, actionable insights with stakeholders in real time.',
      imageUrl:
        'https://limble.com/wp-content/uploads/automate-reporting-green.png',
      learnMoreUrl: 'https://limblecmms.com/cmms/reports-dashboards/'
    },
    {
      title: 'Maintenance management for large facilities',
      description:
        'Facility managers overseeing multiple enterprise properties, including commercial real estate and healthcare use Limble to scale maintenance planning, PMs, and asset management without adding unnecessary complexity.',
      imageUrl:
        'https://limble.com/wp-content/uploads/custom-dashboard-no-date-red-lowercase-2.webp',
      learnMoreUrl: 'https://limble.com/solutions/enterprise/'
    }
  ],
  testimonials: [
    {
      text: 'Panorama Mountain resort streamlined audits and found efficiency at their 3,000-acre ski resort.',
      author: 'Panorama Mountain resort',
      company: ''
    },
    {
      text: 'The beachfront condominium shored up its preventive maintenance program to protect the safety of its residents.',
      author: 'Sanctuary at False Cape',
      company: ''
    }
  ],
  faqs: [
    {
      question: 'What is facility maintenance?',
      answer:
        'Facility maintenance encompasses all the tasks required to keep a building, its systems, and assets safe, functional, and efficient. This includes routine upkeep, repairs, building maintenance (like painting or structural checks), preventive maintenance scheduling for systems like HVAC, managing maintenance requests, and ensuring grounds are well-kept. The goal is to create a safe, comfortable, and productive environment while preserving asset value.'
    },
    {
      question: 'What is facilities maintenance software?',
      answer:
        'Facilities maintenance software, often integrated with a CMMS, helps manage the upkeep of buildings and equipment. It enables facilities to schedule preventive maintenance, track work orders, and manage repairs efficiently. With CMMS features like asset tracking, maintenance history, and resource planning, it ensures safe, cost-effective, and well-maintained facilities that support a better environment.'
    },
    {
      question:
        'What key features should you look for in facility management software solutions?',
      answer:
        'Comprehensive work order management, Preventive maintenance scheduling, Asset management, Inventory management, Reporting and analytics, Mobile accessibility, Integration capabilities, User-friendliness, Customization.'
    },
    {
      question: 'What are the benefits of facility maintenance software?',
      answer:
        'Using facilities maintenance software provides significant benefits for managing and maintaining physical spaces efficiently: Ensure safety, Reduce costs, Streamline operations, Improve efficiency, Enhance user experience, Optimize resource allocation, Support informed decisions, Increase profitability, Achieve operational excellence.'
    },
    {
      question: 'Who uses facilities maintenance software?',
      answer:
        'Facilities maintenance software is used by a wide range of professionals responsible for asset upkeep and facility operations. This includes facility managers, operations managers, maintenance managers, asset managers, and more. They use these powerful management tools to manage assets, schedule maintenance, handle service requests, and ensure safety and compliance.'
    }
  ],
  relatedContent: [
    {
      title: '5 Types of Facilities That Need Facilities Management',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-productive-1.png',
      url: 'https://limble.com/blog/facility-definition/'
    },
    {
      title: 'What is Facility Maintenance?',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-machine.png',
      url: 'https://limble.com/maintenance-definitions/facility-maintenance/'
    },
    {
      title: 'Complete Guide to Facilities Management',
      imageUrl:
        'https://limble.com/wp-content/uploads/related-content-steps.png',
      url: 'https://limble.com/blog/facilities-management/'
    }
  ]
};

function FacilityManagementPage() {
  return <IndustryLayout {...facilityManagementData} />;
}

export default FacilityManagementPage;
