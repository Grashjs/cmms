import IndustryLayout, { IndustryLayoutProps } from "@/src/layouts/IndustryLayout";
import { Inventory, Link, ShoppingCart } from "@mui/icons-material";
import { Metadata } from "next";

const sparePartsData: IndustryLayoutProps = {
  pageTitle: "Open-Source Spare Parts Inventory Management",
  headerTitle: "Control Spare Parts, Costs, and Availability",
  headerSubtitle:
    "Track spare parts across assets and locations, automate reordering, and prevent costly downtime using a fully open-source and self-hosted CMMS built for real maintenance teams.",
  headerImageUrl: "/static/images/features/part-hero.png",
  headerImageSizes: {
    width: 927,
    height: 505,
  },
  canonicalPath: "/features/inventory",

  advantages: [
    {
      title: "Real-time inventory visibility",
      description:
        "Know exactly which parts are in stock, where they are stored, and how quickly they are consumed across all facilities.",
      icon: Inventory,
    },
    {
      title: "Connect parts to assets and work orders",
      description:
        "Link spare parts directly to maintenance activities to track usage, costs, and equipment reliability in one unified system.",
      icon: Link,
    },
    {
      title: "Automate purchasing and restocking",
      description:
        "Define minimum stock levels, trigger reorder alerts, and streamline procurement to avoid shortages and emergency purchases.",
      icon: ShoppingCart,
    },
  ],

  features: [
    {
      title: "Centralized spare parts catalog",
      description:
        "Store detailed information for every part including quantity, location, suppliers, compatible assets, and historical usage.",
      imageUrl: "/static/images/features/spareparts-catalog.png",
    },
    {
      title: "Smart stock tracking and alerts",
      description:
        "Monitor consumption in real time and receive notifications when stock reaches critical thresholds to prevent downtime.",
      imageUrl: "/static/images/features/spareparts-alerts.png",
    },
    {
      title: "Integrated purchasing and cost control",
      description:
        "Create purchase orders, analyze spending trends, and optimize inventory levels to reduce waste and control maintenance budgets.",
      imageUrl: "/static/images/features/spareparts-purchasing.png",
    },
  ],

  testimonials: [],
  faqs: [
    {
      question: "What is spare parts inventory management software?",
      answer:
        "Spare parts inventory management software helps organizations track stock levels, automate reordering, connect parts to maintenance activities, and ensure critical components are always available when needed.",
    },
    {
      question: "Can Atlas CMMS prevent stock shortages?",
      answer:
        "Yes. Atlas CMMS monitors inventory in real time, sends low-stock alerts, and supports automated purchasing workflows to keep essential parts available.",
    },
    {
      question: "Is Atlas CMMS suitable for multi-site inventory management?",
      answer:
        "Absolutely. Atlas CMMS supports multiple locations, centralized reporting, and full visibility across distributed facilities in a self-hosted open-source environment.",
    },
  ],
  relatedContent: [],
  pageDescription:
    "Open-source inventory control that never sleeps. Take charge of your spare parts, automate low-stock alerts, and eliminate maintenance delays with a self-hosted CMMS.",
};

function SparePartsPage() {
  return <IndustryLayout {...sparePartsData} />;
}

export async function generateMetadata(): Promise<Metadata> {
  return {
    title: sparePartsData.pageTitle,
    description: sparePartsData.pageDescription,
  };
}

export default SparePartsPage;
