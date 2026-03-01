import IndustryLayout, { IndustryLayoutProps } from "@/src/layouts/IndustryLayout";
import { Metadata } from "next";

const atlasHospitalityData: IndustryLayoutProps = {
  pageTitle: "Open Source Hotel & Resort Maintenance Software",
  headerTitle: "Host Your Own Guest-Centric Maintenance Platform",
  headerSubtitle:
    "A modern maintenance solution for hotels and resorts that protects your data, supports unlimited users, and keeps room turnover seamless—available in the cloud or self-hosted",
  headerImageUrl: "/static/images/industries/hospitality-hero.jpg",
  headerImageSizes: {
    width: 720,
    height: 427,
  },
  canonicalPath: "/industries/open-source-hospitality-maintenance-software",

  kpis: [
    {
      title: "Savings on subscription fees",
      value: "100",
      type: "percentage",
    },
    {
      title: "Faster room turnaround",
      value: "25",
      type: "percentage",
    },
    {
      title: "Control over proprietary data",
      value: "100",
      type: "percentage",
    },
  ],

  companyLogos: true,

  features: [
    {
      title: "Anonymous Guest Reporting",
      description:
        "Deploy QR codes in guest rooms that link to a lightweight web portal. Guests can report issues without creating an account, keeping your response times fast and frictionless.",
      imageUrl: "https://atlas-cmms.com/assets/features/guest-portal.png",
    },
    {
      title: "Preventive Housekeeping Cycles",
      description:
        "Schedule deep-cleaning rotations and mechanical inspections based on occupancy data. Ensure your high-traffic assets—from chillers to elevators—never fail during a full house.",
      imageUrl: "https://atlas-cmms.com/assets/features/pm-scheduling.png",
      learnMoreUrl: "/features/preventive-maintenance",
    },
    {
      title: "Unlimited Staff & Contractor Seats",
      description:
        "Stop paying per head. Add your entire housekeeping, engineering, and third-party vendor teams to the platform without increasing your monthly overhead.",
      imageUrl: "https://atlas-cmms.com/assets/features/unlimited-users.png",
    },
  ],

  testimonials: [],

  faqs: [
    {
      question: "Why choose open-source for hospitality?",
      answer:
        "Privacy and cost. Most hotels are locked into expensive per-user contracts. Atlas CMMS gives you the freedom to scale across hundreds of rooms without scaling your software bill.",
    },
    {
      question: "Where is our data stored?",
      answer:
        "Wherever you want. You can host Atlas on your own local servers or a private cloud, ensuring guest data is never processed by a third-party vendor.",
    },
    {
      question: "Can we customize the interface?",
      answer:
        "Yes. Since the code is open, you can white-label the guest portal with your hotel’s branding and colors for a consistent luxury experience.",
    },
  ],

  relatedContent: [],
  pageDescription:
    "Open-source hospitality maintenance built for the modern hotelier. Empower your guests, simplify room turnover, and slash licensing fees with a self-hosted, scalable CMMS.",
};

function HospitalityAtlasPage() {
  return <IndustryLayout {...atlasHospitalityData}></IndustryLayout>;
}

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: atlasHospitalityData.pageTitle,
    description: atlasHospitalityData.pageDescription,
    alternates: {
      canonical: atlasHospitalityData.canonicalPath,
    },
  };
}

export default HospitalityAtlasPage;
