import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Typography
} from '@mui/material';
import { ExpandMore } from '@mui/icons-material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

interface FaqItem {
  id: string;
  title: string;
  content: React.ReactNode;
}

export default function Faq() {
  const [expanded, setExpanded] = useState<string | false>(false);
  const { t }: { t: any } = useTranslation();

  const handleChange =
    (panel: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
      setExpanded(isExpanded ? panel : false);
    };

  const faqItems: FaqItem[] = [
    // {
    //   id: 'panel1',
    //   title: 'Is there a fee for implementation?',
    //   content: (
    //     <Typography variant="body1">
    //       Yes, the pricing depends on the implementation package that best fits
    //       your team's needs. Each package includes setup assistance from a
    //       dedicated Implementation Manager to guide and assist you through
    //       system configuration, data importing, and best practices for a smooth
    //       transition.
    //     </Typography>
    //   )
    // },
    {
      id: 'panel2',
      title: 'Which types of users are considered free users?',
      content: (
        <>
          <Typography variant="body1" paragraph>
            There are three user types that do not require a paid license:
          </Typography>
          <Box component="ol" sx={{ pl: 2 }}>
            <li>
              <Typography variant="body1">
                <strong>View Only Users</strong> — These users are typically
                supervisors who log in infrequently to view a snapshot of
                maintenance activity. They can also submit work requests and run
                reports.
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>Requester Users</strong> — These users can only submit
                work requests and view the status of those requests. They cannot
                see work orders, assets, parts, or any other data stored in the
                CMMS.
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>Third-Party Users</strong> — These users are typically
                vendors and contractors. They can't actually sign into the
                system. They can only submit updates to a specific work order
                they're tagged on through a public link.
              </Typography>
            </li>
          </Box>
        </>
      )
    },
    {
      id: 'panel3',
      title: 'Which types of users are considered paid users?',
      content: (
        <>
          <Typography variant="body1" paragraph>
            There are three user types that require a paid license:
          </Typography>
          <Box component="ol" sx={{ pl: 2 }}>
            <li>
              <Typography variant="body1">
                <strong>Admin Users</strong> — These users have the ability to
                add others to the account, accept or deny work requests, and
                edit work order details. They are the users that control the
                account. You can have multiple admins per account.
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>Technical Users</strong> — These users are typically
                technicians who close out work orders in the field. They can
                edit work orders they create but not other work orders. They can
                add pictures and status updates to work orders and create new
                work orders.
              </Typography>
            </li>
            <li>
              <Typography variant="body1">
                <strong>Limited Technical Users</strong> — These users have the
                same privileges as the technical user. The only exception is
                that they can only see work orders assigned to them — not other
                technical users on the account.
              </Typography>
            </li>
          </Box>
        </>
      )
    },
    {
      id: 'panel4',
      title: 'Can I change plans later?',
      content: (
        <Typography>
          Yes, you can upgrade or downgrade your plan at any time. Changes take
          effect at the start of your next billing cycle.
        </Typography>
      )
    },
    {
      id: 'panel5',
      title: 'Is there a free trial?',
      content: (
        <Typography>
          Yes, we offer a 30-day free trial of the Professional plan so you can
          experience all the features before making a decision.
        </Typography>
      )
    },
    {
      id: 'panel6',
      title: 'Do you offer discounts for non-profits?',
      content: (
        <Typography>
          Yes, we offer special pricing for non-profit organizations. Please
          contact our sales team for more information.
        </Typography>
      )
    },
    {
      id: 'panel7',
      title: 'What payment methods do you accept?',
      content: (
        <Typography>
          We accept all major credit cards, bank transfers, and PayPal. For
          Enterprise plans, we can also arrange invoicing.
        </Typography>
      )
    },
    {
      id: 'panel8',
      title: 'Can I cancel my subscription?',
      content: (
        <Typography>
          Yes, you can cancel your subscription at any time. You'll continue to
          have access until the end of your current billing period.
        </Typography>
      )
    },
    {
      id: 'panel9',
      title: 'Is my data secure?',
      content: (
        <Typography>
          Yes, we take data security very seriously. All data is encrypted in
          transit and at rest, and we perform regular security audits.
        </Typography>
      )
    }
  ];

  return (
    <Box sx={{ mt: 8, mb: 8 }}>
      <Typography variant="h2" component="h2" gutterBottom textAlign="center">
        {t('Frequently Asked Questions')}
      </Typography>

      <Box sx={{ mt: 2 }}>
        {faqItems.map((item) => (
          <Accordion
            key={item.id}
            expanded={expanded === item.id}
            onChange={handleChange(item.id)}
          >
            <AccordionSummary
              expandIcon={<ExpandMore />}
              aria-controls={`${item.id}-content`}
              id={`${item.id}-header`}
            >
              <Typography variant="h6" fontWeight={'bold'}>
                {item.title}
              </Typography>
            </AccordionSummary>
            <AccordionDetails>{item.content}</AccordionDetails>
          </Accordion>
        ))}
      </Box>
    </Box>
  );
}
