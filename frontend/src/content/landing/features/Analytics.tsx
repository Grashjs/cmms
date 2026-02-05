import IndustryLayout, {
  IndustryLayoutProps
} from '../../../layouts/IndustryLayout';
import { BarChart, DashboardCustomize, QueryStats } from '@mui/icons-material';

const reportsDashboardsData: IndustryLayoutProps = {
  pageTitle: 'Open-Source Maintenance Reports & Dashboards',
  headerTitle: 'Turn Maintenance Data into Actionable Insights',
  headerSubtitle:
    'Visualize performance, track KPIs, and make smarter maintenance decisions with fully customizable dashboards and automated reporting in an open-source, self-hosted CMMS.',
  headerImageUrl: '/static/images/overview/analytics_screenshot.png',

  advantages: [
    {
      title: 'See real-time maintenance performance',
      description:
        'Monitor work orders, downtime, costs, and asset reliability instantly with live dashboards built for maintenance teams.',
      icon: BarChart
    },
    {
      title: 'Customize dashboards for every role',
      description:
        'Create tailored views for technicians, managers, and executives so everyone sees the data that matters most.',
      icon: DashboardCustomize
    },
    {
      title: 'Make data-driven maintenance decisions',
      description:
        'Analyze trends, identify inefficiencies, and continuously improve maintenance strategy using clear visual insights.',
      icon: QueryStats
    }
  ],

  features: [
    {
      title: 'Automated maintenance reporting',
      description:
        'Generate and share scheduled or real-time reports on assets, work orders, costs, and performance without manual effort.',
      imageUrl: '/static/images/features/reports-automation.png',
      learnMoreUrl: '/account/register'
    },
    {
      title: 'Flexible and interactive dashboards',
      description:
        'Build custom widgets, calculate KPIs, and organize visualizations to match your operational workflows and goals.',
      imageUrl: '/static/images/features/reports-dashboards.png',
      learnMoreUrl: '/account/register'
    },
    {
      title: 'Team and asset performance analytics',
      description:
        'Track MTTR, MTBF, workload distribution, and maintenance efficiency to understand where improvements are needed most.',
      imageUrl: '/static/images/features/reports-analytics.png',
      learnMoreUrl: '/account/register'
    }
  ],

  testimonials: [
    {
      text: 'Atlas CMMS dashboards gave us immediate visibility into maintenance performance and helped leadership make faster, better decisions.',
      author: 'Laura Benali',
      company: 'Technical Director'
    }
  ],

  faqs: [
    {
      question: 'What are maintenance dashboards in a CMMS?',
      answer:
        'Maintenance dashboards display key performance indicators such as work order completion, downtime, maintenance costs, and asset reliability to help teams monitor and improve operations.'
    },
    {
      question: 'Can Atlas CMMS generate automated reports?',
      answer:
        'Yes. Atlas CMMS supports scheduled and real-time reporting that can be shared with teams or management to keep everyone aligned on maintenance performance.'
    },
    {
      question: 'Is reporting fully customizable in Atlas CMMS?',
      answer:
        'Absolutely. Because Atlas CMMS is open source and self-hosted, you can customize dashboards, metrics, and visualizations to match your exact operational needs.'
    }
  ],

  relatedContent: []
};

function ReportsDashboardsPage() {
  return <IndustryLayout {...reportsDashboardsData} />;
}

export default ReportsDashboardsPage;
