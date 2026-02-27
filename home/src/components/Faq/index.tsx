"use client";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Typography
} from '@mui/material';
import { ExpandMore } from '@mui/icons-material';
import { useState } from 'react';
import { useTranslations } from 'next-intl';

export interface FaqItem {
  id?: string;
  title: string;
  content: React.ReactNode;
}

interface FaqProps {
  items: FaqItem[];
  title?: string;
}

export default function Faq({ items, title }: FaqProps) {
  const [expanded, setExpanded] = useState<string | false>(false);
  const t = useTranslations();

  const handleChange =
    (panel: string) => (event: React.SyntheticEvent, isExpanded: boolean) => {
      setExpanded(isExpanded ? panel : false);
    };

  return (
    <Box sx={{ mt: 8, mb: 8 }}>
      {title && (
        <Typography variant="h2" component="h2" gutterBottom textAlign="center">
          {t(title)}
        </Typography>
      )}

      <Box sx={{ mt: 2 }}>
        {items.map((item, index) => {
          const panelId = item.id || `panel-${index}`;
          return (
            <Accordion
              key={panelId}
              expanded={expanded === panelId}
              onChange={handleChange(panelId)}
            >
              <AccordionSummary
                expandIcon={<ExpandMore />}
                aria-controls={`${panelId}-content`}
                id={`${panelId}-header`}
              >
                <Typography variant="h6" fontWeight={'bold'}>
                  {item.title}
                </Typography>
              </AccordionSummary>
              <AccordionDetails>{item.content}</AccordionDetails>
            </Accordion>
          );
        })}
      </Box>
    </Box>
  );
}

