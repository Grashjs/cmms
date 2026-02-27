"use client";
import IndustryLayout, {
  IndustryLayoutProps
} from 'src/layouts/IndustryLayout';
import { Schedule, BuildCircle, Insights } from '@mui/icons-material';

const preventiveMaintenanceData: IndustryLayoutProps = {
  pageTitle: 'Open-Source Preventive Maintenance Software',
  headerTitle: 'Automate Preventive Maintenance with Open-Source CMMS',
  headerSubtitle:
    'Plan recurring maintenance, reduce unexpected failures, and extend asset lifespan using a fully open-source and self-hosted preventive maintenance platform accessible anywhere.',
  headerImageUrl: '/static/images/features/pm-hero.png',
  canonicalPath: 'features/preventive-maintenance',

  advantages: [
    {
      title: 'Automate recurring maintenance',
      description:
        'Create time-based or usage-based maintenance schedules that run automatically without spreadsheets or manual reminders.',
      icon: Schedule
    },
    {
      title: 'Reduce downtime and breakdowns',
      description:
        'Keep equipment reliable by servicing assets before failures occur, improving uptime and operational continuity.',
      icon: BuildCircle
    },
    {
      title: 'Make data-driven decisions',
      description:
        'Analyze maintenance history, costs, and performance trends to continuously optimize your preventive strategy.',
      icon: Insights
    }
  ],

  features: [
    {
      title: 'Flexible maintenance scheduling',
      description:
        'Configure daily, weekly, monthly, or usage-based preventive maintenance tasks for any asset, location, or team.',
      imageUrl: '/static/images/features/pm-scheduling.png'
    },
    {
      title: 'Automatic work order generation',
      description:
        'Generate work orders instantly when preventive schedules are due so technicians always know what to do next.',
      imageUrl: '/static/images/features/pm-workorders.png'
    },
    {
      title: 'Full maintenance history tracking',
      description:
        'Maintain a complete record of inspections, repairs, parts, and technician actions for audits and performance analysis.',
      imageUrl: '/static/images/features/pm-history.png'
    }
  ],

  testimonials: [
    {
      text: 'Since implementing Atlas CMMS preventive maintenance, unexpected equipment failures dropped dramatically and our team works far more efficiently.',
      author: 'Sophie Martin',
      company: 'Operations Manager'
    }
  ],

  faqs: [
    {
      question: 'What is preventive maintenance software?',
      answer:
        'Preventive maintenance software helps organizations schedule and perform routine maintenance before equipment fails, reducing downtime, extending asset lifespan, and improving reliability.'
    },
    {
      question: 'Can Atlas CMMS automate preventive maintenance tasks?',
      answer:
        'Yes. Atlas CMMS automatically schedules maintenance, creates work orders, and tracks completion so your team never misses critical service activities.'
    },
    {
      question: 'Is Atlas CMMS suitable for self-hosting?',
      answer:
        'Absolutely. Atlas CMMS is open source and self-hosted, giving you full control over infrastructure, customization, and data ownership.'
    }
  ],

  relatedContent: [],
  pageDescription:
    'Open-source preventive maintenance built for reliability. Automate schedules, slash repair costs, and extend your equipment life with the world’s most flexible CMMS.'
};

function PreventiveMaintenancePage() {
  return <IndustryLayout {...preventiveMaintenanceData} />;
}

export default PreventiveMaintenancePage;
