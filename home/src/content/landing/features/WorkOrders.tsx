"use client";
import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';
import { AccessTime, Edit, TrendingUp } from '@mui/icons-material';

const workOrdersData: IndustryLayoutProps = {
  pageTitle: 'Open-Source Work Order Management Software',
  headerTitle: 'The #1 Open-Source CMMS for Work Order Management',
  headerSubtitle:
    'Manage maintenance requests, schedule and track work orders, and assign tasks to your team using a fully open-source platform—accessible on both mobile and desktop.',
  headerImageUrl: '/static/images/overview/work_orders_screenshot.png',
  canonicalPath: 'features/work-orders',
  advantages: [
    {
      title: 'Reduce downtime',
      description:
        'Leverage a proactive, preventive maintenance approach with an open-source solution that keeps your operations running smoothly and costs low.',
      icon: AccessTime
    },
    {
      title: 'Boost productivity',
      description:
        'Prioritize and complete work orders faster using collaborative open-source software your team can trust and customize.',
      icon: TrendingUp
    },
    {
      title: 'Full transparency',
      description:
        'Track every work order in your system, ensuring audit readiness and complete visibility into tasks across your team.',
      icon: Edit
    }
  ],

  features: [
    {
      title: 'Create work orders with a scan or a click',
      description:
        'Start new work orders from your dashboard, asset records, or vendors. Add details and photos directly from your phone using our open-source mobile app.',
      imageUrl: '/static/images/features/wo-creation.png'
    },
    {
      title: 'Assign and follow up on tasks',
      description:
        'Automatically assign work orders and link them to assets and parts for a complete, open-source workflow management experience.',
      imageUrl: '/static/images/features/wo-assignment.png'
    },
    {
      title: 'Search, filter, and prioritize work orders',
      description:
        'Easily sort and prioritize work orders company-wide by importance, team, location, asset, and more—all on an open-source platform.',
      imageUrl: '/static/images/features/wo-filter.png'
    }
  ],

  testimonials: [
    {
      text: 'The open-source CMMS has allowed us to fully customize our maintenance workflow and schedule, keeping all tasks under control week by week.',
      author: 'Sam Livingstone',
      company: 'Maintenance Mechanic'
    }
  ],

  faqs: [
    {
      question: 'What is a maintenance work order?',
      answer:
        'A work order is a formal request for a specific task or set of tasks, essential for equipment maintenance, repairs, or operational needs.'
    },
    {
      question: 'Does Atlas CMMS have a mobile app?',
      answer:
        'Yes. Our open-source mobile app is designed for technicians on the go, making it easy to create, track, and complete work orders.'
    },
    {
      question: 'Can I add checklists to work orders?',
      answer:
        'Absolutely. Create your own checklists in our open-source platform.'
    }
  ],
  relatedContent: [],
  pageDescription:
    'Open-source work order management that just works. Simplify requests, track repairs in real-time, and empower your team with a mobile-ready, self-hosted CMMS.'
};

function WorkOrdersPage() {
  return <IndustryLayout {...workOrdersData} />;
}

export default WorkOrdersPage;
